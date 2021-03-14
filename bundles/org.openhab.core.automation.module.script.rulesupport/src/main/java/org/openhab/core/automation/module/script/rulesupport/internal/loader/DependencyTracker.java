/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.automation.module.script.rulesupport.internal.loader;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.openhab.core.automation.module.script.rulesupport.internal.loader.collection.BidiSetBag;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks dependencies between scripts and reloads dependees
 *
 * @author Jonathan Gilbert
 */
@Component(immediate = true, service = DependencyTracker.class)
public class DependencyTracker {

    private final Logger logger = LoggerFactory.getLogger(DependencyTracker.class);

    private final Set<DependencyChangeListener> dependencyChangeListeners = ConcurrentHashMap.newKeySet();

    private final BidiSetBag<String, String> scriptToLibs = new BidiSetBag<>();
    private final ScriptLibraryWatcher scriptLibraryWatcher = new ScriptLibraryWatcher() {
        @Override
        void updateFile(String libraryPath) {
            Set<String> scripts;
            synchronized (scriptToLibs) {
                scripts = new HashSet<>(scriptToLibs.getKeys(libraryPath)); // take a copy as it will change as we
                                                                            // reimport
            }
            DependencyTracker.this.logger.debug("Library {} changed; reimporting {} scripts...", libraryPath,
                    scripts.size());
            for (String scriptUrl : scripts) {
                reimportScript(scriptUrl);
            }
        }
    };

    @Activate
    public void activate() {
        scriptLibraryWatcher.activate();
    }

    @Deactivate
    public void deactivate() {
        scriptLibraryWatcher.deactivate();
    }

    public void addLibForScript(String scriptPath, String libPath) {
        synchronized (scriptToLibs) {
            scriptToLibs.put(scriptPath, libPath);
        }
    }

    public void removeScript(String scriptPath) {
        synchronized (scriptToLibs) {
            scriptToLibs.removeKey(scriptPath);
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, unbind = "removeChangeTracker")
    public void addChangeTracker(DependencyChangeListener listener) {
        dependencyChangeListeners.add(listener);
    }

    public void removeChangeTracker(DependencyChangeListener listener) {
        dependencyChangeListeners.remove(listener);
    }

    public void reimportScript(String scriptPath) {
        for (DependencyChangeListener listener : dependencyChangeListeners) {
            try {
                listener.onDependencyChange(scriptPath);
            } catch (Exception e) {
                logger.warn("Failed to notify tracker of dependency change: {}: {}", e.getClass(), e.getMessage());
            }
        }
    }

    public interface DependencyChangeListener {
        void onDependencyChange(String scriptPath);
    }
}
