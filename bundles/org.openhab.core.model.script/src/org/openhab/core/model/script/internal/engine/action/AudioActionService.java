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
package org.openhab.core.model.script.internal.engine.action;

import org.openhab.core.audio.AudioManager;
import org.openhab.core.model.script.actions.Audio;
import org.openhab.core.model.script.engine.action.ActionService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class AudioActionService implements ActionService {

    public static AudioManager audioManager;

    @Override
    public Class<?> getActionClass() {
        return Audio.class;
    }

    @Reference
    protected void setAudioManager(AudioManager audioManager) {
        AudioActionService.audioManager = audioManager;
    }

    protected void unsetAudioManager(AudioManager audioManager) {
        AudioActionService.audioManager = null;
    }

}
