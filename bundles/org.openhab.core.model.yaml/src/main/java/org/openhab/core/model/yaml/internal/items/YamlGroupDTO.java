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
package org.openhab.core.model.yaml.internal.items;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.model.yaml.internal.util.YamlElementUtils;

/**
 * The {@link YamlGroupDTO} is a data transfer object used to serialize the details of a group item
 * in a YAML configuration file.
 *
 * @author Laurent Garnier - Initial contribution
 */
public class YamlGroupDTO {

    private static final String DEFAULT_FUNCTION = "EQUALITY";
    private static final Set<String> VALID_FUNCTIONS = Set.of("AND", "OR", "NAND", "NOR", "XOR", "COUNT", "AVG",
            "MEDIAN", "SUM", "MIN", "MAX", "LATEST", "EARLIEST", DEFAULT_FUNCTION);

    public String type;
    public String dimension;
    public String function;
    public List<@NonNull String> parameters;

    public YamlGroupDTO() {
    }

    public boolean isValid(@NonNull List<@NonNull String> errors, @NonNull List<@NonNull String> warnings) {
        boolean ok = true;
        if (!YamlElementUtils.isValidItemType(type)) {
            errors.add("invalid value \"%s\" for \"type\" field in group".formatted(type));
            ok = false;
        } else if (YamlElementUtils.isNumberItemType(type)) {
            if (!YamlElementUtils.isValidItemDimension(dimension)) {
                errors.add("invalid value \"%s\" for \"dimension\" field in group".formatted(dimension));
                ok = false;
            }
        } else if (dimension != null) {
            warnings.add("\"dimension\" field in group ignored as type is not Number");
        }
        if (!VALID_FUNCTIONS.contains(getFunction())) {
            errors.add("invalid value \"%s\" for \"function\" field".formatted(function));
            ok = false;
        }
        return ok;
    }

    public @Nullable String getBaseType() {
        return YamlElementUtils.getItemTypeWithDimension(type, dimension);
    }

    public String getFunction() {
        return function != null ? function.toUpperCase() : DEFAULT_FUNCTION;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBaseType(), getFunction());
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        YamlGroupDTO other = (YamlGroupDTO) obj;
        return Objects.equals(getBaseType(), other.getBaseType()) && Objects.equals(getFunction(), other.getFunction())
                && YamlElementUtils.equalsListStrings(parameters, other.parameters);
    }
}
