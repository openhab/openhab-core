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
package org.eclipse.smarthome.core.library.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;

/**
 * This type can be used for items that are dealing with telephony functionality.
 *
 * @author Thomas.Eichstaedt-Engelen / Initially CallType
 * @author Gaël L'hopital - port to Eclipse SmartHome
 *
 */
@NonNullByDefault
public class StringListType implements Command, State {

    protected List<String> typeDetails;

    // constants
    public static final String DELIMITER = ",";
    public static final String ESCAPED_DELIMITER = "\\" + DELIMITER;
    public static final String REGEX_SPLITTER = "(?<!\\\\)" + DELIMITER;

    public StringListType() {
        typeDetails = Collections.emptyList();
    }

    public StringListType(List<String> rows) {
        typeDetails = new ArrayList<>(rows);
    }

    public StringListType(StringType... rows) {
        typeDetails = Arrays.stream(rows).map(StringType::toString).collect(Collectors.toList());
    }

    public StringListType(String... rows) {
        typeDetails = Arrays.asList(rows);
    }

    /**
     * Deserialize the input string, splitting it on every delimiter not preceded by a backslash.
     */
    public StringListType(String serialized) {
        typeDetails = Arrays.stream(serialized.split(REGEX_SPLITTER, -1))
                .map(s -> s.replace(ESCAPED_DELIMITER, DELIMITER)).collect(Collectors.toList());
    }

    public String getValue(final int index) {
        if (index < 0 || index >= typeDetails.size()) {
            throw new IllegalArgumentException("Index is out of range");
        }
        return typeDetails.get(index);
    }

    /**
     * <p>
     * Formats the value of this type according to a pattern (@see
     * {@link Formatter}). One single value of this type can be referenced
     * by the pattern using an index. The item order is defined by the natural
     * (alphabetical) order of their keys.
     *
     * @param pattern the pattern to use containing indexes to reference the
     *                    single elements of this type.
     */
    @Override
    public String format(String pattern) {
        return String.format(pattern, typeDetails.toArray());
    }

    @Override
    public String toString() {
        return toFullString();
    }

    @Override
    public String toFullString() {
        return typeDetails.stream().map(s -> s.replace(DELIMITER, ESCAPED_DELIMITER))
                .collect(Collectors.joining(DELIMITER));
    }

    public static StringListType valueOf(String value) {
        return new StringListType(value);
    }

}
