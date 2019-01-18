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
package org.eclipse.smarthome.automation.module.core.handler;

import java.util.List;
import java.util.Map;

import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.handler.ActionHandler;
import org.eclipse.smarthome.automation.handler.BaseModuleHandler;
import org.eclipse.smarthome.config.core.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a handler for RunRuleAction module type. It runs the rules
 * which's UIDs are passed by the 'ruleUIDs' property. If a rule's status is not
 * IDLE that rule can not run!
 *
 * <pre>
 *Example:
 *
 *"id": "RuleAction",
 *"type": "core.RunRuleAction",
 *"configuration": {
 *     "ruleUIDs": ["UID1", "UID2", "UID3"]
 * }
 * </pre>
 *
 * @author Benedikt Niehues - Initial contribution
 * @author Kai Kreuzer - use rule engine instead of registry
 *
 */
public class RunRuleActionHandler extends BaseModuleHandler<Action> implements ActionHandler {

    /**
     * The UID for this handler for identification in the factory.
     */
    public static final String UID = "core.RunRuleAction";

    /**
     * the key for the 'rulesUIDs' property of the {@link Action}.
     */
    private static final String RULE_UIDS_KEY = "ruleUIDs";
    private static final String CONSIDER_CONDITIONS_KEY = "considerConditions";

    /**
     * The logger
     */
    private final Logger logger = LoggerFactory.getLogger(RunRuleActionHandler.class);

    /**
     * the UIDs of the rules to be executed.
     */
    private final List<String> ruleUIDs;

    /**
     * boolean to express if the conditions should be considered, defaults to
     * true;
     */
    private boolean considerConditions = true;

    @SuppressWarnings("unchecked")
    public RunRuleActionHandler(final Action module) {
        super(module);
        final Configuration config = module.getConfiguration();
        if (config.getProperties().isEmpty()) {
            throw new IllegalArgumentException("'Configuration' can not be empty.");
        }

        ruleUIDs = (List<String>) config.get(RULE_UIDS_KEY);
        if (ruleUIDs == null) {
            throw new IllegalArgumentException("'ruleUIDs' property must not be null.");
        }
        if (config.get(CONSIDER_CONDITIONS_KEY) != null && config.get(CONSIDER_CONDITIONS_KEY) instanceof Boolean) {
            this.considerConditions = ((Boolean) config.get(CONSIDER_CONDITIONS_KEY)).booleanValue();
        }
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> context) {
        // execute each rule after the other; at the moment synchronously
        for (String uid : ruleUIDs) {
            if (callback != null) {
                callback.runNow(uid, considerConditions, context);
            } else {
                logger.warn("Action is not applied to {} because rule engine is not available.", uid);
            }
        }
        // no outputs from this module
        return null;
    }
}
