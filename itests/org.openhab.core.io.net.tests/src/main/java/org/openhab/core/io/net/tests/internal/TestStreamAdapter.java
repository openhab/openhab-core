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
package org.openhab.core.io.net.tests.internal;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.http2.api.Stream;

/**
 * HTTP/2 stream adapter for org.openhab.core.io.net tests.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class TestStreamAdapter implements Stream.Listener {
    public final CompletableFuture<String> completable = new CompletableFuture<>();

    private final ByteArrayOutputStream content = new ByteArrayOutputStream();

    @Override
    public void onDataAvailable(@Nullable Stream stream) {
        if (stream == null) {
            return;
        }
        while (true) {
            Stream.Data data = stream.readData();
            if (data == null) {
                stream.demand();
                return;
            }
            boolean last = data.frame().isEndStream();
            // Collect the bytes of all frames and decode them only once the stream ended: a response can arrive in
            // several frames, the last one often carrying no content at all, and a multi byte character can be split
            // across two frames.
            ByteBuffer buffer = data.frame().getByteBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            content.writeBytes(bytes);
            data.release();
            if (last) {
                completable.complete(content.toString(StandardCharsets.UTF_8));
                return;
            }
        }
    }
}
