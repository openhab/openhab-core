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
package org.openhab.core.thing.events;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.events.Event;
import org.openhab.core.internal.types.StateDescriptionFragmentImpl;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.CommonTriggerEvents;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.binding.builder.ThingStatusInfoBuilder;
import org.openhab.core.thing.dto.ThingDTOMapper;
import org.openhab.core.thing.events.ChannelDescriptionChangedEvent.CommonChannelDescriptionField;
import org.openhab.core.thing.events.ThingEventFactory.ChannelDescriptionChangedEventPayloadBean;
import org.openhab.core.thing.events.ThingEventFactory.ChannelDescriptionPatternPayloadBean;
import org.openhab.core.thing.events.ThingEventFactory.ChannelDescriptionStateOptionsPayloadBean;
import org.openhab.core.thing.events.ThingEventFactory.TriggerEventPayloadBean;
import org.openhab.core.types.StateDescriptionFragment;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.StateOption;

import com.google.gson.Gson;

/**
 * {@link ThingEventFactoryTest} tests the {@link ThingEventFactory}.
 *
 * @author Stefan Bußweiler - Initial contribution
 * @author Christoph Weitkamp - Added ChannelStateDescriptionChangedEvent
 */
@NonNullByDefault
public class ThingEventFactoryTest {
    private static final Gson JSONCONVERTER = new Gson();

    private static final ThingStatusInfo THING_STATUS_INFO = ThingStatusInfoBuilder
            .create(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR).withDescription("Some description")
            .build();

    private final ThingEventFactory factory = new ThingEventFactory();

    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID("binding:type");
    private static final ThingUID THING_UID = new ThingUID(THING_TYPE_UID, "id");
    private static final Thing THING = ThingBuilder.create(THING_TYPE_UID, THING_UID).build();

    private static final String THING_STATUS_EVENT_TOPIC = ThingEventFactory.THING_STATUS_INFO_EVENT_TOPIC
            .replace("{thingUID}", THING_UID.getAsString());
    private static final String THING_ADDED_EVENT_TOPIC = ThingEventFactory.THING_ADDED_EVENT_TOPIC
            .replace("{thingUID}", THING_UID.getAsString());

    private static final String THING_STATUS_EVENT_PAYLOAD = JSONCONVERTER.toJson(THING_STATUS_INFO);
    private static final String THING_ADDED_EVENT_PAYLOAD = JSONCONVERTER.toJson(ThingDTOMapper.map(THING));

