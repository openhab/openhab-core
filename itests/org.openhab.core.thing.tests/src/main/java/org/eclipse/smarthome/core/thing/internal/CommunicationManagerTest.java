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
package org.eclipse.smarthome.core.thing.internal;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.measure.quantity.Temperature;

import org.eclipse.smarthome.core.common.SafeCaller;
import org.eclipse.smarthome.core.common.registry.Provider;
import org.eclipse.smarthome.core.common.registry.ProviderChangeListener;
import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.i18n.UnitProvider;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.items.events.ItemCommandEvent;
import org.eclipse.smarthome.core.items.events.ItemEventFactory;
import org.eclipse.smarthome.core.library.CoreItemFactory;
import org.eclipse.smarthome.core.library.items.NumberItem;
import org.eclipse.smarthome.core.library.items.SwitchItem;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.unit.SIUnits;
import org.eclipse.smarthome.core.service.StateDescriptionService;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.thing.events.ThingEventFactory;
import org.eclipse.smarthome.core.thing.internal.profiles.SystemProfileFactory;
import org.eclipse.smarthome.core.thing.link.ItemChannelLink;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkProvider;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
import org.eclipse.smarthome.core.thing.profiles.ProfileAdvisor;
import org.eclipse.smarthome.core.thing.profiles.ProfileCallback;
import org.eclipse.smarthome.core.thing.profiles.ProfileContext;
import org.eclipse.smarthome.core.thing.profiles.ProfileFactory;
import org.eclipse.smarthome.core.thing.profiles.ProfileTypeUID;
import org.eclipse.smarthome.core.thing.profiles.StateProfile;
import org.eclipse.smarthome.core.thing.profiles.TriggerProfile;
import org.eclipse.smarthome.core.thing.type.ChannelKind;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeRegistry;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.StateDescriptionFragmentBuilder;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

/**
 *
 * @author Simon Kaufmann - initial contribution and API.
 *
 */
public class CommunicationManagerTest extends JavaOSGiTest {

    private class ItemChannelLinkRegistryAdvanced extends ItemChannelLinkRegistry {
        @Override
        protected void addProvider(Provider<ItemChannelLink> provider) {
            super.addProvider(provider);
        }
    }

