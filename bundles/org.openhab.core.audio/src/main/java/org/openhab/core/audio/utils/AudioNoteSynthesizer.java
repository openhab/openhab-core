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
package org.openhab.core.audio.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.ByteArrayAudioStream;

/**
 * Audio note synthesizer. A utility to sent note sounds to audio sinks.
 * Limited to wav little endian streams.
 *
 * @author Miguel Álvarez - Initial contribution
 *
 */
@NonNullByDefault
public class AudioNoteSynthesizer {
    private final long sampleRate;
    private final int bitDepth;
    private final int bitRate;
    private final int channels;
    private final boolean bigEndian;

    public static Set<AudioFormat> getSupportedFormats() {
        return Set.of(new AudioFormat(AudioFormat.CONTAINER_WAVE, AudioFormat.CODEC_PCM_SIGNED, false, null, null, null,
                null));
    }

    public static Sound noteSound(Note note, long millis) {
        return new Sound(note.getFrequency(), millis);
    }

    public static Sound silenceSound(long millis) {
        return new Sound(0.0, millis);
    }

    public AudioNoteSynthesizer(AudioFormat audioFormat) {
        assert audioFormat.getFrequency() != null;
        this.sampleRate = audioFormat.getFrequency();
        assert audioFormat.getBitDepth() != null;
        this.bitDepth = audioFormat.getBitDepth();
        assert audioFormat.getBitRate() != null;
        this.bitRate = audioFormat.getBitRate();
        assert audioFormat.getChannels() != null;
        this.channels = audioFormat.getChannels();
        var bigEndian = audioFormat.isBigEndian();
        assert bigEndian != null;
        this.bigEndian = bigEndian;
    }

    public AudioStream getStream(Note note, long millis) throws IOException {
        return getStream(List.of(noteSound(note, millis)));
    }

    public AudioStream getStream(List<Sound> sounds) throws IOException {
        int byteRate = (int) (sampleRate * bitDepth * channels / 8);
        byte[] audioBuffer = new byte[0];
        for (var sound : sounds) {
            var frequency = sound.frequency;
            var millis = sound.millis;
            int samplesPerChannel = (int) Math.ceil(sampleRate * (((double) millis) / 1000));
            int byteSize = (int) Math.ceil(byteRate * ((double) millis / 1000));
            byte[] audioPart = getAudioBytes(frequency, byteSize, samplesPerChannel);
            audioBuffer = ByteBuffer.allocate(audioBuffer.length + audioPart.length).put(audioBuffer).put(audioPart)
                    .array();
        }
        // ensure min audio size
        int minByteSize = (int) Math.ceil(byteRate * 0.5);
        if (audioBuffer.length < minByteSize) {
            // ensure min duration of half second, prevents issue with pulseaudio sink
            byte[] padBytes = new byte[minByteSize - audioBuffer.length];
            audioBuffer = ByteBuffer.allocate(minByteSize).put(padBytes).put(audioBuffer).array();
        }
        if (!bigEndian) {
            // for little endian add the RIFF header to the stream to increase compatibility
            return getAudioStreamWithRIFFHeader(audioBuffer);
        } else {
            return getAudioStream(audioBuffer);
        }
    }

    private double getSample(double frequency, int sampleNum) {
        return 0xfff * Math.sin(frequency * (2 * Math.PI) * sampleNum / sampleRate);
    }

    private ByteArrayAudioStream getAudioStreamWithRIFFHeader(byte[] audioBytes) throws IOException {
        var jAudioFormat = new javax.sound.sampled.AudioFormat(sampleRate, bitDepth, channels, true, bigEndian);
        AudioInputStream audioInputStreamTemp = new AudioInputStream(new ByteArrayInputStream(audioBytes), jAudioFormat,
                (long) Math.ceil(((double) audioBytes.length) / jAudioFormat.getFrameSize()));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        AudioSystem.write(audioInputStreamTemp, AudioFileFormat.Type.WAVE, outputStream);
        return getAudioStream(outputStream.toByteArray());
    }

    private ByteArrayAudioStream getAudioStream(byte[] audioBytes) {
        return new ByteArrayAudioStream(audioBytes, new AudioFormat(AudioFormat.CONTAINER_WAVE,
                AudioFormat.CODEC_PCM_SIGNED, bigEndian, bitDepth, bitRate, sampleRate, channels));
    }

    private byte[] getAudioBytes(double frequency, int byteSize, int samplesPerChannel) {
        var audioBuffer = ByteBuffer.allocate(byteSize);
        audioBuffer.order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < samplesPerChannel; i++) {
            short sample = (short) getSample(frequency, i);
            for (int c = 0; c < channels; c++) {
                switch (bitDepth) {
                    case 8:
                        audioBuffer.put((byte) (sample & 0xff));
                        break;
                    case 16:
                        audioBuffer.putShort(sample);
                        break;
                    case 24:
                        putInt24Bits(audioBuffer, ((int) sample) << 8);
                        break;
                    case 32:
                        audioBuffer.putInt(((int) sample) << 16);
                        break;
                }
            }
        }
        return audioBuffer.array();
    }

    private void putInt24Bits(ByteBuffer buffer, int value) {
        if (bigEndian) {
            buffer.put((byte) ((value >> 16) & 0xff));
            buffer.put((byte) ((value >> 8) & 0xff));
            buffer.put((byte) (value & 0xff));
        } else {
            buffer.put((byte) (value & 0xff));
            buffer.put((byte) ((value >> 8) & 0xff));
            buffer.put((byte) ((value >> 16) & 0xff));
        }
    }

    public static class Sound {
        private final double frequency;
        private final long millis;

        private Sound(double frequency, long millis) {
            this.frequency = frequency;
            this.millis = millis;
        }
    }

    public enum Note {
        B(List.of("B"), 493.88),
        Bb(List.of("A#", "Bb"), 466.16),
        A(List.of("A"), 440.0),
        Ab(List.of("G#", "Ab"), 415.30),
        G(List.of("G"), 392.0),
        Gb(List.of("F#", "Gb"), 369.99),
        F(List.of("F"), 349.23),
        E(List.of("E"), 329.63),
        Eb(List.of("D#", "Eb"), 311.13),
        D(List.of("D"), 293.66),
        Cb(List.of("C#", "Db"), 277.18),
        C(List.of("C"), 261.63);

        private final List<String> names;
        private final double frequency;

        Note(List<String> names, double frequency) {
            this.names = names;
            this.frequency = frequency;
        }

        public double getFrequency() {
            return frequency;
        }

        public static Optional<Note> fromString(String note) {
            return Arrays.stream(Note.values()).filter(note1 -> note1.names.contains(note)).findAny();
        }
    }
}
