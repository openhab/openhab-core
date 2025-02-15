/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an implementation of an {@link AudioStream} used to transmit raw audio data to a sink.
 *
 * It just pipes the audio through it, the default pipe size is equal to 0.5 seconds of audio,
 * the implementation locks if you set a pipe size lower to the byte length used to write.
 *
 * In order to support audio multiplex out of the box you should create a {@link PipedAudioStream.Group} instance
 * which can be used to create the {@link PipedAudioStream} connected to it and then write to all of them though the
 * group.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@NonNullByDefault
public class PipedAudioStream extends AudioStream {
    private final AudioFormat format;
    private final PipedInputStream pipedInput;
    private final PipedOutputStream pipedOutput;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final LinkedList<Runnable> onCloseChain = new LinkedList<>();

    protected PipedAudioStream(AudioFormat format, int pipeSize, PipedOutputStream outputStream) throws IOException {
        this.pipedOutput = outputStream;
        this.pipedInput = new PipedInputStream(outputStream, pipeSize);
        this.format = format;
    }

    @Override
    public AudioFormat getFormat() {
        return this.format;
    }

    @Override
    public int read() throws IOException {
        if (closed.get()) {
            return -1;
        }
        return pipedInput.read();
    }

    @Override
    public int read(byte @Nullable [] b) throws IOException {
        if (closed.get()) {
            return -1;
        }
        return pipedInput.read(b);
    }

    @Override
    public int read(byte @Nullable [] b, int off, int len) throws IOException {
        if (closed.get()) {
            return -1;
        }
        return pipedInput.read(b, off, len);
    }

    @Override
    public void close() throws IOException {
        if (closed.getAndSet(true)) {
            return;
        }
        if (!this.onCloseChain.isEmpty()) {
            this.onCloseChain.forEach(Runnable::run);
            this.onCloseChain.clear();
        }
        pipedOutput.close();
        pipedInput.close();
    }

    /**
     * Add a new handler that will be executed on stream close.
     * It will be chained to the previous handler if any, and executed in order.
     * 
     * @param onClose block to run on stream close
     */
    public void onClose(Runnable onClose) {
        this.onCloseChain.add(onClose);
    }

    protected PipedOutputStream getOutputStream() {
        return pipedOutput;
    }

    /**
     * Creates a new piped stream group used to open new streams and write data to them.
     *
     * Internal pipe size is 0.5s.
     *
     * @param format the audio format of the group audio streams
     * @return a group instance
     */
    public static Group newGroup(AudioFormat format) {
        int pipeSize = Math.round(( //
        (float) Objects.requireNonNull(format.getFrequency()) * //
                (float) Objects.requireNonNull(format.getBitDepth()) * //
                (float) Objects.requireNonNull(format.getChannels()) //
        ) / 2f);
        return new Group(format, pipeSize);
    }

    /**
     * Creates a new piped stream group used to open new streams and write data to them.
     *
     * @param format the audio format of the group audio streams
     * @param pipeSize the pipe size of the created streams
     * @return a piped stream group instance
     */
    public static Group newGroup(AudioFormat format, int pipeSize) {
        return new Group(format, pipeSize);
    }

    /**
     * The {@link PipedAudioStream.Group} is an {@link OutputStream} implementation that can be use to
     * create one or more {@link PipedAudioStream} instances and write to them at once.
     *
     * The created {@link PipedAudioStream} instances are removed from the group when closed.
     */
    public static class Group extends OutputStream {
        private final int pipeSize;
        private final AudioFormat format;
        private final ConcurrentLinkedQueue<PipedAudioStream> openPipes = new ConcurrentLinkedQueue<>();
        private final Logger logger = LoggerFactory.getLogger(Group.class);

        protected Group(AudioFormat format, int pipeSize) {
            this.pipeSize = pipeSize;
            this.format = format;
        }

