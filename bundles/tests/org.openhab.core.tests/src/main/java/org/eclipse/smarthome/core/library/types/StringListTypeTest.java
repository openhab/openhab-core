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
 * @author Gaël L'hopital
 * @author Kai Kreuzer - added tests for valueOf and toFullString
 */
public class StringListTypeTest {
    @Test
    public void testSerializedEquals_simple() {
        final int DEST_IDX = 0;
        final int ORIG_IDX = 1;

        StringListType call1 = new StringListType("0179999998", "0699222222");
        StringListType call2 = new StringListType("0699222222,0179999998");

        assertEquals(call1.getValue(ORIG_IDX), call2.getValue(DEST_IDX));
        assertEquals(call2.toString(), "0699222222,0179999998");
    }

    @Test
    public void testSerializedEquals_withEscapedEntries() {
        String serialized = "value1,value2,value=with=foo,value\\,with\\,foo,,\\,\\,foo";
        StringListType call4 = new StringListType(serialized);

        assertEquals("value2", call4.getValue(1));
        assertTrue(call4.getValue(4).isEmpty());
        assertEquals("value=with=foo", call4.getValue(2));
        assertEquals("value,with,foo", call4.getValue(3));
        assertEquals(",,foo", call4.getValue(5));
        assertEquals(serialized, call4.toString());
    }

    @Test
    public void testWithEmptyConstituents() {
        StringListType call1 = new StringListType(",0699222222");
        assertEquals("", call1.getValue(0));
        assertEquals("0699222222", call1.getValue(1));

        StringListType call2 = new StringListType("0699222222,");
        assertEquals("0699222222", call2.getValue(0));
        assertEquals("", call2.getValue(1));
    }

    @Test
    public void testError() {
        StringListType type = new StringListType("foo=bar", "electric", "chair");

        try {
            // Index is between 0 and number of elements -1
            @SuppressWarnings("unused")
            String value = type.getValue(-1);
            fail("-1 is an invalid index");
        } catch (IllegalArgumentException e) {
            // That's what we expect.
        }

        try {
            @SuppressWarnings("unused")
            String value = type.getValue(3);
            fail("3 is an invalid index");
        } catch (IllegalArgumentException e) {
            // That's what we expect.
        }
    }

    @Test
    public void testToFullString() {
        StringListType abc = new StringListType("a", "b", "c");
        String fullString = abc.toFullString();
        assertEquals("a,b,c", fullString);
    }

    @Test
    public void testValueOf_simple() {
        StringListType abc = StringListType.valueOf("a,b,c");
        assertEquals("a", abc.getValue(0));
        assertEquals("b", abc.getValue(1));
        assertEquals("c", abc.getValue(2));
    }

    @Test
    public void testValueOf_withEscapedEntries() {
        StringListType abC = StringListType.valueOf("a\\,b,c");
        assertEquals("a,b", abC.getValue(0));
        assertEquals("c", abC.getValue(1));
    }
}
