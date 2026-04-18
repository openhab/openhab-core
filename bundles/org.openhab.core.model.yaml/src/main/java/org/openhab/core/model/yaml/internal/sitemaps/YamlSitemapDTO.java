/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.model.yaml.internal.sitemaps;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.dto.ModularDTO;
import org.openhab.core.io.dto.SerializationException;
import org.openhab.core.model.yaml.YamlElement;
import org.openhab.core.model.yaml.YamlElementName;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The {@link YamlSitemapDTO} is a data transfer object used to serialize a UI sitemap in a YAML configuration file.
 *
 * @author Laurent Garnier - Initial contribution
 */
@YamlElementName("sitemaps")
public class YamlSitemapDTO implements ModularDTO<YamlSitemapDTO, ObjectMapper, JsonNode>, YamlElement, Cloneable {

    private static final String NAME_PATTERN = "^[a-zA-Z0-9_]+$";

    public String name;
    public String label;
    public String icon;
    public List<YamlWidgetDTO> widgets;

    public YamlSitemapDTO() {
    }

    @Override
    public @NonNull String getId() {
        return name == null ? "" : name;
    }

    @Override
    public void setId(@NonNull String id) {
        name = id;
    }

    @Override
    public YamlElement cloneWithoutId() {
        YamlSitemapDTO copy;
        try {
            copy = (YamlSitemapDTO) super.clone();
            copy.name = null;
            return copy;
        } catch (CloneNotSupportedException e) {
            // Will never happen
            return new YamlSitemapDTO();
        }
    }

    @Override
    public boolean isValid(@Nullable List<@NonNull String> errors, @Nullable List<@NonNull String> warnings) {
        // Check that name is present
        if (name == null || name.isBlank()) {
            addToList(errors, "invalid sitemap: name missing while mandatory");
            return false;
        }
        boolean ok = true;
        if (!name.matches(NAME_PATTERN)) {
            addToList(errors,
                    "invalid sitemap \"%s\": name must contain alphanumeric characters and underscores, and must not contain any other symbols."
                            .formatted(name));
            ok = false;
        }
        if (widgets != null && !widgets.isEmpty()) {
            boolean containsFrames = false;
            boolean containsOtherWidgets = false;
            for (int i = 0; i < widgets.size(); i++) {
                String id = "%d/%d".formatted(i + 1, widgets.size());
                YamlWidgetDTO widget = widgets.get(i);
                if (widget.type != null) {
                    containsFrames |= "Frame".equals(widget.type);
                    containsOtherWidgets |= !"Frame".equals(widget.type);
                }
                String wType = widget.type == null ? "?" : widget.type;
                if ("Button".equals(widget.type)) {
                    addToList(warnings,
                            "sitemap \"%s\": should not contain Button, Buttons are only allowed in Buttongrid"
                                    .formatted(name));
                }
                List<String> widgetErrors = new ArrayList<>();
                List<String> widgetWarnings = new ArrayList<>();
                ok &= widget.isValid(widgetErrors, widgetWarnings);
                widgetErrors.forEach(error -> {
                    addToList(errors,
                            "invalid sitemap \"%s\": widget %s of type %s: %s".formatted(name, id, wType, error));
                });
                widgetWarnings.forEach(warning -> {
                    addToList(warnings, "sitemap \"%s\": widget %s of type %s: %s".formatted(name, id, wType, warning));
                });
            }
            if (containsFrames && containsOtherWidgets) {
                addToList(warnings, "sitemap \"%s\": should contain either only Frames or none at all".formatted(name));
            }
        }
        return ok;
    }

    private void addToList(@Nullable List<@NonNull String> list, String value) {
        if (list != null) {
            list.add(value);
        }
    }

    @Override
    public @NonNull YamlSitemapDTO toDto(@NonNull JsonNode node, @NonNull ObjectMapper mapper)
            throws SerializationException {
        YamlPartialSitemapDTO partial;
        YamlSitemapDTO result = new YamlSitemapDTO();
        try {
            partial = mapper.treeToValue(node, YamlPartialSitemapDTO.class);
            result.name = partial.name;
            result.label = partial.label;
            result.icon = partial.icon;
            if (partial.widgets != null) {
                List<YamlWidgetDTO> widgets = new ArrayList<>(partial.widgets.size());
                for (YamlPartialWidgetDTO widget : partial.widgets) {
                    widgets.add(toWidgetDto(widget));
                }
                result.widgets = widgets;
            }
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new SerializationException(e.getMessage(), e);
        }
        return result;
    }

