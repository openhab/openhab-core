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

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PipedAudioStream}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class PipedAudioStreamTest {

    @Test
    public void testGroupCloseAllowsReaderToDrainAllBufferedData() throws IOException {
        AudioFormat format = new AudioFormat(AudioFormat.CONTAINER_FLAC, AudioFormat.CODEC_FLAC, null, 16, null, 44100L,
                1);
        PipedAudioStream.Group group = PipedAudioStream.newGroup(format);
        PipedAudioStream stream = group.getAudioStreamInGroup();

        byte[] testData = new byte[] { 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };
        group.write(testData);
        // Closing the group (writer) must not close the reader or discard buffered bytes
        group.close();

        byte[] readData = stream.readAllBytes();
        assertArrayEquals(testData, readData);

        stream.close();
    }
}
