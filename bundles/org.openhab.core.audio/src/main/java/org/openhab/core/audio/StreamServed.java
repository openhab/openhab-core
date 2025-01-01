/**
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Streams served by the AudioHTTPServer.
 *
 * @author Gwendal Roulleau - Initial contribution
 */
@NonNullByDefault
public record StreamServed(String url, AudioStream audioStream, AtomicInteger currentlyServedStream, AtomicLong timeout,
        boolean multiTimeStream, CompletableFuture<@Nullable Void> playEnd) {
}
