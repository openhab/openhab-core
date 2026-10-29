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
package org.openhab.core.voice.internal.dialog.trigger.ks;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.voice.dialog.trigger.ks.KSService;
import org.openhab.core.voice.dialog.trigger.ks.KSServiceRegistry;
import org.openhab.core.voice.internal.AbstractVoiceServiceRegistry;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * The {@link KSServiceRegistryImpl} is the implementation of the {@link KSServiceRegistry}.
 *
 * @author Florian Hotze - Initial contribution
 */
@Component(service = KSServiceRegistry.class, immediate = true)
@NonNullByDefault
public class KSServiceRegistryImpl extends AbstractVoiceServiceRegistry<KSService> implements KSServiceRegistry {

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addKSService(KSService ksService) {
        addService(ksService);
    }

    protected void removeKSService(KSService ksService) {
        removeService(ksService);
    }
}
