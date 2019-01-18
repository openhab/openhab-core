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
package org.eclipse.smarthome.automation.module.script.rulesupport.shared.simple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.Condition;
import org.eclipse.smarthome.automation.Module;
import org.eclipse.smarthome.automation.Rule;
import org.eclipse.smarthome.automation.RuleRegistry;
import org.eclipse.smarthome.automation.RuleStatus;
import org.eclipse.smarthome.automation.RuleStatusDetail;
import org.eclipse.smarthome.automation.RuleStatusInfo;
import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.Visibility;
import org.eclipse.smarthome.automation.template.RuleTemplate;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.Configuration;

/**
 * convenience Rule class with an action handler. This allows to define Rules which have a execution block.
 *
 * @author Simon Merschjohann - Initial contribution
 * @author Kai Kreuzer - made it implement Rule
 *
 */
@NonNullByDefault
public abstract class SimpleRule implements Rule, SimpleRuleActionHandler {

    @NonNullByDefault({})
    protected List<Trigger> triggers;
    @NonNullByDefault({})
    protected List<Condition> conditions;
    @NonNullByDefault({})
    protected List<Action> actions;
    @NonNullByDefault({})
    protected Configuration configuration;
    @NonNullByDefault({})
    protected List<ConfigDescriptionParameter> configDescriptions;
    @Nullable
    protected String templateUID;
    @NonNullByDefault({})
    protected String uid;
    @Nullable
    protected String name;
    @NonNullByDefault({})
    protected Set<String> tags;
    @NonNullByDefault({})
    protected Visibility visibility;
    @Nullable
    protected String description;

    protected transient volatile RuleStatusInfo status = new RuleStatusInfo(RuleStatus.UNINITIALIZED,
            RuleStatusDetail.NONE);

    public SimpleRule() {
    }

    @Override
    public String getUID() {
        return uid;
    }

    @Override
    @Nullable
    public String getTemplateUID() {
        return templateUID;
    }

    /**
     * This method is used to specify the {@link RuleTemplate} identifier of the template that will be used to
     * by the {@link RuleRegistry} to resolve the {@link Rule}: to validate the {@link Rule}'s configuration, as
     * well as to create and configure the {@link Rule}'s modules.
     */
    public void setTemplateUID(@Nullable String templateUID) {
        this.templateUID = templateUID;
    }

    @Override
    @Nullable
    public String getName() {
        return name;
    }

    /**
     * This method is used to specify the {@link Rule}'s human-readable name.
     *
     * @param ruleName the {@link Rule}'s human-readable name, or {@code null}.
     */
    public void setName(@Nullable String ruleName) {
        name = ruleName;
    }

    @Override
    public Set<String> getTags() {
        return tags;
    }

    /**
     * This method is used to specify the {@link Rule}'s assigned tags.
     *
     * @param ruleTags the {@link Rule}'s assigned tags.
     */
    public void setTags(@Nullable Set<String> ruleTags) {
        tags = ruleTags != null ? ruleTags : new HashSet<>();
    }

    @Override
    @Nullable
    public String getDescription() {
        return description;
    }

    /**
     * This method is used to specify human-readable description of the purpose and consequences of the
     * {@link Rule}'s execution.
     *
     * @param ruleDescription the {@link Rule}'s human-readable description, or {@code null}.
     */
    public void setDescription(@Nullable String ruleDescription) {
        description = ruleDescription;
    }

    @Override
    public Visibility getVisibility() {
        return visibility;
    }

    /**
     * This method is used to specify the {@link Rule}'s {@link Visibility}.
     *
     * @param visibility the {@link Rule}'s {@link Visibility} value.
     */
    public void setVisibility(@Nullable Visibility visibility) {
        this.visibility = visibility == null ? Visibility.VISIBLE : visibility;
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * This method is used to specify the {@link Rule}'s {@link Configuration}.
     *
     * @param ruleConfiguration the new configuration values.
     */
    public void setConfiguration(@Nullable Configuration ruleConfiguration) {
        this.configuration = ruleConfiguration == null ? new Configuration() : ruleConfiguration;
    }

    @Override
    public List<ConfigDescriptionParameter> getConfigurationDescriptions() {
        return configDescriptions;
    }

    /**
     * This method is used to describe with {@link ConfigDescriptionParameter}s
     * the meta info for configuration properties of the {@link Rule}.
     */
    public void setConfigurationDescriptions(@Nullable List<ConfigDescriptionParameter> configDescriptions) {
        this.configDescriptions = configDescriptions == null ? new ArrayList<>() : configDescriptions;
    }

    @Override
    public List<Condition> getConditions() {
        return conditions == null ? Collections.emptyList() : conditions;
    }

    /**
     * This method is used to specify the conditions participating in {@link Rule}.
     *
     * @param conditions a list with the conditions that should belong to this {@link Rule}.
     */
    public void setConditions(@Nullable List<Condition> conditions) {
        this.conditions = conditions;
    }

    @Override
    public List<Action> getActions() {
        return actions == null ? Collections.emptyList() : actions;
    }

    @Override
    public List<Trigger> getTriggers() {
        return triggers == null ? Collections.emptyList() : triggers;
    }

    /**
     * This method is used to specify the actions participating in {@link Rule}
     *
     * @param actions a list with the actions that should belong to this {@link Rule}.
     */
    public void setActions(@Nullable List<Action> actions) {
        this.actions = actions;
    }

    /**
     * This method is used to specify the triggers participating in {@link Rule}
     *
     * @param triggers a list with the triggers that should belong to this {@link Rule}.
     */
    public void setTriggers(@Nullable List<Trigger> triggers) {
        this.triggers = triggers;
    }

    @Override
    public List<Module> getModules() {
        final List<Module> result;
        List<Module> modules = new ArrayList<Module>();
        modules.addAll(triggers);
        modules.addAll(conditions);
        modules.addAll(actions);
        result = Collections.unmodifiableList(modules);
        return result;
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> List<T> getModules(@Nullable Class<T> moduleClazz) {
        final List<T> result;
        if (Module.class == moduleClazz) {
            result = (List<T>) getModules();
        } else if (Trigger.class == moduleClazz) {
            result = (List<T>) triggers;
        } else if (Condition.class == moduleClazz) {
            result = (List<T>) conditions;
        } else if (Action.class == moduleClazz) {
            result = (List<T>) actions;
        } else {
            result = Collections.emptyList();
        }
        return result;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + uid.hashCode();
        return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Rule)) {
            return false;
        }
        Rule other = (Rule) obj;
        if (!uid.equals(other.getUID())) {
            return false;
        }
        return true;
    }

}
