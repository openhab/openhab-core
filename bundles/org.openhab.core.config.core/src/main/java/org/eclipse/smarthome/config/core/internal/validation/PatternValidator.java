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
package org.eclipse.smarthome.config.core.internal.validation;

import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter.Type;
import org.eclipse.smarthome.config.core.validation.ConfigValidationMessage;

/**
 * The {@link ConfigDescriptionParameterValidator} for the pattern attribute of a
 * {@link ConfigDescriptionParameter}.
 *
 * @author Thomas Höfer - Initial contribution
 */
final class PatternValidator implements ConfigDescriptionParameterValidator {

    @Override
    public ConfigValidationMessage validate(ConfigDescriptionParameter parameter, Object value) {
        if (value == null || parameter.getType() != Type.TEXT || parameter.getPattern() == null) {
            return null;
        }

        if (!((String) value).matches(parameter.getPattern())) {
            MessageKey messageKey = MessageKey.PATTERN_VIOLATED;
            return new ConfigValidationMessage(parameter.getName(), messageKey.defaultMessage, messageKey.key, String.valueOf(value),
                    parameter.getPattern());
        }

        return null;
    }
}
