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
package org.openhab.core.model.yaml.internal.items;

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
import org.openhab.core.model.yaml.YamlElement;
import org.openhab.core.model.yaml.YamlElementName;
import org.openhab.core.model.yaml.internal.util.YamlElementUtils;

/**
 * The {@link YamlItemDTO} is a data transfer object used to serialize an item in a YAML configuration file.
 *
 * @author Laurent Garnier - Initial contribution
 */
@YamlElementName("items")
public class YamlItemDTO implements YamlElement, Cloneable {

    private static final Pattern ID_PATTERN = Pattern.compile("[a-zA-Z0-9_][a-zA-Z0-9_-]*");
    private static final Pattern CHANNEL_ID_PATTERN = Pattern
            .compile("[a-zA-Z0-9_][a-zA-Z0-9_-]*(#[a-zA-Z0-9_][a-zA-Z0-9_-]*)?");

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
        if (!ID_PATTERN.matcher(name).matches()) {
            addToList(errors, "invalid item: name \"%s\" not matching the expected syntax %s".formatted(name,
                    ID_PATTERN.pattern()));
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
                if (!ID_PATTERN.matcher(gr).matches()) {
                    addToList(errors,
                            "invalid item \"%s\": value \"%s\" for group name not matching the expected syntax %s"
                                    .formatted(name, gr, ID_PATTERN.pattern()));
                    ok = false;
                }
            }
        }
        if (channel != null) {
            subErrors.clear();
            ok &= isValidChannel(channel, subErrors);
            subErrors.forEach(error -> {
                addToList(errors, "invalid item \"%s\": %s".formatted(name, error));
            });
        }
        if (channels != null) {
            for (String ch : channels.keySet()) {
                subErrors.clear();
                ok &= isValidChannel(ch, subErrors);
                subErrors.forEach(error -> {
                    addToList(errors, "invalid item \"%s\": %s".formatted(name, error));
                });
            }
        }
        if (metadata != null) {
            for (String namespace : metadata.keySet()) {
                if (!ID_PATTERN.matcher(namespace).matches()) {
                    addToList(errors, "invalid item \"%s\": metadata \"%s\" not matching the expected syntax %s"
                            .formatted(name, namespace, ID_PATTERN.pattern()));
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
            if (!ID_PATTERN.matcher(segment).matches()) {
                errors.add("segment \"%s\" in \"icon\" field not matching the expected syntax %s".formatted(segment,
                        ID_PATTERN.pattern()));
                ok = false;
            }
        }
        return ok;
    }

    private boolean isValidChannel(String channelUID, List<@NonNull String> errors) {
        boolean ok = true;
        String[] segments = channelUID.split(AbstractUID.SEPARATOR);
        int nb = segments.length;
        if (nb < 4) {
            errors.add("not enough segments in channel UID \"%s\"; minimum 4 is expected".formatted(channelUID));
            ok = false;
        }
        String segment;
        for (int i = 0; i < (nb - 1); i++) {
            segment = segments[i];
            if (!ID_PATTERN.matcher(segment).matches()) {
                errors.add("segment \"%s\" in channel UID \"%s\" not matching the expected syntax %s".formatted(segment,
                        channelUID, ID_PATTERN.pattern()));
                ok = false;
            }
        }
        segment = segments[nb - 1];
        if (!CHANNEL_ID_PATTERN.matcher(segment).matches()) {
            errors.add("last segment \"%s\" in channel UID \"%s\" not matching the expected syntax %s"
                    .formatted(segment, channelUID, CHANNEL_ID_PATTERN.pattern()));
            ok = false;
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
