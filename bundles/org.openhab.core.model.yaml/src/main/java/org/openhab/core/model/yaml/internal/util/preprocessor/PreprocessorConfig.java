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
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.OpenHAB;

/**
 * Static configuration constants and system path resolution for the YAML preprocessor.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
public final class PreprocessorConfig {

    // Special section keys
    public static final String TEMPLATES_KEY = "templates";
    public static final String PREPROCESSOR_KEY = "preprocessor";
    public static final String GENERATE_RESOLVED_FILE_KEY = "generate_resolved_file";
    public static final String LOAD_INTO_OPENHAB_KEY = "load_into_openhab";
    public static final String VARIABLES_KEY = "variables";
    public static final String PACKAGES_KEY = "packages";

    // Preprocessing limits
    public static final int MAX_INCLUDE_DEPTH = 100;

    private PreprocessorConfig() {
        // Static utility class
    }

    /**
     * Returns the absolute, normalized openHAB configuration root path.
     */
    public static Path getConfigRoot() {
        return Objects.requireNonNull(Path.of(OpenHAB.getConfigFolder()).toAbsolutePath().normalize());
    }

    /**
     * Returns the absolute, normalized openHAB userdata root path.
     */
    public static Path getUserDataRoot() {
        return Objects.requireNonNull(Path.of(OpenHAB.getUserDataFolder()).toAbsolutePath().normalize());
    }

    /**
     * Resolves the relative path of an absolute path with respect to the config root.
     * If the path is not under the config root, the absolute path is returned as is.
     *
     * @param absolutePath the absolute path to resolve
     * @return the relative path if under config root, otherwise the original absolute path
     */
    public static Path resolveRelativePath(Path absolutePath) {
        Path configRoot = getConfigRoot();
        return absolutePath.startsWith(configRoot) ? Objects.requireNonNull(configRoot.relativize(absolutePath))
                : absolutePath;
    }

    /**
     * Resolves the output path for a compiled YAML file.
     *
     * @param absolutePath source file's absolute path
     * @param relativePath source file's relative path (from config root)
     * @return the path where the compiled output should be written
     */
    public static Path resolveCompiledOutputPath(Path absolutePath, Path relativePath) {
        Path configRoot = getConfigRoot();
        if (absolutePath.startsWith(configRoot) && relativePath.getNameCount() >= 2) {
            Path elementRoot = configRoot.resolve(relativePath.subpath(0, 1));
            Path outputRoot = elementRoot.resolve("_generated");
            Path absolutePathRelativeToElementRoot = Objects.requireNonNull(elementRoot.relativize(absolutePath));
            return outputRoot.resolve(absolutePathRelativeToElementRoot);
        } else {
            // Fallback: place next to source in a _generated subdirectory
            Path fallbackDir = Objects.requireNonNull(absolutePath.resolveSibling("_generated"));
            return fallbackDir.resolve(absolutePath.getFileName());
        }
    }
}
