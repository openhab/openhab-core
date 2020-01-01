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

import org.openhab.core.voice.VoiceManager;
import org.openhab.core.model.script.actions.Voice;
import org.openhab.core.model.script.engine.action.ActionService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This class registers an OSGi service for the voice action.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@Component(immediate = true)
public class VoiceActionService implements ActionService {

    public static VoiceManager voiceManager;

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