    private @NonNull YamlWidgetDTO toWidgetDto(@NonNull YamlPartialWidgetDTO partial) throws SerializationException {
        JsonNode objNode;
        YamlWidgetDTO result = new YamlWidgetDTO();
        result.type = partial.type;
        result.item = partial.item;
        result.mappings = partial.mappings;
        result.switchSupport = partial.switchSupport;
        result.releaseOnly = partial.releaseOnly;
        result.height = partial.height;
        result.min = partial.min;
        result.max = partial.max;
        result.step = partial.step;
        result.hint = partial.hint;
        result.url = partial.url;
        result.refresh = partial.refresh;
        result.encoding = partial.encoding;
        result.period = partial.period;
        result.service = partial.service;
        result.legend = partial.legend;
        result.forceAsItem = partial.forceAsItem;
        result.yAxisDecimalPattern = partial.yAxisDecimalPattern;
        result.interpolation = partial.interpolation;
        result.row = partial.row;
        result.column = partial.column;
        result.command = partial.command;
        result.releaseCommand = partial.releaseCommand;
        result.stateless = partial.stateless;

        if (partial.label != null) {
            if (partial.label.isValueNode()) {
                String label = partial.label.asText();
                String format = null;
                if (label != null) {
                    int idx = label.indexOf("[");
                    if (idx >= 0) {
                        format = label.substring(idx + 1, label.length() - 1).trim();
                        label = label.substring(0, idx).trim();
                    }
                }
                YamlWidgetLabelDTO widgetLabel = new YamlWidgetLabelDTO();
                widgetLabel.label = label;
                widgetLabel.format = format;
                result.label = widgetLabel;
            } else if (partial.label instanceof ObjectNode objectNode) {
                YamlWidgetLabelDTO widgetLabel = new YamlWidgetLabelDTO();
                widgetLabel.label = toStringDto(objectNode, "label");
                widgetLabel.format = toStringDto(objectNode, "format");
                if ((objNode = objectNode.get("labelColor")) != null) {
                    if (objNode.isValueNode()) {
                        widgetLabel.labelColor = objNode.asText();
                    } else if (objNode.isArray()) {
                        widgetLabel.labelColor = toRulesDto(objNode);
                    } else {
                        widgetLabel.labelColor = toRuleDto(objNode);
                    }
                }
                if ((objNode = objectNode.get("valueColor")) != null) {
                    if (objNode.isValueNode()) {
                        widgetLabel.valueColor = objNode.asText();
                    } else if (objNode.isArray()) {
                        widgetLabel.valueColor = toRulesDto(objNode);
                    } else {
                        widgetLabel.valueColor = toRuleDto(objNode);
                    }
                }
                result.label = widgetLabel;
            }
        }

        if (partial.icon != null) {
            if (partial.icon.isValueNode()) {
                YamlWidgetIconDTO widgetIcon = new YamlWidgetIconDTO();
                widgetIcon.name = partial.icon.asText();
                result.icon = widgetIcon;
            } else if (partial.icon instanceof ObjectNode objectNode) {
                YamlWidgetIconDTO widgetIcon = new YamlWidgetIconDTO();
                if ((objNode = objectNode.get("name")) != null) {
                    if (objNode.isValueNode()) {
                        widgetIcon.name = objNode.asText();
                    } else if (objNode.isArray()) {
                        widgetIcon.name = toRulesDto(objNode);
                    } else {
                        widgetIcon.name = toRuleDto(objNode);
                    }
                }
                if ((objNode = objectNode.get("static")) != null && objNode.isBoolean()) {
                    widgetIcon.staticIcon = objNode.asBoolean();
                }
                if ((objNode = objectNode.get("color")) != null) {
                    if (objNode.isValueNode()) {
                        widgetIcon.color = objNode.asText();
                    } else if (objNode.isArray()) {
                        widgetIcon.color = toRulesDto(objNode);
                    } else {
                        widgetIcon.color = toRuleDto(objNode);
                    }
                }
                result.icon = widgetIcon;
            }
        }

        if (partial.visibility != null) {
            if (!partial.visibility.isContainerNode()) {
                throw new SerializationException("Expected \"visibility\" to be a container node");
            }
            if (partial.visibility.isArray()) {
                result.visibility = toRulesDto(partial.visibility);
            } else {
                result.visibility = toRuleDto(partial.visibility);
            }
        }

        if (partial.widgets != null) {
            List<YamlWidgetDTO> widgets = new ArrayList<>(partial.widgets.size());
            for (YamlPartialWidgetDTO widget : partial.widgets) {
                widgets.add(toWidgetDto(widget));
            }
            result.widgets = widgets;
        }

        return result;
    }

