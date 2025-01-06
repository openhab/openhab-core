/*
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
package org.openhab.core.io.rest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.DateTimeType;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * This {@link InputStream} will stream {@link Stream}s as JSON one item at a time. This will reduce memory usage when
 * streaming large collections through the REST interface. The input stream creates one JSON representation at a time
 * from the top level elements of the stream. For best performance a flattened stream should be provided. Otherwise a
 * nested collections JSON representation will be fully transformed into memory.
 *
 * @author Henning Treu - Initial contribution
 * @author Jörg Sautter - Use as SequenceInputStream to simplify the logic
 */
@NonNullByDefault
public class Stream2JSONInputStream extends InputStream implements JSONInputStream {

    private static final Gson GSON = new GsonBuilder().setDateFormat(DateTimeType.DATE_PATTERN_WITH_TZ_AND_MS).create();

    private final InputStream stream;

    /**
     * Creates a new {@link Stream2JSONInputStream} backed by the given {@link Stream} source.
     *
     * @param source the {@link Stream} backing this input stream. Must not be null.
     */
    public Stream2JSONInputStream(Stream<?> source) {
        Iterator<String> iterator = source.map(e -> GSON.toJson(e)).iterator();

        Enumeration<InputStream> enumeration = new Enumeration<>() {
            private boolean consumed = false;
            private @Nullable InputStream next = toStream("[");

            @Override
            public boolean hasMoreElements() {
                return next != null || iterator.hasNext();
            }

            @Override
            public InputStream nextElement() {
                InputStream is;

                if (next != null) {
                    is = next;
                    if (!consumed && !iterator.hasNext()) {
                        next = toStream("]");
                        consumed = true;
                    } else {
                        next = null;
                    }
                    return is;
                }

                is = toStream(iterator.next());

                if (iterator.hasNext()) {
                    next = toStream(",");
                } else {
                    next = toStream("]");
                    consumed = true;
                }

                return is;
            }

            private static InputStream toStream(String data) {
                return new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
            }
        };
        stream = new SequenceInputStream(enumeration);
    }

    @Override
    public int read() throws IOException {
        return stream.read();
    }

    @Override
    public int read(byte @Nullable [] b, int off, int len) throws IOException {
        return stream.read(b, off, len);
    }

    @Override
    public long transferTo(OutputStream target) throws IOException {
        return stream.transferTo(target);
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
