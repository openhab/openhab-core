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
package org.openhab.core.thing.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.openhab.core.thing.binding.builder.ThingStatusInfoBuilder.create;
import static org.openhab.core.thing.util.ThingHandlerHelper.isHandlerInitialized;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.builder.ThingBuilder;

/**
 * Test for the ThingHandlerHelper
 *
 * @author Simon Kaufmann - Initial contribution
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class ThingHandlerHelperTest {

    private @NonNullByDefault({}) Thing thing;

    private @Mock @NonNullByDefault({}) ThingHandler thingHandlerMock;

    @BeforeEach
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
        when(thingHandlerMock.getThing()).thenReturn(thing);

        thing.setStatusInfo(create(ThingStatus.UNINITIALIZED).build());
        assertThat(isHandlerInitialized(thingHandlerMock), is(false));

        thing.setStatusInfo(create(ThingStatus.INITIALIZING).build());
        assertThat(isHandlerInitialized(thingHandlerMock), is(false));

        thing.setStatusInfo(create(ThingStatus.REMOVING).build());
        assertThat(isHandlerInitialized(thingHandlerMock), is(false));

        thing.setStatusInfo(create(ThingStatus.REMOVED).build());
        assertThat(isHandlerInitialized(thingHandlerMock), is(false));

        thing.setStatusInfo(create(ThingStatus.UNKNOWN).build());
        assertThat(isHandlerInitialized(thingHandlerMock), is(true));

        thing.setStatusInfo(create(ThingStatus.ONLINE).build());
        assertThat(isHandlerInitialized(thingHandlerMock), is(true));

        thing.setStatusInfo(create(ThingStatus.OFFLINE).build());
        assertThat(isHandlerInitialized(thingHandlerMock), is(true));
    }
}
