/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.semantics.fileconverter;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.converter.ObjectSerializer;
import org.openhab.core.semantics.SemanticTag;

/**
 * {@link SemanticTagSerializer} is the interface to implement by any file generator for {@link SemanticTag} object.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public interface SemanticTagSerializer extends ObjectSerializer<SemanticTag> {

    /**
     * Specify the {@link List} of {@link SemanticTag}s to serialize and associate them with an identifier.
     *
     * @param id the identifier of the {@link SemanticTag} format generation.
     * @param tags the {@link List} of {@link SemanticTag}s to serialize.
     */
    void setSemanticTagsToBeSerialized(String id, List<SemanticTag> tags);
}
