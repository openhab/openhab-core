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
package org.openhab.core.config.core.metadata;

import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ParameterOption;

/**
 * A {@link MetadataConfigDescriptionProvider} implementation can be registered as an OSGi service in order to give
 * guidance to UIs what metadata namespaces should be available and what metadata properties are expected.
 * <p>
 * It will be tracked by the framework and the given information will be translated into config descriptions.
 * <p>
 * Every extension which deals with specific metadata (in its own namespace) is expected to provide an implementation of
 * this interface.
 *
 * @author Simon Kaufmann - Initial contribution
 */
@NonNullByDefault
public interface MetadataConfigDescriptionProvider {

    /**
     * Get the identifier of the metadata namespace
     *
     * @return the metadata namespace
     */
    String getNamespace();

    /**
     * Get the human-readable description of the metadata namespace
     * <p>
     * Overriding this method is optional - it will default to the namespace identifier.
     *
     * @param locale a locale, if available
     * @return the metadata namespace description
     */
    @Nullable
    String getDescription(@Nullable Locale locale);

    /**
     * Get all valid options if the main metadata value should be restricted to certain values.
     *
     * @param locale a locale, if available
     * @return a list of parameter options or {@code null}
     */
    @Nullable
    List<ParameterOption> getParameterOptions(@Nullable Locale locale);

    /**
     * Get the config descriptions for all expected parameters.
     * <p>
     * This list may depend on the current "main" value
     *
     * @param value the current "main" value
     * @param locale a locale, if available
     * @return a list of config description parameters or {@code null}
     */
    @Nullable
    List<ConfigDescriptionParameter> getParameters(String value, @Nullable Locale locale);

}
