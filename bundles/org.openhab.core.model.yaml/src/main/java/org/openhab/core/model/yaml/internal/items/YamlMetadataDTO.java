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
package org.openhab.core.model.yaml.internal.items;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The {@link YamlMetadataDTO} is a data transfer object used to serialize a metadata for a particular namespace
 * in a YAML configuration file.
 *
 * @author Laurent Garnier - Initial contribution
 * @author Jimmy Tanagra - Support scalar metadata namespace
 */
public class YamlMetadataDTO {

    public String value;
    public Map<@NonNull String, @NonNull Object> config;

    public YamlMetadataDTO() {
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static YamlMetadataDTO fromYaml(@Nullable Object source) {
        YamlMetadataDTO dto = new YamlMetadataDTO();

        if (source == null) {
            return dto;
        }

        if (source instanceof Map<?, ?> sourceMap) {
            Object sourceValue = sourceMap.get("value");
            if (sourceValue != null) {
                dto.value = sourceValue.toString();
            }

            Object sourceConfig = sourceMap.get("config");
            if (sourceConfig instanceof Map<?, ?> sourceConfigMap) {
                Map<@NonNull String, @NonNull Object> configMap = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : sourceConfigMap.entrySet()) {
                    Object key = entry.getKey();
                    Object mapValue = entry.getValue();
                    if (key != null && mapValue != null) {
                        configMap.put(key.toString(), mapValue);
                    }
                }
                dto.config = configMap;
            }

            return dto;
        }

        dto.value = source.toString();
        return dto;
    }

    @JsonValue
    public Object serialize() {
        if (config == null || config.isEmpty()) {
            return getValue();
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("value", getValue());
        map.put("config", config);
        return map;
    }

    public @NonNull String getValue() {
        return value == null ? "" : value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getValue(), config);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        YamlMetadataDTO other = (YamlMetadataDTO) obj;
        return Objects.equals(getValue(), other.getValue()) && Objects.equals(config, other.config);
    }
}
