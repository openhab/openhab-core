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

    private static final Tag SUB_TAG = new Tag("!sub");

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
     * - ${ name|capitalize } : Apply capitalize filter
     * - ${ name|default('value') } : Use default if variable is not set
     * - ${ name|upper } : Convert to uppercase
     */
    private static final Pattern SUBSTITUTION_PATTERN = Pattern.compile("""
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

    private final Logger logger = LoggerFactory.getLogger(ModelConstructor.class);

    private final Map<String, Object> variables;
    private final Path currentFile;

    public ModelConstructor(LoaderOptions options, Map<String, Object> variables, Path currentFile, boolean finalPass) {
        super(options);

        this.variables = variables;
        this.currentFile = currentFile;

        this.yamlConstructors.put(Tag.NULL, new ConstructNull());

        // The first pass is only to gather variables, so we use a no-op constructor for efficiency
        this.yamlConstructors.put(SUB_TAG, finalPass ? new ConstructInterpolation() : new ConstructEmpty());
        this.yamlConstructors.put(INCLUDE_TAG, finalPass ? new ConstructInclude() : new ConstructEmpty());
        this.yamlConstructors.put(REPLACE_TAG, finalPass ? new ConstructReplace() : new ConstructEmpty());
        this.yamlConstructors.put(REMOVE_TAG, finalPass ? new ConstructRemove() : new ConstructEmpty());

        logger.trace("ModelConstructor created with vars: {}", variables);
    }

    public class ConstructInterpolation extends AbstractConstruct {

        @Override
        public Object construct(@Nullable Node node) {

            if (node instanceof ScalarNode scalarNode) {
                String value = (String) constructScalar(scalarNode);
                DumperOptions.ScalarStyle scalarStyle = scalarNode.getScalarStyle();
                logger.debug("ConstructInterpolation: value='{}' (null={})", value, value == null);

                Matcher matcher = SUBSTITUTION_PATTERN.matcher(value);
                if (!matcher.find()) {
                    logger.debug("No variable pattern match for '{}'", value);
                    return value;
                }

                // If the whole value is exactly one ${...}, evaluate via Jinjava and return the native object.
                // This preserves non-string types (maps, lists, numbers, booleans) when the value is a plain
                // placeholder.
                if (matcher.matches()) {
                    String content = matcher.group("content").trim();
                    Object rendered = JinjavaTemplateEngine.renderObject(content, variables, currentFile);
                    logger.debug("Jinja object expression: ${{}} => '{}' (type: {})", content, rendered,
                            rendered == null ? "null" : rendered.getClass().getSimpleName());
                    return Objects.requireNonNullElse(rendered, "");
                }

                // Replace ${...} with content (simple variables directly, complex expressions via Jinja)
                String interpolated = matcher.replaceAll(match -> {
                    String content = match.group("content");
                    String template = "{{ " + content + " }}";
                    String rendered = JinjavaTemplateEngine.render(template, variables, currentFile);
                    logger.debug("Jinja expression: ${{}} => '{}'", content, rendered);
                    return Matcher.quoteReplacement(rendered);
                });

                if (interpolated.isEmpty()) {
                    return interpolated;
                }

                // Preserve quoted strings: if original was quoted (double or single), keep it as string
                if (scalarStyle != DumperOptions.ScalarStyle.PLAIN) {
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
                return Objects.requireNonNullElse(result, "");
            } else {
                throw new YAMLException(
                        "%s: !sub can only be applied to scalar nodes, but found: %s".formatted(currentFile, node));
            }
        }
    }

    private static class ConstructNull extends AbstractConstruct {

        // Return an empty string for null values so that the keys are not removed from the map
        // This matches the behavior of Jackson's parser, otherwise some tests will fail
        @Override
        public Object construct(@Nullable Node node) {
            // Always return empty string, regardless of node content
            return "";
        }
    }

    private static class ConstructEmpty extends AbstractConstruct {

        // During non-final pass, ignore special tags and return empty string
        @Override
        public Object construct(@Nullable Node node) {
            return "";
        }
    }

    private class ConstructInclude extends AbstractConstruct {

        @Override
        @SuppressWarnings("unchecked")
        public Object construct(@Nullable Node node) {
            logger.debug("Constructing !include node: {}", node);
            if (node instanceof ScalarNode scalarNode) {
                // ScalarNode's constructor ensured that its value is not null
                String value = constructScalar(scalarNode).trim();
                return new IncludeObject(value, Map.of());
            } else if (node instanceof MappingNode mappingNode) {
                Map<Object, Object> includeOptions = constructMapping(mappingNode);

                String fileName = (String) includeOptions.get("file");
                if (fileName == null) {
                    throw new YAMLException(currentFile + ": missing 'file' key in !include: " + includeOptions);
                }

                Object varsObj = includeOptions.get("vars");
                if (varsObj == null) {
                    return new IncludeObject(fileName, Map.of());
                } else if (varsObj instanceof Map<?, ?>) {
                    return new IncludeObject(fileName, (Map<String, Object>) varsObj);
                } else {
                    throw new YAMLException(currentFile + ": invalid 'vars' type in !include file: " + fileName
                            + ", vars: " + varsObj + " (not a map)");
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
