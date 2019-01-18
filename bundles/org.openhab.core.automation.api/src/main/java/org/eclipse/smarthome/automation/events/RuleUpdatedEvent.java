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
package org.eclipse.smarthome.automation.events;

import org.eclipse.smarthome.automation.dto.RuleDTO;

/**
 * An {@link RuleUpdatedEvent} notifies subscribers that a rule has been updated.
 *
 * @author Benedikt Niehues - initial contribution
 *
 */
public class RuleUpdatedEvent extends AbstractRuleRegistryEvent {

    public static final String TYPE = RuleUpdatedEvent.class.getSimpleName();

    private final RuleDTO oldRule;

    /**
     * constructs a new rule updated event
     *
     * @param topic   the topic of the event
     * @param payload the payload of the event
     * @param source  the source of the event
     * @param rule    the rule for which is this event
     * @param oldRule the rule that has been updated
     */
    public RuleUpdatedEvent(String topic, String payload, String source, RuleDTO rule, RuleDTO oldRule) {
        super(topic, payload, source, rule);
        this.oldRule = oldRule;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * @return the oldRuleDTO
     */
    public RuleDTO getOldRule() {
        return oldRule;
    }

    @Override
    public String toString() {
        return "Rule '" + getRule().uid + "' has been updated.";
    }

}
