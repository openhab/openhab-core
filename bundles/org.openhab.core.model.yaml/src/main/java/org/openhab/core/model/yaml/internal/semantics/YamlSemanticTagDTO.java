/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.model.yaml.YamlElement;
import org.openhab.core.model.yaml.YamlElementName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link YamlSemanticTagDTO} is a data transfer object used to serialize a semantic tag
 * in a YAML configuration file.
 *
 * @author Laurent Garnier - Initial contribution
 */
@YamlElementName("tags")
public class YamlSemanticTagDTO implements YamlElement {

    private final Logger logger = LoggerFactory.getLogger(YamlSemanticTagDTO.class);

    public String uid;
    public String label;
    public String description;
    public List<String> synonyms;

    public YamlSemanticTagDTO() {
    }

    @Override
    public @NonNull String getId() {
        return uid;
    }

    @Override
    public boolean isValid() {
        if (uid == null) {
            logger.debug("uid missing");
            return false;
        }
        return true;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        YamlSemanticTagDTO that = (YamlSemanticTagDTO) obj;
        return Objects.equals(uid, that.uid) && Objects.equals(label, that.label)
                && Objects.equals(description, that.description) && Objects.equals(synonyms, that.synonyms);
    }
}
