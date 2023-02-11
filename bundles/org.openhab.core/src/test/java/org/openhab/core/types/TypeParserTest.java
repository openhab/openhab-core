/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openhab.core.items.GenericItem;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.library.types.StringType;

/**
 * Test the {@link TypeParser}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
class TypeParserTest {

    private final GenericItem stringItem = new StringItem("Test");

    public static class ParameterSet {
        public final String state;
        public final Class<? extends State> expectedDataType;

        public ParameterSet(String state, Class<? extends State> expectedDataType) {
            this.state = state;
            this.expectedDataType = expectedDataType;
        }
    }

    public static Collection<Object[]> stringItemParameters() {
        return Arrays.asList(new Object[][] { //
                { new ParameterSet("UNDEF", UnDefType.class) }, //
                { new ParameterSet("ABC", StringType.class) }, //
                { new ParameterSet("123", StringType.class) }, //
                { new ParameterSet("2014-03-30T10:58:47.033+0000", StringType.class) } //
        });
    }

    @ParameterizedTest
    @MethodSource("stringItemParameters")
    public void testAllDataTypes(ParameterSet parameterSet) {
        State subject = TypeParser.parseState(stringItem.getAcceptedDataTypes(), parameterSet.state);
        assertThat(subject, instanceOf(parameterSet.expectedDataType));
    }
}