    private static final String EVENT = "event";
    private static final String ITEM_NAME_1 = "testItem1";
    private static final String ITEM_NAME_2 = "testItem2";
    private static final String ITEM_NAME_3 = "testItem3";
    private static final String ITEM_NAME_4 = "testItem4";
    private static final SwitchItem ITEM_1 = new SwitchItem(ITEM_NAME_1);
    private static final SwitchItem ITEM_2 = new SwitchItem(ITEM_NAME_2);
    private static final NumberItem ITEM_3 = new NumberItem(ITEM_NAME_3);
    private static final NumberItem ITEM_4 = new NumberItem(ITEM_NAME_4);
    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID("test", "type");
    private static final ThingUID THING_UID = new ThingUID("test", "thing");
    private static final ChannelUID STATE_CHANNEL_UID_1 = new ChannelUID(THING_UID, "state-channel1");
    private static final ChannelUID STATE_CHANNEL_UID_2 = new ChannelUID(THING_UID, "state-channel2");
    private static final ChannelUID STATE_CHANNEL_UID_3 = new ChannelUID(THING_UID, "state-channel3");
    private static final ChannelUID STATE_CHANNEL_UID_4 = new ChannelUID(THING_UID, "state-channel4");
    private static final ChannelTypeUID CHANNEL_TYPE_UID_4 = new ChannelTypeUID("test", "channeltype");
    private static final ChannelUID TRIGGER_CHANNEL_UID_1 = new ChannelUID(THING_UID, "trigger-channel1");
    private static final ChannelUID TRIGGER_CHANNEL_UID_2 = new ChannelUID(THING_UID, "trigger-channel2");
    private static final ItemChannelLink LINK_1_S1 = new ItemChannelLink(ITEM_NAME_1, STATE_CHANNEL_UID_1);
    private static final ItemChannelLink LINK_1_S2 = new ItemChannelLink(ITEM_NAME_1, STATE_CHANNEL_UID_2);
    private static final ItemChannelLink LINK_2_S2 = new ItemChannelLink(ITEM_NAME_2, STATE_CHANNEL_UID_2);
    private static final ItemChannelLink LINK_3_S3 = new ItemChannelLink(ITEM_NAME_3, STATE_CHANNEL_UID_3);
    private static final ItemChannelLink LINK_4_S4 = new ItemChannelLink(ITEM_NAME_4, STATE_CHANNEL_UID_4);
    private static final ItemChannelLink LINK_1_T1 = new ItemChannelLink(ITEM_NAME_1, TRIGGER_CHANNEL_UID_1);
    private static final ItemChannelLink LINK_1_T2 = new ItemChannelLink(ITEM_NAME_1, TRIGGER_CHANNEL_UID_2);
    private static final ItemChannelLink LINK_2_T2 = new ItemChannelLink(ITEM_NAME_2, TRIGGER_CHANNEL_UID_2);
    private static final Thing THING = ThingBuilder.create(THING_TYPE_UID, THING_UID).withChannels(
            ChannelBuilder.create(STATE_CHANNEL_UID_1, "").withKind(ChannelKind.STATE).build(),
            ChannelBuilder.create(STATE_CHANNEL_UID_2, "").withKind(ChannelKind.STATE).build(),
            ChannelBuilder.create(STATE_CHANNEL_UID_3, "Number:Temperature").withKind(ChannelKind.STATE).build(),
            ChannelBuilder.create(STATE_CHANNEL_UID_4, "Number").withKind(ChannelKind.STATE)
                    .withType(CHANNEL_TYPE_UID_4).build(),
            ChannelBuilder.create(TRIGGER_CHANNEL_UID_1, "").withKind(ChannelKind.TRIGGER).build(),
            ChannelBuilder.create(TRIGGER_CHANNEL_UID_2, "").withKind(ChannelKind.TRIGGER).build()).build();

    private CommunicationManager manager;

    @Mock
    private ProfileFactory mockProfileFactory;

    @Mock
    private ProfileAdvisor mockProfileAdvisor;

    @Mock
    private StateProfile stateProfile;

    @Mock
    private TriggerProfile triggerProfile;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private ItemRegistry itemRegistry;

    @Mock
    private ThingRegistry thingRegistry;

    @Mock
    private ThingHandler mockHandler;

    @Mock
    private AutoUpdateManager mockAutoUpdateManager;

    @Mock
    private ChannelTypeRegistry channelTypeRegistry;

    private SafeCaller safeCaller;

