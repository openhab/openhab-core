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

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.model.yaml.internal.util.preprocessor.PreprocessorConfig;
import org.openhab.core.model.yaml.internal.util.preprocessor.PreprocessorContext;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholder.IncludePlaceholder;

/**
 * The {@link VariableProcessor} is responsible for extracting variable definitions from the YAML model and storing
 * them in the preprocessor context for later use in substitutions.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
public class VariableProcessor {
    private final PreprocessorContext context;
    private final RecursiveProcessor recursiveProcessor;
    private final MergeKeyProcessor mergeKeyProcessor;

    public VariableProcessor(PreprocessorContext context, RecursiveProcessor recursiveProcessor,
            MergeKeyProcessor mergeKeyProcessor) {
        this.context = context;
        this.recursiveProcessor = recursiveProcessor;
        this.mergeKeyProcessor = mergeKeyProcessor;
    }

    /**
     * Add special file-related variables
     *
     * These are added early so they're available in variable definitions during the
     * first pass
     * Special variables will override any user-defined variables with the same name
     */
    public void setSpecialVariables() {
        Path absolutePath = context.getAbsolutePath();
        Path fileNamePath = absolutePath.getFileName();
        String fullFileName = fileNamePath != null ? fileNamePath.toString() : "";
        int dotIndex = fullFileName.lastIndexOf(".");
        String fileName = fullFileName;
        String fileExtension = "";
        if (dotIndex > 0) {
            fileName = fullFileName.substring(0, dotIndex);
            fileExtension = fullFileName.substring(dotIndex + 1);
        }
        var parentPath = absolutePath.getParent();
        String directory = parentPath != null ? parentPath.toString() : "";
        Map<String, @Nullable Object> vars = context.getVariables();

        vars.put("OPENHAB_CONF", PreprocessorConfig.getConfigRoot().toString());
        vars.put("OPENHAB_USERDATA", PreprocessorConfig.getUserDataRoot().toString());
        vars.put("__FILE__", absolutePath.toString());
        vars.put("__FILE_NAME__", fileName);
        vars.put("__FILE_EXT__", fileExtension);
        vars.put("__DIRECTORY__", directory);
        vars.put("__DIR__", directory);
    }

    /**
     * Extracts variables from the given map and stores them into the context's variable map.
     *
     * Since variables can reference previously defined variables, we perform incremental resolution
     * while iterating through the variable definitions.
     *
     * @param variablesSection the section of the YAML file containing variable definitions, can be null
     * @param locator the source locator for logging purposes
     * @see PreprocessorConfig#VARIABLES_KEY
     */
    public void extractVariables(@Nullable Object variablesSection, SourceLocator locator) {
        Map<String, @Nullable Object> existingVariables = context.getVariables();

        if (variablesSection instanceof Map<?, ?> variablesMap) {
            Map<Object, @Nullable Object> mergeKeys = new LinkedHashMap<>();

            variablesMap.forEach((key, value) -> {
                if (key instanceof org.openhab.core.model.yaml.internal.util.preprocessor.placeholder.MergeKeyPlaceholder) {
                    mergeKeys.put(key, value);
                    return;
                }

                Object keyObj = recursiveProcessor.process(key);
                if (keyObj == null) {
                    return;
                }

                String keyStr = String.valueOf(keyObj);
                if (!existingVariables.containsKey(keyStr)) {
                    Object resolvedValue = recursiveProcessor.process(value);
                    existingVariables.put(keyStr, resolvedValue);
                }
            });

            if (!mergeKeys.isEmpty()) {
                Map<Object, @Nullable Object> mergedVars = new LinkedHashMap<>(existingVariables);
                Map<Object, @Nullable Object> processedMergeKeys = recursiveProcessor.process(mergeKeys);
                mergedVars.putAll(processedMergeKeys);
                mergeKeyProcessor.resolveMergeKeys(mergedVars);
                existingVariables.clear();
                mergedVars.forEach((k, v) -> existingVariables.put(String.valueOf(k), v));
            }
        } else if (variablesSection instanceof IncludePlaceholder includePlaceholder) {
            Object includedData = recursiveProcessor.process(includePlaceholder, IncludePlaceholder.class);
            extractVariables(includedData, locator);
        } else if (variablesSection != null) {
            var position = locator.findPosition(PreprocessorConfig.VARIABLES_KEY);
            context.getLogger().warn("{}:{} 'variables' is not a map", context.getRelativePath(), position);
        }
    }
}
