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
package org.openhab.core.model.yaml.internal.util.preprocessor.processor;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.model.yaml.internal.util.preprocessor.PreprocessorContext;
import org.openhab.core.model.yaml.internal.util.preprocessor.YamlPreprocessor;
import org.openhab.core.model.yaml.internal.util.preprocessor.YamlPreprocessor.CacheEntry;
import org.openhab.core.model.yaml.internal.util.preprocessor.placeholder.IncludePlaceholder;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * Processor for resolving {@link IncludePlaceholder} in YAML models
 * into the included file content.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
public class IncludeProcessor implements PlaceholderProcessor<IncludePlaceholder> {

    private final PreprocessorContext context;
    private final ConcurrentHashMap<Path, @Nullable CacheEntry> includeCache;

    public IncludeProcessor(PreprocessorContext context, ConcurrentHashMap<Path, @Nullable CacheEntry> includeCache) {
        this.context = context;
        this.includeCache = includeCache;
    }

    /**
     * Resolves an {@link IncludePlaceholder}, loads the referenced file
     * (recursively following any nested includes), and returns the fully expanded
     * content.
     */
    @Override
    public @Nullable Object process(IncludePlaceholder placeholder) {
        return process(placeholder, null);
    }

    public @Nullable Object process(IncludePlaceholder placeholder, @Nullable Object defaultValue) {
        FragmentUtils.Parameters params = FragmentUtils.parseParameters(placeholder, "file");
        if (params == null) {
            context.getLogger().warn("{} Failed to process !include: invalid parameters", placeholder.sourceLocation());
            return defaultValue;
        }

        Object fileNameObj = params.name();
        String includeFileName = String.valueOf(fileNameObj);
        if (fileNameObj == null || includeFileName.isBlank()) {
            context.getLogger().warn("{} Failed to process !include: missing 'file' parameter",
                    placeholder.sourceLocation());
            return defaultValue;
        }
        Path includeFilePath = Objects.requireNonNull(context.getAbsolutePath().resolveSibling(includeFileName));
        Map<String, @Nullable Object> includeVariables = FragmentUtils.combineInjectedVars(params.varsMap(),
                context.getVariables());
        Path includePathRelative = includeFilePath;

        try {
            YamlPreprocessor includePreprocessor = new YamlPreprocessor(includeFilePath, includeVariables,
                    context.getIncludeStack(), context.getIncludeCallback(), context.getLogger().getLogSession(),
                    includeCache);
            includePathRelative = includePreprocessor.getContext().getRelativePath();
            context.getIncludeCallback().accept(includePreprocessor.getContext().getAbsolutePath());
            return includePreprocessor.load();
        } catch (YAMLException e) {
            String location = "";
            if (e instanceof MarkedYAMLException me && me.getProblemMark() != null) {
                Mark mark = me.getProblemMark();
                location = "%d:%d".formatted(mark.getLine() + 1, mark.getColumn() + 1);
            }
            context.getLogger().warn("{} Failed to process !include '{}' {}:{} {} {}", placeholder.sourceLocation(),
                    includeFileName, includePathRelative, location, e.getClass().getSimpleName(), e.getMessage());
        } catch (IOException e) {
            context.getLogger().warn("{} Failed to process !include '{}' {}: {}", placeholder.sourceLocation(),
                    includeFileName, includePathRelative, getFriendlyMessage(e));
        }
        return null;
    }

    private static @Nullable String getFriendlyMessage(Exception e) {
        if (e instanceof FileSystemException fse) {
            // If the JDK provided a specific reason string, use it
            if (fse.getReason() != null && !fse.getReason().isBlank()) {
                return fse.getReason();
            }

            // Otherwise, use our "Sentence case" class name logic
            String name = e.getClass().getSimpleName().replace("Exception", "");
            String spaced = name.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase().trim();
            return (spaced.isBlank()) ? "File system error"
                    : Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
        }
        return e.getMessage();
    }
}
