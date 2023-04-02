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
            if (value instanceof BigDecimal decimal) {
                return stripTrailingZeros(decimal);
            }
            if (value instanceof String string) {
                return stripTrailingZeros(new BigDecimal(string));
            }
            if (value instanceof Byte byte1) {
                return new BigDecimal(byte1).setScale(1);
            }
            if (value instanceof Integer integer) {
                return new BigDecimal(integer).setScale(1);
            }
            if (value instanceof Long long1) {
                return new BigDecimal(long1).setScale(1);
            }
            if (value instanceof Float float1) {
                return new BigDecimal(float1.toString());
            }
            if (value instanceof Double double1) {
                return BigDecimal.valueOf(double1);
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
