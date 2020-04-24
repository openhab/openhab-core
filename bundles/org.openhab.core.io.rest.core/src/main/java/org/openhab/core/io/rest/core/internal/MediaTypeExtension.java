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

import static org.osgi.service.component.annotations.ServiceScope.PROTOTYPE;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.openhab.core.io.rest.RESTConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsExtension;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsMediaType;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * A media type extension for all supported media types.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@Component(scope = PROTOTYPE)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JaxrsExtension
@JaxrsMediaType({ MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN })
public class MediaTypeExtension<T> implements MessageBodyReader<T>, MessageBodyWriter<T> {

    private static String mediaTypeWithoutParams(final MediaType mediaType) {
        return mediaType.getType() + "/" + mediaType.getSubtype();
    }

    private final Map<String, MessageBodyReader<T>> readers = new HashMap<>();
    private final Map<String, MessageBodyWriter<T>> writers = new HashMap<>();

    /**
     * Constructor.
     */
    public MediaTypeExtension() {
        final Gson gson = new GsonBuilder().create();
        readers.put(mediaTypeWithoutParams(MediaType.APPLICATION_JSON_TYPE), new GsonMessageBodyReader<>(gson));
        readers.put(mediaTypeWithoutParams(MediaType.TEXT_PLAIN_TYPE), new PlainMessageBodyReader<>());
        writers.put(mediaTypeWithoutParams(MediaType.APPLICATION_JSON_TYPE), new GsonMessageBodyWriter<>(gson));
        writers.put(mediaTypeWithoutParams(MediaType.TEXT_PLAIN_TYPE), new PlainMessageBodyWriter<>());
    }

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
            final MediaType mediaType) {
        final MessageBodyWriter<T> writer = writers.get(mediaTypeWithoutParams(mediaType));
        return writer != null && writer.isWriteable(type, genericType, annotations, mediaType);
    }

    @Override
    public void writeTo(final T object, final Class<?> type, final Type genericType, final Annotation[] annotations,
            final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
            final OutputStream entityStream) throws IOException, WebApplicationException {
        final MessageBodyWriter<T> writer = writers.get(mediaTypeWithoutParams(mediaType));
        if (writer != null) {
            writer.writeTo(object, type, genericType, annotations, mediaType, httpHeaders, entityStream);
        } else {
            throw new InternalServerErrorException("unsupported media type");
        }
    }

    @Override
    public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations,
            final MediaType mediaType) {
        final MessageBodyReader<T> reader = readers.get(mediaTypeWithoutParams(mediaType));
        return reader != null && reader.isReadable(type, genericType, annotations, mediaType);
    }

    @Override
    public T readFrom(final Class<T> type, final Type genericType, final Annotation[] annotations,
            final MediaType mediaType, final MultivaluedMap<String, String> httpHeaders, final InputStream entityStream)
            throws IOException, WebApplicationException {
        final MessageBodyReader<T> reader = readers.get(mediaTypeWithoutParams(mediaType));
        if (reader != null) {
            return reader.readFrom(type, genericType, annotations, mediaType, httpHeaders, entityStream);
        } else {
            throw new InternalServerErrorException("unsupported media type");
        }
    }
}
