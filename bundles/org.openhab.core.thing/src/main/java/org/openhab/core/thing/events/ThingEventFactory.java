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

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.AbstractEventFactory;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFactory;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.dto.ThingDTO;
import org.openhab.core.thing.dto.ThingDTOMapper;
import org.openhab.core.thing.events.ChannelDescriptionChangedEvent.CommonChannelDescriptionField;
import org.openhab.core.types.CommandOption;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateDescriptionFragment;
import org.openhab.core.types.StateOption;
import org.osgi.service.component.annotations.Component;

/**
 * A {@link ThingEventFactory} is responsible for creating thing event instances:
 * <ul>
 * <li>{@link ThingStatusInfoEvent#TYPE}</li>
 * </ul>
 *
 * @author Stefan Bußweiler - Initial contribution
 * @author Dennis Nobel - Added status changed event
 * @author Christoph Weitkamp - Added ChannelDescriptionChangedEvent
 */
@Component(immediate = true, service = EventFactory.class)
@NonNullByDefault
public class ThingEventFactory extends AbstractEventFactory {
    static final String THING_STATUS_INFO_EVENT_TOPIC = "openhab/things/{thingUID}/status";

    static final String THING_STATUS_INFO_CHANGED_EVENT_TOPIC = "openhab/things/{thingUID}/statuschanged";

    static final String THING_ADDED_EVENT_TOPIC = "openhab/things/{thingUID}/added";

    static final String THING_REMOVED_EVENT_TOPIC = "openhab/things/{thingUID}/removed";

    static final String THING_UPDATED_EVENT_TOPIC = "openhab/things/{thingUID}/updated";

    static final String CHANNEL_DESCRIPTION_CHANGED_TOPIC = "openhab/channels/{channelUID}/descriptionchanged";

    static final String CHANNEL_TRIGGERED_EVENT_TOPIC = "openhab/channels/{channelUID}/triggered";

    /**
     * Constructs a new ThingEventFactory.
     */
    public ThingEventFactory() {
        super(Set.of(ThingStatusInfoEvent.TYPE, ThingStatusInfoChangedEvent.TYPE, ThingAddedEvent.TYPE,
                ThingRemovedEvent.TYPE, ThingUpdatedEvent.TYPE, ChannelDescriptionChangedEvent.TYPE,
                ChannelTriggeredEvent.TYPE));
    }

    @Override
    protected Event createEventByType(String eventType, String topic, String payload, @Nullable String source)
            throws Exception {
        if (ThingStatusInfoEvent.TYPE.equals(eventType)) {
            return createStatusInfoEvent(topic, payload);
        } else if (ThingStatusInfoChangedEvent.TYPE.equals(eventType)) {
            return createStatusInfoChangedEvent(topic, payload);
        } else if (ThingAddedEvent.TYPE.equals(eventType)) {
            return createAddedEvent(topic, payload);
        } else if (ThingRemovedEvent.TYPE.equals(eventType)) {
            return createRemovedEvent(topic, payload);
        } else if (ThingUpdatedEvent.TYPE.equals(eventType)) {
            return createUpdatedEvent(topic, payload);
        } else if (ChannelDescriptionChangedEvent.TYPE.equals(eventType)) {
            return createChannelDescriptionChangedEvent(topic, payload);
        } else if (ChannelTriggeredEvent.TYPE.equals(eventType)) {
            return createTriggerEvent(topic, payload, source);
        }
        throw new IllegalArgumentException("The event type '" + eventType + "' is not supported by this factory.");
    }

    public static class ChannelDescriptionChangedEventPayloadBean {
        public @NonNullByDefault({}) CommonChannelDescriptionField field;
        public @NonNullByDefault({}) String channelUID;
        public Set<String> linkedItemNames = Set.of();
        public @NonNullByDefault({}) String value;
        public @Nullable String oldValue;

        /**
         * Default constructor for deserialization e.g. by Gson.
         */
        protected ChannelDescriptionChangedEventPayloadBean() {
        }

        public ChannelDescriptionChangedEventPayloadBean(CommonChannelDescriptionField field, String channelUID,
                Set<String> linkedItemNames, String value, @Nullable String oldValue) {
            this.field = field;
            this.channelUID = channelUID;
            this.linkedItemNames = linkedItemNames;
            this.value = value;
            this.oldValue = oldValue;
        }
    }

    public static interface CommonChannelDescriptionFieldPayloadBean {
    }

