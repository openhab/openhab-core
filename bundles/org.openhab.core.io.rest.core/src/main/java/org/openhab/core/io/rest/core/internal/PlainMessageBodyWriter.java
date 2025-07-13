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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A message body writer for plain text.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public class PlainMessageBodyWriter<T> implements MessageBodyWriter<T> {

    @Override
    public long getSize(final @Nullable T object, final @Nullable Class<?> type, final @Nullable Type genericType,
            final Annotation @Nullable [] annotations, final @Nullable MediaType mediaType) {
        return -1;
    }

    @Override
    public boolean isWriteable(@Nullable final Class<?> type, final @Nullable Type genericType,
            final Annotation @Nullable [] annotations, final @Nullable MediaType mediaType) {
        return true;
    }

    @Override
    @NonNullByDefault({})
    public void writeTo(final T object, final Class<?> type, final Type genericType, final Annotation[] annotations,
            final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
            final OutputStream entityStream) throws IOException, WebApplicationException {
        if (object == null) {
            return;
        }
        entityStream.write(object.toString().getBytes(StandardCharsets.UTF_8));
    }
}
