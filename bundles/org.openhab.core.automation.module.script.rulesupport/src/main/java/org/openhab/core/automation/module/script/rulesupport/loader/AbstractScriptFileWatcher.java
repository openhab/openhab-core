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
package org.openhab.core.automation.module.script.rulesupport.loader;

import static org.openhab.core.service.WatchService.Kind.CREATE;
import static org.openhab.core.service.WatchService.Kind.DELETE;
import static org.openhab.core.service.WatchService.Kind.MODIFY;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.script.ScriptEngine;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.ScriptDependencyTracker;
import org.openhab.core.automation.module.script.ScriptEngineContainer;
import org.openhab.core.automation.module.script.ScriptEngineManager;
import org.openhab.core.automation.module.script.rulesupport.internal.loader.ScriptFileReference;
import org.openhab.core.common.NamedThreadFactory;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyMarkerFilter;
import org.openhab.core.service.ReadyService;
import org.openhab.core.service.StartLevelService;
import org.openhab.core.service.WatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AbstractScriptFileWatcher} is default implementation for watching a directory for files. If a new/modified
 * file is detected, the script is read and passed to the {@link ScriptEngineManager}. It needs to be sub-classed for
 * actual use.
 *
 * @author Simon Merschjohann - Initial contribution
 * @author Kai Kreuzer - improved logging and removed thread pool
 * @author Jonathan Gilbert - added dependency tracking & per-script start levels; made reusable
 * @author Jan N. Klug - Refactored dependency tracking to script engine factories
 */
