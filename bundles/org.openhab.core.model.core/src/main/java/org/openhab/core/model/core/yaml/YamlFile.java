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
package org.openhab.core.model.core.yaml;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link YamlFile} is the DTO base class used to map a YAML configuration file.
 *
 * A YAML configuration file consists of a version and a list of elements.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public abstract class YamlFile {

    /**
     * YAML file version
     */
    public int version;

    /**
     * Get the list of elements present in the YAML file.
     *
     * @return the list of elements
     */
    public abstract List<? extends YamlElement> getElements();

    /**
     * Check that the version in the YAML file in the expected one.
     *
     * @throws YamlParseException if the version in the file is not the expected one
     */
    protected abstract void checkVersion() throws YamlParseException;

    /**
     * Check that the file content is valid.
     * It includes the check of the version, the check of duplicated elements (same identifier)
     * and the check of each element.
     *
     * @throws YamlParseException if something is invalid
     */
    public void checkValidity() throws YamlParseException {
        // Checking version
        checkVersion();

        // Checking duplicated elements
        List<? extends YamlElement> elts = getElements();
        long nbDistinctIds = elts.stream().map(YamlElement::getId).distinct().count();
        if (nbDistinctIds < elts.size()) {
            throw new YamlParseException("Elements with same ids detected in the file");
        }

        // Checking each element
        for (int i = 0; i < elts.size(); i++) {
            try {
                elts.get(i).checkValidity();
            } catch (YamlParseException e) {
                throw new YamlParseException("Error in element " + (i + 1) + ": " + e.getMessage());
            }
        }
    }
}
