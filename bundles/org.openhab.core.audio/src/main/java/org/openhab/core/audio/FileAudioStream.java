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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.utils.AudioStreamUtils;
import org.openhab.core.audio.utils.AudioWaveUtils;
import org.openhab.core.common.Disposable;

/**
 * This is an AudioStream from an audio file
 *
 * @author Karel Goderis - Initial contribution
 * @author Kai Kreuzer - Refactored to take a file as input
 * @author Christoph Weitkamp - Refactored use of filename extension
 */
@NonNullByDefault
public class FileAudioStream extends AudioStream implements SizeableAudioStream, ClonableAudioStream, Disposable {

    public static final String WAV_EXTENSION = "wav";
    public static final String MP3_EXTENSION = "mp3";
    public static final String OGG_EXTENSION = "ogg";
    public static final String AAC_EXTENSION = "aac";
    public static final String FLAC_EXTENSION = "flac";

    private final File file;
    private final AudioFormat audioFormat;
    private FileInputStream inputStream;
    private final long length;
    private final boolean isTemporaryFile;
    private int markedOffset = 0;
    private int alreadyRead = 0;

    public FileAudioStream(File file) throws AudioException {
        this(file, getAudioFormat(file));
    }

    public FileAudioStream(File file, AudioFormat format) throws AudioException {
        this(file, format, false);
    }

    public FileAudioStream(File file, AudioFormat format, boolean isTemporaryFile) throws AudioException {
        this.file = file;
        this.inputStream = getInputStream(file);
        this.audioFormat = format;
        this.length = file.length();
        this.isTemporaryFile = isTemporaryFile;
    }

    private static AudioFormat getAudioFormat(File file) throws AudioException {
        final String filename = file.getName().toLowerCase();
        final String extension = AudioStreamUtils.getExtension(filename);
        return switch (extension) {
            case WAV_EXTENSION -> parseWavFormat(file);
            case MP3_EXTENSION -> AudioFormat.MP3;
            case OGG_EXTENSION -> AudioFormat.OGG;
            case AAC_EXTENSION -> AudioFormat.AAC;
            case FLAC_EXTENSION -> parseFlacFormat(file);
            default -> throw new AudioException("Unsupported file extension!");
        };
    }

    private static AudioFormat parseWavFormat(File file) throws AudioException {
        try (BufferedInputStream inputStream = new BufferedInputStream(getInputStream(file))) {
            return AudioWaveUtils.parseWavFormat(inputStream);
        } catch (IOException e) {
            throw new AudioException("Cannot parse wav stream", e);
        }
    }

    private static AudioFormat parseFlacFormat(File file) throws AudioException {
        try (BufferedInputStream inputStream = new BufferedInputStream(getInputStream(file))) {
            javax.sound.sampled.AudioFormat format = AudioSystem.getAudioInputStream(inputStream).getFormat();
            int bitDepth = format.getSampleSizeInBits();
            long sampleRate = Float.valueOf(format.getSampleRate()).longValue();
            int channels = format.getChannels();
            Integer bitRate = bitDepth > 0 && channels > 0 ? Math.round(format.getSampleRate() * bitDepth * channels)
                    : null;
            return new AudioFormat(AudioFormat.CONTAINER_FLAC, AudioFormat.CODEC_FLAC, format.isBigEndian(),
                    bitDepth > 0 ? bitDepth : null, bitRate, sampleRate > 0 ? sampleRate : null,
                    channels > 0 ? channels : null);
        } catch (UnsupportedAudioFileException e) {
            return AudioFormat.FLAC;
        } catch (IOException e) {
            throw new AudioException("Cannot parse flac stream", e);
        }
    }

    private static FileInputStream getInputStream(File file) throws AudioException {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new AudioException("File '" + file.getAbsolutePath() + "' not found!");
        }
    }

    @Override
    public AudioFormat getFormat() {
        return audioFormat;
    }

    @Override
    public int read() throws IOException {
        int read = inputStream.read();
        if (read >= 0) {
            alreadyRead++;
        }
        return read;
    }

    @Override
    public int read(byte @Nullable [] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte @Nullable [] b, int off, int len) throws IOException {
        int read = inputStream.read(b, off, len);
        if (read > 0) {
            alreadyRead += read;
        }
        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        long skipped = inputStream.skip(n);
        if (skipped > 0) {
            alreadyRead += (int) skipped;
        }
        return skipped;
    }

    @Override
    public int available() throws IOException {
        return inputStream.available();
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
        super.close();
    }

    @Override
    public long length() {
        return this.length;
    }

    @Override
    public synchronized void reset() throws IOException {
        try {
            inputStream.close();
        } catch (IOException e) {
        }
        try {
            inputStream = getInputStream(file);
            inputStream.skipNBytes(markedOffset);
            alreadyRead = markedOffset;
        } catch (AudioException e) {
            throw new IOException("Cannot reset file input stream: " + e.getMessage(), e);
        }
    }

    @Override
    public synchronized void mark(int readlimit) {
        markedOffset = alreadyRead;
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public InputStream getClonedStream() throws AudioException {
        return getInputStream(file);
    }

    @Override
    public void dispose() throws IOException {
        if (isTemporaryFile) {
            Files.deleteIfExists(file.toPath());
        }
    }
}
