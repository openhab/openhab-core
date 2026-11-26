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
package org.openhab.core.model.yaml.internal.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openhab.core.model.yaml.internal.config.YamlConfigDescriptionParameterDTO;
import org.openhab.core.model.yaml.internal.config.YamlConfigDescriptionParameterDTO.YamlConfigDescriptionParameterListEntryDTO;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

/**
 * A Jackson deserializer for a collection of configuration description parameters that can be specified in either
 * array/list or object/map form.
 *
 * @author Ravi Nadahar - Initial contribution
 */
public class ConfigDescriptionParametersDeserializer
        extends StdDeserializer<Map<String, YamlConfigDescriptionParameterDTO>> {

    private static final long serialVersionUID = 1L;

    public ConfigDescriptionParametersDeserializer() {
        super(Map.class);
    }

    @Override
    public Map<String, YamlConfigDescriptionParameterDTO> deserialize(JsonParser p, DeserializationContext ctxt)
            throws JacksonException {
        if (p.hasToken(JsonToken.START_ARRAY)) {
            return convertArrayToStandardDTO(
                    ctxt.readValue(p, new TypeReference<List<YamlConfigDescriptionParameterListEntryDTO>>() {
                    }));
        }

        return ctxt.readValue(p, new TypeReference<Map<String, YamlConfigDescriptionParameterDTO>>() {
        });
    }

    private Map<String, YamlConfigDescriptionParameterDTO> convertArrayToStandardDTO(
            List<YamlConfigDescriptionParameterListEntryDTO> list) {
        Map<String, YamlConfigDescriptionParameterDTO> result = new LinkedHashMap<>();
        for (YamlConfigDescriptionParameterListEntryDTO entry : list) {
            result.put(entry.name, entry.toYamlConfigDescriptionParameterDTO());
        }
        return result;
    }
}