    @Before
    public void setup() {
        initMocks(this);

        safeCaller = getService(SafeCaller.class);
        assertNotNull(safeCaller);

        manager = new CommunicationManager();
        manager.setEventPublisher(eventPublisher);
        manager.setDefaultProfileFactory(new SystemProfileFactory());
        manager.setSafeCaller(safeCaller);

        doAnswer(invocation -> {
            switch (((Channel) invocation.getArguments()[0]).getKind()) {
                case STATE:
                    return new ProfileTypeUID("test:state");
                case TRIGGER:
                    return new ProfileTypeUID("test:trigger");
            }
            return null;
        }).when(mockProfileAdvisor).getSuggestedProfileTypeUID(isA(Channel.class), isA(String.class));
        doAnswer(invocation -> {
            switch (((ProfileTypeUID) invocation.getArguments()[0]).toString()) {
                case "test:state":
                    return stateProfile;
                case "test:trigger":
                    return triggerProfile;
            }
            return null;
        }).when(mockProfileFactory).createProfile(isA(ProfileTypeUID.class), isA(ProfileCallback.class),
                isA(ProfileContext.class));

        when(mockProfileFactory.getSupportedProfileTypeUIDs()).thenReturn(Stream
                .of(new ProfileTypeUID("test:state"), new ProfileTypeUID("test:trigger")).collect(Collectors.toList()));

        manager.addProfileFactory(mockProfileFactory);
        manager.addProfileAdvisor(mockProfileAdvisor);

        ItemChannelLinkRegistryAdvanced iclRegistry = new ItemChannelLinkRegistryAdvanced();
        iclRegistry.addProvider(new ItemChannelLinkProvider() {
            @Override
            public void addProviderChangeListener(ProviderChangeListener<ItemChannelLink> listener) {
            }

            @Override
            public void removeProviderChangeListener(ProviderChangeListener<ItemChannelLink> listener) {
            }

            @Override
            public Collection<ItemChannelLink> getAll() {
                return Arrays.asList(LINK_1_S1, LINK_1_S2, LINK_2_S2, LINK_1_T1, LINK_1_T2, LINK_2_T2, LINK_3_S3,
                        LINK_4_S4);
            }
        });
        manager.setItemChannelLinkRegistry(iclRegistry);

        when(itemRegistry.get(eq(ITEM_NAME_1))).thenReturn(ITEM_1);
        when(itemRegistry.get(eq(ITEM_NAME_2))).thenReturn(ITEM_2);
        when(itemRegistry.get(eq(ITEM_NAME_3))).thenReturn(ITEM_3);
        when(itemRegistry.get(eq(ITEM_NAME_4))).thenReturn(ITEM_4);
        manager.setItemRegistry(itemRegistry);

        ChannelType channelType4 = mock(ChannelType.class);
        when(channelType4.getItemType()).thenReturn("Number:Temperature");

        when(channelTypeRegistry.getChannelType(CHANNEL_TYPE_UID_4)).thenReturn(channelType4);
        manager.setChannelTypeRegistry(channelTypeRegistry);

        THING.setHandler(mockHandler);

        when(thingRegistry.get(eq(THING_UID))).thenReturn(THING);
        manager.setThingRegistry(thingRegistry);
        manager.addItemFactory(new CoreItemFactory());
        manager.setAutoUpdateManager(mockAutoUpdateManager);

        UnitProvider unitProvider = mock(UnitProvider.class);
        when(unitProvider.getUnit(Temperature.class)).thenReturn(SIUnits.CELSIUS);
        ITEM_3.setUnitProvider(unitProvider);
        ITEM_4.setUnitProvider(unitProvider);
    }

    @Test
    public void testStateUpdated_singleLink() {
        manager.stateUpdated(STATE_CHANNEL_UID_1, OnOffType.ON);
        waitForAssert(() -> {
            verify(stateProfile).onStateUpdateFromHandler(eq(OnOffType.ON));
        });
        verifyNoMoreInteractions(stateProfile);
        verifyNoMoreInteractions(triggerProfile);
    }

    @Test
    public void testStateUpdated_multiLink() {
        manager.stateUpdated(STATE_CHANNEL_UID_2, OnOffType.ON);
        waitForAssert(() -> {
            verify(stateProfile, times(2)).onStateUpdateFromHandler(eq(OnOffType.ON));
        });
        verifyNoMoreInteractions(stateProfile);
        verifyNoMoreInteractions(triggerProfile);
    }

    @Test
    public void testPostCommand_singleLink() {
        manager.postCommand(STATE_CHANNEL_UID_1, OnOffType.ON);
        waitForAssert(() -> {
            verify(stateProfile).onCommandFromHandler(eq(OnOffType.ON));
        });
        verifyNoMoreInteractions(stateProfile);
        verifyNoMoreInteractions(triggerProfile);
    }

    @Test
    public void testPostCommand_multiLink() {
        manager.postCommand(STATE_CHANNEL_UID_2, OnOffType.ON);
        waitForAssert(() -> {
            verify(stateProfile, times(2)).onCommandFromHandler(eq(OnOffType.ON));
        });
        verifyNoMoreInteractions(stateProfile);
        verifyNoMoreInteractions(triggerProfile);
    }

    @Test
    public void testItemCommandEvent_singleLink() {
        manager.receive(ItemEventFactory.createCommandEvent(ITEM_NAME_2, OnOffType.ON));
        waitForAssert(() -> {
            verify(stateProfile).onCommandFromItem(eq(OnOffType.ON));
        });
        verifyNoMoreInteractions(stateProfile);
        verifyNoMoreInteractions(triggerProfile);
        verify(mockAutoUpdateManager).receiveCommand(isA(ItemCommandEvent.class), isA(Item.class));
    }

