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
package org.eclipse.smarthome.core.types;

/**
 * Describes one possible value an event might have.
 *
 * @author Moritz Kammerer - Initial contribution and API
 */
public final class EventOption {

    private String value;
    private String label;

    /**
     * Creates a {@link EventOption} object.
     *
     * @param value value of the event
     * @param label label
     * @throws IllegalArgumentException if value is null
     */
    public EventOption(String value, String label) {
        if (value == null) {
            throw new IllegalArgumentException("Value must not be null.");
        }
        this.value = value;
        this.label = label;
    }

    /**
     * Returns the label (can be null).
     *
     * @return label (can be null)
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the value (can not be null).
     *
     * @return value (can not be null)
     */
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "EventOption [value=" + value + ", label=" + label + "]";
    }
}
