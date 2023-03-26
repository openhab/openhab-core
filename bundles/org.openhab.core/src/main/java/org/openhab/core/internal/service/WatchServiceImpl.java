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
package org.openhab.core.internal.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.service.WatchService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryChangeListener;
import io.methvin.watcher.DirectoryWatcher;

/**
 * The {@link WatchServiceImpl} is the implementation of the {@link WatchService}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = WatchService.class, configurationPid = WatchService.SERVICE_PID, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class WatchServiceImpl implements WatchService, DirectoryChangeListener {

    public static final int PROCESSING_TIME = 1000;

    public @interface WatchServiceConfiguration {
        String name() default "";

        String path() default "";
    }

    private final Logger logger = LoggerFactory.getLogger(WatchServiceImpl.class);

    private final List<Listener> dirPathListeners = new CopyOnWriteArrayList<>();
    private final List<Listener> subDirPathListeners = new CopyOnWriteArrayList<>();
    private final ExecutorService executor;
    private final ScheduledExecutorService scheduler;

    private final String name;
    private final BundleContext bundleContext;

    private @Nullable Path basePath;
    private @Nullable DirectoryWatcher dirWatcher;
    private @Nullable ServiceRegistration<WatchService> reg;

    private final Map<Path, ScheduledFuture<?>> scheduledEvents = new HashMap<>();
    private final Map<Path, List<Kind>> scheduledEventKinds = new ConcurrentHashMap<>();

    @Activate
    public WatchServiceImpl(WatchServiceConfiguration config, BundleContext bundleContext) throws IOException {
        this.bundleContext = bundleContext;
        if (config.name().isBlank()) {
            throw new IllegalArgumentException("service name must not be blank");
        }

        this.name = config.name();
        executor = Executors.newSingleThreadExecutor(r -> new Thread(r, name));
        scheduler = ThreadPoolManager.getScheduledPool("watchservice");
        modified(config);
    }

    @Modified
    public void modified(WatchServiceConfiguration config) throws IOException {
        logger.trace("Trying to setup WatchService '{}' with path '{}'", config.name(), config.path());

        Path basePath = Path.of(config.path()).toAbsolutePath();

        if (basePath.equals(this.basePath)) {
            return;
        }

        this.basePath = basePath;

        try {
            closeWatcherAndUnregister();

            if (!Files.exists(basePath)) {
                logger.info("Watch directory '{}' does not exists. Trying to create it.", basePath);
                Files.createDirectories(basePath);
            }

            DirectoryWatcher newDirWatcher = DirectoryWatcher.builder().listener(this).path(basePath).build();
            CompletableFuture
                    .runAsync(
                            () -> newDirWatcher.watchAsync(executor)
                                    .thenRun(() -> logger.debug("WatchService '{}' has been shut down.", name)),
                            ThreadPoolManager.getScheduledPool(ThreadPoolManager.THREAD_POOL_NAME_COMMON))
                    .thenRun(this::registerWatchService);
            this.dirWatcher = newDirWatcher;
        } catch (NoSuchFileException e) {
            // log message here, otherwise it'll be swallowed by the call to newInstance in the factory
            // also re-throw the exception to indicate that we failed
            logger.warn("Could not instantiate WatchService '{}', directory '{}' is missing.", name, e.getMessage());
            throw e;
        } catch (IOException e) {
            // log message here, otherwise it'll be swallowed by the call to newInstance in the factory
            // also re-throw the exception to indicate that we failed
            logger.warn("Could not instantiate WatchService '{}':", name, e);
            throw e;
        }
    }

    @Deactivate
    public void deactivate() {
        try {
            closeWatcherAndUnregister();
            executor.shutdown();
        } catch (IOException e) {
            logger.warn("Failed to shutdown WatchService '{}'", name, e);
        }
    }

    private void registerWatchService() {
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(WatchService.SERVICE_PROPERTY_NAME, name);
        this.reg = bundleContext.registerService(WatchService.class, this, properties);
        logger.debug("WatchService '{}' completed initialization and registered itself as service.", name);
    }

    private void closeWatcherAndUnregister() throws IOException {
        DirectoryWatcher localDirWatcher = this.dirWatcher;
        if (localDirWatcher != null) {
            localDirWatcher.close();
            this.dirWatcher = null;
        }

        ServiceRegistration<?> localReg = this.reg;
        if (localReg != null) {
            try {
                localReg.unregister();
            } catch (IllegalStateException e) {
                logger.debug("WatchService '{}' was already unregistered.", name, e);
            }
            this.reg = null;
        }
    }

    @Override
    public Path getWatchPath() {
        Path basePath = this.basePath;
        if (basePath == null) {
            throw new IllegalStateException("Trying to access WatchService before initialization completed.");
        }
        return basePath;
    }

    @Override
    public void registerListener(WatchEventListener watchEventListener, List<Path> paths, boolean withSubDirectories) {
        Path basePath = this.basePath;
        if (basePath == null) {
            throw new IllegalStateException("Trying to register listener before initialization completed.");
        }
        for (Path path : paths) {
            Path absolutePath = path.isAbsolute() ? path : basePath.resolve(path).toAbsolutePath();
            if (absolutePath.startsWith(basePath)) {
                if (withSubDirectories) {
                    subDirPathListeners.add(new Listener(absolutePath, watchEventListener));
                } else {
                    dirPathListeners.add(new Listener(absolutePath, watchEventListener));
                }
            } else {
                logger.warn("Tried to add path '{}' to listener '{}', but the base path of this listener is '{}'", path,
                        name, basePath);
            }
        }
    }

    @Override
    public void unregisterListener(WatchEventListener watchEventListener) {
        subDirPathListeners.removeIf(Listener.isListener(watchEventListener));
        dirPathListeners.removeIf(Listener.isListener(watchEventListener));
    }

    @Override
    public void onEvent(@Nullable DirectoryChangeEvent directoryChangeEvent) throws IOException {
        if (directoryChangeEvent == null || directoryChangeEvent.isDirectory()
                || directoryChangeEvent.eventType() == DirectoryChangeEvent.EventType.OVERFLOW) {
            // exit early, we are neither interested in directory events nor in OVERFLOW events
            return;
        }

        Path path = directoryChangeEvent.path();
        Kind kind = switch (directoryChangeEvent.eventType()) {
            case CREATE -> Kind.CREATE;
            case MODIFY -> Kind.MODIFY;
            case DELETE -> Kind.DELETE;
            case OVERFLOW -> Kind.OVERFLOW;
        };

        synchronized (scheduledEvents) {
            ScheduledFuture<?> future = scheduledEvents.remove(path);
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }
            future = scheduler.schedule(() -> notifyListeners(path), PROCESSING_TIME, TimeUnit.MILLISECONDS);
            scheduledEventKinds.computeIfAbsent(path, k -> new CopyOnWriteArrayList<>()).add(kind);
            scheduledEvents.put(path, future);

        }
    }

    private void notifyListeners(Path path) {
        List<Kind> kinds = scheduledEventKinds.remove(path);
        if (kinds == null || kinds.isEmpty()) {
            logger.debug("Tried to notify listeners of change events for '{}', but the event list is empty.", path);
            return;
        }

        if (kinds.size() == 1) {
            // we have only one event
            doNotify(path, kinds.get(0));
            return;
        }

        Kind firstElement = kinds.get(0);
        Kind lastElement = kinds.get(kinds.size() - 1);

        // determine final event
        if (lastElement == Kind.DELETE) {
            if (firstElement == Kind.CREATE) {
                logger.debug("Discarding events for '{}' because file was immediately deleted bafter creation", path);
                return;
            }
            doNotify(path, Kind.DELETE);
        } else if (firstElement == Kind.CREATE) {
            doNotify(path, Kind.CREATE);
        } else {
            doNotify(path, Kind.MODIFY);
        }
    }

    private void doNotify(Path path, Kind kind) {
        logger.trace("Notifying listeners of '{}' event for '{}'.", kind, path);
        subDirPathListeners.stream().filter(isChildOf(path)).forEach(l -> l.notify(path, kind));
        dirPathListeners.stream().filter(isDirectChildOf(path)).forEach(l -> l.notify(path, kind));
    }

    public static Predicate<Listener> isChildOf(Path path) {
        return l -> path.startsWith(l.rootPath);
    }

    public static Predicate<Listener> isDirectChildOf(Path path) {
        return l -> path.startsWith(l.rootPath) && l.rootPath.relativize(path).getNameCount() == 1;
    }

    private record Listener(Path rootPath, WatchEventListener watchEventListener) {

        void notify(Path path, Kind kind) {
            watchEventListener.processWatchEvent(kind, rootPath.relativize(path));
        }

        static Predicate<Listener> isListener(WatchEventListener watchEventListener) {
            return l -> watchEventListener.equals(l.watchEventListener);
        }
    }
}
