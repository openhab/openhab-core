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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.dto.DiscoveryResultDTOMapper;
import org.eclipse.smarthome.config.discovery.internal.DiscoveryResultImpl;
import org.eclipse.smarthome.core.events.Event;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.junit.Test;

import com.google.gson.Gson;

/**
 * {@link InboxEventFactoryTests} tests the {@link InboxEventFactory}.
 *
 * @author Stefan Bußweiler - Initial contribution
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
public class InboxEventFactoryTest {
    InboxEventFactory factory = new InboxEventFactory();

    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID("binding", "type");
    private static final ThingUID THING_UID = new ThingUID(THING_TYPE_UID, "id");

    private static final DiscoveryResult DISCOVERY_RESULT = new DiscoveryResultImpl(THING_TYPE_UID, THING_UID, null,
            null, null, null, 60);

    private static final String INBOX_ADDED_EVENT_TYPE = InboxAddedEvent.TYPE;

    private static final String INBOX_ADDED_EVENT_TOPIC = InboxEventFactory.INBOX_ADDED_EVENT_TOPIC
            .replace("{thingUID}", THING_UID.getAsString());

    private static final String INBOX_ADDED_EVENT_PAYLOAD = new Gson()
            .toJson(DiscoveryResultDTOMapper.map(DISCOVERY_RESULT));

    @Test
    public void inboxEventFactoryCreatesEventAsInboxAddedEventCorrectly() throws Exception {
        Event event = factory.createEvent(INBOX_ADDED_EVENT_TYPE, INBOX_ADDED_EVENT_TOPIC, INBOX_ADDED_EVENT_PAYLOAD,
                null);

        assertThat(event, is(instanceOf(InboxAddedEvent.class)));
        InboxAddedEvent inboxAddedEvent = (InboxAddedEvent) event;
        assertThat(inboxAddedEvent.getType(), is(INBOX_ADDED_EVENT_TYPE));
        assertThat(inboxAddedEvent.getTopic(), is(INBOX_ADDED_EVENT_TOPIC));
        assertThat(inboxAddedEvent.getPayload(), is(INBOX_ADDED_EVENT_PAYLOAD));
        assertThat(inboxAddedEvent.getDiscoveryResult(), not(nullValue()));
        assertThat(inboxAddedEvent.getDiscoveryResult().thingUID, is(THING_UID.getAsString()));
    }

    @Test
    public void inboxEventFactoryCreatesInboxAddedEventCorrectly() {
        InboxAddedEvent event = InboxEventFactory.createAddedEvent(DISCOVERY_RESULT);

        assertThat(event.getType(), is(INBOX_ADDED_EVENT_TYPE));
        assertThat(event.getTopic(), is(INBOX_ADDED_EVENT_TOPIC));
        assertThat(event.getPayload(), is(INBOX_ADDED_EVENT_PAYLOAD));
        assertThat(event.getDiscoveryResult(), not(nullValue()));
        assertThat(event.getDiscoveryResult().thingUID, is(THING_UID.getAsString()));
    }
}
