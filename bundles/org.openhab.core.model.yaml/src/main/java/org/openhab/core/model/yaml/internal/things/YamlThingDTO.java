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
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.AbstractUID;
import org.openhab.core.model.yaml.YamlElement;
import org.openhab.core.model.yaml.YamlElementName;
import org.openhab.core.model.yaml.internal.util.YamlElementUtils;

/**
 * The {@link YamlThingDTO} is a data transfer object used to serialize a thing in a YAML configuration file.
 *
 * @author Laurent Garnier - Initial contribution
 */
@YamlElementName("things")
public class YamlThingDTO implements YamlElement, Cloneable {

    private static final Pattern THING_UID_SEGMENT_PATTERN = Pattern.compile("^[a-zA-Z0-9_][a-zA-Z0-9_-]*$");
    private static final Pattern CHANNEL_ID_PATTERN = Pattern
            .compile("^[a-zA-Z0-9_][a-zA-Z0-9_-]*(#[a-zA-Z0-9_][a-zA-Z0-9_-]*)?$");

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
            if (errors != null) {
                errors.add("thing uid is missing");
            }
            return false;
        }
        boolean ok = true;
        // Check that uid has at least 3 segments and each segment respects the expected syntax
        String[] segments = uid.split(AbstractUID.SEPARATOR);
        if (segments.length < 3) {
            if (errors != null) {
                errors.add("thing %s: uid contains insufficient segments".formatted(uid));
            }
            ok = false;
        }
        for (String segment : segments) {
            if (!THING_UID_SEGMENT_PATTERN.matcher(segment).find()) {
                if (errors != null) {
                    errors.add(
                            "thing %s: segement \"%s\" in uid is not matching the expected syntax [a-zA-Z0-9_][a-zA-Z0-9_-]*"
                                    .formatted(uid, segment));
                }
                ok = false;
            }
        }
        if (bridge != null && !bridge.isBlank()) {
            // Check that bridge has at least 3 segments and each segment respects the expected syntax
            segments = bridge.split(AbstractUID.SEPARATOR);
            if (segments.length < 3) {
                if (errors != null) {
                    errors.add("thing %s: bridge \"%s\" contains insufficient segments".formatted(uid, bridge));
                }
                ok = false;
            }
            for (String segment : segments) {
                if (!THING_UID_SEGMENT_PATTERN.matcher(segment).find()) {
                    if (errors != null) {
                        errors.add(
                                "thing %s: segement \"%s\" in bridge is not matching the expected syntax [a-zA-Z0-9_][a-zA-Z0-9_-]*"
                                        .formatted(uid, segment));
                    }
                    ok = false;
                }
            }
        }
        if (channels != null) {
            for (Map.Entry<@NonNull String, @NonNull YamlChannelDTO> entry : channels.entrySet()) {
                String channelId = entry.getKey();
                if (!CHANNEL_ID_PATTERN.matcher(channelId).find()) {
                    if (errors != null) {
                        errors.add(
                                "thing %s: channel id \"%s\" is not matching the expected syntax [a-zA-Z0-9_][a-zA-Z0-9_-]*(#[a-zA-Z0-9_][a-zA-Z0-9_-]*)?"
                                        .formatted(uid, channelId));
                    }
                    ok = false;
                }
                List<String> channelErrors = new ArrayList<>();
                List<String> channelWarnings = new ArrayList<>();
                ok &= entry.getValue().isValid(channelErrors, channelWarnings);
                if (errors != null) {
                    channelErrors.forEach(error -> {
                        errors.add("thing %s channel %s: %s".formatted(uid, channelId, error));
                    });
                }
                if (warnings != null) {
                    channelWarnings.forEach(warning -> {
                        warnings.add("thing %s channel %s: %s".formatted(uid, channelId, warning));
                    });
                }
            }
        }
        return ok;
    }

    public boolean isBridge() {
        return isBridge == null ? false : isBridge.booleanValue();
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid, bridge, label, location);
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
