/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Test for {@link HexUtils}.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Martin van Wingerden - Implemented the reverse operation
 */
public class HexUtilsTest {

    @Test
    public void testBytesToHexNoParams() {
        byte[] bytes = "ABCD".getBytes();
        String result = HexUtils.bytesToHex(bytes);
        assertEquals("41424344", result);
    }

    @Test
    public void testHexToBytesNoParams() {
        byte[] result = HexUtils.hexToBytes("41424344");
        assertEquals("ABCD", new String(result));
    }

    @Test
    public void testBytesToHexWithDelimiter() {
        byte[] bytes = "ABCD".getBytes();
        String result = HexUtils.bytesToHex(bytes, " ");
        assertEquals("41 42 43 44", result);
    }

    @Test
    public void testHexToBytesWithDelimiter() {
        byte[] result = HexUtils.hexToBytes("41 42 43 44", " ");
        assertEquals("ABCD", new String(result));
    }

    @Test
    public void testBytesToHexWithMultiCharDelimiter() {
        byte[] bytes = "ABCD".getBytes();
        String result = HexUtils.bytesToHex(bytes, "-:-");
        assertEquals("41-:-42-:-43-:-44", result);
    }

    @Test
    public void testHexToBytesWithMultiCharDelimiter() {
        byte[] result = HexUtils.hexToBytes("41-:-42-:-43-:-44", "-:-");
        assertEquals("ABCD", new String(result));
    }

    @Test
    public void testEmptyByteArray() {
        final byte[] input = new byte[0];
        final String str = HexUtils.bytesToHex(input);
        final byte[] output = HexUtils.hexToBytes(str);
        assertArrayEquals(input, output);
    }
}