    private static final ChannelUID CHANNEL_UID = new ChannelUID(THING_UID, "channel");
    private static final String CHANNEL_DESCRIPTION_CHANGED_EVENT_TOPIC = ThingEventFactory.CHANNEL_DESCRIPTION_CHANGED_TOPIC
            .replace("{channelUID}", CHANNEL_UID.getAsString());
    private static final String CHANNEL_DESCRIPTION_PATTERN_PAYLOAD = JSONCONVERTER
            .toJson(new ChannelDescriptionPatternPayloadBean("%s"));
    private static final String CHANNEL_DESCRIPTION_OLD_PATTERN_PAYLOAD = JSONCONVERTER
            .toJson(new ChannelDescriptionPatternPayloadBean("%unit%"));
    private static final String CHANNEL_DESCRIPTION_CHANGED_EVENT_PAYLOAD_ONLY_NEW_VALUE = JSONCONVERTER
            .toJson(new ChannelDescriptionChangedEventPayloadBean(CommonChannelDescriptionField.PATTERN,
                    CHANNEL_UID.getAsString(), Set.of("item1", "item2"), CHANNEL_DESCRIPTION_PATTERN_PAYLOAD, null));
    private static final String CHANNEL_DESCRIPTION_CHANGED_EVENT_PAYLOAD_NEW_AND_OLD_VALUE = JSONCONVERTER
            .toJson(new ChannelDescriptionChangedEventPayloadBean(CommonChannelDescriptionField.PATTERN,
                    CHANNEL_UID.getAsString(), Set.of("item1", "item2"), CHANNEL_DESCRIPTION_PATTERN_PAYLOAD,
                    CHANNEL_DESCRIPTION_OLD_PATTERN_PAYLOAD));
    private static final String CHANNEL_DESCRIPTION_STATE_OPTIONS_PAYLOAD = JSONCONVERTER
            .toJson(new ChannelDescriptionStateOptionsPayloadBean(List.of(new StateOption("offline", "Offline"))));
    private static final String CHANNEL_DESCRIPTION_OLD_STATE_OPTIONS_PAYLOAD = JSONCONVERTER
            .toJson(new ChannelDescriptionStateOptionsPayloadBean(List.of(new StateOption("online", "Online"))));
    private static final String CHANNEL_DESCRIPTION_CHANGED_EVENT_PAYLOAD_ONLY_NEW_OPTIONS = JSONCONVERTER
            .toJson(new ChannelDescriptionChangedEventPayloadBean(CommonChannelDescriptionField.STATE_OPTIONS,
                    CHANNEL_UID.getAsString(), Set.of("item1", "item2"), CHANNEL_DESCRIPTION_STATE_OPTIONS_PAYLOAD,
                    null));
    private static final String CHANNEL_DESCRIPTION_CHANGED_EVENT_PAYLOAD_NEW_AND_OLD_OPTIONS = JSONCONVERTER
            .toJson(new ChannelDescriptionChangedEventPayloadBean(CommonChannelDescriptionField.STATE_OPTIONS,
                    CHANNEL_UID.getAsString(), Set.of("item1", "item2"), CHANNEL_DESCRIPTION_STATE_OPTIONS_PAYLOAD,
                    CHANNEL_DESCRIPTION_OLD_STATE_OPTIONS_PAYLOAD));
    private static final String CHANNEL_DESCRIPTION_STATE_DESCRIPTION_PAYLOAD = JSONCONVERTER
            .toJson(StateDescriptionFragmentBuilder.create() //
                    .withMinimum(BigDecimal.ZERO) //
                    .withMaximum(new BigDecimal(1000)) //
                    .withStep(new BigDecimal(100)) //
                    .withPattern("%.0f K") //
                    .build());
    private static final String CHANNEL_DESCRIPTION_OLD_STATE_DESCRIPTION_PAYLOAD = JSONCONVERTER
            .toJson(StateDescriptionFragmentBuilder.create() //
                    .withMinimum(BigDecimal.ZERO) //
                    .withMaximum(new BigDecimal(6000)) //
                    .withStep(new BigDecimal(100)) //
                    .withPattern("%.0f K") //
                    .build());
    private static final String CHANNEL_DESCRIPTION_CHANGED_EVENT_PAYLOAD_NEW_AND_OLD_DESCRIPTION = JSONCONVERTER
            .toJson(new ChannelDescriptionChangedEventPayloadBean(CommonChannelDescriptionField.ALL,
                    CHANNEL_UID.getAsString(), Set.of("item1", "item2"), CHANNEL_DESCRIPTION_STATE_DESCRIPTION_PAYLOAD,
                    CHANNEL_DESCRIPTION_OLD_STATE_DESCRIPTION_PAYLOAD));
    private static final String CHANNEL_TRIGGERED_EVENT_TOPIC = ThingEventFactory.CHANNEL_TRIGGERED_EVENT_TOPIC
            .replace("{channelUID}", CHANNEL_UID.getAsString());
    private static final String CHANNEL_TRIGGERED_PRESSED_EVENT_PAYLOAD = new Gson()
            .toJson(new TriggerEventPayloadBean(CommonTriggerEvents.PRESSED, CHANNEL_UID.getAsString()));
    private static final String CHANNEL_TRIGGERED_EMPTY_EVENT_PAYLOAD = new Gson()
            .toJson(new TriggerEventPayloadBean("", CHANNEL_UID.getAsString()));

    @Test
    public void testSupportedEventTypes() {
        assertThat(factory.getSupportedEventTypes(),
                containsInAnyOrder(ThingStatusInfoEvent.TYPE, ThingStatusInfoChangedEvent.TYPE, ThingAddedEvent.TYPE,
                        ThingRemovedEvent.TYPE, ThingUpdatedEvent.TYPE, ChannelDescriptionChangedEvent.TYPE,
                        ChannelTriggeredEvent.TYPE));
    }

