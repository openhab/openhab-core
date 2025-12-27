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

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.yaml.snakeyaml.error.YAMLException;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.JinjavaConfig;
import com.hubspot.jinjava.interpret.InterpretException;

/**
 * Wrapper around Jinjava template engine for rendering ${...} expressions.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
class JinjavaTemplateEngine {

    private static final Jinjava JINJAVA;

    static {
        JinjavaConfig config = JinjavaConfig.newBuilder() //
                .withFailOnUnknownTokens(false) // don't fail on unknown variables
                .withMaxRenderDepth(10) //
                .build();
        JINJAVA = new Jinjava(config);
    }

    /**
     * Render a Jinjava template with the given variables.
     *
     * @param template the Jinjava template string (e.g., "{{ name|capitalize }}")
     * @param variables the variable context
     * @param currentFile the current file being processed (for error messages)
     * @return the rendered string
     */
    public static String render(String template, Map<String, Object> variables, Path currentFile) {
        try {
            return JINJAVA.render(template, new HashMap<>(variables));
        } catch (InterpretException e) {
            throw new YAMLException(currentFile + ": (Jinjava) " + e.getMessage(), e);
        }
    }
}
