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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.model.yaml.internal.util.YamlElementUtils;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelTypeUID;

/**
 * The {@link YamlChannelDTO} is a data transfer object used to serialize a channel in a YAML configuration file.
 *
 * @author Laurent Garnier - Initial contribution
 */
public class YamlChannelDTO {

    public String type;
    public String kind;
    public String itemType;
    public String itemDimension;
    public String label;
    public String description;
    public Map<@NonNull String, @NonNull Object> config;

    public YamlChannelDTO() {
    }

    public boolean isValid(@NonNull List<@NonNull String> errors, @NonNull List<@NonNull String> warnings) {
        boolean ok = true;
        if (config != null) {
            try {
                new Configuration(config);
            } catch (IllegalArgumentException e) {
                errors.add("invalid data in \"config\" field: %s".formatted(e.getMessage()));
                ok = false;
            }
        }
        if (type != null) {
            try {
                new ChannelTypeUID("dummy", type);
            } catch (IllegalArgumentException e) {
                errors.add("invalid value \"%s\" for \"type\" field: %s".formatted(type, e.getMessage()));
                ok = false;
            }
            if (kind != null) {
                warnings.add("\"kind\" field ignored; channel kind will be retrieved from the channel type");
            }
            if (itemType != null) {
                warnings.add("\"itemType\" field ignored; item type will be retrieved from the channel type");
            }
            if (itemDimension != null) {
                warnings.add(
                        "\"itemDimension\" field ignored; item type and dimension will be retrieved from the channel type");
            }
        } else if (itemType != null) {
            if (!YamlElementUtils.isValidItemType(itemType)) {
                errors.add("invalid value \"%s\" for \"itemType\" field".formatted(itemType));
                ok = false;
            } else if (YamlElementUtils.isNumberItemType(itemType)) {
                if (!YamlElementUtils.isValidItemDimension(itemDimension)) {
                    errors.add("invalid value \"%s\" for \"itemDimension\" field".formatted(itemDimension));
                    ok = false;
                }
            } else if (itemDimension != null) {
                warnings.add("\"itemDimension\" field ignored as item type is not Number");
            }
            try {
                ChannelKind.parse(kind);
            } catch (IllegalArgumentException e) {
                warnings.add(
                        "invalid value \"%s\" for \"kind\" field; only \"state\" and \"trigger\" whatever the case are valid; \"state\" will be considered"
                                .formatted(kind != null ? kind : "null"));
            }
        } else {
            errors.add("one of the \"type\" and \"itemType\" fields is mandatory");
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

    public @Nullable String getItemType() {
        return YamlElementUtils.getItemTypeWithDimension(itemType, itemDimension);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, getKind(), getItemType(), label, description);
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
                && Objects.equals(getItemType(), other.getItemType()) && Objects.equals(label, other.label)
                && Objects.equals(description, other.description)
                && YamlElementUtils.equalsConfig(config, other.config);
    }
}
