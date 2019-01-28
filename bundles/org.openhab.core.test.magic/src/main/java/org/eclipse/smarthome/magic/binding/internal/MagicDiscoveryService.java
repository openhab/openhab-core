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
package org.eclipse.smarthome.magic.binding.internal;

import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.magic.binding.MagicBindingConstants;
import org.osgi.service.component.annotations.Component;

/**
 * The {@link MagicDiscoveryService} magically discovers magic things.
 *
 * @author Henning Treu - initial contribution
 *
 */
@NonNullByDefault
@Component(service = DiscoveryService.class, immediate = true)
public class MagicDiscoveryService extends AbstractDiscoveryService {

    public MagicDiscoveryService() throws IllegalArgumentException {
        super(new HashSet<>(Arrays.asList(MagicBindingConstants.THING_TYPE_CONFIG_THING)), 0);
    }

    @Override
    protected void startScan() {
        String serialNumber = createRandomSerialNumber();
        DiscoveryResult discoveryResult = DiscoveryResultBuilder
                .create(new ThingUID(MagicBindingConstants.THING_TYPE_CONFIG_THING, serialNumber))
                .withRepresentationProperty(Thing.PROPERTY_SERIAL_NUMBER)
                .withProperty(Thing.PROPERTY_SERIAL_NUMBER, serialNumber).withLabel("Magic Thing").build();
        thingDiscovered(discoveryResult);
    }

    private String createRandomSerialNumber() {
        return UUID.randomUUID().toString();
    }

}
