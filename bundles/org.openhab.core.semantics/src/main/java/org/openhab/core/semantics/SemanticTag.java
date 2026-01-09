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
package org.openhab.core.semantics;

import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Identifiable;

/**
 * This interface defines the core features of an openHAB semantic tag.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public interface SemanticTag extends Identifiable<String> {

    /**
     * Returns the name of the semantic tag.
     *
     * @return the name of the semantic tag
     */
    String getName();

    /**
     * Returns the UID of the parent tag.
     *
     * @return the UID of the parent tag
     */
    String getParentUID();

    /**
     * Returns the label of the semantic tag.
     *
     * @return semantic tag label or an empty string if undefined
     */
    String getLabel();

    /**
     * Returns the description of the semantic tag.
     *
     * @return semantic tag description or an empty string if undefined
     */
    String getDescription();

    /**
     * Returns the synonyms of the semantic tag.
     *
     * @return semantic tag synonyms as a List
     */
    List<String> getSynonyms();

    /**
     * Returns the localized semantic tag.
     *
     * @param locale the locale to be used
     * @return the localized semantic tag
     */
    SemanticTag localized(Locale locale);
}
