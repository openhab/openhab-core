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

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.binding.ThingFactory;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeBuilder;

/**
 * Testing thing properties.
 *
 * @author Thomas Höfer - Initial contribution
 */
@NonNullByDefault
public class ThingPropertiesTest extends JavaOSGiTest {

    private static final Map<String, String> PROPERTIES = Map.of("key1", "value1", "key2", "value2");

    private @NonNullByDefault({}) Thing thing;

    @BeforeEach
    public void setup() {
        ThingType thingType = ThingTypeBuilder.instance(new ThingTypeUID("bindingId", "thingTypeId"), "label")
                .withProperties(PROPERTIES).build();
        thing = ThingFactory.createThing(thingType, new ThingUID(thingType.getUID(), "thingId"), new Configuration());
    }

    @Test
    public void testGetProperties() {
        assertEquals(2, thing.getProperties().size());
        assertEquals("value1", thing.getProperties().get("key1"));
        assertEquals("value2", thing.getProperties().get("key2"));
    }

    @Test
    public void testSetPropertyNewKey() {
        thing.setProperty("key3", "value3");

        assertEquals(3, thing.getProperties().size());
        assertEquals("value1", thing.getProperties().get("key1"));
        assertEquals("value2", thing.getProperties().get("key2"));
        assertEquals("value3", thing.getProperties().get("key3"));
    }

    @Test
    public void testSetPropertyNewValue() {
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

    @Test
    public void testSetPropertyNullKey() {
        assertThrows(IllegalArgumentException.class, () -> thing.setProperty(giveNull(), ""));
    }

    @Test
    public void testSetPropertyEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> thing.setProperty("", ""));
    }
}
