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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.ScriptExtensionProvider;
import org.osgi.service.component.annotations.Component;

/**
 * ScriptExtensionProvider which providers a 'lifecycleTracker' object allowing scripts to register for disposal events.
 *
 * @author Jonathan Gilbert - Initial contribution
 */
@Component(immediate = true)
@NonNullByDefault
public class LifecycleScriptExtensionProvider implements ScriptExtensionProvider {

    private static final String LIFECYCLE_PRESET_NAME = "lifecycle";
    private static final String LIFECYCLE_TRACKER_NAME = "lifecycleTracker";

    private final Map<String, LifecycleTracker> idToTracker = new ConcurrentHashMap<>();

    @Override
    public Collection<String> getDefaultPresets() {
        return Set.of(LIFECYCLE_PRESET_NAME);
    }

    @Override
    public Collection<String> getPresets() {
        return Set.of(LIFECYCLE_PRESET_NAME);
    }

    @Override
    public Collection<String> getTypes() {
        return Set.of(LIFECYCLE_TRACKER_NAME);
    }

    @Override
    public @Nullable Object get(String scriptIdentifier, String type) throws IllegalArgumentException {
        if (LIFECYCLE_TRACKER_NAME.equals(type)) {
            return idToTracker.computeIfAbsent(scriptIdentifier, k -> new LifecycleTracker());
        }

        return null;
    }

    @Override
    public Map<String, Object> importPreset(String scriptIdentifier, String preset) {
        if (LIFECYCLE_PRESET_NAME.equals(preset)) {
            final Object requestedType = get(scriptIdentifier, LIFECYCLE_TRACKER_NAME);
            if (requestedType != null) {
                return Map.of(LIFECYCLE_TRACKER_NAME, requestedType);
            }
        }

        return Collections.emptyMap();
    }

    @Override
    public void unload(String scriptIdentifier) {
        LifecycleTracker tracker = idToTracker.remove(scriptIdentifier);
        if (tracker != null) {
            tracker.dispose();
        }
    }

    public static class LifecycleTracker {
        List<Disposable> disposables = new ArrayList<>();

        public void addDisposeHook(Disposable disposable) {
            disposables.add(disposable);
        }

        void dispose() {
            for (Disposable disposable : disposables) {
                disposable.dispose();
            }
        }
    }

    @FunctionalInterface
    interface Disposable {
        void dispose();
    }
}
