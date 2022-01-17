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

import org.openhab.core.automation.module.script.rulesupport.internal.loader.ScriptLibraryWatcher;
import org.openhab.core.automation.module.script.rulesupport.internal.loader.collection.BidiSetBag;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks dependencies between scripts and reloads dependees
 *
 * @author Jonathan Gilbert - Initial contribution
 */
public class DependencyTracker {

    public String libraryPath;

    private final Logger logger = LoggerFactory.getLogger(DependencyTracker.class);

    private final Set<DependencyChangeListener> dependencyChangeListeners = ConcurrentHashMap.newKeySet();

    private final BidiSetBag<String, String> scriptToLibs = new BidiSetBag<>();
    private ScriptLibraryWatcher scriptLibraryWatcher;

    public DependencyTracker(final String libraryPath) {
        this.libraryPath = libraryPath;
    }

    public void activate() {
        createScriptLibraryWatcher();
        scriptLibraryWatcher.activate();
    }

    public void deactivate() {
        scriptLibraryWatcher.deactivate();
    }

    private void createScriptLibraryWatcher() {
        scriptLibraryWatcher = new ScriptLibraryWatcher(libraryPath) {
            @Override
            protected void updateFile(String libraryPath) {
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
