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
package org.openhab.core.model.yaml.internal.util.preprocessor;

import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.Nullable;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.resolver.Resolver;

/**
 * The {@link ModelResolver} class is a custom resolver for YAML
 * that follows the openHAB model syntax.
 *
 * Resolves only "true" and "false" (case insensitive) to boolean values.
 * This matches the behavior of Jackson's PARSE_BOOLEAN_LIKE_WORDS_AS_STRINGS
 * https://github.com/FasterXML/jackson-dataformats-text/blob/58de8215e55fab161ec8a287bd6b7099926926dd/yaml/src/main/java/com/fasterxml/jackson/dataformat/yaml/YAMLParser.java#L689-L691
 *
 * The default behavior of SnakeYAML is to resolve "True", "False", "yes",
 * "no", "on", "off", etc. to boolean values.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
class ModelResolver extends Resolver {
    public static final Pattern BOOL = Pattern.compile("^(?:true|false)$", Pattern.CASE_INSENSITIVE);

    @Override
    public void addImplicitResolver(@Nullable Tag tag, @Nullable Pattern regexp, @Nullable String first, int limit) {
        if (tag == Tag.BOOL) {
            regexp = BOOL;
            first = "tTfF";
        }
        super.addImplicitResolver(tag, regexp, first, limit);
    }
}
