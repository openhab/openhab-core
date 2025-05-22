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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.representer.Representer;

/**
 * The {@link YamlPreprocessor} is a utility class to load YAML files
 * and preprocess them before they are loaded by {@link YamlModelRepositoryImpl}.
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
    private static final int MAX_INCLUDE_DEPTH = 100; // Stack overflow occurs at 450, depending on system limits

    private static final String VARIABLES_KEY = "variables";
    private static final String PACKAGES_KEY = "packages";

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(YamlPreprocessor.class);

    public static Object load(Path file) throws IOException {
        return load(file, new HashMap<>(), new HashSet<>());
    }

    @SuppressWarnings("unchecked")
    static Object load(Path file, Map<String, String> variables, Set<Path> includeStack) throws IOException {
        LOGGER.debug("Loading file({}): {} with given vars {}", includeStack.size(), file, variables);

        Set<Path> includeStackBranch = new HashSet<>(includeStack);
        if (!includeStackBranch.add(file)) {
            throw new YAMLException("Circular inclusion detected: " + includeStackBranch + " -> " + file);
        }
        if (includeStackBranch.size() > MAX_INCLUDE_DEPTH) {
            throw new YAMLException("Maximum include depth exceeded");
        }

        HashMap<String, String> combinedVars = new HashMap<>(variables);
        // first pass: load the file to extract variables
        Object yamlData = loadYaml(file, variables);
        if (yamlData instanceof Map) {
            if (extractVariables((Map<String, Object>) yamlData, combinedVars)) {
                LOGGER.trace("Combined vars: {}", combinedVars);
            } else {
                LOGGER.warn("{}: 'variables' is not a map", file);
            }
        } else {
            return yamlData;
        }

        addSpecialVariables(combinedVars, file);

        // second pass: load the file again to perform variable substitution
        // and process includes and packages
        Map<String, Object> dataMap = (Map<String, Object>) loadYaml(file, combinedVars);
        dataMap.remove(VARIABLES_KEY); // we've already extracted the variables in the first pass
        LOGGER.trace("Loaded data from {}: {}", file, dataMap);

        dataMap = (Map<String, Object>) processIncludes(file, dataMap, combinedVars, includeStackBranch);
        LOGGER.trace("Loaded includes from {}: {}", file, dataMap);
        Map<String, Object> packages = (Map<String, Object>) dataMap.remove(PACKAGES_KEY);
        dataMap = mergePackages(dataMap, packages);
        LOGGER.trace("Combined data from {}: {}", file, dataMap);
        return dataMap;
    }

    private static Object loadYaml(Path path, Map<String, String> variables) throws IOException {
        try (InputStream inputStream = new FileInputStream(path.toFile())) {
            Yaml yaml = newYaml(variables);
            return yaml.load(inputStream);
        } // let the caller catch the exception and log a message
    }

    /*
     * Extracts variables from the given map.
     *
     * @param variables the map to store the extracted variables.
     * Only variables that are not already present will be added.
     *
     * @return true if the variables were successfully extracted or if they were not present.
     * false if the variables value is not a map.
     */
    private static boolean extractVariables(Map<String, Object> dataMap, HashMap<String, String> variables) {
        Object variablesSection = dataMap.get(VARIABLES_KEY);
        if (variablesSection instanceof Map<?, ?> variablesMap) {
            variablesMap.forEach((key, value) -> {
                if (value instanceof Map) {
                    LOGGER.warn("Value type for variable '{}' cannot be a map", key);
                } else if (value instanceof List) {
                    LOGGER.warn("Value type for variable '{}' cannot be a list", key);
                } else if (value != null && key != null) {
                    variables.putIfAbsent(key.toString(), value.toString());
                }
            });
            return true;
        } else if (variablesSection != null) {
            return false; // not a map, not ok
        }
        return true; // no variables section found, that's ok
    }

    // Add special variables so they get interpolated in the second pass
    private static void addSpecialVariables(Map<String, String> variables, Path file) {
        Path absolutePath = file.toAbsolutePath();
        variables.put("__FILE__", absolutePath.toString());
        String fullFileName = file.getFileName().toString();
        int dotIndex = fullFileName.lastIndexOf(".");
        String fileName = fullFileName;
        String fileExtension = "";
        if (dotIndex > 0) {
            fileName = fullFileName.substring(0, dotIndex);
            fileExtension = fullFileName.substring(dotIndex + 1);
        }
        variables.put("__FILE_NAME__", fileName);
        variables.put("__FILE_EXT__", fileExtension);
        variables.put("__PATH__", absolutePath.getParent().toString());
    }

    /*
     * Process special nodes in the YAML data that correspond to !include.
     * This method is called recursively for nested objects.
     */
    @SuppressWarnings("unchecked")
    private static Object processIncludes(Path file, Object data, Map<String, String> variables,
            Set<Path> includeStack) {
        if (data instanceof IncludeObject includeObject) {
            return loadIncludeFile(file, includeObject, variables, includeStack);
        } else if (data instanceof Map) {
            Map<String, Object> dataMap = (Map<String, Object>) data;
            return dataMap.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            entry -> processIncludes(file, entry.getValue(), variables, includeStack),
                            (existing, replacement) -> replacement, LinkedHashMap::new));
        } else if (data instanceof List) {
            List<Object> dataList = (List<Object>) data;
            return dataList.stream().map(value -> processIncludes(file, value, variables, includeStack)).toList();
        }
        return data;
    }

    private static Object loadIncludeFile(Path file, IncludeObject includeObject, Map<String, String> variables,
            Set<Path> includeStack) {
        Path includeFile = file.resolveSibling(includeObject.fileName());
        Map<String, String> includeVars = new HashMap<>(variables);
        includeVars.putAll(includeObject.vars());
        try {
            return load(includeFile, includeVars, includeStack);
        } catch (IOException e) {
            LOGGER.warn("Error loading include file {}", e.getMessage());
            return Map.of();
        }
    }

    // Recursively merge packages into the main data map
    // if the same key exists in both the main map and the package, the main map value is kept
    @SuppressWarnings("unchecked")
    private static Map<String, Object> mergePackages(Map<String, Object> mainData,
            @Nullable Map<String, Object> packages) {
        if (packages == null) {
            return mainData;
        }
        packages.forEach((packageName, pkg) -> {
            if (pkg instanceof Map) {
                mergeElements(mainData, (Map<String, Object>) pkg);
            } else {
                LOGGER.warn("Package '{}' is not a map: {}", packageName, pkg);
            }
        });
        return mainData;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mergeElements(Map<String, Object> mainData, Map<String, Object> packageData) {
        packageData.forEach((key, value) -> {
            if (mainData.containsKey(key)) {
                Object mainValue = mainData.get(key);
                if (mainValue instanceof Map && value instanceof Map) {
                    Map<String, Object> mainMap = (Map<String, Object>) mainValue;
                    Map<String, Object> pkgMap = (Map<String, Object>) value;
                    mergeElements(mainMap, pkgMap);
                } else if (mainValue instanceof List && value instanceof List) {
                    List<Object> mainList = (List<Object>) mainValue;
                    List<Object> pkgList = (List<Object>) value;
                    mainData.put(key, Stream.concat(mainList.stream(), pkgList.stream()).toList());
                }
                // if the value is not a map or list, keep the main value
            } else {
                mainData.put(key, value);
            }
        });
        return mainData;
    }

    static Yaml newYaml(Map<String, String> variables) {
        return new Yaml(new ModelConstructor(variables), new Representer(new DumperOptions()), new DumperOptions(),
                new ModelResolver());
    }

    public static @Nullable Object getNestedValue(Map<String, Object> data, String... key) {
        Object value = data;
        for (String k : key) {
            if (value instanceof Map<?, ?> map) {
                value = map.get(k);
            } else {
                return null;
            }
        }
        return value;
    }
}
