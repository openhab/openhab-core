/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.config.discovery.inbox.events;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.dto.DiscoveryResultDTOMapper;
import org.openhab.core.config.discovery.internal.DiscoveryResultImpl;
import org.openhab.core.events.Event;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;

import com.google.gson.Gson;

/**
 * {@link InboxEventFactoryTest} tests the {@link InboxEventFactory}.
 *
 * @author Stefan Bußweiler - Initial contribution
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
public class InboxEventFactoryTest {

    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID("binding", "type");
    private static final ThingUID THING_UID = new ThingUID(THING_TYPE_UID, "id");

    private static final DiscoveryResult DISCOVERY_RESULT = new DiscoveryResultImpl(THING_TYPE_UID, THING_UID, null,
            null, null, null, 60);

    private static final String INBOX_ADDED_EVENT_TYPE = InboxAddedEvent.TYPE;

    private static final String INBOX_ADDED_EVENT_TOPIC = InboxEventFactory.INBOX_ADDED_EVENT_TOPIC
            .replace("{thingUID}", THING_UID.getAsString());

    private static final String INBOX_ADDED_EVENT_PAYLOAD = new Gson()
            .toJson(DiscoveryResultDTOMapper.map(DISCOVERY_RESULT));

    private InboxEventFactory factory = new InboxEventFactory();

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
