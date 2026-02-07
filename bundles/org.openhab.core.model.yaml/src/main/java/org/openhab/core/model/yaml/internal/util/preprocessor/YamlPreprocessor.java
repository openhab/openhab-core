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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.model.yaml.internal.util.preprocessor.core.MergeKeyProcessor;
import org.openhab.core.model.yaml.internal.util.preprocessor.core.PackageProcessor;
import org.openhab.core.model.yaml.internal.util.preprocessor.core.RecursiveProcessor;
import org.openhab.core.model.yaml.internal.util.preprocessor.core.SourceLocator;
import org.openhab.core.model.yaml.internal.util.preprocessor.core.TemplateProcessor;
import org.openhab.core.model.yaml.internal.util.preprocessor.core.VariableProcessor;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholder.IfPlaceholder;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholder.IncludePlaceholder;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholder.InsertPlaceholder;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholder.RemovePlaceholder;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholder.ReplacePlaceholder;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholder.SubstitutionPlaceholder;
import org.openhab.core.model.yaml.internal.util.preprocessor.processor.IfProcessor;
import org.openhab.core.model.yaml.internal.util.preprocessor.processor.IncludeProcessor;
import org.openhab.core.model.yaml.internal.util.preprocessor.processor.InsertProcessor;
import org.openhab.core.model.yaml.internal.util.preprocessor.processor.RemoveProcessor;
import org.openhab.core.model.yaml.internal.util.preprocessor.processor.ReplaceProcessor;
import org.openhab.core.model.yaml.internal.util.preprocessor.processor.SubstitutionProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * The {@link YamlPreprocessor} is a utility class to load YAML files
 * and preprocess them before they are loaded by
 * {@link org.openhab.core.model.yaml.internal.YamlModelRepositoryImpl}.
 *
 * The following enhancements are made:
 *
 * <ul>
 * <li>Full support for anchors and aliases.
 * <li>Match Jackson's PARSE_BOOLEAN_LIKE_WORDS_AS_STRINGS: only "true" and
 * "false" (case insensitive) are booleans.
 * <li>Allow variable definitions and variable substitution/interpolation.
 * <li>Support <code>!include</code> tag for including other YAML files.
 * <li>Support combining elements using packages.
 * </ul>
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
public class YamlPreprocessor {
    private static final Logger RAW_LOGGER = LoggerFactory.getLogger(YamlPreprocessor.class);

    private final ConcurrentHashMap<Path, @Nullable CacheEntry> includeCache;

    public static record CacheEntry(byte[] bytes, long mtime) {
    }

    private byte[] yamlBytes = new byte[0];

    private final PreprocessorContext context;

    private final SubstitutionProcessor substitutionProcessor;
    private final IfProcessor ifProcessor;
    private final IncludeProcessor includeProcessor;
    private final InsertProcessor insertProcessor;
    private final RemoveProcessor removeProcessor;
    private final ReplaceProcessor replaceProcessor;

    private final RecursiveProcessor recursiveProcessor;

    private final MergeKeyProcessor mergeKeyProcessor;

    /**
     * Constructs a YamlPreprocessor for the given file path and context.
     *
     * @param path the file path for resolving relative includes
     * @param variables initial variable context
     * @param includeStack current include stack for circular reference detection
     * @param includeCallback callback invoked for each included file
     * @param logSession the log session for warning consolidation
     */
    public YamlPreprocessor(Path path, Map<String, @Nullable Object> variables, Set<Path> includeStack,
            Consumer<Path> includeCallback, LogSession logSession,
            ConcurrentHashMap<Path, @Nullable CacheEntry> includeCache) {
        Path absolutePath = Objects.requireNonNull(path.toAbsolutePath().normalize());
        Path relativePath = PreprocessorConfig.resolveRelativePath(absolutePath);
        BufferedLogger logger = new BufferedLogger(RAW_LOGGER, logSession);

        // Validate circular inclusion and depth before processing
        Set<Path> newIncludeStack = new LinkedHashSet<>(includeStack);
        if (!newIncludeStack.add(absolutePath)) {
            // using stream::toList() causes a null safety warning, whereas
            // collect(Collectors.toList()) does not
            String includeStackChain = String.join(" -> ",
                    newIncludeStack.stream().map(Path::toString).collect(Collectors.toList()));
            throw new YAMLException("Circular inclusion detected: " + includeStackChain + " -> " + absolutePath);
        }
        if (newIncludeStack.size() > PreprocessorConfig.MAX_INCLUDE_DEPTH) {
            throw new YAMLException("Maximum include depth (" + PreprocessorConfig.MAX_INCLUDE_DEPTH + ") exceeded");
        }

        this.includeCache = includeCache;

        this.context = new PreprocessorContext(absolutePath, relativePath, variables, newIncludeStack, includeCallback,
                logger);

        // mergeKeyProcessor isn't part of the recursive processor.
        // It does its own handling
        this.mergeKeyProcessor = new MergeKeyProcessor(context);

        this.recursiveProcessor = new RecursiveProcessor();

        // These processors are participants in recursive processing
        this.substitutionProcessor = new SubstitutionProcessor(context);
        this.ifProcessor = new IfProcessor(context);
        this.includeProcessor = new IncludeProcessor(context, includeCache);
        this.insertProcessor = new InsertProcessor(context, recursiveProcessor, substitutionProcessor);
        this.removeProcessor = new RemoveProcessor();
        this.replaceProcessor = new ReplaceProcessor();

        this.recursiveProcessor.register(SubstitutionPlaceholder.class, substitutionProcessor);
        this.recursiveProcessor.register(IfPlaceholder.class, ifProcessor);
        this.recursiveProcessor.register(IncludePlaceholder.class, includeProcessor);
        this.recursiveProcessor.register(InsertPlaceholder.class, insertProcessor);
        this.recursiveProcessor.register(RemovePlaceholder.class, removeProcessor);
        this.recursiveProcessor.register(ReplacePlaceholder.class, replaceProcessor);
    }

