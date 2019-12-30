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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests for class {@link ChannelGroupUID}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
public class ChannelGroupUIDTest {

    private static final String BINDING_ID = "binding";
    private static final String THING_TYPE_ID = "thing-type";
    private static final String THING_ID = "thing";
    private static final String GROUP_ID = "group";

    private static final ThingUID THING_UID = new ThingUID(BINDING_ID, THING_TYPE_ID, THING_ID);

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidCharacters() {
        new ChannelGroupUID(THING_UID, "id_with_invalidchar%");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotEnoughNumberOfSegments() {
        new ChannelUID("binding:thing-type:group");
    }

    @Test
    public void testChannelGroupUID() {
        ChannelGroupUID channelGroupUID = new ChannelGroupUID(THING_UID, GROUP_ID);
        assertEquals("binding:thing-type:thing:group", channelGroupUID.toString());
        assertEquals(GROUP_ID, channelGroupUID.getId());
        assertEquals(THING_UID, channelGroupUID.getThingUID());
    }
}
