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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.ItemUtil;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.types.util.UnitUtils;

/**
 * The {@link YamlChannelDTO} is a data transfer object used to serialize a channel in a YAML configuration file.
 *
 * @author Laurent Garnier - Initial contribution
 */
public class YamlChannelDTO {

    private static final Pattern CHANNEL_TYPE_PATTERN = Pattern.compile("^[a-zA-Z0-9_][a-zA-Z0-9_-]*$");
    private static final Set<String> VALID_ITEM_TYPES = Set.of(CoreItemFactory.SWITCH, CoreItemFactory.ROLLERSHUTTER,
            CoreItemFactory.CONTACT, CoreItemFactory.STRING, CoreItemFactory.NUMBER, CoreItemFactory.DIMMER,
            CoreItemFactory.DATETIME, CoreItemFactory.COLOR, CoreItemFactory.IMAGE, CoreItemFactory.PLAYER,
            CoreItemFactory.LOCATION, CoreItemFactory.CALL);

    public String type;
    public String kind;
    public String itemType;
    public String label;
    public Map<@NonNull String, @NonNull Object> config;

    public YamlChannelDTO() {
    }

    public boolean isValid(@NonNull List<@NonNull String> errors, @NonNull List<@NonNull String> warnings) {
        boolean ok = true;
        if (type != null) {
            if (!CHANNEL_TYPE_PATTERN.matcher(type).find()) {
                errors.add(
                        "type \"%s\" is not matching the expected syntax [a-zA-Z0-9_][a-zA-Z0-9_-]*".formatted(type));
                ok = false;
            }
            if (kind != null) {
                warnings.add(
                        "kind \"%s\" is ignored as a type is also provided; kind will be retrieved from the channel type"
                                .formatted(kind));
            }
            if (itemType != null) {
                warnings.add(
                        "itemType \"%s\" is ignored as a type is also provided; item type will be retrieved from the channel type"
                                .formatted(itemType));
            }
        } else if (itemType != null) {
            if (!VALID_ITEM_TYPES.contains(ItemUtil.getMainItemType(itemType))) {
                errors.add("itemType \"%s\" is invalid".formatted(itemType));
                ok = false;
            } else if (CoreItemFactory.NUMBER.equals(ItemUtil.getMainItemType(itemType))) {
                String numberDimension = ItemUtil.getItemTypeExtension(itemType);
                if (numberDimension != null) {
                    try {
                        UnitUtils.parseDimension(numberDimension);
                    } catch (IllegalArgumentException e) {
                        errors.add("itemType \"%s\" has invalid number dimension".formatted(itemType));
                        ok = false;
                    }
                }
            }
            try {
                ChannelKind.parse(kind);
            } catch (IllegalArgumentException e) {
                warnings.add(
                        "kind \"%s\" is invalid (only \"state\" and \"trigger\" whatever the case are valid); \"state\" will be considered"
                                .formatted(kind != null ? kind : "null"));
            }
        } else {
            errors.add("type or itemType is mandatory");
            ok = false;
        }
        return ok;
    }

    public ChannelKind getKind() {
        try {
            return ChannelKind.parse(kind);
        } catch (IllegalArgumentException e) {
            return ChannelKind.STATE;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, itemType, label);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        YamlChannelDTO other = (YamlChannelDTO) obj;
        return Objects.equals(type, other.type) && getKind() == other.getKind()
                && Objects.equals(itemType, other.itemType) && Objects.equals(label, other.label)
                && YamlThingDTO.equalsConfig(config, other.config);
    }
}
