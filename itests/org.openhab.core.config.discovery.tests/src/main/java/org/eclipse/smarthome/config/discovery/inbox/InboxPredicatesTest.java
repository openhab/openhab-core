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
package org.eclipse.smarthome.config.discovery.inbox;

import static org.eclipse.smarthome.config.discovery.inbox.InboxPredicates.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultFlag;
import org.eclipse.smarthome.config.discovery.internal.DiscoveryResultImpl;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link InboxPredicates}.
 *
 * @author Andre Fuechsel - Initial Contribution
 */
public class InboxPredicatesTest {

    private static final String BINDING_ID1 = "bindingId1";
    private static final String BINDING_ID2 = "bindingId2";
    private static final String THING_ID1 = "thingId1";
    private static final String THING_ID2 = "thingId2";
    private static final String THING_TYPE_ID1 = "thingTypeId1";
    private static final String THING_TYPE_ID2 = "thingTypeId2";

    private static final ThingUID THING_UID11 = new ThingUID(BINDING_ID1, THING_ID1);
    private static final ThingUID THING_UID12 = new ThingUID(BINDING_ID1, THING_ID2);
    private static final ThingUID THING_UID22 = new ThingUID(BINDING_ID2, THING_ID2);

    private static final String PROP1 = "prop1";
    private static final String PROP2 = "prop2";
    private static final String PROP_VAL1 = "propVal1";
    private static final String PROP_VAL2 = "propVal2";

    private final static ThingTypeUID THING_TYPE_UID11 = new ThingTypeUID(BINDING_ID1, THING_TYPE_ID1);
    private final static ThingTypeUID THING_TYPE_UID12 = new ThingTypeUID(BINDING_ID1, THING_TYPE_ID2);
    private final static ThingTypeUID THING_TYPE_UID21 = new ThingTypeUID(BINDING_ID2, THING_TYPE_ID1);

