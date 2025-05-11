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
package org.openhab.core.types;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Describes one possible value an event might have.
 *
 * @author Moritz Kammerer - Initial contribution
 */
@NonNullByDefault
public final class EventOption {

    private final String value;
    private final @Nullable String label;

    /**
     * Creates an {@link EventOption} object.
     *
     * @param value value of the event
     * @param label label
     * @throws IllegalArgumentException if value is null
     */
    public EventOption(@Nullable String value, @Nullable String label) {
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
    public @Nullable String getLabel() {
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
