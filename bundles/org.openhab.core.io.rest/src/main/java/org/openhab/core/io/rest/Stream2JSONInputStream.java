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
package org.openhab.core.io.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
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
 */
public class Stream2JSONInputStream extends InputStream implements JSONInputStream {

    private final Iterator<String> iterator;

    private InputStream jsonElementStream;

    private boolean firstIteratorElement;

    private final Gson gson = new GsonBuilder().setDateFormat(DateTimeType.DATE_PATTERN_WITH_TZ_AND_MS).create();

    /**
     * Creates a new {@link Stream2JSONInputStream} backed by the given {@link Stream} source.
     *
     * @param source the {@link Stream} backing this input stream. Must not be null.
     * @throws IllegalArgumentException in case the source is null.
     */
    public Stream2JSONInputStream(Stream<?> source) {
        if (source == null) {
            throw new IllegalArgumentException("The source must not be null!");
        }

        iterator = source.map(e -> gson.toJson(e)).iterator();
        jsonElementStream = IOUtils.toInputStream("");
        firstIteratorElement = true;
    }

    @Override
    public int read() throws IOException {
        int result = jsonElementStream.read();

        if (result == -1) { // the current JSON element was completely streamed
            if (finished()) { // we are done streaming the collection
                return -1;
            }

            fillBuffer(); // get the next element into a new jsonElementStream
            result = jsonElementStream.read();
        }

        return result;
    }

    @Override
    public void close() throws IOException {
        jsonElementStream.close();
    }

    private void fillBuffer() {
        String prefix;
        if (firstIteratorElement) {
            prefix = "[";
            firstIteratorElement = false;
        } else {
            prefix = ",";
        }

        String entity = iterator.hasNext() ? iterator.next() : "";

        String postfix = "";
        if (!iterator.hasNext()) {
            postfix = "]";
        }

        IOUtils.closeQuietly(jsonElementStream);
        jsonElementStream = IOUtils.toInputStream(prefix + entity + postfix);
    }

    private boolean finished() {
        return !firstIteratorElement && !iterator.hasNext();
    }
}
