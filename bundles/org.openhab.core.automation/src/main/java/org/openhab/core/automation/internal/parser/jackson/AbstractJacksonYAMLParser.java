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
import org.openhab.core.automation.template.RuleTemplate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;

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
        // the core schema (instead of the JSON schema Jackson 3 uses by default) and the Jackson 2 mapper defaults keep
        // the Jackson 2 interpretation of plain scalars, except YAML 1.1 only ones like yes/no/on/off
        YAML_MAPPER = YAMLMapper
                .builder(YAMLFactory.builder().loadSettings(LeadingZeroStringCoreSchema.loadSettings()).build())
                .configureForJackson2().addMixIn(RuleTemplate.class, RuleTemplateMixin.class).findAndAddModules()
                .build();
    }

    /**
     * Jackson 3 derives the property name "UID" from {@link RuleTemplate#getUID()}, Jackson 2 derived "uid" and wrote
     * it first. Keep both, as the exported YAML is read back into {@code RuleTemplateDTO#uid}.
     */
    @JsonPropertyOrder("uid")
    private abstract static class RuleTemplateMixin {
        @JsonProperty("uid")
        abstract String getUID();
    }

    @Override
    public void serialize(Set<T> dataObjects, OutputStreamWriter writer) throws Exception {
        for (T dataObject : dataObjects) {
            YAML_MAPPER.writeValue(writer, dataObject);
        }
    }
}
