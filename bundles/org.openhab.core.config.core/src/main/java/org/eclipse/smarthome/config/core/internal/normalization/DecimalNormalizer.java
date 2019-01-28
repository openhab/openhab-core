/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.config.core.internal.normalization;

import java.math.BigDecimal;

import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;

/**
 * The normalizer for {@link ConfigDescriptionParameter.Type#DECIMAL}. It converts all number types to BigDecimal,
 * having at least one digit after the floating point. Also {@link String}s are converted if possible.
 *
 * @author Simon Kaufmann - initial contribution and API.
 * @author Thomas Höfer - made class final and minor javadoc changes
 */
final class DecimalNormalizer extends AbstractNormalizer {

    @Override
    public Object doNormalize(Object value) {
        try {
            if (value instanceof BigDecimal) {
                return stripTrailingZeros((BigDecimal) value);
            }
            if (value instanceof String) {
                return stripTrailingZeros(new BigDecimal((String) value));
            }
            if (value instanceof Byte) {
                return new BigDecimal((Byte) value).setScale(1);
            }
            if (value instanceof Integer) {
                return new BigDecimal((Integer) value).setScale(1);
            }
            if (value instanceof Long) {
                return new BigDecimal((Long) value).setScale(1);
            }
            if (value instanceof Float) {
                return new BigDecimal(((Float) value).toString());
            }
            if (value instanceof Double) {
                return BigDecimal.valueOf((Double) value);
            }
        } catch (ArithmeticException | NumberFormatException e) {
            logger.trace("\"{}\" is not a valid decimal number.", value, e);
            return value;
        }
        logger.trace("Class \"{}\" cannot be converted to a decimal number.", value.getClass().getName());
        return value;
    }

    private BigDecimal stripTrailingZeros(BigDecimal value) {
        BigDecimal ret = new BigDecimal(value.stripTrailingZeros().toPlainString());
        if (ret.scale() == 0) {
            ret = ret.setScale(1);
        }
        return ret;
    }

}
