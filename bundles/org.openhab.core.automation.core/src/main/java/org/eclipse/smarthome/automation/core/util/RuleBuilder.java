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
package org.eclipse.smarthome.automation.core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.Condition;
import org.eclipse.smarthome.automation.Rule;
import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.Visibility;
import org.eclipse.smarthome.automation.core.internal.RuleImpl;
import org.eclipse.smarthome.automation.template.RuleTemplate;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.Configuration;

/**
 * This class allows the easy construction of a {@link Rule} instance using the builder pattern.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
@NonNullByDefault
public class RuleBuilder {

    private @NonNullByDefault({}) List<Trigger> triggers;
    private @NonNullByDefault({}) List<Condition> conditions;
    private @NonNullByDefault({}) List<Action> actions;
    private @NonNullByDefault({}) Configuration configuration;
    private @NonNullByDefault({}) List<ConfigDescriptionParameter> configDescriptions;
    private @Nullable String templateUID;
    private @NonNullByDefault({}) final String uid;
    private @Nullable String name;
    private @NonNullByDefault({}) Set<String> tags;
    private @NonNullByDefault({}) Visibility visibility;
    private @Nullable String description;

    protected RuleBuilder(Rule rule) {
        this.triggers = new LinkedList<>(rule.getTriggers());
        this.conditions = new LinkedList<>(rule.getConditions());
        this.actions = new LinkedList<>(rule.getActions());
        this.configuration = new Configuration(rule.getConfiguration());
        this.configDescriptions = new LinkedList<>(rule.getConfigurationDescriptions());
        this.templateUID = rule.getTemplateUID();
        this.uid = rule.getUID();
        this.name = rule.getName();
        this.tags = new HashSet<>(rule.getTags());
        this.visibility = rule.getVisibility();
        this.description = rule.getDescription();
    }

    public static RuleBuilder create(String ruleId) {
        Rule rule = new RuleImpl(ruleId);
        return new RuleBuilder(rule);
    }

    public static RuleBuilder create(Rule r) {
        return create(r.getUID()).withActions(r.getActions()).withConditions(r.getConditions())
                .withTriggers(r.getTriggers()).withConfiguration(r.getConfiguration())
                .withConfigurationDescriptions(r.getConfigurationDescriptions()).withDescription(r.getDescription())
                .withName(r.getName()).withTags(r.getTags());
    }

    public static RuleBuilder create(RuleTemplate template, String uid, @Nullable String name,
            Configuration configuration, Visibility visibility) {
        return create(uid).withActions(template.getActions()).withConditions(template.getConditions())
                .withTriggers(template.getTriggers()).withConfiguration(configuration)
                .withConfigurationDescriptions(template.getConfigurationDescriptions())
                .withDescription(template.getDescription()).withName(name).withTags(template.getTags());
    }

    public RuleBuilder withName(@Nullable String name) {
        this.name = name;
        return this;
    }

    public RuleBuilder withDescription(@Nullable String description) {
        this.description = description;
        return this;
    }

    public RuleBuilder withTemplateUID(@Nullable String uid) {
        this.templateUID = uid;
        return this;
    }

    public RuleBuilder withVisibility(@Nullable Visibility visibility) {
        this.visibility = visibility;
        return this;
    }

    public RuleBuilder withTriggers(@Nullable Trigger... triggers) {
        return withTriggers(Arrays.asList(triggers));
    }

    public RuleBuilder withTriggers(@Nullable List<? extends Trigger> triggers) {
        if (triggers != null) {
            ArrayList<Trigger> triggerList = new ArrayList<>(triggers.size());
            triggers.forEach(t -> triggerList.add(TriggerBuilder.create(t).build()));
            this.triggers = triggerList;
        }
        return this;
    }

    public RuleBuilder withConditions(@Nullable Condition... conditions) {
        return withConditions(Arrays.asList(conditions));
    }

    public RuleBuilder withConditions(@Nullable List<? extends Condition> conditions) {
        if (conditions != null) {
            ArrayList<Condition> conditionList = new ArrayList<>(conditions.size());
            conditions.forEach(c -> conditionList.add(ConditionBuilder.create(c).build()));
            this.conditions = conditionList;
        }
        return this;
    }

    public RuleBuilder withActions(@Nullable Action... actions) {
        return withActions(Arrays.asList(actions));
    }

    public RuleBuilder withActions(@Nullable List<? extends Action> actions) {
        if (actions != null) {
            ArrayList<Action> actionList = new ArrayList<>(actions.size());
            actions.forEach(a -> actionList.add(ActionBuilder.create(a).build()));
            this.actions = actionList;
        }
        return this;
    }

    public RuleBuilder withTags(String... tags) {
        withTags(new HashSet<>(Arrays.asList(tags)));
        return this;
    }

    public RuleBuilder withTags(@Nullable Set<String> tags) {
        this.tags = tags != null ? new HashSet<>(tags) : Collections.emptySet();
        return this;
    }

    public RuleBuilder withConfiguration(@Nullable Configuration ruleConfiguration) {
        this.configuration = new Configuration(ruleConfiguration);
        return this;
    }

    public RuleBuilder withConfigurationDescriptions(@Nullable List<ConfigDescriptionParameter> configDescs) {
        this.configDescriptions = configDescs != null ? new LinkedList<>(configDescs) : Collections.emptyList();
        return this;
    }

    public Rule build() {
        return new RuleImpl(uid, name, description, tags, triggers, conditions, actions, configDescriptions,
                configuration, templateUID, visibility);
    }

}
