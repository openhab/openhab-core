/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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

import static java.nio.file.StandardWatchEventKinds.*;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.openhab.core.config.core.ConfigConstants;
import org.openhab.core.model.core.ModelParser;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.service.AbstractWatchService;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * This class is able to observe multiple folders for changes and notifies the
 * model repository about every change, so that it can update itself.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Fabio Marini - Refactoring to use WatchService
 * @author Ana Dimova - reduce to a single watch thread for all class instances
 */
@Component(name = "org.openhab.core.folder", immediate = true, configurationPid = "org.openhab.folder", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class FolderObserver extends AbstractWatchService {

    /* the model repository is provided as a service */
    private final ModelRepository modelRepository;
    private static final String READYMARKER_TYPE = "dsl";

    private final ReadyService readyService;

    private boolean activated;

    /* map that stores a list of valid file extensions for each folder */
    private final Map<String, String[]> folderFileExtMap = new ConcurrentHashMap<>();

    /* set of file extensions for which we have parsers already registered */
    private final Set<String> parsers = new HashSet<>();

    /* set of files that have been ignored due to a missing parser */
    private final Set<File> ignoredFiles = new HashSet<>();
    private final Map<String, File> nameFileMap = new HashMap<>();

    @Activate
    public FolderObserver(final @Reference ModelRepository modelRepo, final @Reference ReadyService readyService) {
        super(ConfigConstants.getConfigFolder());

        this.modelRepository = modelRepo;
        this.readyService = readyService;
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
        ignoredFiles.addAll(removed.stream().map(name -> nameFileMap.get(name)).collect(Collectors.toSet()));
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

            File folder = getFile(foldername);
            if (folder.exists() && folder.isDirectory()) {
                folderFileExtMap.put(foldername, fileExts);
            } else {
                logger.warn("Directory '{}' does not exist in '{}'. Please check your configuration settings!",
                        foldername, ConfigConstants.getConfigFolder());
            }
        }

        addModelsToRepo();
        super.activate();
        this.activated = true;
    }

    @Override
    @Deactivate
    public void deactivate() {
        this.activated = false;
        super.deactivate();
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
                checkFile(modelRepository, file, ENTRY_CREATE);
                this.ignoredFiles.remove(file);
            }
        }
    }

    @Override
    protected boolean watchSubDirectories() {
        return true;
    }

    @Override
    protected Kind<?>[] getWatchEventKinds(Path directory) {
        if (directory != null && isNotEmpty(folderFileExtMap)) {
            String folderName = directory.getFileName().toString();
            if (folderFileExtMap.containsKey(folderName)) {
                return new Kind<?>[] { ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY };
            }
        }
        return null;
    }

    private boolean isEmpty(final Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    private boolean isNotEmpty(final Map<?, ?> map) {
        return !isEmpty(map);
    }

    private void addModelsToRepo() {
        if (isNotEmpty(folderFileExtMap)) {
            Iterator<String> iterator = folderFileExtMap.keySet().iterator();
            while (iterator.hasNext()) {
                String folderName = iterator.next();

                final String[] validExtension = folderFileExtMap.get(folderName);
                if (validExtension != null && validExtension.length > 0) {
                    File folder = getFile(folderName);

                    File[] files = folder.listFiles(new FileExtensionsFilter(validExtension));
                    if (files != null && files.length > 0) {
                        for (File file : files) {
                            // we omit parsing of hidden files possibly created by editors or operating systems
                            if (!file.isHidden()) {
                                checkFile(modelRepository, file, ENTRY_CREATE);
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

    protected class FileExtensionsFilter implements FilenameFilter {

        private final String[] validExtensions;

        public FileExtensionsFilter(String[] validExtensions) {
            this.validExtensions = validExtensions;
        }

        @Override
        public boolean accept(File dir, String name) {
            if (validExtensions != null && validExtensions.length > 0) {
                for (String extension : validExtensions) {
                    if (name.toLowerCase().endsWith("." + extension)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    @SuppressWarnings("rawtypes")
    private void checkFile(final ModelRepository modelRepository, final File file, final Kind kind) {
        if (file != null) {
            try {
                synchronized (FolderObserver.class) {
                    if ((kind == ENTRY_CREATE || kind == ENTRY_MODIFY)) {
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
                    } else if (kind == ENTRY_DELETE) {
                        modelRepository.removeModel(file.getName());
                        nameFileMap.remove(file.getName());
                    }
                }
            } catch (Exception e) {
                logger.error("Error handling update of file '{}': {}.", file.getAbsolutePath(), e.getMessage(), e);
            }
        }
    }

    private File getFileByFileExtMap(Map<String, String[]> folderFileExtMap, String filename) {
        if (filename != null && !filename.trim().isEmpty() && isNotEmpty(folderFileExtMap)) {
            String extension = getExtension(filename);
            if (extension != null && !extension.trim().isEmpty()) {
                Set<Entry<String, String[]>> entries = folderFileExtMap.entrySet();
                Iterator<Entry<String, String[]>> iterator = entries.iterator();
                while (iterator.hasNext()) {
                    Entry<String, String[]> entry = iterator.next();

                    if (Arrays.stream(entry.getValue()).anyMatch(extension::equals)) {
                        return new File(getFile(entry.getKey()) + File.separator + filename);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the {@link File} object for the given filename. <br />
     * It must be contained in the configuration folder
     *
     * @param filename
     *            the file name to get the {@link File} for
     * @return the corresponding {@link File}
     */
    private File getFile(String filename) {
        return new File(ConfigConstants.getConfigFolder() + File.separator + filename);
    }

    /**
     * Returns the extension of the given file
     *
     * @param filename
     *            the file name to get the extension
     * @return the file's extension
     */
    public String getExtension(String filename) {
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    @Override
    protected void processWatchEvent(WatchEvent<?> event, Kind<?> kind, Path path) {
        File toCheck = getFileByFileExtMap(folderFileExtMap, path.getFileName().toString());
        if (toCheck != null && !toCheck.isHidden()) {
            checkFile(modelRepository, toCheck, kind);
        }
    }
}
