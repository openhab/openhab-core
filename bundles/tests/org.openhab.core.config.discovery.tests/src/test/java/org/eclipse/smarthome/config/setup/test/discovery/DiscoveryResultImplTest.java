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

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultFlag;
import org.eclipse.smarthome.config.discovery.internal.DiscoveryResultImpl;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.junit.Test;

/**
 * The {@link DiscoveryResultTest} checks if any invalid input parameters
 * and the synchronization of {@link DiscoveryResult}s work in a correct way.
 *
 * @author Michael Grammling - Initial Contribution
 * @author Thomas Höfer - Added representation
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
public class DiscoveryResultImplTest {

    private static final int DEFAULT_TTL = 60;

    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConstructorForThingType() {
        new DiscoveryResultImpl(new ThingUID("aa"), null, null, null, null, DEFAULT_TTL);
    }

    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConstructorForTTL() {
        ThingTypeUID thingTypeUID = new ThingTypeUID("bindingId", "thingType");
        new DiscoveryResultImpl(thingTypeUID, new ThingUID(thingTypeUID, "thingId"), null, null, null, null, -2);
    }

    @Test
    public void testValidConstructor() {
        ThingTypeUID thingTypeUID = new ThingTypeUID("bindingId", "thingType");

        DiscoveryResultImpl discoveryResult = new DiscoveryResultImpl(thingTypeUID,
                new ThingUID(thingTypeUID, "thingId"), null, null, null, null, DEFAULT_TTL);

        assertEquals("bindingId:thingType", discoveryResult.getThingTypeUID().toString());
        assertEquals("bindingId:thingType:thingId", discoveryResult.getThingUID().toString());
        assertEquals("bindingId", discoveryResult.getBindingId());
        assertEquals("", discoveryResult.getLabel());
        assertEquals(DiscoveryResultFlag.NEW, discoveryResult.getFlag());

        assertNotNull("The properties must never be null!", discoveryResult.getProperties());
        assertNull(discoveryResult.getRepresentationProperty());
    }

    @Test
    public void testInvalidSynchronize() {
        ThingTypeUID thingTypeUID = new ThingTypeUID("bindingId", "thingType");

        Map<String, Object> discoveryResultSourceMap = new HashMap<>();
        discoveryResultSourceMap.put("ipAddress", "127.0.0.1");

        DiscoveryResultImpl discoveryResult = new DiscoveryResultImpl(thingTypeUID,
                new ThingUID(thingTypeUID, "thingId"), null, discoveryResultSourceMap, "ipAddress", "TARGET",
                DEFAULT_TTL);
        discoveryResult.setFlag(DiscoveryResultFlag.IGNORED);

        discoveryResult.synchronize(null);

        assertEquals("127.0.0.1", discoveryResult.getProperties().get("ipAddress"));
        assertEquals("ipAddress", discoveryResult.getRepresentationProperty());
        assertEquals("TARGET", discoveryResult.getLabel());
        assertEquals(DiscoveryResultFlag.IGNORED, discoveryResult.getFlag());
    }

    @Test
    public void testIrrelevantSynchronize() {
        ThingTypeUID thingTypeUID = new ThingTypeUID("bindingId", "thingType");

        Map<String, Object> discoveryResultSourceMap = new HashMap<>();
        discoveryResultSourceMap.put("ipAddress", "127.0.0.1");

        DiscoveryResultImpl discoveryResult = new DiscoveryResultImpl(thingTypeUID,
                new ThingUID(thingTypeUID, "thingId"), null, discoveryResultSourceMap, "ipAddress", "TARGET",
                DEFAULT_TTL);
        discoveryResult.setFlag(DiscoveryResultFlag.IGNORED);

        DiscoveryResultImpl discoveryResultSource = new DiscoveryResultImpl(thingTypeUID,
                new ThingUID(thingTypeUID, "anotherThingId"), null, null, null, null, DEFAULT_TTL);

        discoveryResult.synchronize(discoveryResultSource);

        assertEquals("127.0.0.1", discoveryResult.getProperties().get("ipAddress"));
        assertEquals("ipAddress", discoveryResult.getRepresentationProperty());
        assertEquals("TARGET", discoveryResult.getLabel());
        assertEquals(DiscoveryResultFlag.IGNORED, discoveryResult.getFlag());
    }

    @Test
    public void testSynchronize() {
        ThingTypeUID thingTypeUID = new ThingTypeUID("bindingId", "thingType");

        Map<String, Object> discoveryResultSourceMap = new HashMap<>();
        discoveryResultSourceMap.put("ipAddress", "127.0.0.1");

        DiscoveryResultImpl discoveryResult = new DiscoveryResultImpl(thingTypeUID,
                new ThingUID(thingTypeUID, "thingId"), null, discoveryResultSourceMap, "ipAddress", "TARGET",
                DEFAULT_TTL);
        discoveryResult.setFlag(DiscoveryResultFlag.IGNORED);

        Map<String, Object> discoveryResultMap = new HashMap<>();
        discoveryResultMap.put("ipAddress", "192.168.178.1");
        discoveryResultMap.put("macAddress", "AA:BB:CC:DD:EE:FF");

        DiscoveryResultImpl discoveryResultSource = new DiscoveryResultImpl(thingTypeUID,
                new ThingUID(thingTypeUID, "thingId"), null, discoveryResultMap, "macAddress", "SOURCE", DEFAULT_TTL);
        discoveryResultSource.setFlag(DiscoveryResultFlag.NEW);

        discoveryResult.synchronize(discoveryResultSource);

        assertEquals("192.168.178.1", discoveryResult.getProperties().get("ipAddress"));
        assertEquals("AA:BB:CC:DD:EE:FF", discoveryResult.getProperties().get("macAddress"));
        assertEquals("macAddress", discoveryResult.getRepresentationProperty());
        assertEquals("SOURCE", discoveryResult.getLabel());
        assertEquals(DiscoveryResultFlag.IGNORED, discoveryResult.getFlag());
    }

    @Test
    public void testThingTypeCompatibility() {
        ThingTypeUID thingTypeUID = new ThingTypeUID("bindingId", "thingType");
        DiscoveryResultImpl discoveryResult = new DiscoveryResultImpl(null, new ThingUID(thingTypeUID, "thingId"), null,
                null, "nothing", "label", DEFAULT_TTL);
        assertNotNull(discoveryResult.getThingTypeUID());
        assertEquals(discoveryResult.getThingTypeUID(), thingTypeUID);
    }
}
