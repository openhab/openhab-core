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
import org.eclipse.smarthome.config.core.internal.validation.TypeIntrospections.TypeIntrospection;
import org.eclipse.smarthome.config.core.validation.ConfigValidationMessage;

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
