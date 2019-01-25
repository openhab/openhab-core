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
package org.openhab.core.automation.rest.internal.dto;

import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleManager;
import org.openhab.core.automation.dto.RuleDTOMapper;

/**
 * This is a utility class to convert between the respective object and its DTO.
 *
 * @author Markus Rathgeb - Initial contribution and API
 */
public class EnrichedRuleDTOMapper extends RuleDTOMapper {

    public static EnrichedRuleDTO map(final Rule rule, final RuleManager ruleEngine) {
        final EnrichedRuleDTO enrichedRuleDto = new EnrichedRuleDTO();
        fillProperties(rule, enrichedRuleDto);
        enrichedRuleDto.status = ruleEngine.getStatusInfo(rule.getUID());
        return enrichedRuleDto;
    }

}
