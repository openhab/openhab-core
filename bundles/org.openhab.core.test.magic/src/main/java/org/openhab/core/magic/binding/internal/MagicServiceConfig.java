/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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

    public String textAdvanced;
    public boolean booleanAdvanced;
    public BigDecimal decimalAdvanced;
    public Integer integerAdvanced;

    public String requiredTextParameter;
    public String verifiedTextParameter;
    public String selectLimited;
    public String selectVariable;

    public List<String> multiselectTextLimit;
    public List<BigDecimal> multiselectIntegerLimit;

    public BigDecimal selectDecimalLimit;

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
