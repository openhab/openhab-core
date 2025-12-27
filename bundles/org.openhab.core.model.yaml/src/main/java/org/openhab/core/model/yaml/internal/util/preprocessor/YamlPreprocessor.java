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

    public static Object load(Path file, Consumer<Path> includeCallback) throws IOException {
        try {
            return load(file, new HashMap<>(), new HashSet<>(), includeCallback);
        } catch (YAMLException e) {
            // rethrow as IOException so the caller has a simpler error handling and avoids dependency on SnakeYAML
            throw new IOException(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    static Object load(Path file, Map<String, Object> variables, Set<Path> includeStack, Consumer<Path> includeCallback)
            throws IOException, YAMLException {
        LOGGER.debug("Loading file({}): {} with given vars {}", includeStack.size(), file, variables);

        Set<Path> includeStackBranch = new HashSet<>(includeStack);
        if (!includeStackBranch.add(file)) {
            String includeStackChain = includeStackBranch.stream().map(Path::toString)
                    .collect(Collectors.joining(" -> "));
            throw new YAMLException("Circular inclusion detected: " + includeStackChain + " -> " + file);
        }
        if (includeStackBranch.size() > MAX_INCLUDE_DEPTH) {
            throw new YAMLException("Maximum include depth (" + MAX_INCLUDE_DEPTH + ") exceeded");
        }

        HashMap<String, Object> combinedVars = new HashMap<>(variables);
        // first pass: load the file to extract variables
        Object yamlData = loadYaml(file, variables, false);
        if (yamlData instanceof Map) {
            if (extractVariables((Map<String, Object>) yamlData, combinedVars)) {
                LOGGER.trace("Combined vars: {}", combinedVars);
            } else {
                LOGGER.warn("{}: 'variables' is not a map", file);
            }
        } else {
            return yamlData;
        }

        // Call this after extracting user variables so that special variables supersede them
        addSpecialVariables(combinedVars, file);

        // Note: The YAML file must be loaded twice.
        // The first pass above extracts user-defined variables,
        // while the second pass below performs variable interpolation.
        // This cannot be avoided, because SnakeYAML executes interpolation during the construction phase itself.
        // Once the object graph is built, substitutions cannot be applied retroactively,
        // so a full reload with the resolved variables is required.
        Map<String, Object> dataMap = (Map<String, Object>) loadYaml(file, combinedVars, true);
        dataMap.remove(VARIABLES_KEY); // we've already extracted the variables in the first pass
        LOGGER.debug("Loaded data from {}: {}", file, dataMap);

        // Extract packages before processing includes so we can handle them separately
        Object packagesObj = dataMap.remove(PACKAGES_KEY);

        // Process includes in everything except packages
        dataMap = (Map<String, Object>) processIncludes(file, dataMap, combinedVars, includeStackBranch,
                includeCallback);
        LOGGER.debug("Loaded includes from {}: {}", file, dataMap);

        // Process packages separately - this allows us to inject the package ID before processing includes
        if (packagesObj instanceof Map<?, ?> packages) {
            mergePackages(file, dataMap, (Map<String, Object>) packages, combinedVars, includeStackBranch,
                    includeCallback);
        } else if (packagesObj != null) {
            LOGGER.warn("{}: The 'packages' section is not a map", file);
        }

        LOGGER.debug("Combined data from {}: {}", file, dataMap);

        dataMap = excludeHiddenKeys(dataMap);
        return dataMap;
    }

    private static Object loadYaml(Path path, Map<String, Object> variables, boolean finalPass) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            Yaml yaml = newYaml(variables, path, finalPass);
            return yaml.load(inputStream);
        } // let the caller catch the exception and log a message
    }

    /**
     * Extracts variables from the given map.
     *
     * @param variables the map to store the extracted variables.
     *            Only variables that are not already present will be added.
     *
     * @return true if the variables were successfully extracted or if they were not present.
     *         false if the variables value is not a map.
     */
    private static boolean extractVariables(Map<String, Object> dataMap, HashMap<String, Object> variables) {
        Object variablesSection = dataMap.get(VARIABLES_KEY);
        if (variablesSection instanceof Map<?, ?> variablesMap) {
            variablesMap.forEach((key, value) -> {
                if (key == null) {
                    LOGGER.warn("Encountered variable with null key in '{}' section; value '{}' will be ignored",
                            VARIABLES_KEY, value);
                } else {
                    // Treat null values as empty strings so variables defined without a value
                    // (e.g., `empty_value:`) are recognized and can be used with filters like `default()`.
                    Object normalized = value == null ? "" : value;
                    variables.putIfAbsent(key.toString(), normalized);
                }
            });
            return true;
        } else if (variablesSection != null) {
            return false; // not a map, not ok
        }
        return true; // no variables section found, that's ok
    }

    // Add special variables so they get interpolated in the second pass
    // Special variables will override any user-defined variables with the same name
    private static void addSpecialVariables(Map<String, Object> variables, Path file) {
        Path absolutePath = file.toAbsolutePath();
        variables.put("__FILE__", absolutePath.toString());
        Path fileNamePath = file.getFileName();
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
    }

    /**
     * Process special nodes in the YAML data that correspond to !include.
     * This method is called recursively for nested objects.
     */
    @SuppressWarnings("unchecked")
    private static Object processIncludes(Path file, Object data, Map<String, Object> variables, Set<Path> includeStack,
            Consumer<Path> includeCallback) {
        if (data instanceof IncludeObject includeObject) {
            return loadIncludeFile(file, includeObject, variables, includeStack, includeCallback);
        } else if (data instanceof Map) {
            Map<String, Object> dataMap = (Map<String, Object>) data;
            return dataMap.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            entry -> processIncludes(file, entry.getValue(), variables, includeStack, includeCallback),
                            (existing, replacement) -> replacement, LinkedHashMap::new));
        } else if (data instanceof List) {
            List<Object> dataList = (List<Object>) data;
            return dataList.stream()
                    .map(value -> processIncludes(file, value, variables, includeStack, includeCallback)).toList();
        }
        return data;
    }

    // Load the included file (recursively) and return its content
    private static Object loadIncludeFile(Path file, IncludeObject includeObject, Map<String, Object> variables,
            Set<Path> includeStack, Consumer<Path> includeCallback) {
        Path includeFile = file.resolveSibling(includeObject.fileName());
        Map<String, Object> includeVars = new HashMap<>(variables);
        includeVars.putAll(includeObject.vars());
        try {
            Object loadedFile = load(includeFile, includeVars, includeStack, includeCallback);
            includeCallback.accept(includeFile);
            return loadedFile;
        } catch (IOException | YAMLException e) {
            // Only wrap the exception if it's not already an "Error loading include file" message
            // to avoid repeating the wrapper message in nested includes
            String errorMessage = e.getMessage();
            if (errorMessage != null) {
                if (errorMessage.startsWith("Error loading include file")) {
                    throw new YAMLException(errorMessage, e);
                }
                errorMessage = ": " + errorMessage;
            } else {
                errorMessage = "";
            }

            // Wrap the exception to indicate where the error occurred
            throw new YAMLException("Error loading include file '" + includeObject.fileName() + "' (included from '"
                    + file + "')" + errorMessage, e);
        }
    }

    // Recursively merge packages into the main data map
    // if the same key exists in both the main map and the package, the main map value is kept
    @SuppressWarnings("unchecked")
    private static void mergePackages(Path file, Map<String, Object> mainData, Map<String, Object> packages,
            Map<String, Object> variables, Set<Path> includeStack, Consumer<Path> includeCallback) {
        packages.forEach((packageId, pkg) -> {
            Object processedPkg = pkg;

            // If the package is an IncludeObject, inject the ID first, then process it
            if (pkg instanceof IncludeObject includeObj) {
                Map<String, Object> newVars = new HashMap<>(includeObj.vars());
                newVars.putIfAbsent(PACKAGE_ID_VAR, packageId);
                IncludeObject pkgWithId = new IncludeObject(includeObj.fileName(), newVars);
                processedPkg = processIncludes(file, pkgWithId, variables, includeStack, includeCallback);
            }

            if (processedPkg instanceof Map) {
                Map<String, Object> pkgMap = (Map<String, Object>) processedPkg;
                // Also inject ID into any nested IncludeObjects within the package
                Map<String, Object> pkgWithId = injectPackageId(pkgMap, packageId);
                // Process any remaining includes after injection
                pkgWithId = (Map<String, Object>) processIncludes(file, pkgWithId, variables, includeStack,
                        includeCallback);
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
     * This adds an id variable with the package name to all includes within the package.
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