    public static class ChannelDescriptionPatternPayloadBean implements CommonChannelDescriptionFieldPayloadBean {
        public @NonNullByDefault({}) String pattern;

        /**
         * Default constructor for deserialization e.g. by Gson.
         */
        protected ChannelDescriptionPatternPayloadBean() {
        }

        public ChannelDescriptionPatternPayloadBean(String pattern) {
            this.pattern = pattern;
        }
    }

    public static class ChannelDescriptionStateOptionsPayloadBean implements CommonChannelDescriptionFieldPayloadBean {
        public @NonNullByDefault({}) List<StateOption> options;

        /**
         * Default constructor for deserialization e.g. by Gson.
         */
        protected ChannelDescriptionStateOptionsPayloadBean() {
        }

        public ChannelDescriptionStateOptionsPayloadBean(List<StateOption> options) {
            this.options = options;
        }
    }

    public static class ChannelDescriptionCommandOptionsPayloadBean
            implements CommonChannelDescriptionFieldPayloadBean {
        public @NonNullByDefault({}) List<CommandOption> options;

        /**
         * Default constructor for deserialization e.g. by Gson.
         */
        protected ChannelDescriptionCommandOptionsPayloadBean() {
        }

        public ChannelDescriptionCommandOptionsPayloadBean(List<CommandOption> options) {
            this.options = options;
        }
    }

    /**
     * Creates a {@link ChannelDescriptionChangedEvent} for a changed {@link StateDescription}. New and optional old
     * value will be serialized to a JSON string from the {@link StateDescriptionFragment} object.
     *
     * @param channelUID the {@link ChannelUID}
     * @param linkedItemNames a {@link Set} of linked item names
     * @param stateDescription the new {@link StateDescriptionFragment}
     * @param oldStateDescription the old {@link StateDescriptionFragment}
     * @return Created {@link ChannelDescriptionChangedEvent}
     */
    public static ChannelDescriptionChangedEvent createChannelDescriptionChangedEvent(ChannelUID channelUID,
            Set<String> linkedItemNames, StateDescriptionFragment stateDescription,
            @Nullable StateDescriptionFragment oldStateDescription) {
        checkNotNull(linkedItemNames, "linkedItemNames");
        checkNotNull(channelUID, "channelUID");
        checkNotNull(stateDescription, "stateDescription");

        String stateDescriptionPayload = serializePayload(stateDescription);
        String oldStateDescriptionPayload = oldStateDescription != null ? serializePayload(oldStateDescription) : null;
        ChannelDescriptionChangedEventPayloadBean bean = new ChannelDescriptionChangedEventPayloadBean(
                CommonChannelDescriptionField.ALL, channelUID.getAsString(), linkedItemNames, stateDescriptionPayload,
                oldStateDescriptionPayload);
        String payload = serializePayload(bean);
        String topic = buildTopic(CHANNEL_DESCRIPTION_CHANGED_TOPIC, channelUID);
        return new ChannelDescriptionChangedEvent(topic, payload, CommonChannelDescriptionField.ALL, channelUID,
                linkedItemNames, stateDescriptionPayload, oldStateDescriptionPayload);
    }

    /**
     * Creates a {@link ChannelDescriptionChangedEvent} for a changed pattern. New and optional old value will be
     * serialized to a JSON string from the {@link ChannelDescriptionPatternPayloadBean} object.
     *
     * @param channelUID the {@link ChannelUID}
     * @param linkedItemNames a {@link Set} of linked item names
     * @param pattern the new pattern
     * @param oldPattern the old pattern
     * @return Created {@link ChannelDescriptionChangedEvent}
     */
    public static ChannelDescriptionChangedEvent createChannelDescriptionPatternChangedEvent(ChannelUID channelUID,
            Set<String> linkedItemNames, String pattern, @Nullable String oldPattern) {
        checkNotNull(linkedItemNames, "linkedItemNames");
        checkNotNull(channelUID, "channelUID");
        checkNotNull(pattern, "pattern");

        String patternPayload = serializePayload(new ChannelDescriptionPatternPayloadBean(pattern));
        String oldPatternPayload = oldPattern != null
                ? serializePayload(new ChannelDescriptionPatternPayloadBean(oldPattern))
                : null;
        ChannelDescriptionChangedEventPayloadBean bean = new ChannelDescriptionChangedEventPayloadBean(
                CommonChannelDescriptionField.PATTERN, channelUID.getAsString(), linkedItemNames, patternPayload,
                oldPatternPayload);
        String payload = serializePayload(bean);
        String topic = buildTopic(CHANNEL_DESCRIPTION_CHANGED_TOPIC, channelUID);
        return new ChannelDescriptionChangedEvent(topic, payload, CommonChannelDescriptionField.PATTERN, channelUID,
                linkedItemNames, patternPayload, oldPatternPayload);
    }

