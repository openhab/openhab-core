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
import java.math.RoundingMode;

import org.openhab.core.config.core.ConfigDescriptionParameter;

/**
 * The normalizer for {@link ConfigDescriptionParameter.Type#INTEGER}. All different number formats will get converted
 * to BigDecimal, not allowing any fractions. Also, {@link String}s will be converted if possible.
 *
 * @author Simon Kaufmann - Initial contribution
 * @author Thomas Höfer - made class final and minor javadoc changes
 */
final class IntNormalizer extends AbstractNormalizer {

    @Override
    public Object doNormalize(Object value) {
        try {
            if (value instanceof BigDecimal decimal) {
                return decimal.setScale(0, RoundingMode.UNNECESSARY);
            }
            if (value instanceof Byte byte1) {
                return new BigDecimal(byte1);
            }
            if (value instanceof Integer integer) {
                return new BigDecimal(integer);
            }
            if (value instanceof Long long1) {
                return BigDecimal.valueOf(long1);
            }
            if (value instanceof String string) {
                return new BigDecimal(string).setScale(0, RoundingMode.UNNECESSARY);
            }
            if (value instanceof Float float1) {
                return new BigDecimal(float1.toString()).setScale(0, RoundingMode.UNNECESSARY);
            }
            if (value instanceof Double double1) {
                return BigDecimal.valueOf(double1).setScale(0, RoundingMode.UNNECESSARY);
            }
        } catch (ArithmeticException | NumberFormatException e) {
            logger.trace("\"{}\" is not a valid integer number.", value, e);
            return value;
        }
        logger.trace("Class \"{}\" cannot be converted to an integer number.", value.getClass().getName());
        return value;
    }
}
