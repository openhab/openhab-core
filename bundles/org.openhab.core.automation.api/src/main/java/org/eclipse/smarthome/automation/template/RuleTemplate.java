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
package org.eclipse.smarthome.automation.template;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.Condition;
import org.eclipse.smarthome.automation.Module;
import org.eclipse.smarthome.automation.Rule;
import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.Visibility;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;

/**
 * This class is used to define {@code Rule Templates} which are shared combination of ready to use modules, which can
 * be configured to produce {@link Rule} instances.
 * <p>
 * The {@link RuleTemplate}s can be used by any creator of Rules, but they can be modified only by its creator. The
 * template modification is done by updating the {@link RuleTemplate}.
 * <p>
 * Templates can have {@code tags} - non-hierarchical keywords or terms for describing them.
 *
 * @author Yordan Mihaylov - Initial Contribution
 * @author Ana Dimova - Initial Contribution
 * @author Vasil Ilchev - Initial Contribution
 * @author Markus Rathgeb - Add default constructor for deserialization
 */
@NonNullByDefault
public class RuleTemplate implements Template {

    /**
     * Holds the {@link RuleTemplate}'s identifier, specified by its creator or randomly generated.
     */
    private final String uid;

    /**
     * Holds a list with the {@link Trigger}s participating in the {@link RuleTemplate}.
     */
    private final List<Trigger> triggers;

    /**
     * Holds a list with the {@link Condition}s participating in the {@link RuleTemplate}.
     */
    private final List<Condition> conditions;

    /**
     * Holds a list with the {@link Action}s participating in the {@link RuleTemplate}.
     */
    private final List<Action> actions;

    /**
     * Holds a set of non-hierarchical keywords or terms for describing the {@link RuleTemplate}.
     */
    private final Set<String> tags;

    /**
     * Holds the short, human-readable label of the {@link RuleTemplate}.
     */
    @Nullable
    private final String label;

    /**
     * Describes the usage of the {@link RuleTemplate} and its benefits.
     */
    @Nullable
    private final String description;

    /**
     * Determines {@link Visibility} of the {@link RuleTemplate}.
     */
    private final Visibility visibility;

    /**
     * Defines a set of configuration properties of the future {@link Rule} instances.
     */
    private final List<ConfigDescriptionParameter> configDescriptions;

    /**
     * Creates a {@link RuleTemplate} instance that will be used for creating {@link Rule}s from a set of modules,
     * belong to the template. When {@code null} is passed for the {@code uid} parameter, the {@link RuleTemplate}'s
     * identifier will be randomly generated.
     *
     * @param uid                the {@link RuleTemplate}'s identifier, or {@code null} if a random identifier should be
     *                           generated.
     * @param label              the short human-readable {@link RuleTemplate}'s label.
     * @param description        a detailed human-readable {@link RuleTemplate}'s description.
     * @param tags               the {@link RuleTemplate}'s assigned tags.
     * @param triggers           the {@link RuleTemplate}'s triggers list, or {@code null} if the {@link RuleTemplate}
     *                           should have no triggers.
     * @param conditions         the {@link RuleTemplate}'s conditions list, or {@code null} if the {@link RuleTemplate}
     *                           should have no conditions.
     * @param actions            the {@link RuleTemplate}'s actions list, or {@code null} if the {@link RuleTemplate}
     *                           should have no actions.
     * @param configDescriptions describing meta-data for the configuration of the future {@link Rule} instances.
     * @param visibility         the {@link RuleTemplate}'s visibility.
     */
    public RuleTemplate(@Nullable String UID, @Nullable String label, @Nullable String description,
            @Nullable Set<String> tags, @Nullable List<Trigger> triggers, @Nullable List<Condition> conditions,
            @Nullable List<Action> actions, @Nullable List<ConfigDescriptionParameter> configDescriptions,
            @Nullable Visibility visibility) {
        this.uid = UID == null ? UUID.randomUUID().toString() : UID;
        this.label = label;
        this.description = description;
        this.triggers = triggers == null ? Collections.emptyList() : Collections.unmodifiableList(triggers);
        this.conditions = conditions == null ? Collections.emptyList() : Collections.unmodifiableList(conditions);
        this.actions = actions == null ? Collections.emptyList() : Collections.unmodifiableList(actions);
        this.configDescriptions = configDescriptions == null ? Collections.emptyList()
                : Collections.unmodifiableList(configDescriptions);
        this.visibility = visibility == null ? Visibility.VISIBLE : visibility;
        this.tags = tags == null ? Collections.emptySet() : Collections.unmodifiableSet(tags);
    }

