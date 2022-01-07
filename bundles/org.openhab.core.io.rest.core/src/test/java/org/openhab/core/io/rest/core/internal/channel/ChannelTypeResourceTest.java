/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.hamcrest.core.IsIterableContaining;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.io.rest.LocaleServiceImpl;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.thing.profiles.ProfileTypeRegistry;
import org.openhab.core.thing.profiles.TriggerProfileType;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeBuilder;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.thing.type.ChannelTypeUID;

/**
 * @author Henning Treu - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
public class ChannelTypeResourceTest {

    private ChannelTypeResource channelTypeResource;

    private @Mock ChannelTypeRegistry channelTypeRegistry;
    private @Mock ConfigDescriptionRegistry configDescriptionRegistry;
    private @Mock LocaleServiceImpl localeService;
    private @Mock ProfileTypeRegistry profileTypeRegistry;

    @BeforeEach
    public void setup() {
        channelTypeResource = new ChannelTypeResource(channelTypeRegistry, configDescriptionRegistry, localeService,
                profileTypeRegistry);
    }

    @Test
    public void getAllShouldRetrieveAllChannelTypes() throws Exception {
        when(localeService.getLocale(null)).thenReturn(Locale.ENGLISH);
        channelTypeResource.getAll(null, null);
        verify(channelTypeRegistry).getChannelTypes(Locale.ENGLISH);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void returnLinkableItemTypesForTriggerChannelType() throws IOException {
        ChannelTypeUID channelTypeUID = new ChannelTypeUID("binding", "ct");
        ChannelType channelType = ChannelTypeBuilder.trigger(channelTypeUID, "Label").build();

        when(channelTypeRegistry.getChannelType(channelTypeUID)).thenReturn(channelType);

        TriggerProfileType profileType = mock(TriggerProfileType.class);
        when(profileType.getSupportedChannelTypeUIDs()).thenReturn(List.of(channelTypeUID));
        when(profileType.getSupportedItemTypes()).thenReturn(List.of(CoreItemFactory.SWITCH, CoreItemFactory.DIMMER));

        when(profileTypeRegistry.getProfileTypes()).thenReturn(List.of(profileType));

        Response response = channelTypeResource.getLinkableItemTypes(channelTypeUID.getAsString());

        verify(channelTypeRegistry).getChannelType(channelTypeUID);
        verify(profileTypeRegistry).getProfileTypes();
        assertThat(response.getStatus(), is(200));
        assertThat((Set<String>) response.getEntity(),
                IsIterableContaining.hasItems(CoreItemFactory.SWITCH, CoreItemFactory.DIMMER));
    }
}
