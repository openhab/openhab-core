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
package org.openhab.core.model.yaml.internal.util.preprocessor.constructor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.model.yaml.internal.util.preprocessor.StringInterpolator;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;

/**
 * The {@link ConstructSub} is the constructor used on the <code>!sub</code> tag
 * to keep track of custom substitution patterns.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
class ConstructSub extends AbstractConstruct {
    ModelConstructor constructor;

    ConstructSub(ModelConstructor constructor) {
        this.constructor = constructor;
    }

    @Override
    @NonNullByDefault({})
    public Object construct(Node node) {
        Pattern patternFromTag = extractPattern(Objects.requireNonNull(node.getTag()));
        Pattern pattern = patternFromTag != null ? patternFromTag : StringInterpolator.DEFAULT_SUBSTITUTION_PATTERN;

        constructor.trackPattern(pattern);
        return constructor.constructByType(node);
    }

    private @Nullable Pattern extractPattern(Tag tag) {
        String raw = tag.getValue();
        String decoded = URLDecoder.decode(raw, StandardCharsets.UTF_8);

        if (!decoded.startsWith(ModelConstructor.SUB_TAG)) {
            return null;
        }

        String suffix = decoded.substring(ModelConstructor.SUB_TAG.length());
        if (!suffix.startsWith(":pattern=")) {
            return null;
        }

        String patternSpec = suffix.substring(":pattern=".length());
        int separator = patternSpec.indexOf("..");
        if (separator <= 0 || separator >= patternSpec.length() - 2) {
            return null;
        }

        String begin = patternSpec.substring(0, separator);
        String end = patternSpec.substring(separator + 2);
        if (begin.isEmpty() || end.isEmpty()) {
            return null;
        }

        return StringInterpolator.compileSubstitutionPattern(begin, end);
    }
}