    @Test
    public void testCreateEventThingStatusInfoEvent() throws Exception {
        Event event = factory.createEvent(ThingStatusInfoEvent.TYPE, THING_STATUS_EVENT_TOPIC,
                THING_STATUS_EVENT_PAYLOAD, null);

        assertThat(event, is(instanceOf(ThingStatusInfoEvent.class)));
        ThingStatusInfoEvent statusEvent = (ThingStatusInfoEvent) event;
        assertEquals(ThingStatusInfoEvent.TYPE, statusEvent.getType());
        assertEquals(THING_STATUS_EVENT_TOPIC, statusEvent.getTopic());
        assertEquals(THING_STATUS_EVENT_PAYLOAD, statusEvent.getPayload());
        assertEquals(THING_STATUS_INFO, statusEvent.getStatusInfo());
        assertEquals(THING_UID, statusEvent.getThingUID());
    }

    @Test
    public void testCreateStatusInfoEvent() {
        ThingStatusInfoEvent event = ThingEventFactory.createStatusInfoEvent(THING_UID, THING_STATUS_INFO);

        assertEquals(ThingStatusInfoEvent.TYPE, event.getType());
        assertEquals(THING_STATUS_EVENT_TOPIC, event.getTopic());
        assertEquals(THING_STATUS_EVENT_PAYLOAD, event.getPayload());
        assertEquals(THING_STATUS_INFO, event.getStatusInfo());
        assertEquals(THING_UID, event.getThingUID());
    }

    @Test
    public void testCreateEventThingAddedEvent() throws Exception {
        Event event = factory.createEvent(ThingAddedEvent.TYPE, THING_ADDED_EVENT_TOPIC, THING_ADDED_EVENT_PAYLOAD,
                null);

        assertThat(event, is(instanceOf(ThingAddedEvent.class)));
        ThingAddedEvent addedEvent = (ThingAddedEvent) event;
        assertEquals(ThingAddedEvent.TYPE, addedEvent.getType());
        assertEquals(THING_ADDED_EVENT_TOPIC, addedEvent.getTopic());
        assertEquals(THING_ADDED_EVENT_PAYLOAD, addedEvent.getPayload());
        assertNotNull(addedEvent.getThing());
        assertEquals(THING_UID.getAsString(), addedEvent.getThing().UID);
    }

    @Test
    public void testCreateAddedEvent() {
        ThingAddedEvent event = ThingEventFactory.createAddedEvent(THING);

        assertEquals(ThingAddedEvent.TYPE, event.getType());
        assertEquals(THING_ADDED_EVENT_TOPIC, event.getTopic());
        assertEquals(THING_ADDED_EVENT_PAYLOAD, event.getPayload());
        assertNotNull(event.getThing());
        assertEquals(THING_UID.getAsString(), event.getThing().UID);
    }

    @Test
    public void testCreateChannelDescriptionChangedEventOnlyNewValue() {
        ChannelDescriptionChangedEvent event = ThingEventFactory
                .createChannelDescriptionPatternChangedEvent(CHANNEL_UID, Set.of("item1", "item2"), "%s", null);

        assertEquals(ChannelDescriptionChangedEvent.TYPE, event.getType());
        assertEquals(CHANNEL_DESCRIPTION_CHANGED_EVENT_TOPIC, event.getTopic());
        assertEquals(CHANNEL_DESCRIPTION_CHANGED_EVENT_PAYLOAD_ONLY_NEW_VALUE, event.getPayload());
        assertEquals(CommonChannelDescriptionField.PATTERN, event.getField());
        assertEquals(CHANNEL_UID, event.getChannelUID());
        assertThat(event.getLinkedItemNames(), hasSize(2));
        assertEquals(CHANNEL_DESCRIPTION_PATTERN_PAYLOAD, event.getValue());
        assertNull(event.getOldValue());
    }

