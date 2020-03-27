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
package org.openhab.core.automation.module.script.internal.defaultscope;

import org.openhab.core.automation.module.script.ScriptExtensionProvider;
import org.osgi.service.component.annotations.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ScriptExtensionProvider which providers a 'lifecycleTracker' object allowing scripts to register for disposal events.
 *
 * @author Jonathan Gilbert - Initial contribution
 */
@Component(immediate = true)
public class LifecycleScriptExtensionProvider implements ScriptExtensionProvider {

    private static final String LIFECYCLE_PRESET_NAME = "lifecycle";
    private static final String LIFECYCLE_TRACKER_NAME = "lifecycleTracker";

    private Map<String, LifecycleTracker> idToTracker= new ConcurrentHashMap<>();

    @Override
    public Collection<String> getDefaultPresets() {
        return Collections.singleton(LIFECYCLE_PRESET_NAME);
    }

    @Override
    public Collection<String> getPresets() {
        return Collections.singleton(LIFECYCLE_PRESET_NAME);
    }

    @Override
    public Collection<String> getTypes() {
        return Collections.singleton(LIFECYCLE_TRACKER_NAME);
    }

    @Override
    public Object get(String scriptIdentifier, String type) throws IllegalArgumentException {
        if(LIFECYCLE_TRACKER_NAME.equals(type)) {
            return idToTracker.computeIfAbsent(scriptIdentifier, k -> new LifecycleTracker());
        }

        return null;
    }

    @Override
    public Map<String, Object> importPreset(String scriptIdentifier, String preset) {
        if(LIFECYCLE_PRESET_NAME.equals(preset)) {
            return Collections.singletonMap(LIFECYCLE_TRACKER_NAME, get(scriptIdentifier, LIFECYCLE_TRACKER_NAME));
        }

        return Collections.emptyMap();
    }

    @Override
    public void unload(String scriptIdentifier) {
        LifecycleTracker tracker = idToTracker.remove(scriptIdentifier);

        if(tracker != null) {
            tracker.dispose();
        }
    }

    public static class LifecycleTracker {
        List<Disposable> disposables = new ArrayList<>();

        public void addDisposeHook(Disposable disposable) {
            disposables.add(disposable);
        }

        void dispose() {
            for(Disposable disposable : disposables) {
                disposable.dispose();
            }
        }
    }

    @FunctionalInterface
    interface Disposable {
        void dispose();
    }
}
