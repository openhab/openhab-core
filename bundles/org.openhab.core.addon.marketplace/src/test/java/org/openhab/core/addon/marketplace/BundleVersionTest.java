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
package org.openhab.core.addon.marketplace;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
 * The {@link BundleVersionTest} contains tests for the {@link BundleVersion} class
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class BundleVersionTest {

    private static Stream<Arguments> provideCompareVersionsArguments() {
        return Stream.of( //
                Arguments.of("3.1.0", "3.1.0", Result.EQUAL), // same versions are equal
                Arguments.of("3.1.0", "3.0.2", Result.NEWER), // minor version is more important than micro
                Arguments.of("3.7.0", "4.0.1.202105311711", Result.OLDER), // major version is more important than minor
                Arguments.of("3.9.1.M1", "3.9.0.M5", Result.NEWER), // micro version is more important than qualifier
                Arguments.of("3.0.0.202105311032", "3.0.0.202106011144", Result.OLDER), // snapshots
                Arguments.of("3.1.0.M3", "3.1.0.M1", Result.NEWER), // milestones are compared numerically
                Arguments.of("3.1.0.M1", "3.1.0.197705310021", Result.OLDER), // snapshot is newer than milestone
                Arguments.of("3.3.0", "3.3.0.202206302115", Result.NEWER), // release is newer than snapshot
                Arguments.of("3.3.0", "3.3.0.RC1", Result.NEWER), // releases are newer than release candidates
                Arguments.of("3.3.0.M5", "3.3.0.RC1", Result.OLDER), // milestones are older than release candidates
                Arguments.of("3.3.0.RC2", "3.3.0.202305201715", Result.OLDER) // snapshots are newer than release
                                                                              // candidates
        );
    }

    @Test
    public void testIllegalRangeThrowsException() {
        BundleVersion bundleVersion = new BundleVersion("3.1.0");
        assertThrows(IllegalArgumentException.class, () -> bundleVersion.inRange("illegal"));
    }

    @ParameterizedTest
    @MethodSource("provideCompareVersionsArguments")
    public void testCompareVersions(String v1, String v2, Result result) {
        BundleVersion version1 = new BundleVersion(v1);
        BundleVersion version2 = new BundleVersion(v2);
        switch (result) {
            case OLDER:
                assertThat(version1.compareTo(version2), lessThan(0));
                break;
            case NEWER:
                assertThat(version1.compareTo(version2), greaterThan(0));
                break;
            case EQUAL:
                assertThat(version1.compareTo(version2), is(0));
                break;
        }
    }

    private static Stream<Arguments> provideInRangeArguments() {
        return Stream.of(Arguments.of("[3.1.0;3.2.1)", true), // in range
                Arguments.of("[3.1.0;3.2.0)", false), // at end of range, non-inclusive
                Arguments.of("[3.1.0;3.2.0]", true), // at end of range, inclusive
                Arguments.of("[3.1.0;3.1.5)", false), // above range
                Arguments.of("[3.3.0;3.4.0)", false), // below range
                Arguments.of("", true), // empty range assumes in range
                Arguments.of(null, true));
    }

    @ParameterizedTest
    @MethodSource("provideInRangeArguments")
    public void inRangeTest(@Nullable String range, boolean result) {
        BundleVersion frameworkVersion = new BundleVersion("3.2.0");
        assertThat(frameworkVersion.inRange(range), is(result));
    }

    private enum Result {
        OLDER,
        NEWER,
        EQUAL
    }
}
