/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.automation.module.script.rulesupport.shared;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleProvider;
import org.openhab.core.common.registry.AbstractProvider;
import org.openhab.core.common.registry.ManagedProvider;
import org.osgi.service.component.annotations.Component;

/**
 * This RuleProvider keeps Rules added by scripts during runtime. This ensures that Rules are not kept on reboot,
 * but have to be added by the scripts again.
 *
 * @author Simon Merschjohann - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { ScriptedRuleProvider.class, RuleProvider.class })
public class ScriptedRuleProvider extends AbstractProvider<Rule>
        implements RuleProvider, ManagedProvider<Rule, String> {
    private final Map<String, Rule> rules = new HashMap<>();

    @Override
    public Collection<Rule> getAll() {
        return rules.values();
    }

    @Override
    public @Nullable Rule get(String ruleUID) {
        return rules.get(ruleUID);
    }

    @Override
    public void add(Rule rule) {
        rules.put(rule.getUID(), rule);

        notifyListenersAboutAddedElement(rule);
    }

    @Deprecated
    public void addRule(Rule rule) {
        add(rule);
    }

    @Override
    public @Nullable Rule update(Rule rule) {
        Rule oldRule = rules.get(rule.getUID());
        if (oldRule != null) {
            rules.put(rule.getUID(), rule);
            notifyListenersAboutUpdatedElement(oldRule, rule);
        }
        return oldRule;
    }

    @Override
    public @Nullable Rule remove(String ruleUID) {
        Rule rule = rules.remove(ruleUID);
        if (rule != null) {
            notifyListenersAboutRemovedElement(rule);
        }
        return rule;
    }

    @Deprecated
    public void removeRule(String ruleUID) {
        remove(ruleUID);
    }

    public void removeRule(Rule rule) {
        remove(rule.getUID());
    }
}
