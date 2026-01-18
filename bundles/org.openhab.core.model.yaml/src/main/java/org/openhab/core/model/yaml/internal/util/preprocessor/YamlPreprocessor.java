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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.OpenHAB;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholders.IncludePlaceholder;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholders.InsertPlaceholder;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholders.RemovePlaceholder;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholders.ReplacePlaceholder;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholders.SubstitutionPlaceholder;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.representer.Representer;

/**
 * The {@link YamlPreprocessor} is a utility class to load YAML files
 * and preprocess them before they are loaded by {@link org.openhab.core.model.yaml.internal.YamlModelRepositoryImpl}.
 *
 * The following enhancements are made:
 *
 * <ul>
 * <li>Full support for anchors and aliases.
 * <li>Match Jackson's PARSE_BOOLEAN_LIKE_WORDS_AS_STRINGS: only "true" and "false" (case insensitive) are booleans.
 * <li>Allow variable definitions and variable substitution/interpolation.
 * <li>Support <code>!include</code> tag for including other YAML files.
 * <li>Support combining elements using packages.
 * </ul>
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
public class YamlPreprocessor {

    private static final int MAX_INCLUDE_DEPTH = 100;

    static final String TEMPLATES_KEY = "templates";
    private static final String PREPROCESSOR_KEY = "preprocessor";
    private static final String GENERATE_RESOLVED_FILE_KEY = "generate_resolved_file";
    private static final String LOAD_INTO_OPENHAB_KEY = "load_into_openhab";
    private static final String VARIABLES_KEY = "variables";
    private static final String PACKAGES_KEY = "packages";
    private static final String PACKAGE_ID_VAR = "package_id";

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(YamlPreprocessor.class);

    private static final Path configRoot = Path.of(OpenHAB.getConfigFolder()).toAbsolutePath().normalize();
    private static final Path userDataRoot = Path.of(OpenHAB.getUserDataFolder()).toAbsolutePath().normalize();

    private final Path currentPath;
    private final Path currentPathRelative;
    private final Map<String, Object> variables;
    private final Map<String, Object> templates;
    private final Set<Path> includeStack;
    private final Consumer<Path> includeCallback;

    YamlPreprocessor(Path path, Map<String, Object> variables, Set<Path> includeStack, Consumer<Path> includeCallback) {
        this.variables = new LinkedHashMap<>(variables);
        this.templates = new HashMap<>();
        this.includeStack = new HashSet<>(includeStack);
        this.includeCallback = includeCallback;
        this.currentPath = path.toAbsolutePath().normalize();

        this.currentPathRelative = currentPath.startsWith(configRoot) ? configRoot.relativize(currentPath)
                : currentPath;

        // Validate circular inclusion and depth before processing
        if (!this.includeStack.add(currentPath)) {
            String includeStackChain = this.includeStack.stream().map(Path::toString)
                    .collect(Collectors.joining(" -> "));
            throw new YAMLException("Circular inclusion detected: " + includeStackChain + " -> " + currentPath);
        }
        if (this.includeStack.size() > MAX_INCLUDE_DEPTH) {
            throw new YAMLException("Maximum include depth (" + MAX_INCLUDE_DEPTH + ") exceeded");
        }
    }

    /**
     * Load and preprocess a YAML file.
     *
     * @param path the file path for resolving relative includes
     * @param includeCallback callback invoked for each included file
     * @return the processed Java object representation of the YAML file
     * @throws IOException if there is an error reading or processing the YAML
     */
    public static Object load(Path path, Consumer<Path> includeCallback) throws IOException {
        try {
            return new YamlPreprocessor(path, Map.of(), Set.of(), includeCallback).load();
        } catch (YAMLException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * Checks if the given relative path is a file generated by the preprocessor.
     *
     * @param relativePath the relative path to check. It is expected to be relative to the config root.
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

    private Object load() throws IOException, YAMLException {
        LOGGER.debug("Loading file({}): {} with given vars {}", includeStack.size(), currentPath, variables);
        byte[] fileBytes = Files.readAllBytes(currentPath);

        // Set special variables early so they're available in variable definitions during the first pass
        // (e.g., variables: !sub with ${__FILE_NAME__})
        setSpecialVariables();

        // first pass: load the file to extract variables
        Object firstPassData = loadYaml(fileBytes, false);
        if (!(firstPassData instanceof Map<?, ?> firstPassMap)) {
            return firstPassData;
        }

        Object variablesSection = firstPassMap.get(VARIABLES_KEY);
        extractVariables(variablesSection);

        Object templatesSection = firstPassMap.get(TEMPLATES_KEY);
        extractTemplates(templatesSection);

        // Note: The YAML file must be loaded twice.
        // The first pass above extracts user-defined variables,
        // while the second pass below performs variable interpolation.
        // This cannot be avoided, because SnakeYAML executes interpolation during the construction phase itself.
        // Once the object graph is built, substitutions cannot be applied retroactively,
        // so a full reload with the resolved variables is required.
        Object finalPassData = loadYaml(fileBytes, true);
        if (!(finalPassData instanceof Map<?, ?> dataMap)) {
            return finalPassData;
        }

        // we've already extracted these in the first pass
        dataMap.remove(VARIABLES_KEY);
        dataMap.remove(TEMPLATES_KEY);

        LOGGER.debug("Loaded data from {}: {}", currentPath, dataMap);

        // Define default preprocessor settings
        boolean generateCompiled = false;
        boolean allowLoading = true;
        if (includeStack.size() == 1) { // only check preprocessor settings for the top-level file
            Object preprocessorSection = dataMap.remove(PREPROCESSOR_KEY);
            generateCompiled = shouldGenerateResolvedFile(preprocessorSection, generateCompiled);
            allowLoading = shouldAllowLoading(preprocessorSection, allowLoading);
        }

        // Extract packages before processing includes so we can handle them separately
        Object packagesObj = dataMap.remove(PACKAGES_KEY);

        // Process includes in everything except packages
        @SuppressWarnings("unchecked") // resolveCompositionPlaceholders returns a Map when given a map
        Map<Object, Object> resolvedDataMap = (Map<Object, Object>) resolveCompositionPlaceholders(dataMap);
        LOGGER.debug("Loaded includes from {}: {}", currentPath, resolvedDataMap);

        // Process packages separately - this allows us to inject the package ID before processing includes
        if (packagesObj instanceof Map<?, ?> packagesMap) {
            mergePackages(resolvedDataMap, packagesMap);
            LOGGER.debug("Merged packages into data in {}: {}", currentPath, resolvedDataMap);
        } else if (packagesObj != null) {
            LOGGER.warn("YAML model {}: The 'packages' section is not a map", currentPathRelative);
        }

        resolvedDataMap = removeHiddenKeys(resolvedDataMap);

        if (generateCompiled) {
            writeCompiledOutput(resolvedDataMap);
        }

        if (!allowLoading) {
            throw new YAMLException(
                    "'%s.%s' is false; loading is disabled".formatted(PREPROCESSOR_KEY, LOAD_INTO_OPENHAB_KEY));
        }

        return resolvedDataMap;
    }

    Path getPath() {
        return currentPath;
    }

    private Object loadYaml(byte[] fileBytes, boolean finalPass) throws IOException {
        Yaml yaml = newYaml(variables, this, finalPass);
        Object result = yaml.load(new ByteArrayInputStream(fileBytes));
        return Objects.requireNonNull(result);
    }

    /**
     * Extracts variables from the given map.
     */
    private void extractVariables(@Nullable Object variablesSection) {
        if (variablesSection instanceof Map<?, ?> variablesMap) {
            Map<String, Object> extractedVariables = new LinkedHashMap<>();
            variablesMap.forEach((k, v) -> {
                Object key = Objects.requireNonNull(k, "Variable key in YAML cannot be null");
                Object value = Objects.requireNonNull(v, () -> "Value for key '" + key + "' cannot be null");
                String resolvedKey = String.valueOf(resolveSubstitutionPlaceholders(key, variables));
                if (!variables.containsKey(resolvedKey)) { // previous variables (e.g. global) take precedence
                    Object resolvedValue = resolveSubstitutionPlaceholders(value, variables);
                    // add to main variables map so it can be used in subsequent variables
                    variables.put(resolvedKey, resolvedValue);
                    extractedVariables.put(resolvedKey, resolvedValue);
                }
            });
            LOGGER.debug("Extracted variables from {}: {}", currentPathRelative, extractedVariables);
        } else if (variablesSection != null) {
            LOGGER.warn("YAML model {}: 'variables' is not a map", currentPathRelative);
        }

        // Set VARS after extracting user-defined variables so it includes them
        variables.put("VARS", Collections.unmodifiableMap(new LinkedHashMap<>(variables)));
    }

    /**
     * Extracts templates from the given map and stores them into the templates map.
     *
     * @param templatesSection
     */
    private void extractTemplates(@Nullable Object templatesSection) {
        if (templatesSection instanceof Map<?, ?> templatesObj) {
            templatesObj.forEach((k, v) -> {
                Object key = Objects.requireNonNull(k);
                Object value = Objects.requireNonNull(v);
                String resolvedKey = String.valueOf(resolveSubstitutionPlaceholders(key, variables));
                templates.put(resolvedKey, value);
            });
        } else if (templatesSection != null) {
            LOGGER.warn("YAML model {}: 'templates' is not a map", currentPathRelative);
        }
    }

    /**
     * Recursively resolve all SubstitutionPlaceholders in a value (map, list, or scalar),
     * now that the full variables map is available.
     */
    private Object resolveSubstitutionPlaceholders(Object value, Map<String, Object> context) {
        if (value instanceof SubstitutionPlaceholder placeholder) {
            try {
                return VariableInterpolationHelper.evaluateValue(placeholder.value(), placeholder.pattern(),
                        placeholder.isPlainScalar(), context, true);
            } catch (IllegalArgumentException e) {
                // Re-wrap as YAMLException for consistency with YAML error handling
                throw new YAMLException(placeholder.contextDescription() + " " + e.getMessage(), e);
            }
        }

        if (value instanceof IncludePlaceholder placeholder) {
            String resolvedFileName = String.valueOf(resolveSubstitutionPlaceholders(placeholder.fileName(), context));
            Map<Object, Object> resolvedVars = new LinkedHashMap<>();
            placeholder.vars().forEach((vk, vv) -> {
                resolvedVars.put(resolveSubstitutionPlaceholders(vk, context),
                        resolveSubstitutionPlaceholders(vv, context));
            });
            IncludePlaceholder resolvedPlaceholder = new IncludePlaceholder(resolvedFileName, resolvedVars,
                    placeholder.contextDescription());
            return resolvedPlaceholder;
        }

        if (value instanceof InsertPlaceholder placeholder) {
            Object resolvedTemplate = resolveSubstitutionPlaceholders(placeholder.template(), context);
            Map<Object, Object> resolvedVars = new LinkedHashMap<>();
            placeholder.vars().forEach((vk, vv) -> {
                resolvedVars.put(resolveSubstitutionPlaceholders(vk, context),
                        resolveSubstitutionPlaceholders(vv, context));
            });
            InsertPlaceholder resolvedPlaceholder = new InsertPlaceholder(resolvedTemplate, resolvedVars,
                    placeholder.contextDescription());
            return resolvedPlaceholder;
        }

        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> resolved = new LinkedHashMap<>();
            map.forEach((k, v) -> {
                Object key = resolveSubstitutionPlaceholders(Objects.requireNonNull(k), context);
                Object val = resolveSubstitutionPlaceholders(Objects.requireNonNull(v), context);
                resolved.put(key, val);
            });
            return resolved;
        }

        if (value instanceof List<?> list) {
            return list.stream().map(item -> resolveSubstitutionPlaceholders(Objects.requireNonNull(item), context))
                    .toList();
        }

        return value;
    }

    /**
     * Add special file-related variables
     *
     * These are added early so they're available in variable definitions during the first pass
     * Special variables will override any user-defined variables with the same name
     */
    private void setSpecialVariables() {
        Path fileNamePath = currentPath.getFileName();
        String fullFileName = fileNamePath != null ? fileNamePath.toString() : "";
        int dotIndex = fullFileName.lastIndexOf(".");
        String fileName = fullFileName;
        String fileExtension = "";
        if (dotIndex > 0) {
            fileName = fullFileName.substring(0, dotIndex);
            fileExtension = fullFileName.substring(dotIndex + 1);
        }
        Path parentPath = currentPath.getParent();
        String directory = parentPath != null ? parentPath.toString() : "";

        variables.put("OPENHAB_CONF", configRoot.toString());
        variables.put("OPENHAB_USERDATA", userDataRoot.toString());
        variables.put("__FILE__", currentPath.toString());
        variables.put("__FILE_NAME__", fileName);
        variables.put("__FILE_EXT__", fileExtension);
        variables.put("__DIRECTORY__", directory);
        variables.put("__DIR__", directory);
    }

    /**
     * Replace !include and !insert placeholders with their resolved content.
     * This method is called recursively for nested objects.
     */
    private Object resolveCompositionPlaceholders(Object data) {
        if (data instanceof IncludePlaceholder placeholder) {
            return resolveIncludePlaceholder(placeholder);
        } else if (data instanceof InsertPlaceholder placeholder) {
            return resolveInsertPlaceholder(placeholder);
        } else if (data instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked") // Our ModelConstructor doesn't return null
            Map<Object, Object> dataMap = (Map<Object, Object>) data;
            return dataMap.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            entry -> resolveCompositionPlaceholders(entry.getValue()),
                            (existing, replacement) -> replacement, LinkedHashMap::new));
        } else if (data instanceof List<?> dataList) {
            return dataList.stream().map(this::resolveCompositionPlaceholders).toList();
        }
        return data;
    }

    /**
     * Resolves an {@code IncludePlaceholder}, loads the referenced file (recursively
     * following any nested includes), and returns the fully expanded content.
     */
    Object resolveIncludePlaceholder(IncludePlaceholder placeholder) {
        String includeFileName = String.valueOf(placeholder.fileName());
        if (includeFileName.isBlank()) {
            // We do another check here in case the includeFileName was a variable that resolved to empty
            throw new YAMLException(placeholder.contextDescription() + " Include file name cannot be empty");
        }
        Path includeFilePath = currentPath.resolveSibling(includeFileName);
        Map<String, Object> localVars = combinePlaceholderVars(placeholder.vars());

        try {
            YamlPreprocessor includePreprocessor = new YamlPreprocessor(includeFilePath, localVars,
                    includeStack, includeCallback);
            includeCallback.accept(includePreprocessor.getPath()); // use the normalized absolute path
            return includePreprocessor.load();
        } catch (IOException | YAMLException e) {
            // Only wrap the exception if it's not already an "Error loading include file" message
            // to avoid repeating the wrapper message in nested includes
            String errorMessage = switch (e.getMessage()) {
                case null -> "";
                case String msg when msg.contains("Error loading include file") -> throw new YAMLException(msg, e);
                case String msg when !msg.isEmpty() && !msg.equals(includeFilePath.toString()) -> ": " + msg;
                default -> "";
            };
            // Wrap the exception to indicate where the error occurred
            throw new YAMLException("%s Error loading include file '%s': %s".formatted(placeholder.contextDescription(), includeFileName, errorMessage), e);
        }
    }

    /**
     * Resolves an {@code InsertPlaceholder} recursively
     * following any nested inserts, and returns the fully expanded content.
     */
    Object resolveInsertPlaceholder(InsertPlaceholder placeholder) {
        String templateName = String.valueOf(placeholder.template());
        if (templateName.isBlank()) {
            // We do another check here in case the templateName was a variable that resolved to empty
            throw new YAMLException(placeholder.contextDescription() + " Insert template name cannot be empty");
        }
        Map<String, Object> localVars = combinePlaceholderVars(placeholder.vars());
        Object templateObj = templates.get(templateName);
        if (templateObj == null) {
            throw new YAMLException("%s Error inserting template '%s': template not found"
                    .formatted(placeholder.contextDescription(), templateName));
        }
        Object resolvedTemplate = resolveSubstitutionPlaceholders(templateObj, localVars);
        return resolvedTemplate;
    }

    /**
     * Builds a combined variable map for includes/inserts.
     * The current variables are included, and any local variables override them.
     *
     * @param placeholderVars the vars from the include/insert placeholder
     * @return the combined variable map that includes the current variables and local variables
     */
    private Map<String, Object> combinePlaceholderVars(Map<Object, Object> placeholderVars) {
        Map<String, Object> combinedVars = new LinkedHashMap<>(variables);
        // local vars override current vars
        placeholderVars.forEach((k, v) -> {
            Object key = Objects.requireNonNull(k, "vars key in YAML cannot be null");
            Object value = Objects.requireNonNull(v, () -> "Value for vars key '" + key + "' cannot be null");
            String resolvedKey = String.valueOf(key);
            combinedVars.put(resolvedKey, value);
        });

        return combinedVars;
    }

    /**
     * Deep merge packages map into the main data map
     * if the same key exists in both the main map and the package, the main map value is kept
     *
     * @param mainData the main data map to merge into
     * @param packages the packages to merge
     */
    private void mergePackages(Map<Object, Object> mainData, Map<?, ?> packages) {
        packages.forEach((pkgKey, pkg) -> {
            String packageId = String.valueOf(pkgKey);
            Object processedPkg = Objects.requireNonNull(pkg);

            Object pkgWithId = injectPackageId(processedPkg, packageId);
            if (resolveCompositionPlaceholders(pkgWithId) instanceof Map<?, ?> resolvedPkg) {
                @SuppressWarnings("unchecked")
                Map<Object, Object> resolvedPkgMap = (Map<Object, Object>) resolvedPkg;
                LOGGER.debug("Merging package '{}' {} into main data: {}", packageId, resolvedPkgMap, mainData);
                mergeElements(mainData, resolvedPkgMap);
            } else {
                LOGGER.warn("YAML model {}: Package '{}' did not resolve to a map: {}", currentPathRelative, packageId,
                        processedPkg);
            }
        });

        // Recursively resolve all ReplacePlaceholder and RemovePlaceholder instances
        applyOverridePlaceholders(mainData);
    }

    /**
     * Recursively inject the package ID into IncludePlaceholder and InsertPlaceholder vars.
     * This adds a `package_id` variable to all includes within the package.
     *
     * @param data the package data to process
     * @param packageId the package ID to inject
     * @return the modified package data
     */
    private static Object injectPackageId(Object data, String packageId) {
        LOGGER.debug("Injecting package_id='{}' into data: {}", packageId, data);
        if (data instanceof IncludePlaceholder placeholder) {
            Map<Object, Object> newVars = new HashMap<>(placeholder.vars());
            newVars.putIfAbsent(PACKAGE_ID_VAR, packageId);
            return new IncludePlaceholder(placeholder.fileName(), newVars, placeholder.contextDescription());
        } else if (data instanceof InsertPlaceholder placeholder) {
            Map<Object, Object> newVars = new HashMap<>(placeholder.vars());
            newVars.putIfAbsent(PACKAGE_ID_VAR, packageId);
            return new InsertPlaceholder(placeholder.template(), newVars, placeholder.contextDescription());
        } else if (data instanceof Map<?, ?> dataMap) {
            return dataMap.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            entry -> injectPackageId(Objects.requireNonNull(entry.getValue()), packageId),
                            (existing, replacement) -> replacement, LinkedHashMap::new));
        } else if (data instanceof List<?> dataList) {
            return dataList.stream().map(item -> injectPackageId(Objects.requireNonNull(item), packageId)).toList();
        } else {
            return data;
        }
    }

    /**
     * Recursively merges packageData into mainData.
     * - Maps are merged in-place (recursive composition) by default
     * - Lists are concatenated (creates new list with both elements) by default
     * - If a value has !replace or !remove tag, the package value is ignored - to be handled later
     * - Other values in packageData are added if not present in mainData
     *
     * Note that mainData is modified in-place.
     *
     * @param mainData the main data map to merge into
     * @param packageData the package data to merge from
     */
    private static void mergeElements(Map<Object, Object> mainData, Map<Object, Object> packageData) {
        packageData.forEach((key, value) -> {
            if (mainData.containsKey(key)) {
                Object mainValue = mainData.get(key);
                if (mainValue instanceof ReplacePlaceholder || mainValue instanceof SubstitutionPlaceholder) {
                    // We'll deal with these recursively in applyOverridePlaceholders
                    return;
                }
                if (mainValue instanceof Map<?, ?> && value instanceof Map<?, ?> valueMap) {
                    @SuppressWarnings("unchecked")
                    Map<Object, Object> mainMap = (Map<Object, Object>) mainValue;
                    @SuppressWarnings("unchecked")
                    Map<Object, Object> pkgMap = (Map<Object, Object>) valueMap;
                    mergeElements(mainMap, pkgMap);
                    mainData.put(key, mainMap);
                    return;
                }
                if (mainValue instanceof List<?> mainValueList && value instanceof List<?> pkgValueList) {
                    mainData.put(key, Stream.concat(pkgValueList.stream(), mainValueList.stream()).toList());
                    return;
                }
                // For non-map/non-list values, keep the main value overwriting the package value
            } else {
                mainData.put(key, value);
            }
        });
    }

    /**
     * Applies ReplacePlaceholder and RemovePlaceholder instances in the data structure.
     * - RemovePlaceholder: removes the key from its parent map
     * - ReplacePlaceholder: unwraps to its contained object
     */
    private static void applyOverridePlaceholders(Map<Object, Object> data) {
        // First, recursively process nested structures
        data.forEach((key, value) -> {
            if (value instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                Map<Object, Object> mapValue = (Map<Object, Object>) value;
                applyOverridePlaceholders(mapValue);
            }
        });

        // Then remove RemovePlaceholder entries and unwrap ReplacePlaceholder entries
        data.entrySet().removeIf(entry -> entry.getValue() instanceof RemovePlaceholder);
        data.replaceAll((key, value) -> value instanceof ReplacePlaceholder replaceObj ? replaceObj.object() : value);
    }

    /**
     * Removes keys that start with a dot from the given map.
     *
     * @param dataMap the map to process
     * @return a new map without keys that start with a dot
     */
    private static Map<Object, Object> removeHiddenKeys(Map<Object, Object> dataMap) {
        return dataMap.entrySet().stream()
                .filter(entry -> !(entry.getKey() instanceof String key && key.startsWith(".")))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (existing, replacement) -> replacement, LinkedHashMap::new));
    }

    private boolean shouldGenerateResolvedFile(@Nullable Object preprocessorSection, boolean defaultValue) {
        return getPreprocessorBoolean(preprocessorSection, GENERATE_RESOLVED_FILE_KEY, defaultValue);
    }

    private boolean shouldAllowLoading(@Nullable Object preprocessorSection, boolean defaultValue) {
        return getPreprocessorBoolean(preprocessorSection, LOAD_INTO_OPENHAB_KEY, defaultValue);
    }

    private boolean getPreprocessorBoolean(@Nullable Object preprocessorSection, String key, boolean defaultValue) {
        if (preprocessorSection == null) {
            return defaultValue;
        }
        if (preprocessorSection instanceof Map<?, ?> preprocessorMap) {
            Object value = preprocessorMap.get(key);
            if (value instanceof Boolean flag) {
                return flag;
            }
            if (value != null) {
                LOGGER.warn("YAML model {}: '{}.{}' is not a boolean; using default {}", currentPathRelative,
                        PREPROCESSOR_KEY, key, defaultValue);
            }
            return defaultValue;
        }

        LOGGER.warn("YAML model {}: '{}' section is not a map; using default {} for '{}'", currentPathRelative,
                PREPROCESSOR_KEY, defaultValue, key);
        return defaultValue;
    }

    private void writeCompiledOutput(Object dataMap) throws IOException {
        Path outputFile;
        if (currentPath.startsWith(configRoot) && currentPathRelative.getNameCount() >= 2) {
            Path elementRoot = configRoot.resolve(currentPathRelative.subpath(0, 1));
            Path outputRoot = elementRoot.resolve("_generated");
            Path currentPathRelativeToElementRoot = elementRoot.relativize(currentPath);
            outputFile = outputRoot.resolve(currentPathRelativeToElementRoot);
        } else {
            LOGGER.warn("YAML model {}: Cannot place compiled output under config folder '{}'; writing next to source",
                    currentPathRelative, configRoot);
            Path fallbackDir = currentPath.resolveSibling("_generated");
            outputFile = fallbackDir.resolve(currentPath.getFileName());
        }

        Path outputDir = outputFile.getParent();
        if (outputDir != null) {
            Files.createDirectories(outputDir);
        }

        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setIndentWithIndicator(true);
        dumperOptions.setIndicatorIndent(2);
        dumperOptions.setPrettyFlow(true);

        Yaml yaml = new Yaml(dumperOptions);
        String compiledYaml = yaml.dump(dataMap);

        compiledYaml = """
                # ==============================================================================
                # Generated by openHAB %s (%s) YAML Preprocessor, DO NOT EDIT
                # Source:    %s
                # Generated: %s
                # ==============================================================================
                                """.strip().formatted(OpenHAB.getVersion(), OpenHAB.buildString(), currentPathRelative,
                java.time.ZonedDateTime.now()) + "\n" + compiledYaml;

        Files.writeString(outputFile, compiledYaml, StandardCharsets.UTF_8);
        LOGGER.info("YAML model {}: Generated compiled YAML output to {}", currentPathRelative, outputFile);
    }

    static Yaml newYaml(Map<String, Object> variables, YamlPreprocessor preprocessor, boolean finalPass) {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        return new Yaml(new ModelConstructor(loaderOptions, variables, preprocessor, finalPass),
                new Representer(new DumperOptions()), new DumperOptions(), loaderOptions, new ModelResolver());
    }
}
