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
import org.openhab.core.events.AbstractEvent;

/**
 * abstract class for rule events
 *
 * @author Benedikt Niehues - Initial contribution
 * @author Markus Rathgeb - Use the DTO for the Rule representation
 */
public abstract class AbstractRuleRegistryEvent extends AbstractEvent {

    private final RuleDTO rule;

    /**
     * Must be called in subclass constructor to create a new rule registry event.
     *
     * @param topic the topic of the event
     * @param payload the payload of the event
     * @param source the source of the event
     * @param ruleDTO the ruleDTO for which this event is created
     */
    public AbstractRuleRegistryEvent(String topic, String payload, String source, RuleDTO rule) {
        super(topic, payload, source);
        this.rule = rule;
    }

    /**
     * @return the RuleDTO which caused the Event
     */
    public RuleDTO getRule() {
        return this.rule;
    }
}
