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
package org.openhab.core.automation.module.script.rulesupport.loader;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.ScriptDependencyTracker;
import org.openhab.core.automation.module.script.rulesupport.internal.loader.ScriptLibraryWatcher;
import org.openhab.core.automation.module.script.rulesupport.internal.loader.collection.BidiSetBag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AbstractScriptDependencyTracker} tracks dependencies between scripts and reloads dependees
 * It needs to be sub-classed for each {@link org.openhab.core.automation.module.script.ScriptEngineFactory}
 * that wants to support dependency tracking
 *
 * @author Jonathan Gilbert - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractScriptDependencyTracker implements ScriptDependencyTracker {
    private final Logger logger = LoggerFactory.getLogger(AbstractScriptDependencyTracker.class);

    protected final String libraryPath;

    private final Set<ScriptDependencyTracker.Listener> dependencyChangeListeners = ConcurrentHashMap.newKeySet();

    private final BidiSetBag<String, String> scriptToLibs = new BidiSetBag<>();
    private @Nullable ScriptLibraryWatcher scriptLibraryWatcher;

    public AbstractScriptDependencyTracker(final String libraryPath) {
        this.libraryPath = libraryPath;
    }

    public void activate() {
        ScriptLibraryWatcher scriptLibraryWatcher = createScriptLibraryWatcher();
        scriptLibraryWatcher.activate();
        this.scriptLibraryWatcher = scriptLibraryWatcher;
    }

    public void deactivate() {
        ScriptLibraryWatcher scriptLibraryWatcher = this.scriptLibraryWatcher;
        if (scriptLibraryWatcher != null) {
            scriptLibraryWatcher.deactivate();
        }
    }

    protected ScriptLibraryWatcher createScriptLibraryWatcher() {
        return new ScriptLibraryWatcher(libraryPath, this::dependencyChanged);
    }

    protected void dependencyChanged(String dependency) {
        Set<String> scripts = new HashSet<>(scriptToLibs.getKeys(dependency)); // take a copy as it will change as we
        AbstractScriptDependencyTracker.this.logger.debug("Library {} changed; reimporting {} scripts...", libraryPath,
                scripts.size());
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
     * Since this is done via service injection and OSGi annotations are not inherited it is required that sub-classes
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
