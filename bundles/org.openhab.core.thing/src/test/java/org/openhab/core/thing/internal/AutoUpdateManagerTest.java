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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.items.events.ItemCommandEvent;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.items.events.ItemStateEvent;
import org.openhab.core.items.events.ItemStatePredictedEvent;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.type.AutoUpdatePolicy;
import org.openhab.core.thing.type.ChannelTypeRegistry;

/**
 *
 * @author Simon Kaufmann - Initial contribution
 * @author Kai Kreuzer - added tests with multiple links
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class AutoUpdateManagerTest {

    private static final String ITEM_NAME = "test";
    private static final ThingUID THING_UID_ONLINE = new ThingUID("test::mock-online");
    private static final ThingUID THING_UID_OFFLINE = new ThingUID("test::mock-offline");
    private static final ThingUID THING_UID_HANDLER_MISSING = new ThingUID("test::handlerMissing");
    private static final ChannelUID CHANNEL_UID_ONLINE_1 = new ChannelUID(THING_UID_ONLINE, "channel1");
    private static final ChannelUID CHANNEL_UID_ONLINE_2 = new ChannelUID(THING_UID_ONLINE, "channel2");
    private static final ChannelUID CHANNEL_UID_OFFLINE_1 = new ChannelUID(THING_UID_OFFLINE, "channel1");
    private static final ChannelUID CHANNEL_UID_ONLINE_GONE = new ChannelUID(THING_UID_ONLINE, "gone");
    private static final ChannelUID CHANNEL_UID_HANDLER_MISSING = new ChannelUID(THING_UID_HANDLER_MISSING, "channel1");
    private @NonNullByDefault({}) ItemCommandEvent event;
    private @NonNullByDefault({}) GenericItem item;
    private @NonNullByDefault({}) ItemCommandEvent groupEvent;
    private @NonNullByDefault({}) GroupItem groupItem;

    private @Mock @NonNullByDefault({}) ChannelTypeRegistry channelTypeRegistryMock;
    private @Mock @NonNullByDefault({}) EventPublisher eventPublisherMock;
    private @Mock @NonNullByDefault({}) ItemChannelLinkRegistry iclRegistryMock;
    private @Mock @NonNullByDefault({}) ThingRegistry thingRegistryMock;
    private @Mock @NonNullByDefault({}) Thing onlineThingMock;
    private @Mock @NonNullByDefault({}) Thing offlineThingMock;
    private @Mock @NonNullByDefault({}) Thing thingMissingHandlerMock;
    private @Mock @NonNullByDefault({}) ThingHandler handlerMock;
    private @Mock @NonNullByDefault({}) MetadataRegistry metadataRegistryMock;

    private final Set<ItemChannelLink> links = new HashSet<>();
    private @NonNullByDefault({}) AutoUpdateManager aum;
    private final Map<ChannelUID, AutoUpdatePolicy> policies = new HashMap<>();

    @BeforeEach
    public void setup() {
        event = ItemEventFactory.createCommandEvent(ITEM_NAME, new StringType("AFTER"));
        item = new StringItem(ITEM_NAME);
        item.setState(new StringType("BEFORE"));
        groupEvent = ItemEventFactory.createCommandEvent("groupTest", new StringType("AFTER"));
        groupItem = new GroupItem("groupTest", new StringItem("test"));
        groupItem.setState(new StringType("BEFORE"));

        when(iclRegistryMock.getLinks(eq(ITEM_NAME))).then(answer -> links);

        when(thingRegistryMock.get(eq(THING_UID_ONLINE))).thenReturn(onlineThingMock);
        when(thingRegistryMock.get(eq(THING_UID_OFFLINE))).thenReturn(offlineThingMock);
        when(thingRegistryMock.get(eq(THING_UID_HANDLER_MISSING))).thenReturn(thingMissingHandlerMock);

        when(onlineThingMock.getHandler()).thenReturn(handlerMock);
        when(onlineThingMock.getStatus()).thenReturn(ThingStatus.ONLINE);
        when(onlineThingMock.getChannel(eq(CHANNEL_UID_ONLINE_1.getId())))
                .thenAnswer(answer -> ChannelBuilder.create(CHANNEL_UID_ONLINE_1, CoreItemFactory.STRING)
                        .withAutoUpdatePolicy(policies.get(CHANNEL_UID_ONLINE_1)).build());
        when(onlineThingMock.getChannel(eq(CHANNEL_UID_ONLINE_2.getId())))
                .thenAnswer(answer -> ChannelBuilder.create(CHANNEL_UID_ONLINE_2, CoreItemFactory.STRING)
                        .withAutoUpdatePolicy(policies.get(CHANNEL_UID_ONLINE_2)).build());

        when(offlineThingMock.getHandler()).thenReturn(handlerMock);
        when(offlineThingMock.getStatus()).thenReturn(ThingStatus.OFFLINE);
        when(offlineThingMock.getChannel(eq(CHANNEL_UID_OFFLINE_1.getId())))
                .thenAnswer(answer -> ChannelBuilder.create(CHANNEL_UID_OFFLINE_1, CoreItemFactory.STRING)
                        .withAutoUpdatePolicy(policies.get(CHANNEL_UID_OFFLINE_1)).build());

        aum = new AutoUpdateManager(new HashMap<>(), channelTypeRegistryMock, eventPublisherMock, iclRegistryMock,
                metadataRegistryMock, thingRegistryMock);
    }

    private void assertStateEvent(String expectedContent, String extectedSource) {
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventPublisherMock, atLeastOnce()).post(eventCaptor.capture());
        Event event = eventCaptor.getAllValues().stream().filter(e -> e instanceof ItemStateEvent).findFirst().get();
        assertEquals(expectedContent, ((ItemStateEvent) event).getItemState().toFullString());
        assertEquals(extectedSource, event.getSource());
        assertNothingHappened();
    }

    private void assertPredictionEvent(String expectedContent, @Nullable String expectedSource) {
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventPublisherMock, atLeastOnce()).post(eventCaptor.capture());
        Event event = eventCaptor.getAllValues().stream().filter(e -> e instanceof ItemStatePredictedEvent).findFirst()
                .get();
        assertEquals(expectedContent, ((ItemStatePredictedEvent) event).getPredictedState().toFullString());
        assertEquals(expectedSource, event.getSource());
        assertNothingHappened();
    }

    private void assertChangeStateTo() {
        assertPredictionEvent("AFTER", null);
        assertNothingHappened();
    }

    private void assertKeepCurrentState() {
        assertPredictionEvent("BEFORE", null);
        assertNothingHappened();
    }

    private void assertNothingHappened() {
        verifyNoMoreInteractions(eventPublisherMock);
    }

    private void setAutoUpdatePolicy(ChannelUID channelUID, AutoUpdatePolicy policy) {
        policies.put(channelUID, policy);
    }

    @Test
    public void testAutoUpdateNoLink() {
        aum.receiveCommand(event, item);

        assertStateEvent("AFTER", AutoUpdateManager.EVENT_SOURCE);
    }

    @Test
    public void testAutoUpdateNoPolicy() {
        links.add(new ItemChannelLink(ITEM_NAME, CHANNEL_UID_ONLINE_1));

        aum.receiveCommand(event, item);

        assertChangeStateTo();
    }

    @Test
    public void testAutoUpdateNoPolicyThingOFFLINE() {
        links.add(new ItemChannelLink(ITEM_NAME, CHANNEL_UID_OFFLINE_1));

        aum.receiveCommand(event, item);

        assertKeepCurrentState();
    }

    @Test
    public void testAutoUpdateNoPolicyThingOFFLINEandThingONLINE() {
        links.add(new ItemChannelLink(ITEM_NAME, CHANNEL_UID_OFFLINE_1));
        links.add(new ItemChannelLink(ITEM_NAME, CHANNEL_UID_ONLINE_1));

        aum.receiveCommand(event, item);

        assertChangeStateTo();
    }

    @Test
    public void testAutoUpdateNoPolicyThingONLINEandThingOFFLINE() {
        links.add(new ItemChannelLink(ITEM_NAME, CHANNEL_UID_ONLINE_1));
        links.add(new ItemChannelLink(ITEM_NAME, CHANNEL_UID_OFFLINE_1));

        aum.receiveCommand(event, item);

        assertChangeStateTo();
    }

    @Test
    public void testAutoUpdateNoPolicyNoHandler() {
        links.add(new ItemChannelLink(ITEM_NAME, CHANNEL_UID_HANDLER_MISSING));

        aum.receiveCommand(event, item);

        assertKeepCurrentState();
    }

    @Test
    public void testAutoUpdateNoPolicyNoThing() {
        links.add(new ItemChannelLink(ITEM_NAME, new ChannelUID(new ThingUID("test::missing"), "gone")));

        aum.receiveCommand(event, item);

        assertKeepCurrentState();
    }

    @Test
    public void testAutoUpdateNoPolicyNoChannel() {
        links.add(new ItemChannelLink(ITEM_NAME, CHANNEL_UID_ONLINE_GONE));

        aum.receiveCommand(event, item);

        assertKeepCurrentState();
    }

    @Test
    public void testAutoUpdatePolicyVETOThingONLINE() {
        links.add(new ItemChannelLink(ITEM_NAME, CHANNEL_UID_ONLINE_1));
        setAutoUpdatePolicy(CHANNEL_UID_ONLINE_1, AutoUpdatePolicy.VETO);

        aum.receiveCommand(event, item);

        assertNothingHappened();
    }

    @Test
    public void testAutoUpdatePolicyRECOMMEND() {
        links.add(new ItemChannelLink(ITEM_NAME, CHANNEL_UID_ONLINE_1));
        setAutoUpdatePolicy(CHANNEL_UID_ONLINE_1, AutoUpdatePolicy.RECOMMEND);

        aum.receiveCommand(event, item);

        assertStateEvent("AFTER", AutoUpdateManager.EVENT_SOURCE);
    }

    @Test
    public void testAutoUpdatePolicyVETObeatsDEFAULT() {
        links.add(new ItemChannelLink(ITEM_NAME, CHANNEL_UID_ONLINE_1));
        links.add(new ItemChannelLink(ITEM_NAME, CHANNEL_UID_ONLINE_2));
        setAutoUpdatePolicy(CHANNEL_UID_ONLINE_1, AutoUpdatePolicy.VETO);
        setAutoUpdatePolicy(CHANNEL_UID_ONLINE_2, AutoUpdatePolicy.DEFAULT);

        aum.receiveCommand(event, item);

        assertNothingHappened();
    }

    @Test
    public void testAutoUpdatePolicyVETObeatsRECOMMEND() {
        links.add(new ItemChannelLink(ITEM_NAME, CHANNEL_UID_ONLINE_1));
        links.add(new ItemChannelLink(ITEM_NAME, CHANNEL_UID_ONLINE_2));
        setAutoUpdatePolicy(CHANNEL_UID_ONLINE_1, AutoUpdatePolicy.VETO);
        setAutoUpdatePolicy(CHANNEL_UID_ONLINE_2, AutoUpdatePolicy.RECOMMEND);

        aum.receiveCommand(event, item);

        assertNothingHappened();
    }

    @Test
    public void testAutoUpdatePolicyDEFAULTbeatsRECOMMEND() {
        links.add(new ItemChannelLink(ITEM_NAME, CHANNEL_UID_ONLINE_1));
        links.add(new ItemChannelLink(ITEM_NAME, CHANNEL_UID_ONLINE_2));
        setAutoUpdatePolicy(CHANNEL_UID_ONLINE_1, AutoUpdatePolicy.DEFAULT);
        setAutoUpdatePolicy(CHANNEL_UID_ONLINE_2, AutoUpdatePolicy.RECOMMEND);

        aum.receiveCommand(event, item);

        assertChangeStateTo();
    }

    @Test
    public void testAutoUpdateErrorInvalidatesVETO() {
        links.add(new ItemChannelLink(ITEM_NAME, CHANNEL_UID_ONLINE_1));
        links.add(new ItemChannelLink(ITEM_NAME, CHANNEL_UID_ONLINE_GONE));
        setAutoUpdatePolicy(CHANNEL_UID_ONLINE_1, AutoUpdatePolicy.RECOMMEND);
        setAutoUpdatePolicy(CHANNEL_UID_ONLINE_GONE, AutoUpdatePolicy.VETO);

        aum.receiveCommand(event, item);

        assertStateEvent("AFTER", AutoUpdateManager.EVENT_SOURCE);
    }

    @Test
    public void testAutoUpdateErrorInvalidatesVETO2() {
        links.add(new ItemChannelLink(ITEM_NAME, CHANNEL_UID_ONLINE_1));
        links.add(new ItemChannelLink(ITEM_NAME, CHANNEL_UID_ONLINE_GONE));
        setAutoUpdatePolicy(CHANNEL_UID_ONLINE_1, AutoUpdatePolicy.DEFAULT);
        setAutoUpdatePolicy(CHANNEL_UID_ONLINE_GONE, AutoUpdatePolicy.VETO);

        aum.receiveCommand(event, item);

        assertChangeStateTo();
    }

    @Test
    public void testAutoUpdateErrorInvalidatesDEFAULT() {
        links.add(new ItemChannelLink(ITEM_NAME, CHANNEL_UID_ONLINE_1));
        links.add(new ItemChannelLink(ITEM_NAME, CHANNEL_UID_ONLINE_GONE));
        setAutoUpdatePolicy(CHANNEL_UID_ONLINE_1, AutoUpdatePolicy.RECOMMEND);
        setAutoUpdatePolicy(CHANNEL_UID_ONLINE_GONE, AutoUpdatePolicy.DEFAULT);

        aum.receiveCommand(event, item);

        assertStateEvent("AFTER", AutoUpdateManager.EVENT_SOURCE);
    }

    @Test
    public void testAutoUpdateMultipleErrors() {
        links.add(new ItemChannelLink(ITEM_NAME, CHANNEL_UID_ONLINE_GONE));
        links.add(new ItemChannelLink(ITEM_NAME, CHANNEL_UID_ONLINE_GONE));
        setAutoUpdatePolicy(CHANNEL_UID_ONLINE_GONE, AutoUpdatePolicy.DEFAULT);

        aum.receiveCommand(event, item);

        assertKeepCurrentState();
    }

    @Test
    public void testAutoUpdateDisabled() {
        aum.modified(Map.of(AutoUpdateManager.PROPERTY_ENABLED, "false"));

        aum.receiveCommand(event, item);

        assertNothingHappened();
    }

    @Test
    public void testAutoUpdateSendOptimisticUpdates() {
        links.add(new ItemChannelLink(ITEM_NAME, CHANNEL_UID_ONLINE_1));
        aum.modified(Map.of(AutoUpdateManager.PROPERTY_SEND_OPTIMISTIC_UPDATES, "true"));

        aum.receiveCommand(event, item);

        assertPredictionEvent("AFTER", null);
        assertStateEvent("AFTER", AutoUpdateManager.EVENT_SOURCE_OPTIMISTIC); // no?
    }

    @Test
    public void testAutoUpdateDisabledForGroupItems() {
        groupItem.addMember(item);
        aum.receiveCommand(groupEvent, groupItem);
        groupItem.removeMember(item);

        assertNothingHappened();
    }
}
