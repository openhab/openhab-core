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
 * The {@link YamlElement} interface must be implemented by any classes that need to be handled by the
 * {@link YamlModelRepositoryImpl}.
 * <p />
 * Implementations
 * <ul>
 * <li>MUST have a default constructor to allow deserialization with Jackson</li>
 * <li>MUST provide {@code equals(Object other)} and {@code hashcode()} methods</li>
 * <li>MUST be annotated with {@link YamlElementName} containing the element name</li>
 * <li>SHOULD implement a proper {@code toString()} method</li>
 * </ul>
 *
 * @author Laurent Garnier - Initial contribution
 * @author Jan N. Klug - Refactoring and improvements to JavaDoc
 */
public interface YamlElement {

    /**
     * Get the identifier of this element.
     * <p />
     * <p />
     * Identifiers
     * <ul>
     * <li>MUST be non-null</li>
     * <li>MUST be unique within a model</li>
     * <li>SHOULD be unique across all models</li>
     * </ul>
     *
     * @return the identifier as a string
     */
    @NonNull
    String getId();

    /**
     * Check if this element is valid and should be included in the model.
     * <p />
     * <p />
     * Implementations
     * <ul>
     * <li>MUST check that at least {link #getId()} returns a non-null value</li>
     * <li>SHOULD log the reason of failed checks at WARN level</li>
     * <li>MAY perform additional checks</li>
     * </ul>
     *
     * @return {@code true} if all the checks are completed successfully
     */
    boolean isValid();
}
