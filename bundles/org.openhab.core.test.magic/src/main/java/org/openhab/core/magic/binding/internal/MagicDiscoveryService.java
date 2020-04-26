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
package org.openhab.core.magic.binding.internal;

import static org.openhab.core.magic.binding.MagicBindingConstants.THING_TYPE_CONFIG_THING;

import java.util.Collections;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Component;

/**
 * The {@link MagicDiscoveryService} magically discovers magic things.
 *
 * @author Henning Treu - Initial contribution
 */
@NonNullByDefault
@Component(service = DiscoveryService.class, immediate = true)
public class MagicDiscoveryService extends AbstractDiscoveryService {

    public MagicDiscoveryService() throws IllegalArgumentException {
        super(Collections.singleton(THING_TYPE_CONFIG_THING), 0);
    }

    @Override
    protected void startScan() {
        String serialNumber = createRandomSerialNumber();
        DiscoveryResult discoveryResult = DiscoveryResultBuilder
                .create(new ThingUID(THING_TYPE_CONFIG_THING, serialNumber))
                .withRepresentationProperty(Thing.PROPERTY_SERIAL_NUMBER)
                .withProperty(Thing.PROPERTY_SERIAL_NUMBER, serialNumber).withLabel("Magic Thing").build();
        thingDiscovered(discoveryResult);
    }

    private String createRandomSerialNumber() {
        return UUID.randomUUID().toString();
    }
}
