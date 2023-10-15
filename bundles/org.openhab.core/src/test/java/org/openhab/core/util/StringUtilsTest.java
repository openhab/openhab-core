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

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * The {@link StringUtils} class defines some static string utility methods
 *
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
public class StringUtilsTest {

    @Test
    public void chompTest() {
        assertEquals("", StringUtils.chomp(""));
        assertEquals(null, StringUtils.chomp(null));
        assertEquals("abc ", StringUtils.chomp("abc \r"));
        assertEquals("abc", StringUtils.chomp("abc\n"));
        assertEquals("abc", StringUtils.chomp("abc\r\n"));
        assertEquals("abc\r\n", StringUtils.chomp("abc\r\n\r\n"));
        assertEquals("abc\n", StringUtils.chomp("abc\n\r"));
        assertEquals("abc\n\rabc", StringUtils.chomp("abc\n\rabc"));
        assertEquals("", StringUtils.chomp("\r"));
        assertEquals("", StringUtils.chomp("\n"));
        assertEquals("", StringUtils.chomp("\r\n"));
    }

    @Test
    public void escapeXmlTest() {
        assertEquals(null, StringUtils.escapeXml(null));
        assertEquals(" ", StringUtils.escapeXml(" "));
        assertEquals("invalidxml", StringUtils.escapeXml("invalidxml"));
        assertEquals("&lt;xmlExample&gt;&amp;&lt;/xmlExample&gt;", StringUtils.escapeXml("<xmlExample>&</xmlExample>"));
        assertEquals("&lt;xmlExample&gt;&quot;&lt;/xmlExample&gt;",
                StringUtils.escapeXml("<xmlExample>\"</xmlExample>"));
        assertEquals("&lt;xmlExample&gt;&apos;&lt;/xmlExample&gt;",
                StringUtils.escapeXml("<xmlExample>\'</xmlExample>"));
    }

    @Test
    public void capitalizeTest() {
        assertEquals(null, StringUtils.capitalize(null));
        assertEquals(" ", StringUtils.capitalize(" "));
        assertEquals("Cat", StringUtils.capitalize("cat"));
        assertEquals("CAt", StringUtils.capitalize("cAt"));
        assertEquals("'cat'", StringUtils.capitalize("'cat'"));
    }

    @Test
    public void capitalizeAllWordsTest() {
        assertEquals(null, StringUtils.capitalizeByUnderscore(null));
        assertEquals("Openhab_Is_Cool", StringUtils.capitalizeByUnderscore("openHAB_is_cool"));
        assertEquals("Foobar_Example", StringUtils.capitalizeByUnderscore("foobar_Example"));
        assertEquals("'another_Test'", StringUtils.capitalizeByUnderscore("'another_test'"));
    }

    @Test
    public void capitalizeWordsTest() {
        assertEquals("OpenHAB Is Cool", StringUtils.capitalizeByWhitespace("openHAB is cool"));
        assertEquals("Foobar Example", StringUtils.capitalizeByWhitespace("foobar Example"));
        assertEquals("'another Test'", StringUtils.capitalizeByWhitespace("'another test'"));
    }

    @Test
    public void getRandomString() {
        String randomstring = StringUtils.getRandomString(10000, "123");
        assertEquals(6666, randomstring.replace("1", "").length(), 333,
                "randomString does not equaly (<5% delta) use all characters in set");
    }

    @Test
    public void padLeft() {
        assertEquals("000000", StringUtils.padLeft("", 6, "0"));
        assertEquals("000000", StringUtils.padLeft(null, 6, "0"));
        assertEquals("000teststr", StringUtils.padLeft("teststr", 10, "0"));
        assertEquals("AAAAAAp3RF@CT", StringUtils.padLeft("p3RF@CT", 13, "A"));
        assertEquals("nopaddingshouldhappen", StringUtils.padLeft("nopaddingshouldhappen", 21, "x"));
        assertEquals("LongerStringThenMinSize", StringUtils.padLeft("LongerStringThenMinSize", 10, "x"));
    }

    @Test
    public void splitByCharacterType() {
        assertArrayEquals(new String[0], StringUtils.splitByCharacterType(null));
        assertArrayEquals(new String[0], StringUtils.splitByCharacterType(""));
        assertArrayEquals(new String[] { "ab", " ", "de", " ", "fg" }, StringUtils.splitByCharacterType("ab de fg"));
        assertArrayEquals(new String[] { "ab", "   ", "de", " ", "fg" },
                StringUtils.splitByCharacterType("ab   de fg"));
        assertArrayEquals(new String[] { "ab", ":", "cd", ":", "ef" }, StringUtils.splitByCharacterType("ab:cd:ef"));
        assertArrayEquals(new String[] { "number", "5" }, StringUtils.splitByCharacterType("number5"));
        assertArrayEquals(new String[] { "foo", "Bar" }, StringUtils.splitByCharacterType("fooBar"));
        assertArrayEquals(new String[] { "foo", "200", "Bar" }, StringUtils.splitByCharacterType("foo200Bar"));
        assertArrayEquals(new String[] { "ASF", "Rules" }, StringUtils.splitByCharacterType("ASFRules"));
    }
}
