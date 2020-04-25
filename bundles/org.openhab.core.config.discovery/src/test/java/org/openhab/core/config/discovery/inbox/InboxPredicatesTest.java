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
package org.openhab.core.config.discovery.inbox;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.openhab.core.config.discovery.inbox.InboxPredicates.*;

import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.DiscoveryResultFlag;
import org.openhab.core.config.discovery.internal.DiscoveryResultImpl;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;

/**
 * Tests for {@link InboxPredicates}.
 *
 * @author Andre Fuechsel - Initial contribution
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

    private static final ThingTypeUID THING_TYPE_UID11 = new ThingTypeUID(BINDING_ID1, THING_TYPE_ID1);
    private static final ThingTypeUID THING_TYPE_UID12 = new ThingTypeUID(BINDING_ID1, THING_TYPE_ID2);
    private static final ThingTypeUID THING_TYPE_UID21 = new ThingTypeUID(BINDING_ID2, THING_TYPE_ID1);

    private static final Map<String, Object> PROPS1 = Collections
            .unmodifiableMap(Stream.of(new SimpleEntry<>(PROP1, PROP_VAL1), new SimpleEntry<>(PROP2, PROP_VAL2))
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue)));
    private static final Map<String, Object> PROPS2 = Collections.singletonMap(PROP2, PROP_VAL2);

    private static final List<DiscoveryResult> RESULTS = Arrays.asList(
            DiscoveryResultBuilder.create(THING_UID11).withThingType(THING_TYPE_UID11).withProperties(PROPS1)
                    .withRepresentationProperty(PROP1).withLabel("label").build(),
            DiscoveryResultBuilder.create(THING_UID12).withThingType(THING_TYPE_UID11).withProperties(PROPS1)
                    .withLabel("label").build(),
            DiscoveryResultBuilder.create(THING_UID12).withThingType(THING_TYPE_UID12).withProperties(PROPS2)
                    .withRepresentationProperty(PROP2).withLabel("label").build(),
            DiscoveryResultBuilder.create(THING_UID22).withThingType(THING_TYPE_UID21).withProperties(PROPS2)
                    .withLabel("label").build());

    @Before
    public void setUp() throws Exception {
        ((DiscoveryResultImpl) RESULTS.get(3)).setFlag(DiscoveryResultFlag.IGNORED);
    }

    @Test
    public void testForBinding() {
        assertThat(RESULTS.stream().filter(forBinding(BINDING_ID1)).collect(Collectors.toList()).size(), is(3));

        assertThat(RESULTS.stream().filter(forBinding(BINDING_ID2)).collect(Collectors.toList()).size(), is(1));
        assertThat(RESULTS.stream().filter(forBinding(BINDING_ID2)).collect(Collectors.toList()).get(0),
                is(equalTo(RESULTS.get(3))));

        assertThat(RESULTS.stream().filter(forBinding(BINDING_ID2)).filter(withFlag(DiscoveryResultFlag.NEW))
                .collect(Collectors.toList()).size(), is(0));

        assertThat(RESULTS.stream().filter(forBinding(BINDING_ID2)).filter(withFlag(DiscoveryResultFlag.IGNORED))
                .collect(Collectors.toList()).size(), is(1));
        assertThat(RESULTS.stream().filter(forBinding(BINDING_ID2)).filter(withFlag(DiscoveryResultFlag.IGNORED))
                .collect(Collectors.toList()).get(0), is(equalTo(RESULTS.get(3))));
    }

    @Test
    public void testForThingTypeUID() {
        assertThat(RESULTS.stream().filter(forThingTypeUID(THING_TYPE_UID11)).collect(Collectors.toList()).size(),
                is(2));

        assertThat(RESULTS.stream().filter(forThingTypeUID(THING_TYPE_UID12)).collect(Collectors.toList()).size(),
                is(1));
        assertThat(RESULTS.stream().filter(forThingTypeUID(THING_TYPE_UID12)).collect(Collectors.toList()).get(0),
                is(equalTo(RESULTS.get(2))));
    }

    @Test
    public void testForThingUID() {
        assertThat(RESULTS.stream().filter(forThingUID(THING_UID11)).collect(Collectors.toList()).size(), is(1));
        assertThat(RESULTS.stream().filter(forThingUID(THING_UID11)).collect(Collectors.toList()).get(0),
                is(equalTo(RESULTS.get(0))));

        assertThat(RESULTS.stream().filter(forThingUID(THING_UID12)).collect(Collectors.toList()).size(), is(2));
        assertThat(RESULTS.stream().filter(forThingUID(THING_UID12)).filter(forThingTypeUID(THING_TYPE_UID12))
                .collect(Collectors.toList()).size(), is(1));
        assertThat(RESULTS.stream().filter(forThingUID(THING_UID12)).filter(forThingTypeUID(THING_TYPE_UID12))
                .collect(Collectors.toList()).get(0), is(equalTo(RESULTS.get(2))));
    }

    @Test
    public void testWithFlag() {
        assertThat(RESULTS.stream().filter(withFlag(DiscoveryResultFlag.NEW)).collect(Collectors.toList()).size(),
                is(3));
        assertThat(RESULTS.stream().filter(withFlag(DiscoveryResultFlag.IGNORED)).collect(Collectors.toList()).size(),
                is(1));
        assertThat(RESULTS.stream().filter(withFlag(DiscoveryResultFlag.IGNORED)).collect(Collectors.toList()).get(0),
                is(equalTo(RESULTS.get(3))));
    }

    @Test
    public void testWithProperty() {
        assertThat(RESULTS.stream().filter(withProperty(PROP1, PROP_VAL1)).collect(Collectors.toList()).size(), is(2));
        assertThat(RESULTS.stream().filter(withProperty(PROP2, PROP_VAL2)).collect(Collectors.toList()).size(), is(4));
        assertThat(RESULTS.stream().filter(withProperty(PROP1, PROP_VAL2)).collect(Collectors.toList()).size(), is(0));
        assertThat(RESULTS.stream().filter(withProperty(PROP2, PROP_VAL1)).collect(Collectors.toList()).size(), is(0));
        assertThat(RESULTS.stream().filter(withProperty(null, PROP_VAL1)).collect(Collectors.toList()).size(), is(0));
    }

    @Test
    public void testWithRepresentationProperty() {
        assertThat(RESULTS.stream().filter(withRepresentationProperty(PROP1)).collect(Collectors.toList()).size(),
                is(1));
        assertThat(RESULTS.stream().filter(withRepresentationProperty(PROP1)).collect(Collectors.toList()).get(0),
                is(equalTo(RESULTS.get(0))));
        assertThat(RESULTS.stream().filter(withRepresentationProperty(PROP2)).collect(Collectors.toList()).size(),
                is(1));
        assertThat(RESULTS.stream().filter(withRepresentationProperty(PROP2)).collect(Collectors.toList()).get(0),
                is(equalTo(RESULTS.get(2))));
    }

    @Test
    public void testWithRepresentationPropertyValue() {
        assertThat(
                RESULTS.stream().filter(withRepresentationPropertyValue(PROP_VAL1)).collect(Collectors.toList()).size(),
                is(1));
        assertThat(
                RESULTS.stream().filter(withRepresentationPropertyValue(PROP_VAL1)).collect(Collectors.toList()).get(0),
                is(equalTo(RESULTS.get(0))));
        assertThat(
                RESULTS.stream().filter(withRepresentationPropertyValue(PROP_VAL2)).collect(Collectors.toList()).size(),
                is(1));
        assertThat(
                RESULTS.stream().filter(withRepresentationPropertyValue(PROP_VAL2)).collect(Collectors.toList()).get(0),
                is(equalTo(RESULTS.get(2))));
    }
}
