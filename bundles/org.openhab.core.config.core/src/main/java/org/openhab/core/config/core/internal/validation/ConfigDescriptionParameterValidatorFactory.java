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

/**
 * The {@link ConfigDescriptionParameterValidatorFactory} creates the corresponding
 * {@link ConfigDescriptionParameterValidator}s used by ConfigDescriptionValidator.
 *
 * @author Thomas Höfer - Initial contribution
 */
public final class ConfigDescriptionParameterValidatorFactory {

    private ConfigDescriptionParameterValidatorFactory() {
        super();
    }

    /**
     * Returns a new validator for the required attribute of a config description parameter.
     *
     * @return a new validator for the required attribute of a config description parameter
     */
    public static ConfigDescriptionParameterValidator createRequiredValidator() {
        return new RequiredValidator();
    }

    /**
     * Returns a new validator for the data type validation of a config description parameter.
     *
     * @return a new validator for the data type validation of a config description parameter
     */
    public static ConfigDescriptionParameterValidator createTypeValidator() {
        return new TypeValidator();
    }

    /**
     * Returns a new validator for the min and max attribute of a config description parameter.
     *
     * @return a new validator for the min and max attribute of a config description parameter
     */
    public static ConfigDescriptionParameterValidator createMinMaxValidator() {
        return new MinMaxValidator();
    }

    /**
     * Returns a new validator for the pattern attribute of a config description parameter.
     *
     * @return a new validator for the pattern attribute of a config description parameter
     */
    public static ConfigDescriptionParameterValidator createPatternValidator() {
        return new PatternValidator();
    }
}
