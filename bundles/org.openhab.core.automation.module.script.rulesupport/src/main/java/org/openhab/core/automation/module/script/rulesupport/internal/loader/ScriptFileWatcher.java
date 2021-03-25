/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.script.ScriptEngine;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.OpenHAB;
import org.openhab.core.automation.module.script.ScriptEngineContainer;
import org.openhab.core.automation.module.script.ScriptEngineManager;
import org.openhab.core.common.NamedThreadFactory;
import org.openhab.core.service.AbstractWatchService;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyMarkerFilter;
import org.openhab.core.service.ReadyService;
import org.openhab.core.service.StartLevelService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ScriptFileWatcher} watches the jsr223 directory for files. If a new/modified file is detected, the script
 * is read and passed to the {@link ScriptEngineManager}.
 *
 * @author Simon Merschjohann - Initial contribution
 * @author Kai Kreuzer - improved logging and removed thread pool
 * @author Jonathan Gilbert - added dependency tracking & per-script start levels
 */
@Component(immediate = true)
public class ScriptFileWatcher extends AbstractWatchService
        implements ReadyService.ReadyTracker, DependencyTracker.DependencyChangeListener {

    private static final String FILE_DIRECTORY = "automation" + File.separator + "jsr223";
    private static final long RECHECK_INTERVAL = 20;

    private final Logger logger = LoggerFactory.getLogger(ScriptFileWatcher.class);

    private final ScriptEngineManager manager;
    private final DependencyTracker dependencyTracker;
    private final ReadyService readyService;

    private @Nullable ScheduledExecutorService scheduler;
    private Supplier<ScheduledExecutorService> executerFactory;

    private final Set<ScriptFileReference> pending = new HashSet<>();
    private final Set<ScriptFileReference> loaded = new HashSet<>();

    private volatile int currentStartLevel = 0;

    @Activate
    public ScriptFileWatcher(final @Reference ScriptEngineManager manager,
            final @Reference DependencyTracker dependencyTracker, final @Reference ReadyService readyService) {
        super(OpenHAB.getConfigFolder() + File.separator + FILE_DIRECTORY);
        this.manager = manager;
        this.dependencyTracker = dependencyTracker;
        this.readyService = readyService;
        this.executerFactory = () -> Executors
                .newSingleThreadScheduledExecutor(new NamedThreadFactory("scriptwatcher"));

        readyService.registerTracker(this, new ReadyMarkerFilter().withType(StartLevelService.STARTLEVEL_MARKER_TYPE));
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
     * Override the executor service. Can be used for testing.
     *
     * @param executerFactory supplier of ScheduledExecutorService
     */
    void setExecuterFactory(Supplier<ScheduledExecutorService> executerFactory) {
        this.executerFactory = executerFactory;
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
                    importFileWhenReady(new ScriptFileReference(url));
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
                    removeFile(new ScriptFileReference(fileUrl));
                }

                if (file.canRead() && (ENTRY_CREATE.equals(kind) || ENTRY_MODIFY.equals(kind))) {
                    importFileWhenReady(new ScriptFileReference(fileUrl));
                }
            } catch (MalformedURLException e) {
                logger.error("malformed", e);
            }
        }
    }

    private void removeFile(ScriptFileReference ref) {
        dequeueUrl(ref);
        String scriptIdentifier = ref.getScriptIdentifier();
        dependencyTracker.removeScript(scriptIdentifier);
        manager.removeEngine(scriptIdentifier);
        loaded.remove(ref);
    }

    private synchronized void importFileWhenReady(ScriptFileReference ref) {
        if (loaded.contains(ref)) {
            this.removeFile(ref); // if already loaded, remove first
        }

        Optional<String> scriptType = ref.getScriptType();

        scriptType.ifPresent(type -> {
            if (ref.getStartLevel() <= currentStartLevel && manager.isSupported(type)) {
                importFile(ref);
            } else {
                enqueue(ref);
            }
        });
    }

    private void importFile(ScriptFileReference ref) {
        String fileName = ref.getScriptFileURL().getFile();
        Optional<String> scriptType = ref.getScriptType();
        assert scriptType.isPresent();

        try (InputStreamReader reader = new InputStreamReader(
                new BufferedInputStream(ref.getScriptFileURL().openStream()), StandardCharsets.UTF_8)) {
            logger.info("Loading script '{}'", fileName);

            String scriptIdentifier = ref.getScriptIdentifier();
            ScriptEngineContainer container = manager.createScriptEngine(scriptType.get(), scriptIdentifier);

            if (container != null) {
                container.getScriptEngine().put(ScriptEngine.FILENAME, fileName);
                manager.loadScript(container.getIdentifier(), reader,
                        dependency -> dependencyTracker.addLibForScript(scriptIdentifier, dependency));

                loaded.add(ref);
                logger.debug("Script loaded: {}", fileName);
            } else {
                logger.error("Script loading error, ignoring file: {}", fileName);
            }
        } catch (IOException e) {
            logger.error("Failed to load file '{}': {}", ref.getScriptFileURL().getFile(), e.getMessage());
        }
    }

    private void enqueue(ScriptFileReference ref) {
        synchronized (pending) {
            pending.add(ref);
        }

        logger.debug("Enqueued {}", ref.getScriptIdentifier());
    }

    private void dequeueUrl(ScriptFileReference ref) {
        synchronized (pending) {
            pending.remove(ref);
        }

        logger.debug("Dequeued {}", ref.getScriptIdentifier());
    }

    private void checkFiles(int forLevel) {
        List<ScriptFileReference> newlySupported;

        synchronized (pending) {
            newlySupported = pending.stream()
                    .filter(ref -> manager.isSupported(ref.getScriptType().get()) && forLevel >= ref.getStartLevel())
                    .sorted().collect(Collectors.toList());
            pending.removeAll(newlySupported);
        }

        for (ScriptFileReference ref : newlySupported) {
            importFileWhenReady(ref);
        }
    }

    private synchronized void onStartLevelChanged(int newLevel) {
        int previousLevel = currentStartLevel;
        currentStartLevel = newLevel;

        if (previousLevel < StartLevelService.STARTLEVEL_MODEL) { // not yet started
            if (newLevel >= StartLevelService.STARTLEVEL_MODEL) { // start
                ScheduledExecutorService localScheduler = executerFactory.get();
                scheduler = localScheduler;
                localScheduler.submit(() -> importResources(new File(pathToWatch)));
                localScheduler.scheduleWithFixedDelay(() -> checkFiles(currentStartLevel), 0, RECHECK_INTERVAL,
                        TimeUnit.SECONDS);
            }
        } else { // already started
            assert scheduler != null;
            if (newLevel < StartLevelService.STARTLEVEL_MODEL) { // stop
                scheduler.shutdown();
                scheduler = null;
            } else if (newLevel > previousLevel) {
                scheduler.submit(() -> checkFiles(newLevel));
            }
        }
    }

    @Override
    public void onDependencyChange(String scriptPath) {
        logger.debug("Reimporting {}...", scriptPath);
        try {
            importFileWhenReady(new ScriptFileReference(new URL(scriptPath)));
        } catch (MalformedURLException e) {
            logger.warn("Failed to reimport {} as it cannot be parsed as a URL", scriptPath);
        }
    }

    @Override
    public synchronized void onReadyMarkerAdded(ReadyMarker readyMarker) {
        int newLevel = Integer.parseInt(readyMarker.getIdentifier());

        if (newLevel > currentStartLevel) {
            onStartLevelChanged(newLevel);
        }
    }

    @Override
    public synchronized void onReadyMarkerRemoved(ReadyMarker readyMarker) {
        int newLevel = Integer.parseInt(readyMarker.getIdentifier());

        if (currentStartLevel > newLevel) {
            while (newLevel-- > 0 && !readyService
                    .isReady(new ReadyMarker(StartLevelService.STARTLEVEL_MARKER_TYPE, Integer.toString(newLevel)))) {
            }
            onStartLevelChanged(newLevel);
        }
    }
}
