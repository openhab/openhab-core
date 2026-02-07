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

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Context holding the state for a {@link YamlPreprocessor} run.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
public class PreprocessorContext {
    private final Map<String, @Nullable Object> variables;
    private final Map<Object, @Nullable Object> templates;
    private final Set<Path> includeStack;
    private final Consumer<Path> includeCallback;
    private final Path absolutePath;
    private final Path relativePath;
    private final BufferedLogger logger;

    public PreprocessorContext(Path absolutePath, Path relativePath, Map<String, @Nullable Object> variables,
            Set<Path> includeStack, Consumer<Path> includeCallback, BufferedLogger logger) {
        this.absolutePath = absolutePath;
        this.relativePath = relativePath;
        this.variables = new LinkedHashMap<>(variables);
        this.templates = new HashMap<>();
        this.includeStack = new LinkedHashSet<>(includeStack);
        this.includeCallback = includeCallback;
        this.logger = logger;
    }

    public Map<String, @Nullable Object> getVariables() {
        return variables;
    }

    public Map<Object, @Nullable Object> getTemplates() {
        return templates;
    }

    public Set<Path> getIncludeStack() {
        return includeStack;
    }

    public Consumer<Path> getIncludeCallback() {
        return includeCallback;
    }

    public Path getAbsolutePath() {
        return absolutePath;
    }

    public Path getRelativePath() {
        return relativePath;
    }

    public BufferedLogger getLogger() {
        return logger;
    }
}
