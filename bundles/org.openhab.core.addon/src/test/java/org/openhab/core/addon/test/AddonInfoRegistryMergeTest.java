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
package org.openhab.core.addon.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.openhab.core.addon.AddonDiscoveryMethod;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.addon.AddonInfoProvider;
import org.openhab.core.addon.AddonInfoRegistry;
import org.openhab.core.addon.AddonMatchProperty;

/**
 * JUnit test for the {@link AddonInfoRegistry} merge function.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
@TestInstance(Lifecycle.PER_CLASS)
class AddonInfoRegistryMergeTest {

    private @Nullable AddonInfoProvider addonInfoProvider0;
    private @Nullable AddonInfoProvider addonInfoProvider1;
    private @Nullable AddonInfoProvider addonInfoProvider2;

    @BeforeAll
    void beforeAll() {
        addonInfoProvider0 = createAddonInfoProvider0();
        addonInfoProvider1 = createAddonInfoProvider1();
        addonInfoProvider2 = createAddonInfoProvider2();
    }

    private AddonInfoProvider createAddonInfoProvider0() {
        AddonInfo addonInfo = AddonInfo.builder("hue", "binding").withName("name-zero")
                .withDescription("description-zero").build();
        AddonInfoProvider provider = mock(AddonInfoProvider.class);
        when(provider.getAddonInfo(anyString(), any(Locale.class))).thenReturn(null);
        when(provider.getAddonInfo(anyString(), eq(null))).thenReturn(null);
        when(provider.getAddonInfo(eq("binding-hue"), any(Locale.class))).thenReturn(addonInfo);
        when(provider.getAddonInfo(eq("binding-hue"), eq(null))).thenReturn(null);
        return provider;
    }

    private AddonInfoProvider createAddonInfoProvider1() {
        AddonDiscoveryMethod discoveryMethod = new AddonDiscoveryMethod().setServiceType("mdns")
                .setMdnsServiceType("_hue._tcp.local.");
        AddonInfo addonInfo = AddonInfo.builder("hue", "binding").withName("name-one")
                .withDescription("description-one").withCountries("GB,NL").withConnection("local")
                .withDiscoveryMethods(List.of(discoveryMethod)).build();
        AddonInfoProvider provider = mock(AddonInfoProvider.class);
        when(provider.getAddonInfo(anyString(), any(Locale.class))).thenReturn(null);
        when(provider.getAddonInfo(anyString(), eq(null))).thenReturn(null);
        when(provider.getAddonInfo(eq("binding-hue"), any(Locale.class))).thenReturn(addonInfo);
        when(provider.getAddonInfo(eq("binding-hue"), eq(null))).thenReturn(null);
        return provider;
    }

    private AddonInfoProvider createAddonInfoProvider2() {
        AddonDiscoveryMethod discoveryMethod = new AddonDiscoveryMethod().setServiceType("upnp")
                .setMatchProperties(List.of(new AddonMatchProperty("modelName", "Philips hue bridge")));
        AddonInfo addonInfo = AddonInfo.builder("hue", "binding").withName("name-two")
                .withDescription("description-two").withCountries("DE,FR").withSourceBundle("source-bundle")
                .withConfigDescriptionURI("http://www.openhab.org").withDiscoveryMethods(List.of(discoveryMethod))
                .build();
        AddonInfoProvider provider = mock(AddonInfoProvider.class);
        when(provider.getAddonInfo(anyString(), any(Locale.class))).thenReturn(null);
        when(provider.getAddonInfo(anyString(), eq(null))).thenReturn(null);
        when(provider.getAddonInfo(eq("binding-hue"), any(Locale.class))).thenReturn(addonInfo);
        when(provider.getAddonInfo(eq("binding-hue"), eq(null))).thenReturn(null);
        return provider;
    }

    /**
     * Test fetching a single addon-info from the registry with no merging.
     */
    @Test
    void testGetOneAddonInfo() {
        AddonInfoRegistry registry = new AddonInfoRegistry();
        assertNotNull(addonInfoProvider0);
        registry.addAddonInfoProvider(Objects.requireNonNull(addonInfoProvider0));

        AddonInfo addonInfo;
        addonInfo = registry.getAddonInfo("aardvark", Locale.US);
        assertNull(addonInfo);
        addonInfo = registry.getAddonInfo("aardvark", null);
        assertNull(addonInfo);
        addonInfo = registry.getAddonInfo("binding-hue", null);
        assertNull(addonInfo);
        addonInfo = registry.getAddonInfo("binding-hue", Locale.US);
        assertNotNull(addonInfo);

        assertEquals("hue", addonInfo.getId());
        assertEquals("binding", addonInfo.getType());
        assertEquals("binding-hue", addonInfo.getUID());
        assertTrue(addonInfo.getName().startsWith("name-"));
        assertTrue(addonInfo.getDescription().startsWith("description-"));
        assertNotEquals("local", addonInfo.getConnection());
        assertEquals(0, addonInfo.getCountries().size());
        assertNotEquals("http://www.openhab.org", addonInfo.getConfigDescriptionURI());
        assertEquals(0, addonInfo.getDiscoveryMethods().size());
    }

    /**
     * Test fetching two addon-info's from the registry with merging.
     */
    @Test
    void testMergeAddonInfos2() {
        AddonInfoRegistry registry = new AddonInfoRegistry();
        assertNotNull(addonInfoProvider0);
        registry.addAddonInfoProvider(Objects.requireNonNull(addonInfoProvider0));
        assertNotNull(addonInfoProvider1);
        registry.addAddonInfoProvider(Objects.requireNonNull(addonInfoProvider1));

        AddonInfo addonInfo;
        addonInfo = registry.getAddonInfo("aardvark", Locale.US);
        assertNull(addonInfo);
        addonInfo = registry.getAddonInfo("aardvark", null);
        assertNull(addonInfo);
        addonInfo = registry.getAddonInfo("binding-hue", null);
        assertNull(addonInfo);
        addonInfo = registry.getAddonInfo("binding-hue", Locale.US);
        assertNotNull(addonInfo);

        assertEquals("hue", addonInfo.getId());
        assertEquals("binding", addonInfo.getType());
        assertEquals("binding-hue", addonInfo.getUID());
        assertTrue(addonInfo.getName().startsWith("name-"));
        assertTrue(addonInfo.getDescription().startsWith("description-"));
        assertNotEquals("source-bundle", addonInfo.getSourceBundle());
        assertEquals("local", addonInfo.getConnection());
        assertEquals(2, addonInfo.getCountries().size());
        assertNotEquals("http://www.openhab.org", addonInfo.getConfigDescriptionURI());
        assertEquals(1, addonInfo.getDiscoveryMethods().size());
    }

    /**
     * Test fetching three addon-info's from the registry with full merging.
     */
    @Test
    void testMergeAddonInfos3() {
        AddonInfoRegistry registry = new AddonInfoRegistry();
        assertNotNull(addonInfoProvider0);
        registry.addAddonInfoProvider(Objects.requireNonNull(addonInfoProvider0));
        assertNotNull(addonInfoProvider1);
        registry.addAddonInfoProvider(Objects.requireNonNull(addonInfoProvider1));
        assertNotNull(addonInfoProvider2);
        registry.addAddonInfoProvider(Objects.requireNonNull(addonInfoProvider2));

        AddonInfo addonInfo;
        addonInfo = registry.getAddonInfo("aardvark", Locale.US);
        assertNull(addonInfo);
        addonInfo = registry.getAddonInfo("aardvark", null);
        assertNull(addonInfo);
        addonInfo = registry.getAddonInfo("binding-hue", null);
        assertNull(addonInfo);
        addonInfo = registry.getAddonInfo("binding-hue", Locale.US);
        assertNotNull(addonInfo);

        assertEquals("hue", addonInfo.getId());
        assertEquals("binding", addonInfo.getType());
        assertEquals("binding-hue", addonInfo.getUID());
        assertTrue(addonInfo.getName().startsWith("name-"));
        assertTrue(addonInfo.getDescription().startsWith("description-"));
        assertEquals("source-bundle", addonInfo.getSourceBundle());
        assertEquals("local", addonInfo.getConnection());
        assertEquals(4, addonInfo.getCountries().size());
        assertEquals("http://www.openhab.org", addonInfo.getConfigDescriptionURI());
        assertEquals(2, addonInfo.getDiscoveryMethods().size());
    }
}
