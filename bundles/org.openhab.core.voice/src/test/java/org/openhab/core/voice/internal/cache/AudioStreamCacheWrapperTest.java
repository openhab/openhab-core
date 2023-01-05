/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.voice.internal.cache;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.voice.TTSException;

/**
 * Test the wrapper stream in the cache system
 *
 * @author Gwendal Roulleau - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
public class AudioStreamCacheWrapperTest {

    private @Mock @NonNullByDefault({}) TTSResult ttsResultMock;

    /**
     * Test the read() method
     *
     * @throws IOException
     */
    @Test
    public void cacheWrapperStreamTest() throws IOException {
        AudioStreamSupplier mockedAudioStreamSupplier = Mockito.mock(AudioStreamSupplier.class);
        when(ttsResultMock.read(0, 1)).thenReturn(new byte[] { 1 });
        when(ttsResultMock.read(1, 1)).thenReturn(new byte[] { 2 });
        when(ttsResultMock.read(2, 1)).thenReturn(new byte[] { 3 });
        when(ttsResultMock.read(3, 1)).thenReturn(new byte[0]);

        try (AudioStreamCacheWrapper audioStreamCacheWrapper = new AudioStreamCacheWrapper(ttsResultMock,
                mockedAudioStreamSupplier)) {
            assertEquals(1, audioStreamCacheWrapper.read());
            assertEquals(2, audioStreamCacheWrapper.read());
            assertEquals(3, audioStreamCacheWrapper.read());
            assertEquals(-1, audioStreamCacheWrapper.read());
        }

        verify(ttsResultMock, times(4)).read(anyInt(), anyInt());
        verify(ttsResultMock).closeAudioStreamClient();
        verifyNoMoreInteractions(ttsResultMock);
    }

    /**
     * Test the read by batch method
     *
     * @throws IOException
     */
    @Test
    public void cacheWrapperStreamReadBunchTest() throws IOException {
        when(ttsResultMock.read(anyInt(), anyInt())).thenReturn(new byte[] { 1 }, new byte[] { 2, 3 }, new byte[0]);

        AudioStreamSupplier mockedAudioStreamSupplier = Mockito.mock(AudioStreamSupplier.class);
        try (AudioStreamCacheWrapper audioStreamCacheWrapper = new AudioStreamCacheWrapper(ttsResultMock,
                mockedAudioStreamSupplier)) {
            assertArrayEquals(new byte[] { 1, 2, 3 }, audioStreamCacheWrapper.readAllBytes());
        }
        verify(ttsResultMock, times(3)).read(anyInt(), anyInt());
        verify(ttsResultMock).closeAudioStreamClient();
        verifyNoMoreInteractions(ttsResultMock);
    }

    /**
     * Read two bytes from the cached TTSResult, then failed and get next bytes from the fallback mechanism
     *
     * @throws IOException
     * @throws TTSException
     */
    @Test
    public void fallbackTest() throws IOException, TTSException {
        // the TTS result will be the first stream read
        // it will read two byte then fail with IOException
        when(ttsResultMock.read(anyInt(), anyInt())).thenReturn(new byte[] { 1 }, new byte[] { 2 })
                .thenThrow(new IOException());

        // this audiostream will be the fallback, the third and fourth bytes will be read from it
        AudioStream mockedAudioStream = Mockito.mock(AudioStream.class);
        when(mockedAudioStream.read()).thenReturn(3, 4);
        AudioStreamSupplier mockedAudioStreamSupplier = Mockito.mock(AudioStreamSupplier.class);
        when(mockedAudioStreamSupplier.fallBackDirectResolution()).thenReturn(mockedAudioStream);

        try (AudioStreamCacheWrapper audioStreamCacheWrapper = new AudioStreamCacheWrapper(ttsResultMock,
                mockedAudioStreamSupplier)) {
            assertEquals((byte) 1, audioStreamCacheWrapper.read());
            assertEquals((byte) 2, audioStreamCacheWrapper.read());
            assertEquals((byte) 3, audioStreamCacheWrapper.read());
            assertEquals((byte) 4, audioStreamCacheWrapper.read());

            // the fallback stream has two bytes skipped, because it has been read from the TTSResult
            verify(mockedAudioStream).skip(2);
        }
    }
}
