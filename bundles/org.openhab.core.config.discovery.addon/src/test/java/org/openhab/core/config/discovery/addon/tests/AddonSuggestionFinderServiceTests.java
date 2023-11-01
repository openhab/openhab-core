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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.jmdns.ServiceInfo;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.jupnp.UpnpService;
import org.jupnp.model.ValidationException;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.ManufacturerDetails;
import org.jupnp.model.meta.ModelDetails;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteDeviceIdentity;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.types.DeviceType;
import org.jupnp.model.types.UDN;
import org.jupnp.registry.Registry;
import org.mockito.Mockito;
import org.openhab.core.addon.AddonDiscoveryMethod;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.addon.AddonInfoProvider;
import org.openhab.core.addon.AddonMatchProperty;
import org.openhab.core.config.discovery.addon.AddonSuggestionFinderService;
import org.openhab.core.config.discovery.addon.finders.MDNSAddonSuggestionFinder;
import org.openhab.core.config.discovery.addon.finders.UpnpAddonSuggestionFinder;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.io.transport.mdns.MDNSClient;

/**
 * JUnit tests for the {@link AddonSuggestionFinderService}.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
@TestInstance(Lifecycle.PER_CLASS)
public class AddonSuggestionFinderServiceTests {

    private @NonNullByDefault({}) LocaleProvider localeProvider;
    private @NonNullByDefault({}) MDNSClient mdnsClient;
    private @NonNullByDefault({}) UpnpService upnpService;
    private @NonNullByDefault({}) AddonInfoProvider addonInfoProvider;
    private @NonNullByDefault({}) AddonSuggestionFinderService addonSuggestionFinderService;

    @AfterAll
    public void cleanUp() {
        assertNotNull(addonSuggestionFinderService);
        try {
            addonSuggestionFinderService.close();
        } catch (Exception e) {
            fail(e);
        }
    }

    @BeforeAll
    public void setup() {
        setupMockLocaleProvider();
        setupMockAddonInfoProvider();
        setupMockMdnsClient();
        setupMockUpnpService();
        createAddonSuggestionFinderService();
    }

    private void createAddonSuggestionFinderService() {
        addonSuggestionFinderService = new AddonSuggestionFinderService(localeProvider);
        assertNotNull(addonSuggestionFinderService);

        UpnpAddonSuggestionFinder upnp = new UpnpAddonSuggestionFinder(upnpService);
        Registry registry = upnpService.getRegistry();
        registry.getRemoteDevices().forEach(device -> upnp.remoteDeviceAdded(registry, device));
        addonSuggestionFinderService.addAddonSuggestionFinder(upnp);

        MDNSAddonSuggestionFinder mdns = new MDNSAddonSuggestionFinder(mdnsClient);
        for (ServiceInfo service : mdnsClient.list("_hue._tcp.local.")) {
            mdns.addService(service);
        }
        for (ServiceInfo service : mdnsClient.list("_printer._tcp.local.")) {
            mdns.addService(service);
        }
        addonSuggestionFinderService.addAddonSuggestionFinder(mdns);
    }

    private void setupMockAddonInfoProvider() {
        AddonDiscoveryMethod hp = new AddonDiscoveryMethod().setServiceType(MDNSAddonSuggestionFinder.SERVICE_TYPE)
                .setMatchProperties(
                        List.of(new AddonMatchProperty("rp", ".*"), new AddonMatchProperty("ty", "hp (.*)")))
                .setMdnsServiceType("_printer._tcp.local.");

        AddonDiscoveryMethod hue1 = new AddonDiscoveryMethod().setServiceType(UpnpAddonSuggestionFinder.SERVICE_TYPE)
                .setMatchProperties(List.of(new AddonMatchProperty("modelName", "Philips hue bridge")));

        AddonDiscoveryMethod hue2 = new AddonDiscoveryMethod().setServiceType(MDNSAddonSuggestionFinder.SERVICE_TYPE)
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

    private void setupMockLocaleProvider() {
        // create the mock
        localeProvider = mock(LocaleProvider.class);
        when(localeProvider.getLocale()).thenReturn(Locale.US);

        // check that it works
        assertNotNull(localeProvider);
        assertEquals(Locale.US, localeProvider.getLocale());
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

    private void setupMockUpnpService() {
        // create the mock
        upnpService = mock(UpnpService.class, Mockito.RETURNS_DEEP_STUBS);
        URL url = null;
        try {
            url = new URL("http://www.openhab.org/");
        } catch (MalformedURLException e) {
            fail("MalformedURLException");
        }
        UDN udn = new UDN("udn");
        InetAddress address = null;
        try {
            address = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            fail("UnknownHostException");
        }
        RemoteDeviceIdentity identity = new RemoteDeviceIdentity(udn, 0, url, new byte[] {}, address);
        DeviceType type = new DeviceType("nameSpace", "type");
        ManufacturerDetails manDetails = new ManufacturerDetails("manufacturer", "manufacturerURI");
        ModelDetails modDetails = new ModelDetails("Philips hue bridge", "modelDescription", "modelNumber", "modelURI");
        DeviceDetails devDetails = new DeviceDetails("friendlyName", manDetails, modDetails, "serialNumber",
                "000123456789");
        List<@Nullable RemoteDevice> remoteDevices = new ArrayList<>();
        try {
            remoteDevices.add(new RemoteDevice(identity, type, devDetails, (RemoteService) null));
        } catch (ValidationException e1) {
            fail("ValidationException");
        }
        when(upnpService.getRegistry().getRemoteDevices()).thenReturn(remoteDevices);

        // check that it works
        assertNotNull(upnpService);
        List<RemoteDevice> result = new ArrayList<>(upnpService.getRegistry().getRemoteDevices());
        assertEquals(1, result.size());
        RemoteDevice device = result.get(0);
        assertEquals("manufacturer", device.getDetails().getManufacturerDetails().getManufacturer());
        assertEquals("serialNumber", device.getDetails().getSerialNumber());
    }

    @Test
    public void testGetSuggestedAddons() {
        addonSuggestionFinderService.addAddonInfoProvider(addonInfoProvider);
        Set<AddonInfo> addons = addonSuggestionFinderService.getSuggestedAddons(localeProvider.getLocale());
        assertEquals(2, addons.size());
        assertFalse(addons.stream().anyMatch(a -> "aardvark".equals(a.getUID())));
        assertTrue(addons.stream().anyMatch(a -> "binding-hue".equals(a.getUID())));
        assertTrue(addons.stream().anyMatch(a -> "binding-hpprinter".equals(a.getUID())));
    }
}
