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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.Command;
import org.openhab.core.types.ComplexType;
import org.openhab.core.types.PrimitiveType;
import org.openhab.core.types.State;

/**
 * This type is used by the {@link org.openhab.core.library.items.PlayerItem}.
 *
 * @author Laurent Arnal - Initial contribution
 */
@NonNullByDefault
public class MediaType implements ComplexType, State, Command {

    public static final String KEY_COMMAND = "command";
    public static final String KEY_PARAM = "param";

    private final MediaCommandType command;
    private final String param;

    public MediaType() {
        this(MediaCommandType.NONE, "");
    }

    public MediaType(MediaCommandType command, @Nullable String param) {
        this.command = command;
        this.param = param != null ? param : "";
    }

    @Override
    public String toString() {
        return toFullString();
    }

    @Override
    public String toFullString() {
        return this.command.toFullString() + "," + param;
    }

    public static MediaType valueOf(String value) {
        List<String> constituents = Arrays.stream(value.split(",")).map(String::trim).toList();
        if (constituents.size() == 2) {
            String commandSt = constituents.getFirst();
            MediaCommandType command = MediaCommandType.valueOf(commandSt);
            return new MediaType(command, constituents.get(1));
        } else {
            throw new IllegalArgumentException(value + " is not a valid HSBType syntax");
        }
    }

    @Override
    public String format(String pattern) {
        return String.format(pattern, param);
    }

    @Override
    public int hashCode() {
        return param.hashCode();
    }

    @Override
    public SortedMap<String, PrimitiveType> getConstituents() {
        TreeMap<String, PrimitiveType> map = new TreeMap<>();
        map.put(KEY_COMMAND, getCommand());
        map.put(KEY_PARAM, getCommand());
        return map;
    }

    public MediaCommandType getCommand() {
        return command;
    }

    public StringType getParam() {
        return new StringType(param);
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
            return obj.equals(param);
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MediaType other = (MediaType) obj;
        return Objects.equals(this.param, other.param);
    }
}
