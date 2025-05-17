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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.Visibility;
import org.openhab.core.common.AbstractUID;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.io.dto.ModularDTO;
import org.openhab.core.io.dto.SerializationException;
import org.openhab.core.model.yaml.YamlElement;
import org.openhab.core.model.yaml.YamlElementName;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The {@link YamlRuleDTO} is a data transfer object used to serialize a rule in a YAML configuration file.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@YamlElementName("rules")
public class YamlRuleDTO implements ModularDTO<YamlRuleDTO, ObjectMapper, JsonNode>, YamlElement, Cloneable {

    protected static final Pattern UID_SEGMENT_PATTERN = Pattern.compile("[a-zA-Z0-9_][a-zA-Z0-9_-]*");

    public String uid; // TODO: (Nad) JavaDocs
    public String templateUid;
    public String label;
    public Set<@NonNull String> tags;
    public String description;
    public String visibility;
    public Map<@NonNull String, @NonNull Object> config;
    public List<@NonNull ConfigDescriptionParameter> configDescriptions;
    public List<@NonNull YamlConditionDTO> conditions;
    public List<@NonNull YamlActionDTO> actions;
    public List<@NonNull YamlModuleDTO> triggers;

    /**
     * Creates a new instance.
     */
    public YamlRuleDTO() {
    }

    public YamlRuleDTO(@NonNull Rule rule) {
        this.uid = rule.getUID();
        this.templateUid = rule.getTemplateUID();
        this.label = rule.getName();
        this.tags = rule.getTags();
        this.description = rule.getDescription();
        this.visibility = rule.getVisibility().name();
        this.config = rule.getConfiguration().getProperties();
        this.configDescriptions = rule.getConfigurationDescriptions();
        List<@NonNull Action> actions = rule.getActions();
        if (!actions.isEmpty()) {
            List<YamlActionDTO> actionDtos = new ArrayList<>(actions.size());
            for (Action action : actions) {
                actionDtos.add(new YamlActionDTO(action));
            }
            this.actions = actionDtos;
        }
        List<@NonNull Condition> conditions = rule.getConditions();
        if (!conditions.isEmpty()) {
            List<YamlConditionDTO> conditionsDtos = new ArrayList<>(conditions.size());
            for (Condition condition : conditions) {
                conditionsDtos.add(new YamlConditionDTO(condition));
            }
            this.conditions = conditionsDtos;
        }
        List<@NonNull Trigger> triggers = rule.getTriggers();
        if (!triggers.isEmpty()) {
            List<YamlModuleDTO> triggerDtos = new ArrayList<>(triggers.size());
            for (Trigger trigger : triggers) {
                triggerDtos.add(new YamlModuleDTO(trigger));
            }
            this.triggers = triggerDtos;
        }
    }

    @Override
    public @NonNull String getId() {
        return uid == null ? "" : uid;
    }

    @Override
    public void setId(@NonNull String id) {
        uid = id;
    }

    /**
     * Parses the string value in {@link #visibility} into a {@link Visibility} instance. Leading and trailing
     * whitespace is ignored, and so is letter case. If the parsing fails, this method returns
     * {@link Visibility#VISIBLE}.
     *
     * @return The resulting {@link Visibility}.
     */
    public Visibility getVisibility() {
        Visibility result = Visibility.typeOf(visibility);
        return result == null ? Visibility.VISIBLE : result;
    }

