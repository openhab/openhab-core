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
package org.openhab.core.config.core.internal.normalization;

import java.math.BigDecimal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.core.ConfigDescriptionParameter;

/**
 * The normalizer for {@link ConfigDescriptionParameter.Type#DECIMAL}. It converts all number types to BigDecimal,
 * having at least one digit after the floating point. Also {@link String}s are converted if possible.
 *
 * @author Simon Kaufmann - Initial contribution
 * @author Thomas Höfer - made class final and minor javadoc changes
 */
@NonNullByDefault
final class DecimalNormalizer extends AbstractNormalizer {

    @Override
    public Object doNormalize(Object value) {
        try {
            return switch (value) {
                case BigDecimal bigDecimalValue -> stripTrailingZeros(bigDecimalValue);
                case String stringValue -> stripTrailingZeros(new BigDecimal(stringValue));
                case Byte byteValue -> new BigDecimal(byteValue);
                case Short shortValue -> new BigDecimal(shortValue);
                case Integer integerValue -> new BigDecimal(integerValue);
                case Long longValue -> new BigDecimal(longValue);
                case Float floatValue -> new BigDecimal(floatValue.toString());
                case Double doubleValue -> BigDecimal.valueOf(doubleValue);
                default -> {
                    logger.trace("Class \"{}\" cannot be converted to a decimal number.", value.getClass().getName());
                    yield value;
                }
            };
        } catch (ArithmeticException | NumberFormatException e) {
            logger.trace("\"{}\" is not a valid decimal number.", value, e);
            return value;
        }
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
