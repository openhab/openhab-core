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
package org.openhab.core.model.yaml.internal.metadata;

import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.core.model.yaml.YamlElement;
import org.openhab.core.model.yaml.YamlElementName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link YamlMetadataDTO} is a data transfer object used to serialize item metadata
 * in a YAML configuration file.
 *
 * @author Jan N. Klug - Initial contribution
 */
@YamlElementName("metadata")
public class YamlMetadataDTO implements YamlElement {
    private final Logger logger = LoggerFactory.getLogger(YamlMetadataDTO.class);

    public String namespace;
    public String itemName;

    public String value;
    public Map<String, Object> config;

    @Override
    public @NonNull String getId() {
        return namespace + ":" + itemName;
    }

    @Override
    public boolean isValid() {
        if (namespace == null || itemName == null) {
            logger.debug("namespace or itemName missing");
            return false;
        }

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        YamlMetadataDTO that = (YamlMetadataDTO) o;
        return Objects.equals(namespace, that.namespace) && Objects.equals(itemName, that.itemName)
                && Objects.equals(value, that.value) && Objects.equals(config, that.config);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, itemName, value, config);
    }

    @Override
    public @NonNull String toString() {
        return "YamlMetadataDTO{namespace='" + namespace + "', itemName='" + itemName + "', value='" + value
                + "', config=" + config + "}";
    }
}
