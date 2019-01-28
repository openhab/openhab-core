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
package org.eclipse.smarthome.core.events;

import java.util.Set;

/**
 * An {@link EventFactory} is responsible for creating {@link Event} instances of specific event types. The Eclipse
 * SmartHome framework uses Event Factories in order to create new Events (
 * {@link #createEvent(String, String, String, String)}) based on the event type, the topic, the payload and the source
 * if an event type is supported ( {@link #getSupportedEventTypes()}).
 * 
 * @author Stefan Bußweiler - Initial contribution
 */
public interface EventFactory {

    /**
     * Create a new event instance of a specific event type.
     * 
     * @param eventType the event type
     * @param topic the topic
     * @param payload the payload
     * @param source the source (can be null)
     * @return the created event instance (not null)
     * @throws IllegalArgumentException if eventType, topic or payload is null or empty
     * @throws IllegalArgumentException if the eventType is not supported
     * @throws Exception if the creation of the event has failed
     */
    Event createEvent(String eventType, String topic, String payload, String source) throws Exception;

    /**
     * Returns a list of all supported event types of this factory.
     * 
     * @return the supported event types (not null)
     */
    Set<String> getSupportedEventTypes();

}
