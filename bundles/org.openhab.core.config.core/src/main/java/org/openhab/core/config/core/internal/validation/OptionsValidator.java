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
import org.openhab.core.config.core.validation.ConfigValidationMessage;

/**
 * The {@link ConfigDescriptionParameterValidator} for the options of a {@link ConfigDescriptionParameter}.
 *
 * @author Christoph Weitkamp - Initial contribution
 * @author Jan N. Klug - Extend for decimal types
 */
@NonNullByDefault
final class OptionsValidator implements ConfigDescriptionParameterValidator {

    @Override
    public @Nullable ConfigValidationMessage validate(ConfigDescriptionParameter param, @Nullable Object value) {
        if (value == null || !param.getLimitToOptions() || param.getOptions().isEmpty()) {
            return null;
        }

        boolean invalid;

        if (param.getType() == ConfigDescriptionParameter.Type.DECIMAL
                || param.getType() == ConfigDescriptionParameter.Type.INTEGER) {
            BigDecimal bdValue = new BigDecimal(value.toString());
            invalid = param.getOptions().stream().map(o -> new BigDecimal(o.getValue()))
                    .noneMatch(v -> v.compareTo(bdValue) == 0);
        } else {
            invalid = param.getOptions().stream().map(o -> o.getValue()).noneMatch(v -> v.equals(value.toString()));
        }

        if (invalid) {
            MessageKey messageKey = MessageKey.OPTIONS_VIOLATED;
            return new ConfigValidationMessage(param.getName(), messageKey.defaultMessage, messageKey.key,
                    String.valueOf(value), param.getOptions());
        }

        return null;
    }
}
