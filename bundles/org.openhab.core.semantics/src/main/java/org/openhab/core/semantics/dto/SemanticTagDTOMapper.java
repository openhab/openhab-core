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
package org.openhab.core.semantics.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.semantics.SemanticTag;
import org.openhab.core.semantics.SemanticTagImpl;

/**
 * The {@link SemanticTagDTOMapper} is an utility class to map semantic tags into
 * semantic tag data transfer objects (DTOs).
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class SemanticTagDTOMapper {

    /**
     * Maps semantic tag DTO into semantic tag object.
     *
     * @param tagDTO the DTO
     * @return the semantic tag object
     */
    public static @Nullable SemanticTag map(@Nullable SemanticTagDTO tagDTO) {
        if (tagDTO == null) {
            throw new IllegalArgumentException("The argument 'tagDTO' must not be null.");
        }
        if (tagDTO.uid == null) {
            throw new IllegalArgumentException("The argument 'tagDTO.uid' must not be null.");
        }

        return new SemanticTagImpl(tagDTO.uid, tagDTO.label, tagDTO.description, tagDTO.synonyms);
    }

    /**
     * Maps semantic tag object into semantic tag DTO.
     *
     * @param tag the semantic tag
     * @return the semantic tag DTO
     */
    public static SemanticTagDTO map(SemanticTag tag) {
        SemanticTagDTO tagDTO = new SemanticTagDTO();
        tagDTO.uid = tag.getUID();
        tagDTO.label = tag.getLabel();
        tagDTO.description = tag.getDescription();
        tagDTO.synonyms = tag.getSynonyms();
        return tagDTO;
    }
}
