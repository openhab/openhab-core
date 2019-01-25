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
package org.openhab.core.automation.module.media.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.core.audio.AudioManager;
import org.eclipse.smarthome.core.voice.VoiceManager;
import org.openhab.core.automation.module.script.ScriptExtensionProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This is a scope provider for features that are related to audio and voice support.
 *
 * @author Kai Kreuzer - Initial contribution
 *
 */
@Component
public class MediaScriptScopeProvider implements ScriptExtensionProvider {
    Map<String, Object> elements = new HashMap<>();

    @Reference
    protected void setAudioManager(AudioManager audioManager) {
        elements.put("audio", audioManager);
    }

    protected void unsetAudioManager(AudioManager audioManager) {
        elements.remove("audio");
    }

    @Reference
    protected void setVoiceManager(VoiceManager voiceManager) {
        elements.put("voice", voiceManager);
    }

    protected void unsetVoiceManager(VoiceManager voiceManager) {
        elements.remove("voice");
    }

    @Override
    public Collection<String> getDefaultPresets() {
        return Collections.singleton("media");
    }

    @Override
    public Collection<String> getPresets() {
        return Collections.singleton("media");
    }

    @Override
    public Collection<String> getTypes() {
        return elements.keySet();
    }

    @Override
    public Object get(String scriptIdentifier, String type) {
        return elements.get(type);
    }

    @Override
    public Map<String, Object> importPreset(String scriptIdentifier, String preset) {
        return elements;
    }

    @Override
    public void unload(String scriptIdentifier) {
    }

}