    @Test
    public void testCreateChannelDescriptionChangedEventNewAndOldValue() {
        ChannelDescriptionChangedEvent event = ThingEventFactory
                .createChannelDescriptionPatternChangedEvent(CHANNEL_UID, Set.of("item1", "item2"), "%s", "%unit%");

        assertEquals(ChannelDescriptionChangedEvent.TYPE, event.getType());
        assertEquals(CHANNEL_DESCRIPTION_CHANGED_EVENT_TOPIC, event.getTopic());
        assertEquals(CHANNEL_DESCRIPTION_CHANGED_EVENT_PAYLOAD_NEW_AND_OLD_VALUE, event.getPayload());
        assertEquals(CommonChannelDescriptionField.PATTERN, event.getField());
        assertEquals(CHANNEL_UID, event.getChannelUID());
        assertThat(event.getLinkedItemNames(), hasSize(2));
        assertEquals(CHANNEL_DESCRIPTION_PATTERN_PAYLOAD, event.getValue());
        assertEquals(CHANNEL_DESCRIPTION_OLD_PATTERN_PAYLOAD, event.getOldValue());
    }

    @Test
    public void testCreateEventChannelDescriptionChangedEventOnlyNewValue() throws Exception {
        Event event = factory.createEvent(ChannelDescriptionChangedEvent.TYPE, CHANNEL_DESCRIPTION_CHANGED_EVENT_TOPIC,
                CHANNEL_DESCRIPTION_CHANGED_EVENT_PAYLOAD_ONLY_NEW_VALUE, null);

        assertThat(event, is(instanceOf(ChannelDescriptionChangedEvent.class)));
        ChannelDescriptionChangedEvent triggeredEvent = (ChannelDescriptionChangedEvent) event;
        assertEquals(ChannelDescriptionChangedEvent.TYPE, triggeredEvent.getType());
        assertEquals(CHANNEL_DESCRIPTION_CHANGED_EVENT_TOPIC, triggeredEvent.getTopic());
        assertEquals(CHANNEL_DESCRIPTION_CHANGED_EVENT_PAYLOAD_ONLY_NEW_VALUE, triggeredEvent.getPayload());
        assertEquals(CommonChannelDescriptionField.PATTERN, triggeredEvent.getField());
        assertEquals(CHANNEL_UID, triggeredEvent.getChannelUID());
        assertThat(triggeredEvent.getLinkedItemNames(), hasSize(2));
        assertEquals(CHANNEL_DESCRIPTION_PATTERN_PAYLOAD, triggeredEvent.getValue());
        assertNull(triggeredEvent.getOldValue());
    }

    @Test
    public void testCreateEventChannelDescriptionChangedEventNewAndOldValue() throws Exception {
        Event event = factory.createEvent(ChannelDescriptionChangedEvent.TYPE, CHANNEL_DESCRIPTION_CHANGED_EVENT_TOPIC,
                CHANNEL_DESCRIPTION_CHANGED_EVENT_PAYLOAD_NEW_AND_OLD_VALUE, null);

        assertThat(event, is(instanceOf(ChannelDescriptionChangedEvent.class)));
        ChannelDescriptionChangedEvent triggeredEvent = (ChannelDescriptionChangedEvent) event;
        assertEquals(ChannelDescriptionChangedEvent.TYPE, triggeredEvent.getType());
        assertEquals(CHANNEL_DESCRIPTION_CHANGED_EVENT_TOPIC, triggeredEvent.getTopic());
        assertEquals(CHANNEL_DESCRIPTION_CHANGED_EVENT_PAYLOAD_NEW_AND_OLD_VALUE, triggeredEvent.getPayload());
        assertEquals(CommonChannelDescriptionField.PATTERN, triggeredEvent.getField());
        assertEquals(CHANNEL_UID, triggeredEvent.getChannelUID());
        assertThat(triggeredEvent.getLinkedItemNames(), hasSize(2));
        assertEquals(CHANNEL_DESCRIPTION_PATTERN_PAYLOAD, triggeredEvent.getValue());
        assertEquals(CHANNEL_DESCRIPTION_OLD_PATTERN_PAYLOAD, triggeredEvent.getOldValue());
    }

