/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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

import org.openhab.core.magic.binding.MagicService;

/**
 * Configuration holder object for {@link MagicService}
 *
 * @author David Graeff - Initial contribution
 */
public class MagicServiceConfig {
    public String text;
    public boolean bool;
    public BigDecimal decimal;
    public Integer integer;

    public String text_advanced;
    public boolean boolean_advanced;
    public BigDecimal decimal_advanced;
    public Integer integer_advanced;

    public String requiredTextParameter;
    public String verifiedTextParameter;
    public String select_limited;
    public String select_variable;

    public List<String> multiselect_text_limit;
    public List<BigDecimal> multiselect_integer_limit;

    public BigDecimal select_decimal_limit;

    @Override
    public String toString() {
        StringBuffer b = new StringBuffer();
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
