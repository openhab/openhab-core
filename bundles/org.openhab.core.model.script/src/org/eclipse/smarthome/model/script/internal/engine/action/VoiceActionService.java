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

import org.eclipse.smarthome.core.voice.VoiceManager;
import org.eclipse.smarthome.model.script.actions.Voice;
import org.eclipse.smarthome.model.script.engine.action.ActionService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This class registers an OSGi service for the voice action.
 *
 * @author Kai Kreuzer - Initial contribution and API
 */
@Component(immediate = true)
public class VoiceActionService implements ActionService {

    public static VoiceManager voiceManager;

    @Override
    public String getActionClassName() {
        return Voice.class.getCanonicalName();
    }

    @Override
    public Class<?> getActionClass() {
        return Voice.class;
    }

    @Reference
    protected void setVoiceManager(VoiceManager voiceManager) {
        VoiceActionService.voiceManager = voiceManager;
    }

    protected void unsetVoiceManager(VoiceManager voiceManager) {
        VoiceActionService.voiceManager = null;
    }

}
