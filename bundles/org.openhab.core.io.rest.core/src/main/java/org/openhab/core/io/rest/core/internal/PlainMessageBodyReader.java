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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A message body reader for plain text.
 *
 * @author Markus Rathgeb - Initial contribution
 */
public class PlainMessageBodyReader<T> implements MessageBodyReader<T> {

    private final Logger logger = LoggerFactory.getLogger(PlainMessageBodyReader.class);

    @Override
    public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations,
            final MediaType mediaType) {
        return true;
    }

    @Override
    public T readFrom(final Class<T> type, final Type genericType, final Annotation[] annotations,
            final MediaType mediaType, final MultivaluedMap<String, String> httpHeaders, final InputStream entityStream)
            throws IOException, WebApplicationException {
        final byte[] data = data(entityStream);
        logger.debug("Received: type={}, genericType={}, annotations={}, mediaType={}, httpHeaders={}", type,
                genericType, annotations, mediaType, httpHeaders);
        if (type.equals(String.class) || genericType.equals(String.class)) {
            return (T) new String(data, StandardCharsets.UTF_8);
        } else if (type.equals(byte[].class) || genericType.equals(byte[].class)) {
            return (T) data;
        } else if (type.equals(Byte[].class) || genericType.equals(Byte[].class)) {
            final Byte[] dataB = new Byte[data.length];
            for (int i = 0; i < data.length; ++i) {
                dataB[i] = data[i];
            }
            return (T) dataB;
        } else {
            throw new InternalServerErrorException(
                    String.format("Cannot assign text plain to type \"%s\", generic type: \"%s\".", type, genericType));
        }
    }

    private static byte[] data(final InputStream is) throws IOException {
        final ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
        int read;
        final byte[] buffer = new byte[1024];
        while ((read = is.read(buffer)) != -1) {
            outputBytes.write(buffer, 0, read);
        }
        return outputBytes.toByteArray();
    }
}
