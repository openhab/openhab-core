/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.core.thing.internal.profiles;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Collection;

import org.eclipse.smarthome.core.library.CoreItemFactory;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.profiles.ProfileType;
import org.eclipse.smarthome.core.thing.profiles.ProfileTypeProvider;
import org.eclipse.smarthome.core.thing.profiles.SystemProfiles;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for the {@link SystemProfileFactory} class.
 *
 * @author Simon Kaufmann - Initial contribution
 */
public class SystemProfileFactoryOSGiTest extends JavaOSGiTest {

    private SystemProfileFactory profileFactory;

    @Before
    public void setUp() {
        profileFactory = getService(ProfileTypeProvider.class, SystemProfileFactory.class);
        assertNotNull(profileFactory);
    }

    @Test
    public void systemProfileTypesShouldBeAvailable() {
        Collection<ProfileType> systemProfileTypes = profileFactory.getProfileTypes(null);
        assertEquals(14, systemProfileTypes.size());
    }

    @Test
    public void testGetSuggestedProfileTypeUID_nullChannelType1() {
        assertThat(profileFactory.getSuggestedProfileTypeUID((ChannelType) null, CoreItemFactory.SWITCH),
                is(nullValue()));
    }

    @Test
    public void testGetSuggestedProfileTypeUID_nullChannelType2() {
        Channel channel = ChannelBuilder.create(new ChannelUID("test:test:test:test"), CoreItemFactory.SWITCH).build();
        assertThat(profileFactory.getSuggestedProfileTypeUID(channel, CoreItemFactory.SWITCH),
                is(SystemProfiles.DEFAULT));
    }
}
