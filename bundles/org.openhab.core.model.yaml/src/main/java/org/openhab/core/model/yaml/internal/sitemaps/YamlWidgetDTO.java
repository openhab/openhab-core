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

import static org.openhab.core.sitemap.registry.SitemapFactory.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.ItemUtil;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * This is a data transfer object that is used to serialize widgets.
 *
 * @author Laurent Garnier - Initial contribution
 * @author Mark Herwege - Add support for nested sitemaps
 */
public class YamlWidgetDTO {

    private static final Set<String> ALLOWED_HINTS = Set.of("text", "number", "date", "time", "datetime");
    private static final Set<String> ALLOWED_INTERPOLATION = Set.of("linear", "step");
    private static final Set<String> LINKABLE_WIDGETS = Set.of(FRAME, BUTTON_GRID, GROUP, TEXT, IMAGE);

    private static final Map<String, Set<String>> MANDATORY_FIELDS = new HashMap<>();
    private static final Map<String, Set<String>> OPTIONAL_FIELDS = new HashMap<>();

    private static final String NESTED_SITEMAP = "Sitemap";

    static {
        MANDATORY_FIELDS.put(FRAME, Set.of());
        MANDATORY_FIELDS.put(BUTTON_GRID, Set.of());
        MANDATORY_FIELDS.put(BUTTON, Set.of("item", "row", "column", "command"));
        MANDATORY_FIELDS.put(GROUP, Set.of("item"));
        MANDATORY_FIELDS.put(TEXT, Set.of());
        MANDATORY_FIELDS.put(COLOR_PICKER, Set.of("item"));
        MANDATORY_FIELDS.put(COLOR_TEMPERATURE_PICKER, Set.of("item"));
        MANDATORY_FIELDS.put(INPUT, Set.of("item"));
        MANDATORY_FIELDS.put(SWITCH, Set.of("item"));
        MANDATORY_FIELDS.put(SELECTION, Set.of("item"));
        MANDATORY_FIELDS.put(SETPOINT, Set.of("item"));
        MANDATORY_FIELDS.put(SLIDER, Set.of("item"));
        MANDATORY_FIELDS.put(IMAGE, Set.of());
        MANDATORY_FIELDS.put(CHART, Set.of("item", "period"));
        MANDATORY_FIELDS.put(VIDEO, Set.of("url"));
        MANDATORY_FIELDS.put(MAPVIEW, Set.of("item"));
        MANDATORY_FIELDS.put(WEBVIEW, Set.of("url"));
        MANDATORY_FIELDS.put(NESTED_SITEMAP, Set.of());
        MANDATORY_FIELDS.put(DEFAULT, Set.of("item"));

        OPTIONAL_FIELDS.put(FRAME, Set.of("item"));
        OPTIONAL_FIELDS.put(BUTTON_GRID, Set.of());
        OPTIONAL_FIELDS.put(BUTTON, Set.of("releaseCommand", "stateless"));
        OPTIONAL_FIELDS.put(GROUP, Set.of());
        OPTIONAL_FIELDS.put(TEXT, Set.of("item"));
        OPTIONAL_FIELDS.put(COLOR_PICKER, Set.of());
        OPTIONAL_FIELDS.put(COLOR_TEMPERATURE_PICKER, Set.of("min", "max"));
        OPTIONAL_FIELDS.put(INPUT, Set.of("hint"));
        OPTIONAL_FIELDS.put(SWITCH, Set.of("mappings"));
        OPTIONAL_FIELDS.put(SELECTION, Set.of("mappings"));
        OPTIONAL_FIELDS.put(SETPOINT, Set.of("min", "max", "step"));
        OPTIONAL_FIELDS.put(SLIDER, Set.of("switchSupport", "releaseOnly", "min", "max", "step"));
        OPTIONAL_FIELDS.put(IMAGE, Set.of("item", "url", "refresh"));
        OPTIONAL_FIELDS.put(CHART,
                Set.of("refresh", "service", "legend", "forceAsItem", "yAxisDecimalPattern", "interpolation"));
        OPTIONAL_FIELDS.put(VIDEO, Set.of("item", "encoding"));
        OPTIONAL_FIELDS.put(MAPVIEW, Set.of("height"));
        OPTIONAL_FIELDS.put(WEBVIEW, Set.of("item", "height"));
        OPTIONAL_FIELDS.put(NESTED_SITEMAP, Set.of("item", "name"));
        OPTIONAL_FIELDS.put(DEFAULT, Set.of("height"));
    }

