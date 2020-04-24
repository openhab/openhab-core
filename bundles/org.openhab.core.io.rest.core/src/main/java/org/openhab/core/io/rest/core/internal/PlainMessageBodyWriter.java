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
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

/**
 * A message body writer for plain text.
 *
 * @author Markus Rathgeb - Initial contribution
 */
public class PlainMessageBodyWriter<T> implements MessageBodyWriter<T> {

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
        entityStream.write(object.toString().getBytes(StandardCharsets.UTF_8));
    }
}
