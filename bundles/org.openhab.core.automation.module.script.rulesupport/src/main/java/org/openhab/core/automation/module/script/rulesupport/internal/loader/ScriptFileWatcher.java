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
package org.openhab.core.automation.module.script.rulesupport.internal.loader;

import static java.nio.file.StandardWatchEventKinds.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.OpenHAB;
import org.openhab.core.automation.module.script.ScriptEngineContainer;
import org.openhab.core.automation.module.script.ScriptEngineManager;
import org.openhab.core.common.NamedThreadFactory;
import org.openhab.core.service.AbstractWatchService;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyMarkerFilter;
import org.openhab.core.service.ReadyService;
import org.openhab.core.service.ReadyService.ReadyTracker;
import org.openhab.core.service.StartLevelService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link ScriptFileWatcher} watches the jsr223 directory for files. If a new/modified file is detected, the script
 * is read and passed to the {@link ScriptEngineManager}.
 *
 * @author Simon Merschjohann - Initial contribution
 * @author Kai Kreuzer - improved logging and removed thread pool
 */
@Component(immediate = true)
public class ScriptFileWatcher extends AbstractWatchService implements ReadyTracker {

    private static final Set<String> EXCLUDED_FILE_EXTENSIONS = new HashSet<>(
            Arrays.asList("txt", "old", "example", "backup", "md", "swp", "tmp", "bak"));
    private static final String FILE_DIRECTORY = "automation" + File.separator + "jsr223";
    private static final long RECHECK_INTERVAL = 20;

    private boolean started = false;

    private final ScriptEngineManager manager;
    private final ReadyService readyService;
    private @Nullable ScheduledExecutorService scheduler;

    private final Map<String, Set<URL>> urlsByScriptExtension = new ConcurrentHashMap<>();
    private final Set<URL> loaded = new HashSet<>();

    @Activate
    public ScriptFileWatcher(final @Reference ScriptEngineManager manager, final @Reference ReadyService readyService) {
        super(OpenHAB.getConfigFolder() + File.separator + FILE_DIRECTORY);
        this.manager = manager;
        this.readyService = readyService;
    }

    @Activate
    @Override
    public void activate() {
        super.activate();
        readyService.registerTracker(this, new ReadyMarkerFilter().withType(StartLevelService.STARTLEVEL_MARKER_TYPE)
                .withIdentifier(Integer.toString(StartLevelService.STARTLEVEL_MODEL)));
    }

    @Deactivate
    @Override
    public void deactivate() {
        readyService.unregisterTracker(this);
        ScheduledExecutorService localScheduler = scheduler;
        if (localScheduler != null) {
            localScheduler.shutdownNow();
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
                if (ENTRY_DELETE.equals(kind)) {
                    removeFile(fileUrl);
                }

                if (file.canRead() && (ENTRY_CREATE.equals(kind) || ENTRY_MODIFY.equals(kind))) {
                    importFile(fileUrl);
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
        String fileName = url.getFile();
        if (loaded.contains(url)) {
            this.removeFile(url); // if already loaded, remove first
        }

        String scriptType = getScriptType(url);
        if (scriptType != null) {
            if (!started) {
                enqueueUrl(url, scriptType);
            } else {
                if (manager.isSupported(scriptType)) {
                    try (InputStreamReader reader = new InputStreamReader(new BufferedInputStream(url.openStream()),
                            StandardCharsets.UTF_8)) {
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

    private void enqueueUrl(URL url, String scriptType) {
        synchronized (urlsByScriptExtension) {
            Set<URL> set = urlsByScriptExtension.get(scriptType);
            if (set == null) {
                set = new HashSet<>();
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

    private @Nullable String getScriptType(URL url) {
        String fileName = url.getPath();
        int index = fileName.lastIndexOf(".");
        if (index == -1) {
            return null;
        }
        String fileExtension = fileName.substring(index + 1);

        // ignore known file extensions for "temp" files
        if (EXCLUDED_FILE_EXTENSIONS.contains(fileExtension) || fileExtension.endsWith("~")) {
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
                try {
                    Path path1 = Paths.get(o1.toURI());
                    String name1 = path1.getFileName().toString();
                    logger.trace("o1 [{}], path1 [{}], name1 [{}]", o1, path1, name1);

                    Path path2 = Paths.get(o2.toURI());
                    String name2 = path2.getFileName().toString();
                    logger.trace("o2 [{}], path2 [{}], name2 [{}]", o2, path2, name2);

                    int nameCompare = name1.compareToIgnoreCase(name2);
                    if (nameCompare != 0) {
                        return nameCompare;
                    } else {
                        int pathCompare = path1.getParent().toString()
                                .compareToIgnoreCase(path2.getParent().toString());
                        return pathCompare;
                    }
                } catch (URISyntaxException e) {
                    logger.error("URI syntax exception", e);
                    return 0;
                }
            }
        });

        synchronized (urlsByScriptExtension) {
            Set<String> newlySupported = new HashSet<>();
            for (String key : urlsByScriptExtension.keySet()) {
                if (manager.isSupported(key)) {
                    newlySupported.add(key);
                }
            }

            for (String key : newlySupported) {
                reimportUrls.addAll(Objects.requireNonNullElse(urlsByScriptExtension.remove(key), Set.of()));
            }
        }

        for (URL url : reimportUrls) {
            importFile(url);
        }
    }

    @Override
    public void onReadyMarkerAdded(@NonNull ReadyMarker readyMarker) {
        started = true;

        ScheduledExecutorService localScheduler = Executors
                .newSingleThreadScheduledExecutor(new NamedThreadFactory("scriptwatcher"));
        scheduler = localScheduler;
        localScheduler.submit(() -> importResources(new File(pathToWatch)));
        localScheduler.scheduleWithFixedDelay(this::checkFiles, RECHECK_INTERVAL, RECHECK_INTERVAL, TimeUnit.SECONDS);
    }

    @Override
    public void onReadyMarkerRemoved(@NonNull ReadyMarker readyMarker) {
        started = false;
    }
}
