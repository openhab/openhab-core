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
package org.openhab.core.io.rest.core.internal.tag;

import java.util.List;

import org.openhab.core.semantics.SemanticTag;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A DTO representing a {@link SemanticTag}.
 *
 * @author Jimmy Tanagra - initial contribution
 * @author Laurent Garnier - Class renamed and members uid, description and editable added
 */
@Schema(name = "EnrichedSemanticTag")
public class EnrichedSemanticTagDTO {
    public String uid;
    public String name;
    public String label;
    public String description;
    public List<String> synonyms;
    public boolean editable;

    public EnrichedSemanticTagDTO(SemanticTag tag, boolean editable) {
        this.uid = tag.getUID();
        this.name = tag.getUID().substring(tag.getUID().lastIndexOf("_") + 1);
        this.label = tag.getLabel();
        this.description = tag.getDescription();
        this.synonyms = tag.getSynonyms();
        this.editable = editable;
    }
}
