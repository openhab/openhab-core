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
package org.openhab.core.io.rest.core.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.MessageBodyWriter;

import org.openhab.core.io.rest.JSONInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * A message body writer for JSON using GSON.
 *
 * @author Markus Rathgeb - Initial contribution
 */
public class GsonMessageBodyWriter<T> implements MessageBodyWriter<T> {

    private final Logger logger = LoggerFactory.getLogger(GsonMessageBodyWriter.class);

    private final Gson gson;

    /**
     * Constructor.
     *
     * @param gson the GSON object to use
     */
    public GsonMessageBodyWriter(final Gson gson) {
        this.gson = gson;
    }

    @Override
    public long getSize(final T object, final Class<?> type, final Type genericType, final Annotation[] annotations,
            final MediaType mediaType) {
        return -1;
    }

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
            final MediaType mediaType) {
        return true;
    }

    @Override
    public void writeTo(final T object, final Class<?> type, final Type genericType, final Annotation[] annotations,
            final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
            final OutputStream entityStream) throws IOException, WebApplicationException {
        if (object == null) {
            return;
        }

        // Log a message if a response builder is received.
        if (object instanceof ResponseBuilder) {
            logger.error(
                    "A REST endpoint returns a ResponseBuilder object. This is mostly wrong and the call to \"build()\" is missing. Please report or fix it. Got: {}",
                    object);
        }

        if (object instanceof InputStream && object instanceof JSONInputStream) {
            ((InputStream) object).transferTo(entityStream);
        } else {
            entityStream.write(gson.toJson(object).getBytes(StandardCharsets.UTF_8));
        }

        // Flush the stream.
        // Keep this code as it has been present before,
        // but I don't think this needs to be done in the message body writer itself.
        entityStream.flush();
    }
}
