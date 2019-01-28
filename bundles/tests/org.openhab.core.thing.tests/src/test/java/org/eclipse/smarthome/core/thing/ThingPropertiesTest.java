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

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.binding.ThingFactory;
import org.eclipse.smarthome.core.thing.type.ThingType;
import org.eclipse.smarthome.core.thing.type.ThingTypeBuilder;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.Before;
import org.junit.Test;

/**
 * Testing thing properties.
 *
 * @author Thomas Höfer - Initial contribution
 */
public class ThingPropertiesTest extends JavaOSGiTest {

    private final Map<String, String> properties = new HashMap<String, String>() {
        private static final long serialVersionUID = 1L;
        {
            put("key1", "value1");
            put("key2", "value2");
        }
    };
    private Thing thing;
    private final String nullString = null; // trick the null-annotation tooling

    @Before
    public void setup() {
        ThingType thingType = ThingTypeBuilder.instance(new ThingTypeUID("bindingId", "thingTypeId"), "label")
                .withProperties(properties).build();
        thing = ThingFactory.createThing(thingType, new ThingUID(thingType.getUID(), "thingId"), new Configuration());
    }

    @Test
    public void testGetProperties() {
        assertEquals(2, thing.getProperties().size());
        assertEquals("value1", thing.getProperties().get("key1"));
        assertEquals("value2", thing.getProperties().get("key2"));
    }

    @Test
    public void testSetProperty_newKey() {
        thing.setProperty("key3", "value3");

        assertEquals(3, thing.getProperties().size());
        assertEquals("value1", thing.getProperties().get("key1"));
        assertEquals("value2", thing.getProperties().get("key2"));
        assertEquals("value3", thing.getProperties().get("key3"));
    }

    @Test
    public void testSetProperty_newValue() {
        String value = thing.setProperty("key2", "value3");

        assertEquals("value2", value);
        assertEquals(2, thing.getProperties().size());
        assertEquals("value1", thing.getProperties().get("key1"));
        assertEquals("value3", thing.getProperties().get("key2"));
    }

    @Test
    public void testRemoveProperty() {
        String value = thing.setProperty("key1", null);

        assertEquals("value1", value);
        assertEquals(1, thing.getProperties().size());
        assertEquals("value2", thing.getProperties().get("key2"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetProperty_nullKey() {
        thing.setProperty(nullString, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetProperty_emptyName() {
        thing.setProperty("", "");
    }
}
