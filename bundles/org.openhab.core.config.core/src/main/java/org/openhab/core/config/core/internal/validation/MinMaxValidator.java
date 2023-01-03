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
package org.openhab.core.config.core.internal.validation;

import java.math.BigDecimal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.config.core.ConfigUtil;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.config.core.internal.validation.TypeIntrospections.TypeIntrospection;
import org.openhab.core.config.core.validation.ConfigValidationMessage;

/**
 * The {@link ConfigDescriptionParameterValidator} for the minimum and maximum attribute of a
 * {@link ConfigDescriptionParameter}.
 *
 * @author Thomas Höfer - Initial contribution
 * @author Chris Jackson - Allow options to be outside of min/max value
 */
@NonNullByDefault
final class MinMaxValidator implements ConfigDescriptionParameterValidator {

    @Override
    public @Nullable ConfigValidationMessage validate(ConfigDescriptionParameter parameter, @Nullable Object value) {
        if (value == null || parameter.getType() == Type.BOOLEAN) {
            return null;
        }

        String normalizedValueString = ConfigUtil.normalizeType(value, parameter).toString();

        // Allow specified options to be outside the min/max value
        // Option values are a string, so we can do a simple compare
        if (parameter.getOptions().stream().map(ParameterOption::getValue)
                .anyMatch(v -> v.equals(normalizedValueString))) {
            return null;
        }

        TypeIntrospection typeIntrospection = TypeIntrospections.get(parameter.getType());
        if (parameter.getMinimum() != null) {
            BigDecimal min = parameter.getMinimum();
            if (typeIntrospection.isMinViolated(value, min)) {
                return createMinMaxViolationMessage(parameter.getName(), typeIntrospection.getMinViolationMessageKey(),
                        min);
            }
        }
        if (parameter.getMaximum() != null) {
            BigDecimal max = parameter.getMaximum();
            if (typeIntrospection.isMaxViolated(value, max)) {
                return createMinMaxViolationMessage(parameter.getName(), typeIntrospection.getMaxViolationMessageKey(),
                        max);
            }
        }
        return null;
    }

    private static ConfigValidationMessage createMinMaxViolationMessage(String parameterName, MessageKey messageKey,
            BigDecimal minMax) {
        return new ConfigValidationMessage(parameterName, messageKey.defaultMessage, messageKey.key,
                String.valueOf(minMax));
    }
}
