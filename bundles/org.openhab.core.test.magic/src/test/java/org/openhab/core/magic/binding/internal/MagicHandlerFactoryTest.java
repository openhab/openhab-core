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
package org.openhab.core.magic.binding.internal;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.openhab.core.magic.binding.MagicBindingConstants;
import org.openhab.core.magic.binding.handler.MagicColorLightHandler;
import org.openhab.core.magic.binding.handler.MagicDimmableLightHandler;
import org.openhab.core.magic.binding.handler.MagicOnOffLightHandler;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;

/**
 * Tests cases for {@link MagicHandlerFactory}.
 *
 * @author Henning Treu - Initial contribution
 */
public class MagicHandlerFactoryTest {

    private MagicHandlerFactory factory;

    @Before
    public void setup() {
        factory = new MagicHandlerFactory();
    }

    @Test
    public void shoudlReturnNullForUnknownThingTypeUID() {
        Thing thing = mock(Thing.class);
        when(thing.getThingTypeUID()).thenReturn(new ThingTypeUID("anyBinding:someThingType"));

        assertThat(factory.createHandler(thing), is(nullValue()));
    }

    @Test
    public void shoudlReturnColorLightHandler() {
        Thing thing = mock(Thing.class);
        when(thing.getThingTypeUID()).thenReturn(MagicBindingConstants.THING_TYPE_COLOR_LIGHT);

        assertThat(factory.createHandler(thing), is(instanceOf(MagicColorLightHandler.class)));
    }

    @Test
    public void shoudlReturnDimmableLightHandler() {
        Thing thing = mock(Thing.class);
        when(thing.getThingTypeUID()).thenReturn(MagicBindingConstants.THING_TYPE_DIMMABLE_LIGHT);

        assertThat(factory.createHandler(thing), is(instanceOf(MagicDimmableLightHandler.class)));
    }

    @Test
    public void shoudlReturnOnOffLightHandler() {
        Thing thing = mock(Thing.class);
        when(thing.getThingTypeUID()).thenReturn(MagicBindingConstants.THING_TYPE_ON_OFF_LIGHT);

        assertThat(factory.createHandler(thing), is(instanceOf(MagicOnOffLightHandler.class)));
    }
}
