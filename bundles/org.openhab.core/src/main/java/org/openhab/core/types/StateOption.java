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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Describes one possible value an item might have.
 *
 * @author Dennis Nobel - Initial contribution
 */
@NonNullByDefault
public final class StateOption {

    private String value;
    private @Nullable String label;

    /**
     * Creates a {@link StateOption} object.
     *
     * @param value the value of the item
     * @param label label
     */
    public StateOption(String value, @Nullable String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * Returns the label.
     *
     * @return label
     */
    public @Nullable String getLabel() {
        return label;
    }

    /**
     * Returns the value.
     *
     * @return value
     */
    public String getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + value.hashCode();
        result = prime * result + (label != null ? label.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        StateOption other = (StateOption) obj;
        return value.equals(other.value) && (label != null ? label.equals(other.label) : other.label == null);
    }

    @Override
    public String toString() {
        return "StateOption [value=" + value + ", label=" + label + "]";
    }
}
