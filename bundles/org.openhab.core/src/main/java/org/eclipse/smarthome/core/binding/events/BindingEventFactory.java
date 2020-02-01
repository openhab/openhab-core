/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.core.binding.events;

import java.util.Set;

import org.eclipse.smarthome.core.events.AbstractEventFactory;
import org.eclipse.smarthome.core.events.Event;
import org.eclipse.smarthome.core.events.EventFactory;
import org.osgi.service.component.annotations.Component;

import jersey.repackaged.com.google.common.collect.Sets;

/**
 * Event factory to create binding level user event notifications
 *
 * @author Chris Jackson - Initial Contribution
 *
 */
@Component(service = EventFactory.class, immediate = true)
public class BindingEventFactory extends AbstractEventFactory {
    private static final String BINDING_EVENT_TOPIC = "smarthome/binding/{binding}/{entity}/{event}";

    public BindingEventFactory(Set<String> supportedEventTypes) {
        super(supportedEventTypes);
    }

    public BindingEventFactory() {
        super(Sets.newHashSet(BindingEvent.TYPE));
    }

    @Override
    protected Event createEventByType(String eventType, String topic, String payload, String source) throws Exception {
        if (eventType.equals(BindingEvent.TYPE)) {
            return new BindingEvent(topic, source, payload);
        }
        throw new IllegalArgumentException(
                eventType + " not supported by " + BindingEventFactory.class.getSimpleName());
    }

    /**
     * Creates a binding event. The event is to be sent on the event bus to advise users of binding status or other
     * information required within the binding.
     *
     * @param binding a {@link String} with the binding name
     * @param entity a {@link String} with binding entity information
     * @param event a {@link String} with the event name
     * @param dto a {@link BindingEventDTO} with the event information to be sent to the user application
     * @return the {@link BindingEvent} to be sent
     */
    public static BindingEvent createBindingEvent(String binding, String entity, String event, BindingEventDTO dto) {
        String topic = BINDING_EVENT_TOPIC;
        topic = topic.replace("{binding}", binding);
        topic = topic.replace("{entity}", entity);
        topic = topic.replace("{event}", event);

        String payload = serializePayload(dto);
        return new BindingEvent(topic, binding, payload);
    }
}
