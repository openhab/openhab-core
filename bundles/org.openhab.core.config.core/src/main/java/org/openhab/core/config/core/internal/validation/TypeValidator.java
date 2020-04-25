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

import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.config.core.internal.validation.TypeIntrospections.TypeIntrospection;
import org.openhab.core.config.core.validation.ConfigValidationMessage;

/**
 * The {@link TypeValidator} validates if the given value can be assigned to the config description parameter according
 * to its type definition.
 *
 * @author Thomas Höfer - Initial contribution
 */
final class TypeValidator implements ConfigDescriptionParameterValidator {

    @Override
    public ConfigValidationMessage validate(ConfigDescriptionParameter parameter, Object value) {
        if (value == null) {
            return null;
        }

        TypeIntrospection typeIntrospection = TypeIntrospections.get(parameter.getType());
        if (!typeIntrospection.isAssignable(value)) {
            return createDataTypeViolationMessage(parameter.getName(), parameter.getType());
        }

        return null;
    }

    private static ConfigValidationMessage createDataTypeViolationMessage(String parameterName, Type type) {
        return new ConfigValidationMessage(parameterName, MessageKey.DATA_TYPE_VIOLATED.defaultMessage,
                MessageKey.DATA_TYPE_VIOLATED.key, type);
    }
}
