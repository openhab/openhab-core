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
package org.eclipse.smarthome.automation.core.dto;

import org.eclipse.smarthome.automation.dto.RuleTemplateDTO;
import org.eclipse.smarthome.automation.template.RuleTemplate;
import org.eclipse.smarthome.config.core.dto.ConfigDescriptionDTOMapper;

/**
 * This is a utility class to convert between the Rule Templates and RuleTemplateDTO objects.
 *
 * @author Ana Dimova - Initial contribution
 *
 */
public class RuleTemplateDTOMapper {

    public static RuleTemplateDTO map(final RuleTemplate template) {
        final RuleTemplateDTO templateDTO = new RuleTemplateDTO();
        fillProperties(template, templateDTO);
        return templateDTO;
    }

    public static RuleTemplate map(final RuleTemplateDTO ruleTemplateDto) {
        return new RuleTemplate(ruleTemplateDto.uid, ruleTemplateDto.label, ruleTemplateDto.description,
                ruleTemplateDto.tags, TriggerDTOMapper.mapDto(ruleTemplateDto.triggers),
                ConditionDTOMapper.mapDto(ruleTemplateDto.conditions), ActionDTOMapper.mapDto(ruleTemplateDto.actions),
                ConfigDescriptionDTOMapper.map(ruleTemplateDto.configDescriptions), ruleTemplateDto.visibility);
    }

    protected static void fillProperties(final RuleTemplate from, final RuleTemplateDTO to) {
        to.label = from.getLabel();
        to.uid = from.getUID();
        to.tags = from.getTags();
        to.description = from.getDescription();
        to.visibility = from.getVisibility();
        to.configDescriptions = ConfigDescriptionDTOMapper.mapParameters(from.getConfigurationDescriptions());
        to.triggers = TriggerDTOMapper.map(from.getTriggers());
        to.conditions = ConditionDTOMapper.map(from.getConditions());
        to.actions = ActionDTOMapper.map(from.getActions());
    }
}
