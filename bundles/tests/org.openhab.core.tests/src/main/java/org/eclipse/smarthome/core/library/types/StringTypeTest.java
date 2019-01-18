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

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author Thomas.Eichstaedt-Engelen
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
        assertFalse(expected1.hashCode() == new StringType("expected2").hashCode());

        assertEquals(empty, StringType.EMPTY);
        assertEquals(empty, new StringType(""));
        assertEquals(empty, new StringType());
        assertEquals(empty, new StringType(null));

        assertEquals(expected1, new StringType("expected1"));
        assertEquals(expected2, new StringType("expected2"));
        assertEquals(false, expected1.equals(new StringType("expected2")));
        assertEquals(false, expected2.equals(new StringType("expected1")));
        assertEquals(false, expected1.equals(StringType.EMPTY));
        assertEquals(false, expected2.equals(StringType.EMPTY));

        assertEquals(true, expected1.equals("expected1"));
        assertEquals(false, expected1.equals("expected2"));

        assertEquals(true, new StringType(null).equals(new StringType(null)));
        assertEquals(true, new StringType("").equals(new StringType(null)));
        assertEquals(true, new StringType(null).equals(new StringType("")));
    }

}
