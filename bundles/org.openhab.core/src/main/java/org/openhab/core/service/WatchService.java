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
package org.openhab.core.service;

import java.nio.file.Path;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link WatchService} defines the interface for a general watch service. It allows registering
 * listeners for subdirectories of the openHAB configuration directory. The reported path in the event is relative to
 * the registered path. Watch services are created by {@link WatchServiceFactory#createWatchService(String, Path)}.
 *
 * For files in the openHAB configuration folder a watch service with the name {@link WatchService#CONFIG_WATCHER_NAME}
 * is registered. For convenience, an OSGi target filter for referencing this watch service is provided
 * {@link WatchService#CONFIG_WATCHER_FILTER}.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public interface WatchService {
    String SERVICE_PID = "org.openhab.core.service.WatchService";
    String SERVICE_PROPERTY_DIR = "watchservice.dir";
    String SERVICE_PROPERTY_NAME = "watchservice.name";
    String CONFIG_WATCHER_NAME = "configWatcher";
    String CONFIG_WATCHER_FILTER = "(" + SERVICE_PROPERTY_NAME + "=" + CONFIG_WATCHER_NAME + ")";

    /**
     * Register a listener for this {@link WatchService}
     *
     * The given listener will be notified about all events related to files and directories (including subdirectories)
     * in the given {@link Path}
     * <p />
     * Listeners must unregister themselves before they are disposed
     *
     * @param watchEventListener the listener for this configuration
     * @param path a {@link Path} that the listener is interested in, relative to the base path of the watch service
     */
    default void registerListener(WatchEventListener watchEventListener, Path path) {
        registerListener(watchEventListener, List.of(path), true);
    }

    /**
     * Register a listener for this {@link WatchService}
     *
     * The given listener will be notified about all events related to files and directories (including subdirectories)
     * in the given {@link Path}
     * <p />
     * Listeners must unregister themselves before they are disposed
     *
     * @param watchEventListener the listener for this configuration
     * @param paths a list of {@link Path} that the listener is interested in, relative to the base path of the watch
     *            service
     */
    default void registerListener(WatchEventListener watchEventListener, List<Path> paths) {
        registerListener(watchEventListener, paths, true);
    }

    /**
     * Register a listener for this {@link WatchService}
     *
     * The given listener will be notified about all events related to files and directories in the given {@link Path}
     * <p />
     * Listeners must unregister themselves before they are disposed
     *
     * @param watchEventListener the listener for this configuration
     * @param path the{@link Path} that the listener is interested in, relative to the base path of the watch service
     * @param withSubDirectories whether subdirectories of the given path should also be watched
     */
    default void registerListener(WatchEventListener watchEventListener, Path path, boolean withSubDirectories) {
        registerListener(watchEventListener, List.of(path), withSubDirectories);
    }

    /**
     * Register a listener for this {@link WatchService}
     *
     * The given listener will be notified about all events related to files and directories in the given list of
     * {@link Path}
     * <p />
     * Listeners must unregister themselves before they are disposed
     *
     * @param watchEventListener the listener for this configuration
     * @param paths a list of {@link Path} that the listener is interested in, relative to the base path of the watch
     *            service
     * @param withSubDirectories whether subdirectories of the given paths should also be watched
     */
    void registerListener(WatchEventListener watchEventListener, List<Path> paths, boolean withSubDirectories);

    /**
     * Unregister a listener from this {@link WatchService}
     *
     * The listener will no longer be notified of watch events
     *
     * @param watchEventListener the listener to unregister
     */
    void unregisterListener(WatchEventListener watchEventListener);

    /**
     * Get the base directory for this {@link WatchService}
     *
     * @return the {@link Path} that is the base path for all reported events
     */
    Path getWatchPath();

    @FunctionalInterface
    interface WatchEventListener {
        /**
         * Notify Listener about watch event
         *
         * @param kind the {@link Kind} of this event
         * @param path the relative path of the file associated with this event
         */
        void processWatchEvent(Kind kind, Path path);
    }

    enum Kind {
        CREATE,
        MODIFY,
        DELETE,
        OVERFLOW
    }
}
