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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholders.IncludePlaceholder;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholders.RemovePlaceholder;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholders.ReplacePlaceholder;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholders.SubstitutionPlaceholder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

/**
 * The {@link ModelConstructor} adds extended functionality to the
 * {@link Constructor} class to support:
 *
 * - Returning empty strings for null values to maintain compatibility with Jackson parser
 * - <code>!sub</code> tag for variable interpolation
 * - <code>!include</code> tag for including other YAML files
 * - <code>!replace</code> tag for replacing parts of the model in a package
 * - <code>!remove</code> tag for removing parts of the model in a package
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
class ModelConstructor extends Constructor {

    // This is implemented as a multi-constructor below.
    private static final String SUB_TAG = "!sub";
    private static final Tag NOSUB_TAG = new Tag("!nosub");

    private static final Tag INCLUDE_TAG = new Tag("!include");
    private static final Tag REPLACE_TAG = new Tag("!replace");
    private static final Tag REMOVE_TAG = new Tag("!remove");

    private static final String DEFAULT_BEGIN = "${";
    private static final String DEFAULT_END = "}";

    private final boolean finalPass;
    private final Map<String, Object> variables;
    private final Path currentFile;
    private final YamlPreprocessor preprocessor;

    private final Representer representer = new Representer(new DumperOptions());

    private final Deque<Boolean> substitutionStack = new ArrayDeque<>();
    private final Deque<Pattern> substitutionPatternStack = new ArrayDeque<>();

    private final Logger logger = LoggerFactory.getLogger(ModelConstructor.class);

    public ModelConstructor(LoaderOptions options, Map<String, Object> variables, YamlPreprocessor preprocessor,
            boolean finalPass) {
        super(options);

        this.finalPass = finalPass;
        this.variables = variables;
        this.currentFile = preprocessor.getFile();
        this.preprocessor = preprocessor;
        this.substitutionStack.push(false);
        this.substitutionPatternStack
                .push(VariableInterpolationHelper.compileSubstitutionPattern(DEFAULT_BEGIN, DEFAULT_END));

        this.yamlConstructors.put(Tag.NULL, new ConstructNull());
        this.yamlConstructors.put(NOSUB_TAG, new ConstructDefault());

        // The first pass is only to gather variables, so we use a no-op constructor for efficiency
        this.yamlConstructors.put(INCLUDE_TAG, finalPass ? new ConstructInclude() : new ConstructEmpty());
        this.yamlConstructors.put(REPLACE_TAG, finalPass ? new ConstructReplace() : new ConstructEmpty());
        this.yamlConstructors.put(REMOVE_TAG, finalPass ? new ConstructRemove() : new ConstructEmpty());

        // Perform substitution in the first pass so that variables can refer to other variables
        this.yamlConstructors.put(Tag.STR, new ConstructInterpolation());

        this.yamlMultiConstructors.put(SUB_TAG, new ConstructSub());

        logger.trace("ModelConstructor created with vars: {}", variables);
    }

    @Override
    @NonNullByDefault({})
    protected Object constructObject(Node node) {
        boolean parent = substitutionStack.peek();
        boolean enabled = resolveSubstitution(node.getTag(), parent);
        substitutionStack.push(enabled);
        substitutionPatternStack.push(substitutionPatternStack.peek());
        try {
            return super.constructObject(node);
        } finally {
            substitutionPatternStack.pop();
            substitutionStack.pop();
        }
    }

    /**
     * Intercept flattenMapping to handle merge keys (<<:) with IncludePlaceholder resolution.
     *
     * <ul>
     * <li>Case 1: <code><<: !include {file: other.yaml}</code>
     * <li>Case 2: <code><<: [ !include {file: one.yaml}, !include {file: two.yaml} ]</code>
     * </ul>
     *
     * Normally when SnakeYAML handles merge keys against an !include tag,
     * it would strip the tag and treat the value as a normal mapping:
     * <code><<: {file: other.yaml}</code>
     * <p>
     * We need to intercept this process to construct the !include node,
     * resolve the resulting IncludePlaceholder, then reconstruct the node
     * with the included content so that SnakeYAML can perform the merge as expected.
     *
     * @param node
     */
    @Override
    @NonNullByDefault({})
    protected void flattenMapping(MappingNode node) {
        if (node.isMerged() && finalPass) {
            List<NodeTuple> originalTuples = new ArrayList<>(node.getValue());
            node.getValue().clear();

            // Now process merge keys (includes)
            // find all merge keys (<<), resolve any IncludePlaceholder found, convert the resolved data back to Nodes
            // and replace the original node with the resolved nodes so that SnakeYAML can perform the merge as usual
            for (var tuple : originalTuples) {
                if (Tag.MERGE.equals(tuple.getKeyNode().getTag())) {
                    logger.debug("Processing merge key in flattenMapping: {}", tuple);
                    Node valueNode = tuple.getValueNode();
                    if (valueNode instanceof MappingNode) {
                        // Merge Key with a single mapping
                        // Handle the case for `<<: !include {file: other.yaml}`
                        if (constructObject(valueNode) instanceof IncludePlaceholder include) {
                            logger.debug("Resolving !include in flattenMapping: {}", include);
                            Node includedNode = resolveIncludePlaceholderAsNode(valueNode, include);
                            NodeTuple resolvedTuple = new NodeTuple(tuple.getKeyNode(), includedNode);
                            tuple = resolvedTuple;
                        }
                    } else if (valueNode instanceof SequenceNode seqNode) {
                        // Merge Key with a sequence of mappings
                        // Handle the case for `<<: [ !include {file: one.yaml}, !include {file: two.yaml} ]`
                        ListIterator<Node> iter = seqNode.getValue().listIterator();
                        while (iter.hasNext()) {
                            Node subNode = iter.next();
                            if (constructObject(subNode) instanceof IncludePlaceholder include) {
                                logger.debug("Resolving !include in flattenMapping list: {}", include);
                                Node includedNode = resolveIncludePlaceholderAsNode(subNode, include);
                                // replace the original list item with the included node
                                iter.set(includedNode);
                            }
                        }
                    }
                }
                // Add the (possibly modified) tuple back to the mapping node
                node.getValue().add(tuple);
            }
        }
        super.flattenMapping(node);
    }

    private Node resolveIncludePlaceholderAsNode(Node nodeContext, IncludePlaceholder includePlaceholder) {
        Object includedData = preprocessor.resolveIncludePlaceholder(includePlaceholder);
        if (includedData instanceof Map<?, ?>) {
            Node includedNode = representer.represent(includedData);
            return includedNode;
        }
        throw new YAMLException(getContext(nodeContext) + " !include did not return a map or list: " + includedData);
    }

    private boolean resolveSubstitution(Tag tag, boolean parent) {
        if (NOSUB_TAG.equals(tag)) {
            return false;
        }

        if (isSubTag(tag)) {
            return true;
        }
        return parent;
    }

    private boolean isSubTag(Tag tag) {
        if (tag.getValue() instanceof String tagValue && tagValue.startsWith(SUB_TAG)) {
            return true;
        }
        return false;
    }

    private String getContext(@Nullable Node node) {
        return VariableInterpolationHelper.formatContext(currentFile, formatLocation(node));
    }

    private String formatLocation(@Nullable Node node) {
        if (node == null) {
            return "";
        }
        Mark startMark = node.getStartMark();
        Mark endMark = node.getEndMark();
        if (startMark != null && endMark != null) {
            return formatMark(startMark) + " to " + formatMark(endMark);
        } else if (startMark != null) {
            return formatMark(startMark);
        }
        return "";
    }

    private String formatMark(Mark mark) {
        // SnakeYAML marks are 0-based; present as 1-based line/column for readability.
        return "[%d:%d]".formatted(mark.getLine() + 1, mark.getColumn() + 1);
    }

    public class ConstructInterpolation extends AbstractConstruct {
        @Override
        public Object construct(@Nullable Node node) {
            if (node instanceof ScalarNode scalarNode) {
                String value = (String) constructScalar(scalarNode);
                boolean enabled = substitutionStack.peek();
                if (enabled || isSubTag(scalarNode.getTag())) {
                    boolean isPlainScalar = scalarNode.getScalarStyle() == DumperOptions.ScalarStyle.PLAIN;
                    if (finalPass) {
                        Pattern pattern = substitutionPatternStack.peek();
                        String contextDescription = getContext(node);
                        try {
                            return VariableInterpolationHelper.evaluateValue(value, pattern, isPlainScalar, variables,
                                    contextDescription, true);
                        } catch (IllegalArgumentException e) {
                            // Re-wrap as YAMLException for consistency with YAML error handling
                            throw new YAMLException(e.getMessage(), e);
                        }
                    } else {
                        // Defer substitution until after variables are progressively populated
                        // so we can do variable chaining
                        return new SubstitutionPlaceholder(value, substitutionPatternStack.peek(), isPlainScalar);
                    }
                }

                return value;
            }
            throw new YAMLException(getContext(node) + " Expected a ScalarNode but got: " + node);
        }
    }

    public class ConstructDefault extends AbstractConstruct {
        @Override
        public Object construct(@Nullable Node node) {
            if (node == null) {
                return "";
            }

            return switch (node) {
                case MappingNode map -> constructMapping(map);
                case SequenceNode seq -> constructSequence(seq);
                case ScalarNode scalar -> Objects.requireNonNull(yamlConstructors.get(Tag.STR)).construct(scalar);
                default -> constructObject(node);
            };
        }
    }

    /**
     * The syntax of the tag is: !sub[:pattern=begin..end]
     * where `begin` and `end` are symbols that consist of one or more characters
     * that denote the start and end of the substitution pattern.
     *
     * The tag is URLDecoded before use.
     *
     * In the absence of the pattern parameter, the default ${..} is used.
     *
     * Examples:
     * - !sub ${variable}
     * - !sub:pattern=$[..] $[..]
     * - !sub:pattern=@(..) @(..)
     * - !sub:pattern=%23%7B%7B..%7D%7D #{{..}} # The characters are URLEncoded because # and {} are not allowed in tags
     */
    public class ConstructSub extends ConstructDefault {
        @Override
        public Object construct(@Nullable Node node) {
            if (node == null) {
                return "";
            }
            Pattern pattern = Objects.requireNonNullElseGet(extractPattern(node.getTag()),
                    () -> VariableInterpolationHelper.compileSubstitutionPattern(DEFAULT_BEGIN, DEFAULT_END));

            substitutionPatternStack.pop();
            substitutionPatternStack.push(pattern);
            return super.construct(node);
        }

        private @Nullable Pattern extractPattern(Tag tag) {
            String raw = tag.getValue();
            String decoded = URLDecoder.decode(raw, StandardCharsets.UTF_8);

            if (!decoded.startsWith(SUB_TAG)) {
                return null;
            }

            String suffix = decoded.substring(SUB_TAG.length());
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

            return VariableInterpolationHelper.compileSubstitutionPattern(begin, end);
        }
    }

    // Return an empty string for null values so that the keys are not removed from the map
    // This matches the behavior of Jackson's parser, otherwise some tests will fail
    private static class ConstructNull extends AbstractConstruct {
        @Override
        public Object construct(@Nullable Node node) {
            return "";
        }
    }

    // Used during non-final pass to avoid processing special tags and just return an empty string
    private static class ConstructEmpty extends AbstractConstruct {
        @Override
        public Object construct(@Nullable Node node) {
            return "";
        }
    }

    private class ConstructInclude extends AbstractConstruct {
        @Override
        public Object construct(@Nullable Node node) {
            logger.debug("Constructing !include node: {}", node);
            if (node instanceof ScalarNode scalarNode) {
                // ScalarNode's constructor ensured that its value is not null
                // so scalarNode.getValue() and therefore constructScalar cannot be null here
                String value = constructScalar(scalarNode).trim();
                return new IncludePlaceholder(value, Map.of());
            } else if (node instanceof MappingNode mappingNode) {
                Map<Object, Object> includeOptions = constructMapping(mappingNode);

                String fileName = (String) includeOptions.get("file");
                if (fileName == null) {
                    throw new YAMLException(getContext(node) + " missing 'file' key in !include: " + includeOptions);
                }

                Object varsObj = includeOptions.get("vars");
                if (varsObj == null) {
                    return new IncludePlaceholder(fileName, Map.of());
                } else if (varsObj instanceof Map<?, ?> varsMap) {
                    Map<String, Object> vars = varsMap.entrySet().stream()
                            .collect(Collectors.toMap(e -> String.valueOf(e.getKey()), Map.Entry::getValue));
                    return new IncludePlaceholder(fileName, vars);
                } else {
                    throw new YAMLException(getContext(node) + " invalid 'vars' type in !include file: " + fileName
                            + ", vars: " + varsObj + " (not a map)");
                }
            }
            throw new YAMLException(getContext(node) + " invalid !include argument type: "
                    + (node == null ? null : node.getNodeId().name()));
        }
    }

    private class ConstructReplace extends AbstractConstruct {
        @Override
        public Object construct(@Nullable Node node) {
            if (node instanceof MappingNode mappingNode) {
                Map<Object, Object> map = constructMapping(mappingNode);
                logger.debug("Constructing !replace map node: {}", map);
                return new ReplacePlaceholder(map);
            } else if (node instanceof SequenceNode sequenceNode) {
                Object list = constructSequence(sequenceNode);
                logger.debug("Constructing !replace sequence node: {}", list);
                return new ReplacePlaceholder(list);
            }
            throw new YAMLException(getContext(node) + " invalid !replace argument. Expected a map or a list.");
        }
    }

    private class ConstructRemove extends AbstractConstruct {
        @Override
        public Object construct(@Nullable Node node) {
            logger.debug("Constructing !remove node: {}", node);
            return new RemovePlaceholder();
        }
    }
}
