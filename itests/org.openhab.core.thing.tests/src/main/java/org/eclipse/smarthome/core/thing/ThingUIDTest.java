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
package org.eclipse.smarthome.core.thing;

import static org.junit.Assert.*;

import org.junit.Test;

public class ThingUIDTest {

    @Test
    public void testThreeSegments() {
        ThingTypeUID thingType = new ThingTypeUID("fake", "type");
        ThingUID t = new ThingUID(thingType, "gaga");

        assertEquals("type", t.getThingTypeId());
        assertEquals("gaga", t.getId());
        assertEquals("fake:type:gaga", t.getAsString());
    }

    @Test
    public void testTwoSegments() {
        ThingUID t = new ThingUID("fake", "gaga");

        assertNull(t.getThingTypeId());
        assertEquals("gaga", t.getId());
        assertEquals("fake::gaga", t.getAsString());
    }

    @Test
    public void testGetBridgeIds() {
        ThingTypeUID thingType = new ThingTypeUID("fake", "type");
        ThingUID t = new ThingUID(thingType, new ThingUID("fake", "something", "bridge"), "thing");

        assertEquals("fake:type:bridge:thing", t.getAsString());
        assertEquals(1, t.getBridgeIds().size());
        assertEquals("bridge", t.getBridgeIds().get(0));
    }
}
