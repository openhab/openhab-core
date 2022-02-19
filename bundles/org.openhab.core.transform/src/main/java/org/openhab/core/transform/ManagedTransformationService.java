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
package org.openhab.core.transform;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link ManagedTransformationService} can be implemented by transformations to make their configuration managable
 * over the REST UI
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public interface ManagedTransformationService {

    /**
     * Check if the provided configuration is valid
     *
     * Services that don't provide validation can rely on the default implementation which returns true
     *
     * @param configuration the configuration to check
     * @return true if configuration is valid or service does not implement validation
     */
    default boolean configurationIsValid(@Nullable String configuration) {
        return true;
    };

    /**
     * Get a list of all file extensions supported by this service
     * 
     * @return A list of file extensions
     */
    List<String> supportedFileExtensions();
}
