/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.audio;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This is an implementation of a {@link FixedLengthAudioStream}, which is based on a simple byte array.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
public class ByteArrayAudioStream extends FixedLengthAudioStream {

    private byte[] bytes;
    private AudioFormat format;
    private ByteArrayInputStream stream;

    public ByteArrayAudioStream(byte[] bytes, AudioFormat format) {
        this.bytes = bytes;
        this.format = format;
        this.stream = new ByteArrayInputStream(bytes);
    }

    @Override
    public AudioFormat getFormat() {
        return format;
    }

    @Override
    public int read() throws IOException {
        return stream.read();
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }

    @Override
    public long length() {
        return bytes.length;
    }

    @Override
    public InputStream getClonedStream() {
        return new ByteArrayAudioStream(bytes, format);
    }

}
