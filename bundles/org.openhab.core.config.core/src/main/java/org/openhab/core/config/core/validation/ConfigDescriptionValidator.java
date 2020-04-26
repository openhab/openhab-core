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
package org.openhab.core.config.core.validation;

import java.net.URI;
import java.util.Map;

import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.Configuration;

/**
 * The {@link ConfigDescriptionValidator} validates a given set of {@link Configuration} parameters against a
 * given {@link ConfigDescription} URI. So it can be used as a static pre-validation to avoid that the configuration of
 * an entity is updated with parameters which do not match with the declarations in the configuration description.
 * If the validator detects one or more mismatches then a {@link ConfigValidationException} is thrown.
 *
 * @author Thomas Höfer - Initial contribution
 * @author Chris Jackson - Handle checks on multiple selection parameters
 */
public interface ConfigDescriptionValidator {

    /**
     * Validates the given configuration parameters against the given configuration description having the given URI.
     *
     * @param configurationParameters the configuration parameters to be validated
     * @param configDescriptionURI the URI of the configuration description against which the configuration parameters
     *            are to be validated
     * @throws ConfigValidationException if one or more configuration parameters do not match with the configuration
     *             description having the given URI
     * @throws NullPointerException if given config description URI or configuration parameters are null
     */
    void validate(Map<String, Object> configurationParameters, URI configDescriptionURI);
}
