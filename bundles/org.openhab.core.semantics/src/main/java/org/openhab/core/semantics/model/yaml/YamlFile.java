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
package org.openhab.core.semantics.model.yaml;

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

    public int version;

    public abstract List<? extends YamlElement> getElements();

    protected abstract void checkVersion() throws YamlParseException;

    public void checkValidity() throws YamlParseException {
        checkVersion();
        List<? extends YamlElement> elts = getElements();
        long nbDistinctIds = elts.stream().map(YamlElement::getId).distinct().count();
        if (nbDistinctIds < elts.size()) {
            throw new YamlParseException((elts.size() - nbDistinctIds + 1) + " elements with same ids");
        }
        for (int i = 0; i < elts.size(); i++) {
            try {
                elts.get(i).checkValidity();
            } catch (YamlParseException e) {
                throw new YamlParseException("Error in element " + (i + 1) + ": " + e.getMessage());
            }
        }
    }
}