    /**
     * Creates a {@link ChannelDescriptionChangedEvent} for changed {@link StateOption}s. New and optional old value
     * will be serialized to a JSON string from the {@link ChannelDescriptionStateOptionsPayloadBean} object.
     *
     * @param channelUID the {@link ChannelUID}
     * @param linkedItemNames a {@link Set} of linked item names
     * @param options the new {@link StateOption}s
     * @param oldOptions the old {@link StateOption}s
     * @return Created {@link ChannelDescriptionChangedEvent}
     */
    public static ChannelDescriptionChangedEvent createChannelDescriptionStateOptionsChangedEvent(ChannelUID channelUID,
            Set<String> linkedItemNames, List<StateOption> options, @Nullable List<StateOption> oldOptions) {
        checkNotNull(linkedItemNames, "linkedItemNames");
        checkNotNull(channelUID, "channelUID");
        checkNotNull(options, "options");

        String stateOptionsPayload = serializePayload(new ChannelDescriptionStateOptionsPayloadBean(options));
        String oldStateOptionsPayload = oldOptions != null
                ? serializePayload(new ChannelDescriptionStateOptionsPayloadBean(oldOptions))
                : null;
        ChannelDescriptionChangedEventPayloadBean bean = new ChannelDescriptionChangedEventPayloadBean(
                CommonChannelDescriptionField.STATE_OPTIONS, channelUID.getAsString(), linkedItemNames,
                stateOptionsPayload, oldStateOptionsPayload);
        String payload = serializePayload(bean);
        String topic = buildTopic(CHANNEL_DESCRIPTION_CHANGED_TOPIC, channelUID);
        return new ChannelDescriptionChangedEvent(topic, payload, CommonChannelDescriptionField.STATE_OPTIONS,
                channelUID, linkedItemNames, stateOptionsPayload, oldStateOptionsPayload);
    }

    /**
     * Creates a {@link ChannelDescriptionChangedEvent} for change {@link CommandOption}s. New and optional old value
     * will be serialized to a JSON string from the {@link ChannelDescriptionCommandOptionsPayloadBean} object.
     *
     * @param channelUID the {@link ChannelUID}
     * @param linkedItemNames a {@link Set} of linked item names
     * @param options the new {@link CommandOption}s
     * @param oldOptions the old {@link CommandOption}s
     * @return Created {@link ChannelDescriptionChangedEvent}
     */
    public static ChannelDescriptionChangedEvent createChannelDescriptionCommandOptionsChangedEvent(
            ChannelUID channelUID, Set<String> linkedItemNames, List<CommandOption> options,
            @Nullable List<CommandOption> oldOptions) {
        checkNotNull(linkedItemNames, "linkedItemNames");
        checkNotNull(channelUID, "channelUID");
        checkNotNull(options, "options");

        String commandOptionsPayload = serializePayload(new ChannelDescriptionCommandOptionsPayloadBean(options));
        String oldCommandOptionsPayload = oldOptions != null
                ? serializePayload(new ChannelDescriptionCommandOptionsPayloadBean(oldOptions))
                : null;
        ChannelDescriptionChangedEventPayloadBean bean = new ChannelDescriptionChangedEventPayloadBean(
                CommonChannelDescriptionField.COMMAND_OPTIONS, channelUID.getAsString(), linkedItemNames,
                commandOptionsPayload, oldCommandOptionsPayload);
        String payload = serializePayload(bean);
        String topic = buildTopic(CHANNEL_DESCRIPTION_CHANGED_TOPIC, channelUID);
        return new ChannelDescriptionChangedEvent(topic, payload, CommonChannelDescriptionField.COMMAND_OPTIONS,
                channelUID, linkedItemNames, commandOptionsPayload, oldCommandOptionsPayload);
    }

