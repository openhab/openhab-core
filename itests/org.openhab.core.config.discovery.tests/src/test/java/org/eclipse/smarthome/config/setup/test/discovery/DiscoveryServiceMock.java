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
package org.eclipse.smarthome.config.setup.test.discovery;

import java.util.Collections;
import java.util.Random;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.config.discovery.internal.DiscoveryResultImpl;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;

/**
 * The {@link DiscoveryServiceMock} is a mock for a {@link DiscoveryService}
 * which can simulate a working and faulty discovery.<br>
 * If this mock is configured to be faulty, an exception is thrown if the
 * discovery is enforced or aborted.
 *
 * @author Michael Grammling - Initial Contribution
 * @author Thomas Höfer - Added representation
 */
public class DiscoveryServiceMock extends AbstractDiscoveryService {

    public static final int DEFAULT_TTL = 60;

    ThingTypeUID thingType;
    int timeout;
    boolean faulty;

    public DiscoveryServiceMock(ThingTypeUID thingType, int timeout) {
        this(thingType, timeout, false);
    }

    public DiscoveryServiceMock(ThingTypeUID thingType, int timeout, boolean faulty) {
        super(Collections.singleton(thingType), timeout);
        this.thingType = thingType;
        this.faulty = faulty;
    }

    @Override
    public void startScan() {
        if (faulty) {
            throw new RuntimeException();
        }
        thingDiscovered(new DiscoveryResultImpl(new ThingUID(thingType, "test" + new Random().nextInt(999999999)), null,
                null, null, null, DEFAULT_TTL));
    }

}
