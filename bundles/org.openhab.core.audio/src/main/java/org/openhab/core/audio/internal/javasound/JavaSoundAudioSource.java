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
package org.openhab.core.audio.internal.javasound;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.TargetDataLine;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioException;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.common.ThreadPoolManager;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final Logger logger = LoggerFactory.getLogger(JavaSoundAudioSource.class);

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
     * Set for control microphone sharing on Windows OS
     */
    private final ConcurrentLinkedQueue<PipedOutputStream> openStreamRefs = new ConcurrentLinkedQueue<>();

    /**
     * Task for writing microphone data to each of the open sources on Windows OS
     */
    private @Nullable Future<?> pipeWriteTask;

    private final ScheduledExecutorService executor;

    /**
     * Constructs a JavaSoundAudioSource
     */
    public JavaSoundAudioSource() {
        executor = ThreadPoolManager.getScheduledPool("OH-core-javasound-source");
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
    public AudioStream getInputStream(AudioFormat expectedFormat) throws AudioException {
        if (!expectedFormat.isCompatible(audioFormat)) {
            throw new AudioException("Cannot produce streams in format " + expectedFormat);
        }
        // on OSs other than windows we can open multiple lines for the microphone
        if (!windowsOS) {
            TargetDataLine microphone = initMicrophone(format);
            var inputStream = new JavaSoundInputStream(new InputStream() {
                @Override
                public int read() throws IOException {
                    return microphone.available();
                }

                @Override
                public int read(byte @Nullable [] b, int off, int len) throws IOException {
                    return microphone.read(b, off, len);
                }

                @Override
                public void close() throws IOException {
                    microphone.close();
                }
            }, audioFormat);
            microphone.start();
            return inputStream;
        }
        // on Windows OS we share the microphone line
        synchronized (openStreamRefs) {
            TargetDataLine microphone = this.microphone;
            if (microphone == null) {
                microphone = initMicrophone(format);
                this.microphone = microphone;
            }
            var pipedOutputStream = new PipedOutputStream();
            PipedInputStream pipedInputStream;
            try {
                pipedInputStream = new PipedInputStream(pipedOutputStream, 1024 * 10) {
                    @Override
                    public void close() throws IOException {
                        unregisterPipe(pipedOutputStream);
                        super.close();
                    }
                };
            } catch (IOException ie) {
                throw new AudioException("Cannot open stream pipe: " + ie.getMessage());
            }
            openStreamRefs.add(pipedOutputStream);
            var inputStream = new JavaSoundInputStream(pipedInputStream, audioFormat);
            microphone.start();
            startPipeWrite();
            return inputStream;
        }
    }

    private void startPipeWrite() {
        if (this.pipeWriteTask == null) {
            this.pipeWriteTask = executor.submit(() -> {
                int lengthRead;
                byte[] buffer = new byte[1024];
                while (!openStreamRefs.isEmpty()) {
                    TargetDataLine stream = this.microphone;
                    if (stream != null) {
                        try {
                            lengthRead = stream.read(buffer, 0, buffer.length);
                            for (PipedOutputStream output : openStreamRefs) {
                                try {
                                    output.write(buffer, 0, lengthRead);
                                    if (openStreamRefs.contains(output)) {
                                        output.flush();
                                    }
                                } catch (InterruptedIOException e) {
                                    if (openStreamRefs.isEmpty()) {
                                        // task has been ended while writing
                                        return;
                                    }
                                    logger.warn("InterruptedIOException while writing to source pipe: {}",
                                            e.getMessage());
                                } catch (IOException e) {
                                    logger.warn("IOException while writing to source pipe: {}", e.getMessage());
                                } catch (RuntimeException e) {
                                    logger.warn("RuntimeException while writing to source pipe: {}", e.getMessage());
                                }
                            }
                        } catch (RuntimeException e) {
                            logger.warn("RuntimeException while reading from JavaSound source: {}", e.getMessage());
                        }
                    } else {
                        logger.warn("Unable access to microphone stream");
                    }
                }
                this.pipeWriteTask = null;
            });
        }
    }

    private void unregisterPipe(PipedOutputStream pipedOutputStream) {
        synchronized (openStreamRefs) {
            openStreamRefs.remove(pipedOutputStream);
            try {
                Thread.sleep(0);
            } catch (InterruptedException ignored) {
            }
            if (openStreamRefs.isEmpty()) {
                Future<?> pipeWriteTask = this.pipeWriteTask;
                if (pipeWriteTask != null) {
                    pipeWriteTask.cancel(true);
                    this.pipeWriteTask = null;
                }
                TargetDataLine microphone = this.microphone;
                if (microphone != null) {
                    microphone.close();
                    this.microphone = null;
                }
            }
            try {
                pipedOutputStream.close();
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public String toString() {
        return "javasound";
    }

    /**
     * Converts a javax.sound.sampled.AudioFormat to an org.openhab.core.audio.AudioFormat
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