    @Test
    public void testCreateChannelDescriptionChangedEventOnlyNewOptions() {
        Set<String> itemNames = Set.of("item1", "item2");
        List<StateOption> options = List.of(new StateOption("offline", "Offline"));
        ChannelDescriptionChangedEvent event = ThingEventFactory
                .createChannelDescriptionStateOptionsChangedEvent(CHANNEL_UID, itemNames, options, null);

        assertEquals(ChannelDescriptionChangedEvent.TYPE, event.getType());
        assertEquals(CHANNEL_DESCRIPTION_CHANGED_EVENT_TOPIC, event.getTopic());
        assertEquals(CHANNEL_DESCRIPTION_CHANGED_EVENT_PAYLOAD_ONLY_NEW_OPTIONS, event.getPayload());
        assertEquals(CommonChannelDescriptionField.STATE_OPTIONS, event.getField());
        assertEquals(CHANNEL_UID, event.getChannelUID());
        assertEquals(itemNames, event.getLinkedItemNames());
        assertEquals(CHANNEL_DESCRIPTION_STATE_OPTIONS_PAYLOAD, event.getValue());
        assertEquals(options,
                JSONCONVERTER.fromJson(event.getValue(), ChannelDescriptionStateOptionsPayloadBean.class).options);
        assertNull(event.getOldValue());
    }

    @Test
    public void testCreateEventChannelDescriptionChangedEventOnlyNewOptions() throws Exception {
        List<StateOption> options = List.of(new StateOption("offline", "Offline"));
        Event event = factory.createEvent(ChannelDescriptionChangedEvent.TYPE, CHANNEL_DESCRIPTION_CHANGED_EVENT_TOPIC,
                CHANNEL_DESCRIPTION_CHANGED_EVENT_PAYLOAD_ONLY_NEW_OPTIONS, null);

        assertThat(event, is(instanceOf(ChannelDescriptionChangedEvent.class)));
        ChannelDescriptionChangedEvent triggeredEvent = (ChannelDescriptionChangedEvent) event;
        assertEquals(ChannelDescriptionChangedEvent.TYPE, triggeredEvent.getType());
        assertEquals(CHANNEL_DESCRIPTION_CHANGED_EVENT_TOPIC, triggeredEvent.getTopic());
        assertEquals(CHANNEL_DESCRIPTION_CHANGED_EVENT_PAYLOAD_ONLY_NEW_OPTIONS, triggeredEvent.getPayload());
        assertEquals(CommonChannelDescriptionField.STATE_OPTIONS, triggeredEvent.getField());
        assertEquals(CHANNEL_UID, triggeredEvent.getChannelUID());
        assertEquals(Set.of("item1", "item2"), triggeredEvent.getLinkedItemNames());
        assertEquals(CHANNEL_DESCRIPTION_STATE_OPTIONS_PAYLOAD, triggeredEvent.getValue());
        assertEquals(options, JSONCONVERTER.fromJson(triggeredEvent.getValue(),
                ChannelDescriptionStateOptionsPayloadBean.class).options);
        assertNull(triggeredEvent.getOldValue());
    }

    @Test
    public void testCreateChannelDescriptionChangedEventOldAndNewOptions() {
        Set<String> itemNames = Set.of("item1", "item2");
        List<StateOption> options = List.of(new StateOption("offline", "Offline"));
        List<StateOption> oldOptions = List.of(new StateOption("online", "Online"));
        ChannelDescriptionChangedEvent event = ThingEventFactory
                .createChannelDescriptionStateOptionsChangedEvent(CHANNEL_UID, itemNames, options, oldOptions);

        assertEquals(ChannelDescriptionChangedEvent.TYPE, event.getType());
        assertEquals(CHANNEL_DESCRIPTION_CHANGED_EVENT_TOPIC, event.getTopic());
        assertEquals(CHANNEL_DESCRIPTION_CHANGED_EVENT_PAYLOAD_NEW_AND_OLD_OPTIONS, event.getPayload());
        assertEquals(CommonChannelDescriptionField.STATE_OPTIONS, event.getField());
        assertEquals(CHANNEL_UID, event.getChannelUID());
        assertEquals(itemNames, event.getLinkedItemNames());
        assertEquals(CHANNEL_DESCRIPTION_STATE_OPTIONS_PAYLOAD, event.getValue());
        assertEquals(options,
                JSONCONVERTER.fromJson(event.getValue(), ChannelDescriptionStateOptionsPayloadBean.class).options);
        assertEquals(CHANNEL_DESCRIPTION_OLD_STATE_OPTIONS_PAYLOAD, event.getOldValue());
        assertEquals(oldOptions,
                JSONCONVERTER.fromJson(event.getOldValue(), ChannelDescriptionStateOptionsPayloadBean.class).options);
    }

