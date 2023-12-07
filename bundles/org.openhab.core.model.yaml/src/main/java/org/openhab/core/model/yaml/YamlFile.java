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

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link YamlFile} is the DTO base class used to map a YAML configuration file.
 *
 * A YAML configuration file consists of a version and a list of elements.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public abstract class YamlFile {

    private final Logger logger = LoggerFactory.getLogger(YamlFile.class);

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
     * Get the version present in the YAML file.
     *
     * @return the version in the file
     */
    public int getVersion() {
        return version;
    }

    /**
     * Check that the file content is valid.
     * It includes the check of duplicated elements (same identifier) and the check of each element.
     *
     * @return true if all the checks are OK
     */
    public boolean isValid() {
        // Checking duplicated elements
        List<? extends YamlElement> elts = getElements();
        long nbDistinctIds = elts.stream().map(YamlElement::getId).distinct().count();
        if (nbDistinctIds < elts.size()) {
            logger.debug("Elements with same ids detected in the file");
            return false;
        }

        // Checking each element
        for (int i = 0; i < elts.size(); i++) {
            if (!elts.get(i).isValid()) {
                logger.debug("Error in element {}", i + 1);
                return false;
            }
        }
        return true;
    }
}
