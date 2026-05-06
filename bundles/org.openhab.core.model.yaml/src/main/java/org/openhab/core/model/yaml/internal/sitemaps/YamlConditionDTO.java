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
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.ItemUtil;

/**
 * This is a data transfer object that is used to serialize a sitemap rule condition.
 *
 * @author Laurent Garnier - Initial contribution
 */
public class YamlConditionDTO {

    private static final Set<String> ALLOWED_CONDITIONS = Set.of("==", "!=", "<", ">", "<=", ">=");

    public String item;
    public String operator;
    public String argument;

    public YamlConditionDTO() {
    }

    public boolean isValid(@NonNull List<@NonNull String> errors, @NonNull List<@NonNull String> warnings) {
        boolean ok = true;
        if (item != null && !ItemUtil.isValidItemName(item)) {
            addToList(errors,
                    "invalid value \"%s\" for \"item\" field in condition; it must begin with a letter or underscore followed by alphanumeric characters and underscores, and must not contain any other symbols"
                            .formatted(item));
            ok = false;
        }
        if (operator != null && !ALLOWED_CONDITIONS.contains(operator)) {
            addToList(errors, "invalid value \"%s\" for \"operator\" field in condition".formatted(operator));
            ok = false;
        }
        if ((item != null || operator != null) && argument == null) {
            addToList(errors, "\"argument\" field missing while mandatory in condition");
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
        return Objects.hash(item, operator, argument);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        YamlConditionDTO other = (YamlConditionDTO) obj;
        return Objects.equals(item, other.item) && Objects.equals(operator, other.operator)
                && Objects.equals(argument, other.argument);
    }
}
