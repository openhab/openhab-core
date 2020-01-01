/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.automation.internal.parser.gson;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
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

import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

/**
 * This class can parse and serialize sets of {@link Template}s.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = Parser.class, property = { "parser.type=parser.template", "format=json" })
public class TemplateGSONParser extends AbstractGSONParser<Template> {

    @Override
    public Set<Template> parse(InputStreamReader reader) throws ParsingException {
        JsonReader jr = new JsonReader(reader);
        try {
            if (jr.hasNext()) {
                JsonToken token = jr.peek();
                Set<Template> templates = new HashSet<>();
                if (JsonToken.BEGIN_ARRAY.equals(token)) {
                    List<RuleTemplateDTO> templateDtos = gson.fromJson(jr, new TypeToken<List<RuleTemplateDTO>>() {
                    }.getType());
                    for (RuleTemplateDTO templateDto : templateDtos) {
                        templates.add(RuleTemplateDTOMapper.map(templateDto));
                    }
                } else {
                    RuleTemplateDTO template = gson.fromJson(jr, RuleTemplateDTO.class);
                    templates.add(RuleTemplateDTOMapper.map(template));
                }
                return templates;
            }
        } catch (Exception e) {
            throw new ParsingException(new ParsingNestedException(ParsingNestedException.TEMPLATE, null, e));
        } finally {
            try {
                jr.close();
            } catch (IOException e) {
            }
        }
        return Collections.emptySet();
    }
}
