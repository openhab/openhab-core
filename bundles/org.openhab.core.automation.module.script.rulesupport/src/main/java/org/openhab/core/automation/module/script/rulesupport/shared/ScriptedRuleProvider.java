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
package org.openhab.core.automation.module.script.rulesupport.shared;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleProvider;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.osgi.service.component.annotations.Component;

/**
 * This RuleProvider keeps Rules at added by scripts during the runtime. This ensures that Rules are not kept on reboot,
 * but have to be added by the scripts again.
 *
 * @author Simon Merschjohann - Initial contribution
 */
@Component(immediate = true, service = { ScriptedRuleProvider.class, RuleProvider.class })
public class ScriptedRuleProvider implements RuleProvider {
    private final Collection<ProviderChangeListener<Rule>> listeners = new ArrayList<>();

    private final Map<String, Rule> rules = new HashMap<>();

    @Override
    public void addProviderChangeListener(ProviderChangeListener<Rule> listener) {
        listeners.add(listener);
    }

    @Override
    public Collection<Rule> getAll() {
        return rules.values();
    }

    @Override
    public void removeProviderChangeListener(ProviderChangeListener<Rule> listener) {
        listeners.remove(listener);
    }

    public void addRule(Rule rule) {
        rules.put(rule.getUID(), rule);

        for (ProviderChangeListener<Rule> providerChangeListener : listeners) {
            providerChangeListener.added(this, rule);
        }
    }

    public void removeRule(String ruleUID) {
        removeRule(rules.get(ruleUID));
    }

    public void removeRule(Rule rule) {
        for (ProviderChangeListener<Rule> providerChangeListener : listeners) {
            providerChangeListener.removed(this, rule);
        }
    }

}
