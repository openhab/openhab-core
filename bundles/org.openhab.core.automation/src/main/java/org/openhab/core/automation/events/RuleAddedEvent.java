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
package org.openhab.core.automation.events;

import org.openhab.core.automation.dto.RuleDTO;

/**
 * An {@link RuleAddedEvent} notifies subscribers that a rule has been added.
 *
 * @author Benedikt Niehues - Initial contribution
 */
public class RuleAddedEvent extends AbstractRuleRegistryEvent {

    public static final String TYPE = RuleAddedEvent.class.getSimpleName();

    /**
     * constructs a new rule added event
     *
     * @param topic the topic of the event
     * @param payload the payload of the event
     * @param source the source of the event
     * @param ruleDTO the rule for which this event is created
     */
    public RuleAddedEvent(String topic, String payload, String source, RuleDTO rule) {
        super(topic, payload, source, rule);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return "Rule '" + getRule().uid + "' has been added.";
    }
}
