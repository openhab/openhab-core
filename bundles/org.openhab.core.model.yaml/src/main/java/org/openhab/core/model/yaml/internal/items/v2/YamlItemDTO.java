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
package org.openhab.core.model.yaml.internal.items.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.AbstractUID;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.ItemUtil;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.model.yaml.YamlElement;
import org.openhab.core.model.yaml.YamlElementName;
import org.openhab.core.model.yaml.internal.items.YamlGroupDTO;
import org.openhab.core.model.yaml.internal.util.YamlElementUtils;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.profiles.ProfileTypeUID;

/**
 * The {@link YamlItemDTO} is a data transfer object used to serialize an item in a YAML configuration file.
 *
 * @author Laurent Garnier - Initial contribution
 * @author Jimmy Tanagra – Added Model V2 support including short‑form metadata syntax
 */
@YamlElementName("items")
public class YamlItemDTO implements YamlElement, Cloneable {

    private static final Pattern ICON_SEGMENT_PATTERN = Pattern.compile("[a-zA-Z0-9_][a-zA-Z0-9_-]*");

    public String name;
    public String type;
    public String dimension;
    public YamlGroupDTO group;
    public String label;
    public String icon;
    public String format;
    public String unit;
    public Boolean autoupdate;
    public List<@NonNull String> groups;
    public Set<@NonNull String> tags;
    public String channel;
    public Map<@NonNull String, @NonNull Map<@NonNull String, @NonNull Object>> channels;
    public Map<@NonNull String, @NonNull YamlMetadataDTO> metadata;

    public YamlItemDTO() {
    }

    @Override
    public @NonNull String getId() {
        return name == null ? "" : name;
    }

    @Override
    public void setId(@NonNull String id) {
        name = id;
    }

    @Override
    public YamlElement cloneWithoutId() {
        YamlItemDTO copy;
        try {
            copy = (YamlItemDTO) super.clone();
            copy.name = null;
            return copy;
        } catch (CloneNotSupportedException e) {
            // Will never happen
            return new YamlItemDTO();
        }
    }

    @Override
    public boolean isValid(@Nullable List<@NonNull String> errors, @Nullable List<@NonNull String> warnings) {
        // Check that name is present
        if (name == null || name.isBlank()) {
            addToList(errors, "invalid item: name missing while mandatory");
            return false;
        }
        boolean ok = true;
        if (!ItemUtil.isValidItemName(name)) {
            addToList(errors,
                    "invalid item \"%s\": \"name\" must begin with a letter or underscore followed by alphanumeric characters and underscores, and must not contain any other symbols."
                            .formatted(name));
            ok = false;
        }
        List<String> subErrors = new ArrayList<>();
        List<String> subWarnings = new ArrayList<>();
        if (type == null || type.isBlank()) {
            addToList(errors, "invalid item \"%s\": \"type\" field missing while mandatory".formatted(name));
            ok = false;
        } else if (GroupItem.TYPE.equalsIgnoreCase(type)) {
            if (dimension != null) {
                addToList(warnings, "item \"%s\": \"dimension\" field ignored as type is Group".formatted(name));
            }
            if (group != null) {
                ok &= group.isValid(subErrors, subWarnings);
                subErrors.forEach(error -> {
                    addToList(errors, "invalid item \"%s\": %s".formatted(name, error));
                });
                subWarnings.forEach(warning -> {
                    addToList(warnings, "item \"%s\": %s".formatted(name, warning));
                });
            }
        } else {
            if (group != null) {
                addToList(warnings, "item \"%s\": \"group\" field ignored as type is not Group".formatted(name));
            }
            if (!YamlElementUtils.isValidItemType(type)) {
                addToList(errors, "invalid item \"%s\": invalid value \"%s\" for \"type\" field".formatted(name, type));
                ok = false;
            } else if (YamlElementUtils.isNumberItemType(type)) {
                if (!YamlElementUtils.isValidItemDimension(dimension)) {
                    addToList(errors, "invalid item \"%s\": invalid value \"%s\" for \"dimension\" field"
                            .formatted(name, dimension));
                    ok = false;
                }
            } else if (dimension != null) {
                addToList(warnings,
                        "item \"%s\": \"dimension\" field ignored as type is not Number".formatted(name, dimension));
            }
        }
        if (icon != null) {
            subErrors.clear();
            ok &= isValidIcon(icon, subErrors);
            subErrors.forEach(error -> {
                addToList(errors, "invalid item \"%s\": %s".formatted(name, error));
            });
        }
        if (groups != null) {
            for (String gr : groups) {
                if (!ItemUtil.isValidItemName(gr)) {
                    addToList(errors,
                            "invalid item \"%s\": value \"%s\" in \"groups\" field must begin with a letter or underscore followed by alphanumeric characters and underscores, and must not contain any other symbols."
                                    .formatted(name, gr));
                    ok = false;
                }
            }
        }
        if (channel != null) {
            subErrors.clear();
            ok &= isValidChannel(channel, null, subErrors);
            subErrors.forEach(error -> {
                addToList(errors, "invalid item \"%s\": %s".formatted(name, error));
            });
        }
        if (channels != null) {
            for (String ch : channels.keySet()) {
                subErrors.clear();
                ok &= isValidChannel(ch, channels.get(ch), subErrors);
                subErrors.forEach(error -> {
                    addToList(errors, "invalid item \"%s\": %s".formatted(name, error));
                });
            }
        }
        if (metadata != null) {
            for (String namespace : metadata.keySet()) {
                try {
                    new MetadataKey(namespace, name);
                } catch (IllegalArgumentException e) {
                    addToList(errors, "invalid item \"%s\": invalid metadata key (\"%s\", \"%s\"): %s".formatted(name,
                            namespace, name, e.getMessage()));
                    ok = false;
                }
            }
            YamlMetadataDTO md = metadata.get("autoupdate");
            if (md != null && autoupdate != null) {
                addToList(warnings,
                        "item \"%s\": \"autoupdate\" field is redundant with \"autoupdate\" metadata; value \"%s\" will be considered"
                                .formatted(name, md.getValue()));
            }
            md = metadata.get("unit");
            if (md != null && unit != null) {
                addToList(warnings,
                        "item \"%s\": \"unit\" field is redundant with \"unit\" metadata; value \"%s\" will be considered"
                                .formatted(name, md.getValue()));
            }
            md = metadata.get("stateDescription");
            Map<@NonNull String, @NonNull Object> mdConfig = md == null ? null : md.config;
            Object pattern = mdConfig == null ? null : mdConfig.get("pattern");
            if (pattern != null && format != null) {
                addToList(warnings,
                        "item \"%s\": \"format\" field is redundant with pattern in \"stateDescription\" metadata; \"%s\" will be considered"
                                .formatted(name, pattern));
            }
        }
        return ok;
    }

