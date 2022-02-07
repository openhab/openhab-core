/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.core.service;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for watch queue readers
 *
 * @author Fabio Marini - Initial contribution
 * @author Dimitar Ivanov - use relative path in watch events. Added option to watch directory events or not
 * @author Ana Dimova - reduce to a single watch thread for all class instances of {@link AbstractWatchService}
 * @author Jan N. Klug - allow multiple listeners for the same directory and add null annotations
 */
@NonNullByDefault
public class WatchQueueReader implements Runnable {
    private static final String THREAD_POOL_NAME = "file-processing";
    private static final int PROCESSING_DELAY = 1000; // ms

    protected final Logger logger = LoggerFactory.getLogger(WatchQueueReader.class);
    private final ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool(THREAD_POOL_NAME);

    protected @Nullable WatchService watchService;

    private final Map<WatchKey, Path> registeredKeys = new HashMap<>();
    private final Map<WatchKey, Set<AbstractWatchService>> keyToService = new HashMap<>();
    private final Map<AbstractWatchService, Map<Path, byte[]>> hashes = new HashMap<>();
    private final List<Notification> notifications = new CopyOnWriteArrayList<>();

    private @Nullable Thread qr;

    private static final WatchQueueReader INSTANCE = new WatchQueueReader();

    /**
     * Perform a simple cast of given event to WatchEvent
     *
     * @param event the event to cast
     * @return the casted event
     */
    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    public static WatchQueueReader getInstance() {
        return INSTANCE;
    }

    private WatchQueueReader() {
        // prevent instantiation
    }

    // used for testing to check if properly terminated
    @Nullable
    WatchService getWatchService() {
        return watchService;
    }

    /**
     * Customize the queue reader to process the watch events for the given directory, provided by the watch service
     *
     * @param watchService the watch service, requesting the watch events for the watched directory
     * @param toWatch the directory being watched by the watch service
     * @param watchSubDirectories a boolean flag that specifies if the child directories of the registered directory
     *            will being watched by the watch service
     */
    protected void customizeWatchQueueReader(AbstractWatchService watchService, Path toWatch,
            boolean watchSubDirectories) {
        try {
            if (watchSubDirectories) {
                // walk through all folders and follow symlinks
                registerWithSubDirectories(watchService, toWatch);
            } else {
                registerDirectoryInternal(watchService, watchService.getWatchEventKinds(toWatch), toWatch);
            }
        } catch (NoSuchFileException e) {
            logger.debug("Not watching folder '{}' as it does not exist.", toWatch);
        } catch (IOException e) {
            logger.warn("Cannot customize folder watcher for folder '{}'", toWatch, e);
        }
    }

