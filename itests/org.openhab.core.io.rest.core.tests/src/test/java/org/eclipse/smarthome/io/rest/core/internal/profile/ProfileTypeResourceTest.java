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
package org.eclipse.smarthome.io.rest.core.internal.profile;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.smarthome.core.thing.profiles.ProfileType;
import org.eclipse.smarthome.core.thing.profiles.ProfileTypeBuilder;
import org.eclipse.smarthome.core.thing.profiles.ProfileTypeRegistry;
import org.eclipse.smarthome.core.thing.profiles.ProfileTypeUID;
import org.eclipse.smarthome.core.thing.profiles.dto.ProfileTypeDTO;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelTypeRegistry;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.test.java.JavaTest;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the ProfileTypeResource
 *
 * @author Stefan Triller - initial contribution
 *
 */
public class ProfileTypeResourceTest extends JavaTest {

    private ProfileTypeResource ressource;

    // UIDs for state profile types
    private final ProfileTypeUID stateProfileTypeUID1 = new ProfileTypeUID("my:stateProfile1");
    private final ChannelTypeUID pt1ChannelType1UID = new ChannelTypeUID("my:channel1");
    private final ChannelType pt1ChannelType1 = ChannelTypeBuilder.state(pt1ChannelType1UID, "channel1", "Switch")
            .build();

    private final ProfileTypeUID stateProfileTypeUID2 = new ProfileTypeUID("my:stateProfile2");

    // UIDs for trigger profile types
    private final ProfileTypeUID triggerProfileTypeUID1 = new ProfileTypeUID("my:triggerProfile1");
    private final ChannelTypeUID pt3ChannelType1UID = new ChannelTypeUID("my:channel3");
    private final ChannelType pt3ChannelType1 = ChannelTypeBuilder.trigger(pt3ChannelType1UID, "channel1").build();

    private final ProfileTypeUID triggerProfileTypeUID2 = new ProfileTypeUID("my:triggerProfile2");

    // some other channel types for testing
    private final ChannelTypeUID otherStateChannelTypeUID = new ChannelTypeUID("other:stateChannel1");
    private final ChannelType otherStateChannelType = ChannelTypeBuilder
            .state(otherStateChannelTypeUID, "channelState1", "Number").build();
    private final ChannelTypeUID otherTriggerChannelTypeUID = new ChannelTypeUID("other:triggerChannel1");
    private final ChannelType otherTriggerChannelType = ChannelTypeBuilder
            .trigger(otherTriggerChannelTypeUID, "channel1").build();

    @Before
    public void setup() {
        ressource = new ProfileTypeResource();

        ChannelTypeRegistry ctRegistry = mock(ChannelTypeRegistry.class);
        ProfileTypeRegistry ptRegistry = mock(ProfileTypeRegistry.class);

        List<ProfileType> profileTypes = new ArrayList<>();
        ProfileType pt1 = ProfileTypeBuilder.newState(stateProfileTypeUID1, "profile1")
                .withSupportedChannelTypeUIDs(pt1ChannelType1UID).withSupportedItemTypesOfChannel("Switch").build();
        ProfileType pt2 = ProfileTypeBuilder.newState(stateProfileTypeUID2, "profile2").build();
        ProfileType pt3 = ProfileTypeBuilder.newTrigger(triggerProfileTypeUID1, "profile3")
                .withSupportedChannelTypeUIDs(pt3ChannelType1UID).build();
        ProfileType pt4 = ProfileTypeBuilder.newTrigger(triggerProfileTypeUID2, "profile4").build();

        profileTypes.add(pt1);
        profileTypes.add(pt2);
        profileTypes.add(pt3);
        profileTypes.add(pt4);

        when(ptRegistry.getProfileTypes(any())).thenReturn(profileTypes);
        when(ctRegistry.getChannelType(pt1ChannelType1UID, null)).thenReturn(pt1ChannelType1);
        when(ctRegistry.getChannelType(pt3ChannelType1UID, null)).thenReturn(pt3ChannelType1);

        when(ctRegistry.getChannelType(otherStateChannelTypeUID, null)).thenReturn(otherStateChannelType);
        when(ctRegistry.getChannelType(otherTriggerChannelTypeUID, null)).thenReturn(otherTriggerChannelType);

        ressource.setChannelTypeRegistry(ctRegistry);
        ressource.setProfileTypeRegistry(ptRegistry);
    }

    @Test
    public void testGetAll() {
        Stream<ProfileTypeDTO> result = ressource.getProfileTypes(null, null, null);

        List<ProfileTypeDTO> list = result.collect(Collectors.toList());
        assertThat(list.size(), is(4));
    }

    @Test
    public void testGetProfileTypesForStateChannel1() {
        Stream<ProfileTypeDTO> result = ressource.getProfileTypes(null, pt1ChannelType1UID.toString(), null);
        List<ProfileTypeDTO> list = result.collect(Collectors.toList());

        // should be both state profiles because the second state profile supports ALL item types on the channel side
        assertThat(list.size(), is(2));

        for (ProfileTypeDTO p : list) {
            assertThat(p.kind, is("STATE"));
        }
    }

    @Test
    public void testGetProfileTypesForOtherChannel() {
        Stream<ProfileTypeDTO> result = ressource.getProfileTypes(null, otherStateChannelTypeUID.toString(), null);
        List<ProfileTypeDTO> list = result.collect(Collectors.toList());

        // should be only the second state profile because the first one is restricted to another item type on the
        // channel side
        assertThat(list.size(), is(1));

        ProfileTypeDTO pt = list.get(0);
        assertThat(pt.kind, is("STATE"));
        assertThat(pt.label, is("profile2"));
        assertThat(pt.uid, is(stateProfileTypeUID2.toString()));
    }

    @Test
    public void testGetProfileTypesForTriggerChannel1() {
        Stream<ProfileTypeDTO> result = ressource.getProfileTypes(null, pt3ChannelType1UID.toString(), null);
        List<ProfileTypeDTO> list = result.collect(Collectors.toList());

        // should be both trigger profiles because the second trigger profile supports ALL channel types
        assertThat(list.size(), is(2));

        for (ProfileTypeDTO p : list) {
            assertThat(p.kind, is("TRIGGER"));
        }
    }

    @Test
    public void testGetProfileTypesForTriggerChannel2() {
        Stream<ProfileTypeDTO> result = ressource.getProfileTypes(null, otherTriggerChannelTypeUID.toString(), null);
        List<ProfileTypeDTO> list = result.collect(Collectors.toList());

        // should be only the second trigger profile because the first one is restricted to another channel type UID
        assertThat(list.size(), is(1));

        ProfileTypeDTO pt = list.get(0);
        assertThat(pt.kind, is("TRIGGER"));
        assertThat(pt.label, is("profile4"));
        assertThat(pt.uid, is(triggerProfileTypeUID2.toString()));
    }
}
