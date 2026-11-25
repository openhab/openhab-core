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
package org.openhab.core.automation.internal.parser.jackson;

import java.io.OutputStreamWriter;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.parser.Parser;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLReadFeature;
import tools.jackson.dataformat.yaml.YAMLSchema;

/**
 * Abstract class that can be used by YAML parsers for the different entity types.
 *
 * @param <T> the type of the entities to parse
 *
 * @author Arne Seime - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractJacksonYAMLParser<T> implements Parser<T> {

    /** The YAML object mapper instance */
    protected static final ObjectMapper YAML_MAPPER;

    static {
        // keep the Jackson 2 interpretation of plain scalars and the Jackson 2 mapper defaults
        YAML_MAPPER = YAMLMapper.builder(YAMLFactory.builder().yamlSchema(YAMLSchema.CORE).build())
                .configureForJackson2().disable(YAMLReadFeature.EMPTY_STRING_AS_NULL).findAndAddModules().build();
    }

    @Override
    public void serialize(Set<T> dataObjects, OutputStreamWriter writer) throws Exception {
        for (T dataObject : dataObjects) {
            YAML_MAPPER.writeValue(writer, dataObject);
        }
    }
}
