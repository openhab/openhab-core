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
import org.eclipse.smarthome.config.core.validation.ConfigValidationMessage;

/**
 * The {@link ConfigDescriptionParameterValidator} for the required attribute of a {@link ConfigDescriptionParameter}.
 *
 * @author Thomas Höfer - Initial contribution
 */
final class RequiredValidator implements ConfigDescriptionParameterValidator {

    @Override
    public ConfigValidationMessage validate(ConfigDescriptionParameter param, Object value) {
        if (param.isRequired() && value == null) {
            MessageKey messageKey = MessageKey.PARAMETER_REQUIRED;
            return new ConfigValidationMessage(param.getName(), messageKey.defaultMessage, messageKey.key);
        }

        return null;
    }

}