    private void registerWithSubDirectories(AbstractWatchService watchService, Path toWatch) throws IOException {
        Files.walkFileTree(toWatch, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
                new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(@Nullable Path subDir,
                            @Nullable BasicFileAttributes attrs) {
                        if (subDir != null) {
                            Kind<?>[] kinds = watchService.getWatchEventKinds(subDir);
                            registerDirectoryInternal(watchService, kinds, subDir);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(@Nullable Path file, @Nullable IOException exc) {
                        if (exc instanceof AccessDeniedException) {
                            logger.warn("Access to folder '{}' was denied, therefore skipping it.",
                                    file != null ? file.toAbsolutePath() : null);
                        }
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                });
    }

    private synchronized void registerDirectoryInternal(AbstractWatchService service, Kind<?> @Nullable [] kinds,
            Path directory) {
        WatchService watchService = this.watchService;
        if (watchService == null) {
            try {
                watchService = FileSystems.getDefault().newWatchService();
                this.watchService = watchService;
                Thread qr = new Thread(this, "openHAB Dir Watcher");
                this.qr = qr;
                qr.start();
            } catch (IOException e) {
                logger.debug("The directory '{}' was not registered in the watch service", directory, e);
                return;
            }
        }
        WatchKey registrationKey = null;
        if (kinds == null) {
            return;
        }
        try {
            registrationKey = directory.register(watchService, kinds);
        } catch (IOException e) {
            logger.debug("The directory '{}' was not registered in the watch service: {}", directory, e.getMessage());
        }
        if (registrationKey != null) {
            registeredKeys.put(registrationKey, directory);
            Set<AbstractWatchService> services = Objects
                    .requireNonNull(keyToService.computeIfAbsent(registrationKey, k -> new HashSet<>()));
            services.add(service);
        } else {
            logger.debug("The directory '{}' was not registered in the watch service", directory);
        }
    }

    public synchronized void stopWatchService(AbstractWatchService service) {
        List<WatchKey> keysToRemove = new LinkedList<>();
        for (Map.Entry<WatchKey, Set<AbstractWatchService>> entry : keyToService.entrySet()) {
            Set<AbstractWatchService> services = entry.getValue();
            services.remove(service);
            if (services.isEmpty()) {
                keysToRemove.add(entry.getKey());
            }
        }
        for (Notification notification : notifications) {
            if (notification.service.equals(service)) {
                notification.future.cancel(true);
                notifications.remove(notification);
            }
        }
        if (keysToRemove.size() == keyToService.size()) {
            try {
                WatchService watchService = this.watchService;
                if (watchService != null) {
                    watchService.close();

                    Thread qr = this.qr;
                    if (qr != null) {
                        qr.interrupt();
                        this.qr = null;
                    }

                    this.watchService = null;
                }
            } catch (IOException e) {
                logger.warn("Cannot deactivate folder watcher", e);
            }
            keyToService.clear();
            registeredKeys.clear();
            hashes.clear();
            notifications.forEach(notification -> notification.future.cancel(true));
            notifications.clear();
        } else {
            for (WatchKey key : keysToRemove) {
                key.cancel();
                keyToService.remove(key);
                registeredKeys.remove(key);
                hashes.remove(service);
            }
        }
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                WatchKey key = null;
                WatchService watchService = this.watchService;
                if (watchService != null) {
                    key = watchService.take();
                }

                if (key == null) {
                    continue;
                }
                for (WatchEvent<?> event : key.pollEvents()) {
                    Kind<?> kind = event.kind();
                    if (kind == OVERFLOW) {
                        logger.warn(
                                "Found an event of kind 'OVERFLOW': {}. File system changes might have been missed.",
                                event);
                        continue;
                    }
                    Path resolvedPath = resolvePath(key, event);

                    if (resolvedPath != null) {
                        // Process the event only when a relative path to it is resolved
                        Set<AbstractWatchService> services;
                        synchronized (this) {
                            services = keyToService.get(key);
                        }
                        if (services != null) {
                            File f = resolvedPath.toFile();
                            if (kind == ENTRY_MODIFY && f.isDirectory()) {
                                logger.trace("Skipping modification event for directory: {}", f);
                            } else {
                                if (kind == ENTRY_MODIFY) {
                                    processModificationEvent(key, event, resolvedPath, services);
                                } else {
                                    services.forEach(s -> s.processWatchEvent(event, kind, resolvedPath));
                                }
                            }
                            if (kind == ENTRY_CREATE && f.isDirectory()) {
                                for (AbstractWatchService service : services) {
                                    if (service.watchSubDirectories()
                                            && service.getWatchEventKinds(resolvedPath) != null) {
                                        registerDirectoryInternal(service, service.getWatchEventKinds(resolvedPath),
                                                resolvedPath);
                                    }
                                }
                            } else if (kind == ENTRY_DELETE) {
                                synchronized (this) {
                                    WatchKey toCancel = null;
                                    for (Map.Entry<WatchKey, Path> entry : registeredKeys.entrySet()) {
                                        if (entry.getValue().equals(resolvedPath)) {
                                            toCancel = entry.getKey();
                                            break;
                                        }
                                    }
                                    if (toCancel != null) {
                                        registeredKeys.remove(toCancel);
                                        keyToService.remove(toCancel);
                                        toCancel.cancel();
                                    }

                                    services.forEach(service -> forgetChecksum(service, resolvedPath));
                                    notifications.forEach(notification -> {
                                        if (notification.path.equals(resolvedPath)) {
                                            notification.future.cancel(true);
                                            notifications.remove(notification);
                                        }
                                    });
                                }
                            }
                        }
                    }
                }

                key.reset();
            } catch (InterruptedException exc) {
                logger.debug("Caught InterruptedException. Shutting down.");
                return;
            } catch (Exception exc) {
                logger.debug("Exception caught in WatchQueueReader. Restarting.", exc);
            }
        }
    }

    /**
     * Schedules forwarding of the event to the listeners (if applicable).
     *
     * By delaying the forwarding, duplicate modification events and those where the actual file-content is not
     * consistent or empty in between will get skipped and the file system gets a chance to "settle" before the
     * framework is going to act on it.
     *
     * Also, modification events are received for meta-data changes (e.g. last modification timestamp or file
     * permissions). They are filtered out by comparing the checksums of the file's content.
     *
     * See also <a href=
     * "https://stackoverflow.com/questions/16777869/java-7-watchservice-ignoring-multiple-occurrences-of-the-same-event">thisdiscussion</a>
     * on Stack Overflow.
     *
     * @param key the {@link WatchKey}
     * @param event the {@link WatchEvent} itself
     * @param resolvedPath the resolved {@link Path} for this event
     * @param services the {@link AbstractWatchService}s that subscribe to this event
     */
    private void processModificationEvent(WatchKey key, WatchEvent<?> event, Path resolvedPath,
            Set<AbstractWatchService> services) {
        synchronized (notifications) {
            for (AbstractWatchService service : services) {
                logger.trace("Modification event for {} ", resolvedPath);
                removeScheduledNotifications(key, service, resolvedPath);
                ScheduledFuture<?> future = scheduler.schedule(() -> {
                    logger.trace("Executing job for {}", resolvedPath);
                    if (removeScheduledNotifications(key, service, resolvedPath)) {
                        logger.trace("Job removed itself for {}", resolvedPath);
                    } else {
                        logger.trace("Job couldn't find itself for {}", resolvedPath);
                    }
                    if (checkAndTrackContent(service, resolvedPath)) {
                        service.processWatchEvent(event, event.kind(), resolvedPath);
                    } else {
                        logger.trace("File content '{}' has not changed, skipping modification event", resolvedPath);
                    }
                }, PROCESSING_DELAY, TimeUnit.MILLISECONDS);
                logger.trace("Scheduled processing of {}", resolvedPath);
                notifications.add(new Notification(key, service, resolvedPath, future));
            }
        }
    }

