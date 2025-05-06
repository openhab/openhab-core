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
package org.openhab.core.model.yaml.internal.rules;

import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.core.automation.Module;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * The {@link YamlRuleDTO} is a data transfer object used to serialize a module in a YAML configuration file.
 *
 * @author Ravi Nadahar - Initial contribution
 */
public class YamlModuleDTO {

    public String id;
    public String label;
    public String description;
    @JsonAlias({ "configuration" })
    public Map<@NonNull String, @NonNull Object> config;
    public String type;

    public YamlModuleDTO() {
    }

    public YamlModuleDTO(@NonNull Module module) {
        this.id = module.getId();
        this.label = module.getLabel();
        this.description = module.getDescription();
        this.config = module.getConfiguration().getProperties();
        this.type = module.getTypeUID();
    }

    @Override
    public int hashCode() {
        return Objects.hash(config, description, id, label, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof YamlModuleDTO)) {
            return false;
        }
        YamlModuleDTO other = (YamlModuleDTO) obj;
        return Objects.equals(config, other.config) && Objects.equals(description, other.description)
                && Objects.equals(id, other.id) && Objects.equals(label, other.label)
                && Objects.equals(type, other.type);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(getClass().getSimpleName());
        builder.append(" [");
        if (id != null) {
            builder.append("id=").append(id).append(", ");
        }
        if (type != null) {
            builder.append("type=").append(type).append(", ");
        }
        if (label != null) {
            builder.append("label=").append(label).append(", ");
        }
        if (description != null) {
            builder.append("description=").append(description).append(", ");
        }
        if (config != null) {
            builder.append("config=").append(config);
        }
        builder.append("]");
        return builder.toString();
    }
}