    private @NonNull List<Object> toRulesDto(@NonNull JsonNode rulesNodes) throws SerializationException {
        List<Object> rules = new ArrayList<>(rulesNodes.size());
        for (Iterator<JsonNode> iterator = rulesNodes.elements(); iterator.hasNext();) {
            rules.add(toRuleDto(iterator.next()));
        }
        return rules;
    }

    private @NonNull Object toRuleDto(@NonNull JsonNode ruleNode) throws SerializationException {
        JsonNode conditionsNode, conditionNode;
        if (!ruleNode.isObject()) {
            throw new SerializationException("Expected rule to be an object node");
        }
        if ((conditionsNode = ruleNode.get("and")) != null) {
            if (!conditionsNode.isArray()) {
                throw new SerializationException("Expected \"and\" to be an array node");
            }
            YamlRuleWithAndConditionsDTO rule = new YamlRuleWithAndConditionsDTO();
            List<YamlConditionDTO> conditions = new ArrayList<>(conditionsNode.size());
            for (Iterator<JsonNode> iterator2 = conditionsNode.elements(); iterator2.hasNext();) {
                conditionNode = iterator2.next();
                if (!conditionNode.isObject()) {
                    throw new SerializationException("Expected condition to be an object node");
                }
                YamlConditionDTO condition = new YamlConditionDTO();
                condition.item = toStringDto(conditionNode, "item");
                condition.operator = toStringDto(conditionNode, "operator");
                condition.argument = toStringDto(conditionNode, "argument");
                conditions.add(condition);
            }
            rule.and = conditions;
            rule.value = toStringDto(ruleNode, "value");
            return rule;
        } else {
            YamlRuleWithUniqueConditionDTO rule = new YamlRuleWithUniqueConditionDTO();
            rule.item = toStringDto(ruleNode, "item");
            rule.operator = toStringDto(ruleNode, "operator");
            rule.argument = toStringDto(ruleNode, "argument");
            rule.value = toStringDto(ruleNode, "value");
            return rule;
        }
    }

    private @Nullable String toStringDto(@NonNull JsonNode objectNode, String field) throws SerializationException {
        JsonNode valueNode;
        if ((valueNode = objectNode.get(field)) != null) {
            if (valueNode.isValueNode()) {
                return valueNode.asText();
            } else {
                throw new SerializationException("Expected \"" + field + "\" to be a value node");
            }
        }
        return null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, label, icon, widgets);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        YamlSitemapDTO other = (YamlSitemapDTO) obj;
        return Objects.equals(name, other.name) && Objects.equals(label, other.label)
                && Objects.equals(icon, other.icon) && Objects.equals(widgets, other.widgets);
    }

    protected static class YamlPartialSitemapDTO {
        public String name;
        public String label;
        public String icon;
        public List<YamlPartialWidgetDTO> widgets;
    }

    protected static class YamlPartialWidgetDTO {
        public String type;
        public String item;
        public JsonNode label;
        public JsonNode icon;
        public List<YamlMappingDTO> mappings;
        public Boolean switchSupport;
        public Boolean releaseOnly;
        public Integer height;
        public BigDecimal min;
        public BigDecimal max;
        @JsonAlias({ "stepsize" })
        public BigDecimal step;
        public String hint;
        public String url;
        public Integer refresh;
        public String encoding;
        public String period;
        public String service;
        public Boolean legend;
        public Boolean forceAsItem;
        public String yAxisDecimalPattern;
        public String interpolation;
        public Integer row;
        public Integer column;
        public String command;
        public String releaseCommand;
        public Boolean stateless;
        public JsonNode visibility;
        public List<YamlPartialWidgetDTO> widgets;
    }
}