    public String type;
    public String item;
    public Object label;
    public Object icon;

    // widget-specific attributes
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
    public String name; // for NestedSitemap

    public Object visibility;

    public List<YamlWidgetDTO> widgets;

    public YamlWidgetDTO() {
    }

    public boolean isValid(@NonNull List<@NonNull String> errors, @NonNull List<@NonNull String> warnings) {
        if (type == null || type.isBlank()) {
            addToList(errors, "\"type\" field missing while mandatory");
            return false;
        }
        if (!MANDATORY_FIELDS.keySet().contains(type)) {
            addToList(errors, "invalid value \"%s\" for \"type\" field".formatted(type));
            return false;
        }

        boolean ok = true;

        if (label instanceof YamlWidgetLabelDTO widgetLabel) {
            ok &= widgetLabel.isValid(errors, warnings);
        }

        if (icon instanceof YamlWidgetIconDTO widgetIcon) {
            ok &= widgetIcon.isValid(errors, warnings);
        }

        ok &= isValidField("item", item, errors, warnings);
        ok &= isValidField("mappings", mappings, errors, warnings);
        ok &= isValidField("switchSupport", switchSupport, errors, warnings);
        ok &= isValidField("releaseOnly", releaseOnly, errors, warnings);
        ok &= isValidField("height", height, errors, warnings);
        ok &= isValidField("min", min, errors, warnings);
        ok &= isValidField("max", max, errors, warnings);
        ok &= isValidField("step", step, errors, warnings);
        ok &= isValidField("hint", hint, errors, warnings);
        ok &= isValidField("url", url, errors, warnings);
        ok &= isValidField("refresh", refresh, errors, warnings);
        ok &= isValidField("encoding", encoding, errors, warnings);
        ok &= isValidField("service", service, errors, warnings);
        ok &= isValidField("period", period, errors, warnings);
        ok &= isValidField("legend", legend, errors, warnings);
        ok &= isValidField("forceAsItem", forceAsItem, errors, warnings);
        ok &= isValidField("yAxisDecimalPattern", yAxisDecimalPattern, errors, warnings);
        ok &= isValidField("interpolation", interpolation, errors, warnings);
        ok &= isValidField("row", row, errors, warnings);
        ok &= isValidField("column", column, errors, warnings);
        ok &= isValidField("command", command, errors, warnings);
        ok &= isValidField("releaseCommand", releaseCommand, errors, warnings);
        ok &= isValidField("stateless", stateless, errors, warnings);
        ok &= isValidField("name", name, errors, warnings);

        if ((SWITCH.equals(type) || SELECTION.equals(type)) && mappings != null && !mappings.isEmpty()) {
            for (YamlMappingDTO mapping : mappings) {
                ok &= mapping.isValid(errors, warnings);
            }
        } else if ((SETPOINT.equals(type) || SLIDER.equals(type) || COLOR_TEMPERATURE_PICKER.equals(type))
                && min instanceof BigDecimal minValue && max instanceof BigDecimal maxValue
                && minValue.doubleValue() > maxValue.doubleValue()) {
            addToList(warnings, "larger value %f for \"min\" field than value %f for \"max\" field"
                    .formatted(minValue.doubleValue(), maxValue.doubleValue()));
        } else if (NESTED_SITEMAP.equals(type) && (name == null || name.isBlank())
                && (item == null || item.isBlank())) {
            addToList(errors, "\"name\" or \"item\" field missing while mandatory for Sitemap widget");
            ok = false;
        }

        List<String> ruleErrors = new ArrayList<>();
        List<String> ruleWarnings = new ArrayList<>();
        if (visibility instanceof List<?> rules) {
            for (Object r : rules) {
                if (r instanceof YamlRuleWithAndConditionsDTO rule) {
                    ok &= rule.isValid(ruleErrors, ruleWarnings);
                    if (rule.value != null) {
                        addToList(warnings, "rule in \"visibility\" field: unexpected \"value\" field is ignored");
                    }
                } else if (r instanceof YamlRuleWithUniqueConditionDTO rule) {
                    ok &= rule.isValid(ruleErrors, ruleWarnings);
                    if (rule.item == null && rule.operator == null && rule.argument == null) {
                        addToList(errors,
                                "invalid rule in \"visibility\" field: \"argument\" field missing while mandatory in condition");
                        ok = false;
                    }
                    if (rule.value != null) {
                        addToList(warnings, "rule in \"visibility\" field: unexpected \"value\" field is ignored");
                    }
                } else {
                    addToList(errors, "invalid type for rule in \"visibility\" field");
                    ok = false;
                }
            }
        } else if (visibility instanceof YamlRuleWithAndConditionsDTO rule) {
            ok &= rule.isValid(ruleErrors, ruleWarnings);
            if (rule.value != null) {
                addToList(warnings, "rule in \"visibility\" field: unexpected \"value\" field is ignored");
            }
        } else if (visibility instanceof YamlRuleWithUniqueConditionDTO rule) {
            ok &= rule.isValid(ruleErrors, ruleWarnings);
            if (rule.item == null && rule.operator == null && rule.argument == null) {
                addToList(errors,
                        "invalid rule in \"visibility\" field: \"argument\" field missing while mandatory in condition");
                ok = false;
            }
            if (rule.value != null) {
                addToList(warnings, "rule in \"visibility\" field: unexpected \"value\" field is ignored");
            }
        }
        ruleErrors.forEach(error -> {
            addToList(errors, "invalid rule in \"visibility\" field: %s".formatted(error));
        });
        ruleWarnings.forEach(warning -> {
            addToList(warnings, "rule in \"visibility\" field: %s".formatted(warning));
        });

        if (widgets != null) {
            if (!LINKABLE_WIDGETS.contains(type)) {
                addToList(errors, "unexpected sub-widgets in %s widget".formatted(type));
                ok = false;
            } else {
                boolean containsFrames = false;
                boolean containsOtherWidgets = false;
                Set<ButtonPosition> noVisibilityRulePositions = new HashSet<>();
                Set<ButtonPosition> visibilityRulePositions = new HashSet<>();
                for (int i = 0; i < widgets.size(); i++) {
                    String id = "%d/%d".formatted(i + 1, widgets.size());
                    YamlWidgetDTO widget = widgets.get(i);
                    if (widget.type != null) {
                        containsFrames |= "Frame".equals(widget.type);
                        containsOtherWidgets |= !"Frame".equals(widget.type);
                    }
                    String wType = widget.type == null ? "?" : widget.type;
                    if ("Buttongrid".equals(type)) {
                        if (!"Button".equals(widget.type)) {
                            addToList(warnings,
                                    "widget %s of type %s: Buttongrid must contain only Buttons".formatted(id, wType));
                        } else if (widget.row instanceof Integer row && row > 0
                                && widget.column instanceof Integer column && column > 0) {
                            ButtonPosition pos = new ButtonPosition(row, column);
                            if (widget.visibility == null
                                    || (widget.visibility instanceof List<?> rules && rules.isEmpty())) {
                                if (noVisibilityRulePositions.contains(pos)) {
                                    addToList(warnings,
                                            "widget %s of type %s: Button widget already exists for position (%d,%d)"
                                                    .formatted(id, wType, row, column));
                                }
                                if (visibilityRulePositions.contains(pos)) {
                                    addToList(warnings,
                                            "widget %s of type %s: Button widget with and without visibility rule for same position (%d,%d)"
                                                    .formatted(id, wType, row, column));
                                }
                                noVisibilityRulePositions.add(pos);
                            } else {
                                if (noVisibilityRulePositions.contains(pos)) {
                                    addToList(warnings,
                                            "widget %s of type %s: Button widget without and with visibility rule for same position (%d,%d)"
                                                    .formatted(id, wType, row, column));
                                }
                                visibilityRulePositions.add(pos);
                            }
                        }
                    } else {
                        if ("Button".equals(widget.type)) {
                            addToList(warnings, "widget %s of type %s: Buttons are only allowed in Buttongrid"
                                    .formatted(id, wType));
                        }
                        if ("Frame".equals(type) && "Frame".equals(widget.type)) {
                            addToList(warnings, "widget %s of type %s: Frame widget must not contain other Frames"
                                    .formatted(id, wType));
                        }
                    }
                    List<String> widgetErrors = new ArrayList<>();
                    List<String> widgetWarnings = new ArrayList<>();
                    ok &= widget.isValid(widgetErrors, widgetWarnings);
                    widgetErrors.forEach(error -> {
                        addToList(errors, "widget %s of type %s: %s".formatted(id, wType, error));
                    });
                    widgetWarnings.forEach(warning -> {
                        addToList(warnings, "widget %s of type %s: %s".formatted(id, wType, warning));
                    });
                }
                if (containsFrames && containsOtherWidgets) {
                    addToList(warnings, "%s widget should contain either only frames or none at all".formatted(type));
                }
            }
        }

        return ok;
    }

