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
 * The {@link AbstractYamlFile} is the DTO base class used to map a YAML configuration file.
 *
 * A YAML configuration file consists of a version and a list of elements.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractYamlFile implements YamlFile {

    private final Logger logger = LoggerFactory.getLogger(AbstractYamlFile.class);

    /**
     * YAML file version
     */
    public int version;

    @Override
    public abstract List<? extends YamlElement> getElements();

    @Override
    public int getVersion() {
        return version;
    }

    @Override
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
