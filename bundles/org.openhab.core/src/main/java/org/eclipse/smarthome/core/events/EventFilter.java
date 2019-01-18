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
 * An {@link EventFilter} can be provided by an {@link EventSubscriber} in order
 * to receive specific {@link Event}s by an {@link EventPublisher} if the filter applies.
 * 
 * @author Stefan Bußweiler - Initial contribution
 */
public interface EventFilter {

    /**
     * Apply the filter on an event. <p> This method is called for each subscribed {@link Event} of an
     * {@link EventSubscriber}. If the filter applies, the event will be dispatched to the
     * {@link EventSubscriber#receive(Event)} method.
     * 
     * @param event the event (not null)
     * @return true if the filter criterion applies
     */
    boolean apply(Event event);
}
