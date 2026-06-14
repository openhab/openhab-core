/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.model.yaml.internal.config;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.core.config.core.ConfigDescriptionParameterGroup;
import org.openhab.core.config.core.ConfigDescriptionParameterGroupBuilder;
import org.openhab.core.config.core.dto.ConfigDescriptionParameterGroupDTO;

/**
 * This is a data transfer object used to (de)serialize a {@link ConfigDescriptionParameterGroup} in a YAML
 * configuration.
 *
 * @author Ravi Nadahar - Initial contribution
 */
public class YamlConfigDescriptionParameterGroupDTO {

    private static final Boolean ADVANCED_DEFAULT = Boolean.FALSE;

    public String context;
    public Boolean advanced;
    public String label;
    public String description;

    /**
     * Creates a new instance.
     */
    public YamlConfigDescriptionParameterGroupDTO() {
    }

    /**
     * Creates a new instance based on the specified {@link ConfigDescriptionParameterGroup}.
     *
     * @param parameterGroup the {@link ConfigDescriptionParameterGroup}.
     * @param includeDefault whether boolean values with the default value should be included.
     */
    public YamlConfigDescriptionParameterGroupDTO(@NonNull ConfigDescriptionParameterGroup parameterGroup,
            boolean includeDefault) {
        this.context = parameterGroup.getContext();
        this.advanced = parameterGroup.isAdvanced();
        if (!includeDefault && ADVANCED_DEFAULT.equals(this.advanced)) {
            this.advanced = null;
        }
        this.label = parameterGroup.getLabel();
        this.description = parameterGroup.getDescription();
    }

    @Override
    public int hashCode() {
        return Objects.hash(advanced, context, description, label);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof YamlConfigDescriptionParameterGroupDTO)) {
            return false;
        }
        YamlConfigDescriptionParameterGroupDTO other = (YamlConfigDescriptionParameterGroupDTO) obj;
        return Objects.equals(advanced, other.advanced) && Objects.equals(context, other.context)
                && Objects.equals(description, other.description) && Objects.equals(label, other.label);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
        if (label != null) {
            builder.append("label=").append(label);
        }
        if (context != null) {
            builder.append(", ").append("context=").append(context);
        }
        if (advanced != null) {
            builder.append(", ").append("advanced=").append(advanced);
        }
        if (description != null) {
            builder.append(", ").append("description=").append(description);
        }
        builder.append("]");
        return builder.toString();
    }

    /**
     * Create a new {@link ConfigDescriptionParameterGroup} from this {@link YamlConfigDescriptionParameterGroupDTO}
     * using the specified name.
     *
     * @param name the name to use.
     * @return The new {@link ConfigDescriptionParameterGroup}.
     */
    public @NonNull ConfigDescriptionParameterGroup map(@NonNull String name) {
        return ConfigDescriptionParameterGroupBuilder.create(name).withContext(context).withAdvanced(advanced)
                .withLabel(label).withDescription(description).build();
    }

    /**
     * Create a new {@link ConfigDescriptionParameterGroupDTO} from this {@link YamlConfigDescriptionParameterGroupDTO}
     * using the specified name.
     *
     * @param name the name to use.
     * @return The new {@link ConfigDescriptionParameterGroupDTO}.
     */
    public @NonNull ConfigDescriptionParameterGroupDTO toConfigDescriptionParameterGroupDTO(@NonNull String name) {
        ConfigDescriptionParameterGroupDTO result = new ConfigDescriptionParameterGroupDTO();
        result.name = name;
        result.context = context;
        result.advanced = advanced;
        result.label = label;
        result.description = description;
        return result;
    }

    /**
     * A variant of {@link YamlConfigDescriptionParameterGroupDTO} where it is specified as a array/list element instead
     * of a map value.
     */
    public static class YamlConfigDescriptionParameterGroupListEntryDTO {

        public String name;
        public String context;
        public Boolean advanced;
        public String label;
        public String description;

        /**
         * Convert this {@link YamlConfigDescriptionParameterGroupListEntryDTO} to a corresponding
         * {@link YamlConfigDescriptionParameterGroupDTO), where the name is missing. The name must be handled/kept
         * independently.
         *
         * @return The resulting {@link YamlConfigDescriptionParameterGroupDTO}.
         */
        public YamlConfigDescriptionParameterGroupDTO toYamlConfigDescriptionParameterGroupDTO() {
            YamlConfigDescriptionParameterGroupDTO result = new YamlConfigDescriptionParameterGroupDTO();
            result.context = context;
            result.advanced = advanced;
            result.label = label;
            result.description = description;
            return result;
        }
    }
}
