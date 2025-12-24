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
    private @NonNullByDefault({}) String name;
    private @NonNullByDefault({}) String value;

    /**
     * Creates a new add-on parameter instance.
     *
     * @param name the parameter name, must not be null or blank
     * @param value the parameter value, must not be null or blank
     * @throws IllegalArgumentException if name or value is null or blank
     */
    public AddonParameter(String name, String value) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be null or empty");
        }
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("value must not be null or empty");
        }
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

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
