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
package org.openhab.core.config.discovery;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;

/**
 * Tests the {@link DiscoveryResultBuilder}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
public class DiscoveryResultBuilderTest {

    private static final String BINDING_ID = "bindingId";
    private static final ThingUID BRIDGE_UID = new ThingUID(new ThingTypeUID(BINDING_ID, "bridgeTypeId"), "bridgeId");
    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID(BINDING_ID, "thingTypeId");
    private static final ThingUID THING_UID = new ThingUID(THING_TYPE_UID, BRIDGE_UID, "thingId");
    private static final String KEY1 = "key1";
    private static final String KEY2 = "key2";
    private static final String VALUE1 = "value1";
    private static final String VALUE2 = "value2";
    private final Map<String, Object> properties = new HashMap<String, Object>() {
        private static final long serialVersionUID = 1L;
        {
            put(KEY1, VALUE1);
            put(KEY2, VALUE2);
        }
    };
    private DiscoveryResultBuilder builder;
    private DiscoveryResult discoveryResult;

    @Before
    public void setup() {
        builder = DiscoveryResultBuilder.create(THING_UID).withThingType(THING_TYPE_UID).withProperties(properties)
                .withRepresentationProperty(KEY1).withLabel("Test");
        discoveryResult = builder.build();
    }

    @Test
    public void testDiscoveryResultBuilder() {
        assertThat(discoveryResult.getThingUID(), is(THING_UID));
        assertThat(discoveryResult.getThingTypeUID(), is(THING_TYPE_UID));
        assertThat(discoveryResult.getBindingId(), is(BINDING_ID));
        assertThat(discoveryResult.getLabel(), is("Test"));
        assertThat(discoveryResult.getProperties().size(), is(2));
        assertThat(discoveryResult.getProperties(), hasEntry(KEY1, VALUE1));
        assertThat(discoveryResult.getProperties(), hasEntry(KEY2, VALUE2));
        assertThat(discoveryResult.getRepresentationProperty(), is(KEY1));
        assertThat(discoveryResult.getTimeToLive(), is(DiscoveryResult.TTL_UNLIMITED));
    }

    @Test
    public void testDiscoveryResultBuilderWithTTL() {
        DiscoveryResult otherDiscoveryResult = builder.withTTL(100L).build();

        assertThat(otherDiscoveryResult.getTimeToLive(), is(100L));
    }

    @Test
    public void testDiscoveryResultBuilderWithMatchingBridge() {
        DiscoveryResult otherDiscoveryResult = builder.withBridge(BRIDGE_UID).build();

        assertThat(otherDiscoveryResult.getBridgeUID(), is(BRIDGE_UID));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDiscoveryResultBuilderWithBridge() {
        @SuppressWarnings("unused")
        DiscoveryResult otherDiscoveryResult = DiscoveryResultBuilder
                .create(new ThingUID(THING_TYPE_UID, "otherThingId")).withBridge(BRIDGE_UID).build();
    }

    @Test
    @Ignore
    public void subsequentBuildsCreateIndependentDiscoveryResults() {
        DiscoveryResult otherDiscoveryResult = builder.withLabel("Second Test").withProperties(Collections.emptyMap())
                .build();

        assertThat(otherDiscoveryResult.getLabel(), is(not(discoveryResult.getLabel())));
        assertThat(otherDiscoveryResult.getProperties().size(), is(not(discoveryResult.getProperties().size())));
    }
}
