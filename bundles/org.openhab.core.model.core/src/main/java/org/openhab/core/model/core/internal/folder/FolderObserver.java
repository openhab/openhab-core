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

import static org.openhab.core.service.WatchService.Kind.CREATE;
import static org.openhab.core.service.WatchService.Kind.MODIFY;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
    private final Map<String, List<String>> folderFileExtMap = new ConcurrentHashMap<>();

    /* set of file extensions for which we have parsers already registered */
    private final Set<String> parsers = new HashSet<>();

    /* set of files that have been ignored due to a missing parser */
    private final Set<File> ignoredFiles = new HashSet<>();
    private final Map<String, File> nameFileMap = new HashMap<>();

    @Activate
    public FolderObserver(final @Reference ModelRepository modelRepo, final @Reference ReadyService readyService,
            final @Reference(target = WatchService.CONFIG_WATCHER_FILTER) WatchService watchService) {
        this.modelRepository = modelRepo;
        this.readyService = readyService;
        this.watchService = watchService;
    }

    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC)
    protected void addModelParser(ModelParser modelParser) {
        parsers.add(modelParser.getExtension());

        if (activated) {
            processIgnoredFiles(modelParser.getExtension());
            readyService.markReady(new ReadyMarker(READYMARKER_TYPE, modelParser.getExtension()));
        }
    }

    protected void removeModelParser(ModelParser modelParser) {
        parsers.remove(modelParser.getExtension());

        Set<String> removed = modelRepository.removeAllModelsOfType(modelParser.getExtension());
        ignoredFiles.addAll(removed.stream().map(nameFileMap::get).collect(Collectors.toSet()));
    }

    @Activate
    public void activate(ComponentContext ctx) {
        Dictionary<String, Object> config = ctx.getProperties();

        Enumeration<String> keys = config.keys();
        while (keys.hasMoreElements()) {
            String foldername = keys.nextElement();
            if (!foldername.matches("[A-Za-z0-9_]*")) {
                // we allow only simple alphanumeric names for model folders - everything else might be other service
                // properties
                continue;
            }

            String[] fileExts = ((String) config.get(foldername)).split(",");

            File folder = watchService.getWatchPath().resolve(foldername).toFile();
            if (folder.exists() && folder.isDirectory()) {
                folderFileExtMap.put(foldername, Arrays.asList(fileExts));
            } else {
                logger.warn("Directory '{}' does not exist in '{}'. Please check your configuration settings!",
                        foldername, OpenHAB.getConfigFolder());
            }
        }

        watchService.registerListener(this, Path.of(""));
        addModelsToRepo();
        this.activated = true;
    }

    @Deactivate
    public void deactivate() {
        watchService.unregisterListener(this);
        this.activated = false;
        deleteModelsFromRepo();
        this.ignoredFiles.clear();
        this.folderFileExtMap.clear();
        this.parsers.clear();
        this.nameFileMap.clear();
    }

    private void processIgnoredFiles(String extension) {
        Set<File> clonedSet = new HashSet<>(this.ignoredFiles);
        for (File file : clonedSet) {
            if (extension.equals(getExtension(file.getPath()))) {
                checkFile(modelRepository, file, CREATE);
                this.ignoredFiles.remove(file);
            }
        }
    }

    private void addModelsToRepo() {
        if (!folderFileExtMap.isEmpty()) {
            for (String folderName : folderFileExtMap.keySet()) {
                final List<String> validExtension = folderFileExtMap.get(folderName);
                if (validExtension != null && validExtension.size() > 0) {
                    File folder = watchService.getWatchPath().resolve(folderName).toFile();

                    File[] files = folder.listFiles(new FileExtensionsFilter(validExtension));
                    if (files != null) {
                        for (File file : files) {
                            // we omit parsing of hidden files possibly created by editors or operating systems
                            if (!file.isHidden()) {
                                checkFile(modelRepository, file, CREATE);
                            }
                        }
                    }
                    for (String ext : validExtension) {
                        readyService.markReady(new ReadyMarker(READYMARKER_TYPE, ext));
                    }
                }
            }
        }
    }

    private void deleteModelsFromRepo() {
        Set<String> folders = folderFileExtMap.keySet();
        for (String folder : folders) {
            Iterable<String> models = modelRepository.getAllModelNamesOfType(folder);
            for (String model : models) {
                logger.debug("Removing file {} from the model repo.", model);
                modelRepository.removeModel(model);
            }
        }
    }

    protected static class FileExtensionsFilter implements FilenameFilter {

        private final List<String> validExtensions;

        public FileExtensionsFilter(List<String> validExtensions) {
            this.validExtensions = validExtensions;
        }

        @Override
        public boolean accept(@NonNullByDefault({}) File dir, @NonNullByDefault({}) String name) {
            for (String extension : validExtensions) {
                if (name.toLowerCase().endsWith("." + extension)) {
                    return true;
                }
            }
            return false;
        }
    }

    private void checkFile(final ModelRepository modelRepository, final File file, final WatchService.Kind kind) {
        try {
            synchronized (FolderObserver.class) {
                if ((kind == CREATE || kind == MODIFY)) {
                    if (parsers.contains(getExtension(file.getName()))) {
                        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
                            nameFileMap.put(file.getName(), file);
                            modelRepository.addOrRefreshModel(file.getName(), inputStream);
                        } catch (IOException e) {
                            logger.warn("Error while opening file during update: {}", file.getAbsolutePath());
                        }
                    } else {
                        ignoredFiles.add(file);
                    }
                } else if (kind == WatchService.Kind.DELETE) {
                    modelRepository.removeModel(file.getName());
                    nameFileMap.remove(file.getName());
                }
            }
        } catch (Exception e) {
            logger.error("Error handling update of file '{}': {}.", file.getAbsolutePath(), e.getMessage(), e);
        }
    }

    /**
     * Returns the extension of the given file
     *
     * @param filename
     *            the file name to get the extension
     * @return the file's extension
     */
    public @Nullable String getExtension(String filename) {
        if (filename.contains(".")) {
            return filename.substring(filename.lastIndexOf(".") + 1);
        } else {
            return null;
        }
    }

    @Override
    public void processWatchEvent(WatchService.Kind kind, Path path) {
        if (path.getNameCount() != 2) {
            logger.trace("{} event for {} ignored, only depth 1 is allowed.", kind, path);
            return;
        }

        String fileExtension = getExtension(path.getFileName().toString());
        List<String> validExtensions = folderFileExtMap.get(path.getName(0).toString());
        if (fileExtension != null && validExtensions != null && validExtensions.contains(fileExtension)) {
            File toCheck = watchService.getWatchPath().resolve(path).toFile();
            if (!toCheck.isHidden()) {
                checkFile(modelRepository, toCheck, kind);
            }
        }
    }
}
