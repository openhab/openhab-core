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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This is a data transfer object that is used to serialize a widget label.
 *
 * @author Laurent Garnier - Initial contribution
 */
public class YamlWidgetLabelDTO {

    public String label;
    public String format;
    public Object labelColor;
    public Object valueColor;

    public YamlWidgetLabelDTO() {
    }

    public boolean isValid(@NonNull List<@NonNull String> errors, @NonNull List<@NonNull String> warnings) {
        boolean ok = true;

        List<String> ruleErrors = new ArrayList<>();
        List<String> ruleWarnings = new ArrayList<>();
        if (labelColor instanceof YamlRuleWithAndConditionsDTO rule) {
            ok &= rule.isValid(ruleErrors, ruleWarnings);
            if (rule.value == null) {
                addToList(errors, "invalid rule in \"labelColor\" field: \"value\" field missing while mandatory");
                ok = false;
            }
        } else if (labelColor instanceof YamlRuleWithUniqueConditionDTO rule) {
            ok &= rule.isValid(ruleErrors, ruleWarnings);
            if (rule.value == null) {
                addToList(errors, "invalid rule in \"labelColor\" field: \"value\" field missing while mandatory");
                ok = false;
            }
        } else if (labelColor instanceof List<?> rules) {
            for (Object r : rules) {
                if (r instanceof YamlRuleWithAndConditionsDTO rule) {
                    ok &= rule.isValid(ruleErrors, ruleWarnings);
                    if (rule.value == null) {
                        addToList(errors,
                                "invalid rule in \"labelColor\" field: \"value\" field missing while mandatory");
                        ok = false;
                    }
                } else if (r instanceof YamlRuleWithUniqueConditionDTO rule) {
                    ok &= rule.isValid(ruleErrors, ruleWarnings);
                    if (rule.value == null) {
                        addToList(errors,
                                "invalid rule in \"labelColor\" field: \"value\" field missing while mandatory");
                        ok = false;
                    }
                } else {
                    addToList(errors, "invalid type for rule in \"labelColor\" field");
                    ok = false;
                }
            }
        } else if (labelColor != null && !(labelColor instanceof String)) {
            addToList(errors, "invalid type for \"labelColor\" field");
            ok = false;
        }
        ruleErrors.forEach(error -> {
            addToList(errors, "invalid rule in \"labelColor\" field: %s".formatted(error));
        });
        ruleWarnings.forEach(warning -> {
            addToList(warnings, "rule in \"labelColor\" field: %s".formatted(warning));
        });

        ruleErrors.clear();
        ruleWarnings.clear();
        if (valueColor instanceof YamlRuleWithAndConditionsDTO rule) {
            ok &= rule.isValid(ruleErrors, ruleWarnings);
            if (rule.value == null) {
                addToList(errors, "invalid rule in \"valueColor\" field: \"value\" field missing while mandatory");
                ok = false;
            }
        } else if (valueColor instanceof YamlRuleWithUniqueConditionDTO rule) {
            ok &= rule.isValid(ruleErrors, ruleWarnings);
            if (rule.value == null) {
                addToList(errors, "invalid rule in \"valueColor\" field: \"value\" field missing while mandatory");
                ok = false;
            }
        } else if (valueColor instanceof List<?> rules) {
            for (Object r : rules) {
                if (r instanceof YamlRuleWithAndConditionsDTO rrule) {
                    ok &= rrule.isValid(ruleErrors, ruleWarnings);
                    if (rrule.value == null) {
                        addToList(errors,
                                "invalid rule in \"valueColor\" field: \"value\" field missing while mandatory");
                        ok = false;
                    }
                } else if (r instanceof YamlRuleWithUniqueConditionDTO rule) {
                    ok &= rule.isValid(ruleErrors, ruleWarnings);
                    if (rule.value == null) {
                        addToList(errors,
                                "invalid rule in \"valueColor\" field: \"value\" field missing while mandatory");
                        ok = false;
                    }
                } else {
                    addToList(errors, "invalid type for rule in \"valueColor\" field");
                    ok = false;
                }
            }
        } else if (valueColor != null && !(valueColor instanceof String)) {
            addToList(errors, "invalid type for \"valueColor\" field");
            ok = false;
        }
        ruleErrors.forEach(error -> {
            addToList(errors, "invalid rule in \"valueColor\" field: %s".formatted(error));
        });
        ruleWarnings.forEach(warning -> {
            addToList(warnings, "rule in \"valueColor\" field: %s".formatted(warning));
        });

        return ok;
    }

    private void addToList(@Nullable List<@NonNull String> list, String value) {
        if (list != null) {
            list.add(value);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, format, labelColor, valueColor);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        YamlWidgetLabelDTO other = (YamlWidgetLabelDTO) obj;
        return Objects.equals(label, other.label) && Objects.equals(format, other.format)
                && Objects.equals(labelColor, other.labelColor) && Objects.equals(valueColor, other.valueColor);
    }
}
