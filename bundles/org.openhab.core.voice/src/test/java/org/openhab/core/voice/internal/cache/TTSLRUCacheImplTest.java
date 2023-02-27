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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.OpenHAB;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.openhab.core.test.storage.VolatileStorageService;
import org.openhab.core.voice.TTSException;
import org.openhab.core.voice.Voice;

/**
 * Test the TTS cache system
 *
 * @author Gwendal Roulleau - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class TTSLRUCacheImplTest {

    private @TempDir @NonNullByDefault({}) Path tempDir;

    private @NonNullByDefault({}) @Mock Voice voiceMock;

    private @Mock @NonNullByDefault({}) CachedTTSService ttsServiceMock;

    private @Mock @NonNullByDefault({}) AudioStream audioStreamMock;

    private @NonNullByDefault({}) Storage<AudioFormatInfo> storage;
    private @NonNullByDefault({}) StorageService storageService;

    @BeforeEach
    public void init() {
        System.setProperty(OpenHAB.USERDATA_DIR_PROG_ARGUMENT, tempDir.toString());
        storageService = new VolatileStorageService();
        this.storage = storageService.getStorage(TTSLRUCacheImpl.VOICE_TTS_CACHE_PID);
    }

    private TTSLRUCacheImpl createTTSCache(long size) throws IOException {
        Map<String, Object> config = new HashMap<>();
        config.put(TTSLRUCacheImpl.CONFIG_CACHE_SIZE_TTS, size);
        config.put(TTSLRUCacheImpl.CONFIG_ENABLE_CACHE_TTS, true);
        TTSLRUCacheImpl voiceLRUCache = new TTSLRUCacheImpl(storageService, config);
        return voiceLRUCache;
    }

    @Test
    public void getCacheMissAndTwoHitAndTTsIsCalledOnlyOnce() throws TTSException, IOException {
        when(ttsServiceMock.getCacheKey("text", voiceMock, AudioFormat.MP3)).thenReturn("filename1");
        when(ttsServiceMock.synthesizeForCache("text", voiceMock, AudioFormat.MP3)).thenReturn(audioStreamMock);
        when(audioStreamMock.getFormat()).thenReturn(AudioFormat.MP3);
        // In this test the audio stream will return two bytes of data, then an empty stream so signal its end :
        when(audioStreamMock.readNBytes(any(Integer.class))).thenReturn(new byte[2], new byte[0]);

        TTSLRUCacheImpl voiceLRUCache = createTTSCache(1000);

        // first cache miss
        AudioStream ttsResultStream = voiceLRUCache.get(ttsServiceMock, "text", voiceMock, AudioFormat.MP3);
        assertEquals(AudioFormat.MP3.getCodec(), ttsResultStream.getFormat().getCodec());
        assertArrayEquals(new byte[2], ttsResultStream.readAllBytes());
        ttsResultStream.close();

        // then cache hit
        ttsResultStream = voiceLRUCache.get(ttsServiceMock, "text", voiceMock, AudioFormat.MP3);
        assertEquals(AudioFormat.MP3.getCodec(), ttsResultStream.getFormat().getCodec());
        assertArrayEquals(new byte[2], ttsResultStream.readAllBytes());
        ttsResultStream.close();

        // then cache hit
        ttsResultStream = voiceLRUCache.get(ttsServiceMock, "text", voiceMock, AudioFormat.MP3);
        // force supplier resolution with a "getFormat" --> won't be called
        assertEquals(AudioFormat.MP3.getCodec(), ttsResultStream.getFormat().getCodec());
        assertArrayEquals(new byte[2], ttsResultStream.readAllBytes());
        ttsResultStream.close();

        // even with three call to get and getFormat, the TTS service and the underlying stream were called
        // only once :
        verify(ttsServiceMock, times(1)).synthesizeForCache("text", voiceMock, AudioFormat.MP3);
        verify(audioStreamMock, times(1)).getFormat();
        // this is called twice because the second call respond with zero and signal the end of stream
        verify(audioStreamMock, times(2)).readNBytes(any(Integer.class));
        // This is called every time to compute the key to search in cache :
        verify(ttsServiceMock, times(3)).getCacheKey("text", voiceMock, AudioFormat.MP3);
    }

    @Test
    public void loadTTSResultsFromCacheDirectory() throws IOException, TTSException {

        // prepare cache directory
        Path cacheDirectory = tempDir.resolve("cache").resolve(TTSLRUCacheImpl.VOICE_TTS_CACHE_PID);
        Files.createDirectories(cacheDirectory);

        // prepare some files
        String key1 = ttsServiceMock.getClass().getSimpleName() + "_" + "filesound1";
        File soundFile1 = cacheDirectory.resolve(key1).toFile();
        storage.put(key1, new AudioFormatInfo("text", null, 42, 16, 16000L, 1, "MP3", null));
        try (FileWriter soundFile1Writer = new FileWriter(soundFile1)) {
            soundFile1Writer.write("falsedata");
        }

        // prepare some files
        String key2 = ttsServiceMock.getClass().getSimpleName() + "_" + "filesound2";
        File soundFile2 = cacheDirectory.resolve(key2).toFile();
        storage.put(key2, new AudioFormatInfo("text2", null, 42, 16, 16000L, 2, "MP3", null));
        try (FileWriter soundFile2Writer = new FileWriter(soundFile2)) {
            soundFile2Writer.write("falsedata");
        }

        // create a LRU cache that will use the above data
        TTSLRUCacheImpl lruCache = createTTSCache(20);

        // prepare fake key from tts
        when(ttsServiceMock.getCacheKey("text", voiceMock, AudioFormat.WAV)).thenReturn("filesound1");
        when(ttsServiceMock.getCacheKey("text2", voiceMock, AudioFormat.WAV)).thenReturn("filesound2");

        AudioStream ttsResult1 = lruCache.get(ttsServiceMock, "text", voiceMock, AudioFormat.WAV);
        assertNotNull(ttsResult1);
        assertEquals(1, ttsResult1.getFormat().getChannels());

        AudioStream ttsResult2 = lruCache.get(ttsServiceMock, "text2", voiceMock, AudioFormat.WAV);
        assertNotNull(ttsResult2);
        assertEquals(2, ttsResult2.getFormat().getChannels());

        // The tts service wasn't called because all data was found in cache :
        verifyNoMoreInteractions(ttsServiceMock);
    }
}
