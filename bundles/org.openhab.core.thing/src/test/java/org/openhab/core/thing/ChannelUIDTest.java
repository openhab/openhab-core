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
package org.openhab.core.thing;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Tests for class {@link ChannelUID}.
 *
 * @author Simon Kaufmann - Initial contribution
 * @author Christoph Weitkamp - Changed pattern for validating last segment to contain either a single `#` or none
 */
public class ChannelUIDTest {

    private static final String BINDING_ID = "binding";
    private static final String THING_TYPE_ID = "thing-type";
    private static final String THING_ID = "thing";
    private static final String GROUP_ID = "group";
    private static final String CHANNEL_ID = "id";
    private static final ThingUID THING_UID = new ThingUID(BINDING_ID, THING_TYPE_ID, THING_ID);

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidCharacters() {
        new ChannelUID(THING_UID, "id_with_invalidchar%");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotEnoughNumberOfSegments() {
        new ChannelUID("binding:thing-type:group#id");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMultipleChannelGroupSeparators() {
        new ChannelUID("binding:thing-type:thing:group#id#what_ever");
    }

    @Test
    public void testChannelUID() {
        ChannelUID channelUID = new ChannelUID(THING_UID, CHANNEL_ID);
        assertEquals("binding:thing-type:thing:id", channelUID.toString());
        assertFalse(channelUID.isInGroup());
        assertEquals(CHANNEL_ID, channelUID.getId());
        assertEquals(CHANNEL_ID, channelUID.getIdWithoutGroup());
        assertNull(channelUID.getGroupId());
        assertEquals(THING_UID, channelUID.getThingUID());
    }

    @Test
    public void testChannelUIDWithGroup() {
        ChannelUID channelUID = new ChannelUID(THING_UID, GROUP_ID, CHANNEL_ID);
        assertEquals("binding:thing-type:thing:group#id", channelUID.toString());
        assertTrue(channelUID.isInGroup());
        assertEquals("group#id", channelUID.getId());
        assertEquals(CHANNEL_ID, channelUID.getIdWithoutGroup());
        assertEquals(GROUP_ID, channelUID.getGroupId());
        assertEquals(THING_UID, channelUID.getThingUID());
    }
}
