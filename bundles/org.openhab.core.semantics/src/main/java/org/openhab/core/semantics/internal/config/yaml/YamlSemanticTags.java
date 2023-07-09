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
package org.openhab.core.semantics.internal.config.yaml;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.model.core.yaml.YamlElement;
import org.openhab.core.model.core.yaml.YamlFile;
import org.openhab.core.model.core.yaml.YamlParseException;

/**
 * The {@link YamlSemanticTags} is a data transfer object used to serialize a list of semantic tags
 * in a YAML configuration file.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class YamlSemanticTags extends YamlFile {

    public List<YamlSemanticTag> tags = List.of();

    @Override
    public List<? extends YamlElement> getElements() {
        return tags;
    }

    @Override
    protected void checkVersion() throws YamlParseException {
        if (version != 1) {
            throw new YamlParseException("Version 1 required; please convert your file");
        }
    }
}
