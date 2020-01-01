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

import org.openhab.core.config.discovery.internal.DiscoveryResultImpl;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;

/**
 * @author Andre Fuechsel - Initial contribution
 */
public class DiscoveryServiceMockOfBridge extends DiscoveryServiceMock {

    ThingUID bridge;

    public DiscoveryServiceMockOfBridge(ThingTypeUID thingType, int timeout, ThingUID bridge) {
        super(thingType, timeout);
        this.bridge = bridge;
    }

    @Override
    public void startScan() {
        thingDiscovered(new DiscoveryResultImpl(new ThingUID(thingType, "test" + new Random().nextInt(999999999)),
                bridge, null, null, null, DEFAULT_TTL));
    }

    public ThingUID getBridge() {
        return bridge;
    }

}
