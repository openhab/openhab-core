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

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.AbstractEventFactory.ZonedDateTimeAdapter;
import org.openhab.core.types.Command;
import org.openhab.core.types.ComplexType;
import org.openhab.core.types.PrimitiveType;
import org.openhab.core.types.State;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * This type is used by the {@link org.openhab.core.library.items.PlayerItem}.
 *
 * @author Laurent Arnal - Initial contribution
 */
@NonNullByDefault
public class MediaCommandType implements ComplexType, State, Command {

    public static final String KEY_COMMAND = "command";
    public static final String KEY_PARAM = "param";
    public static final String KEY_DEVICE = "device";
    public static final String KEY_BINDING = "binding";

    private final MediaCommandEnumType command;
    private final String param;
    private final StringType device;
    private final StringType binding;

    private static final Gson JSONCONVERTER = new GsonBuilder()
            .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeAdapter()).create();

    public MediaCommandType() {
        this(MediaCommandEnumType.NONE, "", new StringType(""), new StringType(""));

    }

    public MediaCommandType(MediaCommandEnumType command, @Nullable String param, @Nullable StringType device,
            @Nullable StringType binding) {
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
        return JSONCONVERTER.toJson(this);
        // return this.state.toFullString() + "," + this.command.toFullString() + "," + param + "," + device + ","
        // + binding;
    }

    public static MediaCommandType valueOf(String value) {
        try {
            MediaCommandType res = JSONCONVERTER.fromJson(value, MediaCommandType.class);
            if (res == null) {
                return new MediaCommandType();
            }
            return res;
        } catch (Exception ex) {
            throw ex;
        }
    }

    @Override
    public String format(String pattern) {
        return String.format(pattern, param);
    }

    @Override
    public int hashCode() {
        return Objects.hash(command, param, device, binding);
    }

    @Override
    public SortedMap<String, PrimitiveType> getConstituents() {
        TreeMap<String, PrimitiveType> map = new TreeMap<>();
        map.put(KEY_COMMAND, getCommand());
        map.put(KEY_PARAM, getCommand());
        map.put(KEY_DEVICE, getDevice());
        map.put(KEY_BINDING, getBinding());
        return map;
    }

    public MediaCommandEnumType getCommand() {
        return command;
    }

    public StringType getParam() {
        return new StringType(param);
    }

    public StringType getDevice() {
        return device;
    }

    public StringType getBinding() {
        return binding;
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
        MediaCommandType other = (MediaCommandType) obj;
        return Objects.equals(this.device, other.device) && Objects.equals(this.param, other.param)
                && Objects.equals(this.device, other.device) && Objects.equals(this.binding, other.binding);
    }
}
