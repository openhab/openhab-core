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
package org.openhab.core.automation.internal.parser.jackson;

import java.io.OutputStreamWriter;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.parser.Parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

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
        YAML_MAPPER = new ObjectMapper(new YAMLFactory());
        YAML_MAPPER.findAndRegisterModules();
    }

    @Override
    public void serialize(Set<T> dataObjects, OutputStreamWriter writer) throws Exception {
        for (T dataObject : dataObjects) {
            YAML_MAPPER.writeValue(writer, dataObject);
        }
    }
}
