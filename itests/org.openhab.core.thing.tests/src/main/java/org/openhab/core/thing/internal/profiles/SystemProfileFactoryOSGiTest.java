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
package org.openhab.core.thing.internal.profiles;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileType;
import org.openhab.core.thing.profiles.ProfileTypeProvider;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.SystemProfiles;
import org.openhab.core.thing.type.ChannelType;

/**
 * Test cases for the {@link SystemProfileFactory} class.
 *
 * @author Simon Kaufmann - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class SystemProfileFactoryOSGiTest extends JavaOSGiTest {

    private final Map<String, Object> properties = Map.of(SystemOffsetProfile.OFFSET_PARAM, BigDecimal.ZERO,
            SystemHysteresisStateProfile.LOWER_PARAM, BigDecimal.TEN, SystemRangeStateProfile.UPPER_PARAM,
            BigDecimal.valueOf(40));

    private @NonNullByDefault({}) SystemProfileFactory profileFactory;

    private @Mock @NonNullByDefault({}) ProfileCallback callbackMock;
    private @Mock @NonNullByDefault({}) ProfileContext contextMock;

    @BeforeEach
    public void beforeEach() {
        profileFactory = getService(ProfileTypeProvider.class, SystemProfileFactory.class);
        assertNotNull(profileFactory);

        when(contextMock.getConfiguration()).thenReturn(new Configuration(properties));
    }

    @Test
    public void systemProfileTypesAndUidsShouldBeAvailable() {
        Collection<ProfileTypeUID> systemProfileTypeUIDs = profileFactory.getSupportedProfileTypeUIDs();
        assertThat(systemProfileTypeUIDs, hasSize(21));

        Collection<ProfileType> systemProfileTypes = profileFactory.getProfileTypes(null);
        assertThat(systemProfileTypes, hasSize(systemProfileTypeUIDs.size()));
    }

    @Test
    public void testFactoryCreatesAvailableProfiles() {
        for (ProfileTypeUID profileTypeUID : profileFactory.getSupportedProfileTypeUIDs()) {
            assertNotNull(profileFactory.createProfile(profileTypeUID, callbackMock, contextMock));
        }
    }

    @Test
    public void testGetSuggestedProfileTypeUIDNullChannelType1() {
        assertNull(profileFactory.getSuggestedProfileTypeUID((ChannelType) null, CoreItemFactory.SWITCH));
    }

    @Test
    public void testGetSuggestedProfileTypeUIDNullChannelType2() {
        Channel channel = ChannelBuilder.create(new ChannelUID("test:test:test:test"), CoreItemFactory.SWITCH).build();
        assertEquals(SystemProfiles.DEFAULT,
                profileFactory.getSuggestedProfileTypeUID(channel, CoreItemFactory.SWITCH));
    }
}
