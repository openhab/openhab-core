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
package org.openhab.core.thing.events;

import org.openhab.core.events.AbstractEvent;
import org.openhab.core.thing.ChannelUID;

/**
 * {@link ChannelTriggeredEvent}s can be used to deliver triggers through the openHAB event bus.
 * Trigger events must be created with the {@link ThingEventFactory}.
 *
 * @author Moritz Kammerer - Initial contribution
 */
public class ChannelTriggeredEvent extends AbstractEvent {

    /**
     * The thing trigger event type.
     */
    public static final String TYPE = ChannelTriggeredEvent.class.getSimpleName();

    /**
     * The channel which triggered the event.
     */
    private final ChannelUID channel;

    /**
     * The event.
     */
    private final String event;

    /**
     * Constructs a new thing trigger event.
     *
     * @param topic the topic. The topic includes the thing UID, see
     *            {@link ThingEventFactory#THING_TRIGGERED_EVENT_TOPIC}
     * @param payload the payload. Contains a serialized {@link ThingEventFactory.TriggerEventPayloadBean}.
     * @param source the source
     * @param channel the channel which triggered the event
     */
    protected ChannelTriggeredEvent(String topic, String payload, String source, String event, ChannelUID channel) {
        super(topic, payload, source);
        this.event = event;
        this.channel = channel;
    }

    /**
     * Returns the event.
     *
     * @return the event
     */
    public String getEvent() {
        return event;
    }

    /**
     * @return the channel which triggered the event
     */
    public ChannelUID getChannel() {
        return channel;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return channel + " triggered " + event;
    }
}
