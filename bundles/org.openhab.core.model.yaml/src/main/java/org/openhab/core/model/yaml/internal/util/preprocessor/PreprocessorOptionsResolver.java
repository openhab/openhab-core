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

import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.model.yaml.internal.util.preprocessor.core.SourceLocator;

/**
 * Small helper that encapsulates logic to determine preprocessor options
 * such as whether to generate the compiled output or allow loading into
 * openHAB. This keeps option parsing/testability out of
 * {@link YamlPreprocessor}.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
final class PreprocessorOptionsResolver {
    private final PreprocessorContext context;
    private final SourceLocator locator;

    private boolean shouldGenerateCompiled = false;
    private boolean shouldAllowLoading = true;

    /**
     * Parses the preprocessor options from the given YAML map, removing the
     * preprocessor section from the map if present.
     *
     * Only the top-level file is checked for preprocessor options;
     * included files will not be able to override these settings.
     *
     * @param yamlMap the YAML map to check for preprocessor options;
     *            this map will be modified by removing the preprocessor section if found
     * @param context the preprocessor context for logging and path information
     * @param locator the source locator for finding positions of options for logging
     */
    PreprocessorOptionsResolver(Map<?, ?> yamlMap, PreprocessorContext context, SourceLocator locator) {
        this.context = context;
        this.locator = locator;
        determinePreprocessorOptions(yamlMap);
    }

    /**
     * Returns true when the resolver is operating on the top-level file (not an include).
     */
    public boolean isTopLevel() {
        return context.getIncludeStack().size() == 1;
    }

    public boolean shouldGenerateCompiled() {
        return shouldGenerateCompiled;
    }

    public boolean shouldAllowLoading() {
        return shouldAllowLoading;
    }

    private void determinePreprocessorOptions(Map<?, ?> yamlMap) {
        // defaults already set on the instance fields
        if (context.getIncludeStack().size() == 1) { // only check preprocessor settings for the top-level file
            Object preprocessorSection = yamlMap.remove(PreprocessorConfig.PREPROCESSOR_KEY);
            this.shouldGenerateCompiled = shouldGenerateResolvedFile(preprocessorSection, this.shouldGenerateCompiled);
            this.shouldAllowLoading = shouldAllowLoading(preprocessorSection, this.shouldAllowLoading);
        }
    }

    private boolean shouldGenerateResolvedFile(@Nullable Object preprocessorSection, boolean defaultValue) {
        return getPreprocessorBoolean(preprocessorSection, PreprocessorConfig.GENERATE_RESOLVED_FILE_KEY, defaultValue);
    }

    private boolean shouldAllowLoading(@Nullable Object preprocessorSection, boolean defaultValue) {
        return getPreprocessorBoolean(preprocessorSection, PreprocessorConfig.LOAD_INTO_OPENHAB_KEY, defaultValue);
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
                var position = locator.findPosition(PreprocessorConfig.PREPROCESSOR_KEY, key);
                context.getLogger().warn("{}:{} '{}.{}' is not a boolean; using default {}", context.getRelativePath(),
                        position, PreprocessorConfig.PREPROCESSOR_KEY, key, defaultValue);
            }
            return defaultValue;
        }

        var position = locator.findPosition(PreprocessorConfig.PREPROCESSOR_KEY);
        context.getLogger().warn("{}:{} '{}' section is not a map; using default {} for '{}'",
                context.getRelativePath(), position, PreprocessorConfig.PREPROCESSOR_KEY, defaultValue, key);
        return defaultValue;
    }
}