    @Test
    public void testCreateEventChannelDescriptionChangedEventOldAndNewOptions() throws Exception {
        List<StateOption> options = List.of(new StateOption("offline", "Offline"));
        List<StateOption> oldOptions = List.of(new StateOption("online", "Online"));
        Event event = factory.createEvent(ChannelDescriptionChangedEvent.TYPE, CHANNEL_DESCRIPTION_CHANGED_EVENT_TOPIC,
                CHANNEL_DESCRIPTION_CHANGED_EVENT_PAYLOAD_NEW_AND_OLD_OPTIONS, null);

        assertThat(event, is(instanceOf(ChannelDescriptionChangedEvent.class)));
        ChannelDescriptionChangedEvent triggeredEvent = (ChannelDescriptionChangedEvent) event;
        assertEquals(ChannelDescriptionChangedEvent.TYPE, triggeredEvent.getType());
        assertEquals(CHANNEL_DESCRIPTION_CHANGED_EVENT_TOPIC, triggeredEvent.getTopic());
        assertEquals(CHANNEL_DESCRIPTION_CHANGED_EVENT_PAYLOAD_NEW_AND_OLD_OPTIONS, triggeredEvent.getPayload());
        assertEquals(CommonChannelDescriptionField.STATE_OPTIONS, triggeredEvent.getField());
        assertEquals(CHANNEL_UID, triggeredEvent.getChannelUID());
        assertEquals(Set.of("item1", "item2"), triggeredEvent.getLinkedItemNames());
        assertEquals(CHANNEL_DESCRIPTION_STATE_OPTIONS_PAYLOAD, triggeredEvent.getValue());
        assertEquals(options, JSONCONVERTER.fromJson(triggeredEvent.getValue(),
                ChannelDescriptionStateOptionsPayloadBean.class).options);
        assertEquals(CHANNEL_DESCRIPTION_OLD_STATE_OPTIONS_PAYLOAD, triggeredEvent.getOldValue());
        assertEquals(oldOptions, JSONCONVERTER.fromJson(triggeredEvent.getOldValue(),
                ChannelDescriptionStateOptionsPayloadBean.class).options);
    }

    @Test
    public void testCreateChannelDescriptionChangedEventOldAndNewStateDescription() {
        Set<String> itemNames = Set.of("item1", "item2");
        StateDescriptionFragment stateDescriptionFragment = StateDescriptionFragmentBuilder.create() //
                .withMinimum(BigDecimal.ZERO) //
                .withMaximum(new BigDecimal(1000)) //
                .withStep(new BigDecimal(100)) //
                .withPattern("%.0f K") //
                .build();
        StateDescriptionFragment oldStateDescriptionFragment = StateDescriptionFragmentBuilder.create() //
                .withMinimum(BigDecimal.ZERO) //
                .withMaximum(new BigDecimal(6000)) //
                .withStep(new BigDecimal(100)) //
                .withPattern("%.0f K") //
                .build();
        ChannelDescriptionChangedEvent event = ThingEventFactory.createChannelDescriptionChangedEvent(CHANNEL_UID,
                itemNames, stateDescriptionFragment, oldStateDescriptionFragment);

        assertEquals(ChannelDescriptionChangedEvent.TYPE, event.getType());
        assertEquals(CHANNEL_DESCRIPTION_CHANGED_EVENT_TOPIC, event.getTopic());
        assertEquals(CHANNEL_DESCRIPTION_CHANGED_EVENT_PAYLOAD_NEW_AND_OLD_DESCRIPTION, event.getPayload());
        assertEquals(CommonChannelDescriptionField.ALL, event.getField());
        assertEquals(CHANNEL_UID, event.getChannelUID());
        assertEquals(itemNames, event.getLinkedItemNames());
        assertEquals(CHANNEL_DESCRIPTION_STATE_DESCRIPTION_PAYLOAD, event.getValue());
        assertEquals(stateDescriptionFragment,
                JSONCONVERTER.fromJson(event.getValue(), StateDescriptionFragmentImpl.class));
        assertEquals(CHANNEL_DESCRIPTION_OLD_STATE_DESCRIPTION_PAYLOAD, event.getOldValue());
        assertEquals(oldStateDescriptionFragment,
                JSONCONVERTER.fromJson(event.getOldValue(), StateDescriptionFragmentImpl.class));
    }

