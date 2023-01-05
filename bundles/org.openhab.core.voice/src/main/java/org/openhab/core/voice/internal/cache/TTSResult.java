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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.FixedLengthAudioStream;
import org.openhab.core.voice.TTSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * A cached sound resulting from a call to a TTSService
 * All objects from this class form a linked list,
 * next and previous value allow navigation through it.
 * This class adds also the capability to serve multiple sinks concurrently
 * without asking already retrieved data to the wrapped stream.
 *
 * @author Gwendal Roulleau - Initial contribution
 */
@NonNullByDefault
public class TTSResult {

    private final Logger logger = LoggerFactory.getLogger(TTSResult.class);

    /**
     * Arbitrary chunk size. Small is less latency but more small calls and CPU load.
     */
    private static final int CHUNK_SIZE = 10000;

    /**
     * Take count of the number of {@link AudioStreamCacheWrapper} currently using this {@link TTSResult}
     */
    private int countAudioStreamClient = 0;

    /**
     * A unique key to identify the result
     * (used to build the filename)
     */
    private final String key;

    private final String text;

    // The inner AudioStream, with a supplier
    private @Nullable AudioStreamSupplier ttsAudioStreamSupplier;
    private @Nullable AudioStream ttsAudioStream;
    private @Nullable AudioFormat audioFormat;

    // The cache files where the sound and its info are stored :
    private final File soundFile;
    private final File infoFile;
    private long currentSize = 0;
    private boolean completed;

    private @Nullable FileChannel fileChannel;
    private final Lock fileOperationLock = new ReentrantLock();

    private Gson gson = new GsonBuilder().create();

    /**
     * This constructor is used when the file is fully cached on disk.
     * The file on disk will provide the data, and the .info file will
     * provide metadata about the format.
     *
     * @param cacheDirectory Where are the cached file stored
     * @param key a unique key
     */
    public TTSResult(Path cacheDirectory, String key) throws IOException {
        this.key = key;
        this.soundFile = cacheDirectory.resolve(key + TTSLRUCacheImpl.SOUND_EXT).toFile();
        this.infoFile = cacheDirectory.resolve(key + TTSLRUCacheImpl.INFO_EXT).toFile();
        this.ttsAudioStreamSupplier = null;
        this.completed = true;
        if (soundFile.length() == 0) {
            throw new IOException("Sound cache file is empty. Throwing it");
        }
        this.currentSize = soundFile.length();

        try (FileReader infoFileReader = new FileReader(infoFile)) {
            AudioFormatInfoFile audioFormatInfoFile = gson.fromJson(infoFileReader, AudioFormatInfoFile.class);

            this.text = audioFormatInfoFile.text;
            this.audioFormat = new AudioFormat(audioFormatInfoFile.container, audioFormatInfoFile.codec,
                    audioFormatInfoFile.bigEndian, audioFormatInfoFile.bitDepth, audioFormatInfoFile.bitRate,
                    audioFormatInfoFile.frequency, audioFormatInfoFile.channels);
        }
    }

    /***
     * This constructor is used when the file is not yet cached on disk.
     * The {@link AudioStreamSupplier} will provide the data.
     *
     * @param cacheDirectory The cache folder
     * @param key A unique key to identify the produced TTS sound
     * @param ttsSynthesizerSupplier The {@link AudioStreamSupplier} for the result we want to cache, from the TTS
     *            service
     */
    public TTSResult(Path cacheDirectory, String key, AudioStreamSupplier ttsSynthesizerSupplier) {
        this.key = key;
        this.text = ttsSynthesizerSupplier.getText();
        this.soundFile = cacheDirectory.resolve(key + TTSLRUCacheImpl.SOUND_EXT).toFile();
        this.infoFile = cacheDirectory.resolve(key + TTSLRUCacheImpl.INFO_EXT).toFile();
        this.ttsAudioStreamSupplier = ttsSynthesizerSupplier;
        this.completed = false;
    }

