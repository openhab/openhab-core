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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.model.yaml.internal.util.preprocessor.tags.IncludeObject;
import org.openhab.core.model.yaml.internal.util.preprocessor.tags.RemoveObject;
import org.openhab.core.model.yaml.internal.util.preprocessor.tags.ReplaceObject;
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
    // Maximum allowed include depth to prevent stack overflow.
    // Stack overflow has been observed at around 450 on a large system,
    // so we use a conservative default of 100 to avoid issues.
    private static final int MAX_INCLUDE_DEPTH = 100;

    private static final String VARIABLES_KEY = "variables";
    private static final String PACKAGES_KEY = "packages";
    private static final String PACKAGE_ID_VAR = "package_id";

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(YamlPreprocessor.class);

    private final Path currentFile;
    private final Map<String, Object> variables;
    private final Set<Path> includeStack;
    private final Consumer<Path> includeCallback;

    private YamlPreprocessor(Path file, Map<String, Object> variables, Set<Path> includeStack,
            Consumer<Path> includeCallback) {
        this.currentFile = file;
        this.variables = new LinkedHashMap<>(variables);
        this.includeStack = new HashSet<>(includeStack);
        this.includeCallback = includeCallback;

        // Validate circular inclusion and depth before processing
        if (!this.includeStack.add(currentFile)) {
            String includeStackChain = this.includeStack.stream().map(Path::toString)
                    .collect(Collectors.joining(" -> "));
            throw new YAMLException("Circular inclusion detected: " + includeStackChain + " -> " + currentFile);
        }
        if (this.includeStack.size() > MAX_INCLUDE_DEPTH) {
            throw new YAMLException("Maximum include depth (" + MAX_INCLUDE_DEPTH + ") exceeded");
        }
    }

    public static Object load(Path file, Consumer<Path> includeCallback) throws IOException {
        try {
            return new YamlPreprocessor(file, new LinkedHashMap<>(), new HashSet<>(), includeCallback).load();
        } catch (YAMLException e) {
            // rethrow as IOException so the caller has a simpler error handling and avoids dependency on SnakeYAML
            throw new IOException(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Object load() throws IOException, YAMLException {
        LOGGER.debug("Loading file({}): {} with given vars {}", includeStack.size(), currentFile, variables);

        // first pass: load the file to extract variables
        Object yamlData = loadYaml(false);
        if (yamlData instanceof Map) {
            if (!extractVariables((Map<String, Object>) yamlData)) {
                LOGGER.warn("{}: 'variables' is not a map", currentFile);
            }
        } else {
            return yamlData;
        }

        // Call this after extracting user variables so that special variables supersede them
        addSpecialVariables();

        // Note: The YAML file must be loaded twice.
        // The first pass above extracts user-defined variables,
        // while the second pass below performs variable interpolation.
        // This cannot be avoided, because SnakeYAML executes interpolation during the construction phase itself.
        // Once the object graph is built, substitutions cannot be applied retroactively,
        // so a full reload with the resolved variables is required.
        Map<String, Object> dataMap = (Map<String, Object>) loadYaml(true);
        dataMap.remove(VARIABLES_KEY); // we've already extracted the variables in the first pass
        LOGGER.debug("Loaded data from {}: {}", currentFile, dataMap);

        // Extract packages before processing includes so we can handle them separately
        Object packagesObj = dataMap.remove(PACKAGES_KEY);

        // Process includes in everything except packages
        dataMap = (Map<String, Object>) processIncludes(dataMap);
        LOGGER.debug("Loaded includes from {}: {}", currentFile, dataMap);

        // Process packages separately - this allows us to inject the package ID before processing includes
        if (packagesObj instanceof Map<?, ?> packages) {
            mergePackages(dataMap, (Map<String, Object>) packages);
        } else if (packagesObj != null) {
            LOGGER.warn("{}: The 'packages' section is not a map", currentFile);
        }

        LOGGER.debug("Combined data from {}: {}", currentFile, dataMap);

        dataMap = excludeHiddenKeys(dataMap);
        return dataMap;
    }

    private Object loadYaml(boolean finalPass) throws IOException {
        try (InputStream inputStream = Files.newInputStream(currentFile)) {
            Yaml yaml = newYaml(variables, currentFile, finalPass);
            return yaml.load(inputStream);
        } // let the caller catch the exception and log a message
    }

    /**
     * Extracts variables from the given map.
     *
     * @return true if the variables were successfully extracted or if they were not present.
     *         false if the variables value is not a map.
     */
    private boolean extractVariables(Map<String, Object> dataMap) {
        Object variablesSection = dataMap.get(VARIABLES_KEY);
        if (variablesSection instanceof Map<?, ?> variablesMap) {
            Map<String, Object> newVariables = new LinkedHashMap<>();
            variablesMap.forEach((key, value) -> {
                if (key == null) {
                    LOGGER.warn("Encountered null key in '{}' section; value '{}' will be ignored", VARIABLES_KEY,
                            value);
                    return;
                }

                String keyStr = key.toString();
                if (!variables.containsKey(keyStr)) {
                    // Treat null values as empty strings so variables defined without a value
                    // (e.g., `empty_value:`) are recognized and can be used with filters like `default()`.
                    Object normalized = value == null ? "" : value;
                    newVariables.put(keyStr, normalized);
                }
            });
            LOGGER.debug("Extracted new variables from {}: {}", currentFile, newVariables);
            variables.putAll(newVariables);
            return true;
        } else if (variablesSection != null) {
            return false; // not a map, not ok
        }
        return true; // no variables section found, that's ok
    }

    // Add special variables so they get interpolated in the second pass
    // Special variables will override any user-defined variables with the same name
    private void addSpecialVariables() {
        Path absolutePath = currentFile.toAbsolutePath();
        variables.put("__FILE__", absolutePath.toString());
        Path fileNamePath = currentFile.getFileName();
        String fullFileName = fileNamePath != null ? fileNamePath.toString() : "";
        int dotIndex = fullFileName.lastIndexOf(".");
        String fileName = fullFileName;
        String fileExtension = "";
        if (dotIndex > 0) {
            fileName = fullFileName.substring(0, dotIndex);
            fileExtension = fullFileName.substring(dotIndex + 1);
        }
        variables.put("__FILE_NAME__", fileName);
        variables.put("__FILE_EXT__", fileExtension);
        Path parentPath = absolutePath.getParent();
        String directory = parentPath != null ? parentPath.toString() : "";
        variables.put("__DIRECTORY__", directory);
        variables.put("__DIR__", directory);

        // Expose variables map for dynamic access (snapshot to avoid self-reference)
        variables.put("VARS", Collections.unmodifiableMap(new LinkedHashMap<>(variables)));
    }

    /**
     * Process special nodes in the YAML data that correspond to !include.
     * This method is called recursively for nested objects.
     */
    @SuppressWarnings("unchecked")
    private Object processIncludes(Object data) {
        if (data instanceof IncludeObject includeObject) {
            return loadIncludeFile(includeObject);
        } else if (data instanceof Map) {
            Map<String, Object> dataMap = (Map<String, Object>) data;
            return dataMap.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> processIncludes(entry.getValue()),
                            (existing, replacement) -> replacement, LinkedHashMap::new));
        } else if (data instanceof List) {
            List<Object> dataList = (List<Object>) data;
            return dataList.stream().map(this::processIncludes).toList();
        }
        return data;
    }

    // Load the included file (recursively) and return its content
    private Object loadIncludeFile(IncludeObject includeObject) {
        String includeFile = includeObject.fileName();
        Path includeFilePath = currentFile.resolveSibling(includeFile);
        Map<String, Object> includeVars = new HashMap<>(variables);
        // include vars override current vars
        includeVars.putAll(includeObject.vars());
        try {
            includeCallback.accept(includeFilePath);
            return new YamlPreprocessor(includeFilePath, includeVars, includeStack, includeCallback).load();
        } catch (IOException | YAMLException e) {
            // Only wrap the exception if it's not already an "Error loading include file" message
            // to avoid repeating the wrapper message in nested includes
            String errorMessage = switch (e.getMessage()) {
                case null -> "";
                case String msg when msg.startsWith("Error loading include file") -> throw new YAMLException(msg, e);
                case String msg when !msg.isEmpty() && !msg.equals(includeFile) -> ": " + msg;
                default -> "";
            };
            // Wrap the exception to indicate where the error occurred
            throw new YAMLException("Error loading include file '" + includeObject.fileName() + "' (included from '"
                    + currentFile + "')" + errorMessage, e);
        }
    }

    // Recursively merge packages into the main data map
    // if the same key exists in both the main map and the package, the main map value is kept
    @SuppressWarnings("unchecked")
    private void mergePackages(Map<String, Object> mainData, Map<String, Object> packages) {
        packages.forEach((packageId, pkg) -> {
            Object processedPkg = pkg;

            // If the package is an IncludeObject, inject the ID first, then process it
            if (pkg instanceof IncludeObject includeObj) {
                Map<String, Object> newVars = new HashMap<>(includeObj.vars());
                newVars.putIfAbsent(PACKAGE_ID_VAR, packageId);
                IncludeObject pkgWithId = new IncludeObject(includeObj.fileName(), newVars);
                processedPkg = processIncludes(pkgWithId);
            }

            if (processedPkg instanceof Map) {
                Map<String, Object> pkgMap = (Map<String, Object>) processedPkg;
                // Also inject the package ID into all nested IncludeObjects within the package
                Map<String, Object> pkgWithId = injectPackageId(pkgMap, packageId);
                // Process any remaining includes after injection
                pkgWithId = (Map<String, Object>) processIncludes(pkgWithId);
                mergeElements(mainData, pkgWithId);
            } else {
                LOGGER.warn("Package '{}' did not resolve to a map: {}", packageId, processedPkg);
            }
        });

        // Recursively resolve all ReplaceObject and RemoveObject instances
        resolveSpecialObjects(mainData);
    }

    /**
     * Recursively inject the package ID into IncludeObject vars.
     * This adds a `package_id` variable to all includes within the package.
     *
     * @param data the package data to process
     * @param packageId the package ID to inject
     * @return the modified package data
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> injectPackageId(Map<String, Object> data, String packageId) {
        return data.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> {
            Object value = entry.getValue();
            if (value instanceof IncludeObject includeObj) {
                Map<String, Object> newVars = new HashMap<>(includeObj.vars());
                newVars.putIfAbsent(PACKAGE_ID_VAR, packageId);
                return new IncludeObject(includeObj.fileName(), newVars);
            } else if (value instanceof Map) {
                return injectPackageId((Map<String, Object>) value, packageId);
            } else if (value instanceof List) {
                List<Object> list = (List<Object>) value;
                return list.stream().map(item -> {
                    if (item instanceof IncludeObject includeObj) {
                        Map<String, Object> newVars = new HashMap<>(includeObj.vars());
                        newVars.putIfAbsent(PACKAGE_ID_VAR, packageId);
                        return new IncludeObject(includeObj.fileName(), newVars);
                    } else if (item instanceof Map) {
                        return injectPackageId((Map<String, Object>) item, packageId);
                    }
                    return item;
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
    @SuppressWarnings("unchecked")
    private static void mergeElements(Map<String, Object> mainData, Map<String, Object> packageData) {
        packageData.forEach((key, value) -> {
            if (mainData.containsKey(key)) {
                Object mainValue = mainData.get(key);
                if (mainValue instanceof ReplaceObject) {
                    // With !replace, we keep only the main value (discard package value entirely)
                    // resolveSpecialObjects will unwrap the ReplaceObject later
                    return;
                }
                if (mainValue instanceof RemoveObject) {
                    // If main value is !remove, we'll ignore the package value
                    // resolveSpecialObjects will remove the key later
                    return;
                }
                // Default behavior: merge maps and lists
                if (mainValue instanceof Map && value instanceof Map) {
                    Map<String, Object> mainMap = (Map<String, Object>) mainValue;
                    Map<String, Object> pkgMap = (Map<String, Object>) value;
                    mergeElements(mainMap, pkgMap);
                    mainData.put(key, mainMap);
                    return;
                }
                if (mainValue instanceof List && value instanceof List) {
                    List<Object> mainList = (List<Object>) mainValue;
                    List<Object> pkgList = (List<Object>) value;
                    // append main list after package list
                    mainData.put(key, Stream.concat(pkgList.stream(), mainList.stream()).toList());
                    return;
                }
                // For non-map/non-list values, keep the main value overwriting the package value
            } else {
                mainData.put(key, value);
            }
        });
    }

    /**
     * Recursively resolves ReplaceObject and RemoveObject instances in the data structure.
     * - RemoveObject: removes the key from its parent map
     * - ReplaceObject: unwraps to its contained object
     */
    @SuppressWarnings("unchecked")
    private static void resolveSpecialObjects(Map<String, Object> data) {
        // First, recursively process nested structures
        data.forEach((key, value) -> {
            if (value instanceof Map) {
                resolveSpecialObjects((Map<String, Object>) value);
            } else if (value instanceof ReplaceObject replaceObj && replaceObj.object() instanceof Map) {
                resolveSpecialObjects((Map<String, Object>) replaceObj.object());
            }
        });

        // Then remove RemoveObject entries and unwrap ReplaceObject entries
        data.entrySet().removeIf(entry -> entry.getValue() instanceof RemoveObject);
        data.replaceAll((key, value) -> value instanceof ReplaceObject replaceObj ? replaceObj.object() : value);
    }

    private static Map<String, Object> excludeHiddenKeys(Map<String, Object> dataMap) {
        // Exclude keys that start with a dot
        return dataMap.entrySet().stream().filter(entry -> !entry.getKey().startsWith(".")).collect(Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (existing, replacement) -> replacement, LinkedHashMap::new));
    }

    static Yaml newYaml(Map<String, Object> variables, Path path, boolean finalPass) {
        LoaderOptions loaderOptions = new LoaderOptions();
        // Throw on duplicate keys; loadIncludeFile will prepend the including
        // file name to help users locate the error in the include chain.
        loaderOptions.setAllowDuplicateKeys(false);
        return new Yaml(new ModelConstructor(loaderOptions, variables, path, finalPass),
                new Representer(new DumperOptions()), new DumperOptions(), loaderOptions, new ModelResolver());
    }
}
