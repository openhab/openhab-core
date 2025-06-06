/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.automation.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.Visibility;
import org.openhab.core.automation.template.RuleTemplate;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.Configuration;

/**
 * This is the internal implementation of a {@link Rule}, which comes with full getters and setters.
 *
 * @author Yordan Mihaylov - Initial contribution
 * @author Ana Dimova - Initial contribution
 * @author Vasil Ilchev - Initial contribution
 * @author Kai Kreuzer - Introduced transient status and made it implement the Rule interface
 */
@NonNullByDefault
public class RuleImpl implements Rule {

    protected List<Trigger> triggers;
    protected List<Condition> conditions;
    protected List<Action> actions;
    protected Configuration configuration;
    protected List<ConfigDescriptionParameter> configDescriptions;
    protected @Nullable String templateUID;
    protected TemplateState templateStatus;
    protected String uid;
    protected @Nullable String name;
    protected Set<String> tags;
    protected Visibility visibility;
    protected @Nullable String description;

    /**
     * Constructor for creating an empty {@link Rule} with a specified rule identifier.
     * When {@code null} is passed for the {@code uid} parameter, the {@link Rule}'s identifier will be randomly
     * generated.
     *
     * @param uid the rule's identifier, or {@code null} if a random identifier should be generated.
     */
    public RuleImpl(@Nullable String uid) {
        this(uid, null, null, null, null, null, null, null, null, null, TemplateState.NO_TEMPLATE, null);
    }

    /**
     * Utility constructor for creating a {@link Rule} from a set of modules, or from a template.
     * When {@code null} is passed for the {@code uid} parameter, the {@link Rule}'s identifier will be randomly
     * generated.
     *
     * @param uid the {@link Rule}'s identifier, or {@code null} if a random identifier should be generated.
     * @param name the rule's name
     * @param description the rule's description
     * @param tags the tags
     * @param triggers the {@link Rule}'s triggers list, or {@code null} if the {@link Rule} should have no triggers or
     *            will be created from a template.
     * @param conditions the {@link Rule}'s conditions list, or {@code null} if the {@link Rule} should have no
     *            conditions, or will be created from a template.
     * @param actions the {@link Rule}'s actions list, or {@code null} if the {@link Rule} should have no actions, or
     *            will be created from a template.
     * @param configDescriptions metadata describing the configuration of the {@link Rule}.
     * @param configuration the values that will configure the modules of the {@link Rule}.
     * @param templateUID the {@link RuleTemplate} identifier of the template that will be used by the
     *            {@link RuleRegistry} to validate the {@link Rule}'s configuration, as well as to create and configure
     *            the {@link Rule}'s modules, or null if the {@link Rule} should not be created from a template.
     * @param visibility the {@link Rule}'s visibility
     */
    public RuleImpl(@Nullable String uid, final @Nullable String name, final @Nullable String description,
            final @Nullable Set<String> tags, @Nullable List<Trigger> triggers, @Nullable List<Condition> conditions,
            @Nullable List<Action> actions, @Nullable List<ConfigDescriptionParameter> configDescriptions,
            @Nullable Configuration configuration, @Nullable String templateUID, TemplateState templateStatus,
            @Nullable Visibility visibility) {
        this.uid = uid == null ? UUID.randomUUID().toString() : uid;
        this.name = name;
        this.description = description;
        this.tags = tags == null ? Set.of() : Set.copyOf(tags);
        this.triggers = triggers == null ? List.of() : List.copyOf(triggers);
        this.conditions = conditions == null ? List.of() : List.copyOf(conditions);
        this.actions = actions == null ? List.of() : List.copyOf(actions);
        this.configDescriptions = configDescriptions == null ? List.of() : List.copyOf(configDescriptions);
        this.configuration = configuration == null ? new Configuration()
                : new Configuration(configuration.getProperties());
        this.templateUID = templateUID;
        this.templateStatus = templateStatus;
        this.visibility = visibility == null ? Visibility.VISIBLE : visibility;
    }

    @Override
    public String getUID() {
        return uid;
    }

    @Override
    public @Nullable String getTemplateUID() {
        return templateUID;
    }

