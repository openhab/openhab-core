/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.core.model.yaml.internal.item;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.core.items.dto.GroupItemDTO;
import org.openhab.core.model.yaml.YamlElement;
import org.openhab.core.model.yaml.YamlElementName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link YamlItemDTO} is a data transfer object used to serialize an item
 * in a YAML configuration file.
 *
 * @author Jan N. Klug - Initial contribution
 */
@YamlElementName("items")
public class YamlItemDTO extends GroupItemDTO implements YamlElement {
    private final Logger logger = LoggerFactory.getLogger(YamlItemDTO.class);

    @Override
    public @NonNull String getId() {
        return name;
    }

    @Override
    public boolean isValid() {
        if (name == null) {
            logger.debug("name missing");
            return false;
        }
        if (type == null) {
            logger.debug("itemType is missing for {}", name);
        }

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        YamlItemDTO that = (YamlItemDTO) o;
        return Objects.equals(name, that.name) && Objects.equals(type, that.type) && Objects.equals(label, that.label)
                && Objects.equals(category, that.category) && Objects.equals(groupNames, that.groupNames)
                && Objects.equals(tags, that.tags) && Objects.equals(groupType, that.groupType)
                && Objects.equals(function, that.function);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, label, category, groupNames, tags, groupType, function);
    }

    @Override
    public @NonNull String toString() {
        return "YamlItemDTO{name='" + name + "', type='" + type + "', label='" + label + "', category='" + category
                + "', groupNames=" + groupNames + ", tags=" + tags + ", baseType='" + groupType + "', function="
                + function + "}";
    }
}
