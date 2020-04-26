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
package org.openhab.core.library.types;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.Command;
import org.openhab.core.types.PrimitiveType;
import org.openhab.core.types.State;

/**
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public enum OnOffType implements PrimitiveType, State, Command {
    ON,
    OFF;

    /**
     * Converts a String value "ON" or "1" to {@link OnOffType#ON} or else to {@link OnOffType#OFF}.
     *
     * @param state String to convert to {@link OnOffType}
     * @return returns the ON or OFF state based on the String
     */
    public static OnOffType from(String state) {
        return from("ON".equalsIgnoreCase(state) || "1".equalsIgnoreCase(state));
    }

    /**
     * @param state boolean to convert to {@link OnOffType}
     * @return returns the ON or OFF state based on the boolean
     */
    public static OnOffType from(boolean state) {
        return state ? ON : OFF;
    }

    @Override
    public String format(String pattern) {
        return String.format(pattern, this.toString());
    }

    @Override
    public String toString() {
        return toFullString();
    }

    @Override
    public String toFullString() {
        return super.toString();
    }

    @Override
    public <T extends State> @Nullable T as(@Nullable Class<T> target) {
        if (target == DecimalType.class) {
            return target.cast(this == ON ? new DecimalType(1) : DecimalType.ZERO);
        } else if (target == PercentType.class) {
            return target.cast(this == ON ? PercentType.HUNDRED : PercentType.ZERO);
        } else if (target == HSBType.class) {
            return target.cast(this == ON ? HSBType.WHITE : HSBType.BLACK);
        } else {
            return State.super.as(target);
        }
    }
}
