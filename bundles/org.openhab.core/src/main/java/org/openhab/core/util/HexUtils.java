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

import java.nio.charset.StandardCharsets;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Static utility methods that are helpful when dealing with hex data and byte arrays.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Martin van Wingerden - Implemented the reverse operation
 */
@NonNullByDefault
public class HexUtils {

    // used for hex conversions
    private static final byte[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E',
            'F' };

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
        return bytesToHexInt(bytes, delimiter != null ? delimiter : "");
    }

    private static String bytesToHexInt(byte[] bytes, CharSequence delimiter) {
        if (bytes.length == 0) {
            return "";
        }
        final StringBuilder sb = new StringBuilder(bytes.length * 2 + delimiter.length() * (bytes.length - 1));
        int pos = 0;
        while (pos < bytes.length - 1) {
            final byte[] hex = byteToHex(bytes[pos++]);
            sb.append((char) hex[0]);
            sb.append((char) hex[1]);
            sb.append(delimiter);
        }
        final byte[] hex = byteToHex(bytes[pos++]);
        sb.append((char) hex[0]);
        sb.append((char) hex[1]);
        return sb.toString();
    }

    /**
     * Converts a byte array into a hex string (in format "0123456789ABCDEF").
     *
     * @param bytes the byte array
     * @return the corresponding hex string
     */
    public static String bytesToHex(byte[] bytes) {
        return bytesToHexInt(bytes, "");
    }

    public static byte[] byteToHex(final byte value) {
        final byte[] out = new byte[2];
        out[0] = HEX[(value >>> 4) & 0x0F];
        out[1] = HEX[value & 0x0F];
        return out;
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

        final String[] splitted = ucHexString.split(delimiter);

        final byte[] bytes = new byte[splitted.length];
        int pos = 0;
        for (final String part : splitted) {
            final byte[] in = part.getBytes(StandardCharsets.UTF_8);
            if (in.length != 2) {
                throw new IllegalArgumentException("hexString needs to have an even length: " + hexString);
            }
            bytes[pos++] = (byte) (unhex(in[0]) << 4 | unhex(in[1]));
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

    public static byte hexToByte(byte high, byte low) {
        return (byte) ((unhex(high) << 4) | unhex(low));
    }

    /**
     * Convert an hex byte created by this utilities back.
     *
     * @param value the byte created by the hex array
     * @return the byte value
     * @throws IllegalArgumentException if a value is invalid
     */
    private static byte unhex(byte value) {
        if ('0' <= value && value <= '9') {
            return (byte) (value - ASCII_DIGITS_START_POSITION);
        } else if ('A' <= value && value <= 'F') {
            return (byte) (value - ASCII_UPPERCASE_LETTERS_START_POSITION + 10);
        } else {
            throw new IllegalArgumentException("hexString contains illegal character for hexToBytes: " + value);
        }
    }
}
