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
package org.eclipse.smarthome.core.thing.internal.profiles;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import org.eclipse.smarthome.core.library.CoreItemFactory;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.profiles.SystemProfiles;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeRegistry;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Simon Kaufmann - initial contribution and API.
 *
 */
public class SystemProfileFactoryTest {

    private ChannelTypeRegistry channelTypeRegistry;
    private SystemProfileFactory factory;

    @Before
    public void setup() {
        channelTypeRegistry = new ChannelTypeRegistry();

        factory = new SystemProfileFactory();
        factory.setChannelTypeRegistry(channelTypeRegistry);
    }

    @Test
    public void testGetSuggestedProfileTypeUID_nullChannelType1() {
        assertThat(factory.getSuggestedProfileTypeUID((ChannelType) null, CoreItemFactory.SWITCH), is(nullValue()));
    }

    @Test
    public void testGetSuggestedProfileTypeUID_nullChannelType2() {
        Channel channel = ChannelBuilder.create(new ChannelUID("test:test:test:test"), CoreItemFactory.SWITCH).build();
        assertThat(factory.getSuggestedProfileTypeUID(channel, CoreItemFactory.SWITCH), is(SystemProfiles.DEFAULT));
    }

}
