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
package org.eclipse.smarthome.core.thing.type;

/**
 * Kind of the channel.
 *
 * @author Moritz Kammerer - Initial contribution and API.
 */
public enum ChannelKind {
    /**
     * Channels which have a state.
     */
    STATE,
    /**
     * Channels which can be triggered.
     */
    TRIGGER;

    /**
     * Parses the input string into a {@link ChannelKind}.
     *
     * @param input the input string
     * @return the parsed ChannelKind.
     * @throws IllegalArgumentException if the input couldn't be parsed.
     */
    public static ChannelKind parse(String input) {
        if (input == null) {
            return STATE;
        }

        for (ChannelKind value : values()) {
            if (value.name().equalsIgnoreCase(input)) {
                return value;
            }
        }

        throw new IllegalArgumentException("Unknown channel kind: '" + input + "'");
    }
}
