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

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link YamlFile} is the interface to manage the generic content of a YAML configuration file.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public interface YamlFile {

    /**
     * Get the list of elements present in the YAML file.
     *
     * @return the list of elements
     */
    List<? extends YamlElement> getElements();

    /**
     * Get the version present in the YAML file.
     *
     * @return the version in the file
     */
    int getVersion();

    /**
     * Check that the file content is valid.
     * It includes the check of duplicated elements (same identifier) and the check of each element.
     *
     * @return true if all the checks are OK
     */
    boolean isValid();
}