    private boolean isValidIcon(String icon, List<@NonNull String> errors) {
        boolean ok = true;
        String[] segments = icon.split(AbstractUID.SEPARATOR);
        int nb = segments.length;
        if (nb > 3) {
            errors.add("too many segments in value \"%s\" for \"icon\" field; maximum 3 is expected".formatted(icon));
            ok = false;
            nb = 3;
        }
        for (int i = 0; i < nb; i++) {
            String segment = segments[i];
            if (!ICON_SEGMENT_PATTERN.matcher(segment).matches()) {
                errors.add("segment \"%s\" in \"icon\" field not matching the expected syntax %s".formatted(segment,
                        ICON_SEGMENT_PATTERN.pattern()));
                ok = false;
            }
        }
        return ok;
    }

    private boolean isValidChannel(String channelUID, @Nullable Map<@NonNull String, @NonNull Object> configuration,
            List<@NonNull String> errors) {
        boolean ok = true;
        try {
            new ChannelUID(channelUID);
        } catch (IllegalArgumentException e) {
            errors.add("invalid channel UID \"%s\": %s".formatted(channelUID, e.getMessage()));
            ok = false;
        }
        if (configuration != null && configuration.containsKey("profile")
                && configuration.get("profile") instanceof String profile) {
            String[] splittedProfile = profile.split(AbstractUID.SEPARATOR, 2);
            try {
                if (splittedProfile.length == 1) {
                    new ProfileTypeUID(ProfileTypeUID.SYSTEM_SCOPE, profile);
                } else {
                    new ProfileTypeUID(splittedProfile[0], splittedProfile[1]);
                }
            } catch (IllegalArgumentException e) {
                errors.add("invalid value \"%s\" for \"profile\" parameter of channel \"%s\": %s".formatted(profile,
                        channelUID, e.getMessage()));
                ok = false;
            }
        }
        return ok;
    }

    private void addToList(@Nullable List<@NonNull String> list, String value) {
        if (list != null) {
            list.add(value);
        }
    }

    public @Nullable String getType() {
        return YamlElementUtils.getItemTypeWithDimension(type, dimension);
    }

    public @NonNull List<@NonNull String> getGroups() {
        return groups == null ? List.of() : groups;
    }

    public @NonNull Set<@NonNull String> getTags() {
        return tags == null ? Set.of() : tags;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, getType(), group, label, icon, format, unit, autoupdate, getGroups(), getTags(),
                channel);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        YamlItemDTO other = (YamlItemDTO) obj;
        return Objects.equals(name, other.name) && Objects.equals(getType(), other.getType())
                && Objects.equals(group, other.group) && Objects.equals(label, other.label)
                && Objects.equals(icon, other.icon) && Objects.equals(format, other.format)
                && Objects.equals(unit, other.unit) && Objects.equals(autoupdate, other.autoupdate)
                && Objects.equals(getGroups(), other.getGroups()) && Objects.equals(getTags(), other.getTags())
                && Objects.equals(channel, other.channel) && equalsChannels(channels, other.channels)
                && equalsMetadata(metadata, other.metadata);
    }

    private boolean equalsChannels(@Nullable Map<@NonNull String, @NonNull Map<@NonNull String, @NonNull Object>> first,
            @Nullable Map<@NonNull String, @NonNull Map<@NonNull String, @NonNull Object>> second) {
        if (first != null && second != null) {
            if (first.size() != second.size()) {
                return false;
            } else {
                return first.entrySet().stream()
                        .allMatch(e -> YamlElementUtils.equalsConfig(e.getValue(), second.get(e.getKey())));
            }
        } else {
            return first == null && second == null;
        }
    }

    private boolean equalsMetadata(@Nullable Map<@NonNull String, @NonNull YamlMetadataDTO> first,
            @Nullable Map<@NonNull String, @NonNull YamlMetadataDTO> second) {
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
