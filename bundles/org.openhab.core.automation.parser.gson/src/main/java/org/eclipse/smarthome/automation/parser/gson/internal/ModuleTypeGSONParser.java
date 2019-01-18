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

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.automation.core.dto.ActionTypeDTOMapper;
import org.eclipse.smarthome.automation.core.dto.ConditionTypeDTOMapper;
import org.eclipse.smarthome.automation.core.dto.TriggerTypeDTOMapper;
import org.eclipse.smarthome.automation.dto.CompositeActionTypeDTO;
import org.eclipse.smarthome.automation.dto.CompositeConditionTypeDTO;
import org.eclipse.smarthome.automation.dto.CompositeTriggerTypeDTO;
import org.eclipse.smarthome.automation.dto.ModuleTypeDTO;
import org.eclipse.smarthome.automation.parser.Parser;
import org.eclipse.smarthome.automation.parser.ParsingException;
import org.eclipse.smarthome.automation.parser.ParsingNestedException;
import org.eclipse.smarthome.automation.type.ActionType;
import org.eclipse.smarthome.automation.type.ConditionType;
import org.eclipse.smarthome.automation.type.ModuleType;
import org.eclipse.smarthome.automation.type.TriggerType;
import org.osgi.service.component.annotations.Component;

/**
 * This class can parse and serialize sets of {@link ModuleType}.
 *
 * @author Kai Kreuzer - Initial Contribution
 *
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
        Map<String, List<? extends ModuleType>> map = new HashMap<String, List<? extends ModuleType>>();

        List<TriggerType> triggers = new ArrayList<TriggerType>();
        List<ConditionType> conditions = new ArrayList<ConditionType>();
        List<ActionType> actions = new ArrayList<ActionType>();
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
