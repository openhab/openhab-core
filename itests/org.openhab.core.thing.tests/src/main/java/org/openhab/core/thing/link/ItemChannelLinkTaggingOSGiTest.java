/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.thing.link;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.ThingRegistry;

/**
 * Tests for {@link ItemChannelLinkRegistry} tagging.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class ItemChannelLinkTaggingOSGiTest extends JavaOSGiTest {

    private static final String THINGS_MODEL_ID = "test.things";
    private static final String ITEMS_MODEL_ID = "test.items";

    private static final String THINGS_MODEL = """
            Bridge hue:bridge-api2:bridge "Bridge" [ipAddress="123.123.123.123", applicationKey="0123456789ABCDEF"] {
                Thing device light "Light" [resourceId="11111111-1111-1111-1111-111111111111"]
                Thing device dimmer "Dimmer" [resourceId="22222222-2222-2222-2222-222222222222"]
            }
                            """;

    private static final String ITEMS_MODEL = """
            String Item_01 {channel="hue:device:bridge:light:color" }
            String Item_02 {channel="hue:device:bridge:light:color" [useTags=false] }
            String Item_03 {channel="hue:device:bridge:light:color" [useTags="false"] }
            String Item_04 "Control, Color" {channel="hue:device:bridge:light:color" [useTags=true] }
            String Item_05 "Control, Color" {channel="hue:device:bridge:light:color" [useTags="true"] }
            String Item_06 "Control Color, Custom" ["Custom"] {channel="hue:device:bridge:light:color" [useTags=true] }
            String Item_07 "'Control', Power, Custom" ["Power", "Custom"] {channel="hue:device:bridge:light:color" }
            String Item_08 "'Control', Power, Custom" ["Power", "Custom"] {channel="hue:device:bridge:light:color" [useTags=true] }
            String Item_09 "Switch, Custom" ["Switch", "Custom"] {channel="hue:device:bridge:light:color" }
            String Item_10 "Switch, Custom" ["Switch", "Custom"] {channel="hue:device:bridge:light:color" [useTags=true] }
            String Item_11 "Switch, Power, Custom" ["Switch", "Power", "Custom"] {channel="hue:device:bridge:light:color" }
            String Item_12 "Switch, Power, Custom" ["Switch", "Power", "Custom"] {channel="hue:device:bridge:light:color" [useTags=true] }
            String Item_13 "Alarm, LowBattery" {channel="hue:device:bridge:dimmer:battery-low" [useTags=true] }
            String Item_14 "Control, Color" {channel="hue:device:bridge:light:color" [useTags=true], channel="hue:device:bridge:dimmer:battery-low" [useTags=true] }
            String Item_15 "Alarm, LowBattery" {channel="hue:device:bridge:light:color" [useTags=false], channel="hue:device:bridge:dimmer:battery-low" [useTags=true] }
                            """;

    private @NonNullByDefault({}) ItemRegistry itemRegistry;
    private @NonNullByDefault({}) ThingRegistry thingRegistry;
    private @NonNullByDefault({}) ModelRepository modelRepository;
    private @NonNullByDefault({}) ItemChannelLinkRegistry itemChannelLinkRegistry;

    @BeforeEach
    public void setup() {
        registerVolatileStorageService();

        itemRegistry = getService(ItemRegistry.class);
        assertNotNull(itemRegistry);

        thingRegistry = getService(ThingRegistry.class);
        assertNotNull(thingRegistry);

        itemChannelLinkRegistry = getService(ItemChannelLinkRegistry.class);
        assertNotNull(itemChannelLinkRegistry);

        modelRepository = getService(ModelRepository.class);
        assertNotNull(modelRepository);
    }

    /**
     * TODO I wrote this to test the channel / item tagging but it fails with the stack trace below ("resource" is null)
     * and in spite of many hours trying I was unable to find a solution to make it work. So in the menatime the test is
     * currently disabled until some kind maintainer can help me to fix it.
     *
     * <pre>
     * {@code
     * TEST org.openhab.core.thing.link.ItemChannelLinkTaggingOSGiTest#assertTagsAreCorrect() <<< ERROR: Cannot invoke
     * "org.eclipse.emf.ecore.resource.Resource.load(java.io.InputStream, java.util.Map)" because "resource" is null
     * java.lang.NullPointerException: Cannot invoke "org.eclipse.emf.ecore.resource.Resource.load(java.io.InputStream, java.util.Map)" because "resource" is null
     *     at org.openhab.core.model.core.internal.ModelRepositoryImpl.validateModel(ModelRepositoryImpl.java:280)
     *     at org.openhab.core.model.core.internal.ModelRepositoryImpl.addOrRefreshModel(ModelRepositoryImpl.java:108)
     *     at org.openhab.core.thing.link.ItemChannelLinkTaggingOSGiTest.assertTagsAreCorrect( ItemChannelLinkTaggingOSGiTest.java:107)
     * }
     * </pre>
     */
    @Test
    @Disabled
    public void assertTagsAreCorrect() {
        modelRepository.addOrRefreshModel(THINGS_MODEL_ID, new ByteArrayInputStream(THINGS_MODEL.getBytes())); // <== !!
        waitForAssert(() -> {
            assertEquals(3, thingRegistry.getAll().size());
        });

        modelRepository.addOrRefreshModel(ITEMS_MODEL_ID, new ByteArrayInputStream(ITEMS_MODEL.getBytes())); // <== !!
        waitForAssert(() -> {
            assertEquals(15, itemRegistry.getAll().size());
            assertEquals(17, itemChannelLinkRegistry.getAll().size());
        });

        Item item;

        item = itemRegistry.get("Item_01");
        assertNotNull(item);
        assertTrue(item.getTags().isEmpty());

        item = itemRegistry.get("Item_02");
        assertNotNull(item);
        assertTrue(item.getTags().isEmpty());

        item = itemRegistry.get("Item_03");
        assertNotNull(item);
        assertTrue(item.getTags().isEmpty());

        item = itemRegistry.get("Item_04");
        assertNotNull(item);
        assertEquals(Set.of("Control", "Color"), item.getTags());

        item = itemRegistry.get("Item_05");
        assertNotNull(item);
        assertEquals(Set.of("Control", "Color"), item.getTags());

        item = itemRegistry.get("Item_06");
        assertNotNull(item);
        assertEquals(Set.of("Control", "Color", "Custom"), item.getTags());

        item = itemRegistry.get("Item_07");
        assertNotNull(item);
        assertTrue(item.getTags().contains("Power"));
        assertTrue(item.getTags().contains("Custom"));

        item = itemRegistry.get("Item_08");
        assertNotNull(item);
        assertTrue(item.getTags().contains("Power"));
        assertTrue(item.getTags().contains("Custom"));

        item = itemRegistry.get("Item_09");
        assertNotNull(item);
        assertEquals(Set.of("Switch", "Custom"), item.getTags());

        item = itemRegistry.get("Item_10");
        assertNotNull(item);
        assertEquals(Set.of("Switch", "Custom"), item.getTags());

        item = itemRegistry.get("Item_11");
        assertNotNull(item);
        assertEquals(Set.of("Switch", "Power", "Custom"), item.getTags());

        item = itemRegistry.get("Item_12");
        assertNotNull(item);
        assertEquals(Set.of("Switch", "Power", "Custom"), item.getTags());

        item = itemRegistry.get("Item_13");
        assertNotNull(item);
        assertEquals(Set.of("Alarm", "LowBattery"), item.getTags());

        item = itemRegistry.get("Item_14");
        assertNotNull(item);
        assertTrue(item.getTags().equals(Set.of("Control", "Color")) //
                || item.getTags().equals(Set.of("Alarm", "LowBattery")));

        item = itemRegistry.get("Item_15");
        assertNotNull(item);
        assertEquals(Set.of("Alarm", "LowBattery"), item.getTags());
    }
}
