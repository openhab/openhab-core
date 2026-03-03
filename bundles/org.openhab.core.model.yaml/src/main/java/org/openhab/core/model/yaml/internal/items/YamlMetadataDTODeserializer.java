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

import java.util.Map;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

/**
 * Custom deserializer for {@link YamlMetadataDTO} that converts any YAML scalar
 * (string, integer, boolean, or float) into a metadata String {@code value} with an empty config.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
class YamlMetadataDTODeserializer extends ValueDeserializer<YamlMetadataDTO> {

    @Override
    public YamlMetadataDTO deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        JsonToken token = p.currentToken();
        if (token == JsonToken.VALUE_STRING || token == JsonToken.VALUE_NUMBER_INT
                || token == JsonToken.VALUE_NUMBER_FLOAT || token == JsonToken.VALUE_TRUE
                || token == JsonToken.VALUE_FALSE) {
            YamlMetadataDTO dto = new YamlMetadataDTO();
            dto.value = p.getValueAsString("");
            return dto;
        }
        if (token == JsonToken.START_OBJECT) {
            JsonNode node = p.readValueAsTree();

            YamlMetadataDTO dto = new YamlMetadataDTO();
            JsonNode valueNode = node.get("value");
            if (valueNode != null && !valueNode.isNull()) {
                dto.value = valueNode.asString();
            }

            JsonNode configNode = node.get("config");
            if (configNode != null && !configNode.isNull()) {
                dto.config = ctxt.readTreeAsValue(configNode, Map.class);
            }
            return dto;
        }
        return (YamlMetadataDTO) ctxt.handleUnexpectedToken(YamlMetadataDTO.class, p);
    }

    @Override
    public YamlMetadataDTO getNullValue(DeserializationContext ctxt) {
        return new YamlMetadataDTO();
    }
}