    /**
     * Loads a YAML file from the given {@link Path} and processes it through the
     * full preprocessing pipeline.
     * <p>
     * This is the main entry point for the YAML preprocessor. It reads the file,
     * parses the YAML, and applies all supported preprocessing features, including:
     * <ul>
     * <li>variable substitution</li>
     * <li>conditional evaluation</li>
     * <li>file includes</li>
     * <li>inline inserts</li>
     * <li>package orchestration</li>
     * <li>YAML anchors and aliases</li>
     * <li>YAML merge keys</li>
     * </ul>
     * The {@code includeCallback} is invoked for each file referenced via an
     * include directive, allowing the caller to track include usage so it can
     * refresh models when included files change.
     * <p>
     * The returned value is the fully evaluated Java object representation of the
     * YAML document after all the preprocessing steps have been applied.
     *
     * @param path the path to the YAML file to load and preprocess; also used as
     *            the base directory for resolving relative includes
     * @param includeCallback a callback invoked for each included file
     * @return the processed Java object representation of the YAML file
     * @throws IOException if the file cannot be read or if preprocessing fails
     */
    public static @Nullable Object load(Path path, Consumer<Path> includeCallback) throws IOException {
        // Create a LogSession autocloseable object. It consolidates warnings and duplicates.
        // Upon exit, any warnings will be logged.
        try (LogSession session = new LogSession()) {
            ConcurrentHashMap<Path, @Nullable CacheEntry> cache = new ConcurrentHashMap<>();
            return load(path, includeCallback, session, cache);
        }
    }