    private ChannelDescriptionChangedEvent createChannelDescriptionChangedEvent(String topic, String payload) {
        String[] topicElements = getTopicElements(topic);
        if (topicElements.length != 4) {
            throw new IllegalArgumentException(
                    "ChannelDescriptionChangedEvent creation failed, invalid topic: " + topic);
        }
        ChannelUID channelUID = new ChannelUID(topicElements[2]);
        ChannelDescriptionChangedEventPayloadBean bean = deserializePayload(payload,
                ChannelDescriptionChangedEventPayloadBean.class);
        return new ChannelDescriptionChangedEvent(topic, payload, bean.field, channelUID, bean.linkedItemNames,
                bean.value, bean.oldValue);
    }

    /**
     * This is a java bean that is used to serialize/deserialize trigger event payload.
     */
    public static class TriggerEventPayloadBean {
        private String event = "";
        private @NonNullByDefault({}) String channel;

        /**
         * Default constructor for deserialization e.g. by Gson.
         */
        protected TriggerEventPayloadBean() {
        }

        public TriggerEventPayloadBean(String event, String channel) {
            this.event = event;
            this.channel = channel;
        }

        public String getEvent() {
            return event;
        }

        public String getChannel() {
            return channel;
        }
    }

    /**
     * Creates a {@link ChannelTriggeredEvent}
     *
     * @param event the event
     * @param channel the {@link ChannelUID}
     * @return Created {@link ChannelTriggeredEvent}
     */
    public static ChannelTriggeredEvent createTriggerEvent(String event, ChannelUID channelUID) {
        checkNotNull(channelUID, "channelUID");
        checkNotNull(event, "event");

        String topic = buildTopic(CHANNEL_TRIGGERED_EVENT_TOPIC, channelUID);
        TriggerEventPayloadBean bean = new TriggerEventPayloadBean(event, channelUID.getAsString());
        String payload = serializePayload(bean);
        return new ChannelTriggeredEvent(topic, payload, null, event, channelUID);
    }

    private Event createTriggerEvent(String topic, String payload, @Nullable String source) {
        String[] topicElements = getTopicElements(topic);
        if (topicElements.length != 4) {
            throw new IllegalArgumentException("ChannelTriggeredEvent creation failed, invalid topic: " + topic);
        }
        ChannelUID channel = new ChannelUID(topicElements[2]);
        TriggerEventPayloadBean bean = deserializePayload(payload, TriggerEventPayloadBean.class);
        return new ChannelTriggeredEvent(topic, payload, source, bean.getEvent(), channel);
    }

    private Event createStatusInfoEvent(String topic, String payload) throws Exception {
        String[] topicElements = getTopicElements(topic);
        if (topicElements.length != 4) {
            throw new IllegalArgumentException("ThingStatusInfoEvent creation failed, invalid topic: " + topic);
        }
        ThingUID thingUID = new ThingUID(topicElements[2]);
        ThingStatusInfo thingStatusInfo = deserializePayload(payload, ThingStatusInfo.class);
        return new ThingStatusInfoEvent(topic, payload, thingUID, thingStatusInfo);
    }

    private Event createStatusInfoChangedEvent(String topic, String payload) throws Exception {
        String[] topicElements = getTopicElements(topic);
        if (topicElements.length != 4) {
            throw new IllegalArgumentException("ThingStatusInfoChangedEvent creation failed, invalid topic: " + topic);
        }
        ThingUID thingUID = new ThingUID(topicElements[2]);
        ThingStatusInfo[] thingStatusInfo = deserializePayload(payload, ThingStatusInfo[].class);
        return new ThingStatusInfoChangedEvent(topic, payload, thingUID, thingStatusInfo[0], thingStatusInfo[1]);
    }

    private Event createAddedEvent(String topic, String payload) throws Exception {
        ThingDTO thingDTO = deserializePayload(payload, ThingDTO.class);
        return new ThingAddedEvent(topic, payload, thingDTO);
    }

    private Event createRemovedEvent(String topic, String payload) throws Exception {
        ThingDTO thingDTO = deserializePayload(payload, ThingDTO.class);
        return new ThingRemovedEvent(topic, payload, thingDTO);
    }

    private Event createUpdatedEvent(String topic, String payload) throws Exception {
        ThingDTO[] thingDTO = deserializePayload(payload, ThingDTO[].class);
        if (thingDTO.length != 2) {
            throw new IllegalArgumentException("ThingUpdateEvent creation failed, invalid payload: " + payload);
        }
        return new ThingUpdatedEvent(topic, payload, thingDTO[0], thingDTO[1]);
    }

