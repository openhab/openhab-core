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
package org.eclipse.smarthome.core.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Test for {@link HexUtils}.
 *
 * @author Kai Kreuzer - Initial implementation
 * @author Martin van Wingerden - Implemented the reverse operation
 */
public class HexUtilsTest {

    @Test
    public void test_bytesToHex_noParams() {
        byte[] bytes = "ABCD".getBytes();
        String result = HexUtils.bytesToHex(bytes);
        assertEquals("41424344", result);
    }

    @Test
    public void test_hexToBytes_noParams() {
        byte[] result = HexUtils.hexToBytes("41424344");
        assertEquals("ABCD", new String(result));
    }

    @Test
    public void test_bytesToHex_withDelimiter() {
        byte[] bytes = "ABCD".getBytes();
        String result = HexUtils.bytesToHex(bytes, " ");
        assertEquals("41 42 43 44", result);
    }

    @Test
    public void test_hexToBytes_withDelimiter() {
        byte[] result = HexUtils.hexToBytes("41 42 43 44", " ");
        assertEquals("ABCD", new String(result));
    }

    @Test
    public void test_bytesToHex_withMultiCharDelimiter() {
        byte[] bytes = "ABCD".getBytes();
        String result = HexUtils.bytesToHex(bytes, "-:-");
        assertEquals("41-:-42-:-43-:-44", result);
    }

    @Test
    public void test_hexToBytes_withMultiCharDelimiter() {
        byte[] result = HexUtils.hexToBytes("41-:-42-:-43-:-44", "-:-");
        assertEquals("ABCD", new String(result));
    }
}
