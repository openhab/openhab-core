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
package org.openhab.core.voice.internal.dialog;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.utils.AudioFormatMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The stream a keyword spotter reads from. It holds on to the last second of audio, letting speech recognition start
 * right where the keyword ended and then keep up as the spotter reads on.
 *
 * @author Dan Cunningham - Initial contribution
 */
@NonNullByDefault
class KSAudioStream extends AudioStream {
    private static final long NANOS_PER_SECOND = Duration.ofSeconds(1).toNanos();
    private static final Duration KEPT_AUDIO = Duration.ofSeconds(1);
    // once a branch falls this far behind, nobody is reading it anymore
    private static final Duration MAX_UNREAD_AUDIO = Duration.ofSeconds(60);

    private final Logger logger = LoggerFactory.getLogger(KSAudioStream.class);
    private final AudioStream stream;
    private final int frameSize;
    private final long bytesPerSecond;
    private final long maxUnreadBytes;
    private final byte[] recent;
    private final List<Branch> branches = new ArrayList<>();
    private long position;
    private boolean sourceEnded;

    /**
     * @param stream an audio stream in a {@link #isSupported(AudioFormat) supported} format
     */
    KSAudioStream(AudioStream stream) {
        AudioFormat format = stream.getFormat();
        if (!isSupported(format)) {
            throw new IllegalArgumentException("Unsupported audio format " + format);
        }
        this.stream = stream;
        this.frameSize = Objects.requireNonNull(format.getBitDepth(), "bit depth must not be null") / 8
                * Objects.requireNonNull(format.getChannels(), "channels must not be null");
        this.bytesPerSecond = Objects.requireNonNull(format.getFrequency(), "frequency must not be null") * frameSize;
        this.maxUnreadBytes = MAX_UNREAD_AUDIO.toSeconds() * bytesPerSecond;
        this.recent = new byte[(int) (KEPT_AUDIO.toSeconds() * bytesPerSecond)];
    }

    /**
     * Whether this is raw audio that a branch can cut at any sample. Containers like WAVE won't work: the header only
     * comes at the start of the stream, and a branch that starts later never sees it.
     */
    static boolean isSupported(@Nullable AudioFormat format) {
        if (format == null) {
            return false;
        }
        Integer bitDepth = format.getBitDepth();
        Integer channels = format.getChannels();
        Long frequency = format.getFrequency();
        return AudioFormat.CONTAINER_NONE.equals(format.getContainer())
                && AudioFormatMatcher.isPcmCodec(format.getCodec()) && bitDepth != null && bitDepth > 0
                && bitDepth % 8 == 0 && channels != null && channels > 0 && frequency != null && frequency > 0;
    }

    @Override
    public AudioFormat getFormat() {
        return stream.getFormat();
    }

    @Override
    public int read() throws IOException {
        byte[] b = new byte[1];
        return read(b, 0, 1) == -1 ? -1 : b[0] & 0xFF;
    }

    @Override
    public int read(byte @Nullable [] b, int off, int len) throws IOException {
        byte[] buffer = Objects.requireNonNull(b, "buffer must not be null");
        int read;
        try {
            read = stream.read(buffer, off, len);
        } catch (IOException e) {
            // leave the stream open in case the spotter keeps reading, and just end the current recognitions
            endBranches();
            throw e;
        }
        if (read == -1) {
            end();
        } else if (read > 0) {
            int count = read;
            synchronized (branches) {
                keep(buffer, off, count);
                branches.removeIf(branch -> !branch.append(buffer, off, count));
            }
        }
        return read;
    }

    @Override
    public int available() throws IOException {
        return stream.available();
    }

    @Override
    public void close() throws IOException {
        end();
        stream.close();
    }

    /**
     * Opens a stream that starts with some of the audio already read, up to a second, and then gets everything read
     * from this stream after that.
     *
     * @param audioBefore how much of the already read audio the new stream starts with
     */
    AudioStream branch(Duration audioBefore) {
        Duration rewind = audioBefore.isNegative() ? Duration.ZERO
                : audioBefore.compareTo(KEPT_AUDIO) > 0 ? KEPT_AUDIO : audioBefore;
        long rewindBytes = rewind.toNanos() * bytesPerSecond / NANOS_PER_SECOND;
        Branch branch = new Branch();
        synchronized (branches) {
            long start = position - Math.min(position, rewindBytes);
            // start on a whole sample, and not past what's been read
            start = Math.min(start + Math.floorMod(-start, frameSize), position - Math.floorMod(position, frameSize));
            for (long next = start; next < position;) {
                int offset = (int) (next % recent.length);
                int len = (int) Math.min(position - next, recent.length - offset);
                branch.append(recent, offset, len);
                next += len;
            }
            if (sourceEnded) {
                branch.end();
            } else {
                branches.add(branch);
            }
            return branch;
        }
    }

    private void keep(byte[] b, int off, int len) {
        int skipped = Math.max(0, len - recent.length);
        for (int done = skipped; done < len;) {
            int offset = (int) ((position + done) % recent.length);
            int count = Math.min(len - done, recent.length - offset);
            System.arraycopy(b, off + done, recent, offset, count);
            done += count;
        }
        position += len;
    }

    private void end() {
        synchronized (branches) {
            sourceEnded = true;
            endBranches();
        }
    }

    private void endBranches() {
        synchronized (branches) {
            branches.forEach(Branch::end);
            branches.clear();
        }
    }

    private class Branch extends AudioStream {
        private final ArrayDeque<byte[]> chunks = new ArrayDeque<>();
        private int chunkOffset;
        private long unread;
        private boolean ended;

        @Override
        public AudioFormat getFormat() {
            return KSAudioStream.this.getFormat();
        }

        /**
         * @return false when the branch has ended and takes no more audio
         */
        synchronized boolean append(byte[] b, int off, int len) {
            if (ended) {
                return false;
            }
            if (unread + len > maxUnreadBytes) {
                logger.debug("Ending a speech recognition stream that is not read anymore");
                end();
                return false;
            }
            chunks.add(Arrays.copyOfRange(b, off, off + len));
            unread += len;
            notifyAll();
            return true;
        }

        synchronized void end() {
            ended = true;
            notifyAll();
        }

        @Override
        public int read() throws IOException {
            byte[] b = new byte[1];
            return read(b, 0, 1) == -1 ? -1 : b[0] & 0xFF;
        }

        @Override
        public synchronized int read(byte @Nullable [] b, int off, int len) throws IOException {
            Objects.checkFromIndexSize(off, len, Objects.requireNonNull(b, "buffer must not be null").length);
            if (len == 0) {
                return 0;
            }
            while (chunks.isEmpty()) {
                if (ended) {
                    return -1;
                }
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedIOException("Interrupted while waiting for audio");
                }
            }
            int read = 0;
            while (read < len && !chunks.isEmpty()) {
                byte[] chunk = chunks.element();
                int count = Math.min(len - read, chunk.length - chunkOffset);
                System.arraycopy(chunk, chunkOffset, b, off + read, count);
                read += count;
                chunkOffset += count;
                if (chunkOffset == chunk.length) {
                    chunks.remove();
                    chunkOffset = 0;
                }
            }
            unread -= read;
            return read;
        }

        @Override
        public synchronized int available() {
            return (int) Math.min(unread, Integer.MAX_VALUE);
        }

        @Override
        public void close() {
            synchronized (branches) {
                branches.remove(this);
            }
            synchronized (this) {
                end();
                chunks.clear();
                unread = 0;
            }
        }
    }
}
