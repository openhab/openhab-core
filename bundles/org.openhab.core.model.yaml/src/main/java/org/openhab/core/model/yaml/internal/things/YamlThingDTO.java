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
package org.openhab.core.model.yaml.internal.things;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.model.yaml.YamlElement;
import org.openhab.core.model.yaml.YamlElementName;
import org.openhab.core.model.yaml.internal.util.YamlElementUtils;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingUID;

/**
 * The {@link YamlThingDTO} is a data transfer object used to serialize a thing in a YAML configuration file.
 *
 * @author Laurent Garnier - Initial contribution
 */
@YamlElementName("things")
public class YamlThingDTO implements YamlElement, Cloneable {

    public String uid;
    public Boolean isBridge;
    public String bridge;
    public String label;
    public String location;
    public Map<@NonNull String, @NonNull Object> config;
    public Map<@NonNull String, @NonNull YamlChannelDTO> channels;

    public YamlThingDTO() {
    }

    @Override
    public @NonNull String getId() {
        return uid == null ? "" : uid;
    }

    @Override
    public void setId(@NonNull String id) {
        uid = id;
    }

    @Override
    public YamlElement cloneWithoutId() {
        YamlThingDTO copy;
        try {
            copy = (YamlThingDTO) super.clone();
            copy.uid = null;
            return copy;
        } catch (CloneNotSupportedException e) {
            // Will never happen
            return new YamlThingDTO();
        }
    }

    @Override
    public boolean isValid(@Nullable List<@NonNull String> errors, @Nullable List<@NonNull String> warnings) {
        // Check that uid is present
        if (uid == null || uid.isBlank()) {
            addToList(errors, "invalid thing: uid is missing while mandatory");
            return false;
        }
        boolean ok = true;
        ThingUID thingUID;
        try {
            thingUID = new ThingUID(uid);
        } catch (IllegalArgumentException e) {
            thingUID = new ThingUID("dummy:dummy:dummy");
            addToList(errors, "invalid thing \"%s\": %s".formatted(uid, e.getMessage()));
            ok = false;
        }
        if (bridge != null && !bridge.isBlank()) {
            try {
                new ThingUID(bridge);
            } catch (IllegalArgumentException e) {
                addToList(errors, "invalid thing \"%s\": invalid value \"%s\" for \"bridge\" field: %s".formatted(uid,
                        bridge, e.getMessage()));
                ok = false;
            }
        }
        if (channels != null) {
            for (Map.Entry<@NonNull String, @NonNull YamlChannelDTO> entry : channels.entrySet()) {
                String channelId = entry.getKey();
                String[] splittedChannelId = channelId.split(ChannelUID.CHANNEL_GROUP_SEPARATOR, 2);
                try {
                    if (splittedChannelId.length == 1) {
                        new ChannelUID(thingUID, channelId);
                    } else {
                        new ChannelUID(thingUID, splittedChannelId[0], splittedChannelId[1]);
                    }
                } catch (IllegalArgumentException e) {
                    addToList(errors, "invalid thing \"%s\": invalid channel id \"%s\": %s".formatted(uid, channelId,
                            e.getMessage()));
                    ok = false;
                }
                List<String> channelErrors = new ArrayList<>();
                List<String> channelWarnings = new ArrayList<>();
                ok &= entry.getValue().isValid(channelErrors, channelWarnings);
                channelErrors.forEach(error -> {
                    addToList(errors, "invalid thing \"%s\": channel \"%s\": %s".formatted(uid, channelId, error));
                });
                channelWarnings.forEach(warning -> {
                    addToList(warnings, "thing \"%s\": channel \"%s\": %s".formatted(uid, channelId, warning));
                });
            }
        }
        return ok;
    }

    private void addToList(@Nullable List<@NonNull String> list, String value) {
        if (list != null) {
            list.add(value);
        }
    }

    public boolean isBridge() {
        return isBridge == null ? false : isBridge.booleanValue();
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid, isBridge(), bridge, label, location);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        YamlThingDTO other = (YamlThingDTO) obj;
        return Objects.equals(uid, other.uid) && isBridge() == other.isBridge() && Objects.equals(bridge, other.bridge)
                && Objects.equals(label, other.label) && Objects.equals(location, other.location)
                && YamlElementUtils.equalsConfig(config, other.config) && equalsChannels(channels, other.channels);
    }

    private boolean equalsChannels(@Nullable Map<@NonNull String, @NonNull YamlChannelDTO> first,
            @Nullable Map<@NonNull String, @NonNull YamlChannelDTO> second) {
        if (first != null && second != null) {
            if (first.size() != second.size()) {
                return false;
            } else {
                return first.entrySet().stream().allMatch(e -> e.getValue().equals(second.get(e.getKey())));
            }
        } else {
            return first == null && second == null;
        }
    }
}
