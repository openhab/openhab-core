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
package org.openhab.core.audio.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSinkRegistry;
import org.openhab.core.common.registry.AbstractServiceRegistry;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * The {@link AudioSinkRegistryImpl} is the implementation of the {@link AudioSinkRegistry}.
 *
 * @author Florian Hotze - Initial contribution
 */
@Component(immediate = true)
@NonNullByDefault
public class AudioSinkRegistryImpl extends AbstractServiceRegistry<AudioSink, String> implements AudioSinkRegistry {

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addAudioSink(AudioSink audioSink) {
        addService(audioSink);
    }

    protected void removeAudioSink(AudioSink audioSink) {
        removeService(audioSink);
    }
}
