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

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.dto.RuleTemplateDTO;
import org.openhab.core.automation.dto.RuleTemplateDTOMapper;
import org.openhab.core.automation.parser.Parser;
import org.openhab.core.automation.parser.ParsingException;
import org.openhab.core.automation.parser.ParsingNestedException;
import org.openhab.core.automation.template.Template;
import org.osgi.service.component.annotations.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * This class can parse and serialize sets of {@link Template}s.
 *
 * @author Arne Seime - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = Parser.class, property = { "parser.type=parser.template", "format=yaml" })
public class TemplateYAMLParser extends AbstractJacksonYAMLParser<Template> {

    @Override
    public Set<Template> parse(InputStreamReader reader) throws ParsingException {
        try {
            Set<Template> templates = new HashSet<>();
            JsonNode rootNode = YAML_MAPPER.readTree(reader);
            if (rootNode.isArray()) {
                List<RuleTemplateDTO> templateDtos = YAML_MAPPER.convertValue(rootNode,
                        new TypeReference<List<RuleTemplateDTO>>() {
                        });
                for (RuleTemplateDTO templateDTO : templateDtos) {
                    templates.add(RuleTemplateDTOMapper.map(templateDTO));
                }
            } else {
                RuleTemplateDTO templateDto = YAML_MAPPER.convertValue(rootNode, new TypeReference<RuleTemplateDTO>() {
                });
                templates.add(RuleTemplateDTOMapper.map(templateDto));
            }
            return templates;
        } catch (Exception e) {
            throw new ParsingException(new ParsingNestedException(ParsingNestedException.TEMPLATE, null, e));
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
            }
        }
    }
}
