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
package org.openhab.core.automation.rest.internal.dto;

import org.openhab.core.automation.RuleStatusInfo;
import org.openhab.core.automation.dto.RuleDTO;

/**
 * This is a data transfer object that is used to serialize rules with dynamic data like the status.
 *
 * @author Kai Kreuzer - Initial contribution
 */
public class EnrichedRuleDTO extends RuleDTO {

    public RuleStatusInfo status;
    public Boolean editable;
}
