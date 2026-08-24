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
import org.openhab.core.model.yaml.internal.util.YamlElementUtils;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This is a data transfer object that is used to serialize a widget icon.
 *
 * @author Laurent Garnier - Initial contribution
 */
public class YamlWidgetIconDTO {

    public Object name;
    @JsonProperty("static")
    @JsonAlias("staticIcon")
    public Boolean staticIcon;
    public Object color;

    public YamlWidgetIconDTO() {
    }

    public boolean isValid(@NonNull List<@NonNull String> errors, @NonNull List<@NonNull String> warnings) {
        boolean ok = true;

        List<String> ruleErrors = new ArrayList<>();
        List<String> ruleWarnings = new ArrayList<>();
        if (name instanceof String nameStr) {
            if (!YamlElementUtils.isValidIcon(nameStr)) {
                addToList(errors,
                        "invalid icon value \"%s\" for \"name\" field; it must contain a maximum of 3 segments separated by a colon, each segment matching pattern [a-zA-Z0-9_][a-zA-Z0-9_-]*"
                                .formatted(nameStr));
                ok = false;
            }
        } else if (name instanceof YamlRuleWithAndConditionsDTO rule) {
            ok &= rule.isValid(ruleErrors, ruleWarnings);
            if (rule.value == null) {
                addToList(errors, "invalid rule in \"name\" field: \"value\" field missing while mandatory");
                ok = false;
            } else if (!YamlElementUtils.isValidIcon(rule.value)) {
                addToList(errors,
                        "invalid rule in \"name\" field: invalid icon value \"%s\" for \"value\" field; it must contain a maximum of 3 segments separated by a colon, each segment matching pattern [a-zA-Z0-9_][a-zA-Z0-9_-]*"
                                .formatted(rule.value));
                ok = false;
            }
        } else if (name instanceof YamlRuleWithUniqueConditionDTO rule) {
            ok &= rule.isValid(ruleErrors, ruleWarnings);
            if (rule.value == null) {
                addToList(errors, "invalid rule in \"name\" field: \"value\" field missing while mandatory");
                ok = false;
            } else if (!YamlElementUtils.isValidIcon(rule.value)) {
                addToList(errors,
                        "invalid rule in \"name\" field: invalid icon value \"%s\" for \"value\" field; it must contain a maximum of 3 segments separated by a colon, each segment matching pattern [a-zA-Z0-9_][a-zA-Z0-9_-]*"
                                .formatted(rule.value));
                ok = false;
            }
        } else if (name instanceof List<?> rules) {
            for (Object r : rules) {
                if (r instanceof YamlRuleWithAndConditionsDTO rule) {
                    ok &= rule.isValid(ruleErrors, ruleWarnings);
                    if (rule.value == null) {
                        addToList(errors, "invalid rule in \"name\" field: \"value\" field missing while mandatory");
                        ok = false;
                    } else if (!YamlElementUtils.isValidIcon(rule.value)) {
                        addToList(errors,
                                "invalid rule in \"name\" field: invalid icon value \"%s\" for \"value\" field; it must contain a maximum of 3 segments separated by a colon, each segment matching pattern [a-zA-Z0-9_][a-zA-Z0-9_-]*"
                                        .formatted(rule.value));
                        ok = false;
                    }
                } else if (r instanceof YamlRuleWithUniqueConditionDTO rule) {
                    ok &= rule.isValid(ruleErrors, ruleWarnings);
                    if (rule.value == null) {
                        addToList(errors, "invalid rule in \"name\" field: \"value\" field missing while mandatory");
                        ok = false;
                    } else if (!YamlElementUtils.isValidIcon(rule.value)) {
                        addToList(errors,
                                "invalid rule in \"name\" field: invalid icon value \"%s\" for \"value\" field; it must contain a maximum of 3 segments separated by a colon, each segment matching pattern [a-zA-Z0-9_][a-zA-Z0-9_-]*"
                                        .formatted(rule.value));
                        ok = false;
                    }
                } else {
                    addToList(errors, "invalid type for rule in \"name\" field");
                    ok = false;
                }
            }
        } else if (name != null && !(name instanceof String)) {
            addToList(errors, "invalid type for \"name\" field");
            ok = false;
        }
        ruleErrors.forEach(error -> {
            addToList(errors, "invalid rule in \"name\" field: %s".formatted(error));
        });
        ruleWarnings.forEach(warning -> {
            addToList(warnings, "rule in \"name\" field: %s".formatted(warning));
        });

        ruleErrors.clear();
        ruleWarnings.clear();
        if (color instanceof YamlRuleWithAndConditionsDTO rule) {
            ok &= rule.isValid(ruleErrors, ruleWarnings);
            if (rule.value == null) {
                addToList(errors, "invalid rule in \"color\" field: \"value\" field missing while mandatory");
                ok = false;
            }
        } else if (color instanceof YamlRuleWithUniqueConditionDTO rule) {
            ok &= rule.isValid(ruleErrors, ruleWarnings);
            if (rule.value == null) {
                addToList(errors, "invalid rule in \"color\" field: \"value\" field missing while mandatory");
                ok = false;
            }
        } else if (color instanceof List<?> rules) {
            for (Object r : rules) {
                if (r instanceof YamlRuleWithAndConditionsDTO rule) {
                    ok &= rule.isValid(ruleErrors, ruleWarnings);
                    if (rule.value == null) {
                        addToList(errors, "invalid rule in \"color\" field: \"value\" field missing while mandatory");
                        ok = false;
                    }
                } else if (r instanceof YamlRuleWithUniqueConditionDTO rule) {
                    ok &= rule.isValid(ruleErrors, ruleWarnings);
                    if (rule.value == null) {
                        addToList(errors, "invalid rule in \"color\" field: \"value\" field missing while mandatory");
                        ok = false;
                    }
                } else {
                    addToList(errors, "invalid type for rule in \"color\" field");
                    ok = false;
                }
            }
        } else if (color != null && !(color instanceof String)) {
            addToList(errors, "invalid type for \"color\" field");
            ok = false;
        }
        ruleErrors.forEach(error -> {
            addToList(errors, "invalid rule in \"color\" field: %s".formatted(error));
        });
        ruleWarnings.forEach(warning -> {
            addToList(warnings, "rule in \"color\" field: %s".formatted(warning));
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
        return Objects.hash(name, staticIcon, color);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        YamlWidgetIconDTO other = (YamlWidgetIconDTO) obj;
        return Objects.equals(name, other.name) && Objects.equals(staticIcon, other.staticIcon)
                && Objects.equals(color, other.color);
    }
}
