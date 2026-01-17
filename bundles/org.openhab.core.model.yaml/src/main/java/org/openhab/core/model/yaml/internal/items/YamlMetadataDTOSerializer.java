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

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Custom serializer for {@link YamlMetadataDTO} that writes the namespace as a scalar
 * when its config is empty, otherwise writes as an object with value and config fields.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
public class YamlMetadataDTOSerializer extends JsonSerializer<YamlMetadataDTO> {
    @Override
    public void serialize(YamlMetadataDTO value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        Map<?, ?> config = value.config;
        boolean configIsEmpty = (config == null || config.isEmpty());
        if (configIsEmpty) {
            gen.writeString(value.getValue());
        } else {
            gen.writeStartObject();
            gen.writeStringField("value", value.getValue());
            gen.writeObjectField("config", value.config);
            gen.writeEndObject();
        }
    }
}
