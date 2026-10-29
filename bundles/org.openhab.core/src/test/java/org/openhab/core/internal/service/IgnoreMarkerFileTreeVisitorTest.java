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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link IgnoreMarkerFileTreeVisitor}.
 *
 * @author miloit - Initial contribution
 */
@NonNullByDefault
public class IgnoreMarkerFileTreeVisitorTest {

    private static void touch(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, "x");
    }

    private static Set<String> visit(Path base, Path start, boolean directories) throws IOException {
        Set<String> result = new TreeSet<>();
        new IgnoreMarkerFileTreeVisitor(base).recursiveVisitFiles(start, dir -> {
            if (directories) {
                result.add(base.relativize(dir).toString());
            }
        }, file -> {
            if (!directories) {
                result.add(base.relativize(file).toString());
            }
        });
        return result;
    }

    @Test
    public void directoriesWithMarkerAreSkippedIncludingSubtree(@TempDir Path base) throws IOException {
        touch(base.resolve("a/f1.txt"));
        touch(base.resolve("b/" + IgnoreMarkerFileTreeVisitor.IGNORE_MARKER_FILE));
        touch(base.resolve("b/f2.txt"));
        touch(base.resolve("b/sub/f3.txt"));
        touch(base.resolve("c/sub/f4.txt"));

        assertEquals(Set.of("", "a", "c", "c/sub"), visit(base, base, true));
        assertEquals(Set.of("a/f1.txt", "c/sub/f4.txt"), visit(base, base, false));
    }

    @Test
    public void markerInBasePathIsIgnored(@TempDir Path base) throws IOException {
        touch(base.resolve(IgnoreMarkerFileTreeVisitor.IGNORE_MARKER_FILE));
        touch(base.resolve("a/f1.txt"));

        assertEquals(Set.of("", "a"), visit(base, base, true));
        assertEquals(Set.of(IgnoreMarkerFileTreeVisitor.IGNORE_MARKER_FILE, "a/f1.txt"), visit(base, base, false));
    }

    @Test
    public void markerIsHonoredWhenVisitingASubdirectory(@TempDir Path base) throws IOException {
        // e.g. a directory that is created later and registered on its own
        touch(base.resolve("b/" + IgnoreMarkerFileTreeVisitor.IGNORE_MARKER_FILE));
        touch(base.resolve("b/f2.txt"));

        assertEquals(Set.of(), visit(base, base.resolve("b"), true));
        assertEquals(Set.of(), visit(base, base.resolve("b"), false));
    }
}
