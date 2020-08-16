/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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

import javax.sound.sampled.TargetDataLine;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;

/**
 * This is an AudioStream from a Java sound API input
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
@NonNullByDefault
public class JavaSoundInputStream extends AudioStream {

    /**
     * TargetDataLine for the input
     */
    private final TargetDataLine input;
    private final AudioFormat format;

    /**
     * Constructs a JavaSoundInputStream with the passed input
     *
     * @param input The mic which data is pulled from
     */
    public JavaSoundInputStream(TargetDataLine input, AudioFormat format) {
        this.format = format;
        this.input = input;
        this.input.start();
    }

    @Override
    public int read() throws IOException {
        byte[] b = new byte[1];

        int bytesRead = read(b);

        if (-1 == bytesRead) {
            return bytesRead;
        }

        Byte bb = Byte.valueOf(b[0]);
        return bb.intValue();
    }

    @SuppressWarnings("null")
    @Override
    public int read(byte @Nullable [] b) throws IOException {
        return input.read(b, 0, b.length);
    }

    @Override
    public int read(byte @Nullable [] b, int off, int len) throws IOException {
        return input.read(b, off, len);
    }

    @Override
    public void close() throws IOException {
        input.close();
    }

    @Override
    public AudioFormat getFormat() {
        return format;
    }
}
