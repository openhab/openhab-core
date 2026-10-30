/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.voice.internal.tts;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.voice.internal.AbstractVoiceServiceRegistry;
import org.openhab.core.voice.tts.TTSService;
import org.openhab.core.voice.tts.TTSServiceRegistry;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * The {@link TTSServiceRegistryImpl} is the implementation of the {@link TTSServiceRegistry}.
 *
 * @author Florian Hotze - Initial contribution
 */
@Component(immediate = true)
@NonNullByDefault
public class TTSServiceRegistryImpl extends AbstractVoiceServiceRegistry<TTSService> implements TTSServiceRegistry {

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addTTSService(TTSService ttsService) {
        addService(ttsService);
    }

    protected void removeTTSService(TTSService ttsService) {
        removeService(ttsService);
    }
}
