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
package org.openhab.core.addon;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.common.Version;

/**
 * The {@link AddonTest} contains tests for the {@link Addon} class.
 *
 * @author - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AddonTest {

    @Test
    public void testBasics() {
        assertThrows(IllegalArgumentException.class, () -> new Addon(null, null, null, null, null, null, false, null,
                null, null, false, false, null, null, null, null, null, null, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new Addon(" ", null, null, null, null, null, false, null,
                null, null, false, false, null, null, null, null, null, null, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new Addon("test", null, null, null, null, null, false, null,
                null, null, false, false, null, null, null, null, null, null, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new Addon("test", "\t", null, null, null, null, false, null,
                null, null, false, false, null, null, null, null, null, null, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new Addon("test", "binding", null, null, null, null, false,
                null, null, null, false, false, null, null, null, null, null, null, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new Addon("test", "binding", "", null, null, null, false,
                null, null, null, false, false, null, null, null, null, null, null, null, null, null, null, null));

        Addon addon = new Addon("testuid", "binding", "testid", null, null, null, false, null, null, null, false, false,
                null, null, null, null, null, null, null, null, null, null, null);
        assertTrue(addon.getProperties().isEmpty());

        addon = new Addon("testuid", "binding", "testid", null, null, null, false, null, null, null, false, false, null,
                null, null, null, List.of("DE", "PL", "UA"), null, null, null, null, null, null);
        assertEquals(3, addon.getCountries().size());
        assertTrue(addon.getProperties().isEmpty());

        addon = new Addon("testuid", "binding", "testid", null, null, null, false, null, null, null, false, false, null,
                null, null, null, List.of("DE", "PL", "UA"), null, null, null, null,
                Map.of("key1", "value1", "key2", "value2"), null);
        assertEquals(3, addon.getCountries().size());
        assertEquals(2, addon.getProperties().size());

        addon = new Addon("testuid", "binding", "testid", null, null, null, false, null, null, null, false, false, null,
                null, null, null, List.of("DE", "PL", "UA"), null, null, null, null,
                Map.of("key1", "value1", "key2", "value2"), List.of("com.example.addon"));
        assertEquals(3, addon.getCountries().size());
        assertEquals(2, addon.getProperties().size());
        assertEquals(1, addon.getLoggerPackages().size());

        addon = new Addon("testuid", "automation", "testid", "Test", Version.valueOf("0.9"), "stable", false,
                "application/x-test", "http://example.com", "Santa", true, false, "None", "Still none", null,
                "nothing, none", List.of("US"), "GPL", "none", "red", "http://image.exammple.com", null,
                List.of("com.example"));
        assertEquals(1, addon.getCountries().size());
        assertEquals(0, addon.getProperties().size());
        assertEquals(1, addon.getLoggerPackages().size());
        assertEquals("testuid", addon.getUid());
        assertEquals("automation", addon.getType());
        assertEquals("testid", addon.getId());
        assertEquals("Test", addon.getLabel());
        assertEquals(Version.valueOf("0.9"), addon.getVersion());
        assertEquals("stable", addon.getMaturity());
        assertFalse(addon.getCompatible());
        assertEquals("application/x-test", addon.getContentType());
        assertEquals("http://example.com", addon.getLink());
        assertEquals("Santa", addon.getAuthor());
        assertTrue(addon.isVerifiedAuthor());
        assertFalse(addon.isInstalled());
        assertEquals("None", addon.getDescription());
        assertEquals("Still none", addon.getDetailedDescription());
        assertMapsEquals(Map.of(), addon.getProperties());
        assertEquals("nothing, none", addon.getKeywords());
        assertIterableEquals(List.of("US"), addon.getCountries());
        assertEquals("GPL", addon.getLicense());
        assertEquals("none", addon.getConnection());
        assertEquals("red", addon.getBackgroundColor());
        assertEquals("http://image.exammple.com", addon.getImageLink());
        assertEquals("", addon.getConfigDescriptionURI());
        assertIterableEquals(List.of("com.example"), addon.getLoggerPackages());
        addon.setInstalled(true);
        assertTrue(addon.isInstalled());
    }

    @Test
    public void testBuilder() {
        Addon.Builder b = Addon.create("uid");
        assertThrows(IllegalArgumentException.class, () -> b.build());
        assertThrows(IllegalArgumentException.class, () -> b.withType("ui").build());
        assertEquals("ui", b.withId("id").build().getType());
        assertEquals("TLabel", b.withLabel("TLabel").build().getLabel());
        assertEquals(Version.EMPTY_VERSION, b.withVersion(new Version(0, 0, 0)).build().getVersion());
        assertEquals("beta", b.withMaturity("beta").build().getMaturity());
        assertTrue(b.withCompatible(true).build().getCompatible());
        assertEquals("img/gif", b.withContentType("img/gif").build().getContentType());
        assertEquals("http://link.example.com", b.withLink("http://link.example.com").build().getLink());
        assertEquals("Nadar", b.withAuthor("Nadar").build().getAuthor());
        assertTrue(b.withInstalled(true).build().isInstalled());
        assertEquals("Nadar", b.withAuthor("Nadar", true).build().getAuthor());
        assertTrue(b.build().isVerifiedAuthor());
        assertEquals("Description", b.withDescription("Description").build().getDescription());
        assertEquals("Detailed description",
                b.withDetailedDescription("Detailed description").build().getDetailedDescription());
        assertEquals("", b.withConfigDescriptionURI(null).build().getConfigDescriptionURI());
        assertEquals("smart, light", b.withKeywords("smart, light").build().getKeywords());
        assertTrue(b.withCountries(null).build().getCountries().isEmpty());
        assertNull(b.getCountries());
        assertEquals("EPL", b.withLicense("EPL").build().getLicense());
        assertEquals("local", b.withConnection("local").build().getConnection());
        assertEquals("green", b.withBackgroundColor("green").build().getBackgroundColor());
        assertEquals("http://image.example.com", b.withImageLink("http://image.example.com").build().getImageLink());
        assertMapsEquals(Map.of("priority", Double.valueOf(2d)),
                b.withProperty("priority", Double.valueOf(2d)).build().getProperties());
        b.withProperties(Map.of("link", "http://example.com", "fresh", Boolean.FALSE));
        assertThat(b.build().getProperties(), hasEntry("link", "http://example.com"));
        assertThat(b.build().getProperties(), hasEntry("fresh", Boolean.FALSE));
        assertIterableEquals(List.of("com.example.basic", "com.example.advanced"),
                b.withLoggerPackages(List.of("com.example.basic", "com.example.advanced")).build().getLoggerPackages());
        assertIterableEquals(List.of("com.example.basic", "com.example.advanced"), b.getLoggerPackages());

        Addon addon = b.build();
        Addon addon2 = Addon.create(addon).withCompatible(false).build();
        assertTrue(addon.getCompatible());
        assertFalse(addon2.getCompatible());

        assertEquals(addon.getType(), addon2.getType());
        assertEquals(addon.getLabel(), addon2.getLabel());
        assertEquals(addon.getVersion(), addon2.getVersion());
        assertEquals(addon.getMaturity(), addon2.getMaturity());
        assertEquals(addon.getContentType(), addon2.getContentType());
        assertEquals(addon.getLink(), addon2.getLink());
        assertEquals(addon.getAuthor(), addon2.getAuthor());
        assertEquals(addon.isVerifiedAuthor(), addon2.isVerifiedAuthor());
        assertEquals(addon.isInstalled(), addon2.isInstalled());
        assertEquals(addon.getDescription(), addon2.getDescription());
        assertEquals(addon.getDetailedDescription(), addon2.getDetailedDescription());
        assertEquals(addon.getConfigDescriptionURI(), addon2.getConfigDescriptionURI());
        assertEquals(addon.getKeywords(), addon2.getKeywords());
        assertIterableEquals(addon.getCountries(), addon2.getCountries());
        assertEquals(addon.getLicense(), addon2.getLicense());
        assertEquals(addon.getConnection(), addon2.getConnection());
        assertEquals(addon.getBackgroundColor(), addon2.getBackgroundColor());
        assertEquals(addon.getImageLink(), addon2.getImageLink());
        assertMapsEquals(addon.getProperties(), addon2.getProperties());
        assertIterableEquals(addon.getLoggerPackages(), addon2.getLoggerPackages());
    }

    private void assertMapsEquals(@Nullable Map<?, ?> a, @Nullable Map<?, ?> b) {
        if (a == null || b == null) {
            assertTrue(a == null && b == null);
            return;
        }
        assertEquals(a.size(), b.size());
        if (a instanceof SortedMap && b instanceof SortedMap) {
            Iterator<?> iterator = b.entrySet().iterator();
            Object o;
            for (Entry<?, ?> entry : a.entrySet()) {
                o = iterator.next();
                assertEquals(entry.getKey(), ((Entry<?, ?>) o).getKey());
                assertEquals(entry.getValue(), ((Entry<?, ?>) o).getValue());
            }
        } else {
            for (Entry<?, ?> entry : a.entrySet()) {
                assertEquals(entry.getValue(), b.get(entry.getKey()));
            }
        }
    }
}
