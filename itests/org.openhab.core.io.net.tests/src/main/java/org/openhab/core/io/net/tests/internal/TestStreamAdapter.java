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
            ByteBuffer buffer = data.frame().getByteBuffer();
            String text = StandardCharsets.UTF_8.decode(buffer).toString();
            data.release();
            if (last) {
                completable.complete(text);
                return;
            }
        }
    }
}
