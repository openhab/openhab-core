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
package org.openhab.core.config.discovery.addon.mdns.tests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jmdns.ServiceInfo;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockito.Mockito;
import org.openhab.core.addon.AddonDiscoveryMethod;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.addon.AddonMatchProperty;
import org.openhab.core.config.discovery.addon.AddonFinder;
import org.openhab.core.config.discovery.addon.AddonFinderConstants;
import org.openhab.core.config.discovery.addon.AddonSuggestionService;
import org.openhab.core.config.discovery.addon.mdns.MdnsAddonFinder;
import org.openhab.core.io.transport.mdns.MDNSClient;

/**
 * JUnit tests for the {@link AddonSuggestionService}.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 * @author Mark Herwege - Adapted to finders in separate packages
 */
@NonNullByDefault
@TestInstance(Lifecycle.PER_CLASS)
public class MdnsAddonFinderTests {

    private @NonNullByDefault({}) MDNSClient mdnsClient;
    private @NonNullByDefault({}) AddonFinder addonFinder;
    private List<AddonInfo> addonInfos = new ArrayList<>();

    @BeforeAll
    public void setup() {
        setupMockMdnsClient();
        setupAddonInfos();
        createAddonFinder();
    }

    private void createAddonFinder() {
        MdnsAddonFinder mdnsAddonFinder = new MdnsAddonFinder(mdnsClient);
        assertNotNull(mdnsAddonFinder);

        for (ServiceInfo service : mdnsClient.list("_hue._tcp.local.")) {
            mdnsAddonFinder.addService(service, true);
        }
        for (ServiceInfo service : mdnsClient.list("_printer._tcp.local.")) {
            mdnsAddonFinder.addService(service, true);
        }

        addonFinder = mdnsAddonFinder;
    }

    private void setupMockMdnsClient() {
        // create the mock
        mdnsClient = mock(MDNSClient.class, Mockito.RETURNS_DEEP_STUBS);
        when(mdnsClient.list(anyString())).thenReturn(new ServiceInfo[] {});
        ServiceInfo hueService = ServiceInfo.create("hue", "hue", 0, 0, 0, false, "hue service");
        when(mdnsClient.list(eq("_hue._tcp.local."))).thenReturn(new ServiceInfo[] { hueService });
        ServiceInfo hpService = ServiceInfo.create("printer", "hpprinter", 0, 0, 0, false, "hp printer service");
        hpService.setText(Map.of("ty", "hp printer", "rp", "anything"));
        when(mdnsClient.list(eq("_printer._tcp.local."))).thenReturn(new ServiceInfo[] { hpService });

        // check that it works
        assertNotNull(mdnsClient);
        ServiceInfo[] result;
        result = mdnsClient.list("_printer._tcp.local.");
        assertEquals(1, result.length);
        assertEquals("hpprinter", result[0].getName());
        assertEquals(2, Collections.list(result[0].getPropertyNames()).size());
        assertEquals("hp printer", result[0].getPropertyString("ty"));
        result = mdnsClient.list("_hue._tcp.local.");
        assertEquals(1, result.length);
        assertEquals("hue", result[0].getName());
        result = mdnsClient.list("aardvark");
        assertEquals(0, result.length);
    }

    private void setupAddonInfos() {
        AddonDiscoveryMethod hp = new AddonDiscoveryMethod().setServiceType(AddonFinderConstants.SERVICE_TYPE_MDNS)
                .setMatchProperties(
                        List.of(new AddonMatchProperty("rp", ".*"), new AddonMatchProperty("ty", "hp (.*)")))
                .setMdnsServiceType("_printer._tcp.local.");
        addonInfos.add(AddonInfo.builder("hpprinter", "binding").withName("HP").withDescription("HP Printer")
                .withDiscoveryMethods(List.of(hp)).build());

        AddonDiscoveryMethod hue = new AddonDiscoveryMethod().setServiceType(AddonFinderConstants.SERVICE_TYPE_MDNS)
                .setMdnsServiceType("_hue._tcp.local.");
        addonInfos.add(AddonInfo.builder("hue", "binding").withName("Hue").withDescription("Hue Bridge")
                .withDiscoveryMethods(List.of(hue)).build());
    }

    @Test
    public void testGetSuggestedAddons() {
        addonFinder.setAddonCandidates(addonInfos);
        Set<AddonInfo> addons = addonFinder.getSuggestedAddons();
        assertEquals(2, addons.size());
        assertFalse(addons.stream().anyMatch(a -> "aardvark".equals(a.getUID())));
        assertTrue(addons.stream().anyMatch(a -> "binding-hue".equals(a.getUID())));
        assertTrue(addons.stream().anyMatch(a -> "binding-hpprinter".equals(a.getUID())));
    }
}
