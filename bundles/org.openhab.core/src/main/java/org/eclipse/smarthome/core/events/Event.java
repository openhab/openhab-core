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

/**
 * {@link Event} objects are delivered by the {@link EventPublisher} through the Eclipse SmartHome event bus.
 * The callback interface {@link EventSubscriber} can be implemented in order to receive such events.
 * 
 * @author Stefan Bußweiler - Initial contribution
 */
public interface Event {

    /**
     * Gets the event type.
     * 
     * @return the event type
     */
    String getType();

    /**
     * Gets the topic of an event.
     * 
     * @return the event topic
     */
    String getTopic();

    /**
     * Gets the payload as a serialized string.
     * 
     * @return the serialized event
     */
    String getPayload();

    /**
     * Gets the name of the source identifying the sender.
     * 
     * @return the name of the source
     */
    String getSource();

}
