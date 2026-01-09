/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.core.ConfigDescriptionParameter;

/**
 * The normalizer for {@link ConfigDescriptionParameter.Type#BOOLEAN}. It tries to convert the given value into a
 * {@link Boolean} object.
 * <p>
 * Therefore it considers numbers (0/1 and their {@link String} representations) as well as {@link String}s, containing
 * apart from the typical "true"/"false" also other values like "yes"/"no", "on"/"off".
 *
 * @author Simon Kaufmann - Initial contribution
 * @author Thomas Höfer - made class final and minor javadoc changes
 */
@NonNullByDefault
final class BooleanNormalizer extends AbstractNormalizer {
    private static final Set<String> TRUES = Set.of("true", "yes", "on", "1");
    private static final Set<String> FALSES = Set.of("false", "no", "off", "0");

    @Override
    public Object doNormalize(Object value) {
        return switch (value) {
            case Boolean bool -> bool;
            case Byte byteValue -> handleNumeric(byteValue.longValue());
            case Integer integerValue -> handleNumeric(integerValue.longValue());
            case Long longValue -> handleNumeric(longValue);
            default -> {
                String s = value.toString().toLowerCase();
                if (TRUES.contains(s)) {
                    yield true;
                } else if (FALSES.contains(s)) {
                    yield false;
                }
                logger.trace("Class \"{}\" cannot be converted to boolean.", value.getClass().getName());
                yield value;
            }
        };
    }

    private Object handleNumeric(long numeric) {
        if (numeric == 1) {
            return true;
        } else if (numeric == 0) {
            return false;
        }
        logger.trace("\"{}\" cannot be interpreted as a boolean.", numeric);
        return numeric;
    }
}
