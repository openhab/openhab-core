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
package org.openhab.core.automation;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.RegistryChangeListener;

/**
 * This class is responsible to provide a {@link RegistryChangeListener} logic. A instance of it is added to
 * {@link RuleRegistry} service, to listen for changes when a single {@link Rule} has been added, updated, enabled,
 * disabled or removed and to involve Rule Engine to process these changes. Also to send a {@code run} command
 * for a single {@link Rule} to the Rule Engine.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public interface RuleManager {

    /**
     * This method gets <b>enabled</b> {@link RuleStatus} for a {@link Rule}.
     * The <b>enabled</b> rule statuses are {@link RuleStatus#UNINITIALIZED}, {@link RuleStatus#IDLE} and
     * {@link RuleStatus#RUNNING}.
     * The <b>disabled</b> rule status is {@link RuleStatus#DISABLED}.
     *
     * @param ruleUID UID of the {@link Rule}
     * @return {@code true} when the {@link RuleStatus} is one of the {@link RuleStatus#UNINITIALIZED},
     *         {@link RuleStatus#IDLE} and {@link RuleStatus#RUNNING}, {@code false} when it is
     *         {@link RuleStatus#DISABLED} and {@code null} when it is not available.
     */
    @Nullable
    Boolean isEnabled(String ruleUID);

    /**
     * This method is used for changing <b>enabled</b> state of the {@link Rule}.
     * The <b>enabled</b> rule statuses are {@link RuleStatus#UNINITIALIZED}, {@link RuleStatus#IDLE} and
     * {@link RuleStatus#RUNNING}.
     * The <b>disabled</b> rule status is {@link RuleStatus#DISABLED}.
     *
     * @param uid the unique identifier of the {@link Rule}.
     * @param isEnabled a new <b>enabled / disabled</b> state of the {@link Rule}.
     */
    void setEnabled(String uid, boolean isEnabled);

    /**
     * This method gets {@link RuleStatusInfo} of the specified {@link Rule}.
     *
     * @param ruleUID UID of the {@link Rule}
     * @return {@link RuleStatusInfo} object containing status of the looking {@link Rule} or null when a rule with
     *         specified UID does not exists.
     */
    @Nullable
    RuleStatusInfo getStatusInfo(String ruleUID);

    /**
     * Utility method which gets {@link RuleStatus} of the specified {@link Rule}.
     *
     * @param ruleUID UID of the {@link Rule}
     * @return {@link RuleStatus} object containing status of the looking {@link Rule} or null when a rule with
     *         specified UID does not exists.
     */
    @Nullable
    RuleStatus getStatus(String ruleUID);

    /**
     * The method skips the triggers and the conditions and directly executes the actions of the rule.
     * This should always be possible unless an action has a mandatory input that is linked to a trigger.
     * In that case the action is skipped and the rule engine continues execution of rest actions.
     *
     * @param ruleUID id of the rule whose actions have to be executed.
     */
    void runNow(String uid);

    /**
     * Same as {@link #runNow(String)} with the additional option to enable/disable evaluation of
     * conditions defined in the target rule. The context can be set here, too, but also might be {@code null}.
     *
     * @param ruleUID id of the rule whose actions have to be executed.
     * @param considerConditions if {@code true} the conditions of the rule will be checked.
     * @param context the context that is passed to the conditions and the actions of the rule.
     */
    void runNow(String uid, boolean considerConditions, @Nullable Map<String, Object> context);
}
