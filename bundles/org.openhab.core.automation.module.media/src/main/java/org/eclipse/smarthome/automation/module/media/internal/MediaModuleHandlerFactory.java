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
package org.eclipse.smarthome.automation.module.media.internal;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import java.util.Collection;

import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.Module;
import org.eclipse.smarthome.automation.handler.BaseModuleHandlerFactory;
import org.eclipse.smarthome.automation.handler.ModuleHandler;
import org.eclipse.smarthome.automation.handler.ModuleHandlerFactory;
import org.eclipse.smarthome.automation.module.media.handler.PlayActionHandler;
import org.eclipse.smarthome.automation.module.media.handler.SayActionHandler;
import org.eclipse.smarthome.core.audio.AudioManager;
import org.eclipse.smarthome.core.voice.VoiceManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 *
 * @author Kai Kreuzer - Initial contribution
 */
@Component(service = ModuleHandlerFactory.class)
public class MediaModuleHandlerFactory extends BaseModuleHandlerFactory {

    private static final Collection<String> TYPES = unmodifiableList(
            asList(SayActionHandler.TYPE_ID, PlayActionHandler.TYPE_ID));
    private VoiceManager voiceManager;
    private AudioManager audioManager;

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public Collection<String> getTypes() {
        return TYPES;
    }

    @Override
    protected ModuleHandler internalCreate(Module module, String ruleUID) {
        if (module instanceof Action) {
            switch (module.getTypeUID()) {
                case SayActionHandler.TYPE_ID:
                    return new SayActionHandler((Action) module, voiceManager);
                case PlayActionHandler.TYPE_ID:
                    return new PlayActionHandler((Action) module, audioManager);
                default:
                    break;
            }
        }
        return null;
    }

    @Reference
    protected void setAudioManager(AudioManager audioManager) {
        this.audioManager = audioManager;
    }

    protected void unsetAudioManager(AudioManager audioManager) {
        this.audioManager = null;
    }

    @Reference
    protected void setVoiceManager(VoiceManager voiceManager) {
        this.voiceManager = voiceManager;
    }

    protected void unsetVoiceManager(VoiceManager voiceManager) {
        this.voiceManager = null;
    }
}
