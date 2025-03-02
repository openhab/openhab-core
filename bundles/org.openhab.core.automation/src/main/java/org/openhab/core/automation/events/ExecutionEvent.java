/*
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
import org.openhab.core.events.AbstractEvent;

/**
 * An {@link ExecutionEvent} is only used to notify rules when a script or the REST API trigger the run.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class ExecutionEvent extends AbstractEvent {

    public static final String TYPE = ExecutionEvent.class.getSimpleName();

    /**
     * Constructs a new rule execution event
     *
     * @param topic the topic of the event
     * @param payload the payload of the event
     * @param source the source of the event
     */
    public ExecutionEvent(String topic, String payload, String source) {
        super(topic, payload, source);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return "Execution triggered by " + getSource();
    }
}
