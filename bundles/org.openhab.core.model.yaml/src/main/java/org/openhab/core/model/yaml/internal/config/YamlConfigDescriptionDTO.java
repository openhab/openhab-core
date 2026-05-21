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

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionBuilder;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameterGroup;
import org.openhab.core.config.core.dto.ConfigDescriptionDTO;
import org.openhab.core.config.core.dto.ConfigDescriptionParameterDTO;
import org.openhab.core.config.core.dto.ConfigDescriptionParameterGroupDTO;
import org.openhab.core.model.yaml.internal.util.ConfigDescriptionParameterGroupsDeserializer;
import org.openhab.core.model.yaml.internal.util.ConfigDescriptionParametersDeserializer;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * This is a data transfer object used to (de)serialize a {@link ConfigDescription} in a YAML configuration.
 *
 * @author Ravi Nadahar - Initial contribution
 */
public class YamlConfigDescriptionDTO {

    public String uri;
    @JsonAlias({ "parameters" })
    @JsonDeserialize(using = ConfigDescriptionParametersDeserializer.class)
    public Map<String, YamlConfigDescriptionParameterDTO> params;
    @JsonAlias({ "parameterGroups" })
    @JsonDeserialize(using = ConfigDescriptionParameterGroupsDeserializer.class)
    public Map<String, YamlConfigDescriptionParameterGroupDTO> paramGroups;

    /**
     * Creates a new instance.
     */
    public YamlConfigDescriptionDTO() {
    }

    /**
     * Creates a new instance based on the specified {@link ConfiConfigDescriptiongDescriptionParameterGroup}.
     *
     * @param configDescription the {@link ConfigDescription}.
     */
    public YamlConfigDescriptionDTO(@NonNull ConfigDescription configDescription) {
        this.uri = toDecodedString(configDescription.getUID());
        List<@NonNull ConfigDescriptionParameter> fromParams = configDescription.getParameters();
        if (!fromParams.isEmpty()) {
            Map<String, YamlConfigDescriptionParameterDTO> params = new LinkedHashMap<>();
            for (ConfigDescriptionParameter param : fromParams) {
                params.put(param.getName(), new YamlConfigDescriptionParameterDTO(param));
            }
            this.params = params;
        }
        List<@NonNull ConfigDescriptionParameterGroup> fromParamGroups = configDescription.getParameterGroups();
        if (!fromParamGroups.isEmpty()) {
            Map<String, YamlConfigDescriptionParameterGroupDTO> paramGroups = new LinkedHashMap<>();
            for (ConfigDescriptionParameterGroup paramGroup : fromParamGroups) {
                paramGroups.put(paramGroup.getName(), new YamlConfigDescriptionParameterGroupDTO(paramGroup));
            }
            this.paramGroups = paramGroups;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(paramGroups, params, uri);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof YamlConfigDescriptionDTO)) {
            return false;
        }
        YamlConfigDescriptionDTO other = (YamlConfigDescriptionDTO) obj;
        return Objects.equals(paramGroups, other.paramGroups) && Objects.equals(params, other.params)
                && Objects.equals(uri, other.uri);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
        if (uri != null) {
            builder.append("uri=").append(uri).append(", ");
        }
        if (params != null) {
            builder.append("params=").append(params).append(", ");
        }
        if (paramGroups != null) {
            builder.append("paramGroups=").append(paramGroups);
        }
        builder.append("]");
        return builder.toString();
    }

    /**
     * Create a new {@link ConfigDescription} from this {@link YamlConfigDescriptionDTO}.
     *
     * @return The new {@link ConfigDescription} instance.
     * @throws UnsupportedOperationException If {@code url} is {@code null} or invalid.
     */
    public @NonNull ConfigDescription map() {
        if (uri == null) {
            throw new UnsupportedOperationException("A " + getClass().getSimpleName()
                    + " without an URI can't be mapped to a ConfigDescription: " + toString());
        }
        try {
            ConfigDescriptionBuilder builder = ConfigDescriptionBuilder.create(URI.create(uri));
            if (params != null) {
                for (Entry<String, YamlConfigDescriptionParameterDTO> paramEntry : params.entrySet()) {
                    builder.withParameter(paramEntry.getValue().map(paramEntry.getKey()));
                }
            }
            if (paramGroups != null) {
                for (Entry<String, YamlConfigDescriptionParameterGroupDTO> paramGroupEntry : paramGroups.entrySet()) {
                    builder.withParameterGroup(paramGroupEntry.getValue().map(paramGroupEntry.getKey()));
                }
            }
            return builder.build();
        } catch (IllegalArgumentException e) {
            throw new UnsupportedOperationException("A " + getClass().getSimpleName()
                    + " without a valid URI can't be mapped to a ConfigDescription: " + toString(), e);
        }
    }

    /**
     * Create a new {@link ConfigDescriptionDTO} from this {@link YamlConfigDescriptionDTO}.
     *
     * @return The new {@link ConfigDescriptionDTO} instance.
     */
    public @NonNull ConfigDescriptionDTO toConfigDescriptionDTO() {
        ConfigDescriptionDTO result = new ConfigDescriptionDTO();
        result.uri = uri;
        if (params != null) {
            List<ConfigDescriptionParameterDTO> parameters = new ArrayList<>();
            for (Entry<String, YamlConfigDescriptionParameterDTO> paramEntry : params.entrySet()) {
                parameters.add(paramEntry.getValue().toConfigDescriptionParameterDTO(paramEntry.getKey()));
            }
            result.parameters = parameters;
        }
        if (paramGroups != null) {
            List<ConfigDescriptionParameterGroupDTO> parameterGroups = new ArrayList<>();
            for (Entry<String, YamlConfigDescriptionParameterGroupDTO> paramGroupEntry : paramGroups.entrySet()) {
                parameterGroups
                        .add(paramGroupEntry.getValue().toConfigDescriptionParameterGroupDTO(paramGroupEntry.getKey()));
            }
            result.parameterGroups = parameterGroups;
        }
        return result;
    }

    private static String toDecodedString(URI uri) {
        // Combine these partials because URI.toString() does not decode
        return uri.getScheme() + ":" + uri.getSchemeSpecificPart();
    }
}
