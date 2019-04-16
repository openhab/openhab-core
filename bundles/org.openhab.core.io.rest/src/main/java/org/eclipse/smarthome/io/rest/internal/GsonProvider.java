/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.io.rest.internal;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import javax.ws.rs.Consumes;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsExtension;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.stream.JsonWriter;

/**
 * JSON reader/writer using GSON.
 *
 * @author Simon Kaufmann - initial contribution and API
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Component(immediate = true)
@JaxrsExtension
public class GsonProvider<T> implements MessageBodyReader<T>, MessageBodyWriter<T> {

    private final Gson gson;

    public GsonProvider() {
        gson = new GsonBuilder().setDateFormat(DateTimeType.DATE_PATTERN_WITH_TZ_AND_MS).create();
    }

    @Override
    public long getSize(T t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    void writeFromIterator(Iterator<?> data, OutputStream entityStream) {
        try (JsonWriter jsonWriter = new JsonWriter(new BufferedWriter(new OutputStreamWriter(entityStream)))) {
            jsonWriter.beginArray();
            while (data.hasNext()) {
                try {
                    Object entity = data.next();
                    gson.toJson(entity, entity.getClass(), jsonWriter);
                    jsonWriter.flush();
                } catch (NoSuchElementException ignored) {
                }
            }
            jsonWriter.endArray();
        } catch (IOException | JsonIOException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @Override
    public void writeTo(T object, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        if (object instanceof Collection<?>) {
            writeFromIterator(((Collection<?>) object).iterator(), entityStream);
        } else if (object instanceof Stream<?>) {
            writeFromIterator(((Stream<?>) object).iterator(), entityStream);
        } else {
            entityStream.write(gson.toJson(object).getBytes(StandardCharsets.UTF_8));
            entityStream.flush();
        }
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public T readFrom(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        try (InputStreamReader reader = new InputStreamReader(entityStream, StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, type);
        }
    }
}
