/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link YamlElement} interface offers an identifier and a check validity method
 * to any element defined in a YAML configuration file.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public interface YamlElement {

    /**
     * Get the identifier of the YAML element
     *
     * @return the identifier as a string
     */
    String getId();

    /**
     * Check that the YAML element is valid
     *
     * @return true if all the checks are OK
     */
    boolean isValid();
}