    private final static Map<String, Object> PROPS1 = Collections
            .unmodifiableMap(Stream.of(new SimpleEntry<>(PROP1, PROP_VAL1), new SimpleEntry<>(PROP2, PROP_VAL2))
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue)));
    private final static Map<String, Object> PROPS2 = Collections.singletonMap(PROP2, PROP_VAL2);

    private final static List<DiscoveryResultImpl> results = Arrays.asList(
            new DiscoveryResultImpl(THING_TYPE_UID11, THING_UID11, null, PROPS1, PROP1, "label",
                    DiscoveryResult.TTL_UNLIMITED),
            new DiscoveryResultImpl(THING_TYPE_UID11, THING_UID12, null, PROPS1, null, "label",
                    DiscoveryResult.TTL_UNLIMITED),
            new DiscoveryResultImpl(THING_TYPE_UID12, THING_UID12, null, PROPS2, PROP2, "label",
                    DiscoveryResult.TTL_UNLIMITED),
            new DiscoveryResultImpl(THING_TYPE_UID21, THING_UID22, null, PROPS2, null, "label",
                    DiscoveryResult.TTL_UNLIMITED));

    @Before
    public void setUp() throws Exception {
        results.get(3).setFlag(DiscoveryResultFlag.IGNORED);
    }

    @Test
    public void testForBinding() {
        assertThat(results.stream().filter(forBinding(BINDING_ID1)).collect(Collectors.toList()).size(), is(3));

        assertThat(results.stream().filter(forBinding(BINDING_ID2)).collect(Collectors.toList()).size(), is(1));
        assertThat(results.stream().filter(forBinding(BINDING_ID2)).collect(Collectors.toList()).get(0),
                is(equalTo(results.get(3))));

        assertThat(results.stream().filter(forBinding(BINDING_ID2)).filter(withFlag(DiscoveryResultFlag.NEW))
                .collect(Collectors.toList()).size(), is(0));

        assertThat(results.stream().filter(forBinding(BINDING_ID2)).filter(withFlag(DiscoveryResultFlag.IGNORED))
                .collect(Collectors.toList()).size(), is(1));
        assertThat(results.stream().filter(forBinding(BINDING_ID2)).filter(withFlag(DiscoveryResultFlag.IGNORED))
                .collect(Collectors.toList()).get(0), is(equalTo(results.get(3))));
    }

    @Test
    public void testForThingTypeUID() {
        assertThat(results.stream().filter(forThingTypeUID(THING_TYPE_UID11)).collect(Collectors.toList()).size(),
                is(2));

        assertThat(results.stream().filter(forThingTypeUID(THING_TYPE_UID12)).collect(Collectors.toList()).size(),
                is(1));
        assertThat(results.stream().filter(forThingTypeUID(THING_TYPE_UID12)).collect(Collectors.toList()).get(0),
                is(equalTo(results.get(2))));
    }

    @Test
    public void testForThingUID() {
        assertThat(results.stream().filter(forThingUID(THING_UID11)).collect(Collectors.toList()).size(), is(1));
        assertThat(results.stream().filter(forThingUID(THING_UID11)).collect(Collectors.toList()).get(0),
                is(equalTo(results.get(0))));

        assertThat(results.stream().filter(forThingUID(THING_UID12)).collect(Collectors.toList()).size(), is(2));
        assertThat(results.stream().filter(forThingUID(THING_UID12)).filter(forThingTypeUID(THING_TYPE_UID12))
                .collect(Collectors.toList()).size(), is(1));
        assertThat(results.stream().filter(forThingUID(THING_UID12)).filter(forThingTypeUID(THING_TYPE_UID12))
                .collect(Collectors.toList()).get(0), is(equalTo(results.get(2))));
    }

    @Test
    public void testWithFlag() {
        assertThat(results.stream().filter(withFlag(DiscoveryResultFlag.NEW)).collect(Collectors.toList()).size(),
                is(3));
        assertThat(results.stream().filter(withFlag(DiscoveryResultFlag.IGNORED)).collect(Collectors.toList()).size(),
                is(1));
        assertThat(results.stream().filter(withFlag(DiscoveryResultFlag.IGNORED)).collect(Collectors.toList()).get(0),
                is(equalTo(results.get(3))));
    }

    @Test
    public void testWithProperty() {
        assertThat(results.stream().filter(withProperty(PROP1, PROP_VAL1)).collect(Collectors.toList()).size(), is(2));
        assertThat(results.stream().filter(withProperty(PROP2, PROP_VAL2)).collect(Collectors.toList()).size(), is(4));
        assertThat(results.stream().filter(withProperty(PROP1, PROP_VAL2)).collect(Collectors.toList()).size(), is(0));
        assertThat(results.stream().filter(withProperty(PROP2, PROP_VAL1)).collect(Collectors.toList()).size(), is(0));
        assertThat(results.stream().filter(withProperty(null, PROP_VAL1)).collect(Collectors.toList()).size(), is(0));
    }

    @Test
    public void testWithRepresentationProperty() {
        assertThat(results.stream().filter(withRepresentationProperty(PROP1)).collect(Collectors.toList()).size(),
                is(1));
        assertThat(results.stream().filter(withRepresentationProperty(PROP1)).collect(Collectors.toList()).get(0),
                is(equalTo(results.get(0))));
        assertThat(results.stream().filter(withRepresentationProperty(PROP2)).collect(Collectors.toList()).size(),
                is(1));
        assertThat(results.stream().filter(withRepresentationProperty(PROP2)).collect(Collectors.toList()).get(0),
                is(equalTo(results.get(2))));
    }

    @Test
    public void testWithRepresentationPropertyValue() {
        assertThat(
                results.stream().filter(withRepresentationPropertyValue(PROP_VAL1)).collect(Collectors.toList()).size(),
                is(1));
        assertThat(
                results.stream().filter(withRepresentationPropertyValue(PROP_VAL1)).collect(Collectors.toList()).get(0),
                is(equalTo(results.get(0))));
        assertThat(
                results.stream().filter(withRepresentationPropertyValue(PROP_VAL2)).collect(Collectors.toList()).size(),
                is(1));
        assertThat(
                results.stream().filter(withRepresentationPropertyValue(PROP_VAL2)).collect(Collectors.toList()).get(0),
                is(equalTo(results.get(2))));
    }

}
