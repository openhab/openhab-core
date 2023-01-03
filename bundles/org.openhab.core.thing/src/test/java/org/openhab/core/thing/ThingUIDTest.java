/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.thing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * @author Stefan Triller - Initial contribution
 */
@NonNullByDefault
public class ThingUIDTest {

    @Test
    public void testThreeSegments() {
        ThingTypeUID thingType = new ThingTypeUID("fake", "type");
        ThingUID subject = new ThingUID(thingType, "thing");

        assertEquals("fake", subject.getBindingId());
        assertEquals("thing", subject.getId());
        assertThat(subject.getAllSegments(), hasSize(3));
        assertEquals("fake:type:thing", subject.getAsString());
    }

    @Test
    public void testTwoSegments() {
        ThingUID subject = new ThingUID("fake", "thing");

        assertEquals("fake", subject.getBindingId());
        assertEquals("thing", subject.getId());
        assertThat(subject.getAllSegments(), hasSize(3));
        assertEquals("fake::thing", subject.getAsString());
    }

    @Test
    public void testGetBridgeIds() {
        ThingTypeUID thingType = new ThingTypeUID("fake", "type");
        ThingUID subject = new ThingUID(thingType, new ThingUID("fake", "something", "bridge"), "thing");

        assertEquals("fake:type:bridge:thing", subject.getAsString());
        assertThat(subject.getBridgeIds(), hasSize(1));
        assertEquals("bridge", subject.getBridgeIds().get(0));
    }
}
