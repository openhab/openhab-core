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
package org.openhab.core.model.yaml.internal.rules;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Rule.TemplateState;
import org.openhab.core.automation.Visibility;
import org.openhab.core.automation.internal.ActionImpl;
import org.openhab.core.automation.internal.ConditionImpl;
import org.openhab.core.automation.internal.TriggerImpl;
import org.openhab.core.common.AbstractUID;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.model.yaml.YamlElement;
import org.openhab.core.model.yaml.YamlElementName;

/**
 * The {@link YamlRuleDTO} is a data transfer object used to serialize a rule in a YAML configuration file.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@YamlElementName("rules")
public class YamlRuleDTO implements YamlElement, Cloneable {

    protected static final Pattern UID_SEGMENT_PATTERN = Pattern.compile("[a-zA-Z0-9_][a-zA-Z0-9_-]*");

    public String uid;
    public String template;
    public TemplateState templateState;
    public String label;
    public Set<@NonNull String> tags;
    public String description;
    public Visibility visibility;
    public Map<@NonNull String, @NonNull Object> config;
    public List<@NonNull ConfigDescriptionParameter> configDescriptions;
    public List<@NonNull ConditionImpl> conditions;
    public List<@NonNull ActionImpl> actions;
    public List<@NonNull TriggerImpl> triggers;

    /**
     * Creates a new instance.
     */
    public YamlRuleDTO() {
    }

    @Override
    public @NonNull String getId() {
        return uid == null ? "" : uid;
    }

    @Override
    public void setId(@NonNull String id) {
        uid = id;
    }

    @Override
    public YamlElement cloneWithoutId() {
        YamlRuleDTO copy;
        try {
            copy = (YamlRuleDTO) super.clone();
            copy.uid = null;
            return copy;
        } catch (CloneNotSupportedException e) {
            // Will never happen
            return new YamlRuleDTO();
        }
    }

    @Override
    public boolean isValid(@Nullable List<@NonNull String> errors, @Nullable List<@NonNull String> warnings) {
        // Check that uid is present
        if (uid == null || uid.isBlank()) {
            addToList(errors, "invalid rule: uid is missing");
            return false;
        }
        boolean ok = true;
        // Check that uid only contains valid characters
        String[] segments = uid.split(AbstractUID.SEPARATOR);
        for (String segment : segments) {
            if (!UID_SEGMENT_PATTERN.matcher(segment).matches()) {
                addToList(errors, "invalid rule \"%s\": segment \"%s\" in the uid doesn't match the expected syntax %s"
                        .formatted(uid, segment, UID_SEGMENT_PATTERN.pattern()));
                ok = false;
            }
        }

        // Check that name is present
        if (label == null || label.isBlank()) {
            addToList(errors, "invalid rule \"%s\": label is missing".formatted(uid));
            ok = false;
        }

        // Check that the rule either has configuration (rule stub) or that it has at least one module
        if ((config == null || config.isEmpty()) && (triggers == null || triggers.isEmpty())
                && (conditions == null || conditions.isEmpty()) && (actions == null || actions.isEmpty())) {
            addToList(errors, "invalid rule \"%s\": the rule is empty".formatted(uid));
            ok = false;
        }
        return ok;
    }

    private void addToList(@Nullable List<@NonNull String> list, String value) {
        if (list != null) {
            list.add(value);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(actions, conditions, config, configDescriptions, description, label, tags, template,
                templateState, triggers, uid, visibility);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof YamlRuleDTO)) {
            return false;
        }
        YamlRuleDTO other = (YamlRuleDTO) obj;
        return Objects.equals(actions, other.actions) && Objects.equals(conditions, other.conditions)
                && Objects.equals(config, other.config) && Objects.equals(configDescriptions, other.configDescriptions)
                && Objects.equals(description, other.description) && Objects.equals(label, other.label)
                && Objects.equals(tags, other.tags) && Objects.equals(template, other.template)
                && templateState == other.templateState && Objects.equals(triggers, other.triggers)
                && Objects.equals(uid, other.uid) && visibility == other.visibility;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("YamlRuleDTO [");
        if (uid != null) {
            builder.append("uid=").append(uid).append(", ");
        }
        if (template != null) {
            builder.append("template=").append(template).append(", ");
        }
        if (templateState != null) {
            builder.append("templateState=").append(templateState).append(", ");
        }
        if (label != null) {
            builder.append("label=").append(label).append(", ");
        }
        if (tags != null) {
            builder.append("tags=").append(tags).append(", ");
        }
        if (description != null) {
            builder.append("description=").append(description).append(", ");
        }
        if (visibility != null) {
            builder.append("visibility=").append(visibility).append(", ");
        }
        if (config != null) {
            builder.append("config=").append(config).append(", ");
        }
        if (configDescriptions != null) {
            builder.append("configDescriptions=").append(configDescriptions).append(", ");
        }
        if (conditions != null) {
            builder.append("conditions=").append(conditions).append(", ");
        }
        if (actions != null) {
            builder.append("actions=").append(actions).append(", ");
        }
        if (triggers != null) {
            builder.append("triggers=").append(triggers);
        }
        builder.append("]");
        return builder.toString();
    }
}
