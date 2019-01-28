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
package org.eclipse.smarthome.core.thing.util;

import static org.eclipse.smarthome.core.thing.binding.builder.ThingStatusInfoBuilder.create;
import static org.eclipse.smarthome.core.thing.util.ThingHandlerHelper.isHandlerInitialized;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * Test for the ThingHandlerHelper
 *
 * @author Simon Kaufmann - initial contribution and API
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
public class ThingHandlerHelperTest {

    private Thing thing;

    private @Mock ThingHandler thingHandler;

    public @Rule MockitoRule rule = MockitoJUnit.rule();

    @Before
    public void setup() {
        thing = ThingBuilder.create(new ThingTypeUID("test:test"), new ThingUID("test:test:test")).build();
    }

    @Test
    public void assertIsHandlerInitializedWorksCorrectlyForAThingStatus() {
        assertThat(isHandlerInitialized(ThingStatus.UNINITIALIZED), is(false));
        assertThat(isHandlerInitialized(ThingStatus.INITIALIZING), is(false));
        assertThat(isHandlerInitialized(ThingStatus.REMOVING), is(false));
        assertThat(isHandlerInitialized(ThingStatus.REMOVED), is(false));
        assertThat(isHandlerInitialized(ThingStatus.UNKNOWN), is(true));
        assertThat(isHandlerInitialized(ThingStatus.ONLINE), is(true));
        assertThat(isHandlerInitialized(ThingStatus.OFFLINE), is(true));
    }

    @Test
    public void assertIsHandlerInitializedWorksCorrectlyForAThing() {
        thing.setStatusInfo(create(ThingStatus.UNINITIALIZED).build());
        assertThat(isHandlerInitialized(thing), is(false));

        thing.setStatusInfo(create(ThingStatus.INITIALIZING).build());
        assertThat(isHandlerInitialized(thing), is(false));

        thing.setStatusInfo(create(ThingStatus.REMOVING).build());
        assertThat(isHandlerInitialized(thing), is(false));

        thing.setStatusInfo(create(ThingStatus.REMOVED).build());
        assertThat(isHandlerInitialized(thing), is(false));

        thing.setStatusInfo(create(ThingStatus.UNKNOWN).build());
        assertThat(isHandlerInitialized(thing), is(true));

        thing.setStatusInfo(create(ThingStatus.ONLINE).build());
        assertThat(isHandlerInitialized(thing), is(true));

        thing.setStatusInfo(create(ThingStatus.OFFLINE).build());
        assertThat(isHandlerInitialized(thing), is(true));
    }

    @Test
    public void assertIsHandlerInitializedWorksCorrectlyForAThingHandler() {
        when(thingHandler.getThing()).thenReturn(thing);

        thing.setStatusInfo(create(ThingStatus.UNINITIALIZED).build());
        assertThat(isHandlerInitialized(thingHandler), is(false));

        thing.setStatusInfo(create(ThingStatus.INITIALIZING).build());
        assertThat(isHandlerInitialized(thingHandler), is(false));

        thing.setStatusInfo(create(ThingStatus.REMOVING).build());
        assertThat(isHandlerInitialized(thingHandler), is(false));

        thing.setStatusInfo(create(ThingStatus.REMOVED).build());
        assertThat(isHandlerInitialized(thingHandler), is(false));

        thing.setStatusInfo(create(ThingStatus.UNKNOWN).build());
        assertThat(isHandlerInitialized(thingHandler), is(true));

        thing.setStatusInfo(create(ThingStatus.ONLINE).build());
        assertThat(isHandlerInitialized(thingHandler), is(true));

        thing.setStatusInfo(create(ThingStatus.OFFLINE).build());
        assertThat(isHandlerInitialized(thingHandler), is(true));
    }
}
