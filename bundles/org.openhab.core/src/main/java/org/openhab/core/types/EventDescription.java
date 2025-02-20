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

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Describes event options and gives information how to interpret it.
 *
 * @author Moritz Kammerer - Initial contribution
 */
@NonNullByDefault
public class EventDescription {
    private final List<EventOption> options;

    /**
     * Creates an event description object.
     *
     * @param options predefined list of options
     */
    public EventDescription(@Nullable List<EventOption> options) {
        this.options = options != null ? List.copyOf(options) : List.of();
    }

    /**
     * Returns a list of predefined events with their label.
     *
     * @return list of predefined events with their label
     */
    public List<EventOption> getOptions() {
        return options;
    }

    @Override
    public String toString() {
        return "EventDescription [options=%s]".formatted(options);
    }
}
