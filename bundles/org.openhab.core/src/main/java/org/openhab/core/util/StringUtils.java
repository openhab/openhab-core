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
package org.openhab.core.util;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Static utility methods that are helpful when dealing with strings.
 *
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
public class StringUtils {

    /**
     * If a newline char exists at the end of the line, it is removed
     *
     * <pre>
     * Util.chomp(null)          = null
     * Util.chomp("")            = ""
     * Util.chomp("abc \r")      = "abc "
     * Util.chomp("abc\n")       = "abc"
     * Util.chomp("abc\r\n")     = "abc"
     * Util.chomp("abc\r\n\r\n") = "abc\r\n"
     * Util.chomp("abc\n\r")     = "abc\n"
     * Util.chomp("abc\n\rabc")  = "abc\n\rabc"
     * Util.chomp("\r")          = ""
     * Util.chomp("\n")          = ""
     * Util.chomp("\r\n")        = ""
     * </pre>
     *
     * @param str the input String to escape, may be null
     * @return the chomped string, may be null
     */
    public static @Nullable String chomp(final @Nullable String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        if (str.endsWith("\r\n")) {
            return str.substring(0, str.length() - 2);
        } else if (str.endsWith("\r") || str.endsWith("\n")) {
            return str.substring(0, str.length() - 1);
        } else {
            return str;
        }
    }

    /**
     * Simple method to escape XML special characters in String.
     * There are five XML special characters which needs to be escaped:
     *
     * <pre>
     * & - &amp;
     * < - &lt;
     * > - &gt;
     * " - &quot;
     * ' - &apos;
     * </pre>
     *
     * @param str the input xml as String to escape, may be null
     * @return the escaped xml as String, may be null
     */
    public static @Nullable String escapeXml(@Nullable String str) {
        if (str == null || str.isEmpty()) {
            return str;
        } else {
            return str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
                    .replace("'", "&apos;");
        }
    }

    /**
     * Capitalizes a String changing the first character to title case.
     * No other characters are changed.
     *
     * <pre>
     * ""       => ""
     * "cat"    => "Cat"
     * "cAt"    => "CAt"
     * "'cat'"  => "'cat'"
     * </pre>
     *
     * @param val the String to capitalize, may be null
     * @return the capitalized String, may be null
     */
    public static @Nullable String capitalize(@Nullable String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        StringBuilder sb = new StringBuilder(str);
        sb.setCharAt(0, Character.toUpperCase(str.charAt(0)));
        return sb.toString();
    }

    /**
     * Capitalizes words in the string. Only the first char of every word is capitalized, other are set to lowercase.
     * Words are recognized by an underscore delimiter.
     *
     * <pre>
      * "openHAB_is_cool"   => "Openhab_Is_Cool"
      * "foobar_Example" => "Foobar_Example"
     * </pre>
     *
     * @param str the String to capitalize, may be null
     * @return the capitalized String, may be null
     */
    public static @Nullable String capitalizeByUnderscore(@Nullable String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        final String delimiter = "_";
        StringBuilder capitalizedFully = new StringBuilder();
        for (String splitStr : str.split(delimiter)) {
            if (splitStr.length() > 0) {
                capitalizedFully.append(splitStr.substring(0, 1).toUpperCase());
            }
            if (splitStr.length() > 1) {
                capitalizedFully.append(splitStr.substring(1).toLowerCase());
            }
            capitalizedFully.append(delimiter);
        }

        capitalizedFully.setLength(Math.max(capitalizedFully.length() - 1, 0));
        return capitalizedFully.toString();
    }

    /**
     * Capitalizes words in the string. Only the first char of every word is capitalized, the rest is left as is.
     * Words are recognized by any whitespace.
     *
     * <pre>
      * "openHAB is cool"   => "OpenHAB Is Cool"
      * "foobar Example" => "Foobar Example"
     * </pre>
     *
     * @param str the String to capitalize, may be null
     * @return the capitalized String, may be null
     */
    public static @Nullable String capitalizeByWhitespace(@Nullable String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        StringBuilder processed = new StringBuilder();
        for (String splitted : str.split("\\s+")) {
            if (splitted.length() > 1) {
                processed.append(splitted.substring(0, 1).toUpperCase());
                processed.append(splitted.substring(1));
            } else {
                processed.append(splitted.toUpperCase());
            }
            processed.append(" ");
        }
        processed.setLength(Math.max(processed.length() - 1, 0));
        return processed.toString();
    }

