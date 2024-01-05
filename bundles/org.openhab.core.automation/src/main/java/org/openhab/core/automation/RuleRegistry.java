/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.Registry;

/**
 * The {@link RuleRegistry} provides basic functionality for managing {@link Rule}s.
 * It can be used to
 * <ul>
 * <li>Add Rules with the {@link #add(Rule)} method.</li>
 * <li>Get the existing rules with the {@link #getByTag(String)}, {@link #getByTags(String[])} methods.</li>
 * <li>Update the existing rules with the {@link #update} method.</li>
 * <li>Remove Rules with the {@link #remove(Object)} method.</li>
 * <li>Manage the state (<b>enabled</b> or <b>disabled</b>) of the Rules:
 * <ul>
 * <li>A newly added Rule is always <b>enabled</b>.</li>
 * <li>To check a Rule's state, use the {@link RuleManager#isEnabled(String)} method.</li>
 * <li>To change a Rule's state, use the {@link RuleManager#setEnabled(String, boolean)} method.</li>
 * </ul>
 * </li>
 * </ul>
 * <p>
 * The {@link RuleRegistry} manages the status of the Rules:
 * <ul>
 * <li>To check a Rule's status info, use the {@link RuleManager#getStatusInfo(String)} method.</li>
 * <li>The status of a Rule enabled with {@link RuleManager#setEnabled(String, boolean)}, is first set to
 * {@link RuleStatus#UNINITIALIZED}.</li>
 * <li>After a Rule is enabled, a verification procedure is initiated. If the verification of the modules IDs,
 * connections between modules and configuration values of the modules is successful, and the module handlers are
 * correctly set, the status is set to {@link RuleStatus#IDLE}.</li>
 * <li>If some of the module handlers disappear, the Rule will become {@link RuleStatus#UNINITIALIZED} again.</li>
 * <li>If one of the Rule's Triggers is triggered, the Rule becomes {@link RuleStatus#RUNNING}.
 * When the execution is complete, it will become {@link RuleStatus#IDLE} again.</li>
 * <li>If a Rule is disabled with {@link RuleManager#setEnabled(String, boolean)}, it's status is set to
 * {@link RuleStatusDetail#DISABLED}.</li>
 * </ul>
 *
 * @author Yordan Mihaylov - Initial contribution
 */
@NonNullByDefault
public interface RuleRegistry extends Registry<Rule, String> {

    /**
     * This method is used to register a {@link Rule} into the {@link RuleRegistry}. First the {@link Rule} become
     * {@link RuleStatus#UNINITIALIZED}.
     * Then verification procedure will be done and the Rule become {@link RuleStatus#IDLE}.
     * If the verification fails, the Rule will stay {@link RuleStatus#UNINITIALIZED}.
     *
     * @param rule a {@link Rule} instance which have to be added into the {@link RuleRegistry}.
     * @return a copy of the added {@link Rule}
     * @throws IllegalArgumentException when a rule with the same UID already exists or some of the conditions or
     *             actions has wrong format of input reference.
     * @throws IllegalStateException when the RuleManagedProvider is unavailable.
     */
    @Override
    Rule add(Rule rule);

    /**
     * Gets a collection of {@link Rule}s which shares same tag.
     *
     * @param tag specifies a tag that will filter the rules.
     * @return collection of {@link Rule}s having specified tag.
     */
    Collection<Rule> getByTag(@Nullable String tag);

    /**
     * Gets a collection of {@link Rule}s which has specified tags.
     *
     * @param tags specifies tags that will filter the rules.
     * @return collection of {@link Rule}s having specified tags.
     */
    Collection<Rule> getByTags(String... tags);
}
