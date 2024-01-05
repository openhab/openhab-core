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
package org.openhab.core.model.yaml.internal.semantics;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.model.yaml.AbstractYamlFile;
import org.openhab.core.model.yaml.YamlElement;

/**
 * The {@link YamlSemanticTags} is a data transfer object used to serialize a list of semantic tags
 * in a YAML configuration file.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class YamlSemanticTags extends AbstractYamlFile {

    public List<YamlSemanticTag> tags = List.of();

    @Override
    public List<? extends YamlElement> getElements() {
        return tags;
    }
}
