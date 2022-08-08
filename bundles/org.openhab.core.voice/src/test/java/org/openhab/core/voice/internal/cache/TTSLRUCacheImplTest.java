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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.OpenHAB;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.voice.TTSException;
import org.openhab.core.voice.TTSService;
import org.openhab.core.voice.Voice;

/**
 * Test the cache system
 *
 * @author Gwendal Roulleau - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class TTSLRUCacheImplTest {

    @TempDir
    @NonNullByDefault({})
    Path tempDir;

    @NonNullByDefault({})
    private @Mock Voice voiceMock;

    @NonNullByDefault({})
    private @Mock TTSService ttsServiceMock;

    @NonNullByDefault({})
    private @Mock AudioStreamSupplier supplierMock;

    @NonNullByDefault({})
    private @Mock AudioStream audioStreamMock;

    @BeforeEach
    public void init() {
        System.setProperty(OpenHAB.USERDATA_DIR_PROG_ARGUMENT, tempDir.toString());
    }

    /**
     * Basic get and set for the LRU cache
     *
     * @throws IOException
     */
    @Test
    public void simpleLRUPutAndGetTest() throws IOException {
        TTSLRUCacheImpl voiceLRUCache = new TTSLRUCacheImpl(10);
        TTSResult ttsResult = new TTSResult(tempDir.toFile(), "key1", supplierMock);
        voiceLRUCache.put("key1", ttsResult);
        assertEquals(ttsResult, voiceLRUCache.get("key1"));
        assertEquals(null, voiceLRUCache.get("key2"));
    }

    /**
     * Test the LRU eviction policy
     *
     * @throws IOException
     */
    @Test
    public void putAndGetAndEvictionOrderLRUTest() throws IOException {
        TTSLRUCacheImpl voiceLRUCache = new TTSLRUCacheImpl(10);
        TTSResult ttsResult1 = new TTSResult(tempDir.toFile(), "key1", supplierMock);
        ttsResult1.setSize(4);
        voiceLRUCache.put("key1", ttsResult1);
        TTSResult ttsResult2 = new TTSResult(tempDir.toFile(), "key2", supplierMock);
        ttsResult2.setSize(4);
        voiceLRUCache.put("key2", ttsResult2);
        TTSResult ttsResult3 = new TTSResult(tempDir.toFile(), "key3", supplierMock);
        ttsResult3.setSize(4);
        voiceLRUCache.put("key3", ttsResult3);
        TTSResult ttsResult4 = new TTSResult(tempDir.toFile(), "key4", supplierMock);
        ttsResult4.setSize(4);
        voiceLRUCache.put("key4", ttsResult4);

        // ttsResult1 should be evicted now (size limit is 10, and effective size is 12 when we try to put the
        // ttsResult4)
        assertEquals(null, voiceLRUCache.get("key1"));

        // getting ttsResult2 will put it in head, ttsResult3 is now tail
        assertEquals(ttsResult2, voiceLRUCache.get("key2"));

        // putting again ttsResult1 should expel tail, which is ttsResult3
        voiceLRUCache.put("key1", ttsResult1);
        assertEquals(null, voiceLRUCache.get("key3"));
    }

    /**
     * Test the file deletion
     *
     * @throws IOException
     */
    @Test
    public void fileDeletionTest() throws IOException {
        TTSResult ttsResult1 = new TTSResult(tempDir.toFile(), "key1", supplierMock);
        File ttsResult1File = tempDir.resolve(ttsResult1.getKey() + TTSLRUCacheImpl.SOUND_EXT).toFile();
        ttsResult1File.createNewFile();
        File ttsResult1InfoFile = tempDir.resolve(ttsResult1.getKey() + TTSLRUCacheImpl.INFO_EXT).toFile();
        ttsResult1InfoFile.createNewFile();

        ttsResult1.deleteFiles();

        assertFalse(ttsResult1File.exists());
        assertFalse(ttsResult1InfoFile.exists());
    }

    /**
     * Test that the eviction policy calls the delete method
     *
     * @throws IOException
     */
    @Test
    public void fileDeletionWhenEvictionTest() throws IOException {
        TTSLRUCacheImpl voiceLRUCache = new TTSLRUCacheImpl(10);

        TTSResult ttsResultToEvict = Mockito.mock(TTSResult.class);
        when(ttsResultToEvict.getCurrentSize()).thenReturn(15L);
        when(ttsResultToEvict.getKey()).thenReturn("ttsResultToEvict");
        voiceLRUCache.put("ttsResultToEvict", ttsResultToEvict);

        // the cache is already full, so the next put will delete ttsResultToEvict
        TTSResult ttsResult2 = new TTSResult(tempDir.toFile(), "key2", supplierMock);
        ttsResult2.setSize(4);
        voiceLRUCache.put("key2", ttsResult2);

        verify(ttsResultToEvict).deleteFiles();
    }

    /**
     * This test checks than we can overwrite a previously
     * cached entry and then it is now at the head position.
     *
     * @throws IOException
     */
    @Test
    public void putExistingResultLRUTest() throws IOException {
        TTSLRUCacheImpl voiceLRUCache = new TTSLRUCacheImpl(10);
        TTSResult ttsResult1 = new TTSResult(tempDir.toFile(), "key1", supplierMock);
        ttsResult1.setSize(4);
        voiceLRUCache.put("key1", ttsResult1);
        TTSResult ttsResult2 = new TTSResult(tempDir.toFile(), "key2", supplierMock);
        ttsResult2.setSize(10);
        voiceLRUCache.put("key2", ttsResult2);

        // put again key1 --> key2 is now tail
        voiceLRUCache.put("key1", ttsResult1);

        TTSResult ttsResult3 = new TTSResult(tempDir.toFile(), "key3", supplierMock);
        ttsResult3.setSize(4);
        voiceLRUCache.put("key3", ttsResult3);

        // ttsResult2 should be expelled now
        assertEquals(null, voiceLRUCache.get("key2"));

        // ttsResult1 and ttsResult3 are a hit
        assertEquals(ttsResult1, voiceLRUCache.get("key1"));
        assertEquals(ttsResult3, voiceLRUCache.get("key3"));
    }

    /**
     * Simulate a cache miss, then two other hits
     * The TTS service is called only once
     *
     * @throws TTSException
     * @throws IOException
     */
    @Test
    public void getOrSynthetizeCacheMissAndHit() throws TTSException, IOException {
        when(ttsServiceMock.getCacheKey("text", voiceMock, AudioFormat.MP3)).thenReturn("filename1");
        when(ttsServiceMock.synthesize("text", voiceMock, AudioFormat.MP3)).thenReturn(audioStreamMock);
        when(audioStreamMock.getFormat()).thenReturn(AudioFormat.MP3);
        // In this test the audio stream will return two bytes of data, then an empty stream so signal its end :
        when(audioStreamMock.readNBytes(any(Integer.class))).thenReturn(new byte[2], new byte[0]);

        TTSLRUCacheImpl voiceLRUCache = new TTSLRUCacheImpl(1000);

        // first cache miss
        AudioStream ttsResultStream = voiceLRUCache.getOrSynthetize(ttsServiceMock, "text", voiceMock, AudioFormat.MP3);
        // force supplier resolution with a "getFormat":
        ttsResultStream.getFormat();
        ttsResultStream.readAllBytes();
        ttsResultStream.close();

        // then cache hit
        ttsResultStream = voiceLRUCache.getOrSynthetize(ttsServiceMock, "text", voiceMock, AudioFormat.MP3);
        // force supplier resolution with a "getFormat" --> won't be called
        ttsResultStream.getFormat();
        ttsResultStream.readAllBytes();
        ttsResultStream.close();

        // then cache hit
        ttsResultStream = voiceLRUCache.getOrSynthetize(ttsServiceMock, "text", voiceMock, AudioFormat.MP3);
        // force supplier resolution with a "getFormat" --> won't be called
        ttsResultStream.getFormat();
        ttsResultStream.readAllBytes();
        ttsResultStream.close();

        // even with three call to getOrSynthetize and getFormat, the TTS service and the underlying stream were called
        // only once :
        verify(ttsServiceMock, times(1)).synthesize("text", voiceMock, AudioFormat.MP3);
        verify(audioStreamMock, times(1)).getFormat();
        // this is called twice because the second call respond with zero and signal the end of stream
        verify(audioStreamMock, times(2)).readNBytes(any(Integer.class));
        // This is called every time to compute the key to search in cache :
        verify(ttsServiceMock, times(3)).getCacheKey("text", voiceMock, AudioFormat.MP3);
    }

    /**
     * Load some TTSResults from files on disk
     *
     * @throws IOException
     */
    @Test
    public void loadTTSResultsFromCacheDirectory() throws IOException {
        // prepare cache directory
        String cacheDir = "/cache/org.openhab.voice.tts/";
        File cacheDirectory = new File(tempDir.toFile() + cacheDir);
        cacheDirectory.mkdirs();

        // prepare some files
        File soundFileInfo1 = new File(tempDir.toFile(), cacheDir + "filesound1.info");
        File soundFile1 = new File(tempDir.toFile(), cacheDir + "filesound1.snd");
        try (FileWriter soundfileInfo1Writer = new FileWriter(soundFileInfo1);
                FileWriter soundFile1Writer = new FileWriter(soundFile1)) {
            soundfileInfo1Writer.write("bigEndian=null\n" + "container=null\n" + "codec=MP3\n" + "channels=1\n"
                    + "bitRate=16\n" + "bitDepth=42\n" + "text=text\n" + "frequency=16000\n");
            soundFile1Writer.write("falsedata");
        }

        // prepare some files
        File soundFile2Info = new File(tempDir.toFile(), cacheDir + "filesound2.info");
        File soundFile2 = new File(tempDir.toFile(), cacheDir + "filesound2.snd");
        try (FileWriter soundFileInfo2Writer = new FileWriter(soundFile2Info);
                FileWriter soundFile2Writer = new FileWriter(soundFile2)) {
            soundFileInfo2Writer.write("bigEndian=null\n" + "container=MP3\n" + "codec=MP3\n" + "channels=2\n"
                    + "bitRate=32\n" + "bitDepth=42\n" + "text=text to say again\n" + "frequency=16000\n");
            soundFile2Writer.write("falsedata");
        }

        // create a LRU cache that will load the above .info files
        TTSLRUCacheImpl lruCache = new TTSLRUCacheImpl(20);

        TTSResult ttsResult1 = lruCache.get("filesound1");
        assertNotEquals(null, ttsResult1);

        TTSResult ttsResult2 = lruCache.get("filesound2");
        assertNotEquals(null, ttsResult2);

        TTSResult ttsResult3 = lruCache.get("filesound3");
        assertEquals(null, ttsResult3);
    }

    @Test
    public void enoughFreeDiskSpaceTest() {
        try {
            new TTSLRUCacheImpl(Long.MAX_VALUE / 10); // arbitrary long value, should throw exception because no disk of
                                                      // this size exists
            fail();
        } catch (IOException e) {
        }
    }

    @Test
    public void testCleanDirectoryOrphanFiles() throws IOException {
        // prepare cache directory
        String cacheDir = "/cache/org.openhab.voice.tts/";
        File cacheDirectory = new File(tempDir.toFile() + cacheDir);
        cacheDirectory.mkdirs();

        // prepare some files : normal entry
        File soundFileInfo1 = new File(tempDir.toFile(), cacheDir + "filesound1.info");
        File soundFile1 = new File(tempDir.toFile(), cacheDir + "filesound1.snd");
        try (FileWriter soundfileInfo1Writer = new FileWriter(soundFileInfo1);
                FileWriter soundFile1Writer = new FileWriter(soundFile1)) {
            soundfileInfo1Writer.write("bigEndian=null\n" + "container=null\n" + "codec=MP3\n" + "channels=1\n"
                    + "bitRate=16\n" + "bitDepth=42\n" + "text=text\n" + "frequency=16000\n");
            soundFile1Writer.write("falsedata");
        }

        // prepare some files : orphan info file
        File soundFile2Info = new File(tempDir.toFile(), cacheDir + "filesound2.info");
        try (FileWriter soundFileInfo2Writer = new FileWriter(soundFile2Info)) {
            soundFileInfo2Writer.write("bigEndian=null\n" + "container=MP3\n" + "codec=MP3\n" + "channels=2\n"
                    + "bitRate=32\n" + "bitDepth=42\n" + "text=text to say again\n" + "frequency=16000\n");
        }

        // prepare some files : orphan sound file
        File soundFile3 = new File(tempDir.toFile(), cacheDir + "filesound3.snd");
        try (FileWriter soundFile3Writer = new FileWriter(soundFile3)) {
            soundFile3Writer.write("fake non empty data");
        }

        // create a LRU cache that will load the above .info files
        new TTSLRUCacheImpl(20);

        // the file for entry 1 still exists :
        assertTrue(soundFile1.exists());
        assertTrue(soundFileInfo1.exists());

        // the file for entry 2 should have been deleted :
        assertFalse(soundFile2Info.exists());

        // the file for entry 3 should have been deleted :
        assertFalse(soundFile3.exists());
    }

    @Test
    public void testCleanDirectoryEmptyFiles() throws IOException {
        // prepare cache directory
        String cacheDir = "/cache/org.openhab.voice.tts/";
        File cacheDirectory = new File(tempDir.toFile() + cacheDir);
        cacheDirectory.mkdirs();

        // prepare some files : normal entry
        File soundFileInfo1 = new File(tempDir.toFile(), cacheDir + "filesound1.info");
        File soundFile1 = new File(tempDir.toFile(), cacheDir + "filesound1.snd");
        try (FileWriter soundfileInfo1Writer = new FileWriter(soundFileInfo1);
                FileWriter soundFile1Writer = new FileWriter(soundFile1)) {
            soundfileInfo1Writer.write("bigEndian=null\n" + "container=null\n" + "codec=MP3\n" + "channels=1\n"
                    + "bitRate=16\n" + "bitDepth=42\n" + "text=text\n" + "frequency=16000\n");
            soundFile1Writer.write("falsedata");
        }

        // prepare some files : empty file
        File soundFile2Info = new File(tempDir.toFile(), cacheDir + "filesound2.info");
        soundFile2Info.createNewFile();
        assertTrue(soundFile2Info.exists());

        // create a LRU cache that will load the above .info files
        new TTSLRUCacheImpl(20);

        // the file for entry 1 still exists :
        assertTrue(soundFile1.exists());
        assertTrue(soundFileInfo1.exists());

        // the file for entry 2 should have been deleted :
        assertFalse(soundFile2Info.exists());
    }
}
