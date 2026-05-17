/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.model.script.helper;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleManager;
import org.openhab.core.model.script.ScriptServiceUtil;

/**
 * {@link ItemExtensions} provides DSL {@link Rule} extensions.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class RuleExtensions {

    /**
     * Run the specified rule.
     *
     * @param rule the {@link Rule} to run.
     * @return A copy of the rule context, including possible return values.
     * @throws IllegalStateException If no {@link RuleManager} instance exists.
     */
    public static Map<String, Object> run(Rule rule) {
        RuleManager ruleManager = ScriptServiceUtil.getRuleManager();
        if (ruleManager == null) {
            throw new IllegalStateException("RuleManager doesn't exist");
        }
        String ruleUID = rule.getUID();
        if (ruleManager.getStatus(ruleUID) == null) {
            throw new IllegalArgumentException("Rule with UID '" + ruleUID + "' doesn't exist");
        }
        return ruleManager.runNow(ruleUID);
    }

    /**
     * Run the specified rule while optionally taking conditions into account.
     *
     * @param rule the {@link Rule} to run.
     * @param considerConditions {@code true} to not run the rule if its conditions don't qualify.
     * @return A copy of the rule context, including possible return values.
     * @throws IllegalStateException If no {@link RuleManager} instance exists.
     */
    public static Map<String, Object> run(Rule rule, boolean considerConditions) {
        RuleManager ruleManager = ScriptServiceUtil.getRuleManager();
        if (ruleManager == null) {
            throw new IllegalStateException("RuleManager doesn't exist");
        }
        String ruleUID = rule.getUID();
        if (ruleManager.getStatus(ruleUID) == null) {
            throw new IllegalArgumentException("Rule with UID '" + ruleUID + "' doesn't exist");
        }
        return ruleManager.runNow(ruleUID, considerConditions, null);
    }

    /**
     * Run the specified rule with the specified context.
     *
     * @param rule the {@link Rule} to run.
     * @param context the {@link Map} of {@link String} and {@link Object} pairs that constitutes the context.
     * @return A copy of the rule context, including possible return values.
     * @throws IllegalStateException If no {@link RuleManager} instance exists.
     */
    public static Map<String, Object> run(Rule rule, Map<String, Object> context) {
        RuleManager ruleManager = ScriptServiceUtil.getRuleManager();
        if (ruleManager == null) {
            throw new IllegalStateException("RuleManager doesn't exist");
        }
        String ruleUID = rule.getUID();
        if (ruleManager.getStatus(ruleUID) == null) {
            throw new IllegalArgumentException("Rule with UID '" + ruleUID + "' doesn't exist");
        }
        return ruleManager.runNow(ruleUID, false, context);
    }

    /**
     * Run the specified rule with the specified context, while optionally taking conditions into account.
     *
     * @param rule the {@link Rule} to run.
     * @param considerConditions {@code true} to not run the rule if its conditions don't qualify.
     * @param context the pairs of {@link String}s and {@link Object}s that constitutes the context. Must be in pairs,
     *            the first is the key, the second is the value.
     * @return A copy of the rule context, including possible return values.
     * @throws IllegalStateException If no {@link RuleManager} instance exists.
     */
    public static Map<String, Object> run(Rule rule, boolean considerConditions, Object... context) {
        return Rules.runRule(rule.getUID(), considerConditions, context);
    }

    /**
     * Run the specified rule with the specified context, while optionally taking conditions into account.
     *
     * @param rule the {@link Rule} to run.
     * @param considerConditions {@code true} to not run the rule if its conditions don't qualify.
     * @param context the {@link Map} of {@link String} and {@link Object} pairs that constitutes the context.
     * @return A copy of the rule context, including possible return values.
     * @throws IllegalStateException If no {@link RuleManager} instance exists.
     */
    public static Map<String, Object> run(Rule rule, boolean considerConditions,
            @Nullable Map<String, Object> context) {
        RuleManager ruleManager = ScriptServiceUtil.getRuleManager();
        if (ruleManager == null) {
            throw new IllegalStateException("RuleManager doesn't exist");
        }
        String ruleUID = rule.getUID();
        if (ruleManager.getStatus(ruleUID) == null) {
            throw new IllegalArgumentException("Rule with UID '" + ruleUID + "' doesn't exist");
        }
        return ruleManager.runNow(ruleUID, considerConditions, context);
    }

    /**
     * Check whether the specified rule is enabled.
     *
     * @param rule the {@link Rule} to check.
     * @return {@code true} if the rule is enabled, {@code false} otherwise.
     * @throws IllegalStateException If no {@link RuleManager} instance exists.
     */
    public static boolean isEnabled(Rule rule) {
        RuleManager ruleManager = ScriptServiceUtil.getRuleManager();
        if (ruleManager == null) {
            throw new IllegalStateException("RuleManager doesn't exist");
        }
        String ruleUID = rule.getUID();
        Boolean result = ruleManager.isEnabled(ruleUID);
        if (result == null) {
            throw new IllegalArgumentException("Rule with UID '" + ruleUID + "' doesn't exist");
        }
        return result.booleanValue();
    }

    /**
     * Set whether the specified rule is enabled.
     *
     * @param rule the {@link Rule} to enable or disable.
     * @param enabled {@code true} to enable the rule, {@code false} to disable the rule.
     * @throws IllegalStateException If no {@link RuleManager} instance exists.
     */
    public static void setEnabled(Rule rule, boolean enabled) {
        RuleManager ruleManager = ScriptServiceUtil.getRuleManager();
        if (ruleManager == null) {
            throw new IllegalStateException("RuleManager doesn't exist");
        }
        ruleManager.setEnabled(rule.getUID(), enabled);
    }
}
