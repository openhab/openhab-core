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
package org.openhab.core.config.core.internal.validation;

import java.math.BigDecimal;

import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.config.core.internal.validation.TypeIntrospections.TypeIntrospection;
import org.openhab.core.config.core.validation.ConfigValidationMessage;

/**
 * The {@link ConfigDescriptionParameterValidator} for the minimum and maximum attribute of a
 * {@link ConfigDescriptionParameter}.
 *
 * @author Thomas Höfer - Initial contribution
 * @authod Chris Jackson - Allow options to be outside of min/max value
 * @param <T>
 */
final class MinMaxValidator implements ConfigDescriptionParameterValidator {

    @Override
    public ConfigValidationMessage validate(ConfigDescriptionParameter parameter, Object value) {
        if (value == null || parameter.getType() == Type.BOOLEAN) {
            return null;
        }

        // Allow specified options to be outside of the min/max value
        for (ParameterOption option : parameter.getOptions()) {
            // Option values are a string, so we can do a simple compare
            if (option.getValue().equals(value.toString())) {
                return null;
            }
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