    @Test
    public void testItemCommandEvent_Decimal2Quantity() {
        // Take unit from accepted item type (see channel built from STATE_CHANNEL_UID_3)
        manager.receive(ItemEventFactory.createCommandEvent(ITEM_NAME_3, DecimalType.valueOf("20")));
        waitForAssert(() -> {
            verify(stateProfile).onCommandFromItem(eq(QuantityType.valueOf("20 °C")));
        });
        verifyNoMoreInteractions(stateProfile);
        verifyNoMoreInteractions(triggerProfile);
    }

    @Test
    public void testItemCommandEvent_Decimal2Quantity_2() {
        // Take unit from state description
        StateDescriptionService stateDescriptionService = mock(StateDescriptionService.class);
        when(stateDescriptionService.getStateDescription(ITEM_NAME_3, null)).thenReturn(
                StateDescriptionFragmentBuilder.create().withPattern("%.1f °F").build().toStateDescription());
        ITEM_3.setStateDescriptionService(stateDescriptionService);

        manager.receive(ItemEventFactory.createCommandEvent(ITEM_NAME_3, DecimalType.valueOf("20")));
        waitForAssert(() -> {
            verify(stateProfile).onCommandFromItem(eq(QuantityType.valueOf("20 °F")));
        });
        verifyNoMoreInteractions(stateProfile);
        verifyNoMoreInteractions(triggerProfile);

        ITEM_3.setStateDescriptionService(null);
    }

    @Test
    public void testItemCommandEvent_Decimal2Quantity_ChannelType() {
        // The command is sent to an item w/o dimension defined and the channel is legacy (created from a ThingType
        // definition before UoM was introduced to the binding). The dimension information might now be defined on the
        // current ThingType.
        manager.receive(ItemEventFactory.createCommandEvent(ITEM_NAME_4, DecimalType.valueOf("20")));
        waitForAssert(() -> {
            verify(stateProfile).onCommandFromItem(eq(QuantityType.valueOf("20 °C")));
        });
        verifyNoMoreInteractions(stateProfile);
        verifyNoMoreInteractions(triggerProfile);
    }

    @Test
    public void testItemCommandEvent_multiLink() {
        manager.receive(ItemEventFactory.createCommandEvent(ITEM_NAME_1, OnOffType.ON));
        waitForAssert(() -> {
            verify(stateProfile, times(2)).onCommandFromItem(eq(OnOffType.ON));
        });
        verifyNoMoreInteractions(stateProfile);
        verifyNoMoreInteractions(triggerProfile);
        verify(mockAutoUpdateManager).receiveCommand(isA(ItemCommandEvent.class), isA(Item.class));
    }

    @Test
    public void testItemCommandEvent_notToSource() {
        manager.receive(
                ItemEventFactory.createCommandEvent(ITEM_NAME_1, OnOffType.ON, STATE_CHANNEL_UID_2.getAsString()));
        waitForAssert(() -> {
            verify(stateProfile).onCommandFromItem(eq(OnOffType.ON));
        });
        verifyNoMoreInteractions(stateProfile);
        verifyNoMoreInteractions(triggerProfile);
        verify(mockAutoUpdateManager).receiveCommand(isA(ItemCommandEvent.class), isA(Item.class));
    }

    @Test
    public void testItemStateEvent_singleLink() {
        manager.receive(ItemEventFactory.createStateEvent(ITEM_NAME_2, OnOffType.ON));
        waitForAssert(() -> {
            verify(stateProfile).onStateUpdateFromItem(eq(OnOffType.ON));
            verify(triggerProfile).onStateUpdateFromItem(eq(OnOffType.ON));
        });
        verifyNoMoreInteractions(stateProfile);
        verifyNoMoreInteractions(triggerProfile);
    }

    @Test
    public void testItemStateEvent_multiLink() {
        manager.receive(ItemEventFactory.createStateEvent(ITEM_NAME_1, OnOffType.ON));
        waitForAssert(() -> {
            verify(stateProfile, times(2)).onStateUpdateFromItem(eq(OnOffType.ON));
            verify(triggerProfile, times(2)).onStateUpdateFromItem(eq(OnOffType.ON));
        });
        verifyNoMoreInteractions(stateProfile);
        verifyNoMoreInteractions(triggerProfile);
    }

