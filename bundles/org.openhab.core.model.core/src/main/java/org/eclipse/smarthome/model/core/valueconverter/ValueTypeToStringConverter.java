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
package org.eclipse.smarthome.model.core.valueconverter;

import java.math.BigDecimal;

import org.eclipse.xtext.conversion.IValueConverter;
import org.eclipse.xtext.conversion.ValueConverterException;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.util.Strings;

/**
 * A {@link ValueTypeToStringConverter} is used to create a {@link String}, {@link Boolean}, or {@link BigDecimal} from
 * an input string and vice versa.
 *
 * @author Alex Tugarev
 */
public class ValueTypeToStringConverter implements IValueConverter<Object> {

    @Override
    public Object toValue(String string, INode node) throws ValueConverterException {
        if (string == null) {
            return null;
        }
        if (string.startsWith("\"") && string.endsWith("\"")) {
            try {
                return Strings.convertFromJavaString(string.substring(1, string.length() - 1), true);
            } catch (IllegalArgumentException e) {
                throw new ValueConverterException(e.getMessage(), node, e);
            }
        }
        if (string.equals("true") || string.equals("false")) {
            return Boolean.valueOf(string);
        }
        try {
            return new BigDecimal(string);
        } catch (NumberFormatException e) {
            throw new ValueConverterException("Number expected.", node, e);
        }
    }

    @Override
    public String toString(Object value) throws ValueConverterException {
        if (value == null) {
            throw new ValueConverterException("Value may not be null.", null, null);
        }
        if (value instanceof String) {
            return toEscapedString((String) value);
        }
        if (value instanceof BigDecimal) {
            BigDecimal decimalValue = (BigDecimal) value;
            return decimalValue.toPlainString();
        }
        if (value instanceof Boolean) {
            return ((Boolean) value).toString();
        }
        throw new ValueConverterException("Unknown value type: " + value.getClass().getSimpleName(), null, null);
    }

    protected String toEscapedString(String value) {
        return '"' + Strings.convertToJavaString(value, false) + '"';
    }
}