    @Test
    public void testCreateTriggerPressedEvent() {
        ChannelTriggeredEvent event = ThingEventFactory.createTriggerEvent(CommonTriggerEvents.PRESSED, CHANNEL_UID);

        assertEquals(ChannelTriggeredEvent.TYPE, event.getType());
        assertEquals(CHANNEL_TRIGGERED_EVENT_TOPIC, event.getTopic());
        assertEquals(CHANNEL_TRIGGERED_PRESSED_EVENT_PAYLOAD, event.getPayload());
        assertNotNull(event.getEvent());
        assertEquals(CommonTriggerEvents.PRESSED, event.getEvent());
        assertEquals(CHANNEL_UID, event.getChannel());
    }

    @Test
    public void testCreateEventChannelTriggeredPressedEvent() throws Exception {
        Event event = factory.createEvent(ChannelTriggeredEvent.TYPE, CHANNEL_TRIGGERED_EVENT_TOPIC,
                CHANNEL_TRIGGERED_PRESSED_EVENT_PAYLOAD, null);

        assertThat(event, is(instanceOf(ChannelTriggeredEvent.class)));
        ChannelTriggeredEvent triggeredEvent = (ChannelTriggeredEvent) event;
        assertEquals(ChannelTriggeredEvent.TYPE, triggeredEvent.getType());
        assertEquals(CHANNEL_TRIGGERED_EVENT_TOPIC, triggeredEvent.getTopic());
        assertEquals(CHANNEL_TRIGGERED_PRESSED_EVENT_PAYLOAD, triggeredEvent.getPayload());
        assertNotNull(triggeredEvent.getEvent());
        assertEquals(CommonTriggerEvents.PRESSED, triggeredEvent.getEvent());
        assertEquals(CHANNEL_UID, triggeredEvent.getChannel());
    }

    @Test
    public void testCreateTriggerEmptyEvent() {
        ChannelTriggeredEvent event = ThingEventFactory.createTriggerEvent("", CHANNEL_UID);

        assertEquals(ChannelTriggeredEvent.TYPE, event.getType());
        assertEquals(CHANNEL_TRIGGERED_EVENT_TOPIC, event.getTopic());
        assertEquals(CHANNEL_TRIGGERED_EMPTY_EVENT_PAYLOAD, event.getPayload());
        assertNotNull(event.getEvent());
        assertEquals("", event.getEvent());
        assertEquals(CHANNEL_UID, event.getChannel());
    }

    @Test
    public void testCreateEventChannelTriggeredEmptyEvent() throws Exception {
        Event event = factory.createEvent(ChannelTriggeredEvent.TYPE, CHANNEL_TRIGGERED_EVENT_TOPIC,
                CHANNEL_TRIGGERED_EMPTY_EVENT_PAYLOAD, null);

        assertThat(event, is(instanceOf(ChannelTriggeredEvent.class)));
        ChannelTriggeredEvent triggeredEvent = (ChannelTriggeredEvent) event;
        assertEquals(ChannelTriggeredEvent.TYPE, triggeredEvent.getType());
        assertEquals(CHANNEL_TRIGGERED_EVENT_TOPIC, triggeredEvent.getTopic());
        assertEquals(CHANNEL_TRIGGERED_EMPTY_EVENT_PAYLOAD, triggeredEvent.getPayload());
        assertNotNull(triggeredEvent.getEvent());
        assertEquals("", triggeredEvent.getEvent());
        assertEquals(CHANNEL_UID, triggeredEvent.getChannel());
    }
}
