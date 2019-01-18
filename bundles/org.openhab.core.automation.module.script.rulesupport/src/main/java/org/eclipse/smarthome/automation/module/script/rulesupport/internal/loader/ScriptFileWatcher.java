/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.automation.module.script.rulesupport.internal.loader;

import static java.nio.file.StandardWatchEventKinds.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.automation.module.script.ScriptEngineContainer;
import org.eclipse.smarthome.automation.module.script.ScriptEngineManager;
import org.eclipse.smarthome.config.core.ConfigConstants;
import org.eclipse.smarthome.core.service.AbstractWatchService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link ScriptFileWatcher} watches the jsr223 directory for files. If a new/modified file is detected, the script
 * is read and passed to the {@link ScriptEngineManager}.
 *
 * @author Simon Merschjohann - initial contribution
 * @author Kai Kreuzer - improved logging and removed thread pool
 *
 */
@Component(immediate = true)
public class ScriptFileWatcher extends AbstractWatchService {
    private static final String FILE_DIRECTORY = "automation" + File.separator + "jsr223";
    private static final long INITIAL_DELAY = 25;
    private static final long RECHECK_INTERVAL = 20;

    private final long earliestStart = System.currentTimeMillis() + INITIAL_DELAY * 1000;

    private ScriptEngineManager manager;
    ScheduledExecutorService scheduler;

    private final Map<String, Set<URL>> urlsByScriptExtension = new ConcurrentHashMap<>();
    private final Set<URL> loaded = new HashSet<>();

    public ScriptFileWatcher() {
        super(ConfigConstants.getConfigFolder() + File.separator + FILE_DIRECTORY);
    }

    @Reference
    public void setScriptEngineManager(ScriptEngineManager manager) {
        this.manager = manager;
    }

    public void unsetScriptEngineManager(ScriptEngineManager manager) {
        this.manager = null;
    }

    @Override
    public void activate() {
        super.activate();
        importResources(new File(pathToWatch));
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(this::checkFiles, INITIAL_DELAY, RECHECK_INTERVAL, TimeUnit.SECONDS);
    }

    @Override
    public void deactivate() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        super.deactivate();
    }

    /**
     * Imports resources from the specified file or directory.
     *
     * @param file the file or directory to import resources from
     */
    private void importResources(File file) {
        if (file.exists()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (!f.isHidden()) {
                        importResources(f);
                    }
                }
            } else {
                try {
                    URL url = file.toURI().toURL();
                    importFile(url);
                } catch (MalformedURLException e) {
                    // can't happen for the 'file' protocol handler with a correctly formatted URI
                    logger.debug("Can't create a URL", e);
                }
            }
        }
    }

    @Override
    protected boolean watchSubDirectories() {
        return true;
    }

    @Override
    protected Kind<?>[] getWatchEventKinds(Path subDir) {
        return new Kind<?>[] { ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY };
    }

    @Override
    protected void processWatchEvent(WatchEvent<?> event, Kind<?> kind, Path path) {
        File file = path.toFile();
        if (!file.isHidden()) {
            try {
                URL fileUrl = file.toURI().toURL();
                if (kind.equals(ENTRY_DELETE)) {
                    this.removeFile(fileUrl);
                }

                if (file.canRead() && (kind.equals(ENTRY_CREATE) || kind.equals(ENTRY_MODIFY))) {
                    this.importFile(fileUrl);
                }
            } catch (MalformedURLException e) {
                logger.error("malformed", e);
            }
        }
    }

    private void removeFile(URL url) {
        dequeueUrl(url);
        manager.removeEngine(getScriptIdentifier(url));
        loaded.remove(url);
    }

    private synchronized void importFile(URL url) {
        String fileName = getFileName(url);
        if (loaded.contains(url)) {
            this.removeFile(url); // if already loaded, remove first
        }

        String scriptType = getScriptType(url);
        if (scriptType != null) {
            if (System.currentTimeMillis() < earliestStart) {
                enqueueUrl(url, scriptType);
            } else {
                if (manager.isSupported(scriptType)) {
                    try (InputStreamReader reader = new InputStreamReader(new BufferedInputStream(url.openStream()))) {
                        logger.info("Loading script '{}'", fileName);

                        ScriptEngineContainer container = manager.createScriptEngine(scriptType,
                                getScriptIdentifier(url));

                        if (container != null) {
                            manager.loadScript(container.getIdentifier(), reader);
                            loaded.add(url);
                            logger.debug("Script loaded: {}", fileName);
                        } else {
                            logger.error("Script loading error, ignoring file: {}", fileName);
                        }
                    } catch (IOException e) {
                        logger.error("Failed to load file '{}': {}", url.getFile(), e.getMessage());
                    }
                } else {
                    enqueueUrl(url, scriptType);

                    logger.info("ScriptEngine for {} not available", scriptType);
                }
            }
        }
    }

    private String getFileName(URL url) {
        String fileName = url.getFile();
        String parentPath = FILE_DIRECTORY.replace('\\', '/');
        if (fileName.contains(parentPath)) {
            fileName = fileName.substring(fileName.lastIndexOf(parentPath) + parentPath.length() + 1);
        }
        return fileName;
    }

    private void enqueueUrl(URL url, String scriptType) {
        synchronized (urlsByScriptExtension) {
            Set<URL> set = urlsByScriptExtension.get(scriptType);
            if (set == null) {
                set = new HashSet<URL>();
                urlsByScriptExtension.put(scriptType, set);
            }
            set.add(url);
            logger.debug("in queue: {}", urlsByScriptExtension);
        }
    }

    private void dequeueUrl(URL url) {
        String scriptType = getScriptType(url);

        if (scriptType != null) {
            synchronized (urlsByScriptExtension) {
                Set<URL> set = urlsByScriptExtension.get(scriptType);
                if (set != null) {
                    set.remove(url);
                    if (set.isEmpty()) {
                        urlsByScriptExtension.remove(scriptType);
                    }
                }
                logger.debug("in queue: {}", urlsByScriptExtension);
            }
        }
    }

    private String getScriptType(URL url) {
        String fileName = url.getPath();
        int idx = fileName.lastIndexOf(".");
        if (idx == -1) {
            return null;
        }
        String fileExtension = fileName.substring(idx + 1);

        // ignore known file extensions for "temp" files
        if (fileExtension.equals("txt") || fileExtension.endsWith("~") || fileExtension.endsWith("swp")) {
            return null;
        }
        return fileExtension;
    }

    private String getScriptIdentifier(URL url) {
        return url.toString();
    }

    private void checkFiles() {
        SortedSet<URL> reimportUrls = new TreeSet<URL>(new Comparator<URL>() {
            @Override
            public int compare(URL o1, URL o2) {
                String f1 = o1.getPath();
                String f2 = o2.getPath();
                return String.CASE_INSENSITIVE_ORDER.compare(f1, f2);
            }
        });

        synchronized (urlsByScriptExtension) {
            HashSet<String> newlySupported = new HashSet<>();
            for (String key : urlsByScriptExtension.keySet()) {
                if (manager.isSupported(key)) {
                    newlySupported.add(key);
                }
            }

            for (String key : newlySupported) {
                reimportUrls.addAll(urlsByScriptExtension.remove(key));
            }
        }

        for (URL url : reimportUrls) {
            importFile(url);
        }
    }
}
