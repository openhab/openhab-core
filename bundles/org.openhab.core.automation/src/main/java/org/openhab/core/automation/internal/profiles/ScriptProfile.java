/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.core.automation.internal.profiles;

import java.util.HashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.RuleManager;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.items.Item;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.StateProfile;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ScriptProfile} delegates its actions to scripts.
 *
 * Scripts are just rules that are tagged "Script". If they have any triggers
 * or conditions, they are ignored. `callback`, `item`, and `command` or `state` (as
 * appropriate) are provided as context to the script. If script returns `true`
 * then the default action (as if it were a {@link SystemDefaultProfile}) is
 * taken; otherwise no other actions are taken and it's up to the script to
 * take an action with callback.
 *
 * @author Cody Cutrer - Initial contribution
 */
@NonNullByDefault
public class ScriptProfile implements StateProfile {
    public static final ProfileTypeUID PROFILE_TYPE_UID = new ProfileTypeUID(ProfileTypeUID.SYSTEM_SCOPE, "script");

    private static final String ON_COMMAND_FROM_ITEM_PARAM = "onCommandFromItem";
    private static final String ON_STATE_UPDATE_FROM_HANDLER_PARAM = "onStateUpdateFromHandler";
    private static final String ON_COMMAND_FROM_HANDLER_PARAM = "onCommandFromHandler";
    private static final String ON_STATE_UPDATE_FROM_ITEM_PARAM = "onStateUpdateFromItem";

    private static final String CALLBACK_VAR = "callback";
    private static final String COMMAND_VAR = "command";
    private static final String ITEM_VAR = "item";
    private static final String STATE_VAR = "state";
    private static final String RESULT_VAR_SUFFIX = ".result";

    public static final String SCRIPT_TAG = "Script";

    private final Logger logger = LoggerFactory.getLogger(ScriptProfile.class);

    private final ProfileCallback callback;
    private final @Nullable Item item;
    private final RuleManager ruleManager;
    private final RuleRegistry ruleRegistry;
    private @Nullable final String onCommandFromItemParam, onStateUpdateFromHandlerParam, onCommandFromHandlerParam,
            onStateUpdateFromItemParam;

    public ScriptProfile(ProfileCallback callback, ProfileContext context, RuleManager ruleManager,
            RuleRegistry ruleRegistry) {
        this.callback = callback;
        this.item = context.getItem();
        this.ruleManager = ruleManager;
        this.ruleRegistry = ruleRegistry;

        var config = context.getConfiguration();
        onCommandFromItemParam = getParam(config, ON_COMMAND_FROM_ITEM_PARAM);
        onStateUpdateFromHandlerParam = getParam(config, ON_STATE_UPDATE_FROM_HANDLER_PARAM);
        onCommandFromHandlerParam = getParam(config, ON_COMMAND_FROM_HANDLER_PARAM);
        onStateUpdateFromItemParam = getParam(config, ON_STATE_UPDATE_FROM_ITEM_PARAM);
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return PROFILE_TYPE_UID;
    }

    @Override
    public void onCommandFromItem(Command command) {
        if (runScript(ON_COMMAND_FROM_ITEM_PARAM, onCommandFromItemParam, COMMAND_VAR, command)) {
            callback.handleCommand(command);
        }
    }

    @Override
    public void onStateUpdateFromHandler(State state) {
        if (runScript(ON_STATE_UPDATE_FROM_HANDLER_PARAM, onStateUpdateFromHandlerParam, STATE_VAR, state)) {
            callback.sendUpdate(state);
        }
    }

    @Override
    public void onCommandFromHandler(Command command) {
        if (runScript(ON_COMMAND_FROM_HANDLER_PARAM, onCommandFromHandlerParam, COMMAND_VAR, command)) {
            callback.sendCommand(command);
        }
    }

    @Override
    public void onStateUpdateFromItem(State state) {
        runScript(ON_STATE_UPDATE_FROM_ITEM_PARAM, onStateUpdateFromItemParam, STATE_VAR, state);
    }

    private @Nullable String getParam(Configuration config, String paramName) {
        Object param = config.get(paramName);
        if (param != null && !(param instanceof String)) {
            logger.error("Parameter '{}' must be a String. Profile will be inactive.", paramName);
            return null;
        }

        if (param != null) {
            logger.debug("Profile configured with {}='{}'", paramName, param);
        }

        return (String) param;
    }

    private boolean runScript(String callbackName, @Nullable String scriptName, String variableName,
            Type variableValue) {
        if (scriptName == null || scriptName.isEmpty()) {
            return true;
        }

        var rule = ruleRegistry.get(scriptName);
        if (rule != null && !rule.getTags().contains(SCRIPT_TAG)) {
            rule = null;
        }
        if (rule == null) {
            logger.warn("Script '{}' is missing for callback {}. Taking no action.", scriptName, callbackName);
            // It was properly configured, so don't run the default actions.
            return false;
        }
        var context = new HashMap<String, Object>();
        context.put(CALLBACK_VAR, callback);
        context.put(ITEM_VAR, item);
        context.put(variableName, variableValue);

        ruleManager.runNow(scriptName, false, context);

        for (var entry : context.entrySet()) {
            if (entry.getKey().endsWith(RESULT_VAR_SUFFIX) && entry.getValue() instanceof Boolean) {
                boolean result = (Boolean) entry.getValue();
                if (result) {
                    logger.debug("Script '{}' returned true; executing default action.", scriptName);
                } else {
                    logger.debug("Script '{}' returned false; skipping default action.", scriptName);
                }
                return result;
            }
        }
        return false;
    }
}
