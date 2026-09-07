/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.service.WatchService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryChangeListener;
import io.methvin.watcher.DirectoryWatcher;
import io.methvin.watcher.hashing.FileHash;

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
    private final Map<Path, FileHash> hashCache = new ConcurrentHashMap<>();
    private final ConfigurationAdmin configurationAdmin;
    private final ExecutorService executor;
    private final ScheduledExecutorService scheduler;

    private final String name;
    private final BundleContext bundleContext;
    private volatile @Nullable Path basePath;
    volatile @Nullable DirectoryWatcher dirWatcher;

    private final Map<Path, ScheduledFuture<?>> scheduledEvents = new HashMap<>();
    private final Map<Path, List<DirectoryChangeEvent>> scheduledEventKinds = new ConcurrentHashMap<>();

    @Activate
    public WatchServiceImpl(@Reference ConfigurationAdmin configurationAdmin, WatchServiceConfiguration config,
            BundleContext bundleContext, ComponentContext componentContext) throws IOException {
        this.configurationAdmin = configurationAdmin;
        this.bundleContext = bundleContext;
        if (config.name().isBlank()) {
            throw new IllegalArgumentException("service name must not be blank");
        }

        this.name = config.name();
        executor = Executors.newSingleThreadExecutor(r -> new Thread(r, name));
        scheduler = ThreadPoolManager.getScheduledPool("watchservice");
        modified(config, componentContext);
    }

    @Modified
    public void modified(WatchServiceConfiguration config, final ComponentContext componentContext) throws IOException {
        logger.trace("Trying to setup WatchService '{}' with path '{}'", config.name(), config.path());

        Path basePath = Path.of(config.path()).toAbsolutePath();

        if (basePath.equals(this.basePath)) {
            return;
        }

        final boolean cycle = this.basePath != null;
        this.basePath = basePath;

        try {
            closeWatcher();

            if (!Files.exists(basePath)) {
                logger.info("Watch directory '{}' does not exist. Trying to create it.", basePath);
                Files.createDirectories(basePath);
            }

            DirectoryWatcher newDirWatcher = DirectoryWatcher.builder().listener(this).path(basePath).build();
            ThreadPoolManager.getScheduledPool(ThreadPoolManager.THREAD_POOL_NAME_COMMON).execute(() -> {
                if (cycle) {
                    Object pid = componentContext.getProperties().get("service.pid");
                    if (pid instanceof String pidString) {
                        Configuration[] configs = null;
                        try {
                            configs = configurationAdmin.listConfigurations("(service.pid=" + pidString + ")");
                        } catch (IOException | InvalidSyntaxException e) {
                            logger.warn("WatchService '{}': Failed to acquire configuration, cannot restart service",
                                    name, e);
                        }

                        if (configs != null && configs.length > 0) {
                            final AtomicReference<@Nullable ServiceRegistration<ConfigurationListener>> registrationReference = new AtomicReference<>();

                            ConfigurationListener tempListener = new ConfigurationListener() {
                                @Override
                                public void configurationEvent(@Nullable ConfigurationEvent event) {
                                    if (event != null && event.getType() == ConfigurationEvent.CM_DELETED
                                            && pidString.equals(event.getPid())) {
                                        logger.debug("WatchService '{}': Configuration deleted", name);

                                        // Unregister the listener first, this is a one trick pony
                                        ServiceRegistration<ConfigurationListener> registration = registrationReference
                                                .get();
                                        if (registration != null) {
                                            try {
                                                registration.unregister();
                                            } catch (IllegalStateException e) {
                                                // Already unregistered
                                            }
                                        }

                                        createConfiguration(basePath);
                                    }
                                }
                            };

                            ServiceRegistration<ConfigurationListener> registration = bundleContext
                                    .registerService(ConfigurationListener.class, tempListener, null);
                            registrationReference.set(registration);
                            try {
                                configs[0].delete();
                            } catch (IOException | RuntimeException e) {
                                logger.warn("WatchService '{}': Failed to delete configuration, cannot restart service",
                                        name, e);
                                try {
                                    registration.unregister();
                                } catch (IllegalStateException e2) {
                                    // Already unregistered
                                }
                            }
                        }
                    }
                }
                newDirWatcher.watchAsync(executor)
                        .thenRun(() -> logger.debug("WatchService '{}' has been shut down.", name));
            });
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
            closeWatcher();
            executor.shutdownNow();
            try {
                executor.awaitTermination(1000L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // We still want to cancel the scheduled events, so let it be re-caught after that
            }
            synchronized (scheduledEvents) {
                for (ScheduledFuture<?> future : scheduledEvents.values()) {
                    if (!future.isDone()) {
                        future.cancel(true);
                    }
                }
                for (ScheduledFuture<?> future : scheduledEvents.values()) {
                    if (!future.isDone()) {
                        try {
                            future.get(1000L, TimeUnit.MILLISECONDS);
                        } catch (CancellationException e) {
                            // This is what we want. move on
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        } catch (ExecutionException | TimeoutException e) {
                            logger.debug("Failed to conclude scheduled event during deactivate: {}", e.getMessage(), e);
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to shutdown WatchService '{}'", name, e);
        }
    }

    private void closeWatcher() throws IOException {
        DirectoryWatcher localDirWatcher = this.dirWatcher;
        if (localDirWatcher != null) {
            localDirWatcher.close();
            this.dirWatcher = null;
        }

        hashCache.clear();
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
        logger.trace("onEvent {}", directoryChangeEvent);
        if (directoryChangeEvent == null || directoryChangeEvent.isDirectory()
                || directoryChangeEvent.eventType() == DirectoryChangeEvent.EventType.OVERFLOW) {
            // exit early, we are neither interested in directory events nor in OVERFLOW events
            return;
        }

        Path path = directoryChangeEvent.path();

        synchronized (scheduledEvents) {
            ScheduledFuture<?> future = scheduledEvents.remove(path);
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }
            future = scheduler.schedule(() -> notifyListeners(path), PROCESSING_TIME, TimeUnit.MILLISECONDS);
            Objects.requireNonNull(scheduledEventKinds.computeIfAbsent(path, k -> new CopyOnWriteArrayList<>()))
                    .add(directoryChangeEvent);
            scheduledEvents.put(path, future);
        }
    }

    private void createConfiguration(Path basePath) {
        logger.debug("WatchService '{}': Creating new configuration for path '{}'", name, basePath);
        try {
            String filter = "(&(name=" + name + ")" + "(service.factoryPid=" + WatchService.SERVICE_PID + "))";
            Configuration[] configurations = configurationAdmin.listConfigurations(filter);

            if (configurations == null || configurations.length == 0) {
                Configuration c = configurationAdmin.createFactoryConfiguration(WatchService.SERVICE_PID, "?");
                Dictionary<String, Object> map = new Hashtable<>();

                map.put("name", name);
                map.put(WatchService.SERVICE_PROPERTY_NAME, name);
                map.put("path", basePath.toString());
                c.update(map);
            } else {
                Configuration c = configurations[0];
                Dictionary<String, Object> map = c.getProperties();
                map.put("name", name);
                map.put(WatchService.SERVICE_PROPERTY_NAME, name);
                map.put("path", basePath.toString());
                c.update(map);
            }
        } catch (IOException | InvalidSyntaxException e) {
            logger.error("WatchService '{}': Failed to create configuration with path '{}'", name, basePath, e);
        }
    }

    private void notifyListeners(Path path) {
        List<DirectoryChangeEvent> events = scheduledEventKinds.remove(path);
        if (events == null || events.isEmpty()) {
            logger.debug("Tried to notify listeners of change events for '{}', but the event list is empty.", path);
            return;
        }

        DirectoryChangeEvent firstElement = events.getFirst();
        DirectoryChangeEvent lastElement = events.getLast();

        // determine final event
        if (lastElement.eventType() == DirectoryChangeEvent.EventType.DELETE) {
            if (firstElement.eventType() == DirectoryChangeEvent.EventType.CREATE) {
                logger.debug("Discarding events for '{}' because file was immediately deleted after creation", path);
                return;
            }
            hashCache.remove(lastElement.path());
            doNotify(path, Kind.DELETE);
        } else if (firstElement.eventType() == DirectoryChangeEvent.EventType.CREATE) {
            if (lastElement.hash() == null) {
                logger.warn("Detected invalid event (hash must not be null for CREATE/MODIFY): {}", lastElement);
                return;
            }
            hashCache.put(lastElement.path(), lastElement.hash());
            doNotify(path, Kind.CREATE);
        } else {
            if (lastElement.hash() == null) {
                logger.warn("Detected invalid event (hash must not be null for CREATE/MODIFY): {}", lastElement);
                return;
            }
            FileHash oldHash = hashCache.put(lastElement.path(), lastElement.hash());
            if (!Objects.equals(oldHash, lastElement.hash())) {
                // only notify if hashes are different, otherwise the file content did not chnge
                doNotify(path, Kind.MODIFY);
            }
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

        void notify(Path fullPath, Kind kind) {
            watchEventListener.processWatchEvent(kind, fullPath);
        }

        static Predicate<Listener> isListener(WatchEventListener watchEventListener) {
            return l -> watchEventListener.equals(l.watchEventListener);
        }
    }
}