@NonNullByDefault
public abstract class AbstractScriptFileWatcher implements WatchService.WatchEventListener, ReadyService.ReadyTracker,
        ScriptDependencyTracker.Listener, ScriptEngineManager.FactoryChangeListener, ScriptFileWatcher {

    private static final Set<String> EXCLUDED_FILE_EXTENSIONS = Set.of("txt", "old", "example", "backup", "md", "swp",
            "tmp", "bak");
    private static final String REGEX_SEPARATOR = "\\".equals(File.separator) ? "\\\\" : File.separator;

    private static final List<Pattern> START_LEVEL_PATTERNS = List.of( //
            // script in immediate slXX directory
            Pattern.compile(".*" + REGEX_SEPARATOR + "sl(\\d{2})" + REGEX_SEPARATOR + "[^" + REGEX_SEPARATOR + "]+"),
            // script named <name>.slXX.<ext>
            Pattern.compile(".*" + REGEX_SEPARATOR + "[^" + REGEX_SEPARATOR + "]+\\.sl(\\d{2})\\.[^" + REGEX_SEPARATOR
                    + ".]+"));

    private final Logger logger = LoggerFactory.getLogger(AbstractScriptFileWatcher.class);

    private final ScriptEngineManager manager;
    private final ReadyService readyService;
    private final WatchService watchService;
    private final Path watchPath;
    private final boolean watchSubDirectories;

    protected ScheduledExecutorService scheduler;

    private final Map<String, ScriptFileReference> scriptMap = new ConcurrentHashMap<>();
    private final Map<String, Lock> scriptLockMap = new ConcurrentHashMap<>();
    private final CompletableFuture<@Nullable Void> initialized = new CompletableFuture<>();

    private volatile int currentStartLevel;

    public AbstractScriptFileWatcher(final WatchService watchService, final ScriptEngineManager manager,
            final ReadyService readyService, final StartLevelService startLevelService, final String fileDirectory,
            boolean watchSubDirectories) {
        this.watchService = watchService;
        this.manager = manager;
        this.readyService = readyService;
        this.watchSubDirectories = watchSubDirectories;
        this.watchPath = watchService.getWatchPath().resolve(fileDirectory);
        this.scheduler = getScheduler();
        // Start with the lowest level to ensure the code in onReadyMarkerAdded runs
        this.currentStartLevel = StartLevelService.STARTLEVEL_OSGI;
    }

    /**
     * Get the base path that is used by this {@link ScriptFileWatcher}
     *
     * @return the {@link Path} used
     */
    protected Path getWatchPath() {
        return watchPath;
    }

    /**
     * Can be overridden by subclasses (e.g. for testing)
     *
     * @return a {@link ScheduledExecutorService}
     */
    protected ScheduledExecutorService getScheduler() {
        return Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("scriptwatcher"));
    }

    public void activate() {
        if (!Files.exists(watchPath)) {
            try {
                Files.createDirectories(watchPath);
            } catch (IOException e) {
                logger.warn("Failed to create watched directory: {}", watchPath);
            }
        } else if (!Files.isDirectory(watchPath)) {
            logger.warn("Trying to watch directory {}, however it is a file", watchPath);
        }

        manager.addFactoryChangeListener(this);
        readyService.registerTracker(this, new ReadyMarkerFilter().withType(StartLevelService.STARTLEVEL_MARKER_TYPE));
        watchService.registerListener(this, watchPath, watchSubDirectories);
    }

    public void deactivate() {
        watchService.unregisterListener(this);
        manager.removeFactoryChangeListener(this);
        readyService.unregisterTracker(this);

        CompletableFuture.allOf(
                Set.copyOf(scriptMap.keySet()).stream().map(this::removeFile).toArray(CompletableFuture<?>[]::new))
                .thenRun(scheduler::shutdownNow);
    }

    @Override
    public CompletableFuture<@Nullable Void> ifInitialized() {
        return initialized;
    }

    /**
     * Get the scriptType (file-extension or MIME-type) for a given file
     * <p />
     * The scriptType is determined by the file extension. The extensions in {@link #EXCLUDED_FILE_EXTENSIONS} are
     * ignored. Implementations should override this
     * method if they provide a MIME-type instead of the file extension.
     *
     * @param scriptFilePath the {@link Path} to the script
     * @return an {@link Optional<String>} containing the script type
     */
    protected Optional<String> getScriptType(Path scriptFilePath) {
        String fileName = scriptFilePath.toString();
        int index = fileName.lastIndexOf(".");
        if (index == -1) {
            return Optional.empty();
        }
        String fileExtension = fileName.substring(index + 1);

        // ignore known file extensions for "temp" files
        if (EXCLUDED_FILE_EXTENSIONS.contains(fileExtension) || fileExtension.endsWith("~")) {
            return Optional.empty();
        }
        return Optional.of(fileExtension);
    }

    /**
     * Gets the individual start level for a given file
     * <p />
     * The start level is derived from the name and location of
     * the file by {@link #START_LEVEL_PATTERNS}. If no match is found, {@link StartLevelService#STARTLEVEL_RULEENGINE}
     * is used.
     *
     * @param scriptFilePath the {@link Path} to the script
     * @return the start level for this script
     */
    protected int getStartLevel(Path scriptFilePath) {
        for (Pattern p : START_LEVEL_PATTERNS) {
            Matcher m = p.matcher(scriptFilePath.toString());
            if (m.find() && m.groupCount() > 0) {
                try {
                    return Integer.parseInt(m.group(1));
                } catch (NumberFormatException nfe) {
                    logger.warn("Extracted start level {} from {}, but it's not an integer. Ignoring.", m.group(1),
                            scriptFilePath);
                }
            }
        }

        return StartLevelService.STARTLEVEL_RULEENGINE;
    }

    private List<Path> listFiles(Path path, boolean includeSubDirectory) {
        try (Stream<Path> stream = Files.walk(path, includeSubDirectory ? Integer.MAX_VALUE : 1)) {
            return stream.filter(file -> !Files.isDirectory(file)).toList();
        } catch (IOException ignored) {
        }
        return List.of();
    }

    @Override
    public void processWatchEvent(WatchService.Kind kind, Path path) {
        if (!initialized.isDone()) {
            // discard events if the initial import has not finished
            return;
        }

        Path fullPath = watchPath.resolve(path);
        File file = fullPath.toFile();

        // Subdirectory events are filtered out by WatchService, so we only need to deal with files
        if (kind == DELETE) {
            String scriptIdentifier = ScriptFileReference.getScriptIdentifier(fullPath);
            if (scriptMap.containsKey(scriptIdentifier)) {
                removeFile(scriptIdentifier);
            }
        } else if (!file.isHidden() && file.canRead() && (kind == CREATE || kind == MODIFY)) {
            addFiles(listFiles(fullPath, watchSubDirectories));
        }
    }

    private CompletableFuture<Void> addFiles(Collection<Path> files) {
        return CompletableFuture.allOf(files.stream().map(this::getScriptFileReference).filter(Optional::isPresent)
                .map(Optional::get).sorted().map(this::addScriptFileReference).toArray(CompletableFuture<?>[]::new));
    }

    private CompletableFuture<Void> addScriptFileReference(ScriptFileReference newRef) {
        ScriptFileReference ref = scriptMap.computeIfAbsent(newRef.getScriptIdentifier(), k -> newRef);
        // check if we are ready to load the script, otherwise we don't need to queue it
        if (currentStartLevel >= ref.getStartLevel() && !ref.getQueueStatus().getAndSet(true)) {
            return importFileWhenReady(ref.getScriptIdentifier());
        }
        return CompletableFuture.completedFuture(null);
    }

    private Optional<ScriptFileReference> getScriptFileReference(Path path) {
        return getScriptType(path).map(scriptType -> new ScriptFileReference(path, scriptType, getStartLevel(path)));
    }

    private CompletableFuture<Void> removeFile(String scriptIdentifier) {
        return CompletableFuture.runAsync(() -> {
            Lock lock = getLockForScript(scriptIdentifier);
            try {
                scriptMap.computeIfPresent(scriptIdentifier, (id, ref) -> {
                    if (ref.getLoadedStatus().get()) {
                        manager.removeEngine(scriptIdentifier);
                        logger.debug("Unloaded script '{}'", scriptIdentifier);
                    } else {
                        logger.debug("Dequeued script '{}'", scriptIdentifier);
                    }

                    return null;
                });
            } finally {
                if (scriptMap.containsKey(scriptIdentifier)) {
                    logger.warn("Failed to unload script '{}'", scriptIdentifier);
                }

                scriptLockMap.remove(scriptIdentifier);
                lock.unlock();
            }
        }, scheduler);
    }

    private synchronized Lock getLockForScript(String scriptIdentifier) {
        Lock lock = scriptLockMap.computeIfAbsent(scriptIdentifier, k -> new ReentrantLock());
        lock.lock();

        return lock;
    }

    private CompletableFuture<Void> importFileWhenReady(String scriptIdentifier) {
        return CompletableFuture.runAsync(() -> {
            ScriptFileReference ref = scriptMap.get(scriptIdentifier);
            if (ref == null) {
                logger.warn("Failed to import script '{}': script reference not found", scriptIdentifier);
                return;
            }

            ref.getQueueStatus().set(false);

            Lock lock = getLockForScript(scriptIdentifier);
            try {
                if (ref.getLoadedStatus().get()) {
                    manager.removeEngine(scriptIdentifier);
                    logger.debug("Unloaded script '{}'", scriptIdentifier);
                }

                if (manager.isSupported(ref.getScriptType()) && ref.getStartLevel() <= currentStartLevel) {
                    logger.info("(Re-)Loading script '{}'", scriptIdentifier);
                    if (createAndLoad(ref)) {
                        ref.getLoadedStatus().set(true);
                    } else {
                        // make sure script engine is successfully closed and the loading is re-tried
                        manager.removeEngine(ref.getScriptIdentifier());
                        ref.getLoadedStatus().set(false);
                    }
                } else {
                    ref.getLoadedStatus().set(false);
                    logger.debug("Enqueued script '{}'", ref.getScriptIdentifier());
                }
            } finally {
                lock.unlock();
            }
        }, scheduler);
    }

    private boolean createAndLoad(ScriptFileReference ref) {
        String scriptIdentifier = ref.getScriptIdentifier();

        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(ref.getScriptFilePath()),
                StandardCharsets.UTF_8)) {
            ScriptEngineContainer container = manager.createScriptEngine(ref.getScriptType(),
                    ref.getScriptIdentifier());
            if (container != null) {
                container.getScriptEngine().put(ScriptEngine.FILENAME, scriptIdentifier);
                if (manager.loadScript(container.getIdentifier(), reader)) {
                    return true;
                }
            }
            logger.warn("Script loading error, ignoring file '{}'", scriptIdentifier);
        } catch (IOException e) {
            logger.warn("Failed to load file '{}': {}", ref.getScriptFilePath(), e.getMessage());
        }

        return false;
    }

    @Override
    public void onDependencyChange(String scriptIdentifier) {
        logger.debug("Reimporting {}...", scriptIdentifier);
        ScriptFileReference ref = scriptMap.get(scriptIdentifier);
        if (ref != null && !ref.getQueueStatus().getAndSet(true)) {
            importFileWhenReady(scriptIdentifier);
        }
    }

    @Override
    public synchronized void onReadyMarkerAdded(ReadyMarker readyMarker) {
        int previousLevel = currentStartLevel;
        currentStartLevel = Integer.parseInt(readyMarker.getIdentifier());

        logger.trace("Added ready marker {}: start level changed from {} to {}. watchPath: {}", readyMarker,
                previousLevel, currentStartLevel, watchPath);

        if (currentStartLevel < StartLevelService.STARTLEVEL_STATES) {
            // ignore start level less than 30
            return;
        }

        if (!initialized.isDone()) {
            addFiles(listFiles(watchPath, watchSubDirectories)).thenRun(() -> initialized.complete(null));
        } else {
            scriptMap.values().stream().sorted()
                    .filter(ref -> needsStartLevelProcessing(ref, previousLevel, currentStartLevel))
                    .forEach(ref -> importFileWhenReady(ref.getScriptIdentifier()));
        }
    }

    private boolean needsStartLevelProcessing(ScriptFileReference ref, int previousLevel, int newLevel) {
        int refStartLevel = ref.getStartLevel();
        return !ref.getLoadedStatus().get() && newLevel >= refStartLevel && previousLevel < refStartLevel
                && !ref.getQueueStatus().getAndSet(true);
    }

    @Override
    public void onReadyMarkerRemoved(ReadyMarker readyMarker) {
        // we don't need to process this, as openHAB only reduces its start level when the system is shutdown
        // in this case the service is de-activated anyway and al scripts are removed by {@link #deactivate}
    }

    @Override
    public void factoryAdded(@Nullable String scriptType) {
        scriptMap.forEach((scriptIdentifier, ref) -> {
            if (ref.getScriptType().equals(scriptType) && !ref.getQueueStatus().getAndSet(true)) {
                importFileWhenReady(scriptIdentifier);
            }
        });
    }

    @Override
    public void factoryRemoved(@Nullable String scriptType) {
        if (scriptType == null) {
            return;
        }
        Set<String> toRemove = scriptMap.values().stream()
                .filter(ref -> ref.getLoadedStatus().get() && scriptType.equals(ref.getScriptType()))
                .map(ScriptFileReference::getScriptIdentifier).collect(Collectors.toSet());
        toRemove.forEach(this::removeFile);
    }
}
