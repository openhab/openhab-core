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
package org.openhab.core.audio.internal.transcode;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;

/**
 * An {@link AudioStream} implementation that delegates audio reading to an underlying {@link InputStream}
 * and retains audio format and stream ID metadata.
 * It supports attaching {@link Closeable}s to be called when the stream is closed.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class TranscodedAudioStream extends AudioStream {
    private final AudioFormat format;
    private final @Nullable String id;
    private final InputStream input;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Queue<Closeable> onCloseChain = new ConcurrentLinkedQueue<>();

    public TranscodedAudioStream(AudioFormat format, @Nullable String id, InputStream input) {
        this.format = Objects.requireNonNull(format);
        this.id = id;
        this.input = Objects.requireNonNull(input);
    }

    @Override
    public AudioFormat getFormat() {
        return format;
    }

    @Override
    public @Nullable String getId() {
        return id;
    }

    @Override
    public int read() throws IOException {
        if (closed.get()) {
            return -1;
        }
        return input.read();
    }

    @Override
    public int read(byte @Nullable [] b) throws IOException {
        if (closed.get()) {
            return -1;
        }
        return input.read(b);
    }

    @Override
    public int read(byte @Nullable [] b, int off, int len) throws IOException {
        if (closed.get()) {
            return -1;
        }
        return input.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        if (closed.get()) {
            return -1;
        }
        return input.skip(n);
    }

    @Override
    public int available() throws IOException {
        if (closed.get()) {
            return -1;
        }
        return input.available();
    }

    @Override
    public void close() throws IOException {
        if (closed.getAndSet(true)) {
            return;
        }
        IOException thrownException = null;

        try {
            input.close();
        } catch (IOException e) {
            thrownException = e;
        }

        for (Closeable action : onCloseChain) {
            try {
                action.close();
            } catch (IOException e) {
                if (thrownException == null) {
                    thrownException = e;
                } else {
                    thrownException.addSuppressed(e);
                }
            }
        }

        if (thrownException != null) {
            throw thrownException;
        }
    }

    /**
     * Adds a {@link Closeable} to the chain of {@link Closeable}s to be called when the stream is closed.
     * 
     * @param closeable the closeable to call when the stream is closed
     * @throws IllegalStateException if the stream is already closed
     */
    public void onClose(Closeable closeable) {
        if (closed.get()) {
            throw new IllegalStateException("Stream is already closed");
        }
        onCloseChain.add(closeable);
    }
}
