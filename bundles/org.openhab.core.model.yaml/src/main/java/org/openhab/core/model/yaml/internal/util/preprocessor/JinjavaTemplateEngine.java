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
package org.openhab.core.model.yaml.internal.util.preprocessor;

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.JinjavaConfig;
import com.hubspot.jinjava.features.FeatureConfig;
import com.hubspot.jinjava.features.FeatureStrategies;
import com.hubspot.jinjava.interpret.Context;
import com.hubspot.jinjava.interpret.InterpretException;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.interpret.TemplateError;

/**
 * Wrapper around Jinjava template engine for rendering ${...} expressions.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
class JinjavaTemplateEngine {

    private static final JinjavaConfig STRICT_CONFIG;
    private static final JinjavaConfig LENIENT_CONFIG;
    private static final Jinjava STRICT_JINJAVA;
    private static final Jinjava LENIENT_JINJAVA;

    static {
        STRICT_CONFIG = JinjavaConfig.newBuilder() //
                .withFeatureConfig(FeatureConfig.newBuilder()
                        .add(JinjavaInterpreter.OUTPUT_UNDEFINED_VARIABLES_ERROR, FeatureStrategies.ACTIVE).build())
                .withFailOnUnknownTokens(true) //
                .withMaxRenderDepth(1) //
                .withMaxMacroRecursionDepth(0) // We don't use macros, disable recursion limit
                .withEnableRecursiveMacroCalls(false) //
                .build();

        LENIENT_CONFIG = JinjavaConfig.newBuilder() //
                .withFailOnUnknownTokens(false) //
                .withMaxRenderDepth(1) //
                .withMaxMacroRecursionDepth(0) // We don't use macros, disable recursion limit
                .withEnableRecursiveMacroCalls(false) //
                .build();

        STRICT_JINJAVA = new Jinjava(STRICT_CONFIG);
        LENIENT_JINJAVA = new Jinjava(LENIENT_CONFIG);
    }

    /**
     * Evaluate a Jinjava expression and return the raw object result (no string coercion).
     *
     * @param expression the expression content without delimiters (e.g., "user.profile")
     * @param variables the variable context
     * @return the evaluated object (map/list/number/boolean/string)
     */
    public static Object renderObject(String expression, Map<String, Object> variables, boolean finalPass)
            throws InterpretException {
        Jinjava jinjava = finalPass ? STRICT_JINJAVA : LENIENT_JINJAVA;
        JinjavaConfig config = finalPass ? STRICT_CONFIG : LENIENT_CONFIG;

        @SuppressWarnings("null")
        Context context = new Context(jinjava.getGlobalContext(), variables);
        JinjavaInterpreter interpreter = new JinjavaInterpreter(jinjava, context, config);
        Object result = interpreter.resolveELExpression(expression, 0);
        List<TemplateError> errors = interpreter.getErrorsCopy();
        if (!errors.isEmpty()) {
            String message = errors.getFirst().getMessage().replaceAll("InterpretException: ", "");
            throw new InterpretException(message);
        }
        return normalizeType(result);
    }

    /**
     * Normalize types returned by Jinjava to match SnakeYAML's
     *
     * Jinjava tends to return Long for all integer numbers.
     * SnakeYAML, however, uses Integer for small integers.
     * Normalize accordingly to avoid type mismatches.
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
