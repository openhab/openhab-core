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
package org.openhab.core.common;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * The {@link VersionRangeTest} contains tests for the {@link VersionRange} class
 *
 * @author - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class VersionRangeTest {

    @Test
    public void testIllegalRangeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> VersionRange.valueOf("illegal"));
        assertThrows(IllegalArgumentException.class, () -> VersionRange.valueOf("[4.3,0:)"));
        assertThrows(IllegalArgumentException.class, () -> new VersionRange("  "));
        assertThrows(IllegalArgumentException.class, () -> new VersionRange("[,]"));
        assertThrows(IllegalArgumentException.class, () -> new VersionRange("[  ,  ]"));
        assertThrows(IllegalArgumentException.class, () -> new VersionRange('b', Version.valueOf("1.3.0"), null, ']'));
        assertThrows(IllegalArgumentException.class, () -> new VersionRange('(', Version.valueOf("1.3.0"), null, 'e'));
    }

    @Test
    public void testConstructor() {
        VersionRange range = new VersionRange('(', Version.valueOf("4.2.2"), null, ')');
        assertThat(range.includes(Version.valueOf((String) null)), is(false));
        assertThat(range.includes(Version.valueOf("4.2.2")), is(false));
        range = new VersionRange('[', Version.valueOf("4.2.2"), null, ')');
        assertThat(range.includes(Version.valueOf("4.2.2")), is(true));
    }

    @Test
    public void testIncludes() {
        assertThat(VersionRange.valueOf("[3.3.0;3.4.0)").includes(Version.valueOf("3.2.0")), is(false));
        assertThat(VersionRange.valueOf("[3.3.0;3.4.0]").includes(Version.valueOf("3.4.0.M1")), is(true));
        assertThat(VersionRange.valueOf("[3.3.0;3.4.0)").includes(Version.valueOf("3.4.0.M1")), is(false));
        assertThat(VersionRange.valueOf("[3.3.0;)").includes(Version.valueOf("3.4.0.M1")), is(true));
    }

    @Test
    public void testToString() {
        assertEquals("[0.0.0,]", VersionRange.valueOf(null).toString());
        assertEquals("[0.0.0,]", VersionRange.valueOf("").toString());
        assertEquals("[2.4.0,]", VersionRange.valueOf("[2.4:]").toString());
        assertEquals("(4.2.3,]", VersionRange.valueOf("(4.2.3;]").toString());
        assertEquals("[3.1.0,3.2.0)", VersionRange.valueOf("[3.1.0;3.2.0)").toString());
        assertEquals("[3.1.0,3.2.0]", VersionRange.valueOf("[3.1.0;3.2.0]").toString());
        assertEquals("(3.1.0,3.2.9.alpha)", VersionRange.valueOf("(3.1.0;3.2.9.alpha)").toString());
        assertEquals("(3.1.0,3.2.0.SNAPSHOT]", VersionRange.valueOf("(3.1.0  ; 3.2. 0.SNAPSHOT]").toString());
    }

    private static Stream<Arguments> provideInRangeArguments() {
        return Stream.of(Arguments.of("5.0.0.RC1", "[3.3.0;5.0.0.0]", true),
                Arguments.of("3.2.0", "[3.1.0;3.2.1)", true), // in range
                Arguments.of("3.2.0", "[3.1.0;3.2.0)", false), // at end of range, non-inclusive
                Arguments.of("3.2.0", "[3.1.0;3.2.0]", true), // at end of range, inclusive
                Arguments.of("3.2.0", "[3.1.0;3.1.5)", false), // above range
                Arguments.of("3.2.0", "[3.3.0;3.4.0)", false), // below range
                Arguments.of("3.2.0", "", true), // empty range assumes in range
                Arguments.of("3.2.0", null, true), // null range assumes in range
                Arguments.of("5.0.0.RC1", "[3.3.0;5.0.0)", false), Arguments.of("5.0.0.RC1", "[3.3.0;5.0.0.0]", true),
                Arguments.of("5.0.0.M4", "[3.3.0;5.0)", false), Arguments.of("5.0.0.202510140119", "[3.3.0:5)", false),
                Arguments.of("5.0.0.202510140119", "[3.3.0,)", true),
                Arguments.of("3.3.0.202310140119", "[3.3.0,)", false),
                Arguments.of("3.3.0.202310140119", "[3.3.0.0,)", true), Arguments.of("3.3.0", "[3.3.0,)", true),
                Arguments.of("3.3.0", "(3.3.0,)", false), Arguments.of("3.3.1", "(3.3.0..)", true),
                Arguments.of("2.0.0.M2", "[2.0.0.M1;2.0.0.RC1)", true), Arguments.of("5.2.0-RC1", "[5.2.0.M3;6)", true),
                Arguments.of("5.2.0-SNAPSHOT", "[5.2.0.M3;6)", true), Arguments.of("5.2.0.M1", "[5.2.0.M3;6)", false),
                Arguments.of("6.0.0.M1", "[5.2.0.M3;6)", false),
                Arguments.of("3.3.0", "  [ 3 .  3   .0.   alpha;)", true));
    }

    @ParameterizedTest
    @MethodSource("provideInRangeArguments")
    public void inRangeTest(String versionStr, @Nullable String rangeStr, boolean result) {
        Version version = Version.valueOf(versionStr);
        VersionRange range = VersionRange.valueOf(rangeStr);
        assertThat(range.includes(version), is(result));
    }
}
