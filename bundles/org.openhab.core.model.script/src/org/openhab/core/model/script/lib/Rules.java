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
package org.openhab.core.model.script.lib;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleManager;
import org.openhab.core.model.script.ScriptServiceUtil;
import org.openhab.core.model.script.internal.util.Utils;

/**
 * {@link Rules} provides DSL access to rule operations.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class Rules {

    /**
     * Get a rule by UID.
     *
     * @param ruleUid
     * @return The {@link Rule} or {@code null} if no matching rule exists.
     */
    public static @Nullable Rule getRule(String ruleUid) {
        return ScriptServiceUtil.getRuleRegistry().get(ruleUid);
    }

    /**
     * Run the rule with the specified UID.
     *
     * @param ruleUid the UID of the rule to run.
     * @return A copy of the rule context, including possible return values.
     * @throws IllegalArgumentException If a rule with the specified UID doesn't exist.
     * @throws IllegalStateException If no {@link RuleManager} instance exists.
     */
    public static Map<String, @Nullable Object> runRule(String ruleUid) {
        RuleManager ruleManager = ScriptServiceUtil.getRuleManager();
        if (ruleManager == null) {
            throw new IllegalStateException("RuleManager doesn't exist");
        }
        if (ruleManager.getStatus(ruleUid) == null) {
            throw new IllegalArgumentException("Rule with UID '" + ruleUid + "' doesn't exist");
        }
        return ruleManager.runNow(ruleUid);
    }

    /**
     * Run the rule with the specified UID while optionally taking conditions into account.
     *
     * @param ruleUid the UID of the rule to run.
     * @param considerConditions {@code true} to not run the rule if its conditions don't qualify.
     * @return A copy of the rule context, including possible return values.
     * @throws IllegalArgumentException If a rule with the specified UID doesn't exist.
     * @throws IllegalStateException If no {@link RuleManager} instance exists.
     */
    public static Map<String, @Nullable Object> runRule(String ruleUid, boolean considerConditions) {
        RuleManager ruleManager = ScriptServiceUtil.getRuleManager();
        if (ruleManager == null) {
            throw new IllegalStateException("RuleManager doesn't exist");
        }
        if (ruleManager.getStatus(ruleUid) == null) {
            throw new IllegalArgumentException("Rule with UID '" + ruleUid + "' doesn't exist");
        }
        return ruleManager.runNow(ruleUid, considerConditions, null);
    }

    /**
     * Run the rule with the specified UID with the specified context.
     *
     * @param ruleUid the UID of the rule to run.
     * @param context the {@link Map} of {@link String} and {@link Object} pairs that constitutes the context.
     * @return A copy of the rule context, including possible return values.
     * @throws IllegalArgumentException If a rule with the specified UID doesn't exist.
     * @throws IllegalStateException If no {@link RuleManager} instance exists.
     */
    public static Map<String, @Nullable Object> runRule(String ruleUid, Map<String, @Nullable Object> context) {
        RuleManager ruleManager = ScriptServiceUtil.getRuleManager();
        if (ruleManager == null) {
            throw new IllegalStateException("RuleManager doesn't exist");
        }
        if (ruleManager.getStatus(ruleUid) == null) {
            throw new IllegalArgumentException("Rule with UID '" + ruleUid + "' doesn't exist");
        }
        return ruleManager.runNow(ruleUid, false, context);
    }

    /**
     * Run the rule with the specified UID with the specified context, while optionally taking conditions into
     * account.
     *
     * @param ruleUid the UID of the rule to run.
     * @param considerConditions {@code true} to not run the rule if its conditions don't qualify.
     * @param context the pairs of {@link String}s and {@link Object}s that constitutes the context. Must be in pairs,
     *            the first is the key, the second is the value.
     * @return A copy of the rule context, including possible return values.
     * @throws IllegalArgumentException If a rule with the specified UID doesn't exist.
     * @throws IllegalStateException If no {@link RuleManager} instance exists.
     */
    public static Map<String, @Nullable Object> runRule(String ruleUid, boolean considerConditions, Object... context) {
        RuleManager ruleManager = ScriptServiceUtil.getRuleManager();
        if (ruleManager == null) {
            throw new IllegalStateException("RuleManager doesn't exist");
        }
        if (ruleManager.getStatus(ruleUid) == null) {
            throw new IllegalArgumentException("Rule with UID '" + ruleUid + "' doesn't exist");
        }
        return ruleManager.runNow(ruleUid, considerConditions, Utils.parseObjectArrayNullableValues(context));
    }

    /**
     * Run the rule with the specified UID with the specified context, while optionally taking conditions into
     * account.
     *
     * @param ruleUid the UID of the rule to run.
     * @param considerConditions {@code true} to not run the rule if its conditions don't qualify.
     * @param context the {@link Map} of {@link String} and {@link Object} pairs that constitutes the context.
     * @return A copy of the rule context, including possible return values.
     * @throws IllegalArgumentException If a rule with the specified UID doesn't exist.
     * @throws IllegalStateException If no {@link RuleManager} instance exists.
     */
    public static Map<String, @Nullable Object> runRule(String ruleUid, boolean considerConditions,
            @Nullable Map<String, @Nullable Object> context) {
        RuleManager ruleManager = ScriptServiceUtil.getRuleManager();
        if (ruleManager == null) {
            throw new IllegalStateException("RuleManager doesn't exist");
        }
        if (ruleManager.getStatus(ruleUid) == null) {
            throw new IllegalArgumentException("Rule with UID '" + ruleUid + "' doesn't exist");
        }
        return ruleManager.runNow(ruleUid, considerConditions, context);
    }

    /**
     * Check whether the specified rule is enabled.
     *
     * @param ruleUid the UID of the rule to check.
     * @return {@code true} if the rule is enabled, {@code false} otherwise.
     * @throws IllegalArgumentException If a rule with the specified UID doesn't exist.
     * @throws IllegalStateException If no {@link RuleManager} instance exists.
     */
    public static boolean isRuleEnabled(String ruleUid) {
        RuleManager ruleManager = ScriptServiceUtil.getRuleManager();
        if (ruleManager == null) {
            throw new IllegalStateException("RuleManager doesn't exist");
        }
        Boolean result = ruleManager.isEnabled(ruleUid);
        if (result == null) {
            throw new IllegalArgumentException("Rule with UID '" + ruleUid + "' doesn't exist");
        }
        return result.booleanValue();
    }

    /**
     * Set whether the specified rule is enabled.
     *
     * @param ruleUid the UID of the rule to enable or disable.
     * @param enabled {@code true} to enable the rule, {@code false} to disable the rule.
     * @throws IllegalArgumentException If a rule with the specified UID doesn't exist.
     * @throws IllegalStateException If no {@link RuleManager} instance exists.
     */
    public static void setRuleEnabled(String ruleUid, boolean enabled) {
        RuleManager ruleManager = ScriptServiceUtil.getRuleManager();
        if (ruleManager == null) {
            throw new IllegalStateException("RuleManager doesn't exist");
        }
        ruleManager.setEnabled(ruleUid, enabled);
    }

    /**
     * @return The {@link RuleManager} or {@code null}.
     */
    public static @Nullable RuleManager getRuleManager() {
        return ScriptServiceUtil.getRuleManager();
    }
}
