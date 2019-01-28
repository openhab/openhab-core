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
package org.eclipse.smarthome.model.valueconverter;

import java.util.regex.Pattern;

import org.eclipse.xtext.common.services.DefaultTerminalConverters;
import org.eclipse.xtext.conversion.IValueConverter;
import org.eclipse.xtext.conversion.ValueConverter;
import org.eclipse.xtext.conversion.ValueConverterException;
import org.eclipse.xtext.conversion.impl.AbstractNullSafeConverter;
import org.eclipse.xtext.nodemodel.INode;

public class SitemapConverters extends DefaultTerminalConverters {

    private static final Pattern ID_PATTERN = Pattern.compile("\\p{Alpha}\\w*");

    @ValueConverter(rule = "Icon")
    public IValueConverter<String> Icon() {

        return new IValueConverter<String>() {

            @Override
            public String toValue(String string, INode node) throws ValueConverterException {
                if (string != null && string.startsWith("\"")) {
                    return string.substring(1, string.length() - 1);
                }
                return string;
            }

            @Override
            public String toString(String value) throws ValueConverterException {
                if (containsWhiteSpace(value)) {
                    return "\"" + value + "\"";
                }
                return value;
            }

        };
    }

    @ValueConverter(rule = "Command")
    public IValueConverter<String> Command() {
        return new AbstractNullSafeConverter<String>() {
            @Override
            protected String internalToValue(String string, INode node) {
                if ((string.startsWith("'") && string.endsWith("'"))
                        || (string.startsWith("\"") && string.endsWith("\""))) {
                    return STRING().toValue(string, node);
                }
                return ID().toValue(string, node);
            }

            @Override
            protected String internalToString(String value) {
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
