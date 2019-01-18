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

import java.util.Arrays;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Static utility methods that are helpful when dealing with hex data and byte arrays.
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Martin van Wingerden - Implemented the reverse operation
 */
@NonNullByDefault
public class HexUtils {

    // used for hex conversions
    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    private static final int ASCII_DIGITS_START_POSITION = 48;
    private static final int ASCII_UPPERCASE_LETTERS_START_POSITION = 65;

    private HexUtils() {
        // private constructor as we only have static methods
    }

    /**
     * Converts a byte array into a hex string with a given delimiter.
     * Example: Delimiter "-" results in Strings like "01-23-45".
     *
     * @param bytes the byte array
     * @param delimiter a delimiter that is placed between every two bytes
     * @return the corresponding hex string
     */
    public static String bytesToHex(byte[] bytes, @Nullable CharSequence delimiter) {
        return Arrays.stream(toObjects(bytes)).map(b -> {
            int v = b & 0xFF;
            return "" + hexArray[v >>> 4] + hexArray[v & 0x0F];
        }).collect(Collectors.joining(delimiter != null ? delimiter : ""));
    }

    /**
     * Converts a byte array into a hex string (in format "0123456789ABCDEF").
     *
     * @param bytes the byte array
     * @return the corresponding hex string
     */
    public static String bytesToHex(byte[] bytes) {
        return bytesToHex(bytes, null);
    }

    private static Byte[] toObjects(byte[] bytes) {
        Byte[] bytesObjects = new Byte[bytes.length];
        Arrays.setAll(bytesObjects, n -> bytes[n]);
        return bytesObjects;
    }

    /**
     * Converts an hex string (eg. format "01-23-45") into a byte array
     *
     * @param hexString the hex string
     * @param delimiter a delimiter that was placed between every two bytes
     * @return the corresponding byte array
     */
    public static byte[] hexToBytes(String hexString, String delimiter) {
        // first convert to upper case to ease the rest
        String ucHexString = hexString.toUpperCase();

        Byte[] bytesObjects = Arrays.stream(ucHexString.split(delimiter)).map(s -> {
            if (s.length() != 2) {
                throw new IllegalArgumentException("hexString needs to have an even length: " + hexString);
            }

            return (byte) (hexCharacterToBin(s.charAt(0)) * 16 + hexCharacterToBin(s.charAt(1)));
        }).toArray(Byte[]::new);

        byte[] bytes = new byte[bytesObjects.length];
        for (int i = 0; i < bytesObjects.length; i++) {
            bytes[i] = bytesObjects[i];
        }
        return bytes;
    }

    /**
     * Converts an hex string (in format "0123456789ABCDEF") into a byte array
     *
     * @param hexString the hex string
     * @return the corresponding byte array
     */
    public static byte[] hexToBytes(String hexString) {
        return hexToBytes(hexString, "(?<=\\G.{2})");
    }

    /**
     * Convert an upper case hex character to a byte
     *
     * @param chacacter an upper case hex character
     * @return the byte value of the character
     * @throws IllegalArgumentException if a value is found which is not an upper case hex character
     */
    private static byte hexCharacterToBin(char character) {
        if ('0' <= character && character <= '9') {
            return (byte) (character - ASCII_DIGITS_START_POSITION);
        } else if ('A' <= character && character <= 'F') {
            return (byte) (character - ASCII_UPPERCASE_LETTERS_START_POSITION + 10);
        } else {
            throw new IllegalArgumentException("hexString contains illegal character for hexToBytes: " + character);
        }
    }
}
