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
package org.openhab.core.automation.module.script.rulesupport.shared;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleProvider;
import org.openhab.core.common.registry.AbstractProvider;
import org.osgi.service.component.annotations.Component;

/**
 * This RuleProvider keeps Rules added by scripts during runtime. This ensures that Rules are not kept on reboot,
 * but have to be added by the scripts again.
 *
 * @author Simon Merschjohann - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { ScriptedRuleProvider.class, RuleProvider.class })
public class ScriptedRuleProvider extends AbstractProvider<Rule> implements RuleProvider {
    private final Map<String, Rule> rules = new HashMap<>();

    @Override
    public Collection<Rule> getAll() {
        return rules.values();
    }

    public void addRule(Rule rule) {
        rules.put(rule.getUID(), rule);

        notifyListenersAboutAddedElement(rule);
    }

    public void removeRule(String ruleUID) {
        Rule rule = rules.get(ruleUID);
        if (rule != null) {
            removeRule(rule);
        }
    }

    public void removeRule(Rule rule) {
        rules.remove(rule.getUID());
        notifyListenersAboutRemovedElement(rule);
    }
}
