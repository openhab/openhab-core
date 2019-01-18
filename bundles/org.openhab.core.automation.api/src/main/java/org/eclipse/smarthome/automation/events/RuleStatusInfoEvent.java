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

import org.eclipse.smarthome.automation.RuleStatusInfo;
import org.eclipse.smarthome.core.events.AbstractEvent;

/**
 * An {@link RuleStatusInfoEvent} notifies subscribers that a rule status has been updated.
 *
 * @author Benedikt Niehues - initial contribution
 * @author Kai Kreuzer - added toString method
 *
 */
public class RuleStatusInfoEvent extends AbstractEvent {

    public static final String TYPE = RuleStatusInfoEvent.class.getSimpleName();

    private RuleStatusInfo statusInfo;
    private String ruleId;

    /**
     * constructs a new rule status event
     *
     * @param topic      the topic of the event
     * @param payload    the payload of the event
     * @param source     the source of the event
     * @param statusInfo the status info for this event
     * @param ruleId     the rule for which this event is
     */
    public RuleStatusInfoEvent(String topic, String payload, String source, RuleStatusInfo statusInfo, String ruleId) {
        super(topic, payload, source);
        this.statusInfo = statusInfo;
        this.ruleId = ruleId;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * @return the statusInfo
     */
    public RuleStatusInfo getStatusInfo() {
        return statusInfo;
    }

    /**
     * @return the ruleId
     */
    public String getRuleId() {
        return ruleId;
    }

    @Override
    public String toString() {
        return ruleId + " updated: " + statusInfo.toString();
    }

}
