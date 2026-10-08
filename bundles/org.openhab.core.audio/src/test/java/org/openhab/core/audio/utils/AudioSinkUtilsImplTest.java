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
package org.openhab.core.audio.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.audio.AudioFormat;

/**
 * Tests for {@link AudioSinkUtilsImpl}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class AudioSinkUtilsImplTest {

    private final AudioSinkUtilsImpl audioSinkUtils = new AudioSinkUtilsImpl();

    @Test
    public void testTransferWithFlushFlushesOutput() throws Exception {
        byte[] inputData = new byte[25000];
        for (int i = 0; i < inputData.length; i++) {
            inputData[i] = (byte) (i % 128);
        }

        AtomicInteger flushCount = new AtomicInteger();
        ByteArrayOutputStream baos = new ByteArrayOutputStream() {
            @Override
            public void flush() throws IOException {
                super.flush();
                flushCount.incrementAndGet();
            }
        };

        ByteArrayInputStream in = new ByteArrayInputStream(inputData);
        audioSinkUtils.transferAndAnalyzeLength(in, baos, AudioFormat.PCM_SIGNED, true);

        assertArrayEquals(inputData, baos.toByteArray());
        assertTrue(flushCount.get() >= 2, "Expected multiple flushes when flush=true, got: " + flushCount.get());
    }

    @Test
    public void testTransferWithoutFlushDoesNotFlushOutput() throws Exception {
        byte[] inputData = new byte[25000];
        for (int i = 0; i < inputData.length; i++) {
            inputData[i] = (byte) (i % 128);
        }

        AtomicInteger flushCount = new AtomicInteger();
        ByteArrayOutputStream baos = new ByteArrayOutputStream() {
            @Override
            public void flush() throws IOException {
                super.flush();
                flushCount.incrementAndGet();
            }
        };

        ByteArrayInputStream in = new ByteArrayInputStream(inputData);
        audioSinkUtils.transferAndAnalyzeLength(in, baos, AudioFormat.PCM_SIGNED, false);

        assertArrayEquals(inputData, baos.toByteArray());
        assertEquals(0, flushCount.get(), "Expected 0 flushes when flush=false");
    }

    @Test
    public void testDefaultOverloadDoesNotFlush() throws Exception {
        byte[] inputData = new byte[25000];
        AtomicInteger flushCount = new AtomicInteger();
        ByteArrayOutputStream baos = new ByteArrayOutputStream() {
            @Override
            public void flush() throws IOException {
                super.flush();
                flushCount.incrementAndGet();
            }
        };

        ByteArrayInputStream in = new ByteArrayInputStream(inputData);
        audioSinkUtils.transferAndAnalyzeLength(in, baos, AudioFormat.PCM_SIGNED);

        assertEquals(0, flushCount.get(), "Expected default 3-arg overload to not flush");
    }
}
