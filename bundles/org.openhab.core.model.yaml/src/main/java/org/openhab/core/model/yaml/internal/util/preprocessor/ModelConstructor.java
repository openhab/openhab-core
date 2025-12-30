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
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.model.yaml.internal.util.preprocessor.tags.IncludeObject;
import org.openhab.core.model.yaml.internal.util.preprocessor.tags.RemoveObject;
import org.openhab.core.model.yaml.internal.util.preprocessor.tags.ReplaceObject;
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
import org.yaml.snakeyaml.nodes.SequenceNode;
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
    private static final Tag REPLACE_TAG = new Tag("!replace");
    private static final Tag REMOVE_TAG = new Tag("!remove");

    /**
     * Matches ${...} interpolation patterns in YAML strings.
     *
     * Allows nested quotes like default('{foo}') without prematurely stopping at the inner '}'
     *
     * Examples:
     * - ${name} : Simple variable substitution
     * - ${name|capitalize} : Apply capitalize filter
     * - ${name|default('value')} : Use default if variable is not set
     * - ${name|upper} : Convert to uppercase
     */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("""
            \\$\\{
            (?<content>
                (?:
                    [^}"']+ |
                    "[^"]*" |
                    '[^']*'
                )+
            )
            \\}
            """, Pattern.COMMENTS);

    // Pattern for simple variable names (no pipes, operators)
    private static final Pattern SIMPLE_VAR_PATTERN = Pattern.compile("^\\s*[a-zA-Z_][a-zA-Z0-9_]*\\s*$");

    // Pattern for matching plain YAML scalar that is just ${varname}
    private static final Pattern SIMPLE_VAR_INTERPOLATION = Pattern
            .compile("^\\$\\{\\s*(?<name>[a-zA-Z_][a-zA-Z0-9_]*)\\s*\\}$");

    private final Logger logger = LoggerFactory.getLogger(ModelConstructor.class);

    private final Map<String, Object> variables;
    private final Path currentFile;
    private final boolean finalPass;

    public ModelConstructor(LoaderOptions options, Map<String, Object> variables, Path currentFile, boolean finalPass) {
        super(options);

        this.variables = variables;
        this.currentFile = currentFile;
        this.finalPass = finalPass;

        this.yamlConstructors.put(Tag.NULL, new ConstructNull());
        this.yamlConstructors.put(INCLUDE_TAG, new ConstructInclude());
        this.yamlConstructors.put(REPLACE_TAG, new ConstructReplace());
        this.yamlConstructors.put(REMOVE_TAG, new ConstructRemove());

        if (finalPass) {
            this.yamlConstructors.put(Tag.STR, new ConstructInterpolation());
        }
        logger.trace("ModelConstructor created with vars: {}", variables);
    }

    public class ConstructInterpolation extends AbstractConstruct {

        @Override
        public Object construct(@Nullable Node node) {
            if (node == null) {
                return "";
            }

            ScalarNode scalarNode = (ScalarNode) node;
            DumperOptions.ScalarStyle scalarStyle = scalarNode.getScalarStyle();
            String value = (String) constructScalar(scalarNode);

            logger.debug("ConstructInterpolation: value='{}' (null={}), style={}", value, value == null, scalarStyle);

            if (value == null) {
                logger.warn("{}: constructScalar() returned null on node {}!", currentFile, node);
                return "";
            }

            // don't interpolate single quoted strings
            if (scalarStyle == DumperOptions.ScalarStyle.SINGLE_QUOTED) {
                return value;
            }

            if (scalarStyle == DumperOptions.ScalarStyle.PLAIN) {
                Matcher simpleMatch = SIMPLE_VAR_INTERPOLATION.matcher(value);
                if (simpleMatch.matches()) {
                    // Direct variable lookup without Jinja for unquoted ${varname}
                    // This is a special case to return non-string types like integers, booleans, lists, maps
                    String varName = simpleMatch.group("name");
                    Object varValue = variables.get(varName);
                    logger.debug("Simple variable lookup: ${{}} => '{}'", varName, varValue);
                    return varValue == null ? "" : varValue;
                }
            }

            Matcher matcher = VARIABLE_PATTERN.matcher(value);
            if (!matcher.find()) {
                logger.debug("No variable pattern match for '{}'", value);
                return value;
            }

            // Replace ${...} with content (simple variables directly, complex expressions via Jinja)
            String interpolated = matcher.replaceAll(match -> {
                String content = match.group("content");

                // Check if this is a simple variable name (no filters, operators)
                if (SIMPLE_VAR_PATTERN.matcher(content).matches()) {
                    // Direct variable lookup without Jinja
                    Object varValue = variables.get(content.strip());
                    String renderedValue = varValue == null ? "" : varValue.toString();
                    logger.debug("Simple variable lookup: ${{}} => '{}'", content, renderedValue);
                    return Matcher.quoteReplacement(renderedValue);
                }

                // Complex expression - use Jinja
                String template = "{{ " + content + " }}";
                String rendered = JinjavaTemplateEngine.render(template, variables, currentFile);
                logger.debug("Jinja expression: ${{}} => '{}'", content, rendered);
                return Matcher.quoteReplacement(rendered);
            });

            if (interpolated.isEmpty()) {
                return interpolated;
            }

            // Preserve quoted strings: if original was quoted (double or single), keep it as string
            if (scalarStyle == DumperOptions.ScalarStyle.DOUBLE_QUOTED
                    || scalarStyle == DumperOptions.ScalarStyle.LITERAL
                    || scalarStyle == DumperOptions.ScalarStyle.FOLDED) {
                return interpolated;
            }

            // For plain (unquoted) scalars, allow type inference.
            // Resolve the interpolated node because the type might change e.g.
            // ${var1} => 1: originally a STR, it now becomes !!int 1
            ModelResolver resolver = new ModelResolver();
            Tag newTag = resolver.resolve(NodeId.scalar, interpolated, true);
            // now find the correct constructor for the new node
            Construct constructor = yamlConstructors.get(newTag);
            if (constructor == null) {
                throw new YAMLException("%s: no constructor found for substituted value '%s' => '%s' with tag %s"
                        .formatted(currentFile, value, interpolated, newTag));
            }
            ScalarNode replacedNode = new ScalarNode(newTag, interpolated, scalarNode.getStartMark(),
                    scalarNode.getEndMark(), scalarStyle);
            // finally, construct the new node
            Object result = constructor.construct(replacedNode);
            logger.debug("ConstructInterpolation result: {} (type: {})", result,
                    result == null ? "null" : result.getClass().getSimpleName());
            return result;
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
                    throw new YAMLException(currentFile + ": missing 'file' key in !include: " + includeOptions);
                }

                try {
                    Map<String, Object> vars = Optional.ofNullable(includeOptions.get("vars")).map(Map.class::cast)
                            .orElse(Map.of());
                    return new IncludeObject(fileName, vars);
                } catch (ClassCastException e) {
                    throw new YAMLException(currentFile + ": invalid 'vars' type in !include file: " + fileName
                            + ", vars: " + includeOptions.get("vars") + " (not a map)", e);
                }
            }
            throw new YAMLException(currentFile + ": invalid !include argument type: "
                    + (node == null ? null : node.getClass().getName()));
        }
    }

    private class ConstructReplace extends AbstractConstruct {
        @Override
        public Object construct(@Nullable Node node) {
            if (node instanceof MappingNode mappingNode) {
                Map<Object, Object> map = constructMapping(mappingNode);
                logger.debug("Constructing !replace map node: {}", map);
                return new ReplaceObject(map);
            } else if (node instanceof SequenceNode sequenceNode) {
                Object list = constructSequence(sequenceNode);
                logger.debug("Constructing !replace sequence node: {}", list);
                return new ReplaceObject(list);
            }
            throw new YAMLException(currentFile + ": invalid !replace argument. Expected a map or a list.");
        }
    }

    private class ConstructRemove extends AbstractConstruct {
        @Override
        public Object construct(@Nullable Node node) {
            logger.debug("Constructing !remove node: {}", node);
            return new RemoveObject();
        }
    }
}
