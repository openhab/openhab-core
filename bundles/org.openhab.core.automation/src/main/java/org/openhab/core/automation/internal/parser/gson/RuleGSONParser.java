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
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.dto.RuleDTO;
import org.openhab.core.automation.dto.RuleDTOMapper;
import org.openhab.core.automation.parser.Parser;
import org.openhab.core.automation.parser.ParsingException;
import org.openhab.core.automation.parser.ParsingNestedException;
import org.osgi.service.component.annotations.Component;

import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

/**
 * This class can parse and serialize sets of {@link Rule}s.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
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
