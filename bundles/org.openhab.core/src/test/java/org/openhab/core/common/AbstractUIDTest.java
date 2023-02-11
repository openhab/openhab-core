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
package org.openhab.core.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * The {@link AbstractUIDTest} contains tests for
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class AbstractUIDTest {
    private static final int MINIMUM_SEGMENTS = 3;

    @Test
    public void segmentsAreProperlyAdded() {
        TestUID testUID = new TestUID("seg1", "seg2", "seg3");
        assertThat(testUID.getAllSegments(), hasSize(3));
    }

    @Test
    void lessThanMinimumSegmentsNotAllowed() {
        assertThrows(IllegalArgumentException.class, () -> new TestUID("seg1", "seg2"));
    }

    @Test
    public void emptyMiddleSegmentAllowed() {
        TestUID testUID = new TestUID("seg1", "", "seg3");
        assertThat(testUID.getAllSegments(), hasSize(3));
    }

    @Test
    public void emptyLastSegmentNotAllowed() {
        assertThrows(IllegalArgumentException.class, () -> new TestUID("seg1", "seg2", ""));
    }

    @Test
    public void illegalCharactersNotAllowed() {
        assertThrows(IllegalArgumentException.class, () -> new TestUID("seg1", "seg2", "seg."));
    }

    @Test
    public void segmentsEqualStringUID() {
        TestUID testUID1 = new TestUID("seg1", "seg2", "seg3");
        TestUID testUID2 = new TestUID("seg1:seg2:seg3");

        assertThat(testUID1, is(testUID2));
    }

    private static class TestUID extends AbstractUID {

        public TestUID(String... segments) {
            super(segments);
        }

        public TestUID(String uid) {
            super(uid);
        }

        @Override
        protected int getMinimalNumberOfSegments() {
            return MINIMUM_SEGMENTS;
        }
    }
}
