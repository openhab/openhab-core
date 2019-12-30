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
package org.openhab.core.service;

import static java.nio.file.StandardWatchEventKinds.*;

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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
 */
public class WatchQueueReader implements Runnable {

    private static final String THREAD_POOL_NAME = "file-processing";
    private static final int PROCESSING_DELAY = 1000; // ms

    protected final Logger logger = LoggerFactory.getLogger(WatchQueueReader.class);
    private final ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool(THREAD_POOL_NAME);

    protected WatchService watchService;

    private final Map<WatchKey, Path> registeredKeys = new HashMap<>();
    private final Map<WatchKey, AbstractWatchService> keyToService = new HashMap<>();
    private final Map<AbstractWatchService, Map<Path, byte[]>> hashes = new HashMap<>();
    private final Map<WatchKey, @Nullable Map<Path, @Nullable ScheduledFuture<?>>> futures = new ConcurrentHashMap<>();

    private Thread qr;

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
                new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path subDir, BasicFileAttributes attrs)
                            throws IOException {
                        Kind<?>[] kinds = watchService.getWatchEventKinds(subDir);
                        if (kinds != null) {
                            registerDirectoryInternal(watchService, kinds, subDir);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        if (exc instanceof AccessDeniedException) {
                            logger.warn("Access to folder '{}' was denied, therefore skipping it.",
                                    file.toAbsolutePath().toString());
                        }
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                });
    }

    private synchronized void registerDirectoryInternal(AbstractWatchService service, Kind<?>[] kinds, Path directory) {
        if (watchService == null) {
            try {
                watchService = FileSystems.getDefault().newWatchService();
                qr = new Thread(this, "Dir Watcher");
                qr.start();
            } catch (IOException e) {
                logger.debug("The directory '{}' was not registered in the watch service", directory, e);
                return;
            }
        }
        WatchKey registrationKey = null;
        try {
            registrationKey = directory.register(this.watchService, kinds);
        } catch (IOException e) {
            logger.debug("The directory '{}' was not registered in the watch service: {}", directory, e.getMessage());
        }
        if (registrationKey != null) {
            registeredKeys.put(registrationKey, directory);
            keyToService.put(registrationKey, service);
        } else {
            logger.debug("The directory '{}' was not registered in the watch service", directory);
        }
    }

    public synchronized void stopWatchService(AbstractWatchService service) {
        if (watchService != null) {
            List<WatchKey> keys = new LinkedList<>();
            for (WatchKey key : keyToService.keySet()) {
                if (keyToService.get(key) == service) {
                    keys.add(key);
                }
            }
            if (keys.size() == keyToService.size()) {
                try {
                    watchService.close();
                } catch (IOException e) {
                    logger.warn("Cannot deactivate folder watcher", e);
                }
                watchService = null;
                keyToService.clear();
                registeredKeys.clear();
                hashes.clear();
                futures.values().forEach(keyFutures -> keyFutures.values().forEach(future -> future.cancel(true)));
                futures.clear();
            } else {
                for (WatchKey key : keys) {
                    key.cancel();
                    keyToService.remove(key);
                    registeredKeys.remove(key);
                    hashes.remove(service);
                    Map<Path, @Nullable ScheduledFuture<?>> keyFutures = futures.remove(key);
                    if (keyFutures != null) {
                        keyFutures.values().forEach(future -> future.cancel(true));
                    }
                }
            }
        }
    }

    @Override
    public void run() {
        try {
            for (;;) {
                WatchKey key;
                try {
                    key = watchService.take();
                } catch (InterruptedException exc) {
                    logger.info("Caught InterruptedException: {}", exc.getLocalizedMessage());
                    return;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == OVERFLOW) {
                        logger.warn(
                                "Found an event of kind 'OVERFLOW': {}. File system changes might have been missed.",
                                event);
                        continue;
                    }

                    Path resolvedPath = resolvePath(key, event);
                    if (resolvedPath != null) {
                        // Process the event only when a relative path to it is resolved
                        AbstractWatchService service;
                        synchronized (this) {
                            service = keyToService.get(key);
                        }
                        if (service != null) {
                            File f = resolvedPath.toFile();
                            if (kind == ENTRY_MODIFY && f.isDirectory()) {
                                logger.trace("Skipping modification event for directory: {}", f);
                            } else {
                                if (kind == ENTRY_MODIFY) {
                                    processModificationEvent(key, event, resolvedPath, service);
                                } else {
                                    service.processWatchEvent(event, kind, resolvedPath);
                                }
                            }
                            if (kind == ENTRY_CREATE && f.isDirectory() && service.watchSubDirectories()
                                    && service.getWatchEventKinds(resolvedPath) != null) {
                                registerDirectoryInternal(service, service.getWatchEventKinds(resolvedPath),
                                        resolvedPath);
                            } else if (kind == ENTRY_DELETE) {
                                synchronized (this) {
                                    WatchKey toCancel = null;
                                    for (WatchKey k : registeredKeys.keySet()) {
                                        if (registeredKeys.get(k).equals(resolvedPath)) {
                                            toCancel = k;
                                            break;
                                        }
                                    }
                                    if (toCancel != null) {
                                        registeredKeys.remove(toCancel);
                                        keyToService.remove(toCancel);
                                        toCancel.cancel();
                                    }
                                    forgetChecksum(service, resolvedPath);
                                    Map<Path, @Nullable ScheduledFuture<?>> keyFutures = futures.get(key);
                                    if (keyFutures != null) {
                                        ScheduledFuture<?> future = keyFutures.remove(resolvedPath);
                                        if (future != null) {
                                            future.cancel(true);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                key.reset();
            }
        } catch (Exception exc) {
            logger.debug("ClosedWatchServiceException caught! {}. \n{} Stopping ", exc.getLocalizedMessage(),
                    Thread.currentThread().getName());
            return;
        }
    }

    /**
     * Schedules forwarding of the event to the listeners (if appliccable).
     * <p>
     * By delaying the forwarding, duplicate modification events and those where the actual file-content is not
     * consistent or empty in between will get skipped and the file system gets a chance to "settle" before the
     * framework is going to act on it.
     * <p>
     * Also, modification events are received for meta-data changes (e.g. last modification timestamp or file
     * permissions). They are filtered out by comparing the checksums of the file's content.
     * <p>
     * See also
     * <a href=
     * "https://stackoverflow.com/questions/16777869/java-7-watchservice-ignoring-multiple-occurrences-of-the-same-event">this
     * discussion</a> on Stack Overflow.
     *
     *
     * @param key
     * @param event
     * @param resolvedPath
     * @param service
     */
    private void processModificationEvent(WatchKey key, WatchEvent<?> event, Path resolvedPath,
            AbstractWatchService service) {
        synchronized (futures) {
            logger.trace("Modification event for {} ", resolvedPath);
            ScheduledFuture<?> previousFuture = removeScheduledJob(key, resolvedPath);
            if (previousFuture != null) {
                previousFuture.cancel(true);
                logger.trace("Cancelled previous for {} ", resolvedPath);
            }
            ScheduledFuture<?> future = scheduler.schedule(() -> {
                logger.trace("Executing job for {}", resolvedPath);
                ScheduledFuture<?> res = removeScheduledJob(key, resolvedPath);
                if (res != null) {
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
            rememberScheduledJob(key, resolvedPath, future);
        }
    }

    private Path resolvePath(WatchKey key, WatchEvent<?> event) {
        WatchEvent<Path> ev = cast(event);
        // Context for directory entry event is the file name of entry.
        Path contextPath = ev.context();
        Path baseWatchedDir = null;
        Path registeredPath = null;
        synchronized (this) {
            baseWatchedDir = keyToService.get(key).getSourcePath();
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

    private byte[] hash(Path path) {
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
        Map<Path, byte[]> keyHashes = hashes.get(service);
        if (keyHashes == null) {
            keyHashes = new HashMap<>();
            hashes.put(service, keyHashes);
        }
        byte[] oldHash = keyHashes.put(resolvedPath, newHash);
        return oldHash == null || !Arrays.equals(oldHash, newHash);
    }

    private void forgetChecksum(AbstractWatchService service, Path resolvedPath) {
        Map<Path, byte[]> keyHashes = hashes.get(service);
        if (keyHashes != null) {
            keyHashes.remove(resolvedPath);
        }
    }

    private Map<Path, @Nullable ScheduledFuture<?>> getKeyFutures(WatchKey key) {
        Map<Path, @Nullable ScheduledFuture<?>> keyFutures = futures.get(key);
        if (keyFutures == null) {
            keyFutures = new ConcurrentHashMap<>();
            futures.put(key, keyFutures);
        }
        return keyFutures;
    }

    private ScheduledFuture<?> removeScheduledJob(WatchKey key, Path resolvedPath) {
        Map<Path, @Nullable ScheduledFuture<?>> keyFutures = getKeyFutures(key);
        return keyFutures.remove(resolvedPath);
    }

    private void rememberScheduledJob(WatchKey key, Path resolvedPath, ScheduledFuture<?> future) {
        Map<Path, @Nullable ScheduledFuture<?>> keyFutures = getKeyFutures(key);
        keyFutures.put(resolvedPath, future);
    }

}
