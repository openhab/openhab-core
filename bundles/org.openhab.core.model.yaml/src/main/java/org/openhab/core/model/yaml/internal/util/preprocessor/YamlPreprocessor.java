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
    private final Set<Path> includeStack;
    private final Consumer<Path> includeCallback;

    YamlPreprocessor(Path path, Map<String, Object> variables, Set<Path> includeStack, Consumer<Path> includeCallback) {
        this.variables = new LinkedHashMap<>(variables);
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
        dataMap.remove(VARIABLES_KEY); // we've already extracted the variables in the first pass
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
        @SuppressWarnings("unchecked") // resolveIncludes returns a Map when given a map
        Map<Object, Object> resolvedDataMap = (Map<Object, Object>) resolveIncludes(dataMap);
        LOGGER.debug("Loaded includes from {}: {}", currentPath, resolvedDataMap);

        // Process packages separately - this allows us to inject the package ID before processing includes
        if (packagesObj instanceof Map<?, ?> packagesMap) {
            mergePackages(resolvedDataMap, packagesMap);
            LOGGER.debug("Merged packages into data in {}: {}", currentPath, resolvedDataMap);
        } else if (packagesObj != null) {
            LOGGER.warn("YAML model {}: The 'packages' section is not a map", currentPathRelative);
        }

        resolvedDataMap = excludeHiddenKeys(resolvedDataMap);

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
        return yaml.load(new ByteArrayInputStream(fileBytes));
    }

    /**
     * Extracts variables from the given map.
     */
    private void extractVariables(@Nullable Object variablesSection) {
        // Resolve if it's a substitution placeholder (e.g., variables: !sub)
        if (variablesSection instanceof SubstitutionPlaceholder) {
            variablesSection = resolveSubstitutionPlaceholders(variablesSection);
        }

        if (variablesSection instanceof Map<?, ?> variablesMap) {
            Map<String, Object> extractedVariables = new LinkedHashMap<>();
            variablesMap.forEach((k, v) -> {
                Object key = Objects.requireNonNull(k, "Variable key in YAML cannot be null");
                Object value = Objects.requireNonNull(v, () -> "Value for key '" + key + "' cannot be null");
                LOGGER.debug("Extracting variable '{}' with raw value '{}' from {}", key, value, currentPathRelative);
                String resolvedKey = String.valueOf(resolveSubstitutionPlaceholders(key));
                if (!variables.containsKey(resolvedKey)) { // previous variables (e.g. global) take precedence
                    Object resolvedValue = resolveSubstitutionPlaceholders(value);
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
     * Recursively resolve all SubstitutionPlaceholders in a value (map, list, or scalar),
     * now that the full variables map is available.
     */
    private Object resolveSubstitutionPlaceholders(Object value) {
        // If the value is an IncludePlaceholder, resolve its fileName and vars
        if (value instanceof IncludePlaceholder includePlaceholder) {
            String resolvedFileName = String.valueOf(resolveSubstitutionPlaceholders(includePlaceholder.fileName()));
            Map<Object, Object> resolvedVars = new LinkedHashMap<>();
            includePlaceholder.vars().forEach((vk, vv) -> {
                resolvedVars.put(resolveSubstitutionPlaceholders(vk), resolveSubstitutionPlaceholders(vv));
            });
            IncludePlaceholder resolvedIncludePlaceholder = new IncludePlaceholder(resolvedFileName, resolvedVars);
            return resolveIncludePlaceholder(resolvedIncludePlaceholder);
        }
        if (value instanceof SubstitutionPlaceholder placeholder) {
            String contextDescription = currentPathRelative + " [variables]";
            try {
                return VariableInterpolationHelper.evaluateValue(placeholder.value(), placeholder.pattern(),
                        placeholder.isPlainScalar(), variables, contextDescription, false);
            } catch (IllegalArgumentException e) {
                // Re-wrap as YAMLException for consistency with YAML error handling
                throw new YAMLException(e.getMessage(), e);
            }
        }
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> resolved = new LinkedHashMap<>();
            map.forEach((k, v) -> {
                Object key = resolveSubstitutionPlaceholders(Objects.requireNonNull(k));
                Object val = resolveSubstitutionPlaceholders(Objects.requireNonNull(v));
                resolved.put(key, val);
            });
            return resolved;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(this::resolveSubstitutionPlaceholders).toList();
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
     * Process special nodes in the YAML data that correspond to !include.
     * This method is called recursively for nested objects.
     */
    private Object resolveIncludes(Object data) {
        if (data instanceof IncludePlaceholder includeObject) {
            return resolveIncludePlaceholder(includeObject);
        } else if (data instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked") // Our ModelConstructor doesn't return null
            Map<Object, Object> dataMap = (Map<Object, Object>) data;
            return dataMap.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> resolveIncludes(entry.getValue()),
                            (existing, replacement) -> replacement, LinkedHashMap::new));
        } else if (data instanceof List<?> dataList) {
            return dataList.stream().map(this::resolveIncludes).toList();
        }
        return data;
    }

    /**
     * Resolves an {@code IncludePlaceholder}, loads the referenced file (recursively
     * following any nested includes), and returns the fully expanded content.
     */
    Object resolveIncludePlaceholder(IncludePlaceholder includeObject) {
        String includeFileName = String.valueOf(includeObject.fileName());
        if (includeFileName.isBlank()) {
            // We do another check here in case the includeFileName was a variable that resolved to empty
            throw new YAMLException("Include file name cannot be empty (included from '" + currentPath + "')");
        }
        Path includeFilePath = currentPath.resolveSibling(includeFileName);
        Map<String, Object> includeVars = new HashMap<>(variables);
        // include vars override current vars
        includeObject.vars().forEach((k, v) -> {
            Object key = Objects.requireNonNull(k, "Include variable key in YAML cannot be null");
            Object value = Objects.requireNonNull(v,
                    () -> "Value for include variable key '" + key + "' cannot be null");
            String resolvedKey = String.valueOf(key);
            includeVars.put(resolvedKey, value);
        });
        try {
            YamlPreprocessor includePreprocessor = new YamlPreprocessor(includeFilePath, includeVars,
                    includeStack, includeCallback);
            includeCallback.accept(includePreprocessor.getPath()); // use the normalized absolute path
            return includePreprocessor.load();
        } catch (IOException | YAMLException e) {
            // Only wrap the exception if it's not already an "Error loading include file" message
            // to avoid repeating the wrapper message in nested includes
            String errorMessage = switch (e.getMessage()) {
                case null -> "";
                case String msg when msg.startsWith("Error loading include file") -> throw new YAMLException(msg, e);
                case String msg when !msg.isEmpty() && !msg.equals(includeFilePath) -> ": " + msg;
                default -> "";
            };
            // Wrap the exception to indicate where the error occurred
            throw new YAMLException("Error loading include file '" + includeFileName + "' (included from '"
                    + currentPath + ")" + errorMessage, e);
        }
    }

    /**
     * Recursively merge packages into the main data map
     * if the same key exists in both the main map and the package, the main map value is kept
     *
     * @param mainData the main data map to merge into
     * @param packages the packages to merge
     */
    private void mergePackages(Map<Object, Object> mainData, Map<?, ?> packages) {
        packages.forEach((pkgKey, pkg) -> {
            String packageId = String.valueOf(pkgKey);
            Object processedPkg = pkg;

            // If the package is an IncludePlaceholder, inject the ID first, then process it
            if (pkg instanceof IncludePlaceholder includeObj) {
                Map<Object, Object> newVars = new HashMap<>(includeObj.vars());
                newVars.putIfAbsent(PACKAGE_ID_VAR, packageId);
                IncludePlaceholder pkgWithId = new IncludePlaceholder(includeObj.fileName(), newVars);
                processedPkg = resolveIncludes(pkgWithId);
            }

            if (processedPkg instanceof Map<?, ?> pkgMap) {
                // Also inject the package ID into all nested IncludePlaceholders within the package
                Map<Object, Object> pkgWithId = injectPackageId(pkgMap, packageId);
                // Process any remaining includes after injection
                @SuppressWarnings("unchecked") // resolveIncludes returns a Map when given a map
                Map<Object, Object> resolvedPkgWithId = (Map<Object, Object>) resolveIncludes(pkgWithId);
                mergeElements(mainData, resolvedPkgWithId);
            } else {
                LOGGER.warn("YAML model {}: Package '{}' did not resolve to a map: {}", currentPathRelative, packageId,
                        processedPkg);
            }
        });

        // Recursively resolve all ReplacePlaceholder and RemovePlaceholder instances
        resolvePlaceholders(mainData);
    }

    /**
     * Recursively inject the package ID into IncludePlaceholder vars.
     * This adds a `package_id` variable to all includes within the package.
     *
     * @param data the package data to process
     * @param packageId the package ID to inject
     * @return the modified package data
     */
    private static Map<Object, Object> injectPackageId(Map<?, ?> data, String packageId) {
        return data.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> {
            Object value = Objects.requireNonNull(entry.getValue());
            if (value instanceof IncludePlaceholder includeObj) {
                Map<Object, Object> newVars = new HashMap<>(includeObj.vars());
                newVars.putIfAbsent(PACKAGE_ID_VAR, packageId);
                return new IncludePlaceholder(includeObj.fileName(), newVars);
            } else if (value instanceof Map<?, ?> valueMap) {
                return injectPackageId(valueMap, packageId);
            } else if (value instanceof List<?> listValue) {
                return listValue.stream().map(item -> {
                    if (item instanceof IncludePlaceholder includeObj) {
                        Map<Object, Object> newVars = new HashMap<>(includeObj.vars());
                        newVars.putIfAbsent(PACKAGE_ID_VAR, packageId);
                        return new IncludePlaceholder(includeObj.fileName(), newVars);
                    } else if (item instanceof Map<?, ?> mapItem) {
                        return injectPackageId(mapItem, packageId);
                    }
                    return Objects.requireNonNull(item);
                }).toList();
            }
            return value;
        }, (existing, replacement) -> replacement, LinkedHashMap::new));
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
            Objects.requireNonNull(key, "Encountered null key when merging package data");
            Objects.requireNonNull(value, "Encountered null value for key '" + key + "' when merging package data");
            if (mainData.containsKey(key)) {
                Object mainValue = mainData.get(key);
                if (mainValue instanceof ReplacePlaceholder) {
                    // With !replace, we keep only the main value (discard package value entirely)
                    // resolvePlaceholders will unwrap the ReplacePlaceholder later
                    return;
                }
                if (mainValue instanceof RemovePlaceholder) {
                    // If main value is !remove, we'll ignore the package value
                    // resolvePlaceholders will remove the key later
                    return;
                }
                // Default behavior: merge maps and lists
                if (mainValue instanceof Map<?, ?> && value instanceof Map<?, ?> valueMap) {
                    @SuppressWarnings("unchecked") // SnakeYAML constructs maps as Map<Object, Object>
                    Map<Object, Object> mainMap = (Map<Object, Object>) mainValue;
                    @SuppressWarnings("unchecked")
                    Map<Object, Object> pkgMap = (Map<Object, Object>) valueMap;
                    mergeElements(mainMap, pkgMap);
                    mainData.put(key, mainMap);
                    return;
                }
                if (mainValue instanceof List<?> mainValueList && value instanceof List<?> pkgValueList) {
                    // append main list after package list
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
     * Recursively resolves ReplacePlaceholder and RemovePlaceholder instances in the data structure.
     * - RemovePlaceholder: removes the key from its parent map
     * - ReplacePlaceholder: unwraps to its contained object
     */
    private static void resolvePlaceholders(Map<Object, Object> data) {
        // First, recursively process nested structures
        data.forEach((key, value) -> {
            if (value instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                Map<Object, Object> mapValue = (Map<Object, Object>) value;
                resolvePlaceholders(mapValue);
            }
        });

        // Then remove RemovePlaceholder entries and unwrap ReplacePlaceholder entries
        data.entrySet().removeIf(entry -> entry.getValue() instanceof RemovePlaceholder);
        data.replaceAll((key, value) -> value instanceof ReplacePlaceholder replaceObj ? replaceObj.object() : value);
    }

    private static Map<Object, Object> excludeHiddenKeys(Map<Object, Object> dataMap) {
        // Exclude keys that start with a dot
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
