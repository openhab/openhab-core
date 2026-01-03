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
import com.hubspot.jinjava.interpret.Context;
import com.hubspot.jinjava.interpret.InterpretException;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;

/**
 * Wrapper around Jinjava template engine for rendering ${...} expressions.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
class JinjavaTemplateEngine {

    private static final JinjavaConfig JINJAVA_CONFIG;
    private static final Jinjava JINJAVA;

    static {
        JINJAVA_CONFIG = JinjavaConfig.newBuilder() //
                .withFailOnUnknownTokens(true) // surface undefined variables instead of silently ignoring
                .withMaxRenderDepth(10) //
                .build();
        JINJAVA = new Jinjava(JINJAVA_CONFIG);
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

    /**
     * Evaluate a Jinjava expression and return the raw object result (no string coercion).
     *
     * @param expression the expression content without delimiters (e.g., "user.profile")
     * @param variables the variable context
     * @param currentFile the current file being processed (for error messages)
     * @return the evaluated object (map/list/number/boolean/string)
     */
    public static Object renderObject(String expression, Map<String, Object> variables, Path currentFile) {
        try {
            Context context = new Context(JINJAVA.getGlobalContext());
            context.putAll(variables);
            JinjavaInterpreter interpreter = new JinjavaInterpreter(JINJAVA, context, JINJAVA_CONFIG);
            Object result = interpreter.resolveELExpression(expression, 0);
            return normalizeType(result);
        } catch (InterpretException e) {
            throw new YAMLException(currentFile + ": (Jinjava) " + e.getMessage(), e);
        }
    }

    /**
     * Normalize types returned by Jinjava to avoid Long where Integer is expected.
     * This is to maintain the same behavior as SnakeYAML which uses Integer for small integers.
     *
     * Example:
     *
     * ```yaml
     * native: 1 # returns a Java Integer
     * jinja: ${ 1 } # before normalization returns a Java Long
     * ```
     *
     * @param obj the object to normalize
     * @return the normalized object
     */
    private static Object normalizeType(Object obj) {
        if (obj instanceof Long longValue) {
            // Check if the value fits in an Integer
            if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
                return Math.toIntExact(longValue);
            }
        }
        return obj;
    }
}