    @Override
    public @NonNull YamlRuleDTO toDto(@NonNull JsonNode node, @NonNull ObjectMapper mapper)
            throws SerializationException {
        YamlPartialRuleDTO partial;
        YamlRuleDTO result = new YamlRuleDTO();
        try {
            partial = mapper.treeToValue(node, YamlPartialRuleDTO.class);
            result.uid = partial.uid;
            result.templateUid = partial.templateUid;
            result.label = partial.label;
            result.tags = partial.tags;
            result.description = partial.description;
            result.visibility = partial.visibility;
            result.config = partial.config;
            result.configDescriptions = partial.configDescriptions;

            Map<@NonNull String, @NonNull Object> config;
            String translatedType;
            if (partial.actions != null && !partial.actions.isEmpty()) {
                if (!partial.actions.isArray()) {
                    throw new SerializationException("Expected actions to be an array node");
                }
                List<YamlActionDTO> actions = new ArrayList<>(partial.actions.size());
                JsonNode actionNode;
                YamlActionDTO action;
                for (Iterator<JsonNode> iterator = partial.actions.elements(); iterator.hasNext();) {
                    actionNode = iterator.next();
                    action = mapper.treeToValue(actionNode, YamlActionDTO.class);
                    action.type = ModuleTypeAliases.aliasToType(Action.class, action.type);
                    if ((config = action.config) != null && config.containsKey("script")
                            && config.get("type") instanceof String type) {
                        if (!type.equals(translatedType = MIMETypeAliases.aliasToType(type))) {
                            config.put("type", translatedType);
                        }
                    }
                    actions.add(action);
                }
                result.actions = actions;
            }
            if (partial.conditions != null && !partial.conditions.isEmpty()) {
                if (!partial.conditions.isArray()) {
                    throw new SerializationException("Expected conditions to be an array node");
                }
                List<YamlConditionDTO> conditions = new ArrayList<>(partial.conditions.size());
                JsonNode conditionNode;
                YamlConditionDTO condition;
                for (Iterator<JsonNode> iterator = partial.conditions.elements(); iterator.hasNext();) {
                    conditionNode = iterator.next();
                    condition = mapper.treeToValue(conditionNode, YamlConditionDTO.class);
                    condition.type = ModuleTypeAliases.aliasToType(Condition.class, condition.type);
                    if ((config = condition.config) != null && config.containsKey("script")
                            && config.get("type") instanceof String type) {
                        if (!type.equals(translatedType = MIMETypeAliases.aliasToType(type))) {
                            config.put("type", translatedType);
                        }
                    }
                    conditions.add(condition);
                }
                result.conditions = conditions;
            }
            if (partial.triggers != null && !partial.triggers.isEmpty()) {
                if (!partial.triggers.isArray()) {
                    throw new SerializationException("Expected triggers to be an array node");
                }
                List<YamlModuleDTO> triggers = new ArrayList<>(partial.triggers.size());
                JsonNode triggerNode;
                YamlModuleDTO trigger;
                for (Iterator<JsonNode> iterator = partial.triggers.elements(); iterator.hasNext();) {
                    triggerNode = iterator.next();
                    trigger = mapper.treeToValue(triggerNode, YamlModuleDTO.class);
                    trigger.type = ModuleTypeAliases.aliasToType(Trigger.class, trigger.type);
                    if ((config = trigger.config) != null && config.containsKey("script")
                            && config.get("type") instanceof String type) {
                        if (!type.equals(translatedType = MIMETypeAliases.aliasToType(type))) {
                            config.put("type", translatedType);
                        }
                    }
                    triggers.add(trigger);
                }
                result.triggers = triggers;
            }
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new SerializationException(e.getMessage(), e);
        }
        return result;
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
        return Objects.hash(actions, conditions, config, configDescriptions, description, label, tags, templateUid,
                triggers, uid, visibility);
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
                && Objects.equals(tags, other.tags) && Objects.equals(templateUid, other.templateUid)
                && Objects.equals(triggers, other.triggers) && Objects.equals(uid, other.uid)
                && visibility == other.visibility;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("YamlRuleDTO [");
        if (uid != null) {
            builder.append("uid=").append(uid).append(", ");
        }
        if (templateUid != null) {
            builder.append("templateUID=").append(templateUid).append(", ");
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

    protected static class YamlPartialRuleDTO {
        public String uid;
        public String templateUid;
        public String label;
        public Set<@NonNull String> tags;
        public String description;
        public String visibility;
        public Map<@NonNull String, @NonNull Object> config;
        public List<@NonNull ConfigDescriptionParameter> configDescriptions;
        public JsonNode conditions;
        public JsonNode actions;
        public JsonNode triggers;
    }
}
