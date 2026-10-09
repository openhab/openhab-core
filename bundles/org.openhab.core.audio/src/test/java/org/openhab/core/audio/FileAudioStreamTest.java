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
package org.openhab.core.audio;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Files;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.common.Disposable;

/**
 * Unit tests for {@link FileAudioStream}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class FileAudioStreamTest {

    @Test
    public void testTempFileFileAudioStreamDeletesFileOnDispose() throws Exception {
        File tempFile = Files.createTempFile("test-audio-", ".wav").toFile();
        Files.write(tempFile.toPath(), new byte[] { 0, 1, 2, 3 });

        FileAudioStream stream = new FileAudioStream(tempFile, AudioFormat.WAV, true);
        assertInstanceOf(Disposable.class, stream);

        assertTrue(tempFile.exists());
        stream.close();
        // Closing does not delete the temporary file
        assertTrue(tempFile.exists());

        // Disposing deletes the temporary file
        stream.dispose();
        assertFalse(tempFile.exists());
    }

    @Test
    public void testNonTempFileAudioStreamDoesNotDeleteFileOnDispose() throws Exception {
        File tempFile = Files.createTempFile("test-audio-", ".wav").toFile();
        Files.write(tempFile.toPath(), new byte[] { 0, 1, 2, 3 });

        FileAudioStream stream = new FileAudioStream(tempFile, AudioFormat.WAV, false);

        assertTrue(tempFile.exists());
        stream.close();
        assertTrue(tempFile.exists());

        stream.dispose();
        // Disposing a non-temporary file stream should keep the file
        assertTrue(tempFile.exists());

        Files.deleteIfExists(tempFile.toPath());
    }
}
