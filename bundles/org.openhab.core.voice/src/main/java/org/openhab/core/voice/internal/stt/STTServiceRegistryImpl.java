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
package org.openhab.core.voice.internal.stt;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.voice.internal.AbstractVoiceServiceRegistry;
import org.openhab.core.voice.stt.STTService;
import org.openhab.core.voice.stt.STTServiceRegistry;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * The {@link STTServiceRegistryImpl} is the implementation of the {@link STTServiceRegistry}.
 *
 * @author Florian Hotze - Initial contribution
 */
@Component(immediate = true)
@NonNullByDefault
public class STTServiceRegistryImpl extends AbstractVoiceServiceRegistry<STTService> implements STTServiceRegistry {

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addSTTService(STTService sttService) {
        addService(sttService);
    }

    protected void removeSTTService(STTService sttService) {
        removeService(sttService);
    }
}
