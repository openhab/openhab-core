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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.methvin.watcher.hashing.FileHash;
import io.methvin.watcher.hashing.FileHasher;

/**
 * Tests for {@link WatchServiceImpl#contentHash(Path, FileHash)}.
 *
 * @author miloit - Initial contribution
 */
@NonNullByDefault
public class WatchServiceImplContentHashTest {

    @Test
    public void smallFilesGetTheContentHash(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("a.txt");
        Files.writeString(file, "hello");

        FileHash eventHash = FileHash.fromLong(1);

        assertEquals(FileHasher.DEFAULT_FILE_HASHER.hash(file), WatchServiceImpl.contentHash(file, eventHash));
    }

    @Test
    public void sameContentGivesSameHashRegardlessOfModificationTime(@TempDir Path dir) throws IOException {
        Path a = dir.resolve("a.txt");
        Path b = dir.resolve("b.txt");
        Files.writeString(a, "same");
        Files.writeString(b, "same");
        Files.setLastModifiedTime(b, FileTime.fromMillis(1_000_000L));
        FileHash eventHash = FileHash.fromLong(1);

        assertEquals(WatchServiceImpl.contentHash(a, eventHash), WatchServiceImpl.contentHash(b, eventHash));
    }

    @Test
    public void differentContentGivesDifferentHash(@TempDir Path dir) throws IOException {
        Path a = dir.resolve("a.txt");
        Path b = dir.resolve("b.txt");
        Files.writeString(a, "one");
        Files.writeString(b, "two");
        FileHash eventHash = FileHash.fromLong(1);

        assertNotEquals(WatchServiceImpl.contentHash(a, eventHash), WatchServiceImpl.contentHash(b, eventHash));
    }

    @Test
    public void bigFilesUseTheEventHash(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("big.bin");
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw")) {
            raf.setLength(WatchServiceImpl.MAX_CONTENT_HASH_SIZE + 1);
        }
        FileHash eventHash = FileHash.fromLong(42);

        assertSame(eventHash, WatchServiceImpl.contentHash(file, eventHash));
    }

    @Test
    public void missingFilesUseTheEventHash(@TempDir Path dir) {
        FileHash eventHash = FileHash.fromLong(42);

        assertSame(eventHash, WatchServiceImpl.contentHash(dir.resolve("missing.txt"), eventHash));
    }
}
