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

import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link WatchServiceFactory} is used to create {@link WatchService} instances.
 *
 * For files in the openHAB configuration folder a watch service with the name {@link WatchService#CONFIG_WATCHER_NAME}
 * is registered. For convenience, an OSGi target filter for referencing this watch service is provided
 * {@link WatchService#CONFIG_WATCHER_FILTER}.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public interface WatchServiceFactory {

    /**
     * Create a new {@link WatchService} service component with the given name and path or return the already existing
     * instance if a {@link WatchService} with the given name was created before.
     *
     * @param name the name of the service to create/get (must follow the conventions of an OSGi service name)
     * @param basePath the base path of the watch service (path is created if it does not exist)
     * @return a {@link WatchService} with the given configuration
     * @throws IOException if the {@link WatchService} could not be instantiated
     */
    void createWatchService(String name, Path basePath) throws IOException;

    /**
     * Dispose the {@link WatchService} service component
     *
     * @param name the name of the {@link WatchService}
     */
    void removeWatchService(String name);
}
