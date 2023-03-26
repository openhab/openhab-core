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
package org.openhab.core.io.net.tests.internal;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.http2.api.Stream;
import org.eclipse.jetty.http2.frames.DataFrame;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

/**
 * HTTP/2 stream adapter for org.openhab.core.io.net tests.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@WebSocket
@NonNullByDefault
public class TestStreamAdapter extends Stream.Listener.Adapter {
    public final CompletableFuture<String> completable = new CompletableFuture<>();

    @Override
    public void onData(@Nullable Stream stream, @Nullable DataFrame frame, @Nullable Callback callback) {
        assertNotNull(stream);
        assertNotNull(frame);
        assertTrue(frame.isEndStream());
        completable.complete(StandardCharsets.UTF_8.decode(frame.getData()).toString());
    }
}
