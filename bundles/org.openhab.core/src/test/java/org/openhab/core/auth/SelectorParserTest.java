/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.auth;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SelectorParser}.
 *
 * @author Gabor Bicskei - Initial contribution
 */
@NonNullByDefault
public class SelectorParserTest {

    @Test
    public void testParseWildcard() {
        Selector selector = SelectorParser.parse("*");
        assertInstanceOf(AllSelector.class, selector);
        assertTrue(selector.matches("anything"));
        assertEquals("*", selector.expression());
    }

    @Test
    public void testParseById() {
        Selector selector = SelectorParser.parse("id:LivingRoom_Light");
        assertInstanceOf(ByIdSelector.class, selector);
        assertTrue(selector.matches("LivingRoom_Light"));
        assertFalse(selector.matches("Kitchen_Light"));
        assertEquals("id:LivingRoom_Light", selector.expression());
    }

    @Test
    public void testParseByGroup() {
        Selector selector = SelectorParser.parse("group:gLights");
        assertInstanceOf(ByGroupSelector.class, selector);
        assertFalse(selector.matches("anything")); // requires entity access
        assertEquals("group:gLights", selector.expression());
        assertEquals("gLights", ((ByGroupSelector) selector).group());
    }

    @Test
    public void testParseByTag() {
        Selector selector = SelectorParser.parse("tag:Lighting");
        assertInstanceOf(ByTagSelector.class, selector);
        assertFalse(selector.matches("anything"));
        assertEquals("tag:Lighting", selector.expression());
        assertEquals("Lighting", ((ByTagSelector) selector).tag());
    }

    @Test
    public void testParseByLocation() {
        Selector selector = SelectorParser.parse("location:LivingRoom");
        assertInstanceOf(ByLocationSelector.class, selector);
        assertFalse(selector.matches("anything"));
        assertEquals("location:LivingRoom", selector.expression());
        assertEquals("LivingRoom", ((ByLocationSelector) selector).location());
    }

    @Test
    public void testParseInvalidNoColonThrows() {
        assertThrows(IllegalArgumentException.class, () -> SelectorParser.parse("invalid"));
    }

    @Test
    public void testParseUnknownPrefixThrows() {
        assertThrows(IllegalArgumentException.class, () -> SelectorParser.parse("unknown:value"));
    }

    @Test
    public void testAllSelectorEquality() {
        assertEquals(new AllSelector(), new AllSelector());
    }

    @Test
    public void testByIdSelectorEquality() {
        assertEquals(new ByIdSelector("item1"), new ByIdSelector("item1"));
        assertNotEquals(new ByIdSelector("item1"), new ByIdSelector("item2"));
    }

    @Test
    public void testByGroupSelectorEquality() {
        assertEquals(new ByGroupSelector("gLights"), new ByGroupSelector("gLights"));
        assertNotEquals(new ByGroupSelector("gLights"), new ByGroupSelector("gHeating"));
    }

    @Test
    public void testByTagSelectorEquality() {
        assertEquals(new ByTagSelector("Lighting"), new ByTagSelector("Lighting"));
        assertNotEquals(new ByTagSelector("Lighting"), new ByTagSelector("Heating"));
    }

    @Test
    public void testByLocationSelectorEquality() {
        assertEquals(new ByLocationSelector("LivingRoom"), new ByLocationSelector("LivingRoom"));
        assertNotEquals(new ByLocationSelector("LivingRoom"), new ByLocationSelector("Kitchen"));
    }

    @Test
    public void testAllSelectorSingleton() {
        assertSame(AllSelector.INSTANCE, SelectorParser.parse("*"));
    }

    @Test
    public void testDifferentSelectorTypesNotEqual() {
        assertNotEquals(new AllSelector(), new ByIdSelector("*"));
        assertNotEquals(new ByIdSelector("gLights"), new ByGroupSelector("gLights"));
        assertNotEquals(new ByTagSelector("tag"), new ByLocationSelector("tag"));
    }
}
