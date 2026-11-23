/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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

/**
 * There are situations when item states do not have any defined value.
 * This might be because they have not been initialized yet (never
 * received a state update so far) or because their state is ambiguous
 * (e.g. a dimmed light that is treated as a switch (ON/OFF) will have
 * an undefined state if it is dimmed to 50%).
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public enum UnDefType implements PrimitiveType, State {
    /**
     * Indicates that a value exists but cannot be represented as a concrete state. Examples include
     * Infinity and NaN cannot be represented as a DecimalType; a sensor that knows the
     * reading is out of range, and thus cannot know what the exact value is; a rollershutter that
     * can know when it's at either extreme, but not the exact position in between.
     */
    UNDEF,

    /**
     * Indicates the absence of a value, for example when an item has not yet been initialized,
     * has gone offline so we can no longer determine its state, or if a channel is not relevant
     * for a specific device.
     */
    NULL;

    @Override
    public String format(String pattern) {
        return pattern.formatted(this.toString());
    }

    @Override
    public String toString() {
        return toFullString();
    }

    @Override
    public String toFullString() {
        return super.toString();
    }
}
