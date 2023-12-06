/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.core.model.core.internal.folder;

import static org.openhab.core.service.WatchService.Kind.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.OpenHAB;
import org.openhab.core.model.core.ModelParser;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyService;
import org.openhab.core.service.WatchService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is able to observe multiple folders for changes and notifies the
 * model repository about every change, so that it can update itself.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Fabio Marini - Refactoring to use WatchService
 * @author Ana Dimova - reduce to a single watch thread for all class instances
 */
@NonNullByDefault
@Component(name = "org.openhab.core.folder", immediate = true, configurationPid = "org.openhab.folder", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class FolderObserver implements WatchService.WatchEventListener {
    private final WatchService watchService;
    private final Logger logger = LoggerFactory.getLogger(FolderObserver.class);

    /* the model repository is provided as a service */
    private final ModelRepository modelRepository;
    private static final String READYMARKER_TYPE = "dsl";

    private final ReadyService readyService;

    private boolean activated;

    /* map that stores a list of valid file extensions for each folder */
    private final Map<String, Set<String>> folderFileExtMap = new ConcurrentHashMap<>();

    /* set of file extensions for which we have parsers already registered */
    private final Set<String> parsers = new HashSet<>();

    /* set of file extensions for missing parsers during activation */
    private final Set<String> missingParsers = new HashSet<>();

    /* set of files that have been ignored due to a missing parser */
    private final Set<Path> ignoredPaths = new HashSet<>();
    private final Map<String, Path> namePathMap = new HashMap<>();

    @Activate
    public FolderObserver(final @Reference ModelRepository modelRepo, final @Reference ReadyService readyService,
            final @Reference(target = WatchService.CONFIG_WATCHER_FILTER) WatchService watchService) {
        this.modelRepository = modelRepo;
        this.readyService = readyService;
        this.watchService = watchService;
    }

    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC)
    protected void addModelParser(ModelParser modelParser) {
        String extension = modelParser.getExtension();
        logger.debug("Adding parser for '{}' extension", extension);
        parsers.add(extension);

        if (activated) {
            processIgnoredPaths(extension);
            readyService.markReady(new ReadyMarker(READYMARKER_TYPE, extension));
            logger.debug("Marked extension '{}' as ready", extension);
        } else {
            logger.debug("{} is not yet activated", FolderObserver.class.getSimpleName());
        }
    }

    protected void removeModelParser(ModelParser modelParser) {
        String extension = modelParser.getExtension();
        logger.debug("Removing parser for '{}' extension", extension);
        parsers.remove(extension);

        Set<String> removed = modelRepository.removeAllModelsOfType(extension);
        ignoredPaths.addAll(removed.stream().map(namePathMap::get).collect(Collectors.toSet()));
    }

    @Activate
    public void activate(ComponentContext ctx) {
        Dictionary<String, Object> config = ctx.getProperties();

        Enumeration<String> keys = config.keys();
        while (keys.hasMoreElements()) {
            String folderName = keys.nextElement();
            if (!folderName.matches("[A-Za-z0-9_]*")) {
                // we allow only simple alphanumeric names for model folders - everything else might be other service
                // properties
                continue;
            }

            Path folderPath = watchService.getWatchPath().resolve(folderName);
            if (Files.exists(folderPath) && Files.isDirectory(folderPath)) {
                String[] validExtensions = ((String) config.get(folderName)).split(",");
                folderFileExtMap.put(folderName, Set.of(validExtensions));
            } else {
                logger.warn("Directory '{}' does not exist in '{}'. Please check your configuration settings!",
                        folderName, OpenHAB.getConfigFolder());
            }
        }

        watchService.registerListener(this, Path.of(""));

        addModelsToRepo();
        this.activated = true;
        logger.debug("{} has been activated", FolderObserver.class.getSimpleName());

        // process ignored paths for missing parsers which were added during activation
        for (String extension : missingParsers) {
            if (parsers.contains(extension)) {
                processIgnoredPaths(extension);
                readyService.markReady(new ReadyMarker(READYMARKER_TYPE, extension));
                logger.debug("Marked extension '{}' as ready", extension);
            }
        }
        missingParsers.clear();
    }

    @Deactivate
    public void deactivate() {
        watchService.unregisterListener(this);
        activated = false;
        deleteModelsFromRepo();
        ignoredPaths.clear();
        folderFileExtMap.clear();
        parsers.clear();
        namePathMap.clear();
        logger.debug("{} has been deactivated", FolderObserver.class.getSimpleName());
    }

    private void processIgnoredPaths(String extension) {
        logger.debug("Processing {} ignored paths for '{}' extension", ignoredPaths.size(), extension);

        Set<Path> clonedSet = new HashSet<>(ignoredPaths);
        for (Path path : clonedSet) {
            if (extension.equals(getExtension(path))) {
                checkPath(path, CREATE);
                ignoredPaths.remove(path);
            }
        }

        logger.debug("Finished processing ignored paths for '{}' extension. {} ignored paths remain", extension,
                ignoredPaths.size());
    }

    private void addModelsToRepo() {
        for (Map.Entry<String, Set<String>> entry : folderFileExtMap.entrySet()) {
            String folderName = entry.getKey();
            Set<String> validExtensions = entry.getValue();

            if (validExtensions.isEmpty()) {
                logger.debug("Folder '{}' has no valid extensions", folderName);
                continue;
            }

            Path folderPath = watchService.getWatchPath().resolve(folderName);
            logger.debug("Adding files in '{}' to the model", folderPath);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(folderPath,
                    new FileExtensionsFilter(validExtensions))) {
                stream.forEach(path -> checkPath(path, CREATE));
            } catch (IOException e) {
                logger.warn("Failed to list entries in directory: {}", folderPath.toAbsolutePath(), e);
            }

            for (String extension : validExtensions) {
                if (parsers.contains(extension) && !missingParsers.contains(extension)) {
                    readyService.markReady(new ReadyMarker(READYMARKER_TYPE, extension));
                    logger.debug("Marked extension '{}' as ready", extension);
                }
            }
        }

        logger.debug("Added {} model files and {} ignored paths remain", namePathMap.size(), ignoredPaths.size());
    }

    private void deleteModelsFromRepo() {
        for (String folder : folderFileExtMap.keySet()) {
            Iterable<String> models = modelRepository.getAllModelNamesOfType(folder);
            for (String model : models) {
                logger.debug("Removing file {} from the model repo.", model);
                modelRepository.removeModel(model);
            }
        }
    }

    protected static class FileExtensionsFilter implements DirectoryStream.Filter<Path> {
        private final Set<String> validExtensions;

        public FileExtensionsFilter(Set<String> validExtensions) {
            this.validExtensions = validExtensions;
        }

        @Override
        public boolean accept(Path entry) throws IOException {
            String extension = getExtension(entry);
            return extension != null && validExtensions.contains(extension);
        }
    }

    private void checkPath(final Path path, final WatchService.Kind kind) {
        try {
            if (Files.isHidden(path)) {
                // we omit parsing of hidden files possibly created by editors or operating systems
                if (logger.isDebugEnabled()) {
                    logger.debug("Omitting update of hidden file '{}'", path.toAbsolutePath());
                }
                return;
            }

            synchronized (FolderObserver.class) {
                String fileName = path.getFileName().toString();
                if (kind == CREATE || kind == MODIFY) {
                    String extension = getExtension(fileName);
                    if (parsers.contains(extension)) {
                        try (InputStream inputStream = Files.newInputStream(path)) {
                            namePathMap.put(fileName, path);
                            modelRepository.addOrRefreshModel(fileName, inputStream);
                            logger.debug("Added/refreshed '{}' model", fileName);
                        } catch (IOException e) {
                            logger.warn("Error while opening file during update: {}", path.toAbsolutePath());
                        }
                    } else if (extension != null) {
                        ignoredPaths.add(path);
                        if (!activated) {
                            missingParsers.add(extension);
                        }
                        if (logger.isDebugEnabled()) {
                            logger.debug("Missing parser for '{}' extension, added ignored path: {}", extension,
                                    path.toAbsolutePath());
                        }
                    }
                } else if (kind == WatchService.Kind.DELETE) {
                    modelRepository.removeModel(fileName);
                    namePathMap.remove(fileName);
                    logger.debug("Removed '{}' model ", fileName);
                }
            }
        } catch (Exception e) {
            logger.error("Error handling update of file '{}': {}.", path.toAbsolutePath(), e.getMessage(), e);
        }
    }

    private static @Nullable String getExtension(String fileName) {
        return fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".") + 1) : null;
    }

    private static @Nullable String getExtension(Path path) {
        return getExtension(path.getFileName().toString());
    }

    @Override
    public void processWatchEvent(WatchService.Kind kind, Path path) {
        if (path.getNameCount() != 2) {
            logger.trace("{} event for {} ignored (only depth 1 allowed)", kind, path);
            return;
        }

        String extension = getExtension(path);
        if (extension == null) {
            logger.trace("{} event for {} ignored (extension null)", kind, path);
            return;
        }

        String folderName = path.getName(0).toString();
        Set<String> validExtensions = folderFileExtMap.get(folderName);
        if (validExtensions == null) {
            logger.trace("{} event for {} ignored (folder '{}' extensions null)", kind, path, folderName);
            return;
        }
        if (!validExtensions.contains(extension)) {
            logger.trace("{} event for {} ignored ('{}' extension is invalid)", kind, path, extension);
            return;
        }

        Path resolvedPath = watchService.getWatchPath().resolve(path);
        checkPath(resolvedPath, kind);
    }
}
