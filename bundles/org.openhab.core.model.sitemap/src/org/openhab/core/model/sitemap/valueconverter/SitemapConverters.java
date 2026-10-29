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
            .compile("(?:[A-Za-z_][A-Za-z_\\-0-9]*|[0-9]+[A-Za-z_\\-][A-Za-z_\\-0-9]*)");
    private static final Pattern INT_PATTERN = Pattern.compile("(?:[0-9]+)");
    private static final Pattern ICON_SEGMENT_PATTERN = Pattern.compile("(?:[A-Za-z_0-9][A-Za-z_0-9\\-]*)");
    private static final String PERIOD_PART = "(?:P(?=\\d|T\\d)(?:\\d+Y)?(?:\\d+M)?(?:\\d+W)?(?:\\d+D)?"
            + "(?:T(?=\\d)(?:\\d+H)?(?:\\d+M)?(?:\\d+S)?)?|\\d*[YMWDh])";
    private static final Pattern PERIOD_PATTERN = Pattern.compile("(?:" + PERIOD_PART + "?-)?" + PERIOD_PART);
    // Mirrors the Number rule: optional '-', then INT or FLOAT (INT '.' INT)
    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?[0-9]+(?:\\.[0-9]+)?");
    public static final Pattern AMBIGUOUS_STATE_PATTERN = Pattern
            .compile("[A-Za-z_][A-Za-z_0-9]*-[0-9]+(?:\\.[0-9]+)?");

    @ValueConverter(rule = "Icon")
    public IValueConverter<String> Icon() {

        return new AbstractNullSafeConverter<>() {
            @Override
            public String internalToValue(String string, INode node) throws ValueConverterException {
                String trimmedString = string;
                if ((string.startsWith("'") && string.endsWith("'"))
                        || (string.startsWith("\"") && string.endsWith("\""))) {
                    trimmedString = STRING().toValue(string, node);
                }
                String[] segments = trimmedString.split(":", -1);
                if (segments.length > 3) {
                    throw new ValueConverterException("Icon name cannot contain more than 3 segments separated by ':'",
                            node, null);
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < segments.length; i++) {
                    if (i != 0) {
                        sb.append(":");
                    }
                    String segment = segments[i];
                    if (i == segments.length - 1) {
                        // For backward compatibility, we allow the icon name to contain a file extension, but we remove
                        // it when validating the name
                        int lastDotIndex = segment.lastIndexOf(".");
                        segment = (lastDotIndex != -1) && (lastDotIndex != segment.length() - 1)
                                ? segment.substring(0, lastDotIndex)
                                : segment;
                    }
                    // allow for ^ prefix when parsing DSL to escape reserved names
                    segment = segment.startsWith("^") ? segment.substring(1) : segment;
                    if (!ICON_SEGMENT_PATTERN.matcher(segment).matches()) {
                        throw new ValueConverterException(
                                "Icon name segment '" + segment + "' is not a valid identifier", node, null);
                    }
                    sb.append(segment);
                }
                return sb.toString();
            }

            @Override
            public String internalToString(String value) throws ValueConverterException {
                String[] segments = value.split(":", -1);
                if (segments.length > 3) {
                    return STRING().toString(value);
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < segments.length; i++) {
                    if (i != 0) {
                        sb.append(":");
                    }
                    String segment = segments[i];
                    if (!ICON_SEGMENT_PATTERN.matcher(segment).matches()) {
                        return STRING().toString(value);
                    }
                    if (INT_PATTERN.matcher(segment).matches()) {
                        sb.append(segment); // lexed as INT, not ID
                    } else {
                        sb.append(ID().toString(segment));
                    }
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
                // allow for ^ prefix when parsing DSL to escape reserved names
                String trimmedString = string.startsWith("^") ? string.substring(1) : string;
                if (ID_PATTERN.matcher(trimmedString).matches()) {
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
                if (AMBIGUOUS_STATE_PATTERN.matcher(value).matches()) {
                    return STRING().toString(value);
                }
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
                if (NUMBER_PATTERN.matcher(string).matches()) {
                    // keep the text as is, to preserve leading zeros and trailing decimals
                    return string;
                }
                return ID().toValue(string, node);
            }

            @Override
            protected String internalToString(String value) throws ValueConverterException {
                if (NUMBER_PATTERN.matcher(value).matches()) {
                    return value;
                }
                if (ID_PATTERN.matcher(value).matches()) {
                    return ID().toString(value);
                } else {
                    return STRING().toString(value);
                }
            }
        };
    }

    @ValueConverter(rule = "Period")
    public IValueConverter<String> Period() {
        return new AbstractNullSafeConverter<>() {
            @Override
            protected String internalToValue(String string, INode node) throws ValueConverterException {
                boolean isQuoted = (string.startsWith("'") && string.endsWith("'"))
                        || (string.startsWith("\"") && string.endsWith("\""));
                String value;
                if (isQuoted) {
                    value = STRING().toValue(string, node);
                } else {
                    boolean dash = string.startsWith("-");
                    String trimmedString = dash ? string.substring(1) : string;
                    // allow for ^ prefix when parsing DSL to escape reserved names
                    trimmedString = trimmedString.startsWith("^") ? trimmedString.substring(1) : trimmedString;
                    value = (dash ? "-" : "") + trimmedString;
                }
                if (!PERIOD_PATTERN.matcher(value).matches()) {
                    throw new ValueConverterException("Period value '" + value
                            + "' is not a valid period format, should be ISO 8601 'PnYnMnDTnHnMnS-PnYnMnDTnHnMnS'",
                            node, null);
                }
                return value;
            }

            @Override
            protected String internalToString(String value) throws ValueConverterException {
                if (!PERIOD_PATTERN.matcher(value).matches()) {
                    return STRING().toString(value);
                }
                return value.startsWith("-") ? "-" + ID().toString(value.substring(1)) : ID().toString(value);
            }
        };
    }
}
