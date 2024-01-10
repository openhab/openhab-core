/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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

import org.eclipse.jdt.annotation.NonNullByDefault;
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
public class FileAudioStream extends FixedLengthAudioStream implements Disposable {

    public static final String WAV_EXTENSION = "wav";
    public static final String MP3_EXTENSION = "mp3";
    public static final String OGG_EXTENSION = "ogg";
    public static final String AAC_EXTENSION = "aac";

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
        switch (extension) {
            case WAV_EXTENSION:
                return parseWavFormat(file);
            case MP3_EXTENSION:
                return AudioFormat.MP3;
            case OGG_EXTENSION:
                return AudioFormat.OGG;
            case AAC_EXTENSION:
                return AudioFormat.AAC;
            default:
                throw new AudioException("Unsupported file extension!");
        }
    }

    private static AudioFormat parseWavFormat(File file) throws AudioException {
        try (BufferedInputStream inputStream = new BufferedInputStream(getInputStream(file))) {
            return AudioWaveUtils.parseWavFormat(inputStream);
        } catch (IOException e) {
            throw new AudioException("Cannot parse wav stream", e);
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
        alreadyRead++;
        return read;
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
            Files.delete(file.toPath());
        }
    }
}