    @Test
    public void testItemStateEvent_notToSource() {
        manager.receive(
                ItemEventFactory.createStateEvent(ITEM_NAME_1, OnOffType.ON, STATE_CHANNEL_UID_2.getAsString()));
        waitForAssert(() -> {
            verify(stateProfile).onStateUpdateFromItem(eq(OnOffType.ON));
            verify(triggerProfile, times(2)).onStateUpdateFromItem(eq(OnOffType.ON));
        });
        verifyNoMoreInteractions(stateProfile);
        verifyNoMoreInteractions(triggerProfile);
    }

    @Test
    public void testChannelTriggeredEvent_singleLink() {
        manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_1));
        waitForAssert(() -> {
            verify(triggerProfile).onTriggerFromHandler(eq(EVENT));
        });
        verifyNoMoreInteractions(stateProfile);
        verifyNoMoreInteractions(triggerProfile);
    }

    @Test
    public void testChannelTriggeredEvent_multiLink() {
        manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_2));
        waitForAssert(() -> {
            verify(triggerProfile, times(2)).onTriggerFromHandler(eq(EVENT));
        });
        verifyNoMoreInteractions(stateProfile);
        verifyNoMoreInteractions(triggerProfile);
    }

    @Test
    public void testProfileIsReused() {
        for (int i = 0; i < 3; i++) {
            manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_2));
        }

        waitForAssert(() -> {
            verify(mockProfileFactory, times(2)).createProfile(isA(ProfileTypeUID.class), isA(ProfileCallback.class),
                    isA(ProfileContext.class));
            verify(mockProfileFactory, atLeast(0)).getSupportedProfileTypeUIDs();
            verify(mockProfileAdvisor, atLeast(0)).getSuggestedProfileTypeUID(any(Channel.class), any());
        });
        verifyNoMoreInteractions(mockProfileFactory);
        verifyNoMoreInteractions(mockProfileAdvisor);
    }

    @Test
    public void testProfileIsNotReusedOnFactoryChange() {
        for (int i = 0; i < 3; i++) {
            manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_2));
        }
        verify(mockProfileFactory, times(2)).createProfile(isA(ProfileTypeUID.class), isA(ProfileCallback.class),
                isA(ProfileContext.class));

        manager.removeProfileFactory(mockProfileFactory);
        manager.addProfileFactory(mockProfileFactory);

        for (int i = 0; i < 3; i++) {
            manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_2));
        }

        waitForAssert(() -> {
            verify(mockProfileFactory, times(4)).createProfile(isA(ProfileTypeUID.class), isA(ProfileCallback.class),
                    isA(ProfileContext.class));
            verify(mockProfileFactory, atLeast(0)).getSupportedProfileTypeUIDs();
            verify(mockProfileAdvisor, atLeast(0)).getSuggestedProfileTypeUID(any(Channel.class), any());
        });
        verifyNoMoreInteractions(mockProfileFactory);
        verifyNoMoreInteractions(mockProfileAdvisor);
    }

    @Test
    public void testProfileIsNotReusedOnLinkChange() {
        for (int i = 0; i < 3; i++) {
            manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_2));
        }
        waitForAssert(() -> {
            verify(mockProfileFactory, times(2)).createProfile(isA(ProfileTypeUID.class), isA(ProfileCallback.class),
                    isA(ProfileContext.class));
        });

        manager.removed(LINK_2_T2);
        manager.added(LINK_2_T2);

        for (int i = 0; i < 3; i++) {
            manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_2));
        }

        waitForAssert(() -> {
            verify(mockProfileFactory, times(3)).createProfile(isA(ProfileTypeUID.class), isA(ProfileCallback.class),
                    isA(ProfileContext.class));
            verify(mockProfileFactory, atLeast(0)).getSupportedProfileTypeUIDs();
            verify(mockProfileAdvisor, atLeast(0)).getSuggestedProfileTypeUID(any(Channel.class), any());
        });
        verifyNoMoreInteractions(mockProfileFactory);
        verifyNoMoreInteractions(mockProfileAdvisor);
    }

    @Test
    public void testProfileIsReusedOnUnrelatedLinkChange() {
        for (int i = 0; i < 3; i++) {
            manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_2));
        }

        manager.removed(LINK_1_S1);
        manager.added(LINK_1_S1);

        for (int i = 0; i < 3; i++) {
            manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_2));
        }

        waitForAssert(() -> {
            verify(mockProfileFactory, times(2)).createProfile(isA(ProfileTypeUID.class), isA(ProfileCallback.class),
                    isA(ProfileContext.class));
            verify(mockProfileFactory, atLeast(0)).getSupportedProfileTypeUIDs();
            verify(mockProfileAdvisor, atLeast(0)).getSuggestedProfileTypeUID(any(Channel.class), any());
        });
        verifyNoMoreInteractions(mockProfileFactory);
        verifyNoMoreInteractions(mockProfileAdvisor);
    }

    @Test
    public void testProfileIsNotReusedOnLinkUpdate() {
        for (int i = 0; i < 3; i++) {
            manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_2));
        }
        verify(mockProfileFactory, times(2)).createProfile(isA(ProfileTypeUID.class), isA(ProfileCallback.class),
                isA(ProfileContext.class));

        manager.updated(LINK_2_T2, LINK_2_T2);

        for (int i = 0; i < 3; i++) {
            manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_2));
        }

        waitForAssert(() -> {
            verify(mockProfileFactory, times(3)).createProfile(isA(ProfileTypeUID.class), isA(ProfileCallback.class),
                    isA(ProfileContext.class));
            verify(mockProfileFactory, atLeast(0)).getSupportedProfileTypeUIDs();
            verify(mockProfileAdvisor, atLeast(0)).getSuggestedProfileTypeUID(any(Channel.class), any());
        });
        verifyNoMoreInteractions(mockProfileFactory);
        verifyNoMoreInteractions(mockProfileAdvisor);
    }

    @Test
    public void testItemCommandEvent_typeDowncast() {
        Thing thing = ThingBuilder.create(THING_TYPE_UID, THING_UID)
                .withChannels(ChannelBuilder.create(STATE_CHANNEL_UID_2, "Dimmer").withKind(ChannelKind.STATE).build())
                .build();
        thing.setHandler(mockHandler);
        when(thingRegistry.get(eq(THING_UID))).thenReturn(thing);

        manager.receive(ItemEventFactory.createCommandEvent(ITEM_NAME_2, HSBType.fromRGB(128, 128, 128)));
        waitForAssert(() -> {
            ArgumentCaptor<Command> commandCaptor = ArgumentCaptor.forClass(Command.class);
            verify(stateProfile).onCommandFromItem(commandCaptor.capture());
            Command command = commandCaptor.getValue();
            assertNotNull(command);
            assertEquals(PercentType.class, command.getClass());
        });
        verifyNoMoreInteractions(stateProfile);
        verifyNoMoreInteractions(triggerProfile);
    }

    @Test
    public void testItemStateEvent_typeDowncast() {
        Thing thing = ThingBuilder.create(THING_TYPE_UID, THING_UID)
                .withChannels(ChannelBuilder.create(STATE_CHANNEL_UID_2, "Dimmer").withKind(ChannelKind.STATE).build())
                .build();
        thing.setHandler(mockHandler);
        when(thingRegistry.get(eq(THING_UID))).thenReturn(thing);

        manager.receive(ItemEventFactory.createStateEvent(ITEM_NAME_2, HSBType.fromRGB(128, 128, 128)));
        waitForAssert(() -> {
            ArgumentCaptor<State> stateCaptor = ArgumentCaptor.forClass(State.class);
            verify(stateProfile).onStateUpdateFromItem(stateCaptor.capture());
            State state = stateCaptor.getValue();
            assertNotNull(state);
            assertEquals(PercentType.class, state.getClass());
        });
        verifyNoMoreInteractions(stateProfile);
        verifyNoMoreInteractions(triggerProfile);
    }

}
