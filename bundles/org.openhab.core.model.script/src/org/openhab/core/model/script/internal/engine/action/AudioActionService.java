/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
import org.openhab.core.audio.AudioManager;
import org.openhab.core.model.script.actions.Audio;
import org.openhab.core.model.script.engine.action.ActionService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
@NonNullByDefault
public class AudioActionService implements ActionService {

    public static @Nullable AudioManager audioManager;

    @Activate
    public AudioActionService(final @Reference AudioManager audioManager) {
        AudioActionService.audioManager = audioManager;
    }

    @Override
    public Class<?> getActionClass() {
        return Audio.class;
    }
}
