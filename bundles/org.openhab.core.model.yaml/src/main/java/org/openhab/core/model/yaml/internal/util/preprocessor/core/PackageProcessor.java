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
package org.openhab.core.model.yaml.internal.util.preprocessor.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.model.yaml.internal.util.preprocessor.PreprocessorContext;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholder.IncludePlaceholder;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholder.InsertPlaceholder;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholder.ReplacePlaceholder;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholder.SubstitutionPlaceholder;
import org.openhab.core.model.yaml.internal.util.preprocessor.processor.FragmentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processor for handling 'packages' in YAML models.
 *
 * Merges package definitions into the main data structure, injecting the package ID
 * into any included or inserted fragments.
 *
 * This is slightly different to the other processors as it operates on the overall
 * data structure rather than merely performing resolutions of a particular placeholder.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
public class PackageProcessor {
    private static final Logger RAW_LOGGER = LoggerFactory.getLogger(PackageProcessor.class);
    private static final String PACKAGES_KEY = "packages";
    private static final String PACKAGE_ID_VAR = "package_id";

    private final PreprocessorContext context;
    private final SourceLocator sourceLocator;
    private final RecursiveProcessor recursiveProcessor;

    public PackageProcessor(PreprocessorContext context, SourceLocator sourceLocator,
            RecursiveProcessor recursiveProcessor) {
        this.context = context;
        this.sourceLocator = sourceLocator;
        this.recursiveProcessor = recursiveProcessor;
    }

    /**
     * Convenience wrapper that accepts the raw 'packages' section value and
     * applies merging if it's a map, otherwise logs the same warning used by
     * the preprocessor.
     *
     * @param yamlMap the root YAML map to merge packages into
     * @param packagesObj the raw 'packages' section value to process and merge, or null if not present
     */
    public void mergePackages(Map<?, ?> yamlMap, @Nullable Object packagesObj) {
        if (packagesObj instanceof Map<?, ?> packagesMap) {
            mergePackages(yamlMap, packagesMap);
            context.getLogger().debug("Merged packages into data in {}: {}", context.getAbsolutePath(), yamlMap);
        } else if (packagesObj != null) {
            var position = sourceLocator.findPosition(PACKAGES_KEY);
            context.getLogger().warn("{}:{} The 'packages' section is not a map", context.getRelativePath(), position);
        }
    }

    /**
     * Deep merge packages map into the main data map
     */
    private void mergePackages(Map<?, ?> mainData, Map<?, ?> packages) {
        packages.forEach((pkgKey, pkg) -> {
            String packageId = String.valueOf(pkgKey);
            Object processedPkg = Objects.requireNonNull(pkg);

            Object pkgWithId = injectPackageId(processedPkg, packageId);
            Object resolvedPkg = recursiveProcessor.process(pkgWithId,
                    Set.of(SubstitutionPlaceholder.class, IncludePlaceholder.class, InsertPlaceholder.class));

            resolvedPkg = stripEmptyMapsAndLists(resolvedPkg);
            if (!(resolvedPkg instanceof Map<?, ?> packageMap)) {
                var position = sourceLocator.findPosition(PACKAGES_KEY, packageId);
                context.getLogger().warn("{}:{} package '{}' resolved to {} instead of a Map",
                        context.getRelativePath(), position, packageId,
                        resolvedPkg == null ? "null" : resolvedPkg.getClass().getSimpleName());
                return;
            }

            context.getLogger().debug("Merging package '{}' {} into main data: {}", packageId, packageMap, mainData);
            mergeElements(packageId, mainData, packageMap);
        });
    }

