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

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.ScriptDependencyTracker;
import org.openhab.core.automation.module.script.rulesupport.internal.loader.BidiSetBag;
import org.openhab.core.service.AbstractWatchService;
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
public abstract class AbstractScriptDependencyTracker implements ScriptDependencyTracker {
    private final Logger logger = LoggerFactory.getLogger(AbstractScriptDependencyTracker.class);

    protected final String libraryPath;

    private final Set<ScriptDependencyTracker.Listener> dependencyChangeListeners = ConcurrentHashMap.newKeySet();

    private final BidiSetBag<String, String> scriptToLibs = new BidiSetBag<>();
    private @Nullable AbstractWatchService dependencyWatchService;

    public AbstractScriptDependencyTracker(final String libraryPath) {
        this.libraryPath = libraryPath;
    }

    public void activate() {
        AbstractWatchService dependencyWatchService = createDependencyWatchService();
        dependencyWatchService.activate();
        this.dependencyWatchService = dependencyWatchService;
    }

    public void deactivate() {
        AbstractWatchService dependencyWatchService = this.dependencyWatchService;
        if (dependencyWatchService != null) {
            dependencyWatchService.deactivate();
        }
    }

    protected AbstractWatchService createDependencyWatchService() {
        return new AbstractWatchService(libraryPath) {
            @Override
            protected boolean watchSubDirectories() {
                return true;
            }

            @Override
            protected WatchEvent.Kind<?> @Nullable [] getWatchEventKinds(Path path) {
                return new WatchEvent.Kind<?>[] { ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY };
            }

            @Override
            protected void processWatchEvent(WatchEvent<?> watchEvent, WatchEvent.Kind<?> kind, Path path) {
                File file = path.toFile();
                if (!file.isHidden() && (kind.equals(ENTRY_DELETE)
                        || (file.canRead() && (kind.equals(ENTRY_CREATE) || kind.equals(ENTRY_MODIFY))))) {
                    dependencyChanged(file.getPath());
                }
            }
        };
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
