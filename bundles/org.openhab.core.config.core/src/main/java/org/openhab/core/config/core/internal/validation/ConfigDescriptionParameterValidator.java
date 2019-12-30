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
import org.openhab.core.config.core.validation.ConfigValidationMessage;

/**
 * The {@link ConfigDescriptionParameterValidator} can be implemented to provide a specific validation of a
 * {@link ConfigDescriptionParameter} and its value to be set.
 *
 * @author Thomas Höfer - Initial contribution
 */
public interface ConfigDescriptionParameterValidator {

    /**
     * Validates the given value against the given {@link ConfigDescriptionParameter}.
     *
     * @param parameter the configuration description parameter
     * @param value the value to be set for the config description parameter
     *
     * @return a {@link ConfigValidationMessage} if value does not meet the declaration of the parameter,
     *         otherwise null
     */
    ConfigValidationMessage validate(ConfigDescriptionParameter parameter, Object value);
}
