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
package org.openhab.core.automation.internal.module.handler;

import java.util.Map;

import org.openhab.core.automation.Condition;
import org.openhab.core.automation.handler.BaseConditionModuleHandler;
import org.openhab.core.events.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the implementation of a event condition which checks if inputs matches configured values.
 *
 * @author Benedikt Niehues - Initial contribution
 * @author Kai Kreuzer - refactored and simplified customized module handling
 */
public class GenericEventConditionHandler extends BaseConditionModuleHandler {

    public final Logger logger = LoggerFactory.getLogger(GenericEventConditionHandler.class);

    public static final String MODULETYPE_ID = "core.GenericEventCondition";

    private static final String TOPIC = "topic";
    private static final String EVENTTYPE = "eventType";
    private static final String SOURCE = "source";
    private static final String PAYLOAD = "payload";

    public GenericEventConditionHandler(Condition module) {
        super(module);
    }

    private boolean isConfiguredAndMatches(String keyParam, String value) {
        Object mo = module.getConfiguration().get(keyParam);
        String configValue = mo != null && mo instanceof String ? (String) mo : null;
        if (configValue != null) {
            if (PAYLOAD.equals(keyParam)) {
                // automatically adding wildcards only for payload matching
                configValue = configValue.startsWith("*") ? configValue : ".*" + configValue;
                configValue = configValue.endsWith("*") ? configValue : configValue + ".*";
            }
            if (value != null) {
                return value.matches(configValue);
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    @Override
    public boolean isSatisfied(Map<String, Object> inputs) {
        Event event = inputs.get("event") != null ? (Event) inputs.get("event") : null;
        if (event != null) {
            return isConfiguredAndMatches(TOPIC, event.getTopic()) && isConfiguredAndMatches(SOURCE, event.getSource())
                    && isConfiguredAndMatches(PAYLOAD, event.getPayload())
                    && isConfiguredAndMatches(EVENTTYPE, event.getType());
        }
        return false;
    }
}
