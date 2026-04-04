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
package org.openhab.core.io.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.SseEventSink;

/**
 * Tests for {@link SseBroadcaster}.
 *
 * @author Holger Friedrich - Initial contribution
 */
@NonNullByDefault
public class SseBroadcasterTest {

    private final SseBroadcaster<String> broadcaster = new SseBroadcaster<>();
    private final OutboundSseEvent event = mock(OutboundSseEvent.class);

    @Test
    public void sendIfRemovesSinkThatFailsSynchronouslyAndContinues() {
        SseEventSink failingSink = mock(SseEventSink.class);
        when(failingSink.send(event)).thenThrow(new IllegalStateException("ServiceLocator has been shut down"));
        SseEventSink workingSink = mock(SseEventSink.class);
        when(workingSink.send(event)).thenReturn(CompletableFuture.completedFuture(null));

        @SuppressWarnings("unchecked")
        SseBroadcaster.Listener<String> listener = mock(SseBroadcaster.Listener.class);
        broadcaster.addListener(listener);
        broadcaster.add(failingSink, "failing");
        broadcaster.add(workingSink, "working");

        assertDoesNotThrow(() -> broadcaster.send(event));

        verify(workingSink).send(event);
        verify(failingSink).close();
        verify(listener).sseEventSinkRemoved(failingSink, "failing");
        assertNull(broadcaster.getInfo(failingSink));
        assertEquals("working", broadcaster.getInfo(workingSink));
    }

    @Test
    public void sendIfRemovesSinkThatFailsAsynchronously() {
        SseEventSink failingSink = mock(SseEventSink.class);
        when(failingSink.send(event)).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("closed")));

        broadcaster.add(failingSink, "failing");
        broadcaster.send(event);

        verify(failingSink).close();
        assertNull(broadcaster.getInfo(failingSink));
    }
}
