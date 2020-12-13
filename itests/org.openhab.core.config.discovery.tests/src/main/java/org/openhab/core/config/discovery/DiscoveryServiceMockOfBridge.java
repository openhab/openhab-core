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
package org.openhab.core.config.discovery;

import java.util.Random;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;

/**
 * @author Andre Fuechsel - Initial contribution
 */
@NonNullByDefault
public class DiscoveryServiceMockOfBridge extends DiscoveryServiceMock {

    final ThingUID bridgeUID;

    public DiscoveryServiceMockOfBridge(ThingTypeUID thingType, int timeout, ThingUID bridgeUID) {
        super(thingType, timeout);
        this.bridgeUID = bridgeUID;
    }

    @Override
    public void startScan() {
        if (faulty) {
            throw new RuntimeException();
        }
        thingDiscovered(DiscoveryResultBuilder
                .create(new ThingUID(thingType, bridgeUID, "test" + new Random().nextInt(999999999)))
                .withBridge(bridgeUID).withTTL(DEFAULT_TTL).build());
    }

    public ThingUID getBridge() {
        return bridgeUID;
    }
}