    private boolean removeScheduledNotifications(WatchKey key, AbstractWatchService service, Path path) {
        Set<Notification> notifications = this.notifications.stream().filter(f -> f.matches(key, service, path))
                .collect(Collectors.toSet());
        if (notifications.size() > 1) {
            logger.warn("Found more than one notification for {} / {} / {}. This is a bug.", key, service, path);
        }
        notifications.forEach(notification -> notification.future.cancel(true));
        this.notifications.removeAll(notifications);
        return !notifications.isEmpty();
    }

    private @Nullable Path resolvePath(WatchKey key, WatchEvent<?> event) {
        WatchEvent<Path> ev = cast(event);
        // Context for directory entry event is the file name of entry.
        Path contextPath = ev.context();
        List<Path> baseWatchedDir;
        Path registeredPath;
        synchronized (this) {
            baseWatchedDir = keyToService.getOrDefault(key, Set.of()).stream().map(AbstractWatchService::getSourcePath)
                    .filter(Objects::nonNull).map(Objects::requireNonNull).collect(Collectors.toList());
            registeredPath = registeredKeys.get(key);
        }
        if (registeredPath != null) {
            // If the path has been registered in the watch service it relative path can be resolved
            // The context path is resolved by its already registered parent path
            return registeredPath.resolve(contextPath);
        }

        logger.warn(
                "Detected invalid WatchEvent '{}' and key '{}' for entry '{}' in not registered file or directory of '{}'",
                event, key, contextPath, baseWatchedDir);
        return null;
    }

    private byte @Nullable [] hash(Path path) {
        try {
            MessageDigest digester = MessageDigest.getInstance("SHA-256");
            if (!Files.exists(path)) {
                return null;
            }
            try (InputStream is = Files.newInputStream(path)) {
                byte[] buffer = new byte[4069];
                int read;
                do {
                    read = is.read(buffer);
                    if (read > 0) {
                        digester.update(buffer, 0, read);
                    }
                } while (read != -1);
            }
            return digester.digest();
        } catch (NoSuchAlgorithmException | IOException e) {
            logger.debug("Error calculating the hash of file {}", path, e);
            return null;
        }
    }

    /**
     * Calculate a checksum of the given file and report back whether it has changed since the last time.
     *
     * @param service the service determining the scope
     * @param resolvedPath the file path
     * @return {@code true} if the file content has changed since the last call to this method
     */
    private boolean checkAndTrackContent(AbstractWatchService service, Path resolvedPath) {
        byte[] newHash = hash(resolvedPath);
        if (newHash == null) {
            return true;
        }
        Map<Path, byte[]> keyHashes = Objects.requireNonNull(hashes.computeIfAbsent(service, s -> new HashMap<>()));
        byte[] oldHash = keyHashes.put(resolvedPath, newHash);
        return oldHash == null || !Arrays.equals(oldHash, newHash);
    }

    private void forgetChecksum(AbstractWatchService service, Path resolvedPath) {
        Map<Path, byte[]> keyHashes = hashes.get(service);
        if (keyHashes != null) {
            keyHashes.remove(resolvedPath);
        }
    }

    /**
     * The {@link Notification} stores the information of a single notification
     */
    private static class Notification {
        public final WatchKey key;
        public final AbstractWatchService service;
        public final Path path;
        public final ScheduledFuture<?> future;

        private Notification(WatchKey key, AbstractWatchService service, Path path, ScheduledFuture<?> future) {
            this.key = key;
            this.service = service;
            this.path = path;
            this.future = future;
        }

        public boolean matches(WatchKey key, AbstractWatchService service, Path path) {
            return this.key.equals(key) && this.service.equals(service) && this.path.equals(path);
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Notification notification = (Notification) o;
            return key.equals(notification.key) && service.equals(notification.service)
                    && path.equals(notification.path) && future.equals(notification.future);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, service, path, future);
        }
    }
}
