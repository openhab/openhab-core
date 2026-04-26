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

import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This is a data transfer object that is used to serialize a sitemap rule with AND conditions.
 *
 * @author Laurent Garnier - Initial contribution
 */
public class YamlRuleWithAndConditionsDTO {

    public List<YamlConditionDTO> and;
    public String value;

    public YamlRuleWithAndConditionsDTO() {
    }

    public boolean isValid(@NonNull List<@NonNull String> errors, @NonNull List<@NonNull String> warnings) {
        boolean ok = true;
        if (and != null && !and.isEmpty()) {
            for (YamlConditionDTO condition : and) {
                ok &= condition.isValid(errors, warnings);
                if (condition.argument == null) {
                    addToList(errors, "\"argument\" field missing while mandatory in condition");
                    ok = false;
                }
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
    public int hashCode() {
        return Objects.hash(and, value);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        YamlRuleWithAndConditionsDTO other = (YamlRuleWithAndConditionsDTO) obj;
        return Objects.equals(and, other.and) && Objects.equals(value, other.value);
    }
}