        /**
         * Creates a new {@link PipedAudioStream} connected to the group.
         * The stream unregisters itself from the group on close.
         * 
         * @return a new {@link PipedAudioStream} to pipe data written to the group
         * @throws IOException when unable to create the stream
         */
        public PipedAudioStream getAudioStreamInGroup() throws IOException {
            var pipedOutput = new PipedOutputStream();
            var audioStream = new PipedAudioStream(format, pipeSize, pipedOutput);
            if (!openPipes.add(audioStream)) {
                audioStream.close();
                throw new IOException("Unable to add new piped stream to group");
            }
            audioStream.onClose(() -> {
                if (!openPipes.remove(audioStream)) {
                    logger.warn("Trying to remove an unregistered stream, this is not expected");
                }
            });
            return audioStream;
        }

        /**
         * Returns true if this group has no streams connected.
         *
         * @return true if this group has no streams connected
         */
        public boolean isEmpty() {
            return openPipes.isEmpty();
        }

        /**
         * Returns the number of streams connected.
         *
         * @return the number of streams connected
         */
        public int size() {
            return openPipes.size();
        }

        @Override
        public void write(byte @Nullable [] b, int off, int len) {
            synchronized (openPipes) {
                for (var pipe : openPipes) {
                    try {
                        pipe.getOutputStream().write(b, off, len);
                    } catch (InterruptedIOException e) {
                        logger.warn("InterruptedIOException while writing to pipe: {}", e.getMessage());
                    } catch (IOException e) {
                        logger.warn("IOException while writing to pipe: {}", e.getMessage());
                    } catch (RuntimeException e) {
                        logger.warn("RuntimeException while writing to pipe: {}", e.getMessage());
                    }
                }
            }
        }

        @Override
        public void write(int b) throws IOException {
            synchronized (openPipes) {
                for (var pipe : openPipes) {
                    try {
                        pipe.getOutputStream().write(b);
                    } catch (InterruptedIOException e) {
                        logger.warn("InterruptedIOException while writing to pipe: {}", e.getMessage());
                    } catch (IOException e) {
                        logger.warn("IOException while writing to pipe: {}", e.getMessage());
                    } catch (RuntimeException e) {
                        logger.warn("RuntimeException while writing to pipe: {}", e.getMessage());
                    }
                }
            }
        }

        @Override
        public void write(byte @Nullable [] bytes) {
            synchronized (openPipes) {
                for (var pipe : openPipes) {
                    try {
                        pipe.getOutputStream().write(bytes);
                    } catch (InterruptedIOException e) {
                        logger.warn("InterruptedIOException on pipe flush: {}", e.getMessage());
                    } catch (IOException e) {
                        logger.warn("IOException on pipe flush: {}", e.getMessage());
                    } catch (RuntimeException e) {
                        logger.warn("RuntimeException on pipe flush: {}", e.getMessage());
                    }
                }
            }
        }

        @Override
        public void flush() {
            synchronized (openPipes) {
                for (var pipe : openPipes) {
                    try {
                        pipe.getOutputStream().flush();
                    } catch (InterruptedIOException e) {
                        logger.warn("InterruptedIOException while writing to pipe: {}", e.getMessage());
                    } catch (IOException e) {
                        logger.warn("IOException while writing to pipe: {}", e.getMessage());
                    } catch (RuntimeException e) {
                        logger.warn("RuntimeException while writing to pipe: {}", e.getMessage());
                    }
                }
            }
        }

        @Override
        public void close() {
            synchronized (openPipes) {
                for (var pipe : openPipes) {
                    try {
                        pipe.close();
                    } catch (InterruptedIOException e) {
                        logger.warn("InterruptedIOException closing pipe: {}", e.getMessage());
                    } catch (IOException e) {
                        logger.warn("IOException closing pipe: {}", e.getMessage());
                    } catch (RuntimeException e) {
                        logger.warn("RuntimeException closing pipe: {}", e.getMessage());
                    }
                }
                openPipes.clear();
            }
        }
    }
}
