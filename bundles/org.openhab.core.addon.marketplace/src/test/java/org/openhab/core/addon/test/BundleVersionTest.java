/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.core.addon.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
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
@MockitoSettings(strictness = Strictness.WARN)
public class BundleVersionTest {

    private static Stream<Arguments> provideCompareVersionsArguments() {
        return Stream.of( //
                Arguments.of("3.1.0", "3.1.0", Result.EQUAL), // same versions are equal
                Arguments.of("3.1.0", "3.0.2", Result.NEWER), // minor version is more important than micro
                Arguments.of("3.7.0", "4.0.1.202105311711", Result.OLDER), // major version is more important than minor
                Arguments.of("3.9.1.M1", "3.9.0.M5", Result.NEWER), // micro version is more important than qualifier
                Arguments.of("3.0.0.202105311025", "3.0.0.202106011145", Result.EQUAL), // all snapshots equal
                Arguments.of("3.1.0.M3", "3.1.0.M1", Result.NEWER), // milestones are compared numerically
                Arguments.of("3.1.0.M1", "3.1.0.197705310000", Result.OLDER), // snapshot is newer than milestone
                Arguments.of("3.3.0", "3.3.0.20220630211555", Result.NEWER) // release is newer than snapshot
        );
    }

    @ParameterizedTest
    @MethodSource("provideCompareVersionsArguments")
    public void compareVersionsTest(String v1, String v2, Result result) {
        BundleVersion version1 = new BundleVersion(v1);
        BundleVersion version2 = new BundleVersion(v2);
        switch (result) {
            case OLDER:
                assertTrue(version1.compareTo(version2) < 0);
                break;
            case NEWER:
                assertTrue(version1.compareTo(version2) > 0);
                break;
            case EQUAL:
                assertEquals(0, version1.compareTo(version2));
                break;
        }
    }

    private enum Result {
        OLDER,
        NEWER,
        EQUAL
    }
}
