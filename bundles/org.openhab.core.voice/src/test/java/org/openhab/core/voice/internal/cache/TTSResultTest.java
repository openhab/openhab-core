/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

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
import org.openhab.core.voice.TTSException;
import org.openhab.core.voice.Voice;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Test the cache system
 *
 * @author Gwendal Roulleau - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class TTSResultTest {

    @TempDir
    @NonNullByDefault({})
    Path tempDir;

    @Mock
    @NonNullByDefault({})
    TTSCachedService ttsServiceMock;

    @Mock
    @NonNullByDefault({})
    Voice voiceMock;

    @BeforeEach
    public void init() {
        System.setProperty(OpenHAB.USERDATA_DIR_PROG_ARGUMENT, tempDir.toString());
    }

    public static class FakeAudioStream extends AudioStream {

        ByteArrayInputStream innerInputStream;
        private boolean closed = false;

        public FakeAudioStream(byte[] byteToReturn) {
            innerInputStream = new ByteArrayInputStream(byteToReturn);
        }

        @Override
        public AudioFormat getFormat() {
            return AudioFormat.MP3;
        }

        @Override
        public int read() throws IOException {
            return innerInputStream.read();
        }

        @Override
        public void close() throws IOException {
            this.closed = true;
            innerInputStream.close();
        }

        public boolean isClosed() {
            return closed;
        }
    }

    /**
     * Get twice the AudioStream with only one call.
     *
     * @throws TTSException
     * @throws IOException
     */
    @Test
    public void getAudioStreamTwiceWithOnlyOneCallToTheTTSAndCompare() throws TTSException, IOException {
        AudioStream fakeAudioStream = new FakeAudioStream("This is a false string to simulate some data".getBytes());
        AudioStreamSupplier supplier = new AudioStreamSupplier(ttsServiceMock, "text", voiceMock, AudioFormat.MP3);
        when(ttsServiceMock.synthesizeForCache("text", voiceMock, AudioFormat.MP3)).thenReturn(fakeAudioStream);

        // broken fallback supplier to ensure that the service will exclusively use the main cache :
        AudioStreamSupplier fallbackSupplierBroken = new AudioStreamSupplier(ttsServiceMock, "WRONG DATA", voiceMock,
                AudioFormat.MP3);

        TTSResult ttsResult = new TTSResult(tempDir, "key1", supplier);

        // get audiostream wrapped by the cache system
        AudioStream actualAudioStream = ttsResult.getAudioStreamClient(fallbackSupplierBroken);
        String actuallyRead = new String(actualAudioStream.readAllBytes(), StandardCharsets.UTF_8);
        actualAudioStream.close();
        // ensure that the data are not corrupted
        assertEquals("This is a false string to simulate some data", actuallyRead);

        // get again the audiostream wrapped by the cache system
        actualAudioStream = ttsResult.getAudioStreamClient(fallbackSupplierBroken);
        actuallyRead = new String(actualAudioStream.readAllBytes(), StandardCharsets.UTF_8);
        actualAudioStream.close();
        // ensure that the data are not corrupted
        assertEquals("This is a false string to simulate some data", actuallyRead);

        // Ensure the TTS service was called only once :
        verify(ttsServiceMock, times(1)).synthesizeForCache("text", voiceMock, AudioFormat.MP3);
    }

    /**
     * Load some TTSResults from files on disk
     *
     * @throws IOException
     */
    @Test
    public void loadTTSResultFromFile() throws IOException {
        // broken fallback supplier to ensure that the service will exclusively use the main cache :
        AudioStreamSupplier fallbackSupplierBroken = new AudioStreamSupplier(ttsServiceMock, "WRONG DATA", voiceMock,
                AudioFormat.MP3);

        // prepare an info file
        File soundFile1Info = tempDir.resolve("filesound1.info").toFile();
        TTSResult.AudioFormatInfoFile audioFormatInfoFile = new TTSResult.AudioFormatInfoFile("text", null, 42, 16,
                16000L, 1, "MP3", null);
        try (FileWriter sound1fileInfoWriter1 = new FileWriter(soundFile1Info)) {
            Gson gson = new GsonBuilder().create();
            gson.toJson(audioFormatInfoFile, sound1fileInfoWriter1);
        }

        // prepare the related sound file
        File soundFile1Snd = tempDir.resolve("filesound1.snd").toFile();
        try (FileWriter fileWriterSnd = new FileWriter(soundFile1Snd)) {
            fileWriterSnd.write("Fake data");
        }

        // Build the TTSResult that will load the info file
        TTSResult ttsResultBuildByFile = new TTSResult(tempDir, "filesound1");

        assertEquals("text", ttsResultBuildByFile.getText());

        // Test the fake AudioStream as string data
        AudioStream audioStreamClient = ttsResultBuildByFile.getAudioStreamClient(fallbackSupplierBroken);
        String readFromFile = new String(audioStreamClient.readAllBytes());
        audioStreamClient.close();
        assertEquals("Fake data", readFromFile);

        // test audio format
        assertEquals(42, audioStreamClient.getFormat().getBitDepth());
        assertEquals(16, audioStreamClient.getFormat().getBitRate());
        assertEquals(16000, audioStreamClient.getFormat().getFrequency());
        assertEquals("MP3", audioStreamClient.getFormat().getCodec());
        assertEquals(1, audioStreamClient.getFormat().getChannels());
        assertEquals(null, audioStreamClient.getFormat().getContainer());
    }

    /**
     * Test that the service can handle several calls concurrently and get the TTS result only once
     *
     * @throws TTSException
     * @throws IOException
     */
    @Test
    public void loadTwoStreamsAtTheSameTimeFromTheSameTTS() throws TTSException, IOException {
        // broken fallback supplier to ensure that the service will exclusively use the main cache :
        AudioStreamSupplier fallbackSupplierBroken = new AudioStreamSupplier(ttsServiceMock, "WRONG DATA", voiceMock,
                AudioFormat.MP3);

        // init simulated data stream
        FakeAudioStream fakeAudioStream = new FakeAudioStream(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 });
        AudioStreamSupplier supplier = new AudioStreamSupplier(ttsServiceMock, "text", voiceMock, AudioFormat.MP3);
        when(ttsServiceMock.synthesizeForCache("text", voiceMock, AudioFormat.MP3)).thenReturn(fakeAudioStream);

        TTSResult ttsResult = new TTSResult(tempDir, "key1", supplier);

        // get a first audiostream wrapped by the cache system
        AudioStream actualAudioStream1 = ttsResult.getAudioStreamClient(fallbackSupplierBroken);

        // get a second, concurrent, audiostream wrapped by the cache system
        AudioStream actualAudioStream2 = ttsResult.getAudioStreamClient(fallbackSupplierBroken);

        // read bytes from the two stream concurrently
        byte[] byteReadFromStream1 = actualAudioStream1.readNBytes(4);
        byte[] byteReadFromStream2 = actualAudioStream2.readNBytes(4);

        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, byteReadFromStream1);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, byteReadFromStream2);

        // second read, from the two stream concurrently
        byteReadFromStream1 = actualAudioStream1.readNBytes(4);
        byteReadFromStream2 = actualAudioStream2.readNBytes(4);

        actualAudioStream1.close();
        assertFalse(fakeAudioStream.isClosed()); // not closed because there is still one open
        actualAudioStream2.close();
        assertTrue(fakeAudioStream.isClosed()); // all client closed, the main stream should also be closed

        assertArrayEquals(new byte[] { 5, 6, 7, 8 }, byteReadFromStream1);
        assertArrayEquals(new byte[] { 5, 6, 7, 8 }, byteReadFromStream1);

        // we call the TTS service only once
        verify(ttsServiceMock, times(1)).synthesizeForCache("text", voiceMock, AudioFormat.MP3);
        verifyNoMoreInteractions(ttsServiceMock);
    }

    /**
     * Test that the service can handle several calls concurrently in two threads and get the TTS result only once
     *
     * @throws TTSException
     * @throws IOException
     */
    @Test
    public void loadTwoThreadsAtTheSameTimeFromTheSameTTS() throws TTSException, IOException {
        // broken fallback supplier to ensure that the service will exclusively use the main cache :
        AudioStreamSupplier fallbackSupplierBroken = new AudioStreamSupplier(ttsServiceMock, "WRONG DATA", voiceMock,
                AudioFormat.MP3);

        // init simulated data stream
        byte[] randomData = getRandomData(10 * 10240);
        FakeAudioStream fakeAudioStream = new FakeAudioStream(randomData);
        AudioStreamSupplier supplier = new AudioStreamSupplier(ttsServiceMock, "text", voiceMock, AudioFormat.MP3);
        when(ttsServiceMock.synthesizeForCache("text", voiceMock, AudioFormat.MP3)).thenReturn(fakeAudioStream);

        TTSResult ttsResult = new TTSResult(tempDir, "key1", supplier);

        // get a first audiostream wrapped by the cache system
        AudioStream actualAudioStream1 = ttsResult.getAudioStreamClient(fallbackSupplierBroken);

        // get a second, concurrent, audiostream wrapped by the cache system
        AudioStream actualAudioStream2 = ttsResult.getAudioStreamClient(fallbackSupplierBroken);

        // read bytes from the two stream concurrently
        List<AudioStream> parallelAudioStreamList = Arrays.asList(actualAudioStream1, actualAudioStream2);
        List<byte[]> bytesResultList = parallelAudioStreamList.parallelStream().map(stream -> readSafe(stream))
                .collect(Collectors.toList());

        assertArrayEquals(randomData, bytesResultList.get(0));
        assertArrayEquals(randomData, bytesResultList.get(1));

        actualAudioStream1.close();
        assertFalse(fakeAudioStream.isClosed()); // not closed because there is still one open
        actualAudioStream2.close();
        assertTrue(fakeAudioStream.isClosed()); // all client closed, the main stream should also be closed

        // we call the TTS service only once
        verify(ttsServiceMock, times(1)).synthesizeForCache("text", voiceMock, AudioFormat.MP3);
        verifyNoMoreInteractions(ttsServiceMock);
    }

    private byte[] readSafe(AudioStream audioStream) {
        try {
            return audioStream.readAllBytes();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    private byte[] getRandomData(int length) {
        Random random = new Random();
        byte[] randomBytes = new byte[length];
        random.nextBytes(randomBytes);
        return randomBytes;
    }

    @Test
    public void testStreamWithSeveralChunks() throws TTSException, IOException {
        // broken fallback supplier to ensure that the service will exclusively use the main cache :
        AudioStreamSupplier fallbackSupplierBroken = new AudioStreamSupplier(ttsServiceMock, "WRONG DATA", voiceMock,
                AudioFormat.MP3);

        // init simulated data stream (two chunks of 10k)
        byte[] randomData = getRandomData(2 * 10240);
        FakeAudioStream fakeAudioStream = new FakeAudioStream(randomData);
        AudioStreamSupplier supplier = new AudioStreamSupplier(ttsServiceMock, "text", voiceMock, AudioFormat.MP3);
        when(ttsServiceMock.synthesizeForCache("text", voiceMock, AudioFormat.MP3)).thenReturn(fakeAudioStream);

        TTSResult ttsResult = new TTSResult(tempDir, "key1", supplier);

        AudioStream audioStreamClient = ttsResult.getAudioStreamClient(fallbackSupplierBroken);
        byte[] bytesRead = audioStreamClient.readAllBytes();
        audioStreamClient.close();
        assertTrue(fakeAudioStream.isClosed());

        assertArrayEquals(randomData, bytesRead);
    }

    /**
     * Get the total length of the stream by forcing it to read everything
     *
     * @throws TTSException
     * @throws IOException
     */
    @Test
    public void testGetTotalSize() throws TTSException, IOException {
        // broken fallback supplier to ensure that the service will exclusively use the main cache :
        AudioStreamSupplier fallbackSupplierBroken = new AudioStreamSupplier(ttsServiceMock, "WRONG DATA", voiceMock,
                AudioFormat.MP3);

        // init simulated data stream (two chunks of 10k)
        byte[] randomData = getRandomData(2 * 10240);
        AudioStream fakeAudioStream = new FakeAudioStream(randomData);
        AudioStreamSupplier supplier = new AudioStreamSupplier(ttsServiceMock, "text", voiceMock, AudioFormat.MP3);
        when(ttsServiceMock.synthesizeForCache("text", voiceMock, AudioFormat.MP3)).thenReturn(fakeAudioStream);

        TTSResult ttsResult = new TTSResult(tempDir, "key1", supplier);
        AudioStream audioStreamClient = ttsResult.getAudioStreamClient(fallbackSupplierBroken);
        Long totalSize = ttsResult.getTotalSize();
        audioStreamClient.close();
        assertEquals(2 * 10240, totalSize);
    }
}
