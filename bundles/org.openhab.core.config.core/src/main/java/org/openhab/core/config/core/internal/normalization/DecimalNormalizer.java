/**
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
package org.openhab.core.config.core.internal.normalization;

import java.math.BigDecimal;

import org.openhab.core.config.core.ConfigDescriptionParameter;

/**
 * The normalizer for {@link ConfigDescriptionParameter.Type#DECIMAL}. It converts all number types to BigDecimal,
 * having at least one digit after the floating point. Also {@link String}s are converted if possible.
 *
 * @author Simon Kaufmann - Initial contribution
 * @author Thomas Höfer - made class final and minor javadoc changes
 */
final class DecimalNormalizer extends AbstractNormalizer {

    @Override
    public Object doNormalize(Object value) {
        try {
            if (value instanceof BigDecimal bigDecimalValue) {
                return stripTrailingZeros(bigDecimalValue);
            }
            if (value instanceof String stringValue) {
                return stripTrailingZeros(new BigDecimal(stringValue));
            }
            if (value instanceof Byte byteValue) {
                return new BigDecimal(byteValue);
            }
            if (value instanceof Short shortValue) {
                return new BigDecimal(shortValue);
            }
            if (value instanceof Integer integerValue) {
                return new BigDecimal(integerValue);
            }
            if (value instanceof Long longValue) {
                return new BigDecimal(longValue);
            }
            if (value instanceof Float floatValue) {
                return new BigDecimal(floatValue.toString());
            }
            if (value instanceof Double doubleValue) {
                return BigDecimal.valueOf(doubleValue);
            }
        } catch (ArithmeticException | NumberFormatException e) {
            logger.trace("\"{}\" is not a valid decimal number.", value, e);
            return value;
        }
        logger.trace("Class \"{}\" cannot be converted to a decimal number.", value.getClass().getName());
        return value;
    }

    private BigDecimal stripTrailingZeros(BigDecimal value) {
        BigDecimal ret = value;
        if (ret.scale() > 1) {
            ret = ret.stripTrailingZeros();
            if (ret.scale() == 0) {
                ret = ret.setScale(1);
            }
        }
        return ret;
    }
}
