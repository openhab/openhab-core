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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.smarthome.core.events.Event;
import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.items.MetadataRegistry;
import org.eclipse.smarthome.core.items.events.ItemCommandEvent;
import org.eclipse.smarthome.core.items.events.ItemEventFactory;
import org.eclipse.smarthome.core.items.events.ItemStateEvent;
import org.eclipse.smarthome.core.items.events.ItemStatePredictedEvent;
import org.eclipse.smarthome.core.library.items.StringItem;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.link.ItemChannelLink;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
import org.eclipse.smarthome.core.thing.type.AutoUpdatePolicy;
import org.eclipse.smarthome.core.thing.type.ChannelTypeRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

/**
 *
 * @author Simon Kaufmann - initial contribution and API
 * @author Kai Kreuzer - added tests with multiple links
 *
 */
public class AutoUpdateManagerTest {

    private static final ThingUID THING_UID_ONLINE = new ThingUID("test::mock-online");
    private static final ThingUID THING_UID_OFFLINE = new ThingUID("test::mock-offline");
    private static final ThingUID THING_UID_HANDLER_MISSING = new ThingUID("test::handlerMissing");
    private static final ChannelUID CHANNEL_UID_ONLINE_1 = new ChannelUID(THING_UID_ONLINE, "channel1");
    private static final ChannelUID CHANNEL_UID_ONLINE_2 = new ChannelUID(THING_UID_ONLINE, "channel2");
    private static final ChannelUID CHANNEL_UID_OFFLINE_1 = new ChannelUID(THING_UID_OFFLINE, "channel1");
    private static final ChannelUID CHANNEL_UID_ONLINE_GONE = new ChannelUID(THING_UID_ONLINE, "gone");
    private static final ChannelUID CHANNEL_UID_HANDLER_MISSING = new ChannelUID(THING_UID_HANDLER_MISSING, "channel1");
    private ItemCommandEvent event;
    private GenericItem item;
    private @Mock EventPublisher mockEventPublisher;
    private @Mock ItemChannelLinkRegistry mockLinkRegistry;
    private @Mock ThingRegistry mockThingRegistry;
    private @Mock Thing mockThingOnline;
    private @Mock Thing mockThingOffline;
    private @Mock Thing mockThingHandlerMissing;
    private @Mock ThingHandler mockHandler;
    private @Mock MetadataRegistry mockMetadataRegistry;

    private final List<ItemChannelLink> links = new LinkedList<>();
    private AutoUpdateManager aum;
    private final Map<ChannelUID, AutoUpdatePolicy> policies = new HashMap<>();

    @Before
    public void setup() {
        initMocks(this);
        event = ItemEventFactory.createCommandEvent("test", new StringType("AFTER"));
        item = new StringItem("test");
        item.setState(new StringType("BEFORE"));

        when(mockLinkRegistry.stream()).then(answer -> links.stream());
        when(mockLinkRegistry.getAll()).then(answer -> links);
        when(mockThingRegistry.get(eq(THING_UID_ONLINE))).thenReturn(mockThingOnline);
        when(mockThingRegistry.get(eq(THING_UID_OFFLINE))).thenReturn(mockThingOffline);
        when(mockThingRegistry.get(eq(THING_UID_HANDLER_MISSING))).thenReturn(mockThingHandlerMissing);
        when(mockThingOnline.getHandler()).thenReturn(mockHandler);
        when(mockThingOnline.getStatus()).thenReturn(ThingStatus.ONLINE);
        when(mockThingOnline.getChannel(eq(CHANNEL_UID_ONLINE_1.getId())))
                .thenAnswer(answer -> ChannelBuilder.create(CHANNEL_UID_ONLINE_1, "String")
                        .withAutoUpdatePolicy(policies.get(CHANNEL_UID_ONLINE_1)).build());
        when(mockThingOnline.getChannel(eq(CHANNEL_UID_ONLINE_2.getId())))
                .thenAnswer(answer -> ChannelBuilder.create(CHANNEL_UID_ONLINE_2, "String")
                        .withAutoUpdatePolicy(policies.get(CHANNEL_UID_ONLINE_2)).build());
        when(mockThingOffline.getHandler()).thenReturn(mockHandler);
        when(mockThingOffline.getStatus()).thenReturn(ThingStatus.OFFLINE);
        when(mockThingOffline.getChannel(eq(CHANNEL_UID_OFFLINE_1.getId())))
                .thenAnswer(answer -> ChannelBuilder.create(CHANNEL_UID_OFFLINE_1, "String")
                        .withAutoUpdatePolicy(policies.get(CHANNEL_UID_OFFLINE_1)).build());

        aum = new AutoUpdateManager();
        aum.setItemChannelLinkRegistry(mockLinkRegistry);
        aum.setEventPublisher(mockEventPublisher);
        aum.setThingRegistry(mockThingRegistry);
        aum.setMetadataRegistry(mockMetadataRegistry);
        aum.setChannelTypeRegistry(mock(ChannelTypeRegistry.class));
    }

