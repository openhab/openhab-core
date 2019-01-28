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
package org.eclipse.smarthome.model.thing.valueconverter;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.eclipse.smarthome.core.thing.UID;
import org.eclipse.xtext.conversion.IValueConverter;
import org.eclipse.xtext.conversion.ValueConverterException;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.util.Strings;

/**
 * A {@link UIDtoStringConverter} is used to create {@link UID} string
 * representations from an input string and vice versa. If a segment of the
 * parsed {@link UID} string doesn't match the ID rule, it will be escaped.
 *
 * @author Alex Tugarev
 */
public class UIDtoStringConverter implements IValueConverter<String> {

    private static final String SEPERATOR = ":";

    @Override
    public String toValue(final String string, INode node) throws ValueConverterException {
        if (string == null) {
            return null;
        }
        String[] ids = string.split(SEPERATOR);
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            if (id != null && id.startsWith("\"") && id.endsWith("\"")) {
                try {
                    ids[i] = Strings.convertFromJavaString(id.substring(1, id.length() - 1), true);
                } catch (IllegalArgumentException e) {
                    throw new ValueConverterException(e.getMessage(), node, e);
                }
            }
        }
        return Arrays.stream(ids).collect(Collectors.joining(SEPERATOR));
    }

    @Override
    public String toString(String value) throws ValueConverterException {
        if (value == null) {
            throw new ValueConverterException("Value may not be null.", null, null);
        }
        String[] ids = value.split(SEPERATOR);
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            if (id != null && !id.matches("[A-Za-z0-9_]*")) {
                // string escapes each segment which doesn't match:
                // terminal ID: '^'?('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'_'|'0'..'9')*;
                ids[i] = toEscapedString(id);
            }
        }
        return Arrays.stream(ids).collect(Collectors.joining(SEPERATOR));
    }

    protected String toEscapedString(String value) {
        return '"' + Strings.convertToJavaString(value, false) + '"';
    }
}
