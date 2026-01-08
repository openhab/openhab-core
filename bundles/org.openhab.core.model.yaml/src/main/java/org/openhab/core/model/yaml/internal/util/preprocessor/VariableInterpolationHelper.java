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
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.hubspot.jinjava.interpret.InterpretException;

/**
 * The {@link VariableInterpolationHelper} provides utility methods for performing
 * variable substitution and expression evaluation in YAML processing.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
class VariableInterpolationHelper {

    /**
     * Matches ${...} interpolation patterns in YAML strings.
     *
     * Allows nested quotes like default('{foo}') without prematurely stopping at the inner '}'
     *
     * Examples:
     * - ${name} : Simple variable substitution
     * - ${ name|capitalize } : Apply capitalize filter
     * - ${ name|default('value') } : Use default if variable is not set
     * - ${ name|upper } : Convert to uppercase
     */
    static final Pattern DEFAULT_SUBSTITUTION_PATTERN = Pattern.compile("""
            \\$\\{
            (?<content>
                (?:
                    "[^"]*" |
                    '[^']*' |
                    [^}]+
                )*
            )
            \\}
            """, Pattern.COMMENTS);

    private VariableInterpolationHelper() {
        // Utility class, no instances
    }

    /**
     * Compiles a substitution pattern from begin and end delimiters.
     *
     * @param begin the opening delimiter (e.g., "${")
     * @param end the closing delimiter (e.g., "}")
     * @return the compiled pattern
     */
    static Pattern compileSubstitutionPattern(String begin, String end) {
        if ("${".equals(begin) && "}".equals(end)) {
            return DEFAULT_SUBSTITUTION_PATTERN;
        }
        String quotedBegin = Pattern.quote(begin);
        String quotedEnd = Pattern.quote(end);
        // Allow quoted segments to contain the end delimiter; otherwise stop at the first closing delimiter.
        String content = """
                "[^"]*"|'[^']*'|(?:(?!%s).)
                """.formatted(quotedEnd).strip();
        String regex = quotedBegin + "(?<content>(" + content + ")+?)" + quotedEnd;
        return Pattern.compile(regex, Pattern.DOTALL);
    }

    /**
     * Evaluates a variable value, performing substitution if needed.
     * Returns the native object type if the entire value is a single placeholder,
     * otherwise returns a string with interpolated values.
     *
     * @param value the raw string value
     * @param pattern the substitution pattern to use
     * @param isPlainScalar whether the scalar uses plain style (for preserving type)
     * @param variables the variable map
     * @param contextDescription description of the context for error messages (e.g., file path)
     * @param finalPass whether this is the final pass
     * @return the evaluated value
     */
    static Object evaluateValue(String value, Pattern pattern, boolean isPlainScalar, Map<String, Object> variables,
            String contextDescription, boolean finalPass) {
        Matcher matcher = pattern.matcher(value);
        if (!matcher.find()) {
            return value;
        }

        // If the whole value is exactly one ${...}, evaluate via Jinjava and return the native object.
        // This preserves non-string types (maps, lists, numbers, booleans) when the value is a plain
        // placeholder.
        if (isPlainScalar && matcher.matches()) {
            String content = matcher.group("content");
            return evaluateExpression(content, variables, finalPass, contextDescription);
        }

        // Replace ${...} with content (simple variables directly, complex expressions via Jinja)
        String interpolated = matcher.replaceAll(match -> {
            String content = match.group("content");
            String rendered = evaluateExpression(content, variables, finalPass, contextDescription).toString();
            return Matcher.quoteReplacement(rendered);
        });

        return interpolated;
    }

    /**
     * Evaluates a Jinjava expression.
     *
     * @param expression the expression to evaluate
     * @param variables the variable map
     * @param finalPass whether this is the final pass
     * @param contextDescription description of the context for error messages
     * @return the evaluated result
     * @throws IllegalArgumentException if evaluation fails
     */
    static Object evaluateExpression(String expression, Map<String, Object> variables, boolean finalPass,
            String contextDescription) {
        try {
            Object rendered = JinjavaTemplateEngine.renderObject(expression, variables, finalPass);
            return Objects.requireNonNullElse(rendered, "");
        } catch (InterpretException e) {
            throw new IllegalArgumentException("%s: %s".formatted(contextDescription, e.getMessage()), e);
        }
    }

    /**
     * Formats a context description from a file path and optional location string.
     *
     * @param currentFile the file being processed
     * @param location optional location within the file (e.g., "[line:col]")
     * @return formatted context string
     */
    static String formatContext(Path currentFile, String location) {
        if (location.isEmpty()) {
            return currentFile.toString();
        }
        return "%s:%s".formatted(currentFile, location);
    }
}
