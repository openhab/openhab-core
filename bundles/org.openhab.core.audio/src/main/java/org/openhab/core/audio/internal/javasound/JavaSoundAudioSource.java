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
package org.openhab.core.audio.internal.javasound;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.TargetDataLine;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioException;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.audio.AudioStream;
import org.osgi.service.component.annotations.Component;

/**
 * This is an AudioSource from an input channel of the host.
 *
 * @author Kelly Davis - Initial contribution and API
 * @author Kai Kreuzer - Refactored and stabilized
 * @author Miguel Álvarez - Share microphone line only under Windows OS
 *
 */
@NonNullByDefault
@Component(service = AudioSource.class, immediate = true)
public class JavaSoundAudioSource implements AudioSource {

    /**
     * Java Sound audio format
     */
    private final javax.sound.sampled.AudioFormat format = new javax.sound.sampled.AudioFormat(16000.0f, 16, 1, true,
            false);

    /**
     * AudioFormat of the JavaSoundAudioSource
     */
    private final AudioFormat audioFormat = convertAudioFormat(format);

    /**
     * Running on Windows OS
     */
    private final boolean windowsOS = System.getProperty("os.name", "Unknown").startsWith("Win");

    /**
     * TargetDataLine for sharing the mic on Windows OS due to limitations
     */
    private @Nullable TargetDataLine microphone;

    /**
     * Set for control microphone close on Windows OS
     */
    private final Set<Object> openStreamRefs = new HashSet<>();

    /**
     * Constructs a JavaSoundAudioSource
     */
    public JavaSoundAudioSource() {
    }

    private TargetDataLine initMicrophone(javax.sound.sampled.AudioFormat format) throws AudioException {
        try {
            TargetDataLine microphone = AudioSystem.getTargetDataLine(format);
            microphone.open(format);
            return microphone;
        } catch (Exception e) {
            throw new AudioException("Error creating the audio input stream: " + e.getMessage(), e);
        }
    }

    @Override
    public synchronized AudioStream getInputStream(AudioFormat expectedFormat) throws AudioException {
        if (!expectedFormat.isCompatible(audioFormat)) {
            throw new AudioException("Cannot produce streams in format " + expectedFormat);
        }
        // on OSs other than windows we can open multiple lines for the microphone
        if (!windowsOS) {
            return new JavaSoundInputStream(initMicrophone(format), audioFormat);
        }
        // on Windows OS we share the microphone line
        var ref = new Object();
        TargetDataLine microphoneDataLine;
        synchronized (openStreamRefs) {
            microphoneDataLine = this.microphone;
            if (microphoneDataLine == null) {
                microphoneDataLine = initMicrophone(format);
                this.microphone = microphoneDataLine;
            }
            openStreamRefs.add(ref);
        }
        return new JavaSoundInputStream(microphoneDataLine, audioFormat, () -> {
            synchronized (openStreamRefs) {
                var microphone = this.microphone;
                if (openStreamRefs.remove(ref) && openStreamRefs.isEmpty() && microphone != null) {
                    microphone.close();
                    this.microphone = null;
                }
            }
        });
    }

    @Override
    public String toString() {
        return "javasound";
    }

    /**
     * Converts a javax.sound.sampled.AudioFormat to a org.openhab.core.audio.AudioFormat
     *
     * @param audioFormat the AudioFormat to convert
     * @return The converted AudioFormat
     */
    private static AudioFormat convertAudioFormat(javax.sound.sampled.AudioFormat audioFormat) {
        String container = AudioFormat.CONTAINER_WAVE;

        String codec = audioFormat.getEncoding().toString();

        Boolean bigEndian = Boolean.valueOf(audioFormat.isBigEndian());

        int frameSize = audioFormat.getFrameSize(); // In bytes
        int bitsPerFrame = frameSize * 8;
        Integer bitDepth = ((AudioSystem.NOT_SPECIFIED == frameSize) ? null : Integer.valueOf(bitsPerFrame));

        float frameRate = audioFormat.getFrameRate();
        Integer bitRate = ((AudioSystem.NOT_SPECIFIED == frameRate) ? null
                : Integer.valueOf((int) (frameRate * bitsPerFrame)));

        float sampleRate = audioFormat.getSampleRate();
        Long frequency = ((AudioSystem.NOT_SPECIFIED == sampleRate) ? null : Long.valueOf((long) sampleRate));

        return new AudioFormat(container, codec, bigEndian, bitDepth, bitRate, frequency);
    }

    @Override
    public String getId() {
        return "javasound";
    }

    @Override
    public String getLabel(@Nullable Locale locale) {
        return "System Microphone";
    }

    @Override
    public Set<AudioFormat> getSupportedFormats() {
        return Set.of(audioFormat);
    }
}
