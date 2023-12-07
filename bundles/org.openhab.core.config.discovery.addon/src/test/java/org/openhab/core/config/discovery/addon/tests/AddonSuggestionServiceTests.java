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
package org.openhab.core.config.discovery.addon.tests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.openhab.core.config.discovery.addon.AddonFinderConstants.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.openhab.core.addon.AddonDiscoveryMethod;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.addon.AddonInfoProvider;
import org.openhab.core.addon.AddonMatchProperty;
import org.openhab.core.config.discovery.addon.AddonFinder;
import org.openhab.core.config.discovery.addon.AddonFinderConstants;
import org.openhab.core.config.discovery.addon.AddonSuggestionService;
import org.openhab.core.i18n.LocaleProvider;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * JUnit tests for the {@link AddonSuggestionService}.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 * @author Mark Herwege - Adapted to finders in separate packages
 */
@NonNullByDefault
@TestInstance(Lifecycle.PER_CLASS)
public class AddonSuggestionServiceTests {

    private @NonNullByDefault({}) ConfigurationAdmin configurationAdmin;
    private @NonNullByDefault({}) LocaleProvider localeProvider;
    private @NonNullByDefault({}) AddonInfoProvider addonInfoProvider;
    private @NonNullByDefault({}) AddonFinder mdnsAddonFinder;
    private @NonNullByDefault({}) AddonFinder upnpAddonFinder;
    private @NonNullByDefault({}) AddonSuggestionService addonSuggestionService;

    private final Map<String, Object> config = Map.of(AddonFinderConstants.CFG_FINDER_MDNS, true,
            AddonFinderConstants.CFG_FINDER_UPNP, true);

    @AfterAll
    public void cleanUp() {
        assertNotNull(addonSuggestionService);
        try {
            addonSuggestionService.close();
        } catch (Exception e) {
            fail(e);
        }
    }

    @BeforeAll
    public void setup() {
        setupMockConfigurationAdmin();
        setupMockLocaleProvider();
        setupMockAddonInfoProvider();
        setupMockMdnsAddonFinder();
        setupMockUpnpAddonFinder();
        addonSuggestionService = createAddonSuggestionService();
    }

    private AddonSuggestionService createAddonSuggestionService() {
        AddonSuggestionService addonSuggestionService = new AddonSuggestionService(configurationAdmin, localeProvider,
                config);
        assertNotNull(addonSuggestionService);

        addonSuggestionService.addAddonFinder(mdnsAddonFinder);
        addonSuggestionService.addAddonFinder(upnpAddonFinder);

        return addonSuggestionService;
    }

    private void setupMockConfigurationAdmin() {
        // create the mock
        configurationAdmin = mock(ConfigurationAdmin.class);
        Configuration configuration = mock(Configuration.class);
        try {
            when(configurationAdmin.getConfiguration(any())).thenReturn(configuration);
        } catch (IOException e) {
        }
        when(configuration.getProperties()).thenReturn(null);

        // check that it works
        assertNotNull(configurationAdmin);
        try {
            assertNull(configurationAdmin.getConfiguration(AddonSuggestionService.CONFIG_PID).getProperties());
        } catch (IOException e) {
        }
    }

    private void setupMockLocaleProvider() {
        // create the mock
        localeProvider = mock(LocaleProvider.class);
        when(localeProvider.getLocale()).thenReturn(Locale.US);

        // check that it works
        assertNotNull(localeProvider);
        assertEquals(Locale.US, localeProvider.getLocale());
    }