    /**
     * This method is used to specify the {@link RuleTemplate} identifier of the template that will be used to by the
     * {@link RuleRegistry} to resolve the {@link RuleImpl}: to validate the {@link RuleImpl}'s configuration, as well
     * as to create and configure the {@link RuleImpl}'s modules.
     */
    public void setTemplateUID(@Nullable String templateUID) {
        this.templateUID = templateUID;
    }

    @Override
    public TemplateState getTemplateState() {
        return templateStatus;
    }

    /**
     * This method is used to specify the current rule template state.
     *
     * @param templateState the {@link TemplateState} to set.
     */
    public void setTemplateStatus(TemplateState templateState) {
        this.templateStatus = Objects.requireNonNull(templateState);
    }

    @Override
    public @Nullable String getName() {
        return name;
    }

    /**
     * This method is used to specify the {@link RuleImpl}'s human-readable name.
     *
     * @param ruleName the {@link RuleImpl}'s human-readable name, or {@code null}.
     */
    public void setName(@Nullable String ruleName) {
        name = ruleName;
    }

    @Override
    public Set<String> getTags() {
        return tags;
    }

    /**
     * This method is used to specify the {@link RuleImpl}'s assigned tags.
     *
     * @param ruleTags the {@link RuleImpl}'s assigned tags.
     */
    public void setTags(@Nullable Set<String> ruleTags) {
        tags = ruleTags == null ? Set.of() : Collections.unmodifiableSet(ruleTags);
    }

    @Override
    public @Nullable String getDescription() {
        return description;
    }

    /**
     * This method is used to specify human-readable description of the purpose and consequences of the
     * {@link RuleImpl}'s execution.
     *
     * @param ruleDescription the {@link RuleImpl}'s human-readable description, or {@code null}.
     */
    public void setDescription(@Nullable String ruleDescription) {
        description = ruleDescription;
    }

    @Override
    public Visibility getVisibility() {
        return visibility;
    }

    /**
     * This method is used to specify the {@link RuleImpl}'s {@link Visibility}.
     *
     * @param visibility the {@link RuleImpl}'s {@link Visibility} value.
     */
    public void setVisibility(@Nullable Visibility visibility) {
        this.visibility = visibility == null ? Visibility.VISIBLE : visibility;
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * This method is used to specify the {@link RuleImpl}'s {@link Configuration}.
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
     * This method is used to describe with {@link ConfigDescriptionParameter}s the meta info for configuration
     * properties of the {@link RuleImpl}.
     */
    public void setConfigurationDescriptions(@Nullable List<ConfigDescriptionParameter> configDescriptions) {
        this.configDescriptions = configDescriptions == null ? List.of()
                : Collections.unmodifiableList(configDescriptions);
    }

    @Override
    public List<Condition> getConditions() {
        return conditions;
    }

    /**
     * This method is used to specify the conditions participating in {@link RuleImpl}.
     *
     * @param conditions a list with the conditions that should belong to this {@link RuleImpl}.
     */
    public void setConditions(@Nullable List<Condition> conditions) {
        this.conditions = conditions == null ? List.of() : Collections.unmodifiableList(conditions);
    }

    @Override
    public List<Action> getActions() {
        return actions;
    }

    @Override
    public List<Trigger> getTriggers() {
        return triggers;
    }

    /**
     * This method is used to specify the actions participating in {@link RuleImpl}
     *
     * @param actions a list with the actions that should belong to this {@link RuleImpl}.
     */
    public void setActions(@Nullable List<Action> actions) {
        this.actions = actions == null ? List.of() : Collections.unmodifiableList(actions);
    }

    /**
     * This method is used to specify the triggers participating in {@link RuleImpl}
     *
     * @param triggers a list with the triggers that should belong to this {@link RuleImpl}.
     */
    public void setTriggers(@Nullable List<Trigger> triggers) {
        this.triggers = triggers == null ? List.of() : Collections.unmodifiableList(triggers);
    }

    @Override
    public List<Module> getModules() {
        List<Module> modules = new ArrayList<>();
        modules.addAll(triggers);
        modules.addAll(conditions);
        modules.addAll(actions);
        return Collections.unmodifiableList(modules);
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
        if (!(obj instanceof RuleImpl)) {
            return false;
        }
        RuleImpl other = (RuleImpl) obj;
        if (!uid.equals(other.uid)) {
            return false;
        }
        return true;
    }
}