    /**
     * Recursively inject the package ID into IncludePlaceholder and InsertPlaceholder vars.
     */
    private static @Nullable Object injectPackageId(@Nullable Object data, String packageId) {
        RAW_LOGGER.debug("Injecting package_id='{}' into data: {}", packageId, data);
        if (data instanceof IncludePlaceholder placeholder) {
            FragmentUtils.Parameters params = FragmentUtils.parseParameters(placeholder, "file");
            if (params == null) {
                return null;
            }

            Map<String, @Nullable Object> defaultId = Map.of(PACKAGE_ID_VAR, packageId);
            Map<String, @Nullable Object> newVars = FragmentUtils.combineInjectedVars(params.varsMap(), defaultId);
            Object newValue = FragmentUtils.toValue(params.name(), newVars, "file");
            return placeholder.withValue(newValue);
        } else if (data instanceof InsertPlaceholder placeholder) {
            FragmentUtils.Parameters params = FragmentUtils.parseParameters(placeholder, "template");
            if (params == null) {
                return null;
            }

            Map<String, @Nullable Object> defaultId = Map.of(PACKAGE_ID_VAR, packageId);
            Map<String, @Nullable Object> newVars = FragmentUtils.combineInjectedVars(params.varsMap(), defaultId);
            Object newValue = FragmentUtils.toValue(params.name(), newVars, "template");
            return placeholder.withValue(newValue);
        } else if (data instanceof Map<?, ?> dataMap) {
            return dataMap.entrySet().stream().collect(LinkedHashMap::new,
                    (m, v) -> m.put(v.getKey(), injectPackageId(v.getValue(), packageId)), LinkedHashMap::putAll);
        } else if (data instanceof List<?> dataList) {
            return dataList.stream().map(item -> injectPackageId(Objects.requireNonNull(item), packageId)).toList();
        } else {
            return data;
        }
    }

    private void mergeElements(String packageId, Map<?, ?> mainData, Map<?, ?> packageData) {
        packageData.forEach((packageKey, packageValue) -> {
            if (mainData.containsKey(packageKey)) {
                Object mainValue = mainData.get(packageKey);
                if (mainValue instanceof ReplacePlaceholder || mainValue instanceof SubstitutionPlaceholder) {
                    return; // Ignore the package value - we'll process these later
                }
                if (mainValue instanceof Map<?, ?> mainValueMap) {
                    if (packageValue instanceof Map<?, ?> packageValueMap) {
                        mergeElements(packageId, mainValueMap, packageValueMap);
                        @SuppressWarnings("unchecked")
                        Map<@Nullable Object, Object> rawMainData = (Map<@Nullable Object, Object>) mainData;
                        rawMainData.put(packageKey, mainValueMap);
                        return;
                    } else {
                        // Type mismatch - keep main value
                        var position = sourceLocator.findPosition(PACKAGES_KEY, packageId);
                        context.getLogger().warn(
                                "{}:{} Type mismatch when merging package ID '{}' for key '{}': main is Map, package is {}; keeping main value",
                                context.getRelativePath(), position, packageId, packageKey,
                                packageValue == null ? "null" : packageValue.getClass().getSimpleName());
                        return;
                    }
                }
                if (mainValue instanceof List<?> mainValueList) {
                    if (packageValue instanceof List<?> pkgValueList) {
                        @SuppressWarnings("unchecked")
                        Map<@Nullable Object, Object> rawMainData = (Map<@Nullable Object, Object>) mainData;
                        rawMainData.put(packageKey,
                                Stream.concat(pkgValueList.stream(), mainValueList.stream()).toList());
                        return;
                    } else {
                        // Type mismatch - keep main value
                        var position = sourceLocator.findPosition(PACKAGES_KEY, packageId);
                        context.getLogger().warn(
                                "{}:{} Type mismatch when merging package ID '{}' for key '{}': main is List, package is {}; keeping main value",
                                context.getRelativePath(), position, packageId, packageKey,
                                packageValue == null ? "null" : packageValue.getClass().getSimpleName());
                        return;
                    }
                }
                // For non-map/non-list values, keep the main value ignoring the package value
            } else {
                @SuppressWarnings("unchecked")
                Map<@Nullable Object, @Nullable Object> rawMainData = (Map<@Nullable Object, @Nullable Object>) mainData;
                rawMainData.put(packageKey, packageValue);
            }
        });
    }

    private static @Nullable Object stripEmptyMapsAndLists(@Nullable Object data) {
        if (data == null || data instanceof String s && s.isBlank()) {
            return null;
        }
        if (data instanceof Map<?, ?> map) {
            var result = new LinkedHashMap<Object, Object>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                Object key = e.getKey();
                Object value = stripEmptyMapsAndLists(e.getValue());
                if (value != null) {
                    result.put(key, value);
                }
            }
            return result.isEmpty() ? null : result;
        } else if (data instanceof List<?> list) {
            var result = new java.util.ArrayList<Object>(list.size());
            for (Object item : list) {
                Object value = stripEmptyMapsAndLists(item);
                if (value != null) {
                    result.add(value);
                }
            }
            return result.isEmpty() ? null : result;
        }
        return data;
    }
}
