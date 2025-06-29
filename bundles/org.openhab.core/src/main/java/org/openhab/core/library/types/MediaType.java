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

    public static final String KEY_STATE = "state";
    public static final String KEY_COMMAND = "command";
    public static final String KEY_PARAM = "param";
    public static final String KEY_DEVICE = "device";
    public static final String KEY_BINDING = "binding";

    private final PlayPauseType state;
    private final MediaCommandType command;
    private final String param;
    private final StringType device;
    private final StringType binding;

    public MediaType() {
        this(PlayPauseType.PLAY, MediaCommandType.NONE, "", new StringType(""), new StringType(""));
    }

    public MediaType(PlayPauseType state, MediaCommandType command, @Nullable String param, @Nullable StringType device,
            @Nullable StringType binding) {
        this.state = state;
        this.command = command;
        this.param = param != null ? param : "";
        this.device = device != null ? device : new StringType("");
        this.binding = binding != null ? binding : new StringType("");
    }

    @Override
    public String toString() {
        return toFullString();
    }

    @Override
    public String toFullString() {
        return this.state.toFullString() + "," + this.command.toFullString() + "," + param + "," + device + ","
                + binding;
    }

    public static MediaType valueOf(String value) {
        List<String> constituents = Arrays.stream(value.split(",")).map(String::trim).toList();

        PlayPauseType state = PlayPauseType.valueOf(constituents.get(0));
        MediaCommandType command = MediaCommandType.valueOf(constituents.get(1));
        String param = constituents.get(2);
        StringType device = new StringType(constituents.get(3));
        StringType binding = new StringType(constituents.get(4));

        return new MediaType(state, command, param, device, binding);
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
        map.put(KEY_STATE, getState());
        map.put(KEY_COMMAND, getCommand());
        map.put(KEY_PARAM, getCommand());
        map.put(KEY_DEVICE, getDevice());
        return map;
    }

    public PlayPauseType getState() {
        return state;
    }

    public MediaCommandType getCommand() {
        return command;
    }

    public StringType getParam() {
        return new StringType(param);
    }

    public StringType getDevice() {
        return device;
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
        return Objects.equals(this.param, other.param) && Objects.equals(this.device, other.device)
                && Objects.equals(this.state, other.state) && Objects.equals(this.binding, other.binding);
    }
}
