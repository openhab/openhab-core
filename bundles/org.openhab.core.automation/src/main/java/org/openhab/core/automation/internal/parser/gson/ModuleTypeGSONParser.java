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

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openhab.core.automation.dto.ActionTypeDTOMapper;
import org.openhab.core.automation.dto.CompositeActionTypeDTO;
import org.openhab.core.automation.dto.CompositeConditionTypeDTO;
import org.openhab.core.automation.dto.CompositeTriggerTypeDTO;
import org.openhab.core.automation.dto.ConditionTypeDTOMapper;
import org.openhab.core.automation.dto.ModuleTypeDTO;
import org.openhab.core.automation.dto.TriggerTypeDTOMapper;
import org.openhab.core.automation.parser.Parser;
import org.openhab.core.automation.parser.ParsingException;
import org.openhab.core.automation.parser.ParsingNestedException;
import org.openhab.core.automation.type.ActionType;
import org.openhab.core.automation.type.ConditionType;
import org.openhab.core.automation.type.ModuleType;
import org.openhab.core.automation.type.TriggerType;
import org.osgi.service.component.annotations.Component;

/**
 * This class can parse and serialize sets of {@link ModuleType}.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@Component(immediate = true, service = Parser.class, property = { "parser.type=parser.module.type", "format=json" })
public class ModuleTypeGSONParser extends AbstractGSONParser<ModuleType> {

    public ModuleTypeGSONParser() {
    }

    @Override
    public Set<ModuleType> parse(InputStreamReader reader) throws ParsingException {
        try {
            ModuleTypeParsingContainer mtContainer = gson.fromJson(reader, ModuleTypeParsingContainer.class);
            Set<ModuleType> result = new HashSet<>();
            addAll(result, mtContainer.triggers);
            addAll(result, mtContainer.conditions);
            addAll(result, mtContainer.actions);
            return result;
        } catch (Exception e) {
            throw new ParsingException(new ParsingNestedException(ParsingNestedException.MODULE_TYPE, null, e));
        }
    }

    @Override
    public void serialize(Set<ModuleType> dataObjects, OutputStreamWriter writer) throws Exception {
        Map<String, List<? extends ModuleType>> map = createMapByType(dataObjects);
        gson.toJson(map, writer);
    }

    private void addAll(Set<ModuleType> result, List<? extends ModuleTypeDTO> moduleTypes) {
        if (moduleTypes != null) {
            for (ModuleTypeDTO mt : moduleTypes) {
                if (mt instanceof CompositeTriggerTypeDTO) {
                    result.add(TriggerTypeDTOMapper.map((CompositeTriggerTypeDTO) mt));
                } else if (mt instanceof CompositeConditionTypeDTO) {
                    result.add(ConditionTypeDTOMapper.map((CompositeConditionTypeDTO) mt));
                } else if (mt instanceof CompositeActionTypeDTO) {
                    result.add(ActionTypeDTOMapper.map((CompositeActionTypeDTO) mt));
                }
            }
        }
    }

    private Map<String, List<? extends ModuleType>> createMapByType(Set<ModuleType> dataObjects) {
        Map<String, List<? extends ModuleType>> map = new HashMap<>();

        List<TriggerType> triggers = new ArrayList<>();
        List<ConditionType> conditions = new ArrayList<>();
        List<ActionType> actions = new ArrayList<>();
        for (ModuleType moduleType : dataObjects) {
            if (moduleType instanceof TriggerType) {
                triggers.add((TriggerType) moduleType);
            } else if (moduleType instanceof ConditionType) {
                conditions.add((ConditionType) moduleType);
            } else if (moduleType instanceof ActionType) {
                actions.add((ActionType) moduleType);
            }
        }
        map.put("triggers", triggers);
        map.put("conditions", conditions);
        map.put("actions", actions);
        return map;
    }
}
