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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholder.IfPlaceholder;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholder.InterpolablePlaceholder;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholder.MergeKeyPlaceholder;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholder.Placeholder;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholder.SubstitutionPlaceholder;
import org.openhab.core.model.yaml.internal.util.preprocessor.processor.PlaceholderProcessor;

/**
 * The {@link RecursiveProcessor} processes a YAML data tree, resolving any placeholders found
 * using registered handlers for specific placeholder types.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
public class RecursiveProcessor {

    private final Map<Class<? extends Placeholder>, PlaceholderProcessor<?>> handlers = new LinkedHashMap<>();

    /**
     * Registers a handler for a specific placeholder type.
     *
     * @param clazz the placeholder class to handle
     * @param handler the processor that can resolve the placeholder
     */
    public void register(Class<? extends Placeholder> clazz, PlaceholderProcessor<?> handler) {
        handlers.put(clazz, handler);
    }

    /**
     * Default: process the entire tree with all registered handlers.
     * Placeholders are processed if their class matches any registered handler.
     *
     * This is the most common usage, but the other overloads allow for more control
     * and optimization by restricting which handlers are applied.
     *
     * @param data the YAML data tree to process
     * @return the processed data tree with placeholders resolved
     */
    public @Nullable Object process(@Nullable Object data) {
        return processInternal(data, handlers.keySet(), null);
    }

    /**
     * Processes the data tree but only applies handlers for the specified placeholder classes.
     *
     * @param data the YAML data tree to process
     * @param placeholderClass the placeholder class to handle
     * @return the processed data tree with the given placeholders resolved
     */
    public @Nullable Object process(@Nullable Object data, Class<? extends Placeholder> placeholderClass) {
        return processInternal(data, Set.of(placeholderClass), null);
    }

    /**
     * Processes the data tree but only applies handlers for the specified placeholder classes.
     *
     * @param data the YAML data tree to process
     * @param allowedTypes the set of placeholder classes to process
     * @return the processed data tree with the given placeholders resolved
     */
    public @Nullable Object process(@Nullable Object data, Set<Class<? extends Placeholder>> allowedTypes) {
        return processInternal(data, allowedTypes, null);
    }

    /**
     * Processes the data tree but uses the provided override handler for the specified placeholder class
     * instead of the registered handler.
     * This allows for custom processing of specific placeholders without affecting the global handler registry.
     *
     * @param data the YAML data tree to process
     * @param placeholderClass the placeholder class to handle with the override handler
     * @param callback the processor to use for the specified placeholder class
     * @return the processed data tree with the given placeholders resolved using the override handler
     */
    public <T extends Placeholder> @Nullable Object process(@Nullable Object data, Class<T> placeholderClass,
            PlaceholderProcessor<T> callback) {
        return processInternal(data, Set.of(placeholderClass), callback);
    }

    // Map version of the overload for convenience
    public Map<Object, @Nullable Object> process(Map<?, ?> data) {
        return process(data, handlers.keySet());
    }

    @SuppressWarnings("unchecked")
    public Map<Object, @Nullable Object> process(Map<?, ?> data, Set<Class<? extends Placeholder>> allowedTypes) {
        Object processed = processInternal(data, allowedTypes, null);
        if (processed instanceof Map<?, ?> map) {
            return (Map<Object, @Nullable Object>) map;
        }
        throw new IllegalStateException("Expected processed result to be a Map but was: "
                + (processed == null ? "null" : processed.getClass()));
    }

    /**
     * The actual tree traversal.
     */
    private @Nullable Object processInternal(@Nullable Object data, Set<Class<? extends Placeholder>> allowedTypes,
            @Nullable PlaceholderProcessor<?> overrideHandler) {

        if (data == null) {
            return null;
        }

        Class<?> clazz = data.getClass();

        if (data instanceof Placeholder placeholder && allowedTypes.contains(clazz)) {
            // Use the override callback if provided, otherwise look up in registry
            PlaceholderProcessor<?> handler = (overrideHandler != null) ? overrideHandler : handlers.get(clazz);

            if (handler != null) {
                // Resolve placeholder value (arguments) first before processing the placeholder itself
                // So that e.g. !include ${filename} gets the real argument value to process
                if (placeholder instanceof InterpolablePlaceholder interpolable) {
                    Object processedValue;
                    if (interpolable.eagerArgumentProcessing()) {
                        // Eagerly process arguments using all registered handlers
                        processedValue = process(interpolable.value());
                    } else {
                        // Only perform substitutions in arguments
                        // for !if conditions, don't process placeholders (e.g. !include) in the unselected branch
                        processedValue = process(interpolable.value(), SubstitutionPlaceholder.class);
                    }
                    placeholder = (Placeholder) interpolable.withValue(processedValue);
                }

                // Execute and recurse
                Object result = invokeHandler(handler, placeholder);
                return processInternal(result, allowedTypes, overrideHandler);
            }
        }

        if (data instanceof Map<?, ?> map) {
            return resolveMap(map, allowedTypes, overrideHandler);
        }

        if (data instanceof List<?> list) {
            return resolveList(list, allowedTypes, overrideHandler);
        }

        return data;
    }

    @SuppressWarnings("unchecked")
    private @Nullable <T extends Placeholder> Object invokeHandler(PlaceholderProcessor<?> handler,
            Placeholder placeholder) {
        return ((PlaceholderProcessor<T>) handler).process((T) placeholder);
    }

    /**
     * Resolves a map by processing its keys and values, applying placeholder handlers as needed,
     * and handling special cases like merge keys and removal signals.
     *
     * This method ensures that the map is processed efficiently while correctly handling the various placeholder.
     *
     * The method processes each entry in the map, applying the following logic:
     *
     * 1. MergeKey Handling: If the value is null and the key is a MergeKeyPlaceholder,
     * it checks if the original value was a SubstitutionPlaceholder or IfPlaceholder
     * and replaces it with an empty map to allow the merge to be ignored without causing errors.
     *
     * 2. Removal Handling: If the processed key or value is null, or if either is a RemovalSignal,
     * the entry is skipped (effectively removed from the result). Literal null values are preserved.
     *
     * 3. Structural Changes:
     * If the key changes after processing, the entry is added to the result map with the new key.
     * If only the value changes, it updates the value in place if the key is unchanged,
     * or adds it to the result map if a new map is already being built due to previous changes.
     *
     * 4. No Changes: If neither the key nor the value changes, the original entry is retained.
     * If any changes occur during processing, a new map is returned;
     * otherwise, the original map is returned to avoid unnecessary copying.
     *
     * @param rawMap the original map to process
     * @param allowedTypes the set of placeholder classes to process
     * @param override an optional override handler for a specific placeholder class
     * @return the processed map with placeholders resolved, or the original map if no changes were made
     */
    private Object resolveMap(Map<?, ?> rawMap, Set<Class<? extends Placeholder>> allowedTypes,
            @Nullable PlaceholderProcessor<?> override) {
        if (rawMap.isEmpty()) {
            return rawMap;
        }

        @SuppressWarnings("unchecked")
        Map<Object, @Nullable Object> map = (Map<Object, @Nullable Object>) rawMap;

        Map<Object, @Nullable Object> result = null;

        Iterator<Map.Entry<Object, @Nullable Object>> it = map.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<Object, @Nullable Object> entry = it.next();
            Object oldKey = entry.getKey();
            Object oldVal = entry.getValue();

            Object newKey = processInternal(oldKey, allowedTypes, override);
            Object newVal = processInternal(oldVal, allowedTypes, override);

            // 1. MergeKey logic
            if (newVal == null && oldKey instanceof MergeKeyPlaceholder) {
                if (oldVal instanceof SubstitutionPlaceholder || oldVal instanceof IfPlaceholder) {
                    newVal = Collections.emptyMap(); // Map.of() is immutable, beware!
                }
            }

            // 2. The Filter - Dropping null keys or removal signals
            if (newKey == null //
                    || newKey == RemovalSignal.REMOVE //
                    || newVal == RemovalSignal.REMOVE //
                    // Only drop if the original value was non-null to allow literal null values
                    || (newVal == null && oldVal != null)) {

                if (result == null) {
                    result = migrateToNewMap(map, oldKey);
                }
                continue;
            }

            if (!Objects.equals(newKey, oldKey)) {
                // 4. Structural Change (Key change)
                if (result == null) {
                    result = migrateToNewMap(map, oldKey);
                }
                result.put(newKey, newVal);
            } else if (!Objects.equals(newVal, oldVal)) {
                // 5. Value Change (In-place if possible)
                if (result != null) {
                    result.put(newKey, newVal);
                } else {
                    entry.setValue(newVal);
                }
            } else if (result != null) {
                // 6. No Change (Carry over to the new map)
                result.put(newKey, newVal);
            }
        }

        return result != null ? result : map;
    }

    private Map<Object, @Nullable Object> migrateToNewMap(Map<Object, @Nullable Object> original, Object stopKey) {
        Map<Object, @Nullable Object> newMap = new LinkedHashMap<>(original.size());
        for (Map.Entry<Object, @Nullable Object> entry : original.entrySet()) {
            Object k = entry.getKey();
            if (Objects.equals(k, stopKey)) {
                break;
            }
            newMap.put(k, entry.getValue());
        }
        return newMap;
    }

    private Object resolveList(List<?> list, Set<Class<? extends Placeholder>> allowedTypes,
            @Nullable PlaceholderProcessor<?> override) {
        boolean changed = false;
        List<@Nullable Object> result = new ArrayList<>(list.size());

        for (Object oldItem : list) {
            Object newItem = processInternal(oldItem, allowedTypes, override);

            if (!Objects.equals(newItem, oldItem)) {
                changed = true;
            }

            // Strip RemovalSignal and null values from lists (to satisfy removesNullListElements)
            if (newItem != RemovalSignal.REMOVE && newItem != null) {
                result.add(newItem);
            } else {
                changed = true;
            }
        }

        return changed ? result : list;
    }
}
