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

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.Construct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;

/**
 * The {@link ModelConstructor} adds extended functionality to the
 * {@link Constructor} class to support:
 *
 * - Nested variable interpolation
 * - <code>!include</code> tag for including other YAML files
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
class ModelConstructor extends Constructor {

    private static final Tag INCLUDE_TAG = new Tag("!include");
    private static final int MAX_VAR_NESTING_DEPTH = 10;

    /**
     * Matches variable interpolation patterns in YAML strings.
     *
     * Supported syntax (subset of bash variable substitution):
     *   - ${var}             : Simple variable substitution. If 'var' is not set, returns empty string.
     *   - ${var-default}     : If 'var' is set but empty, returns empty string . If not set, returns 'default'.
     *   - ${var:-default}    : If 'var' is set but empty, returns 'default'. If not set, returns 'default'.
     *   - Default values can be single-quoted, double-quoted, or unquoted.
     *
     * Regex breakdown:
     *   - (?<name>\\w+)           : Captures the variable name (alphanumeric and underscore).
     *   - (?<separator>:?-)       : Optionally captures the separator (either '-' or ':-').
     *   - '(?<defaultsq>[^']*)'   : Captures single-quoted default value (inside quotes).
     *   - "(?<defaultdq>[^"]*)"   : Captures double-quoted default value (inside quotes).
     *   - (?<default>[^}]*)       : Captures unquoted default value (up to closing brace).
     *   - All default value groups are optional.
     *
     * Additional notes:
     *   - If no default value is provided, an empty string is returned.
     *   - Whitespace inside default values is preserved.
     *   - Nested variable patterns are not matched by this regex (handled separately).
     *   - Invalid patterns (e.g., missing closing brace) will not match.
     */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("""
            \\$\\{
                (?<name>\\w+)
                (?:
                    (?<separator>:?-)
                    (?:
                        '(?<defaultsq>[^']*)' |   # single-quoted default (capture inside quotes)
                        "(?<defaultdq>[^"]*)" |   # double-quoted default (capture inside quotes)
                        (?<default>[^}]*)         # unquoted default
                    )
                )?
            \\}
            """, Pattern.COMMENTS);

    private final Logger logger = LoggerFactory.getLogger(ModelConstructor.class);

    private final Map<String, String> variables;

    public ModelConstructor(Map<String, String> variables) {
        super(new LoaderOptions());

        this.variables = variables;

        this.yamlConstructors.put(INCLUDE_TAG, new ConstructInclude());
        this.yamlConstructors.put(Tag.STR, new ConstructInterpolation());
        this.yamlConstructors.put(Tag.NULL, new ConstructNull());
        logger.trace("ModelConstructor created with vars: {}", variables);
    }

    public class ConstructInterpolation extends AbstractConstruct {

        public Object construct(@Nullable Node node) {
            ScalarNode scalarNode = (ScalarNode) node;

            String value = (String) constructScalar(scalarNode);

            // don't interpolate single quoted strings
            if (scalarNode == null || scalarNode.getScalarStyle() == DumperOptions.ScalarStyle.SINGLE_QUOTED) {
                return value;
            }

            Matcher matcher = VARIABLE_PATTERN.matcher(value);
            if (!matcher.find()) {
                return value;
            }

            String interpolated = value;
            int nestedLevel = 0;

            do {
                interpolated = matcher.replaceAll(match -> {
                    String variableName = match.group("name");
                    String separator = match.group("separator");
                    String defaultSingleQuoted = match.group("defaultsq");
                    String defaultDoubleQuoted = match.group("defaultdq");
                    String defaultUnquoted = match.group("default");
                    String resolved = resolveVariable(variableName, separator,
                            defaultDoubleQuoted != null ? defaultDoubleQuoted
                                    : defaultSingleQuoted != null ? defaultSingleQuoted : defaultUnquoted);
                    logger.debug("Interpolating variable {} => {}", variableName, resolved);
                    return Matcher.quoteReplacement(resolved);
                });
                if (nestedLevel++ > MAX_VAR_NESTING_DEPTH) {
                    throw new YAMLException("Variable nesting is too deep in " + value);
                }
                matcher = VARIABLE_PATTERN.matcher(interpolated);
            } while (matcher.find());

            // resolve the interpolated node because the type might change e.g.
            // ${var1} => 1: originally a STR, it now becomes !!int 1
            ModelResolver resolver = new ModelResolver();
            Tag newTag = resolver.resolve(NodeId.scalar, interpolated, true);
            ScalarNode replacedNode = new ScalarNode(newTag, interpolated, scalarNode.getStartMark(),
                    scalarNode.getEndMark(), scalarNode.getScalarStyle());
            // now find the correct constructor for the new node
            Construct constructor = yamlConstructors.get(newTag);
            if (constructor == null) {
                throw new YAMLException("No constructor found for substituted value '%s' => '%s' with tag %s"
                        .formatted(value, interpolated, newTag));
            }
            // finally, construct the new node
            return constructor.construct(replacedNode);
        }

        /**
         * Implement the logic for missing and unset variables
         *
         * @param name - variable name in the template
         * @param separator - separator in the template, can be :-, -
         * @param defaultValue - default value or the error in the template.
         *            If defaultValue is null, return an empty string.
         * @return the value to resolve in the template
         */
        private String resolveVariable(String name, @Nullable String separator, @Nullable String defaultValue) {
            String value = variables.get(name);
            if (isSetAndNotEmpty(value)) {
                return value;
            }
            if (shouldUseDefault(separator, value)) {
                return getDefaultOrEmpty(defaultValue);
            }
            return "";
        }

        private boolean isSetAndNotEmpty(@Nullable String value) {
            return value != null && !value.isEmpty();
        }

        private boolean shouldUseDefault(@Nullable String separator, @Nullable String value) {
            if (separator == null) {
                return false;
            }
            if (separator.startsWith(":")) {
                return value == null || value.isEmpty();
            } else {
                return value == null;
            }
        }

        private String getDefaultOrEmpty(@Nullable String defaultValue) {
            return defaultValue != null ? defaultValue : "";
        }
    }

    private class ConstructNull extends AbstractConstruct {

        // Return an empty string for null values so that the keys are not removed from the map
        // This matches the behavior of Jackson's parser, otherwise some tests will fail
        @Override
        public Object construct(@Nullable Node node) {
            if (node != null) {
                constructScalar((ScalarNode) node);
            }
            return "";
        }
    }

    private class ConstructInclude extends AbstractConstruct {

        @Override
        public Object construct(@Nullable Node node) {
            logger.debug("Constructing !include node: {}", node);
            if (node instanceof ScalarNode scalarNode) {
                String value = constructScalar(scalarNode).trim();
                return new IncludeObject(value, Map.of());
            } else if (node instanceof MappingNode mappingNode) {
                Map<Object, Object> includeOptions = constructMapping(mappingNode);

                String fileName = (String) includeOptions.get("file");
                if (fileName == null) {
                    logger.warn("Missing 'file' key in !include: {}", includeOptions);
                    return Map.of();
                }

                @SuppressWarnings("unchecked")
                Map<String, String> vars = Optional.ofNullable(includeOptions.get("vars")).filter(Map.class::isInstance)
                        .map(Map.class::cast).orElse(Map.of());
                return new IncludeObject(fileName, vars);
            } else {
                logger.warn("Invalid !include argument type: {}", node == null ? null : node.getClass().getName());
            }
            return Map.of();
        }
    }
}
