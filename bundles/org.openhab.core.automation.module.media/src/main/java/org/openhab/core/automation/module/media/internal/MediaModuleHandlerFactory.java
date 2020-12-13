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
package org.openhab.core.automation.module.media.internal;

import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioManager;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.handler.BaseModuleHandlerFactory;
import org.openhab.core.automation.handler.ModuleHandler;
import org.openhab.core.automation.handler.ModuleHandlerFactory;
import org.openhab.core.voice.VoiceManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Christoph Weitkamp - Added parameter volume
 */
@NonNullByDefault
@Component(service = ModuleHandlerFactory.class)
public class MediaModuleHandlerFactory extends BaseModuleHandlerFactory {

    private static final Collection<String> TYPES = List.of(SayActionHandler.TYPE_ID, PlayActionHandler.TYPE_ID);
    private final VoiceManager voiceManager;
    private final AudioManager audioManager;

    @Activate
    public MediaModuleHandlerFactory(final @Reference AudioManager audioManager,
            final @Reference VoiceManager voiceManager) {
        this.audioManager = audioManager;
        this.voiceManager = voiceManager;
    }

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
    protected @Nullable ModuleHandler internalCreate(Module module, String ruleUID) {
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
}