    private void setupMockAddonInfoProvider() {
        AddonDiscoveryMethod hp = new AddonDiscoveryMethod().setServiceType(AddonFinderConstants.SERVICE_TYPE_MDNS)
                .setMatchProperties(
                        List.of(new AddonMatchProperty("rp", ".*"), new AddonMatchProperty("ty", "hp (.*)")))
                .setMdnsServiceType("_printer._tcp.local.");

        AddonDiscoveryMethod hue1 = new AddonDiscoveryMethod().setServiceType(AddonFinderConstants.SERVICE_TYPE_UPNP)
                .setMatchProperties(List.of(new AddonMatchProperty("modelName", "Philips hue bridge")));

        AddonDiscoveryMethod hue2 = new AddonDiscoveryMethod().setServiceType(AddonFinderConstants.SERVICE_TYPE_MDNS)
                .setMdnsServiceType("_hue._tcp.local.");

        // create the mock
        addonInfoProvider = mock(AddonInfoProvider.class);
        Set<AddonInfo> addonInfos = new HashSet<>();
        addonInfos.add(AddonInfo.builder("hue", "binding").withName("Hue").withDescription("Hue Bridge")
                .withDiscoveryMethods(List.of(hue1, hue2)).build());

        addonInfos.add(AddonInfo.builder("hpprinter", "binding").withName("HP").withDescription("HP Printer")
                .withDiscoveryMethods(List.of(hp)).build());
        when(addonInfoProvider.getAddonInfos(any(Locale.class))).thenReturn(addonInfos);

        // check that it works
        assertNotNull(addonInfoProvider);
        Set<AddonInfo> addonInfos2 = addonInfoProvider.getAddonInfos(Locale.US);
        assertEquals(2, addonInfos2.size());
        assertTrue(addonInfos2.stream().anyMatch(a -> "binding-hue".equals(a.getUID())));
        assertTrue(addonInfos2.stream().anyMatch(a -> "binding-hpprinter".equals(a.getUID())));
    }

    private void setupMockMdnsAddonFinder() {
        // create the mock
        mdnsAddonFinder = mock(AddonFinder.class);

        Set<AddonInfo> addonInfos = addonInfoProvider.getAddonInfos(Locale.US).stream().filter(
                c -> c.getDiscoveryMethods().stream().anyMatch(m -> SERVICE_TYPE_MDNS.equals(m.getServiceType())))
                .collect(Collectors.toSet());
        when(mdnsAddonFinder.getSuggestedAddons()).thenReturn(addonInfos);

        // check that it works
        assertNotNull(mdnsAddonFinder);
        Set<AddonInfo> addonInfos2 = mdnsAddonFinder.getSuggestedAddons();
        assertEquals(2, addonInfos2.size());
        assertTrue(addonInfos2.stream().anyMatch(a -> "binding-hue".equals(a.getUID())));
        assertTrue(addonInfos2.stream().anyMatch(a -> "binding-hpprinter".equals(a.getUID())));
    }

    private void setupMockUpnpAddonFinder() {
        // create the mock
        upnpAddonFinder = mock(AddonFinder.class);

        Set<AddonInfo> addonInfos = addonInfoProvider.getAddonInfos(Locale.US).stream().filter(
                c -> c.getDiscoveryMethods().stream().anyMatch(m -> SERVICE_TYPE_UPNP.equals(m.getServiceType())))
                .collect(Collectors.toSet());
        when(upnpAddonFinder.getSuggestedAddons()).thenReturn(addonInfos);

        // check that it works
        assertNotNull(upnpAddonFinder);
        Set<AddonInfo> addonInfos2 = upnpAddonFinder.getSuggestedAddons();
        assertEquals(1, addonInfos2.size());
        assertTrue(addonInfos2.stream().anyMatch(a -> "binding-hue".equals(a.getUID())));
    }

    @Test
    public void testGetSuggestedAddons() {
        addonSuggestionService.addAddonInfoProvider(addonInfoProvider);
        Set<AddonInfo> addons = addonSuggestionService.getSuggestedAddons(localeProvider.getLocale());
        assertEquals(2, addons.size());
        assertFalse(addons.stream().anyMatch(a -> "aardvark".equals(a.getUID())));
        assertTrue(addons.stream().anyMatch(a -> "binding-hue".equals(a.getUID())));
        assertTrue(addons.stream().anyMatch(a -> "binding-hpprinter".equals(a.getUID())));
    }
}
