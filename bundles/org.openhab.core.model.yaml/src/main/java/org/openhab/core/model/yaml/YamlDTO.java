/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.core.model.yaml;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.core.model.yaml.internal.YamlModelRepositoryImpl;

/**
 * The {@link YamlDTO} interface must be implemented by any classes that need to be handled by the
 * {@link YamlModelRepositoryImpl}.
 * <p />
 * Implementations MUST provide {@code equals(Object other)} and {@code hashcode()} methods
 *
 * @author Laurent Garnier - Initial contribution
 */
public interface YamlDTO {

    /**
     * Get the identifier of this element
     *
     * Identifiers
     *
     * - MUST be unique within a model
     * - SHOULD be unique across all models
     *
     * @return the identifier as a string
     */
    @NonNull
    String getId();

    /**
     * Check if this element is valid
     *
     * Implementations
     *
     * - MUST check that at least {link #getId()} returns a non-null value
     * - SHOULD log the reason of failed checks at WARN level
     * - CAN perform additional checks
     *
     * @return {@code true} if all the checks are completed successfully
     */
    boolean isValid();
}
