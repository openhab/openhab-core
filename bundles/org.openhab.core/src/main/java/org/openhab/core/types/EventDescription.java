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
package org.openhab.core.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Describes event options and gives information how to interpret it.
 *
 * @author Moritz Kammerer - Initial contribution
 */
public class EventDescription {
    private final List<EventOption> options;

    /**
     * Creates an event description object.
     *
     * @param options predefined list of options
     */
    public EventDescription(List<EventOption> options) {
        if (options != null) {
            this.options = Collections.unmodifiableList(new ArrayList<>(options));
        } else {
            this.options = Collections.unmodifiableList(new ArrayList<>(0));
        }
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
        return "EventDescription [options=" + options + "]";
    }
}
