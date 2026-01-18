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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholders.IncludePlaceholder;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholders.InsertPlaceholder;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholders.RemovePlaceholder;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholders.ReplacePlaceholder;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholders.SubstitutionPlaceholder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.Construct;
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

    private static final Tag INSERT_TAG = new Tag("!insert");

    private static final String DEFAULT_BEGIN = "${";
    private static final String DEFAULT_END = "}";

    private final boolean finalPass;
    private final Map<String, Object> variables;
    private final YamlPreprocessor preprocessor;
    private final Construct interpolationConstruct;

    private final Representer representer = new Representer(new DumperOptions());

    // We always have at least one element in the stack, so peek() is safe.
    private final Deque<Boolean> substitutionStack = new ArrayDeque<>();
    private final Deque<Pattern> substitutionPatternStack = new ArrayDeque<>();

    private boolean inTemplatesSection = false;
    // Stack to track the current mapping key path
    private final Deque<@Nullable String> mappingKeyPath = new ArrayDeque<>();

    private final Logger logger = LoggerFactory.getLogger(ModelConstructor.class);

    public ModelConstructor(LoaderOptions options, Map<String, Object> variables, YamlPreprocessor preprocessor,
            boolean finalPass) {
        super(options);

        this.finalPass = finalPass;
        this.variables = variables;
        this.preprocessor = preprocessor;
        this.substitutionStack.push(false);
        this.substitutionPatternStack
                .push(VariableInterpolationHelper.compileSubstitutionPattern(DEFAULT_BEGIN, DEFAULT_END));

        this.yamlMultiConstructors.put(SUB_TAG, new ConstructSub());

        this.yamlConstructors.put(Tag.NULL, new ConstructNull());
        this.yamlConstructors.put(NOSUB_TAG, new ConstructDefault());

        this.interpolationConstruct = new ConstructInterpolation();
        this.yamlConstructors.put(Tag.STR, interpolationConstruct);
        this.yamlConstructors.put(INCLUDE_TAG, new ConstructInclude());
        this.yamlConstructors.put(INSERT_TAG, new ConstructInsert());

        this.yamlConstructors.put(REPLACE_TAG, new ConstructReplace());
        this.yamlConstructors.put(REMOVE_TAG, new ConstructRemove());

        logger.trace("ModelConstructor created with vars: {}", variables);
    }

    @Override
    @NonNullByDefault({})
    protected Object constructObject(Node node) {
        boolean parent = Objects.requireNonNull(substitutionStack.peek());
        boolean enabled = resolveSubstitution(node.getTag(), parent);
        substitutionStack.push(enabled);
        substitutionPatternStack.push(Objects.requireNonNull(substitutionPatternStack.peek()));
        try {
            return super.constructObject(node);
        } finally {
            substitutionPatternStack.pop();
            substitutionStack.pop();
        }
    }

    /**
     * The whole purpose of overriding this method is to keep track of whether we're
     * in the templates section or not, so that we can skip substitution there during final pass.
     *
     * @param node
     * @param mapping
     */
    @Override
    @NonNullByDefault({})
    protected void constructMapping2ndStep(MappingNode node, Map<Object, Object> mapping) {
        flattenMapping(node);

        // Determine the parent key for this mapping
        String parentKey = mappingKeyPath.peek();
        boolean enteredTemplatesSection = false;
        if (mappingKeyPath.size() == 1 && YamlPreprocessor.TEMPLATES_KEY.equals(parentKey)) {
            inTemplatesSection = true;
            enteredTemplatesSection = true;
        }

        for (NodeTuple tuple : node.getValue()) {
            String key = null;
            Node keyNode = tuple.getKeyNode();
            if (keyNode instanceof ScalarNode scalar) {
                key = scalar.getValue();
            }
            Object keyObj = constructObject(keyNode);
            mappingKeyPath.push(key);
            Object value = constructObject(tuple.getValueNode());
            mappingKeyPath.pop();
            mapping.put(keyObj, value);
        }

        if (enteredTemplatesSection) {
            inTemplatesSection = false;
        }
    }

    /**
     * Intercept flattenMapping to handle merge keys (<<:) with
     * IncludePlaceholder and TemplatePlaceholder resolutions.
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
        if (node.isMerged()) {
            List<NodeTuple> originalTuples = new ArrayList<>(node.getValue());
            node.getValue().clear();

            // Now process merge keys (includes)
            // find all merge keys (<<), resolve any IncludePlaceholder found, convert the resolved data back to Nodes
            // and replace the original node with the resolved nodes so that SnakeYAML can perform the merge as usual
            for (var tuple : originalTuples) {
                if (Tag.MERGE.equals(tuple.getKeyNode().getTag())) {
                    logger.debug("Processing merge key in flattenMapping: {}", tuple);
                    Node valueNode = tuple.getValueNode();
                    if (valueNode instanceof SequenceNode seqNode) {
                        // Merge Key with a sequence of mappings
                        // Handle the case for `<<: [ !include {file: xxx.yaml} | !sub <scalar>, ... ]`
                        ListIterator<Node> iter = seqNode.getValue().listIterator();
                        while (iter.hasNext()) {
                            Node sequenceItem = iter.next();
                            if (resolveIncludeNode(sequenceItem) instanceof Node includedNode) {
                                iter.set(includedNode);
                            } else if (resolveInsertNode(sequenceItem) instanceof Node insertedNode) {
                                iter.set(insertedNode);
                            } else if (resolveScalarSubNode(sequenceItem) instanceof Node subNode) {
                                iter.set(subNode);
                            }
                        }
                    } else if (resolveIncludeNode(valueNode) instanceof Node includedNode) {
                        // Merge Key with a single mapping
                        // Handle the case for `<<: !include {file: other.yaml}`
                        tuple = new NodeTuple(tuple.getKeyNode(), includedNode);
                    } else if (resolveInsertNode(valueNode) instanceof Node insertedNode) {
                        // Merge Key with a single mapping
                        // Handle the case for `<<: !insert template_name`
                        tuple = new NodeTuple(tuple.getKeyNode(), insertedNode);
                    } else if (resolveScalarSubNode(valueNode) instanceof Node subNode) {
                        // Merge Key with a single mapping with substitution
                        // Handle the case for `<<: !sub ...`
                        tuple = new NodeTuple(tuple.getKeyNode(), subNode);
                    }
                }
                // Add the (possibly modified) tuple back to the mapping node
                node.getValue().add(tuple);
            }
        }
        super.flattenMapping(node);
    }

    /**
     * Resolve an !include node if present for merge key processing.
     *
     * In non-final pass, return an empty mapping because we haven't got the full variable context.
     * Otherwise "!include filename" will be constructed as a scalar which is invalid for merge keys.
     *
     * @param node
     * @return the resolved Node, or null if the node is not an !include node
     */
    private @Nullable Node resolveIncludeNode(Node node) {
        if (!INCLUDE_TAG.equals(node.getTag())) {
            return null;
        }
        logger.debug("Resolving !include node: {}", node);
        if (finalPass && constructObject(node) instanceof IncludePlaceholder includePlaceholder) {
            Object includedData = preprocessor.resolveIncludePlaceholder(includePlaceholder);
            if (!(includedData instanceof Map<?, ?>)) {
                throw new YAMLException(getContext(node) + " Included content must be a mapping for merge key. Found: "
                        + includedData.getClass().getName());
            }
            Node result = representer.represent(includedData);
            // Prevent substitution of the included content in the current context by
            // tagging the entire subtree with NOSUB_TAG so ConstructInterpolation
            // sees the !nosub tag on nested nodes as well.
            markNoSub(result);
            return result;
        }
        return representer.represent(Map.of());
    }

    /**
     * Resolve an !insert node if present for merge key processing.
     *
     * @param node
     * @return
     */
    private @Nullable Node resolveInsertNode(Node node) {
        if (!INSERT_TAG.equals(node.getTag())) {
            return null;
        }
        logger.debug("Resolving !insert node: {}", node);
        if (finalPass && constructObject(node) instanceof InsertPlaceholder insertPlaceholder) {
            Object includedData = preprocessor.resolveInsertPlaceholder(insertPlaceholder);
            if (!(includedData instanceof Map<?, ?>)) {
                throw new YAMLException(getContext(node) + " Inserted content must be a mapping for merge key. Found: "
                        + includedData.getClass().getName());
            }
            Node result = representer.represent(includedData);
            // Prevent substitution of the included content in the current context by
            // tagging the entire subtree with NOSUB_TAG so ConstructInterpolation
            // sees the !nosub tag on nested nodes as well.
            markNoSub(result);
            return result;
        }
        return representer.represent(Map.of());
    }

    /**
     * Resolve a !sub scalar node if present for merge key processing.
     *
     * @param node
     * @return the interpolated Node, or null if the node is not a !sub scalar node
     */
    private @Nullable Node resolveScalarSubNode(Node node) {
        if (!(node instanceof ScalarNode && isSubTag(node.getTag()))) {
            return null;
        }
        logger.debug("Resolving a scalar !sub node: {}", node);
        if (finalPass) {
            Object interpolated = interpolationConstruct.construct(node);
            if (!(interpolated instanceof Map<?, ?>)) {
                throw new YAMLException(
                        getContext(node) + " Substituted content must be a mapping for merge key. Found: "
                                + (interpolated == null ? "null" : interpolated.getClass().getName()));
            }
            Node result = representer.represent(interpolated);
            // Prevent further substitution of the substituted content in the current context by
            // tagging the entire subtree with NOSUB_TAG so ConstructInterpolation
            // sees the !nosub tag on nested nodes as well.
            markNoSub(result);
            return result;
        }
        return representer.represent(Map.of());
    }

    /**
     * Recursively mark the node and its children with NOSUB_TAG to prevent substitution.
     *
     * @param node
     */
    private void markNoSub(Node node) {
        node.setTag(NOSUB_TAG);
        if (node instanceof MappingNode mapping) {
            for (NodeTuple t : mapping.getValue()) {
                markNoSub(t.getKeyNode());
                markNoSub(t.getValueNode());
            }
        } else if (node instanceof SequenceNode seq) {
            for (Node n : seq.getValue()) {
                markNoSub(n);
            }
        }
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
        if (tag.getValue().startsWith(SUB_TAG)) {
            return true;
        }
        return false;
    }

    private String getContext(Node node) {
        return VariableInterpolationHelper.formatContext(preprocessor.getPath(), formatLocation(node));
    }

    private String formatLocation(Node node) {
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
        @NonNullByDefault({})
        public Object construct(Node node) {
            if (node instanceof ScalarNode scalarNode) {
                String value = (String) constructScalar(scalarNode);
                boolean enabled = Objects.requireNonNull(substitutionStack.peek());
                if (enabled || isSubTag(scalarNode.getTag())) {
                    String contextDescription = getContext(node);
                    // We don't want to resolve substitutions in the templates section during final pass.
                    // Template substitutions are resolved at insertion time to include insertion context.
                    if (finalPass && !inTemplatesSection) {
                        Pattern pattern = Objects.requireNonNull(substitutionPatternStack.peek());
                        try {
                            return VariableInterpolationHelper.evaluateValue(value, pattern, scalarNode.isPlain(),
                                    variables, true);
                        } catch (IllegalArgumentException e) {
                            // Re-wrap as YAMLException for consistency with YAML error handling
                            throw new YAMLException(contextDescription + ": " + e.getMessage(), e);
                        }
                    } else {
                        // Defer substitution until after variables are progressively populated
                        // so we can do variable chaining
                        Pattern pattern = Objects.requireNonNull(substitutionPatternStack.peek());
                        return new SubstitutionPlaceholder(value, pattern, scalarNode.isPlain(), contextDescription);
                    }
                }

                return value;
            }
            throw new YAMLException(getContext(node) + " Expected a ScalarNode but got: " + node);
        }
    }

    public class ConstructDefault extends AbstractConstruct {
        @Override
        @NonNullByDefault({})
        public Object construct(Node node) {
            return switch (node) {
                case MappingNode map -> constructMapping(map);
                case SequenceNode seq -> constructSequence(seq);
                case ScalarNode scalar -> interpolationConstruct.construct(scalar);
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
        @NonNullByDefault({})
        public Object construct(Node node) {
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

    /**
     * Return an empty string for null values so that the keys are not removed from the map
     * This matches the behavior of Jackson's parser, otherwise some tests will fail
     */
    private class ConstructNull extends AbstractConstruct {
        @Override
        public Object construct(@Nullable Node node) {
            if (node != null) {
                constructScalar((ScalarNode) node);
            }
            return "";
        }
    }

    /**
     * Constructs an IncludePlaceholder from an !include node.
     *
     * This is done in both passes.
     *
     * In the first pass the IncludePlaceholder may be used to load variables from include files.
     * During the first pass, the file name and vars may contain SubstitutionPlaceholder objects.
     * They will be resolved in the YamlPreprocessor extractVariables method incrementally so that
     * they can refer to variables defined before them.
     *
     * Only !include tags found in variable definitions will be processed in the first pass.
     * They won't be processed again in the final pass because extractVariables is only done in the first pass.
     *
     * Only !include tags found in the model content will be processed in the final pass.
     */
    private class ConstructInclude extends AbstractConstruct {
        @Override
        @NonNullByDefault({})
        public Object construct(Node node) {
            logger.debug("Constructing !include node: {}", node);
            Object fileName;
            Map<Object, Object> localVars = Map.of();

            if (node instanceof ScalarNode scalarNode) {
                // Accept any scalar, including unresolved variable expressions
                fileName = interpolationConstruct.construct(scalarNode);
            } else if (node instanceof MappingNode mappingNode) {
                @SuppressWarnings("null")
                Map<String, Object> includeOptions = extractOptions(mappingNode, INCLUDE_TAG.getValue());
                fileName = includeOptions.get("file");

                @SuppressWarnings("unchecked")
                Map<Object, Object> varsMap = (Map<Object, Object>) includeOptions.get("vars");
                localVars = varsMap != null ? varsMap : Map.of();
            } else {
                throw new YAMLException(getContext(node) + " invalid !include argument type: "
                        + (node == null ? null : node.getNodeId().name()));
            }

            if (fileName == null || (fileName instanceof String str && str.isBlank())) {
                throw new YAMLException(getContext(node) + " !include missing or empty 'file' option.");
            }

            // Check the fileName validity after variable substitution in the resolution process
            return new IncludePlaceholder(fileName, localVars, getContext(node));
        }
    }

    /**
     * Handles !insert tag: returns an InsertPlaceholder to be processed later.
     *
     * Syntax:
     * <ul>
     * <li>!insert template_name
     * <li>!insert { template: template_name, vars: { ... } }
     * </ul>
     */
    private class ConstructInsert extends AbstractConstruct {
        @Override
        @NonNullByDefault({})
        public Object construct(Node node) {
            logger.debug("Constructing !insert node: {}", node);
            Object templateName = null;
            Map<Object, Object> localVars = Map.of();

            if (node instanceof ScalarNode scalarNode) {
                // Accept any scalar, including unresolved variable expressions
                templateName = interpolationConstruct.construct(scalarNode);
            } else if (node instanceof MappingNode mappingNode) {
                @SuppressWarnings("null")
                Map<String, Object> insertOptions = extractOptions(mappingNode, INSERT_TAG.getValue());
                templateName = insertOptions.get("template");

                @SuppressWarnings("unchecked")
                Map<Object, Object> varsMap = (Map<Object, Object>) insertOptions.get("vars");
                localVars = varsMap != null ? varsMap : Map.of();
            } else {
                throw new YAMLException(getContext(node) + " invalid !insert argument type: "
                        + (node == null ? null : node.getNodeId().name()));
            }
            if (templateName == null || (templateName instanceof String str && str.isBlank())) {
                throw new YAMLException(getContext(node) + " !insert missing or empty 'template' option.");
            }

            return new InsertPlaceholder(templateName, localVars, getContext(node));
        }
    }

    /**
     * A helper method to extract options from a MappingNode for !include and !insert tags.
     *
     * This ensures that the keys are scalars and do not use !sub tag for substitution.
     * The values (options) are constructed as normal objects, which may include SubstitutionPlaceholder objects.
     *
     * It also ensures that the 'vars' option is always present as a Map (empty if not provided).
     *
     * @param mappingNode the node to extract options from
     * @param tagName the tag name for error messages
     * @return the options map
     */
    private Map<String, Object> extractOptions(MappingNode mappingNode, String tagName) {
        Map<String, Object> options = new HashMap<>();
        for (NodeTuple t : mappingNode.getValue()) {
            if (!(t.getKeyNode() instanceof ScalarNode keyNode)) {
                throw new YAMLException("%s %s option keys must be scalars. Found: %s"
                        .formatted(getContext(mappingNode), tagName, t.getKeyNode().getNodeId().name()));
            } else if (isSubTag(keyNode.getTag())) {
                throw new YAMLException("%s %s option keys cannot use !sub tag for substitution."
                        .formatted(getContext(keyNode), tagName));
            }
            String key = String.valueOf(constructScalar(keyNode));
            Object value = constructObject(t.getValueNode());
            options.put(key, value);
        }

        Object varsObj = options.get("vars");
        if (varsObj != null && !(varsObj instanceof Map<?, ?>)) {
            throw new YAMLException("%s %s vars expected a map but found: %s".formatted(getContext(mappingNode),
                    tagName, varsObj.getClass().getName()));
        }

        return options;
    }

    private class ConstructReplace extends AbstractConstruct {
        @Override
        @NonNullByDefault({})
        public Object construct(Node node) {
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