    /**
     * Gets the unique identifier of the {@link RuleTemplate}. It can be specified by the {@link RuleTemplate}'s
     * creator, or randomly generated.
     *
     * @return an identifier of this {@link RuleTemplate}. Can't be {@code null}.
     */
    @Override
    public String getUID() {
        return uid;
    }

    /**
     * Gets the {@link RuleTemplate}'s assigned tags.
     *
     * @return the {@link RuleTemplate}'s assigned tags.
     */
    @Override
    public Set<String> getTags() {
        return tags;
    }

    /**
     * Gets the {@link RuleTemplate}'s human-readable label.
     *
     * @return the {@link RuleTemplate}'s human-readable label, or {@code null} if not specified.
     */
    @Override
    public @Nullable String getLabel() {
        return label;
    }

    /**
     * Gets the human-readable description of the purpose of the {@link RuleTemplate}.
     *
     * @return the {@link RuleTemplate}'s human-readable description, or {@code null}.
     */
    @Override
    public @Nullable String getDescription() {
        return description;
    }

    /**
     * Gets the {@link RuleTemplate}'s {@link Visibility}.
     *
     * @return the {@link RuleTemplate}'s {@link Visibility} value.
     */
    @Override
    public Visibility getVisibility() {
        return visibility;
    }

    /**
     * Gets the {@link List} with {@link ConfigDescriptionParameter}s defining meta info for configuration properties of
     * the future {@link Rule} instances.
     *
     * @return a {@link List} of {@link ConfigDescriptionParameter}s.
     */
    public List<ConfigDescriptionParameter> getConfigurationDescriptions() {
        return configDescriptions;
    }

    /**
     * Gets a {@link Module} participating in the {@link RuleTemplate}.
     *
     * @param moduleId unique identifier of a module in this {@link RuleTemplate}.
     * @return module with specified identifier or {@code null} when such does not exist.
     */
    public @Nullable Module getModule(String moduleId) {
        for (Module module : getModules(Module.class)) {
            if (module.getId().equals(moduleId)) {
                return module;
            }
        }
        return null;
    }

    /**
     * Gets the modules of the {@link RuleTemplate}, corresponding to the specified class.
     *
     * @param moduleClazz defines the class of the looking modules. It can be {@link Module}, {@link Trigger},
     *                    {@link Condition} or {@link Action}.
     * @return the modules of defined type or empty list if the {@link RuleTemplate} has no modules that belong to the
     *         specified type.
     */
    @SuppressWarnings("unchecked")
    public <T extends Module> List<T> getModules(Class<T> moduleClazz) {
        final List<T> result;
        if (Module.class == moduleClazz) {
            List<Module> modules = new ArrayList<>();
            modules.addAll(triggers);
            modules.addAll(conditions);
            modules.addAll(actions);
            result = (List<T>) Collections.unmodifiableList(modules);
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

    /**
     * Gets the triggers participating in {@link RuleTemplate}.
     *
     * @return a list with the triggers that belong to this {@link RuleTemplate}.
     */
    public List<Trigger> getTriggers() {
        return triggers;
    }

    /**
     * Gets the conditions participating in {@link RuleTemplate}.
     *
     * @return a list with the conditions that belong to this {@link RuleTemplate}.
     */
    public List<Condition> getConditions() {
        return conditions;
    }

    /**
     * Gets the actions participating in {@link RuleTemplate}.
     *
     * @return a list with the actions that belong to this {@link RuleTemplate}.
     */
    public List<Action> getActions() {
        return actions;
    }

    /**
     * Returns the hash code of this object depends on the hash code of the UID that it owns.
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + uid.hashCode();
        return result;
    }

    /**
     * Two objects are equal if they own equal UIDs.
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof RuleTemplate)) {
            return false;
        }
        RuleTemplate other = (RuleTemplate) obj;
        if (!uid.equals(other.uid)) {
            return false;
        }
        return true;
    }
}
