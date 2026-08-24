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
package org.openhab.core.model.yaml.internal.things;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * The {@link YamlThingDTOTest} contains tests for the {@link YamlThingDTO} class.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class YamlThingDTOTest {

    @Test
    public void testIsValid() throws IOException {
        YamlThingDTO th = new YamlThingDTO();
        assertFalse(th.isValid(null, null));

        th.uid = "id";
        assertFalse(th.isValid(null, null));

        th.uid = "binding:id";
        assertFalse(th.isValid(null, null));

        th.uid = "binding:type:@id";
        assertFalse(th.isValid(null, null));

        th.uid = "binding:type:id";
        assertTrue(th.isValid(null, null));

        th.uid = "binding:type:$idBridge:id";
        assertFalse(th.isValid(null, null));

        th.uid = "binding:type:idBridge:id";
        assertTrue(th.isValid(null, null));

        th.bridge = "idBridge";
        assertFalse(th.isValid(null, null));

        th.bridge = "binding:idBridge";
        assertFalse(th.isValid(null, null));

        th.bridge = "binding:type:id#Bridge";
        assertFalse(th.isValid(null, null));

        th.bridge = "binding:type:idBridge";
        assertTrue(th.isValid(null, null));

        YamlChannelDTO ch = new YamlChannelDTO();
        th.channels = Map.of("channel", ch);
        assertFalse(th.isValid(null, null));

        ch.type = "channel-type";
        th.channels = Map.of("channel", ch);
        assertTrue(th.isValid(null, null));

        th.channels = Map.of("channel@name", ch);
        assertFalse(th.isValid(null, null));

        th.channels = Map.of("group#channel", ch);
        assertTrue(th.isValid(null, null));
    }

    @Test
    public void testEquals() throws IOException {
        YamlThingDTO th1 = new YamlThingDTO();
        YamlThingDTO th2 = new YamlThingDTO();

        th1.uid = "binding:type:id";
        th2.uid = "binding:type:id2";
        assertFalse(th1.equals(th2));
        th2.uid = "binding:type:id";
        assertTrue(th1.equals(th2));
        assertEquals(th1.hashCode(), th2.hashCode());

        th1.isBridge = true;
        th2.isBridge = true;
        assertTrue(th1.equals(th2));
        assertEquals(th1.hashCode(), th2.hashCode());

        th1.bridge = "binding:bridge:idBridge";
        th2.bridge = "binding:bridge:idBridge";
        assertTrue(th1.equals(th2));
        assertEquals(th1.hashCode(), th2.hashCode());

        th1.label = "A label";
        th2.label = "A label";
        assertTrue(th1.equals(th2));
        assertEquals(th1.hashCode(), th2.hashCode());

        th1.location = "A location";
        th2.location = "A location";
        assertTrue(th1.equals(th2));
        assertEquals(th1.hashCode(), th2.hashCode());

        th1.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
        th2.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
        assertTrue(th1.equals(th2));
        assertEquals(th1.hashCode(), th2.hashCode());

        YamlChannelDTO ch1 = new YamlChannelDTO();
        ch1.type = "channel-type";
        YamlChannelDTO ch2 = new YamlChannelDTO();
        ch2.type = "channel-type";
        th1.channels = Map.of("channel", ch1);
        th2.channels = Map.of("channel", ch2);
        assertTrue(th1.equals(th2));
        assertEquals(th1.hashCode(), th2.hashCode());
    }

    @Test
    public void testEqualsWithLabel() throws IOException {
        YamlThingDTO th1 = new YamlThingDTO();
        YamlThingDTO th2 = new YamlThingDTO();

        th1.uid = "binding:type:id";
        th2.uid = "binding:type:id";

        th1.label = null;
        th2.label = null;
        assertTrue(th1.equals(th2));
        assertEquals(th1.hashCode(), th2.hashCode());
        th1.label = "A label";
        th2.label = null;
        assertFalse(th1.equals(th2));
        th1.label = null;
        th2.label = "A label";
        assertFalse(th1.equals(th2));
        th1.label = "A label";
        th2.label = "A different label";
        assertFalse(th1.equals(th2));
        th1.label = "A label";
        th2.label = "A label";
        assertTrue(th1.equals(th2));
        assertEquals(th1.hashCode(), th2.hashCode());
    }

    @Test
    public void testEqualsWithLocation() throws IOException {
        YamlThingDTO th1 = new YamlThingDTO();
        YamlThingDTO th2 = new YamlThingDTO();

        th1.uid = "binding:type:id";
        th2.uid = "binding:type:id";

        th1.location = null;
        th2.location = null;
        assertTrue(th1.equals(th2));
        assertEquals(th1.hashCode(), th2.hashCode());
        th1.location = "A location";
        th2.location = null;
        assertFalse(th1.equals(th2));
        th1.location = null;
        th2.location = "A location";
        assertFalse(th1.equals(th2));
        th1.location = "A location";
        th2.location = "A different location";
        assertFalse(th1.equals(th2));
        th1.location = "A location";
        th2.location = "A location";
        assertTrue(th1.equals(th2));
        assertEquals(th1.hashCode(), th2.hashCode());
    }

    @Test
    public void testEqualsWithIsBridge() throws IOException {
        YamlThingDTO th1 = new YamlThingDTO();
        YamlThingDTO th2 = new YamlThingDTO();

        th1.uid = "binding:type:id";
        th2.uid = "binding:type:id";

        th1.isBridge = null;
        th2.isBridge = null;
        assertTrue(th1.equals(th2));
        assertEquals(th1.hashCode(), th2.hashCode());
        th1.isBridge = false;
        th2.isBridge = true;
        assertFalse(th1.equals(th2));
        th1.isBridge = true;
        th2.isBridge = false;
        assertFalse(th1.equals(th2));
        th1.isBridge = true;
        th2.isBridge = null;
        assertFalse(th1.equals(th2));
        th1.isBridge = null;
        th2.isBridge = true;
        assertFalse(th1.equals(th2));
        th1.isBridge = false;
        th2.isBridge = null;
        assertTrue(th1.equals(th2));
        assertEquals(th1.hashCode(), th2.hashCode());
        th1.isBridge = null;
        th2.isBridge = false;
        assertTrue(th1.equals(th2));
        assertEquals(th1.hashCode(), th2.hashCode());
        th1.isBridge = false;
        th2.isBridge = false;
        assertTrue(th1.equals(th2));
        assertEquals(th1.hashCode(), th2.hashCode());
        th1.isBridge = true;
        th2.isBridge = true;
        assertTrue(th1.equals(th2));
        assertEquals(th1.hashCode(), th2.hashCode());
    }

    @Test
    public void testEqualsWithBridge() throws IOException {
        YamlThingDTO th1 = new YamlThingDTO();
        YamlThingDTO th2 = new YamlThingDTO();

        th1.uid = "binding:type:id";
        th2.uid = "binding:type:id";

        th1.bridge = null;
        th2.bridge = null;
        assertTrue(th1.equals(th2));
        assertEquals(th1.hashCode(), th2.hashCode());
        th1.bridge = "binding:bridge:idBridge";
        th2.bridge = null;
        assertFalse(th1.equals(th2));
        th1.bridge = null;
        th2.bridge = "binding:bridge:idBridge";
        assertFalse(th1.equals(th2));
        th1.bridge = "binding:bridge:idBridge";
        th2.bridge = "binding:bridge:idBridge2";
        assertFalse(th1.equals(th2));
        th1.bridge = "binding:bridge:idBridge";
        th2.bridge = "binding:bridge:idBridge";
        assertTrue(th1.equals(th2));
        assertEquals(th1.hashCode(), th2.hashCode());
    }

    @Test
    public void testEqualsWithConfigurations() throws IOException {
        YamlThingDTO th1 = new YamlThingDTO();
        YamlThingDTO th2 = new YamlThingDTO();

        th1.uid = "binding:type:id";
        th2.uid = "binding:type:id";

        th1.config = null;
        th2.config = null;
        assertTrue(th1.equals(th2));
        assertEquals(th1.hashCode(), th2.hashCode());
        th1.config = Map.of();
        th2.config = Map.of();
        assertTrue(th1.equals(th2));
        assertEquals(th1.hashCode(), th2.hashCode());
        th1.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
        th2.config = null;
        assertFalse(th1.equals(th2));
        th1.config = null;
        th2.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
        assertFalse(th1.equals(th2));
        th1.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
        th2.config = Map.of("param1", "other value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
        assertFalse(th1.equals(th2));
        th2.config = Map.of("param1", "value", "param2", 25, "param3", true, "param4", List.of("val 1", "val 2"));
        assertFalse(th1.equals(th2));
        th2.config = Map.of("param1", "value", "param2", 50, "param3", false, "param4", List.of("val 1", "val 2"));
        assertFalse(th1.equals(th2));
        th2.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "value 2"));
        assertFalse(th1.equals(th2));
        th2.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1"));
        assertFalse(th1.equals(th2));
        th2.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", 75);
        assertFalse(th1.equals(th2));
        th2.config = Map.of("param1", "value", "param2", 50, "param3", true);
        assertFalse(th1.equals(th2));
        th2.config = Map.of("param1", "value", "param2", 50, "param3", true, "param4", List.of("val 1", "val 2"));
        assertTrue(th1.equals(th2));
        assertEquals(th1.hashCode(), th2.hashCode());
        th2.config = Map.of("param4", List.of("val 1", "val 2"), "param3", true, "param2", 50, "param1", "value");
        assertTrue(th1.equals(th2));
        assertEquals(th1.hashCode(), th2.hashCode());
    }

    @Test
    public void testEqualsWithChannels() throws IOException {
        YamlThingDTO th1 = new YamlThingDTO();
        th1.uid = "binding:type:id";
        YamlThingDTO th2 = new YamlThingDTO();
        th2.uid = "binding:type:id";

        YamlChannelDTO ch1 = new YamlChannelDTO();
        ch1.type = "channel-type";
        YamlChannelDTO ch2 = new YamlChannelDTO();
        ch2.type = "channel-other-type";
        YamlChannelDTO ch3 = new YamlChannelDTO();
        ch3.type = "channel-type";
        YamlChannelDTO ch4 = new YamlChannelDTO();
        ch4.type = "channel-other-type";

        th1.channels = Map.of("channel1", ch1);
        th2.channels = Map.of("channel1", ch3);
        assertTrue(th1.equals(th2));
        assertEquals(th1.hashCode(), th2.hashCode());

        th1.channels = Map.of("channel1", ch1, "channel2", ch2);
        th2.channels = Map.of("channel1", ch3, "channel2", ch4);
        assertTrue(th1.equals(th2));
        assertEquals(th1.hashCode(), th2.hashCode());

        th1.channels = Map.of("channel1", ch1, "channel2", ch2);
        th2.channels = Map.of("channel2", ch4, "channel1", ch3);
        assertTrue(th1.equals(th2));
        assertEquals(th1.hashCode(), th2.hashCode());

        th1.channels = Map.of("channel1", ch1);
        th2.channels = null;
        assertFalse(th1.equals(th2));
        th1.channels = Map.of("channel1", ch1);
        th2.channels = Map.of();
        assertFalse(th1.equals(th2));

        th1.channels = null;
        th2.channels = Map.of("channel1", ch3);
        assertFalse(th1.equals(th2));
        th1.channels = Map.of();
        th2.channels = Map.of("channel1", ch3);
        assertFalse(th1.equals(th2));

        th1.channels = Map.of("channel1", ch1, "channel2", ch2);
        th2.channels = Map.of("channel1", ch3);
        assertFalse(th1.equals(th2));

        th1.channels = Map.of("channel1", ch1);
        th2.channels = Map.of("channel1", ch4);
        assertFalse(th1.equals(th2));

        th1.channels = Map.of("channel", ch1);
        th2.channels = Map.of("channel1", ch3);
        assertFalse(th1.equals(th2));
    }
}
