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
package org.openhab.core.model.script.internal.engine.action;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.model.script.actions.Voice;
import org.openhab.core.model.script.engine.action.ActionService;
import org.openhab.core.voice.VoiceManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This class registers an OSGi service for the voice action.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true)
public class VoiceActionService implements ActionService {

    public static @Nullable VoiceManager voiceManager;

    @Activate
    public VoiceActionService(final @Reference VoiceManager voiceManager) {
        VoiceActionService.voiceManager = voiceManager;
    }

    @Override
    public Class<?> getActionClass() {
        return Voice.class;
    }
}
