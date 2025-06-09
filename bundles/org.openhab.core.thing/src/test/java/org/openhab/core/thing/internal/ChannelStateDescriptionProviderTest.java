/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.thing.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.DynamicStateDescriptionProvider;
import org.openhab.core.thing.type.ThingTypeRegistry;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateDescriptionFragment;
import org.openhab.core.types.StateDescriptionFragmentBuilder;

/**
 * @author Maksym Krasovskyi - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
public class ChannelStateDescriptionProviderTest {

    private @NonNullByDefault({}) ChannelStateDescriptionProvider channelStateDescriptionProvider;
    private @Mock @NonNullByDefault({}) DynamicStateDescriptionProvider dynamicStateDescriptionProvider;
    private @Mock @NonNullByDefault({}) ItemChannelLinkRegistry itemChannelLinkRegistry;
    private @Mock @NonNullByDefault({}) ThingTypeRegistry thingTypeRegistry;
    private @Mock @NonNullByDefault({}) ThingRegistry thingRegistry;

    private static final ChannelUID CHANNEL_UID_1 = new ChannelUID("channel:f:g:1");
    private static final ChannelUID CHANNEL_UID_2 = new ChannelUID("channel:f:g:2");

    private static final String ITEM_1 = "item1";

    @BeforeEach
    public void setup() {
        channelStateDescriptionProvider = new ChannelStateDescriptionProvider(itemChannelLinkRegistry,
                thingTypeRegistry, thingRegistry);
    }

    @ParameterizedTest
    @CsvSource({ "true, true, true", "true, false, false", "false, true, false", "false, false, false" })
    public void testStateDescriptionFromMultipleChannels(Boolean channel1State, Boolean channel2State,
            Boolean expectedItemState) {
        when(itemChannelLinkRegistry.getBoundChannels(ITEM_1))
                .thenReturn(new HashSet<>(Arrays.asList(CHANNEL_UID_1, CHANNEL_UID_2)));
        // Setup channel 1
        Channel channel1 = ChannelBuilder.create(CHANNEL_UID_1).build();
        when(thingRegistry.getChannel(CHANNEL_UID_1)).thenReturn(channel1);
        StateDescription stateDescription1 = StateDescriptionFragmentBuilder.create().withMinimum(BigDecimal.ZERO)
                .withMaximum(new BigDecimal(100)).withStep(BigDecimal.ONE).withReadOnly(channel1State).withPattern("%s")
                .build().toStateDescription();

        ChannelType channelType1 = Mockito.mock(ChannelType.class);

        when(channelType1.getState()).thenReturn(stateDescription1);
        when(thingTypeRegistry.getChannelType(channel1, Locale.ENGLISH)).thenReturn(channelType1);
        when(dynamicStateDescriptionProvider.getStateDescription(channel1, stateDescription1, Locale.ENGLISH))
                .thenReturn(StateDescriptionFragmentBuilder.create(stateDescription1).build().toStateDescription());

        // Setup channel 2
        Channel channel2 = ChannelBuilder.create(CHANNEL_UID_2).build();
        when(thingRegistry.getChannel(CHANNEL_UID_2)).thenReturn(channel2);
        StateDescription stateDescription2 = StateDescriptionFragmentBuilder.create().withMinimum(BigDecimal.ZERO)
                .withMaximum(new BigDecimal(100)).withStep(BigDecimal.ONE).withReadOnly(channel2State).withPattern("%s")
                .build().toStateDescription();
        ChannelType channelType2 = Mockito.mock(ChannelType.class);
        when(channelType2.getState()).thenReturn(stateDescription2);
        when(thingTypeRegistry.getChannelType(channel2, Locale.ENGLISH)).thenReturn(channelType2);
        when(dynamicStateDescriptionProvider.getStateDescription(channel2, stateDescription2, Locale.ENGLISH))
                .thenReturn(StateDescriptionFragmentBuilder.create(stateDescription2).build().toStateDescription());

        channelStateDescriptionProvider.addDynamicStateDescriptionProvider(dynamicStateDescriptionProvider);

        @Nullable
        StateDescriptionFragment stateDescriptionResult = channelStateDescriptionProvider
                .getStateDescriptionFragment(ITEM_1, Locale.ENGLISH);
        assertNotNull(stateDescriptionResult);
        assertEquals(expectedItemState, stateDescriptionResult.isReadOnly());
    }

    @Test
    public void testStateDescriptionWithSingleReadOnlyChannel() {
        when(itemChannelLinkRegistry.getBoundChannels(ITEM_1)).thenReturn(new HashSet<>(Arrays.asList(CHANNEL_UID_1)));
        // Setup channel 1
        Channel channel1 = ChannelBuilder.create(CHANNEL_UID_1).build();
        when(thingRegistry.getChannel(CHANNEL_UID_1)).thenReturn(channel1);
        StateDescription stateDescription1 = StateDescriptionFragmentBuilder.create().withMinimum(BigDecimal.ZERO)
                .withMaximum(new BigDecimal(100)).withStep(BigDecimal.ONE).withReadOnly(Boolean.TRUE).withPattern("%s")
                .build().toStateDescription();
        ChannelType channelType = Mockito.mock(ChannelType.class);
        when(channelType.getState()).thenReturn(stateDescription1);
        when(thingTypeRegistry.getChannelType(channel1, Locale.ENGLISH)).thenReturn(channelType);

        when(dynamicStateDescriptionProvider.getStateDescription(channel1, stateDescription1, Locale.ENGLISH))
                .thenReturn(StateDescriptionFragmentBuilder.create(stateDescription1).build().toStateDescription());

        channelStateDescriptionProvider.addDynamicStateDescriptionProvider(dynamicStateDescriptionProvider);

        @Nullable
        StateDescriptionFragment stateDescriptionResult = channelStateDescriptionProvider
                .getStateDescriptionFragment(ITEM_1, Locale.ENGLISH);
        assertNotNull(stateDescriptionResult);
        assertTrue(stateDescriptionResult.isReadOnly());
    }

    @Test
    public void testStateDescriptionWithSingleWriteOnlyChannel() {
        when(itemChannelLinkRegistry.getBoundChannels(ITEM_1)).thenReturn(new HashSet<>(Arrays.asList(CHANNEL_UID_1)));
        // Setup channel 1
        Channel channel1 = ChannelBuilder.create(CHANNEL_UID_1).build();
        when(thingRegistry.getChannel(CHANNEL_UID_1)).thenReturn(channel1);
        StateDescription stateDescription1 = StateDescriptionFragmentBuilder.create().withMinimum(BigDecimal.ZERO)
                .withMaximum(new BigDecimal(100)).withStep(BigDecimal.ONE).withReadOnly(Boolean.FALSE).withPattern("%s")
                .build().toStateDescription();
        ChannelType channelType = Mockito.mock(ChannelType.class);
        when(channelType.getState()).thenReturn(stateDescription1);
        when(thingTypeRegistry.getChannelType(channel1, Locale.ENGLISH)).thenReturn(channelType);

        when(dynamicStateDescriptionProvider.getStateDescription(channel1, stateDescription1, Locale.ENGLISH))
                .thenReturn(StateDescriptionFragmentBuilder.create(stateDescription1).build().toStateDescription());

        channelStateDescriptionProvider.addDynamicStateDescriptionProvider(dynamicStateDescriptionProvider);

        @Nullable
        StateDescriptionFragment stateDescriptionResult = channelStateDescriptionProvider
                .getStateDescriptionFragment(ITEM_1, Locale.ENGLISH);
        assertNotNull(stateDescriptionResult);
        assertFalse(stateDescriptionResult.isReadOnly());
    }
}
