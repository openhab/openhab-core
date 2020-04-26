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
package org.openhab.core.io.rest.core.internal.channel;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.hamcrest.core.IsCollectionContaining;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openhab.core.io.rest.LocaleServiceImpl;
import org.openhab.core.thing.profiles.ProfileTypeRegistry;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.TriggerProfileType;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.thing.type.ChannelTypeUID;

/**
 * @author Henning Treu - Initial contribution
 */
public class ChannelTypeResourceTest {

    private ChannelTypeResource channelTypeResource;

    @Mock
    private ChannelTypeRegistry channelTypeRegistry;

    @Mock
    private ProfileTypeRegistry profileTypeRegistry;

    @Before
    public void setup() {
        initMocks(this);
        channelTypeResource = new ChannelTypeResource();
        channelTypeResource.setLocaleService(new LocaleServiceImpl());
        channelTypeResource.setChannelTypeRegistry(channelTypeRegistry);
        channelTypeResource.setProfileTypeRegistry(profileTypeRegistry);
    }

    @Test
    public void getAllShouldRetrieveAllChannelTypes() throws Exception {
        channelTypeResource.getAll(null, null);
        verify(channelTypeRegistry).getChannelTypes(any(Locale.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void returnLinkableItemTypesForTriggerChannelType() throws IOException {
        ChannelType channelType = mockChannelType("ct");
        ChannelTypeUID uid = channelType.getUID();
        ProfileTypeUID profileTypeUID = new ProfileTypeUID("system:profileType");

        when(channelTypeRegistry.getChannelType(uid)).thenReturn(channelType);

        TriggerProfileType profileType = mock(TriggerProfileType.class);
        when(profileType.getUID()).thenReturn(profileTypeUID);
        when(profileType.getSupportedChannelTypeUIDs()).thenReturn(Collections.singletonList(uid));
        when(profileType.getSupportedItemTypes()).thenReturn(Arrays.asList("Switch", "Dimmer"));

        when(profileTypeRegistry.getProfileTypes()).thenReturn(Collections.singletonList(profileType));

        Response response = channelTypeResource.getLinkableItemTypes(uid.getAsString());

        verify(channelTypeRegistry).getChannelType(uid);
        verify(profileTypeRegistry).getProfileTypes();
        assertThat(response.getStatus(), is(200));
        assertThat((Set<String>) response.getEntity(), IsCollectionContaining.hasItems("Switch", "Dimmer"));
    }

    private ChannelType mockChannelType(String channelId) {
        return new ChannelType(new ChannelTypeUID("binding", channelId), false, null, ChannelKind.TRIGGER, "Label",
                null, null, null, null, null, null);
    }
}
