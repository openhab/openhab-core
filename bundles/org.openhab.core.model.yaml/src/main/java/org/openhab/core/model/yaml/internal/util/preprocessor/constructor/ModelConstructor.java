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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.model.yaml.internal.util.preprocessor.StringInterpolator;
import org.openhab.core.model.yaml.internal.util.preprocessor.core.MergeKeyProcessor;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholder.IfPlaceholder;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholder.IncludePlaceholder;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholder.InsertPlaceholder;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholder.MergeKeyPlaceholder;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholder.RemovePlaceholder;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholder.ReplacePlaceholder;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholder.SubstitutionPlaceholder;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;

/**
 * Extends SnakeYAML's {@link Constructor} to add support for the
 * preprocessor's custom YAML tags and model‑transformation features.
 * <p>
 * The {@code ModelConstructor} handles all extended tags, including:
 * <ul>
 * <li><code>!sub</code> — variable interpolation</li>
 * <li><code>!nosub</code> — disable interpolation for a value</li>
 * <li><code>!if</code> — conditional evaluation</li>
 * <li><code>!include</code> — include external YAML files</li>
 * <li><code>!insert</code> — insert templates with variable context</li>
 * <li><code>!replace</code> — replace parts of the model within a package</li>
 * <li><code>!remove</code> — remove parts of the model within a package</li>
 * </ul>
 * These extensions allow the preprocessor to construct a fully evaluated
 * in‑memory model before further processing or consumption.
 *
 * @author Jimmy Tanagra — Initial contribution
 */
@NonNullByDefault
public class ModelConstructor extends Constructor {
    static final String SUB_TAG = "!sub";

    private static final Tag NOSUB_TAG = new Tag("!nosub");
    private static final Tag IF_TAG = new Tag("!if");
    private static final Tag INCLUDE_TAG = new Tag("!include");
    private static final Tag REPLACE_TAG = new Tag("!replace");
    private static final Tag REMOVE_TAG = new Tag("!remove");
    private static final Tag INSERT_TAG = new Tag("!insert");

    // An intermediary tag to replace Tag.Merge during flattening to prevent SnakeYAML
    // from processing merge keys before we can handle them in our MergeKeyProcessor
    private static final Tag DEFERRED_MERGE_TAG = new Tag("!deferred-merge");

    String sourcePath;
    final Deque<Boolean> substitutionStack = new ArrayDeque<>();
    final Deque<Pattern> substitutionPatternStack = new ArrayDeque<>();

    public ModelConstructor(LoaderOptions options, String sourcePath) {
        super(options);

        this.sourcePath = sourcePath;
        this.substitutionStack.push(false);
        this.substitutionPatternStack.push(StringInterpolator.DEFAULT_SUBSTITUTION_PATTERN);

        this.yamlMultiConstructors.put(SUB_TAG, new ConstructSub(this));
        this.yamlConstructors.put(NOSUB_TAG, new ConstructNoSub(this));

        this.yamlConstructors.put(Tag.STR, new ConstructStr(this));

        this.yamlConstructors.put(DEFERRED_MERGE_TAG,
                new ConstructInterpolablePlaceholder<MergeKeyPlaceholder>(this, MergeKeyPlaceholder::new));
        this.yamlConstructors.put(IF_TAG,
                new ConstructInterpolablePlaceholder<IfPlaceholder>(this, IfPlaceholder::new));
        this.yamlConstructors.put(INCLUDE_TAG,
                new ConstructInterpolablePlaceholder<IncludePlaceholder>(this, IncludePlaceholder::new));
        this.yamlConstructors.put(INSERT_TAG,
                new ConstructInterpolablePlaceholder<InsertPlaceholder>(this, InsertPlaceholder::new));
        this.yamlConstructors.put(REPLACE_TAG,
                new ConstructInterpolablePlaceholder<ReplacePlaceholder>(this, ReplacePlaceholder::new));
        this.yamlConstructors.put(REMOVE_TAG,
                new ConstructInterpolablePlaceholder<RemovePlaceholder>(this, RemovePlaceholder::new));
    }

