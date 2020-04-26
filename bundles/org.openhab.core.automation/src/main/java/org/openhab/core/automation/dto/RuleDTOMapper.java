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
package org.openhab.core.automation.dto;

import org.openhab.core.automation.Rule;
import org.openhab.core.automation.util.RuleBuilder;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.core.dto.ConfigDescriptionDTOMapper;

/**
 * This is a utility class to convert between the respective object and its DTO.
 *
 * @author Markus Rathgeb - Initial contribution
 * @author Kai Kreuzer - Changed to using RuleBuilder
 */
public class RuleDTOMapper {

    public static RuleDTO map(final Rule rule) {
        final RuleDTO ruleDto = new RuleDTO();
        fillProperties(rule, ruleDto);
        return ruleDto;
    }

    public static Rule map(final RuleDTO ruleDto) {
        return RuleBuilder.create(ruleDto.uid).withActions(ActionDTOMapper.mapDto(ruleDto.actions))
                .withConditions(ConditionDTOMapper.mapDto(ruleDto.conditions))
                .withTriggers(TriggerDTOMapper.mapDto(ruleDto.triggers))
                .withConfiguration(new Configuration(ruleDto.configuration))
                .withConfigurationDescriptions(ConfigDescriptionDTOMapper.map(ruleDto.configDescriptions))
                .withTemplateUID(ruleDto.templateUID).withVisibility(ruleDto.visibility).withTags(ruleDto.tags)
                .withName(ruleDto.name).withDescription(ruleDto.description).build();
    }

    protected static void fillProperties(final Rule from, final RuleDTO to) {
        to.triggers = TriggerDTOMapper.map(from.getTriggers());
        to.conditions = ConditionDTOMapper.map(from.getConditions());
        to.actions = ActionDTOMapper.map(from.getActions());
        to.configuration = from.getConfiguration().getProperties();
        to.configDescriptions = ConfigDescriptionDTOMapper.mapParameters(from.getConfigurationDescriptions());
        to.templateUID = from.getTemplateUID();
        to.uid = from.getUID();
        to.name = from.getName();
        to.tags = from.getTags();
        to.visibility = from.getVisibility();
        to.description = from.getDescription();
    }
}
