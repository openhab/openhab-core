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
package org.eclipse.smarthome.model.script.internal.engine.action;

import org.eclipse.smarthome.core.audio.AudioManager;
import org.eclipse.smarthome.model.script.actions.Audio;
import org.eclipse.smarthome.model.script.engine.action.ActionService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class AudioActionService implements ActionService {

    public static AudioManager audioManager;

    @Override
    public String getActionClassName() {
        return Audio.class.getCanonicalName();
    }

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
