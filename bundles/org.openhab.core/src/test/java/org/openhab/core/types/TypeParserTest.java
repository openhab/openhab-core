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
package org.openhab.core.types;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.items.GenericItem;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.StringType;

/**
 * Test the {@link TypeParser}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
class TypeParserTest {

    private final GenericItem stringItem = new StringItem("Test");

    @Test
    void testThatUNDEFAsStringIsParsedToUnDefType() {
        State subject = TypeParser.parseState(stringItem.getAcceptedDataTypes(), "UNDEF");
        assertThat(subject instanceof UnDefType, is(true));
    }

    @Test
    void testThatANumberAsStringIsParsedDateTimeType() {
        State subject = TypeParser.parseState(stringItem.getAcceptedDataTypes(), "123");
        assertThat(subject instanceof DateTimeType, is(true));
    }

    @Test
    void testThatADateAsStringIsParsedDateTimeType() {
        State subject = TypeParser.parseState(stringItem.getAcceptedDataTypes(), "2014-03-30T10:58:47.033+0000");
        assertThat(subject instanceof DateTimeType, is(true));
    }

    @Test
    void testThatAStringIsParsedToStringType() {
        State subject = TypeParser.parseState(stringItem.getAcceptedDataTypes(), "ABC");
        assertThat(subject instanceof StringType, is(true));
    }
}
