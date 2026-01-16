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
package org.openhab.core.audio.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioFormat;

/**
 * Some utility methods for sink
 *
 * @author Gwendal Roulleau - Initial contribution
 *
 */
@NonNullByDefault
public interface AudioSinkUtils {

    /**
     * Transfers data from an input stream to an output stream and computes on the fly its duration
     *
     * @param in the input stream giving audio data ta play
     * @param out the output stream receiving data to play
     * @return the timestamp (from System.nanoTime) when the sound should be fully played. Returns null if computing
     *         time fails.
     * @throws IOException if reading from the stream or writing to the stream failed
     */
    @Nullable
    Long transferAndAnalyzeLength(InputStream in, OutputStream out, AudioFormat audioFormat) throws IOException;
}