    /**
     * Get total size of the underlying sound stream.
     * If not already completed, will query the stream inside,
     * or get all the data.
     *
     * @return
     */
    protected Long getTotalSize() {
        if (completed) { // we already know the total size of the sound
            return currentSize;
        } else {
            // first try to check if the inner stream has the information
            AudioStream ttsAudioStreamLocal = ttsAudioStream;
            if (ttsAudioStreamLocal != null
                    && ttsAudioStreamLocal instanceof FixedLengthAudioStream fixedLengthAudioStream) {
                return fixedLengthAudioStream.length();
            }
            // else, we must force-read all the stream to get the real size
            try {
                read(0, Integer.MAX_VALUE);
            } catch (IOException e) {
                logger.debug("Cannot read the total size of the TTS result. Using 0", e);
            }
            return currentSize;
        }
    }

    /**
     * Get the current size
     *
     * @return
     */
    public long getCurrentSize() {
        return currentSize;
    }

    protected void setSize(long size) {
        this.currentSize = size;
    }

    public String getText() {
        return text;
    }

    public String getKey() {
        return key;
    }

    /**
     * Open an openHAB cached AudioStream wrapped around the file
     * There could be several clients AudioStream on the same TTSResult
     *
     * @param fallbackAudioStreamSupplier If something goes wrong with the cache, this supplier will provide the
     *            AudioStream directly from the TTS service
     *
     * @return An {@link AudioStream} that can be used to play sound
     */
    protected AudioStream getAudioStream(AudioStreamSupplier fallbackAudioStreamSupplier) {
        logger.debug("Trying to open a cache audiostream client for {}", soundFile.getName());

        // if this TTSResult was loaded from file, take the opportunity to record the supplier :
        if (ttsAudioStreamSupplier == null) {
            ttsAudioStreamSupplier = fallbackAudioStreamSupplier;
        }

        fileOperationLock.lock();
        try {
            countAudioStreamClient++;
            // we could have to open the fileChannel
            FileChannel fileChannelLocal = fileChannel;
            if (fileChannelLocal == null || !soundFile.exists()) {
                try {
                    fileChannelLocal = FileChannel.open(soundFile.toPath(),
                            EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE));
                    fileChannel = fileChannelLocal;
                    // if the file size is 0 but the completed boolean is true, THEN it means the file have
                    // been deleted. We must mark the file as to be recreated by reseting everything :
                    if (completed && fileChannelLocal.size() == 0) {
                        logger.debug("The cached sound file {} is not present anymore. We have to recreate it",
                                soundFile.getName());
                        resetStream(fallbackAudioStreamSupplier);
                    }
                } catch (IOException e) {
                    logger.info("Cannot open the TTS cached fileChannel for file {}", soundFile.getName());
                }
            }
        } finally {
            fileOperationLock.unlock();
        }
        return new AudioStreamCacheWrapper(this, fallbackAudioStreamSupplier);
    }

    private void resetStream(AudioStreamSupplier fallbackAudioStreamSupplier) {
        completed = false;
        currentSize = 0;
        ttsAudioStream = null;
        ttsAudioStreamSupplier = fallbackAudioStreamSupplier;
    }

    /**
     * This method is called by a wrapper when it has been closed by a client
     * The file and the inner stream could then be closed, if and only if no other client are accessing it.
     *
     * @throws IOException
     */
    protected void closeAudioStreamClient() throws IOException {
        logger.debug("Trying to close a cached audiostream client for {}", soundFile.getName());
        fileOperationLock.lock();
        try {
            countAudioStreamClient--;
            if (countAudioStreamClient <= 0) {// no more client reading or writing : closing the
                                              // filechannel
                try {
                    FileChannel fileChannelLocal = fileChannel;
                    if (fileChannelLocal != null) {
                        try {
                            logger.debug("Effectively close the TTS cache filechannel for {}", soundFile.getName());
                            fileChannelLocal.close();
                        } finally {
                            fileChannel = null;
                        }
                    }
                } finally {
                    AudioStream ttsAudioStreamLocal = ttsAudioStream;
                    if (ttsAudioStreamLocal != null) {
                        ttsAudioStreamLocal.close();
                    }
                }
            }
        } finally {
            fileOperationLock.unlock();
        }
    }

    /**
     * This method is called when we can't defer anymore the call to the TTS service.
     * The request is then resolved to get the real AudioStream
     *
     * @throws TTSException
     */
    private void resolveAudioStreamSupplier() throws TTSException {
        AudioStreamSupplier audioStreamSupplierLocal = ttsAudioStreamSupplier;
        if (ttsAudioStream == null && audioStreamSupplierLocal != null && !audioStreamSupplierLocal.isResolved()) {
            logger.trace("Trying to synchronize for resolving supplier");
            synchronized (audioStreamSupplierLocal) {
                if (!audioStreamSupplierLocal.isResolved()) { // test again after getting the lock
                    try {
                        AudioStream audioStreamResolved = audioStreamSupplierLocal.resolve();
                        ttsAudioStream = audioStreamResolved;
                        AudioFormat audioFormatFromTTSAudioStream = audioStreamResolved.getFormat();
                        this.audioFormat = audioFormatFromTTSAudioStream;
                        // now that we get the real response format, we can create the .info file
                        createInfoFile(audioFormatFromTTSAudioStream);
                    } catch (IOException e) {
                        throw new TTSException("Cannot create TTS cache file", e);
                    }
                }
            }
        }
    }

    /**
     * Get AudioFormat for this TTSResult.
     * Check the underlying AudioStream if the metadata is not available
     *
     * @return
     */
    protected AudioFormat getAudioFormat() {
        AudioFormat audioFormatLocal = this.audioFormat;
        if (audioFormatLocal != null) {
            return audioFormatLocal;
        } else {
            try {
                resolveAudioStreamSupplier();
            } catch (TTSException e) {
                logger.warn("Cannot get or store audio format from the TTS audio service: {}", e.getMessage());
            }
            audioFormatLocal = audioFormat;
            if (audioFormatLocal == null) { // should't happen : resolve and synthezise MUST fill it
                logger.warn("Cannot get audio format for TTS sound file {}. Assuming WAV", soundFile.getName());
                return AudioFormat.WAV;
            }
            return audioFormatLocal;
        }
    }

    /**
     * Read from the cached file. If there is not enough bytes to read in the file, the AudioStream from the TTS service
     * will be queried.
     *
     * @param start The offset to read the file from
     * @param sizeToRead the number of byte to read
     * @return A byte array from the file. The size may or may not be the sizeToRead requested
     * @throws IOException
     */
    protected byte[] read(int start, int sizeToRead) throws IOException {
        FileChannel fileChannelLocal = fileChannel;
        if (fileChannelLocal == null) {
            throw new IOException("Cannot read TTS cache from null file channel. Shouldn't happen");
        }
        try {
            // check if we need to get data from the inner stream. Note : if completeSize != null, then the end of
            // stream has already been reached :
            if (start + sizeToRead > fileChannelLocal.size() && !completed) {
                logger.trace("Maybe need to get data from inner stream");
                resolveAudioStreamSupplier();
                // try to get new bytes from the inner stream
                AudioStream ttsAudioStreamLocal = ttsAudioStream;
                if (ttsAudioStreamLocal != null) {
                    logger.trace("Trying to synchronize for reading inner audiostream");
                    synchronized (ttsAudioStreamLocal) {
                        // now that we really have the lock, test again if we really need data from the stream
                        while (start + sizeToRead > fileChannelLocal.size() && !completed) {
                            logger.trace("Really need to get data from inner stream");
                            byte[] readFromTTS = ttsAudioStreamLocal.readNBytes(CHUNK_SIZE);
                            if (readFromTTS.length == 0) { // we read all the stream
                                logger.trace("End of the stream reached");
                                completed = true;
                            } else {
                                fileChannelLocal.write(ByteBuffer.wrap(readFromTTS), currentSize);
                                logger.trace("writing {} bytes to {}", readFromTTS.length, soundFile.getName());
                                currentSize += readFromTTS.length;
                            }
                        }
                    }
                }
            }
            // the cache file is now filled, get bytes from it.
            long maxToRead = Math.min(currentSize, sizeToRead);
            ByteBuffer byteBufferFromChannelFile = ByteBuffer.allocate((int) maxToRead);
            int byteReadNumber = fileChannelLocal.read(byteBufferFromChannelFile, Integer.valueOf(start).longValue());
            logger.debug("Read {} bytes from the filechannel", byteReadNumber);
            if (byteReadNumber > 0) {
                byte[] resultByteArray = new byte[byteReadNumber];
                byteBufferFromChannelFile.rewind();
                byteBufferFromChannelFile.get(resultByteArray);
                return resultByteArray;
            } else {
                return new byte[0];
            }
        } catch (TTSException e) {
            throw new IOException("Cannot read byte from the TTS service", e);
        }
    }

    /**
     * Return the number of bytes that we can actually read without calling
     * the underlying stream
     *
     * @param offset
     * @return
     */
    protected int availableFrom(int offset) {
        FileChannel fileChannelLocal = fileChannel;
        if (fileChannelLocal == null) {
            return 0;
        }
        try {
            return Math.max(0, Long.valueOf(fileChannelLocal.size() - offset).intValue());
        } catch (IOException e) {
            logger.debug("Cannot get file length for TTS sound file {}", soundFile.getName());
            return 0;
        }
    }

    /**
     * Create the .info file alongside
     *
     * @param responseFormat The audio format effectively produced by the TTS
     * @throws IOException
     */
    private void createInfoFile(AudioFormat responseFormat) throws IOException {
        if (!soundFile.canWrite() || !infoFile.createNewFile()) {
            throw new IOException("Cannot write sound cache file " + soundFile.getAbsolutePath() + ". Check rights");
        }

        AudioFormatInfoFile audioFormatInfoFile = new AudioFormatInfoFile(text, responseFormat.isBigEndian(),
                responseFormat.getBitDepth(), responseFormat.getBitRate(), responseFormat.getFrequency(),
                responseFormat.getChannels(), responseFormat.getCodec(), responseFormat.getContainer());

        try (FileWriter infoFileWriter = new FileWriter(infoFile)) {
            gson.toJson(audioFormatInfoFile, infoFileWriter);
        }
    }

    public void deleteFiles() {
        logger.debug("Receiving call to delete the audio file {}", soundFile.getName());
        fileOperationLock.lock();
        try {
            // check if a client is actually reading the file
            if (countAudioStreamClient <= 0) {
                logger.debug("Effectively deleting the audio file {}", soundFile.getName());
                // delete the .info file :
                soundFile.delete();
                // and the associated file
                infoFile.delete();
            }
        } finally {
            fileOperationLock.unlock();
        }
    }

    // We cannot use a record yet (requires Gson v2.10)
    public static class AudioFormatInfoFile {
        public final String text;
        public final @Nullable Boolean bigEndian;
        public final @Nullable Integer bitDepth;
        public final @Nullable Integer bitRate;
        public final @Nullable Long frequency;
        public final @Nullable Integer channels;
        public final @Nullable String codec;
        public final @Nullable String container;

        public AudioFormatInfoFile(String text, @Nullable Boolean bigEndian, @Nullable Integer bitDepth,
                @Nullable Integer bitRate, @Nullable Long frequency, @Nullable Integer channels, @Nullable String codec,
                @Nullable String container) {
            super();
            this.text = text;
            this.bigEndian = bigEndian;
            this.bitDepth = bitDepth;
            this.bitRate = bitRate;
            this.frequency = frequency;
            this.channels = channels;
            this.codec = codec;
            this.container = container;
        }
    }
}
