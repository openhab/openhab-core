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
package org.openhab.core.semantics;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This is the main implementing class of the {@link SemanticTag} interface.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class SemanticTagImpl implements SemanticTag {

    private static final String TAGS_BUNDLE_NAME = "tags";

    private String uid;
    private String name;
    private String parent;
    private String label;
    private String description;
    private List<String> synonyms;

    public SemanticTagImpl(String uid, @Nullable String label, @Nullable String description,
            @Nullable List<String> synonyms) {
        this(uid, label, description);
        if (synonyms != null) {
            this.synonyms = new ArrayList<>();
            for (String synonym : synonyms) {
                this.synonyms.add(synonym.trim());
            }
        }
    }

    public SemanticTagImpl(String uid, @Nullable String label, @Nullable String description,
            @Nullable String synonyms) {
        this(uid, label, description);
        if (synonyms != null && !synonyms.isBlank()) {
            this.synonyms = new ArrayList<>();
            for (String synonym : synonyms.split(",")) {
                this.synonyms.add(synonym.trim());
            }
        }
    }

    private SemanticTagImpl(String uid, @Nullable String label, @Nullable String description) {
        this.uid = uid;
        int idx = uid.lastIndexOf("_");
        if (idx < 0) {
            this.name = uid.trim();
            this.parent = "";
        } else {
            this.name = uid.substring(idx + 1).trim();
            this.parent = uid.substring(0, idx).trim();
        }
        this.label = label == null ? "" : label.trim();
        this.description = description == null ? "" : description.trim();
        this.synonyms = List.of();
    }

    @Override
    public String getUID() {
        return uid;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getParentUID() {
        return parent;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public List<String> getSynonyms() {
        return synonyms;
    }

    @Override
    public SemanticTag localized(Locale locale) {
        ResourceBundle rb = ResourceBundle.getBundle(TAGS_BUNDLE_NAME, locale,
                Control.getNoFallbackControl(Control.FORMAT_PROPERTIES));
        String label;
        List<String> synonyms;
        String description;
        try {
            String entry = rb.getString(uid);
            int idx = entry.indexOf(",");
            if (idx >= 0) {
                label = entry.substring(0, idx);
                String synonymsCsv = entry.substring(idx + 1);
                synonyms = synonymsCsv.isBlank() ? null : List.of(synonymsCsv.split(","));
            } else {
                label = entry;
                synonyms = null;
            }
        } catch (MissingResourceException e) {
            label = getLabel();
            synonyms = getSynonyms();
        }
        try {
            description = rb.getString(uid + "__description");
        } catch (MissingResourceException e) {
            description = getDescription();
        }

        return new SemanticTagImpl(uid, label, description, synonyms);
    }
}