    private void assertStateEvent(String expectedContent, String extectedSource) {
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(mockEventPublisher, atLeastOnce()).post(eventCaptor.capture());
        Event event = eventCaptor.getAllValues().stream().filter(e -> e instanceof ItemStateEvent).findFirst().get();
        assertEquals(expectedContent, ((ItemStateEvent) event).getItemState().toFullString());
        assertEquals(extectedSource, event.getSource());
        assertNothingHappened();
    }

    private void assertPredictionEvent(String expectedContent, String extectedSource) {
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(mockEventPublisher, atLeastOnce()).post(eventCaptor.capture());
        Event event = eventCaptor.getAllValues().stream().filter(e -> e instanceof ItemStatePredictedEvent).findFirst()
                .get();
        assertEquals(expectedContent, ((ItemStatePredictedEvent) event).getPredictedState().toFullString());
        assertEquals(extectedSource, event.getSource());
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
        verifyNoMoreInteractions(mockEventPublisher);
    }

    private void setAutoUpdatePolicy(ChannelUID channelUID, AutoUpdatePolicy policy) {
        policies.put(channelUID, policy);
    }

    @Test
    public void testAutoUpdate_noLink() {
        aum.receiveCommand(event, item);

        assertStateEvent("AFTER", AutoUpdateManager.EVENT_SOURCE);
    }

    @Test
    public void testAutoUpdate_noPolicy() {
        links.add(new ItemChannelLink("test", CHANNEL_UID_ONLINE_1));

        aum.receiveCommand(event, item);

        assertChangeStateTo();
    }

    @Test
    public void testAutoUpdate_noPolicy_thingOFFLINE() {
        links.add(new ItemChannelLink("test", CHANNEL_UID_OFFLINE_1));

        aum.receiveCommand(event, item);

        assertKeepCurrentState();
    }

    @Test
    public void testAutoUpdate_noPolicy_thingOFFLINE_and_thingONLINE() {
        links.add(new ItemChannelLink("test", CHANNEL_UID_OFFLINE_1));
        links.add(new ItemChannelLink("test", CHANNEL_UID_ONLINE_1));

        aum.receiveCommand(event, item);

        assertChangeStateTo();
    }

    @Test
    public void testAutoUpdate_noPolicy_thingONLINE_and_thingOFFLINE() {
        links.add(new ItemChannelLink("test", CHANNEL_UID_ONLINE_1));
        links.add(new ItemChannelLink("test", CHANNEL_UID_OFFLINE_1));

        aum.receiveCommand(event, item);

        assertChangeStateTo();
    }

    @Test
    public void testAutoUpdate_noPolicy_noHandler() {
        links.add(new ItemChannelLink("test", CHANNEL_UID_HANDLER_MISSING));

        aum.receiveCommand(event, item);

        assertKeepCurrentState();
    }

    @Test
    public void testAutoUpdate_noPolicy_noThing() {
        links.add(new ItemChannelLink("test", new ChannelUID(new ThingUID("test::missing"), "gone")));

        aum.receiveCommand(event, item);

        assertKeepCurrentState();
    }

    @Test
    public void testAutoUpdate_noPolicy_noChannel() {
        links.add(new ItemChannelLink("test", CHANNEL_UID_ONLINE_GONE));

        aum.receiveCommand(event, item);

        assertKeepCurrentState();
    }

    @Test
    public void testAutoUpdate_policyVETO_thingONLINE() {
        links.add(new ItemChannelLink("test", CHANNEL_UID_ONLINE_1));
        setAutoUpdatePolicy(CHANNEL_UID_ONLINE_1, AutoUpdatePolicy.VETO);

        aum.receiveCommand(event, item);

        assertNothingHappened();
    }

    @Test
    public void testAutoUpdate_policyRECOMMEND() {
        links.add(new ItemChannelLink("test", CHANNEL_UID_ONLINE_1));
        setAutoUpdatePolicy(CHANNEL_UID_ONLINE_1, AutoUpdatePolicy.RECOMMEND);

        aum.receiveCommand(event, item);

        assertStateEvent("AFTER", AutoUpdateManager.EVENT_SOURCE);
    }

    @Test
    public void testAutoUpdate_policyVETObeatsDEFAULT() {
        links.add(new ItemChannelLink("test", CHANNEL_UID_ONLINE_1));
        links.add(new ItemChannelLink("test", CHANNEL_UID_ONLINE_2));
        setAutoUpdatePolicy(CHANNEL_UID_ONLINE_1, AutoUpdatePolicy.VETO);
        setAutoUpdatePolicy(CHANNEL_UID_ONLINE_2, AutoUpdatePolicy.DEFAULT);

        aum.receiveCommand(event, item);

        assertNothingHappened();
    }