    /**
     * Internal method to allow passing in a LogSession so we can manage it externally in tests.
     *
     * @param path the file path for resolving relative includes
     * @param includeCallback callback invoked for each included file
     * @param logSession the LogSession to use for logging warnings during loading
     * @param includeCache the cache for included files to optimize repeated loads
     * @return the processed Java object representation of the YAML file
     * @throws IOException if there is an error reading or processing the YAML
     */
    static @Nullable Object load(Path path, Consumer<Path> includeCallback, LogSession logSession,
            ConcurrentHashMap<Path, @Nullable CacheEntry> includeCache) throws IOException {
        Path modelPath = path;
        try {
            YamlPreprocessor preprocessor = new YamlPreprocessor(path, Map.of(), Set.of(), includeCallback, logSession,
                    includeCache);
            modelPath = preprocessor.getContext().getRelativePath();
            Object result = preprocessor.load();

            // Print a summary of warnings before the LogSession outputs all the warnings.
            int totalWarnings = logSession.getTotalWarningCount();
            if (totalWarnings > 0) {
                int unique = logSession.getTrackedWarnings().size();
                String issuesLabel = (unique == 1) ? "unique issue" : "unique issues";
                String warningLabel = (totalWarnings == 1) ? "warning" : "warnings";

                RAW_LOGGER.warn("Loading YAML model {}: Preprocessing completed with {} {} ({} {}).", modelPath,
                        totalWarnings, warningLabel, unique, issuesLabel);
            }

            return result;
        } catch (MarkedYAMLException e) {
            String errorMsg = e.getMessage();
            Mark mark = e.getProblemMark();
            if (mark != null) {
                String location = "%d:%d".formatted(mark.getLine() + 1, mark.getColumn() + 1);
                String errorClass = e.getClass().getSimpleName();
                errorMsg = "\n%s:%s %s %s".formatted(modelPath, location, errorClass, e.getMessage());
            }
            throw new IOException(errorMsg, e);
        } catch (YAMLException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * Internal load method that performs the actual loading and preprocessing of the YAML file.
     *
     * @return the processed Java object representation of the YAML file
     * @throws IOException if there is an error reading the YAML file
     * @throws YAMLException if there is an error during YAML parsing or preprocessing
     */
    public @Nullable Object load() throws IOException, YAMLException {
        context.getLogger().debug("Loading file({}): {} with given vars {}", context.getIncludeStack().size(),
                context.getAbsolutePath(), context.getVariables());

        // Phase 1: read file bytes and initialize helper objects
        readYamlBytes();
        SourceLocator locator = initSourceLocator();

        // Phase 2: set up initial variables available during first pass
        VariableProcessor variableProcessor = new VariableProcessor(context, recursiveProcessor, mergeKeyProcessor);
        variableProcessor.setSpecialVariables();

        // Phase 3: parse YAML
        Object yamlObj = parseYaml();
        if (!(yamlObj instanceof Map<?, ?> yamlMap)) {
            return yamlObj;
        }

        // Phase 4: extract variables and templates
        Object variablesSection = yamlMap.remove(PreprocessorConfig.VARIABLES_KEY);
        variableProcessor.extractVariables(variablesSection, locator);

        Object templatesSection = yamlMap.remove(PreprocessorConfig.TEMPLATES_KEY);
        new TemplateProcessor(context, recursiveProcessor, mergeKeyProcessor, locator)
                .extractTemplates(templatesSection);

        // Phase 5: extract packages before includes/inserts
        @Nullable
        Object packagesObj = yamlMap.remove(PreprocessorConfig.PACKAGES_KEY);

        // Phase 6: process substitutions, conditional placeholders (!if), !include and !insert
        yamlMap = recursiveProcessor.process(yamlMap, Set.of(SubstitutionPlaceholder.class, IfPlaceholder.class,
                IncludePlaceholder.class, InsertPlaceholder.class));

        // Phase 7: resolve merge keys in main data
        mergeKeyProcessor.resolveMergeKeys(yamlMap);

        // Phase 8: process and merge packages
        new PackageProcessor(context, locator, recursiveProcessor).mergePackages(yamlMap, packagesObj);

        // Phase 9: process override placeholders (!replace, !remove)
        yamlMap = recursiveProcessor.process(yamlMap, Set.of(ReplacePlaceholder.class, RemovePlaceholder.class));

        // Phase 10: final cleanup and optional compiled output
        PreprocessorUtils.removeHiddenKeys(yamlMap);

        // Phase 11: preprocessor settings (top-level file only)
        PreprocessorOptionsResolver options = new PreprocessorOptionsResolver(yamlMap, context, locator);
        if (options.isTopLevel()) {
            if (options.shouldGenerateCompiled()) {
                PreprocessorUtils.writeCompiledOutput(yamlMap, context);
            }

            if (!options.shouldAllowLoading()) {
                throw new YAMLException("Processing skipped: the in-file setting '%s.%s' is set to false"
                        .formatted(PreprocessorConfig.PREPROCESSOR_KEY, PreprocessorConfig.LOAD_INTO_OPENHAB_KEY));
            }
        }

        return yamlMap;
    }

    /**
     * Reads the YAML file bytes with caching based on file modification time to optimize repeated loads of the same
     * file.
     * The bytes are stored in the instance variable {@code yamlBytes} for subsequent processing.
     *
     * @throws IOException
     */
    private void readYamlBytes() throws IOException {
        Path realPath = Objects.requireNonNull(context.getAbsolutePath().toRealPath());
        try {
            final Path key = Objects.requireNonNull(realPath);
            CacheEntry entry = includeCache.compute(key, (p, existing) -> {
                Path nonNullPath = Objects.requireNonNull(p);
                try {
                    long mtime = Files.getLastModifiedTime(nonNullPath).toMillis();
                    if (existing != null && existing.mtime == mtime) {
                        return existing;
                    }
                    byte[] bytes = Files.readAllBytes(nonNullPath);
                    return new CacheEntry(Objects.requireNonNull(bytes), mtime);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            this.yamlBytes = (entry != null && entry.bytes != null) ? entry.bytes : new byte[0];
        } catch (UncheckedIOException e) {
            throw (IOException) Objects.requireNonNull(e.getCause(), "UncheckedIOException must have a cause");
        }
    }

    private SourceLocator initSourceLocator() {
        return new SourceLocator(yamlBytes);
    }

    private @Nullable Object parseYaml() throws IOException {
        return PreprocessorUtils.loadYaml(yamlBytes, context.getRelativePath());
    }

    /**
     * Checks if the given relative path is a file generated by the preprocessor.
     *
     * @param relativePath the relative path to check. It is expected to be relative
     *            to the config root.
     * @return true if it's a preprocessor-generated file, false otherwise
     */
    public static boolean isGeneratedFile(Path relativePath) {
        if (relativePath.isAbsolute()) {
            throw new IllegalArgumentException("Path must be relative");
        }
        return relativePath.getNameCount() > 1 && relativePath.subpath(1, 2).toString().equals("_generated");
    }

    /**
     * Checks if the given model name is an include file based on its extension.
     * An include file ends with .inc.yml or .inc.yaml.
     *
     * @param modelName the model name to check
     * @return true if it's an include file, false otherwise
     */
    public static boolean isIncludeFile(String modelName) {
        return modelName.endsWith(".inc.yml") || modelName.endsWith(".inc.yaml");
    }

    /**
     * Returns the preprocessor context.
     */
    public PreprocessorContext getContext() {
        return context;
    }
}
