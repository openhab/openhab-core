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
package org.openhab.core.model.sitemap.valueconverter;

import java.util.regex.Pattern;

import org.eclipse.xtext.common.services.DefaultTerminalConverters;
import org.eclipse.xtext.conversion.IValueConverter;
import org.eclipse.xtext.conversion.ValueConverter;
import org.eclipse.xtext.conversion.ValueConverterException;
import org.eclipse.xtext.conversion.impl.AbstractNullSafeConverter;
import org.eclipse.xtext.nodemodel.INode;

public class SitemapConverters extends DefaultTerminalConverters {

    private static final Pattern ID_PATTERN = Pattern
            .compile("(?:[A-Za-z_][A-Za-z_0-9]*|[0-9]+[A-Za-z_][A-Za-z_0-9]*)");
    // allow for ^ prefix when parsing DSL to escape reserved names
    private static final Pattern ID_EXT_PATTERN = Pattern
            .compile("(?:\\^?[A-Za-z_][A-Za-z_0-9]*|[0-9]+[A-Za-z_][A-Za-z_0-9]*)");
    private static final Pattern INT_PATTERN = Pattern.compile("[0-9]+");

    @ValueConverter(rule = "Icon")
    public IValueConverter<String> Icon() {

        return new AbstractNullSafeConverter<>() {
            @Override
            public String internalToValue(String string, INode node) throws ValueConverterException {
                if ((string.startsWith("'") && string.endsWith("'"))
                        || (string.startsWith("\"") && string.endsWith("\""))) {
                    return STRING().toValue(string, node);
                }
                String[] parts = string.split(":", -1);
                if (parts.length > 3) {
                    throw new ValueConverterException("Icon name cannot contain more than 3 parts separated by ':'",
                            node, null);
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < parts.length - 1; i++) {
                    if (!ID_EXT_PATTERN.matcher(parts[i]).matches()) {
                        throw new ValueConverterException("Icon name part '" + parts[i] + "' is not a valid identifier",
                                node, null);
                    }
                    sb.append(ID().toValue(parts[i], node)).append(":");
                }
                String[] lastParts = parts[parts.length - 1].split("-", -1);
                for (int i = 0; i < lastParts.length; i++) {
                    if (i != 0) {
                        sb.append("-");
                    }
                    if (!ID_EXT_PATTERN.matcher(lastParts[i]).matches()) {
                        throw new ValueConverterException(
                                "Icon name part '" + parts[parts.length - 1] + "' is not a valid identifier", node,
                                null);
                    }
                    sb.append(ID().toValue(lastParts[i], node));
                }
                return sb.toString();
            }

            @Override
            public String internalToString(String value) throws ValueConverterException {
                if (containsWhiteSpace(value)) {
                    return STRING().toString(value);
                }
                String[] parts = value.split(":", -1);
                if (parts.length > 3) {
                    return STRING().toString(value);
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < parts.length - 1; i++) {
                    if (!ID_PATTERN.matcher(parts[i]).matches()) {
                        return STRING().toString(value);
                    }
                    sb.append(ID().toString(parts[i])).append(":");
                }
                String[] lastParts = parts[parts.length - 1].split("-", -1);
                for (int i = 0; i < lastParts.length; i++) {
                    if (i != 0) {
                        sb.append("-");
                    }
                    if (!ID_PATTERN.matcher(lastParts[i]).matches()) {
                        return STRING().toString(value);
                    }
                    sb.append(ID().toString(lastParts[i]));
                }

                return sb.toString();
            }
        };
    }

    @ValueConverter(rule = "XState")
    public IValueConverter<String> XState() {

        return new AbstractNullSafeConverter<>() {
            @Override
            protected String internalToValue(String string, INode node) throws ValueConverterException {
                if ((string.startsWith("'") && string.endsWith("'"))
                        || (string.startsWith("\"") && string.endsWith("\""))) {
                    return STRING().toValue(string, node);
                }
                if (ID_EXT_PATTERN.matcher(string).matches()) {
                    return ID().toValue(string, node);
                }
                // Don't interpret each part as INT() to preserve leading zeros, but validate that they are numbers
                String[] parts = string.split("\\.", -1);
                if (parts.length > 2) {
                    throw new ValueConverterException("State invalid number format", node, null);
                }
                for (int i = 0; i < parts.length; i++) {
                    if (!INT_PATTERN.matcher(parts[i]).matches()) {
                        throw new ValueConverterException("State invalid number format", node, null);
                    }
                }
                return string;
            }

            @Override
            protected String internalToString(String value) throws ValueConverterException {
                if (ID_PATTERN.matcher(value).matches()) {
                    return ID().toString(value);
                }
                String[] parts = value.split("\\.", -1);
                if (parts.length <= 2) {
                    for (int i = 0; i < parts.length; i++) {
                        if (!INT_PATTERN.matcher(parts[i]).matches()) {
                            return STRING().toString(value);
                        }
                    }
                    return value;
                }
                return STRING().toString(value);
            }
        };
    }

    @ValueConverter(rule = "Command")
    public IValueConverter<String> Command() {
        return new AbstractNullSafeConverter<>() {
            @Override
            protected String internalToValue(String string, INode node) throws ValueConverterException {
                if ((string.startsWith("'") && string.endsWith("'"))
                        || (string.startsWith("\"") && string.endsWith("\""))) {
                    return STRING().toValue(string, node);
                }
                return ID().toValue(string, node);
            }

            @Override
            protected String internalToString(String value) throws ValueConverterException {
                if (ID_PATTERN.matcher(value).matches()) {
                    return ID().toString(value);
                } else {
                    return STRING().toString(value);
                }
            }
        };
    }

    public static boolean containsWhiteSpace(final String string) {
        if (string != null) {
            for (int i = 0; i < string.length(); i++) {
                if (Character.isWhitespace(string.charAt(i))) {
                    return true;
                }
            }
        }
        return false;
    }
}
