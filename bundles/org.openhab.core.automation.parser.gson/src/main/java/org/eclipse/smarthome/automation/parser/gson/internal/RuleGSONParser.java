/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.automation.parser.gson.internal;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.smarthome.automation.Rule;
import org.eclipse.smarthome.automation.core.dto.RuleDTOMapper;
import org.eclipse.smarthome.automation.dto.RuleDTO;
import org.eclipse.smarthome.automation.parser.Parser;
import org.eclipse.smarthome.automation.parser.ParsingException;
import org.eclipse.smarthome.automation.parser.ParsingNestedException;
import org.osgi.service.component.annotations.Component;

import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

/**
 * This class can parse and serialize sets of {@link Rule}s.
 *
 * @author Kai Kreuzer - Initial Contribution
 *
 */
@Component(immediate = true, service = Parser.class, property = { "parser.type=parser.rule", "format=json" })
public class RuleGSONParser extends AbstractGSONParser<Rule> {

    @Override
    public Set<Rule> parse(InputStreamReader reader) throws ParsingException {
        JsonReader jr = new JsonReader(reader);
        try {
            Set<Rule> rules = new HashSet<>();
            if (jr.hasNext()) {
                JsonToken token = jr.peek();
                if (JsonToken.BEGIN_ARRAY.equals(token)) {
                    List<RuleDTO> ruleDtos = gson.fromJson(jr, new TypeToken<List<RuleDTO>>() {
                    }.getType());
                    for (RuleDTO ruleDto : ruleDtos) {
                        rules.add(RuleDTOMapper.map(ruleDto));
                    }
                } else {
                    RuleDTO ruleDto = gson.fromJson(jr, RuleDTO.class);
                    rules.add(RuleDTOMapper.map(ruleDto));
                }
                return rules;
            }
        } catch (Exception e) {
            throw new ParsingException(new ParsingNestedException(ParsingNestedException.RULE, null, e));
        } finally {
            try {
                jr.close();
            } catch (IOException e) {
            }
        }
        return Collections.emptySet();
    }

}
