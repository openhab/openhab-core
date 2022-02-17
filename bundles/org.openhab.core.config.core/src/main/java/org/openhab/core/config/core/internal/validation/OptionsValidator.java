/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.validation.ConfigValidationMessage;

/**
 * The {@link ConfigDescriptionParameterValidator} for the options of a {@link ConfigDescriptionParameter}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
final class OptionsValidator implements ConfigDescriptionParameterValidator {

    @Override
    public @Nullable ConfigValidationMessage validate(ConfigDescriptionParameter param, @Nullable Object value) {
        if (value == null || !param.getLimitToOptions() || param.getOptions().isEmpty()) {
            return null;
        }

        // Option values are a string, so we can do a simple compare
        if (param.getOptions().stream().map(o -> o.getValue()).noneMatch(v -> v.equals(value.toString()))) {
            MessageKey messageKey = MessageKey.OPTIONS_VIOLATED;
            return new ConfigValidationMessage(param.getName(), messageKey.defaultMessage, messageKey.key,
                    String.valueOf(value), param.getOptions());
        }
        return null;
    }
}
