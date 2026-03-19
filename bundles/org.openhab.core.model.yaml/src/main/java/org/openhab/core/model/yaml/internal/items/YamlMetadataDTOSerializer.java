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
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * Custom serializer for {@link YamlMetadataDTO} that writes the namespace as a scalar
 * when its config is empty, otherwise writes as an object with value and config fields.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
public class YamlMetadataDTOSerializer extends ValueSerializer<YamlMetadataDTO> {
    @Override
    public void serialize(YamlMetadataDTO value, JsonGenerator gen, SerializationContext serializers)
            throws JacksonException {
        Map<?, ?> config = value.config;
        boolean configIsEmpty = (config == null || config.isEmpty());
        if (configIsEmpty) {
            gen.writeString(value.getValue());
        } else {
            gen.writeStartObject();
            gen.writeName("value");
            gen.writeString(value.getValue());
            gen.writeName("config");
            serializers.writeValue(gen, value.config);
            gen.writeEndObject();
        }
    }
}