    @Test
    public void testAutoUpdate_policyVETObeatsRECOMMEND() {
        links.add(new ItemChannelLink("test", CHANNEL_UID_ONLINE_1));
        links.add(new ItemChannelLink("test", CHANNEL_UID_ONLINE_2));
        setAutoUpdatePolicy(CHANNEL_UID_ONLINE_1, AutoUpdatePolicy.VETO);
        setAutoUpdatePolicy(CHANNEL_UID_ONLINE_2, AutoUpdatePolicy.RECOMMEND);

        aum.receiveCommand(event, item);

        assertNothingHappened();
    }

    @Test
    public void testAutoUpdate_policyDEFAULTbeatsRECOMMEND() {
        links.add(new ItemChannelLink("test", CHANNEL_UID_ONLINE_1));
        links.add(new ItemChannelLink("test", CHANNEL_UID_ONLINE_2));
        setAutoUpdatePolicy(CHANNEL_UID_ONLINE_1, AutoUpdatePolicy.DEFAULT);
        setAutoUpdatePolicy(CHANNEL_UID_ONLINE_2, AutoUpdatePolicy.RECOMMEND);

        aum.receiveCommand(event, item);

        assertChangeStateTo();
    }

    @Test
    public void testAutoUpdate_errorInvalidatesVETO() {
        links.add(new ItemChannelLink("test", CHANNEL_UID_ONLINE_1));
        links.add(new ItemChannelLink("test", CHANNEL_UID_ONLINE_GONE));
        setAutoUpdatePolicy(CHANNEL_UID_ONLINE_1, AutoUpdatePolicy.RECOMMEND);
        setAutoUpdatePolicy(CHANNEL_UID_ONLINE_GONE, AutoUpdatePolicy.VETO);

        aum.receiveCommand(event, item);

        assertStateEvent("AFTER", AutoUpdateManager.EVENT_SOURCE);
    }

    @Test
    public void testAutoUpdate_errorInvalidatesVETO2() {
        links.add(new ItemChannelLink("test", CHANNEL_UID_ONLINE_1));
        links.add(new ItemChannelLink("test", CHANNEL_UID_ONLINE_GONE));
        setAutoUpdatePolicy(CHANNEL_UID_ONLINE_1, AutoUpdatePolicy.DEFAULT);
        setAutoUpdatePolicy(CHANNEL_UID_ONLINE_GONE, AutoUpdatePolicy.VETO);

        aum.receiveCommand(event, item);

        assertChangeStateTo();
    }

    @Test
    public void testAutoUpdate_errorInvalidatesDEFAULT() {
        links.add(new ItemChannelLink("test", CHANNEL_UID_ONLINE_1));
        links.add(new ItemChannelLink("test", CHANNEL_UID_ONLINE_GONE));
        setAutoUpdatePolicy(CHANNEL_UID_ONLINE_1, AutoUpdatePolicy.RECOMMEND);
        setAutoUpdatePolicy(CHANNEL_UID_ONLINE_GONE, AutoUpdatePolicy.DEFAULT);

        aum.receiveCommand(event, item);

        assertStateEvent("AFTER", AutoUpdateManager.EVENT_SOURCE);
    }

    @Test
    public void testAutoUpdate_multipleErrors() {
        links.add(new ItemChannelLink("test", CHANNEL_UID_ONLINE_GONE));
        links.add(new ItemChannelLink("test", CHANNEL_UID_ONLINE_GONE));
        setAutoUpdatePolicy(CHANNEL_UID_ONLINE_GONE, AutoUpdatePolicy.DEFAULT);

        aum.receiveCommand(event, item);

        assertKeepCurrentState();
    }

    @Test
    public void testAutoUpdate_disabled() {
        aum.modified(Collections.singletonMap(AutoUpdateManager.PROPERTY_ENABLED, "false"));

        aum.receiveCommand(event, item);

        assertNothingHappened();
    }

    @Test
    public void testAutoUpdate_sendOptimisticUpdates() {
        links.add(new ItemChannelLink("test", CHANNEL_UID_ONLINE_1));
        aum.modified(Collections.singletonMap(AutoUpdateManager.PROPERTY_SEND_OPTIMISTIC_UPDATES, "true"));

        aum.receiveCommand(event, item);

        assertPredictionEvent("AFTER", null);
        assertStateEvent("AFTER", AutoUpdateManager.EVENT_SOURCE_OPTIMISTIC); // no?
    }

}
