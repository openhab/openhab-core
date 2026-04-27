/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.model.yaml.internal.util;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * A Jackson deserializer for {@link Date} that accepts a wide range of formats:
 * <ul>
 * <li>Epoch milliseconds (numeric)</li>
 * <li>ISO 8601 (e.g. {@code 2021-03-08T19:41:03Z})</li>
 * <li>Legacy openHAB UI format (e.g. {@code Mar 8, 2021, 7:41:03 PM})</li>
 * </ul>
 *
 * @author Jimmy Tanagra - Initial contribution
 */
public class FlexibleDateDeserializer extends StdDeserializer<Date> {

    private static final long serialVersionUID = 1L;

    private static final List<String> STRING_PATTERNS = List.of( //
            "MMM d, yyyy, h:mm:ss a", // legacy openHAB UI export format
            "MMM d, yyyy, hh:mm:ss a", // zero-padded hour variant
            "yyyy-MM-dd'T'HH:mm:ssXXX", // ISO 8601 with offset
            "yyyy-MM-dd'T'HH:mm:ss", // ISO 8601 local
            "yyyy-MM-dd HH:mm:ss", // common SQL-like format
            "yyyy-MM-dd" // date only
    );

    public FlexibleDateDeserializer() {
        super(Date.class);
    }

    @Override
    public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_NUMBER_INT) {
            return new Date(p.getLongValue());
        }

        String text = p.getText().trim();

        // Try each string pattern
        for (String pattern : STRING_PATTERNS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);
                sdf.setLenient(false);
                return sdf.parse(text);
            } catch (ParseException ignored) {
            }
        }

        // Try ISO 8601 via Instant (handles Z, offsets, etc.)
        try {
            return Date.from(Instant.parse(text));
        } catch (DateTimeParseException ignored) {
        }

        // Try parsing as epoch millis from string
        try {
            return new Date(Long.parseLong(text));
        } catch (NumberFormatException ignored) {
        }

        throw ctxt.weirdStringException(text, Date.class,
                "Cannot parse date: unrecognized format. Accepted formats: epoch (ms), ISO 8601, or 'MMM d, yyyy, h:mm:ss a'");
    }
}
