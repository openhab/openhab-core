/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.library.types;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * @author Thomas Eichstaedt-Engelen - Initial contribution
 */
public class StringTypeTest {

    @Test
    @SuppressWarnings("unlikely-arg-type")
    public void testEquals() {
        StringType empty = new StringType("");
        StringType expected1 = new StringType("expected1");
        StringType expected2 = new StringType("expected2");

        assertEquals(empty.hashCode(), StringType.EMPTY.hashCode());
        assertEquals(empty.hashCode(), new StringType("").hashCode());
        assertEquals(empty.hashCode(), new StringType().hashCode());
        assertEquals(empty.hashCode(), new StringType(null).hashCode());

        assertEquals(expected1.hashCode(), new StringType("expected1").hashCode());
        assertEquals(expected2.hashCode(), new StringType("expected2").hashCode());
        assertNotEquals(expected1.hashCode(), new StringType("expected2").hashCode());

        assertEquals(empty, StringType.EMPTY);
        assertEquals(empty, new StringType(""));
        assertEquals(empty, new StringType());
        assertEquals(empty, new StringType(null));

        assertEquals(expected1, new StringType("expected1"));
        assertEquals(expected2, new StringType("expected2"));
        // Do not change to assertEquals(), because we want to check if .equals() works as expected!
        assertFalse(expected1.equals(new StringType("expected2")));
        assertFalse(expected2.equals(new StringType("expected1")));
        assertFalse(expected1.equals(StringType.EMPTY));
        assertFalse(expected2.equals(StringType.EMPTY));

        // Do not change to assertEquals(), because we want to check if .equals() works as expected!
        assertTrue(expected1.equals("expected1"));
        assertFalse(expected1.equals("expected2"));

        // Do not change to assertEquals(), because we want to check if .equals() works as expected!
        assertTrue(new StringType(null).equals(new StringType(null)));
        assertTrue(new StringType("").equals(new StringType(null)));
        assertTrue(new StringType(null).equals(new StringType("")));
    }
}
