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
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.model.core.yaml.YamlElement;
import org.openhab.core.model.core.yaml.YamlParseException;

/**
 * The {@link YamlSemanticTag} is a data transfer object used to serialize a semantic tag
 * in a YAML configuration file.
 *
 * @author Laurent Garnier - Initial contribution
 */
public class YamlSemanticTag implements YamlElement {

    public String uid;
    public String label;
    public String description;
    public List<String> synonyms;

    public YamlSemanticTag() {
    }

    @Override
    public String getId() {
        return uid;
    }

    @Override
    public void checkValidity() throws YamlParseException {
        if (uid == null) {
            throw new YamlParseException("uid missing");
        }
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        YamlSemanticTag that = (YamlSemanticTag) obj;
        return Objects.equals(uid, that.uid) && Objects.equals(label, that.label)
                && Objects.equals(description, that.description) && Objects.equals(synonyms, that.synonyms);
    }
}
