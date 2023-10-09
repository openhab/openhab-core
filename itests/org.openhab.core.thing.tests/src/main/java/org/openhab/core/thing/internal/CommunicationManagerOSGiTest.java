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
package org.openhab.core.thing.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.List;

import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.common.SafeCaller;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.ItemStateConverter;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.events.ItemCommandEvent;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.ImperialUnits;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.events.ThingEventFactory;
import org.openhab.core.thing.internal.profiles.SystemProfileFactory;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkProvider;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.profiles.ProfileAdvisor;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileFactory;
import org.openhab.core.thing.profiles.ProfileTypeProvider;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.StateProfile;
import org.openhab.core.thing.profiles.TriggerProfile;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;

/**
 *
 * @author Simon Kaufmann - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class CommunicationManagerOSGiTest extends JavaOSGiTest {

    private class ItemChannelLinkRegistryAdvanced extends ItemChannelLinkRegistry {
        public ItemChannelLinkRegistryAdvanced(ThingRegistry thingRegistry, ItemRegistry itemRegistry) {
            super(thingRegistry, itemRegistry);
        }

        @Override
        protected void addProvider(Provider<ItemChannelLink> provider) {
            super.addProvider(provider);
        }
    }

    private static final UnitProvider UNIT_PROVIDER_MOCK = mock(UnitProvider.class);

    private static final String EVENT = "event";
    private static final String ITEM_NAME_1 = "testItem1";
    private static final String ITEM_NAME_2 = "testItem2";
    private static final String ITEM_NAME_3 = "testItem3";
    private static final String ITEM_NAME_4 = "testItem4";
    private static final String ITEM_NAME_5 = "testItem5";
    private static final SwitchItem ITEM_1 = new SwitchItem(ITEM_NAME_1);
    private static final SwitchItem ITEM_2 = new SwitchItem(ITEM_NAME_2);
    private static final NumberItem ITEM_3 = new NumberItem(ITEM_NAME_3);
    private static final NumberItem ITEM_4 = new NumberItem(ITEM_NAME_4);
    private NumberItem item5 = new NumberItem(ITEM_NAME_5); // will be replaced later by dimension item

    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID("test", "type");
    private static final ThingUID THING_UID = new ThingUID("test", "thing");
    private static final ChannelUID STATE_CHANNEL_UID_1 = new ChannelUID(THING_UID, "state-channel1");
    private static final ChannelUID STATE_CHANNEL_UID_2 = new ChannelUID(THING_UID, "state-channel2");
    private static final ChannelUID STATE_CHANNEL_UID_3 = new ChannelUID(THING_UID, "state-channel3");
    private static final ChannelUID STATE_CHANNEL_UID_4 = new ChannelUID(THING_UID, "state-channel4");
    private static final ChannelUID STATE_CHANNEL_UID_5 = new ChannelUID(THING_UID, "state-channel5");
    private static final ChannelTypeUID CHANNEL_TYPE_UID_4 = new ChannelTypeUID("test", "channeltype");
    private static final ChannelUID TRIGGER_CHANNEL_UID_1 = new ChannelUID(THING_UID, "trigger-channel1");
    private static final ChannelUID TRIGGER_CHANNEL_UID_2 = new ChannelUID(THING_UID, "trigger-channel2");
    private static final ItemChannelLink LINK_1_S1 = new ItemChannelLink(ITEM_NAME_1, STATE_CHANNEL_UID_1);
    private static final ItemChannelLink LINK_1_S2 = new ItemChannelLink(ITEM_NAME_1, STATE_CHANNEL_UID_2);
    private static final ItemChannelLink LINK_2_S2 = new ItemChannelLink(ITEM_NAME_2, STATE_CHANNEL_UID_2);
    private static final ItemChannelLink LINK_3_S3 = new ItemChannelLink(ITEM_NAME_3, STATE_CHANNEL_UID_3);
    private static final ItemChannelLink LINK_4_S4 = new ItemChannelLink(ITEM_NAME_4, STATE_CHANNEL_UID_4);
    private static final ItemChannelLink LINK_5_S5 = new ItemChannelLink(ITEM_NAME_5, STATE_CHANNEL_UID_5);
    private static final ItemChannelLink LINK_1_T1 = new ItemChannelLink(ITEM_NAME_1, TRIGGER_CHANNEL_UID_1);
    private static final ItemChannelLink LINK_1_T2 = new ItemChannelLink(ITEM_NAME_1, TRIGGER_CHANNEL_UID_2);
    private static final ItemChannelLink LINK_2_T2 = new ItemChannelLink(ITEM_NAME_2, TRIGGER_CHANNEL_UID_2);
    private static final Thing THING = ThingBuilder.create(THING_TYPE_UID, THING_UID).withChannels(
            ChannelBuilder.create(STATE_CHANNEL_UID_1).withKind(ChannelKind.STATE).build(),
            ChannelBuilder.create(STATE_CHANNEL_UID_2).withKind(ChannelKind.STATE).build(),
            ChannelBuilder.create(STATE_CHANNEL_UID_3, "Number:Temperature").withKind(ChannelKind.STATE).build(),
            ChannelBuilder.create(STATE_CHANNEL_UID_4, CoreItemFactory.NUMBER).withKind(ChannelKind.STATE)
                    .withType(CHANNEL_TYPE_UID_4).build(),
            ChannelBuilder.create(STATE_CHANNEL_UID_5, "Number:Temperature").withKind(ChannelKind.STATE).build(),
            ChannelBuilder.create(TRIGGER_CHANNEL_UID_1).withKind(ChannelKind.TRIGGER).build(),
            ChannelBuilder.create(TRIGGER_CHANNEL_UID_2).withKind(ChannelKind.TRIGGER).build()).build();

    private @Mock @NonNullByDefault({}) AutoUpdateManager autoUpdateManagerMock;
    private @Mock @NonNullByDefault({}) EventPublisher eventPublisherMock;
    private @Mock @NonNullByDefault({}) ItemRegistry itemRegistryMock;
    private @Mock @NonNullByDefault({}) ItemStateConverter itemStateConverterMock;
    private @Mock @NonNullByDefault({}) ProfileAdvisor profileAdvisorMock;
    private @Mock @NonNullByDefault({}) ProfileFactory profileFactoryMock;
    private @Mock @NonNullByDefault({}) StateProfile stateProfileMock;
    private @Mock @NonNullByDefault({}) ThingHandler thingHandlerMock;
    private @Mock @NonNullByDefault({}) ThingRegistry thingRegistryMock;
    private @Mock @NonNullByDefault({}) TriggerProfile triggerProfileMock;

    private @NonNullByDefault({}) CommunicationManager manager;
    private @NonNullByDefault({}) SafeCaller safeCaller;

    private ItemChannelLinkRegistryAdvanced iclRegistry = new ItemChannelLinkRegistryAdvanced(thingRegistryMock,
            itemRegistryMock);

    @BeforeEach
    public void beforeEach() {
        when(UNIT_PROVIDER_MOCK.getUnit(Temperature.class)).thenReturn(SIUnits.CELSIUS);
        item5 = new NumberItem("Number:Temperature", ITEM_NAME_5, UNIT_PROVIDER_MOCK);

        safeCaller = getService(SafeCaller.class);
        assertNotNull(safeCaller);

        SystemProfileFactory profileFactory = getService(ProfileTypeProvider.class, SystemProfileFactory.class);

        assertNotNull(profileFactory);

        manager = new CommunicationManager(autoUpdateManagerMock, profileFactory, iclRegistry, itemRegistryMock,
                itemStateConverterMock, eventPublisherMock, safeCaller, thingRegistryMock);

        doAnswer(invocation -> {
            switch (((Channel) invocation.getArguments()[0]).getKind()) {
                case STATE:
                    return new ProfileTypeUID("test:state");
                case TRIGGER:
                    return new ProfileTypeUID("test:trigger");
            }
            return null;
        }).when(profileAdvisorMock).getSuggestedProfileTypeUID(isA(Channel.class), isA(String.class));
        doAnswer(invocation -> {
            switch (((ProfileTypeUID) invocation.getArguments()[0]).toString()) {
                case "test:state":
                    return stateProfileMock;
                case "test:trigger":
                    return triggerProfileMock;
            }
            return null;
        }).when(profileFactoryMock).createProfile(isA(ProfileTypeUID.class), isA(ProfileCallback.class),
                isA(ProfileContext.class));

        when(profileFactoryMock.getSupportedProfileTypeUIDs())
                .thenReturn(List.of(new ProfileTypeUID("test:state"), new ProfileTypeUID("test:trigger")));

        manager.addProfileFactory(profileFactoryMock);
        manager.addProfileAdvisor(profileAdvisorMock);

        iclRegistry.addProvider(new ItemChannelLinkProvider() {
            @Override
            public void addProviderChangeListener(ProviderChangeListener<ItemChannelLink> listener) {
            }

            @Override
            public void removeProviderChangeListener(ProviderChangeListener<ItemChannelLink> listener) {
            }

            @Override
            public Collection<ItemChannelLink> getAll() {
                return List.of(LINK_1_S1, LINK_1_S2, LINK_2_S2, LINK_1_T1, LINK_1_T2, LINK_2_T2, LINK_3_S3, LINK_4_S4,
                        LINK_5_S5);
            }
        });

        when(itemRegistryMock.get(eq(ITEM_NAME_1))).thenReturn(ITEM_1);
        when(itemRegistryMock.get(eq(ITEM_NAME_2))).thenReturn(ITEM_2);
        when(itemRegistryMock.get(eq(ITEM_NAME_3))).thenReturn(ITEM_3);
        when(itemRegistryMock.get(eq(ITEM_NAME_4))).thenReturn(ITEM_4);
        when(itemRegistryMock.get(eq(ITEM_NAME_5))).thenReturn(item5);

        ChannelType channelType4 = mock(ChannelType.class);
        when(channelType4.getItemType()).thenReturn("Number:Temperature");

        THING.setHandler(thingHandlerMock);

        when(thingRegistryMock.get(eq(THING_UID))).thenReturn(THING);

        manager.addItemFactory(new CoreItemFactory(UNIT_PROVIDER_MOCK));
    }

    @Test
    public void testStateUpdatedSingleLink() {
        manager.stateUpdated(STATE_CHANNEL_UID_1, OnOffType.ON);
        waitForAssert(() -> {
            verify(stateProfileMock).onStateUpdateFromHandler(eq(OnOffType.ON));
        });
        verifyNoMoreInteractions(stateProfileMock);
        verifyNoMoreInteractions(triggerProfileMock);
    }

    @Test
    public void testStateUpdatedMultiLink() {
        manager.stateUpdated(STATE_CHANNEL_UID_2, OnOffType.ON);
        waitForAssert(() -> {
            verify(stateProfileMock, times(2)).onStateUpdateFromHandler(eq(OnOffType.ON));
        });
        verifyNoMoreInteractions(stateProfileMock);
        verifyNoMoreInteractions(triggerProfileMock);
    }

    @Test
    public void testPostCommandSingleLink() {
        manager.postCommand(STATE_CHANNEL_UID_1, OnOffType.ON);
        waitForAssert(() -> {
            verify(stateProfileMock).onCommandFromHandler(eq(OnOffType.ON));
        });
        verifyNoMoreInteractions(stateProfileMock);
        verifyNoMoreInteractions(triggerProfileMock);
    }

    @Test
    public void testPostCommandMultiLink() {
        manager.postCommand(STATE_CHANNEL_UID_2, OnOffType.ON);
        waitForAssert(() -> {
            verify(stateProfileMock, times(2)).onCommandFromHandler(eq(OnOffType.ON));
        });
        verifyNoMoreInteractions(stateProfileMock);
        verifyNoMoreInteractions(triggerProfileMock);
    }

    @Test
    public void testItemCommandEventSingleLink() {
        manager.receive(ItemEventFactory.createCommandEvent(ITEM_NAME_2, OnOffType.ON));
        waitForAssert(() -> {
            verify(stateProfileMock).onCommandFromItem(eq(OnOffType.ON));
        });
        verifyNoMoreInteractions(stateProfileMock);
        verifyNoMoreInteractions(triggerProfileMock);
        verify(autoUpdateManagerMock).receiveCommand(isA(ItemCommandEvent.class), isA(Item.class));
    }

    @Test
    public void testItemCommandEventDecimal2Quantity() {
        // Take unit from accepted item type (see channel built from STATE_CHANNEL_UID_3)
        manager.receive(ItemEventFactory.createCommandEvent(ITEM_NAME_5, DecimalType.valueOf("20")));
        waitForAssert(() -> {
            verify(stateProfileMock).onCommandFromItem(eq(QuantityType.valueOf("20 °C")));
        });
        verifyNoMoreInteractions(stateProfileMock);
        verifyNoMoreInteractions(triggerProfileMock);
    }

    @Test
    public void testItemCommandEventDecimal2Quantity2() {
        MetadataKey key = new MetadataKey(NumberItem.UNIT_METADATA_NAMESPACE, ITEM_NAME_5);
        Metadata metadata = new Metadata(key, ImperialUnits.FAHRENHEIT.toString(), null);
        item5.addedMetadata(metadata);

        manager.receive(ItemEventFactory.createCommandEvent(ITEM_NAME_5, DecimalType.valueOf("20")));
        waitForAssert(() -> {
            verify(stateProfileMock).onCommandFromItem(eq(QuantityType.valueOf("20 °F")));
        });
        verifyNoMoreInteractions(stateProfileMock);
        verifyNoMoreInteractions(triggerProfileMock);
    }

    @Test
    public void testItemCommandEventMultiLink() {
        manager.receive(ItemEventFactory.createCommandEvent(ITEM_NAME_1, OnOffType.ON));
        waitForAssert(() -> {
            verify(stateProfileMock, times(2)).onCommandFromItem(eq(OnOffType.ON));
        });
        verifyNoMoreInteractions(stateProfileMock);
        verifyNoMoreInteractions(triggerProfileMock);
        verify(autoUpdateManagerMock).receiveCommand(isA(ItemCommandEvent.class), isA(Item.class));
    }

    @Test
    public void testItemCommandEventNotToSource() {
        manager.receive(
                ItemEventFactory.createCommandEvent(ITEM_NAME_1, OnOffType.ON, STATE_CHANNEL_UID_2.getAsString()));
        waitForAssert(() -> {
            verify(stateProfileMock).onCommandFromItem(eq(OnOffType.ON));
        });
        verifyNoMoreInteractions(stateProfileMock);
        verifyNoMoreInteractions(triggerProfileMock);
        verify(autoUpdateManagerMock).receiveCommand(isA(ItemCommandEvent.class), isA(Item.class));
    }

    @Test
    public void testItemStateEventSingleLink() {
        manager.receive(ItemEventFactory.createStateUpdatedEvent(ITEM_NAME_2, OnOffType.ON));
        waitForAssert(() -> {
            verify(stateProfileMock).onStateUpdateFromItem(eq(OnOffType.ON));
            verify(triggerProfileMock).onStateUpdateFromItem(eq(OnOffType.ON));
        });
        verifyNoMoreInteractions(stateProfileMock);
        verifyNoMoreInteractions(triggerProfileMock);
    }

    @Test
    public void testItemStateEventMultiLink() {
        manager.receive(ItemEventFactory.createStateUpdatedEvent(ITEM_NAME_1, OnOffType.ON));
        waitForAssert(() -> {
            verify(stateProfileMock, times(2)).onStateUpdateFromItem(eq(OnOffType.ON));
            verify(triggerProfileMock, times(2)).onStateUpdateFromItem(eq(OnOffType.ON));
        });
        verifyNoMoreInteractions(stateProfileMock);
        verifyNoMoreInteractions(triggerProfileMock);
    }

    @Test
    public void testItemStateEventNotToSource() {
        manager.receive(
                ItemEventFactory.createStateUpdatedEvent(ITEM_NAME_1, OnOffType.ON, STATE_CHANNEL_UID_2.getAsString()));
        waitForAssert(() -> {
            verify(stateProfileMock).onStateUpdateFromItem(eq(OnOffType.ON));
            verify(triggerProfileMock, times(2)).onStateUpdateFromItem(eq(OnOffType.ON));
        });
        verifyNoMoreInteractions(stateProfileMock);
        verifyNoMoreInteractions(triggerProfileMock);
    }

    @Test
    public void testChannelTriggeredEventSingleLink() {
        manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_1));
        waitForAssert(() -> {
            verify(triggerProfileMock).onTriggerFromHandler(eq(EVENT));
        });
        verifyNoMoreInteractions(stateProfileMock);
        verifyNoMoreInteractions(triggerProfileMock);
    }

    @Test
    public void testChannelTriggeredEventMultiLink() {
        manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_2));
        waitForAssert(() -> {
            verify(triggerProfileMock, times(2)).onTriggerFromHandler(eq(EVENT));
        });
        verifyNoMoreInteractions(stateProfileMock);
        verifyNoMoreInteractions(triggerProfileMock);
    }

    @Test
    public void testProfileIsReused() {
        for (int i = 0; i < 3; i++) {
            manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_2));
        }

        waitForAssert(() -> {
            verify(profileFactoryMock, times(2)).createProfile(isA(ProfileTypeUID.class), isA(ProfileCallback.class),
                    isA(ProfileContext.class));
            verify(profileFactoryMock, atLeast(0)).getSupportedProfileTypeUIDs();
            verify(profileAdvisorMock, atLeast(0)).getSuggestedProfileTypeUID(any(Channel.class), any());
        });
        verifyNoMoreInteractions(profileFactoryMock);
        verifyNoMoreInteractions(profileAdvisorMock);
    }

    @Test
    public void testProfileIsNotReusedOnFactoryChange() {
        for (int i = 0; i < 3; i++) {
            manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_2));
        }
        verify(profileFactoryMock, times(2)).createProfile(isA(ProfileTypeUID.class), isA(ProfileCallback.class),
                isA(ProfileContext.class));

        manager.removeProfileFactory(profileFactoryMock);
        manager.addProfileFactory(profileFactoryMock);

        for (int i = 0; i < 3; i++) {
            manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_2));
        }

        waitForAssert(() -> {
            verify(profileFactoryMock, times(4)).createProfile(isA(ProfileTypeUID.class), isA(ProfileCallback.class),
                    isA(ProfileContext.class));
            verify(profileFactoryMock, atLeast(0)).getSupportedProfileTypeUIDs();
            verify(profileAdvisorMock, atLeast(0)).getSuggestedProfileTypeUID(any(Channel.class), any());
        });
        verifyNoMoreInteractions(profileFactoryMock);
        verifyNoMoreInteractions(profileAdvisorMock);
    }

    @Test
    public void testProfileIsNotReusedOnLinkChange() {
        for (int i = 0; i < 3; i++) {
            manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_2));
        }
        waitForAssert(() -> {
            verify(profileFactoryMock, times(2)).createProfile(isA(ProfileTypeUID.class), isA(ProfileCallback.class),
                    isA(ProfileContext.class));
        });

        manager.removed(LINK_2_T2);
        manager.added(LINK_2_T2);

        for (int i = 0; i < 3; i++) {
            manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_2));
        }

        waitForAssert(() -> {
            verify(profileFactoryMock, times(3)).createProfile(isA(ProfileTypeUID.class), isA(ProfileCallback.class),
                    isA(ProfileContext.class));
            verify(profileFactoryMock, atLeast(0)).getSupportedProfileTypeUIDs();
            verify(profileAdvisorMock, atLeast(0)).getSuggestedProfileTypeUID(any(Channel.class), any());
        });
        verifyNoMoreInteractions(profileFactoryMock);
        verifyNoMoreInteractions(profileAdvisorMock);
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
            verify(profileFactoryMock, times(2)).createProfile(isA(ProfileTypeUID.class), isA(ProfileCallback.class),
                    isA(ProfileContext.class));
            verify(profileFactoryMock, atLeast(0)).getSupportedProfileTypeUIDs();
            verify(profileAdvisorMock, atLeast(0)).getSuggestedProfileTypeUID(any(Channel.class), any());
        });
        verifyNoMoreInteractions(profileFactoryMock);
        verifyNoMoreInteractions(profileAdvisorMock);
    }

    @Test
    public void testProfileIsNotReusedOnLinkUpdate() {
        for (int i = 0; i < 3; i++) {
            manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_2));
        }
        verify(profileFactoryMock, times(2)).createProfile(isA(ProfileTypeUID.class), isA(ProfileCallback.class),
                isA(ProfileContext.class));

        manager.updated(LINK_2_T2, LINK_2_T2);

        for (int i = 0; i < 3; i++) {
            manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_2));
        }

        waitForAssert(() -> {
            verify(profileFactoryMock, times(3)).createProfile(isA(ProfileTypeUID.class), isA(ProfileCallback.class),
                    isA(ProfileContext.class));
            verify(profileFactoryMock, atLeast(0)).getSupportedProfileTypeUIDs();
            verify(profileAdvisorMock, atLeast(0)).getSuggestedProfileTypeUID(any(Channel.class), any());
        });
        verifyNoMoreInteractions(profileFactoryMock);
        verifyNoMoreInteractions(profileAdvisorMock);
    }

    @Test
    public void testProfileIsNotReusedOnItemChange() {
        for (int i = 0; i < 3; i++) {
            manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_2));
        }
        verify(profileFactoryMock, times(2)).createProfile(isA(ProfileTypeUID.class), isA(ProfileCallback.class),
                isA(ProfileContext.class));

        manager.receive(ItemEventFactory.createUpdateEvent(ITEM_2, ITEM_2));

        for (int i = 0; i < 3; i++) {
            manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_2));
        }

        waitForAssert(() -> {
            verify(profileFactoryMock, times(3)).createProfile(isA(ProfileTypeUID.class), isA(ProfileCallback.class),
                    isA(ProfileContext.class));
            verify(profileFactoryMock, atLeast(0)).getSupportedProfileTypeUIDs();
            verify(profileAdvisorMock, atLeast(0)).getSuggestedProfileTypeUID(any(Channel.class), any());
        });
        verifyNoMoreInteractions(profileFactoryMock);
        verifyNoMoreInteractions(profileAdvisorMock);
    }

    @Test
    public void testProfileIsNotReusedOnThingChange() {
        for (int i = 0; i < 3; i++) {
            manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_2));
        }
        verify(profileFactoryMock, times(2)).createProfile(isA(ProfileTypeUID.class), isA(ProfileCallback.class),
                isA(ProfileContext.class));

        manager.receive(ThingEventFactory.createUpdateEvent(THING, THING));

        for (int i = 0; i < 3; i++) {
            manager.receive(ThingEventFactory.createTriggerEvent(EVENT, TRIGGER_CHANNEL_UID_2));
        }

        waitForAssert(() -> {
            verify(profileFactoryMock, times(4)).createProfile(isA(ProfileTypeUID.class), isA(ProfileCallback.class),
                    isA(ProfileContext.class));
            verify(profileFactoryMock, atLeast(0)).getSupportedProfileTypeUIDs();
            verify(profileAdvisorMock, atLeast(0)).getSuggestedProfileTypeUID(any(Channel.class), any());
        });
        verifyNoMoreInteractions(profileFactoryMock);
        verifyNoMoreInteractions(profileAdvisorMock);
    }

    @Test
    public void testItemCommandTypeDowncast() {
        Thing thing = ThingBuilder
                .create(THING_TYPE_UID, THING_UID).withChannels(ChannelBuilder
                        .create(STATE_CHANNEL_UID_2, CoreItemFactory.DIMMER).withKind(ChannelKind.STATE).build())
                .build();
        thing.setHandler(thingHandlerMock);
        when(thingRegistryMock.get(eq(THING_UID))).thenReturn(thing);
        Command command = manager.toAcceptedCommand(HSBType.fromRGB(128, 128, 128),
                thing.getChannel(STATE_CHANNEL_UID_2), ITEM_2);
        assertEquals(PercentType.class, command.getClass());
    }
}