    private boolean isValidField(String field, @Nullable Object value, @NonNull List<@NonNull String> errors,
            @NonNull List<@NonNull String> warnings) {
        boolean ok = true;
        Set<String> mandatory_fields = Objects.requireNonNull(MANDATORY_FIELDS.get(type));
        Set<String> optional_fields = Objects.requireNonNull(OPTIONAL_FIELDS.get(type));
        if (value == null && mandatory_fields.contains(field)) {
            addToList(errors, "\"%s\" field missing while mandatory".formatted(field));
            ok = false;
        } else if (value != null && !mandatory_fields.contains(field) && !optional_fields.contains(field)) {
            addToList(warnings, "unexpected \"%s\" field is ignored".formatted(field));
        } else if ("item".equals(field) && value instanceof String item && !ItemUtil.isValidItemName(item)) {
            addToList(errors,
                    "invalid value \"%s\" for \"%s\" field; it must begin with a letter or underscore followed by alphanumeric characters and underscores, and must not contain any other symbols"
                            .formatted(item, field));
            ok = false;
        } else if ("height".equals(field) && value instanceof Integer height && height <= 0) {
            addToList(height < 0 ? errors : warnings,
                    "invalid value %d for \"%s\" field; value must be greater than 0".formatted(height, field));
            if (height < 0) {
                ok = false;
            }
        } else if ("refresh".equals(field) && value instanceof Integer refresh && refresh <= 0) {
            addToList(refresh < 0 ? errors : warnings,
                    "invalid value %d for \"%s\" field; value must be greater than 0".formatted(refresh, field));
            if (refresh < 0) {
                ok = false;
            }
        } else if ("row".equals(field) && value instanceof Integer row && row <= 0) {
            addToList(errors, "invalid value %d for \"%s\" field; value must be greater than 0".formatted(row, field));
            ok = false;
        } else if ("column".equals(field) && value instanceof Integer column && column <= 0) {
            addToList(errors,
                    "invalid value %d for \"%s\" field; value must be greater than 0".formatted(column, field));
            ok = false;
        } else if ("step".equals(field) && value instanceof BigDecimal step && step.doubleValue() <= 0) {
            addToList(warnings, "invalid value %f for \"%s\" field; value must be greater than 0"
                    .formatted(step.doubleValue(), field));
        } else if ("hint".equals(field) && value instanceof String hint && !ALLOWED_HINTS.contains(hint)) {
            addToList(warnings, "invalid value \"%s\" for \"%s\" field".formatted(hint, field));
        } else if ("interpolation".equals(field) && value instanceof String interpolation
                && !ALLOWED_INTERPOLATION.contains(interpolation)) {
            addToList(warnings, "invalid value \"%s\" for \"%s\" field".formatted(interpolation, field));
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
        return Objects.hash(type, item, label, icon, mappings, switchSupport, releaseOnly, height, min, max, step, hint,
                url, refresh, encoding, service, period, legend, forceAsItem, yAxisDecimalPattern, interpolation, row,
                column, command, releaseCommand, stateless, visibility, widgets);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        YamlWidgetDTO other = (YamlWidgetDTO) obj;
        return Objects.equals(type, other.type) && Objects.equals(item, other.item)
                && Objects.equals(label, other.label) && Objects.equals(icon, other.icon)
                && Objects.equals(mappings, other.mappings) && Objects.equals(switchSupport, other.switchSupport)
                && Objects.equals(releaseOnly, other.releaseOnly) && Objects.equals(height, other.height)
                && Objects.equals(min, other.min) && Objects.equals(max, other.max) && Objects.equals(step, other.step)
                && Objects.equals(hint, other.hint) && Objects.equals(url, other.url)
                && Objects.equals(refresh, other.refresh) && Objects.equals(encoding, other.encoding)
                && Objects.equals(service, other.service) && Objects.equals(period, other.period)
                && Objects.equals(legend, other.legend) && Objects.equals(forceAsItem, other.forceAsItem)
                && Objects.equals(yAxisDecimalPattern, other.yAxisDecimalPattern)
                && Objects.equals(interpolation, other.interpolation) && Objects.equals(row, other.row)
                && Objects.equals(column, other.column) && Objects.equals(command, other.command)
                && Objects.equals(releaseCommand, other.releaseCommand) && Objects.equals(stateless, other.stateless)
                && Objects.equals(visibility, other.visibility) && Objects.equals(widgets, other.widgets);
    }

    record ButtonPosition(Integer row, Integer column) {
    }
}
