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
     * There are five XML Special characters which needs to be escaped:
     * 
     * <pre>
     * & - &amp;
     * < - &lt;
     * > - &gt;
     * " - &quot;
     * ' - &apos;
     * </pre>
     *
     * @param xml the input xml as String to escape, may not be null
     * @return the escaped xml as String
     */
    public static String escapeXml(String xml) {
        xml = xml.replace("&", "&amp;");
        xml = xml.replace("<", "&lt;");
        xml = xml.replace(">", "&gt;");
        xml = xml.replace("\"", "&quot;");
        xml = xml.replace("'", "&apos;");
        return xml;
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
     * @param val the String to capitalize, may not be null
     * @return the capitalized String
     */
    public static String capitalize(String val) {
        if (val.isEmpty()) {
            return val;
        }
        StringBuilder sb = new StringBuilder(val);
        sb.setCharAt(0, Character.toUpperCase(val.charAt(0)));
        return sb.toString();
    }

    /**
     * Capitalizes words in the string. Only the first char of every word is capitalized, other are set to lowercasing.
     * Words are recognized by an underscore delimiter
     *
     * <pre>
      * "openHAB_is_cool"   => "Openhab Is Cool"
      * "foobar_Example" => "Foobar Example"
     * </pre>
     * 
     * @param input the String to capitalize, may not be null
     * @return the capitalized String
     */
    public static String capitalizeFully(String input) {
        final String delimiter = "_";
        StringBuilder capitalizedFully = new StringBuilder();
        for (String str : input.split(delimiter)) {
            if (str.length() > 0) {
                capitalizedFully.append(str.substring(0, 1).toUpperCase());
            }
            if (str.length() > 1) {
                capitalizedFully.append(str.substring(1).toLowerCase());
            }
        }

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
     * @param input the String to capitalize, may be null
     * @return the capitalized String
     */
    public static String capitalizeWords(@Nullable String input) {
        if (input == null) {
            return "";
        }

        StringBuilder processed = new StringBuilder();
        for (String splitted : input.split("\\s+")) {
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
     * @param input the String to pad, may be null
     * @param minSize the minimum String size to return
     * @param padString the String to add when padding
     * @return the padded String
     */
    public static String padLeft(@Nullable String input, int minSize, String padString) {
        if (input == null) {
            input = "";
        }
        return String.format("%" + minSize + "s", input).replace(" ", padString);
    }

    /**
     * Creates a random string
     * 
     * @param length the length of the String to return
     * @param charset the characters to use to create the String
     * @return the random String
     */
    public static String randomString(int length, String charset) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = (int) (charset.length() * Math.random());
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
    public static String randomAlphabetic(int length) {
        return StringUtils.randomString(length, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvxyz");
    }

    /**
     * Creates a random string with only hex characters
     * 
     * @param length the length of the String to return
     * @return the random String
     */
    public static String randomHex(int length) {
        return StringUtils.randomString(length, "0123456789ABCDEF");
    }

    /**
     * Creates a random string with [A-Za-z0-9] characters
     * 
     * @param length the length of the String to return
     * @return the random String
     */
    public static String randomAlphanummeric(int length) {
        return StringUtils.randomString(length, "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvxyz");
    }

    /**
     * Splits the string by character type into an array
     * 
     * <pre>
     * "ab de fg"   => "ab", " ", "de", " ", "fg";
     * "ab   de fg" => "ab", " ", "de", " ", "fg";
     * "ab:cd:ef"   => "ab", ":", "cd", ":", "ef";
     * "number5"    =>  "number", "5"
     * "fooBar"     =>  "foo", "Bar"
     * "OHRules"    =>  "OH", "Rules"
     * </pre>
     * 
     * @param input the input String to split, may be null
     * @return the splitted String
     */
    public static String[] splitByCharacterType(@Nullable String input) {
        if (input == null) {
            return new String[0];
        }
        if (input.isBlank()) {
            return new String[0];
        }
        List<String> cache = new ArrayList<>();
        char[] inputAsCharArray = input.toCharArray();
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
     * @param input the input xml as String to unescape, may not be null
     * @return the unescaped xml as String
     */
    public static String unEscapeXml(String xml) {
        xml = xml.replace("&amp;", "&");
        xml = xml.replace("&lt;", "<");
        xml = xml.replace("&gt;", ">");
        xml = xml.replace("&quot;", "\"");
        xml = xml.replace("&apos;", "'");
        return xml;
    }
}