    /**
     * Gets a string representation of the node's location for logging purposes.
     *
     * @param node the YAML node to get the location of
     * @return a string describing the source location of the node, including file path and line/column if available
     */
    String getLocation(Node node) {
        String location = "";
        Mark startMark = node.getStartMark();
        if (startMark != null) {
            location = ":%d:%d".formatted(startMark.getLine() + 1, startMark.getColumn() + 1);
        }
        return this.sourcePath + location;
    }

    /**
     * Default construction method that routes to the appropriate construct method
     * based on the node type.
     *
     * Use this instead of constructObject() to avoid an infinite recursion when
     * constructing a node on a custom tag.
     *
     * @param node the node to construct
     * @return the constructed object
     */
    protected @Nullable Object constructByType(Node node) {
        return switch (node) {
            case MappingNode map -> constructMapping(map);
            case SequenceNode seq -> constructSequence(seq);
            case ScalarNode scalar -> constructScalarOrSubstitution(scalar);

            // A fallback, should not happen. SnakeYAML will catch the infinite recursion and report it.
            default -> constructObject(node);
        };
    }

    /**
     * Construct a scalar node, potentially as a SubstitutionPlaceholder
     * if the current substitution state is enabled.
     *
     * @param node
     * @return
     */
    @SuppressWarnings("null") // The stacks and SnakeYAML methods shouldn't return null
    protected @Nullable Object constructScalarOrSubstitution(Node node) {
        ScalarNode scalarNode = (ScalarNode) node;
        Tag tag = scalarNode.getTag();
        String value = constructScalar(scalarNode);
        boolean enabled = substitutionStack.peek();
        if (enabled || isSubstitutionTag(tag)) {
            Pattern pattern = substitutionPatternStack.peek();
            String location = getLocation(scalarNode);
            return new SubstitutionPlaceholder(value, pattern, location);
        }
        return value;
    }

    /**
     * Intercept constructObject to keep track of the current substitution state.
     *
     * @param node the node to construct
     * @return the constructed object
     */
    @Override
    @NonNullByDefault({})
    protected @Nullable Object constructObject(Node node) {
        boolean parent = Objects.requireNonNull(substitutionStack.peek());
        boolean enabled = resolveSubstitution(Objects.requireNonNull(node.getTag()), parent);
        substitutionStack.push(enabled);
        substitutionPatternStack.push(Objects.requireNonNull(substitutionPatternStack.peek()));
        try {
            return super.constructObject(node);
        } finally {
            substitutionPatternStack.pop();
            substitutionStack.pop();
        }
    }

    private static boolean resolveSubstitution(Tag tag, boolean parent) {
        if (NOSUB_TAG.equals(tag)) {
            return false;
        }

        if (isSubstitutionTag(tag)) {
            return true;
        }
        return parent;
    }

    static boolean isSubstitutionTag(Tag tag) {
        return tag.getValue().startsWith(SUB_TAG);
    }

    /**
     * Intercept flattenMapping to convert merge keys (<<)
     * to a special tag to prevent SnakeYAML from processing them as a merge key.
     *
     * This is done because the merge key values may contain custom tags that
     * need to be resolved before we can perform the merge.
     *
     * Our {@link MergeKeyProcessor} will later perform the merge logic.
     */
    @Override
    @NonNullByDefault({})
    protected void flattenMapping(MappingNode node) throws YAMLException {
        if (node.isMerged()) {
            node.getValue().forEach(tuple -> {
                Node keyNode = tuple.getKeyNode();
                if (Tag.MERGE.equals(keyNode.getTag())) {
                    keyNode.setTag(DEFERRED_MERGE_TAG);
                }
            });
        }
        super.flattenMapping(node);
    }

    protected void trackPattern(Pattern pattern) {
        substitutionPatternStack.pop();
        substitutionPatternStack.push(pattern);
    }
}
