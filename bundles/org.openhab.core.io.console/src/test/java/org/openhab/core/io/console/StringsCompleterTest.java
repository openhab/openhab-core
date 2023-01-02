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
package org.openhab.core.io.console;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * @author Cody Cutrer - Initial contribution
 */
@NonNullByDefault
public class StringsCompleterTest {
    @Test
    public void completeSimple() {
        var sc = new StringsCompleter(List.of("def", "abc", "ghi"), false);
        var candidates = new ArrayList<String>();

        // positive match
        assertTrue(sc.complete(new String[] { "a" }, 0, 1, candidates));
        assertEquals(1, candidates.size());
        assertEquals("abc ", candidates.get(0));
        candidates.clear();

        // negative match
        assertFalse(sc.complete(new String[] { "z" }, 0, 1, candidates));
        assertTrue(candidates.isEmpty());

        // case insensitive
        assertTrue(sc.complete(new String[] { "A" }, 0, 1, candidates));
        assertEquals(1, candidates.size());
        assertEquals("abc ", candidates.get(0));
        candidates.clear();

        // second argument
        assertTrue(sc.complete(new String[] { "a", "d" }, 1, 1, candidates));
        assertEquals(1, candidates.size());
        assertEquals("def ", candidates.get(0));
        candidates.clear();

        // cursor not at end of word (truncates rest)
        assertTrue(sc.complete(new String[] { "a", "dg" }, 1, 1, candidates));
        assertEquals(1, candidates.size());
        assertEquals("def ", candidates.get(0));
        candidates.clear();

        // first argument when second is present
        assertTrue(sc.complete(new String[] { "a", "d" }, 0, 1, candidates));
        assertEquals(1, candidates.size());
        assertEquals("abc ", candidates.get(0));
    }

    @Test
    public void caseSensitive() {
        var sc = new StringsCompleter(List.of("dEf", "ABc", "ghi"), true);
        var candidates = new ArrayList<String>();

        assertFalse(sc.complete(new String[] { "D" }, 0, 1, candidates));
        assertTrue(candidates.isEmpty());

        assertFalse(sc.complete(new String[] { "ab" }, 0, 1, candidates));
        assertTrue(candidates.isEmpty());

        assertTrue(sc.complete(new String[] { "AB" }, 0, 1, candidates));
        assertEquals(1, candidates.size());
        assertEquals("ABc ", candidates.get(0));
    }

    @Test
    public void multipleCandidates() {
        var sc = new StringsCompleter(List.of("abcde", "bcde", "abcdef", "abcdd", "abcdee", "abcdf"), false);
        var candidates = new ArrayList<String>();

        assertTrue(sc.complete(new String[] { "abcd" }, 0, 4, candidates));
        assertEquals(5, candidates.size());
        assertEquals("abcdd ", candidates.get(0));
        assertEquals("abcde ", candidates.get(1));
        assertEquals("abcdee ", candidates.get(2));
        assertEquals("abcdef ", candidates.get(3));
        assertEquals("abcdf ", candidates.get(4));
        candidates.clear();

        assertTrue(sc.complete(new String[] { "abcde" }, 0, 5, candidates));
        assertEquals(3, candidates.size());
        assertEquals("abcde ", candidates.get(0));
        assertEquals("abcdee ", candidates.get(1));
        assertEquals("abcdef ", candidates.get(2));
        candidates.clear();

        assertTrue(sc.complete(new String[] { "abcdee" }, 0, 6, candidates));
        assertEquals(1, candidates.size());
        assertEquals("abcdee ", candidates.get(0));
    }
}
