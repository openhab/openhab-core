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
package org.openhab.core.magic.binding.internal;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.magic.binding.MagicService;

/**
 * Configuration holder object for {@link MagicService}
 *
 * @author David Graeff - Initial contribution
 */
@NonNullByDefault
public class MagicServiceConfig {
    public @Nullable String text;
    public boolean bool;
    public @Nullable BigDecimal decimal;
    public @Nullable Integer integer;

    public @Nullable String textAdvanced;
    public boolean booleanAdvanced;
    public @Nullable BigDecimal decimalAdvanced;
    public @Nullable Integer integerAdvanced;

    public @Nullable String requiredTextParameter;
    public @Nullable String verifiedTextParameter;
    public @Nullable String selectLimited;
    public @Nullable String selectVariable;

    public @Nullable List<String> multiselectTextLimit;
    public @Nullable List<BigDecimal> multiselectIntegerLimit;

    public @Nullable BigDecimal selectDecimalLimit;

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        for (Field field : this.getClass().getDeclaredFields()) {
            Object value;
            try {
                value = field.get(this);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                continue;
            }
            if (value != null) {
                b.append("MagicService config ");
                b.append(field.getName());
                b.append(" = ");
                b.append(value);
            }
        }
        return b.toString();
    }
}
