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
package org.openhab.core.library.types;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.Command;
import org.openhab.core.types.PrimitiveType;
import org.openhab.core.types.State;

/**
 * This type is used by the {@link org.openhab.core.library.items.PlayerItem}.
 *
 * @author Laurent Arnal - Initial contribution
 */
@NonNullByDefault
public class MediaType implements PrimitiveType, State, Command {

    private final String value;

    public MediaType() {
        this("");
    }

    public MediaType(@Nullable String value) {
        this.value = value != null ? value : "";
    }

    @Override
    public String toString() {
        return toFullString();
    }

    @Override
    public String toFullString() {
        return value;
    }

    public static StringType valueOf(@Nullable String value) {
        return new StringType(value);
    }

    @Override
    public String format(String pattern) {
        return String.format(pattern, value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof String) {
            return obj.equals(value);
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MediaType other = (MediaType) obj;
        return Objects.equals(this.value, other.value);
    }
}