    /**
     * Pads the string from the left
     *
     * <pre>
      * padLeft("9", 4, "0")        => "0009"
      * padLeft("3112", 12, "*")    => "********3112"
      * padLeft("openHAB", 4, "*")  => "openHAB"
     * </pre>
     *
     * @param str the String to pad, may be null
     * @param minSize the minimum String size to return
     * @param padString the String to add when padding
     * @return the padded String
     */
    public static String padLeft(@Nullable String str, int minSize, String padString) {
        String paddedString = str == null ? "" : str;
        return String.format("%" + minSize + "s", paddedString).replace(" ", padString);
    }

    /**
     * Creates a random string
     *
     * @param length the length of the String to return
     * @param charset the characters to use to create the String
     * @return the random String
     */
    public static String getRandomString(int length, String charset) {
        StringBuilder sb = new StringBuilder(length);
        SecureRandom secureRandom = new SecureRandom();
        for (int i = 0; i < length; i++) {
            final int index = secureRandom.nextInt(charset.length());
            sb.append(charset.charAt(index));
        }

        return sb.toString();
    }

    /**
     * Creates a random string with [A-Zaz] characters
     *
     * @param length the length of the String to return
     * @return the random String
     */
    public static String getRandomAlphabetic(int length) {
        return StringUtils.getRandomString(length, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvxyz");
    }

    /**
     * Creates a random string with only hex characters
     *
     * @param length the length of the String to return
     * @return the random String
     */
    public static String getRandomHex(int length) {
        return StringUtils.getRandomString(length, "0123456789ABCDEF");
    }

    /**
     * Creates a random string with [A-Za-z0-9] characters
     *
     * @param length the length of the String to return
     * @return the random String
     */
    public static String getRandomAlphanumeric(int length) {
        return StringUtils.getRandomString(length, "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvxyz");
    }

    /**
     * Splits the string by character type into an array
     *
     * <pre>
     * "ab de fg"   => "ab", " ", "de", " ", "fg";
     * "ab   de fg" => "ab", "   ", "de", " ", "fg";
     * "ab:cd:ef"   => "ab", ":", "cd", ":", "ef";
     * "number5"    =>  "number", "5"
     * "fooBar"     =>  "foo", "Bar"
     * "OHRules"    =>  "OH", "Rules"
     * </pre>
     *
     * @param str the input String to split, may be null
     * @return the splitted String
     */
    public static String[] splitByCharacterType(@Nullable String str) {
        if (str == null || str.isBlank()) {
            return new String[0];
        }

        List<String> cache = new ArrayList<>();
        char[] inputAsCharArray = str.toCharArray();
        int prevType = Character.getType(inputAsCharArray[0]);
        int prevTypeStart = 0;
        for (int i = prevTypeStart + 1; i < inputAsCharArray.length; i++) {
            int curType = Character.getType(inputAsCharArray[i]);
            if (prevType == curType) {
                continue;
            }
            if (curType == Character.LOWERCASE_LETTER && prevType == Character.UPPERCASE_LETTER) {
                int tmpStart = i - 1;
                if (tmpStart != prevTypeStart) {
                    cache.add(new String(inputAsCharArray, prevTypeStart, tmpStart - prevTypeStart));
                    prevTypeStart = tmpStart;
                }
            } else {
                cache.add(new String(inputAsCharArray, prevTypeStart, i - prevTypeStart));
                prevTypeStart = i;
            }
            prevType = curType;
        }
        cache.add(new String(inputAsCharArray, prevTypeStart, inputAsCharArray.length - prevTypeStart));
        return cache.toArray(String[]::new);
    }

    /**
     * Simple method to un escape XML special characters in String.
     * There are five XML Special characters which needs to be escaped:
     *
     * <pre>
     * & => &amp;
     * < => &lt;
     * > => &gt;
     * " => &quot;
     * ' => &apos;
     * </pre>
     *
     * @param input the input xml as String to unescape, may be null
     * @return the unescaped xml as String, may be null
     */
    public static @Nullable String unEscapeXml(@Nullable String str) {
        if (str == null || str.isEmpty()) {
            return str;
        } else {
            return str.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                    .replace("&apos;", "'");
        }
    }
}
