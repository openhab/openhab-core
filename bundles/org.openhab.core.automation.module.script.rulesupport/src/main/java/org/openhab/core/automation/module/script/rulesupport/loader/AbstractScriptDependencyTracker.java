/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.module.script.ScriptDependencyTracker;
import org.openhab.core.common.BidiSetBag;
import org.openhab.core.service.WatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AbstractScriptDependencyTracker} tracks dependencies between scripts and reloads dependees
 * It needs to be sub-classed for each {@link org.openhab.core.automation.module.script.ScriptEngineFactory}
 * that wants to support dependency tracking
 *
 * @author Jonathan Gilbert - Initial contribution
 * @author Jan N. Klug - Refactored to OSGi service
 */
@NonNullByDefault
public abstract class AbstractScriptDependencyTracker
        implements ScriptDependencyTracker, WatchService.WatchEventListener {
    private final Logger logger = LoggerFactory.getLogger(AbstractScriptDependencyTracker.class);

    protected final Path libraryPath;

    private final Set<ScriptDependencyTracker.Listener> dependencyChangeListeners = ConcurrentHashMap.newKeySet();

    private final BidiSetBag<String, String> scriptToLibs = new BidiSetBag<>();
    private final WatchService watchService;

    protected AbstractScriptDependencyTracker(WatchService watchService, final String fileDirectory) {
        this.watchService = watchService;
        this.libraryPath = watchService.getWatchPath().resolve(fileDirectory);

        if (!Files.exists(libraryPath)) {
            try {
                Files.createDirectories(libraryPath);
            } catch (IOException e) {
                logger.warn("Failed to create watched directory: {}", libraryPath);
            }
        } else if (!Files.isDirectory(libraryPath)) {
            logger.warn("Trying to watch directory {}, however it is a file", libraryPath);
        }

        watchService.registerListener(this, this.libraryPath);
    }

    public void deactivate() {
        watchService.unregisterListener(this);
    }

    public Path getLibraryPath() {
        return libraryPath;
    }

    @Override
    public void processWatchEvent(WatchService.Kind kind, Path fullPath) {
        File file = fullPath.toFile();
        if (kind == DELETE || (!file.isHidden() && file.canRead() && (kind == CREATE || kind == MODIFY))) {
            dependencyChanged(file.toString());
        }
    }

    protected void dependencyChanged(String dependency) {
        Set<String> scripts = new HashSet<>(scriptToLibs.getKeys(dependency)); // take a copy as it will change as we
        logger.debug("Library {} changed; reimporting {} scripts...", libraryPath, scripts.size());
        for (String scriptUrl : scripts) {
            for (ScriptDependencyTracker.Listener listener : dependencyChangeListeners) {
                try {
                    listener.onDependencyChange(scriptUrl);
                } catch (Exception e) {
                    logger.warn("Failed to notify tracker of dependency change: {}: {}", e.getClass(), e.getMessage());
                }
            }
        }
    }

    @Override
    public Consumer<String> getTracker(String scriptId) {
        return dependencyPath -> startTracking(scriptId, dependencyPath);
    }

    @Override
    public void removeTracking(String scriptId) {
        scriptToLibs.removeKey(scriptId);
    }

    protected void startTracking(String scriptId, String libPath) {
        scriptToLibs.put(scriptId, libPath);
    }

    /**
     * Add a dependency change listener
     *
     * Since this is done via service injection and OSGi annotations are not inherited it is required that subclasses
     * expose this method with proper annotation
     *
     * @param listener the dependency change listener
     */
    public void addChangeTracker(ScriptDependencyTracker.Listener listener) {
        dependencyChangeListeners.add(listener);
    }

    public void removeChangeTracker(ScriptDependencyTracker.Listener listener) {
        dependencyChangeListeners.remove(listener);
    }
}
