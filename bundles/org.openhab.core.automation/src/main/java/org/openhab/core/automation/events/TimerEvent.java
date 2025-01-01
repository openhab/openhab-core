/**
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.automation.events;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.AbstractEvent;

/**
 * An {@link TimerEvent} is only used to notify rules when timer triggers fire.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class TimerEvent extends AbstractEvent {

    public static final String TYPE = TimerEvent.class.getSimpleName();

    /**
     * Constructs a new timer event
     *
     * @param topic the topic of the event
     * @param payload the payload of the event (contains trigger configuration)
     * @param source the source of the event
     */
    public TimerEvent(String topic, String payload, @Nullable String source) {
        super(topic, payload, source);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return "Timer " + getSource() + " triggered.";
    }
}
