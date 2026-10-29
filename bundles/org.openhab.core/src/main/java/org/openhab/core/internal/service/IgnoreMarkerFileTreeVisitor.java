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
package org.openhab.core.internal.service;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.methvin.watcher.visitor.FileTreeVisitor;

/**
 * A {@link FileTreeVisitor} that skips every directory (including its whole subtree) that contains a marker file
 * named {@link #IGNORE_MARKER_FILE}. Skipped directories are neither registered for watching nor hashed.
 *
 * @author miloit - Initial contribution
 */
@NonNullByDefault
public class IgnoreMarkerFileTreeVisitor implements FileTreeVisitor {

    public static final String IGNORE_MARKER_FILE = ".openhab_ignorefilewatch";

    private final Path root;

    /**
     * @param root the base path of the watch service; a marker file directly in it is ignored on purpose, because it
     *            would disable the whole service
     */
    public IgnoreMarkerFileTreeVisitor(Path root) {
        this.root = root;
    }

    @Override
    public void recursiveVisitFiles(Path file, Callback onDirectory, Callback onFile) throws IOException {
        Files.walkFileTree(file, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!dir.equals(root) && Files.exists(dir.resolve(IGNORE_MARKER_FILE))) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                onDirectory.call(dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                onFile.call(path);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
