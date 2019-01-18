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
import org.eclipse.smarthome.automation.RuleRegistry;
import org.eclipse.smarthome.automation.handler.ActionHandler;
import org.eclipse.smarthome.automation.handler.BaseModuleHandler;
import org.eclipse.smarthome.config.core.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a handler for RuleEnablementAction module type.
 * It enables or disables the rules which's UIDs are passed by the 'ruleUIDs' property.
 * !!! If a rule's status is NOT_INITIALIZED that rule can't be enabled. !!!
 *
 * <pre>
 *Example:
 *
 *"id": "RuleAction",
 *"type": "core.RuleEnablementAction",
 *"configuration": {
 *     "enable": true,
 *     "ruleUIDs": ["UID1", "UID2", "UID3"]
 * }
 * </pre>
 *
 * @author Plamen Peev - Initial contribution and API
 * @author Kai Kreuzer - use rule engine instead of registry
 *
 */
public class RuleEnablementActionHandler extends BaseModuleHandler<Action> implements ActionHandler {

    /**
     * This filed contains the type of this handler so it can be recognized from the factory.
     */
    public static final String UID = "core.RuleEnablementAction";

    /**
     * This field is a key to the 'enable' property of the {@link Action}.
     */
    private static final String ENABLE_KEY = "enable";

    /**
     * This field is a key to the 'rulesUIDs' property of the {@link Action}.
     */
    private static final String RULE_UIDS_KEY = "ruleUIDs";

    /**
     * This logger is used to log warning message if at some point {@link RuleRegistry} service becomes unavailable.
     */
    private final Logger logger = LoggerFactory.getLogger(RuleEnablementActionHandler.class);

    /**
     * This field stores the UIDs of the rules to which the action will be applied.
     */
    private final List<String> UIDs;

    /**
     * This field stores the value for the setEnabled() method of {@link RuleRegistry}.
     */
    private final boolean enable;

    @SuppressWarnings("unchecked")
    public RuleEnablementActionHandler(final Action module) {
        super(module);
        final Configuration config = module.getConfiguration();

        final Boolean enable = (Boolean) config.get(ENABLE_KEY);
        if (enable == null) {
            throw new IllegalArgumentException("'enable' property can not be null.");
        }
        this.enable = enable.booleanValue();

        UIDs = (List<String>) config.get(RULE_UIDS_KEY);
        if (UIDs == null) {
            throw new IllegalArgumentException("'ruleUIDs' property can not be null.");
        }
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> context) {
        for (String uid : UIDs) {
            if (callback != null) {
                callback.setEnabled(uid, enable);
            } else {
                logger.warn("Action is not applied to {} because rule engine is not available.", uid);
            }
        }
        return null;
    }
}
