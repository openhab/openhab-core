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
package org.eclipse.smarthome.config.discovery.inbox.events;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.dto.DiscoveryResultDTO;
import org.eclipse.smarthome.config.discovery.dto.DiscoveryResultDTOMapper;
import org.eclipse.smarthome.core.events.AbstractEventFactory;
import org.eclipse.smarthome.core.events.Event;
import org.eclipse.smarthome.core.events.EventFactory;
import org.osgi.service.component.annotations.Component;

/**
 * An {@link InboxEventFactory} is responsible for creating inbox event instances.
 *
 * @author Stefan Bußweiler - Initial contribution
 */
@Component(immediate = true, service = EventFactory.class)
public class InboxEventFactory extends AbstractEventFactory {

    static final String INBOX_ADDED_EVENT_TOPIC = "smarthome/inbox/{thingUID}/added";

    static final String INBOX_REMOVED_EVENT_TOPIC = "smarthome/inbox/{thingUID}/removed";

    static final String INBOX_UPDATED_EVENT_TOPIC = "smarthome/inbox/{thingUID}/updated";

    /**
     * Constructs a new InboxEventFactory.
     */
    public InboxEventFactory() {
        super(Stream.of(InboxAddedEvent.TYPE, InboxUpdatedEvent.TYPE, InboxRemovedEvent.TYPE)
                .collect(Collectors.toSet()));
    }

    @Override
    protected Event createEventByType(String eventType, String topic, String payload, String source) throws Exception {
        Event event = null;
        if (eventType.equals(InboxAddedEvent.TYPE)) {
            event = createAddedEvent(topic, payload);
        } else if (eventType.equals(InboxRemovedEvent.TYPE)) {
            event = createRemovedEvent(topic, payload);
        } else if (eventType.equals(InboxUpdatedEvent.TYPE)) {
            event = createUpdatedEvent(topic, payload);
        }
        return event;
    }

    private Event createAddedEvent(String topic, String payload) {
        DiscoveryResultDTO resultDTO = deserializePayload(payload, DiscoveryResultDTO.class);
        return new InboxAddedEvent(topic, payload, resultDTO);
    }

    private Event createRemovedEvent(String topic, String payload) {
        DiscoveryResultDTO resultDTO = deserializePayload(payload, DiscoveryResultDTO.class);
        return new InboxRemovedEvent(topic, payload, resultDTO);
    }

    private Event createUpdatedEvent(String topic, String payload) {
        DiscoveryResultDTO resultDTO = deserializePayload(payload, DiscoveryResultDTO.class);
        return new InboxUpdatedEvent(topic, payload, resultDTO);
    }

    /**
     * Creates an inbox added event.
     *
     * @param discoveryResult the discovery result
     * @return the created inbox added event
     * @throws IllegalArgumentException if discoveryResult is null
     */
    public static InboxAddedEvent createAddedEvent(DiscoveryResult discoveryResult) {
        assertValidArgument(discoveryResult);
        String topic = buildTopic(INBOX_ADDED_EVENT_TOPIC, discoveryResult.getThingUID().getAsString());
        DiscoveryResultDTO resultDTO = map(discoveryResult);
        String payload = serializePayload(resultDTO);
        return new InboxAddedEvent(topic, payload, resultDTO);
    }

    /**
     * Creates an inbox removed event.
     *
     * @param discoveryResult the discovery result
     * @return the created inbox removed event
     * @throws IllegalArgumentException if discoveryResult is null
     */
    public static InboxRemovedEvent createRemovedEvent(DiscoveryResult discoveryResult) {
        assertValidArgument(discoveryResult);
        String topic = buildTopic(INBOX_REMOVED_EVENT_TOPIC, discoveryResult.getThingUID().getAsString());
        DiscoveryResultDTO resultDTO = map(discoveryResult);
        String payload = serializePayload(resultDTO);
        return new InboxRemovedEvent(topic, payload, resultDTO);
    }

    /**
     * Creates an inbox updated event.
     *
     * @param discoveryResult the discovery result
     * @return the created inbox updated event
     * @throws IllegalArgumentException if discoveryResult is null
     */
    public static InboxUpdatedEvent createUpdatedEvent(DiscoveryResult discoveryResult) {
        assertValidArgument(discoveryResult);
        String topic = buildTopic(INBOX_UPDATED_EVENT_TOPIC, discoveryResult.getThingUID().getAsString());
        DiscoveryResultDTO resultDTO = map(discoveryResult);
        String payload = serializePayload(resultDTO);
        return new InboxUpdatedEvent(topic, payload, resultDTO);
    }

    private static void assertValidArgument(DiscoveryResult discoveryResult) {
        checkNotNull(discoveryResult, "discoveryResult");
    }

    private static String buildTopic(String topic, String thingUID) {
        return topic.replace("{thingUID}", thingUID);
    }

    private static DiscoveryResultDTO map(DiscoveryResult discoveryResult) {
        return DiscoveryResultDTOMapper.map(discoveryResult);
    }

}
