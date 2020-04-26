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
package org.openhab.core.automation.internal.ruleengine;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleStatus;
import org.openhab.core.automation.RuleStatusDetail;
import org.openhab.core.automation.RuleStatusInfo;
import org.openhab.core.automation.handler.ModuleHandler;

/**
 * This class holds the information that is necessary for the rule engine.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public class WrappedRule {

    private static <T extends WrappedModule, U extends Module> List<T> map(final List<U> in, Function<U, T> factory,
            final Collection<WrappedModule<Module, ModuleHandler>> coll) {
        // explicit cast to List <? extends T> as JDK compiler complains
        return Collections.unmodifiableList((List<? extends T>) in.stream().map(module -> {
            final T impl = factory.apply(module);
            coll.add(impl);
            return impl;
        }).collect(Collectors.toList()));
    }

    private final Rule rule;

    private RuleStatusInfo statusInfo = new RuleStatusInfo(RuleStatus.UNINITIALIZED, RuleStatusDetail.NONE);

    private final List<WrappedModule<Module, ModuleHandler>> modules;
    private final List<WrappedAction> actions;
    private final List<WrappedCondition> conditions;
    private final List<WrappedTrigger> triggers;

    public WrappedRule(final Rule rule) {
        this.rule = rule;
        final LinkedList<WrappedModule<Module, ModuleHandler>> modules = new LinkedList<>();
        this.actions = map(rule.getActions(), WrappedAction::new, modules);
        this.conditions = map(rule.getConditions(), WrappedCondition::new, modules);
        this.triggers = map(rule.getTriggers(), WrappedTrigger::new, modules);
        this.modules = Collections.unmodifiableList(modules);
    }

    public final String getUID() {
        return rule.getUID();
    }

    public final Rule unwrap() {
        return rule;
    }

    public RuleStatusInfo getStatusInfo() {
        return statusInfo;
    }

    public void setStatusInfo(final RuleStatusInfo statusInfo) {
        this.statusInfo = statusInfo;
    }

    public List<WrappedAction> getActions() {
        return actions;
    }

    public List<WrappedCondition> getConditions() {
        return conditions;
    }

    public List<WrappedTrigger> getTriggers() {
        return triggers;
    }

    public List<WrappedModule<Module, ModuleHandler>> getModules() {
        return modules;
    }
}