    /**
     * Creates a new thing status info event based on a thing UID and a thing status info object.
     *
     * @param thingUID the thing UID
     * @param thingStatusInfo the thing status info object
     * @return the created thing status info event
     * @throws IllegalArgumentException if thingUID or thingStatusInfo is null
     */
    public static ThingStatusInfoEvent createStatusInfoEvent(ThingUID thingUID, ThingStatusInfo thingStatusInfo) {
        checkNotNull(thingUID, "thingUID");
        checkNotNull(thingStatusInfo, "thingStatusInfo");

        String topic = buildTopic(THING_STATUS_INFO_EVENT_TOPIC, thingUID);
        String payload = serializePayload(thingStatusInfo);
        return new ThingStatusInfoEvent(topic, payload, thingUID, thingStatusInfo);
    }

    /**
     * Creates a new thing status info changed event based on a thing UID, a thing status info and the old thing status
     * info object.
     *
     * @param thingUID the thing UID
     * @param thingStatusInfo the thing status info object
     * @param oldThingStatusInfo the old thing status info object
     * @return the created thing status info changed event
     * @throws IllegalArgumentException if thingUID or thingStatusInfo is null
     */
    public static ThingStatusInfoChangedEvent createStatusInfoChangedEvent(ThingUID thingUID,
            ThingStatusInfo thingStatusInfo, ThingStatusInfo oldThingStatusInfo) {
        checkNotNull(thingUID, "thingUID");
        checkNotNull(thingStatusInfo, "thingStatusInfo");
        checkNotNull(oldThingStatusInfo, "oldThingStatusInfo");

        String topic = buildTopic(THING_STATUS_INFO_CHANGED_EVENT_TOPIC, thingUID);
        String payload = serializePayload(new ThingStatusInfo[] { thingStatusInfo, oldThingStatusInfo });
        return new ThingStatusInfoChangedEvent(topic, payload, thingUID, thingStatusInfo, oldThingStatusInfo);
    }

    /**
     * Creates a thing added event.
     *
     * @param thing the thing
     * @return the created thing added event
     * @throws IllegalArgumentException if thing is null
     */
    public static ThingAddedEvent createAddedEvent(Thing thing) {
        assertValidThing(thing);
        String topic = buildTopic(THING_ADDED_EVENT_TOPIC, thing.getUID());
        ThingDTO thingDTO = map(thing);
        String payload = serializePayload(thingDTO);
        return new ThingAddedEvent(topic, payload, thingDTO);
    }

    /**
     * Creates a thing removed event.
     *
     * @param thing the thing
     * @return the created thing removed event
     * @throws IllegalArgumentException if thing is null
     */
    public static ThingRemovedEvent createRemovedEvent(Thing thing) {
        assertValidThing(thing);
        String topic = buildTopic(THING_REMOVED_EVENT_TOPIC, thing.getUID());
        ThingDTO thingDTO = map(thing);
        String payload = serializePayload(thingDTO);
        return new ThingRemovedEvent(topic, payload, thingDTO);
    }

    /**
     * Creates a thing updated event.
     *
     * @param thing the thing
     * @param oldThing the old thing
     * @return the created thing updated event
     * @throws IllegalArgumentException if thing or oldThing is null
     */
    public static ThingUpdatedEvent createUpdateEvent(Thing thing, Thing oldThing) {
        assertValidThing(thing);
        assertValidThing(oldThing);
        String topic = buildTopic(THING_UPDATED_EVENT_TOPIC, thing.getUID());
        ThingDTO thingDTO = map(thing);
        ThingDTO oldThingDTO = map(oldThing);
        List<ThingDTO> thingDTOs = new LinkedList<>();
        thingDTOs.add(thingDTO);
        thingDTOs.add(oldThingDTO);
        String payload = serializePayload(thingDTOs);
        return new ThingUpdatedEvent(topic, payload, thingDTO, oldThingDTO);
    }

    private static void assertValidThing(Thing thing) {
        checkNotNull(thing, "thing");
        checkNotNull(thing.getUID(), "thingUID of the thing");
    }

    private static String buildTopic(String topic, ThingUID thingUID) {
        return topic.replace("{thingUID}", thingUID.getAsString());
    }

    private static String buildTopic(String topic, ChannelUID channelUID) {
        return topic.replace("{channelUID}", channelUID.getAsString());
    }

    private static ThingDTO map(Thing thing) {
        return ThingDTOMapper.map(thing);
    }
}
