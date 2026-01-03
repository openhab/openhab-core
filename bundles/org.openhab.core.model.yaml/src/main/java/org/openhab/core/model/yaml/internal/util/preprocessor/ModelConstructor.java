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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
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
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;

import com.hubspot.jinjava.interpret.InterpretException;

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

    private static final String DEFAULT_BEGIN = "${";
    private static final String DEFAULT_END = "}";

    private final Map<String, Object> variables;
    private final Path currentFile;

    private final Deque<Boolean> substitutionStack = new ArrayDeque<>();
    private final Deque<Pattern> substitutionPatternStack = new ArrayDeque<>();

    private final Logger logger = LoggerFactory.getLogger(ModelConstructor.class);

    public ModelConstructor(LoaderOptions options, Map<String, Object> variables, Path currentFile, boolean finalPass) {
        super(options);

        this.variables = variables;
        this.currentFile = currentFile;
        this.substitutionStack.push(false);
        this.substitutionPatternStack.push(compileSubstitutionPattern(DEFAULT_BEGIN, DEFAULT_END));

        this.yamlConstructors.put(Tag.NULL, new ConstructNull());
        this.yamlConstructors.put(NOSUB_TAG, new ConstructDefault());

        // The first pass is only to gather variables, so we use a no-op constructor for efficiency
        this.yamlConstructors.put(INCLUDE_TAG, finalPass ? new ConstructInclude() : new ConstructEmpty());
        this.yamlConstructors.put(REPLACE_TAG, finalPass ? new ConstructReplace() : new ConstructEmpty());
        this.yamlConstructors.put(REMOVE_TAG, finalPass ? new ConstructRemove() : new ConstructEmpty());

        // We do need to resolve strings normally in the first pass - don't resolve to an empty string
        this.yamlConstructors.put(Tag.STR, finalPass ? new ConstructInterpolation() : new ConstructYamlStr());

        this.yamlMultiConstructors.put(SUB_TAG, new ConstructSub());

        logger.trace("ModelConstructor created with vars: {}", variables);
    }

    @Override
    protected Object constructObject(@Nullable Node node) {
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

    public class ConstructInterpolation extends AbstractConstruct {
        @Override
        public Object construct(@Nullable Node node) {
            if (!(node instanceof ScalarNode scalarNode)) {
                String location = node == null ? "" : formatLocation(node);
                throw new YAMLException(
                        "%s:%s Expected a ScalarNode but got: %s".formatted(currentFile, location, node));
            }

            String value = (String) constructScalar(scalarNode);
            boolean enabled = substitutionStack.peek();
            if (enabled || isSubTag(scalarNode.getTag())) {
                return performSubstitution(value, scalarNode);
            }

            return value;
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
                case ScalarNode scalar -> yamlConstructors.get(Tag.STR).construct(scalar);
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
                    () -> compileSubstitutionPattern(DEFAULT_BEGIN, DEFAULT_END));

            substitutionPatternStack.pop();
            substitutionPatternStack.push(pattern);
            return super.construct(node);
        }
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

        return compileSubstitutionPattern(begin, end);
    }

    private Object performSubstitution(String value, ScalarNode scalarNode) {
        Pattern pattern = substitutionPatternStack.peek();
        Matcher matcher = pattern.matcher(value);
        if (!matcher.find()) {
            logger.debug("No variable pattern match for '{}'", value);
            return value;
        }

        // If the whole value is exactly one ${...}, evaluate via Jinjava and return the native object.
        // This preserves non-string types (maps, lists, numbers, booleans) when the value is a plain
        // placeholder.
        if (scalarNode.getScalarStyle() == DumperOptions.ScalarStyle.PLAIN && matcher.matches()) {
            String content = matcher.group("content");
            return evaluateExpression(content, variables, scalarNode);
        }

        // Replace ${...} with content (simple variables directly, complex expressions via Jinja)
        String interpolated = matcher.replaceAll(match -> {
            String content = match.group("content");
            return evaluateExpression(content, variables, scalarNode).toString();
        });

        // Return as String
        return interpolated;
    }

    private Object evaluateExpression(String expression, Map<String, Object> variables, ScalarNode scalarNode) {
        try {
            Object rendered = JinjavaTemplateEngine.renderObject(expression, variables);
            return Objects.requireNonNullElse(rendered, "");
        } catch (InterpretException e) {
            // Format this as `path:[line,col] <rest of msg>` so it's clickable in vscode
            throw new YAMLException("%s:%s %s".formatted(currentFile, formatLocation(scalarNode), e.getMessage()), e);
        }
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

    private Pattern compileSubstitutionPattern(String begin, String end) {
        if (DEFAULT_BEGIN.equals(begin) && DEFAULT_END.equals(end)) {
            return SUBSTITUTION_PATTERN;
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
                return new IncludeObject(value, Map.of());
            } else if (node instanceof MappingNode mappingNode) {
                @SuppressWarnings("unchecked")
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
                    + (node == null ? null : node.getNodeId().name()));
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
