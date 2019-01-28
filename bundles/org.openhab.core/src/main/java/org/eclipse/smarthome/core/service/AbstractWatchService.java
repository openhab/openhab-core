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
package org.eclipse.smarthome.core.service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for OSGI services that access to file system by Java WatchService. <br />
 * See the WatchService <a href=
 * "http://docs.oracle.com/javase/7/docs/api/java/nio/file/WatchService.html"
 * >java docs</a> for more details
 *
 * @author Fabio Marini
 * @author Dimitar Ivanov - added javadoc; introduced WatchKey to directory mapping for the queue reader
 * @author Ana Dimova - reduce to a single watch thread for all class instances of {@link AbstractWatchService}
 *
 */
public abstract class AbstractWatchService {

    /**
     * Default logger for ESH Watch Services
     */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected String pathToWatch;

    protected AbstractWatchService(String pathToWatch) {
        this.pathToWatch = pathToWatch;
    }

    /**
     * The queue reader
     */
    protected WatchQueueReader watchQueueReader;

    /**
     * Method to call on service activation
     */
    public void activate() {
        Path pathToWatch = getSourcePath();
        if (pathToWatch != null) {
            watchQueueReader = WatchQueueReader.getInstance();
            watchQueueReader.customizeWatchQueueReader(this, pathToWatch, watchSubDirectories());
        }
    }

    /**
     * Method to call on service deactivation
     */
    public void deactivate() {
        WatchQueueReader watchQueueReader = this.watchQueueReader;
        if (watchQueueReader != null) {
            watchQueueReader.stopWatchService(this);
        }
        this.watchQueueReader = null;
    }

    /**
     * @return the path to be watched as a {@link String}. The returned path should be applicable for creating a
     *         {@link Path} with the {@link Paths#get(String, String...)} method.
     */
    public Path getSourcePath() {
        if (StringUtils.isNotBlank(pathToWatch)) {
            return Paths.get(pathToWatch);
        }
        return null;
    }

    /**
     * Determines whether the subdirectories of the source path (determined by the {@link #getSourcePath()}) will be
     * watched or not.
     *
     * @return <code>true</code> if the subdirectories will be watched and <code>false</code> if only the source path
     *         (determined by the {@link #getSourcePath()}) will be watched
     */
    protected abstract boolean watchSubDirectories();

    /**
     * Provides the {@link WatchKey}s for the registration of the directory, which will be registered in the watch
     * service.
     *
     * @param directory the directory, which will be registered in the watch service
     * @return The array of {@link WatchKey}s for the registration or <code>null</code> if no registration has been
     *         done.
     */
    protected abstract Kind<?>[] getWatchEventKinds(Path directory);

    /**
     * Processes the given watch event. Note that the kind and the number of the events for the watched directory is a
     * platform dependent (see the "Platform dependencies" sections of {@link WatchService}).
     *
     * @param event the watch event to be handled
     * @param kind the event's kind
     * @param path the path of the event (resolved to the {@link #baseWatchedDir})
     */
    protected abstract void processWatchEvent(WatchEvent<?> event, Kind<?> kind, Path path);

}
