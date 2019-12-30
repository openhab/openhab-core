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
package org.openhab.core.events;

/**
 * The {@link EventPublisher} posts {@link Event}s through the openHAB event bus in an asynchronous way.
 * Posted events can be received by implementing the {@link EventSubscriber} callback interface.
 *
 * @author Stefan Bußweiler - Initial contribution
 */
public interface EventPublisher {

    /**
     * Posts an event through the event bus in an asynchronous way.
     * 
     * @param event the event posted through the event bus
     * @throws IllegalArgumentException if the event is null
     * @throws IllegalArgumentException if one of the event properties type, payload or topic is null
     * @throws IllegalStateException if the underlying event bus module is not available
     */
    void post(Event event) throws IllegalArgumentException, IllegalStateException;
}
