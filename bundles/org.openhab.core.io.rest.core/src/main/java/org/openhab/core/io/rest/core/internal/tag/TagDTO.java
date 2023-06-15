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
package org.openhab.core.io.rest.core.internal.tag;

import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.semantics.SemanticTags;
import org.openhab.core.semantics.Tag;

/**
 * A DTO representing a Semantic {@link Tag}.
 *
 * @author Jimmy Tanagra - initial contribution
 */
@NonNullByDefault
public class TagDTO {
    String name;
    String label;
    List<String> synonyms;

    public TagDTO(Class<? extends Tag> tag, Locale locale) {
        this.name = tag.getSimpleName();
        this.label = SemanticTags.getLabel(tag, locale);
        this.synonyms = SemanticTags.getSynonyms(tag, locale);
    }
}
