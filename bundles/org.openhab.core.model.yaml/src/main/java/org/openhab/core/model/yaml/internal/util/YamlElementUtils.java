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
package org.openhab.core.model.yaml.internal.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.types.util.UnitUtils;
import org.openhab.core.util.StringUtils;

/**
 * Static utility methods that are helpful when dealing with YAML elements.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class YamlElementUtils {

    public static boolean equalsConfig(@Nullable Map<String, Object> first, @Nullable Map<String, Object> second) {
        if (first != null && second != null) {
            return first.size() != second.size() ? false
                    : first.entrySet().stream().allMatch(e -> equalsConfigValue(e.getValue(), second.get(e.getKey())));
        } else {
            return first == null && second == null;
        }
    }

    private static boolean equalsConfigValue(Object first, @Nullable Object second) {
        return (first instanceof List firstList && second instanceof List secondList)
                ? Arrays.equals(firstList.toArray(), secondList.toArray())
                : first.equals(second);
    }

    public static @Nullable String getAdjustedItemType(@Nullable String type) {
        return type == null ? null : StringUtils.capitalize(type);
    }

    public static boolean isValidItemType(@Nullable String type) {
        String adjustedType = getAdjustedItemType(type);
        return adjustedType == null ? true : CoreItemFactory.VALID_ITEM_TYPES.contains(adjustedType);
    }

    public static boolean isNumberItemType(@Nullable String type) {
        return CoreItemFactory.NUMBER.equals(getAdjustedItemType(type));
    }

    public static @Nullable String getAdjustedItemDimension(@Nullable String dimension) {
        return dimension == null ? null : StringUtils.capitalize(dimension);
    }

    public static boolean isValidItemDimension(@Nullable String dimension) {
        String adjustedDimension = getAdjustedItemDimension(dimension);
        if (adjustedDimension != null) {
            try {
                UnitUtils.parseDimension(adjustedDimension);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return true;
    }

    public static @Nullable String getItemTypeWithDimension(@Nullable String type, @Nullable String dimension) {
        String adjustedType = getAdjustedItemType(type);
        String adjustedDimension = getAdjustedItemDimension(dimension);
        return adjustedType != null ? adjustedType + (adjustedDimension == null ? "" : ":" + adjustedDimension) : null;
    }
}
