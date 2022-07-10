/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.core.transform;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.Identifiable;

/**
 * The {@link TransformationConfiguration} encapsulates a transformation configuration
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class TransformationConfiguration implements Identifiable<String> {
    private final String uid;
    private final String label;
    private final String type;
    private final @Nullable String context; // for backward deserialization compatibility
    private final @Nullable String language;
    private final String content;

    /**
     * @param uid the configuration UID. The format is config:&lt;type&gt;:&lt;name&gt;[:&lt;locale&gt;]. For backward
     *            compatibility also filenames are allowed.
     * @param type the type of the configuration (file extension for file-based providers)
     * @param context context (e.g. script type or blockly blocks) of the configuration as JSON string
     * @param language the language of this configuration (<code>null</code> if not set)
     * @param content the content of this configuration
     */
    public TransformationConfiguration(String uid, String label, String type, String context, @Nullable String language,
            String content) {
        this.uid = uid;
        this.label = label;
        this.type = type;
        this.context = context;
        this.content = content;
        this.language = language;
    }

    @Override
    public String getUID() {
        return uid;
    }

    public String getLabel() {
        return label;
    }

    public String getType() {
        return type;
    }

    public String getContext() {
        return Objects.requireNonNullElse(context, "{}");
    }

    public @Nullable String getLanguage() {
        return language;
    }

    public String getContent() {
        return content;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransformationConfiguration that = (TransformationConfiguration) o;
        return uid.equals(that.uid) && label.equals(that.label) && type.equals(that.type)
                && Objects.equals(context, that.context) && Objects.equals(language, that.language)
                && content.equals(that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid, label, type, context, language, content);
    }

    @Override
    public String toString() {
        return "TransformationConfiguration{uid='" + uid + "', label='" + label + "', type='" + type + "', context = '"
                + context + "', language='" + language + "', content='" + content + "'}";
    }
}
