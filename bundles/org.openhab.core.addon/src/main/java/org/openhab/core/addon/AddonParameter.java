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
package org.openhab.core.addon;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * DTO for serialization of a add-on discovery parameter.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public class AddonParameter {
    private final String name;
    private final String value;

    /**
     * Creates a new add-on parameter instance.
     *
     * @param name the parameter name, must not be null or blank
     * @param value the parameter value, must not be null or blank
     * @throws IllegalArgumentException if name or value is null or blank
     */
    public AddonParameter(String name, String value) {
        this.name = validateAndNormalize("AddonParameter.name", name);
        this.value = validateAndNormalize("AddonParameter.value", value);
    }

    /**
     * Validates that a string field is not null or blank, and normalizes it by trimming whitespace.
     *
     * @param fieldName the name of the field for error messages (e.g., "AddonParameter.name")
     * @param value the value to validate and normalize
     * @return the trimmed value
     * @throws IllegalArgumentException if value is null or blank
     */
    private static String validateAndNormalize(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be null or blank");
        }
        return value.trim();
    }

    /**
     * Gets the add-on parameter name.
     *
     * @return the add-on parameter name, never null or blank
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the add-on parameter value.
     *
     * @return the add-on parameter value, never null or blank
     */
    public String getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
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
        AddonParameter other = (AddonParameter) obj;
        return Objects.equals(name, other.name) && Objects.equals(value, other.value);
    }

    @Override
    public String toString() {
        return "AddonParameter [name=" + name + ", value=" + value + "]";
    }
}
