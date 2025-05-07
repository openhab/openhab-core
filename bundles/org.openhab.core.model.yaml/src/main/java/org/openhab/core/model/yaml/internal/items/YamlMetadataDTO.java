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

import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.model.yaml.internal.util.YamlElementUtils;

/**
 * The {@link YamlMetadataDTO} is a data transfer object used to serialize a metadata for a particular namespace
 * in a YAML configuration file.
 *
 * @author Laurent Garnier - Initial contribution
 */
public class YamlMetadataDTO {

    public String value;
    public Map<@NonNull String, @NonNull Object> config;

    public YamlMetadataDTO() {
    }

    public @NonNull String getValue() {
        return value == null ? "" : value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getValue());
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        YamlMetadataDTO other = (YamlMetadataDTO) obj;
        return Objects.equals(getValue(), other.getValue()) && YamlElementUtils.equalsConfig(config, other.config);
    }
}
