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
package org.openhab.core.config.discovery.addon.tests.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.jmdns.ServiceInfo;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
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
import org.mockito.Mockito;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.AddonService;
import org.openhab.core.config.discovery.addon.finder.AddonSuggestionFinderService;
import org.openhab.core.io.transport.mdns.MDNSClient;
import org.openhab.core.test.java.JavaOSGiTest;

import com.thoughtworks.xstream.XStreamException;

/**
 * Integration tests for the {@link SuggestedAddonFinderService}.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
@TestInstance(Lifecycle.PER_CLASS)
public class SuggestedAddonFinderServiceOSGiTest extends JavaOSGiTest {

    private @NonNullByDefault({}) MDNSClient mdnsClient;
    private @NonNullByDefault({}) UpnpService upnpService;
    private @NonNullByDefault({}) AddonService addonService;
    private @NonNullByDefault({}) AddonSuggestionFinderService addonSuggestionFinderService;

    @BeforeAll
    public void setup() {
        setupAddonServiceMock();
        setupMdnsClientMock();
        setupUpnpServiceMock();
        setupService();
    }

    private void setupAddonServiceMock() {
        // create the mock
        addonService = mock(AddonService.class);
        List<Addon> addons = new ArrayList<>();
        addons.add(Addon.create("binding.hue").withType("binding").withId("hue").build());
        addons.add(Addon.create("binding.hpprinter").withType("binding").withId("hpprinter").build());
        when(addonService.getAddons(Locale.US)).thenReturn(addons);

        // check that it works
        assertNotNull(addonService);
        assertEquals(2, addonService.getAddons(Locale.US).size());
        assertEquals("binding.hue", addonService.getAddons(Locale.US).get(0).getUid());
        assertEquals("binding.hpprinter", addonService.getAddons(Locale.US).get(1).getUid());
    }

    private void setupMdnsClientMock() {
        // create the mock
        mdnsClient = mock(MDNSClient.class);
        ServiceInfo hueService = ServiceInfo.create("test", "hue", 0, 0, 0, false, "hue service");
        when(mdnsClient.list("_hue._tcp.local.")).thenReturn(new ServiceInfo[] { hueService });
        ServiceInfo hpService = ServiceInfo.create("test", "hpprinter", 0, 0, 0, false, "hp printer service");
        hpService.setText(Map.of("ty", "hp printer", "rp", "anything"));
        when(mdnsClient.list("_printer._tcp.local.")).thenReturn(new ServiceInfo[] { hpService });

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
    }

    private void setupService() {
        // create the service
        try {
            addonSuggestionFinderService = new AddonSuggestionFinderService(mdnsClient, upnpService);
        } catch (IOException e) {
            fail("Error loading XML");
        } catch (XStreamException e) {
            fail("Error parsing XML");
        }
        addonSuggestionFinderService.addAddonService(addonService);

        // check that it exists
        assertNotNull(addonSuggestionFinderService);
    }

    private void setupUpnpServiceMock() {
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
        ModelDetails modDetails = new ModelDetails("modelName", "modelDescription", "modelNumber", "modelURI");
        DeviceDetails devDetails = new DeviceDetails("friendlyName", manDetails, modDetails, "serialNumber",
                "000123456789");
        List<@Nullable RemoteDevice> remoteDevice = new ArrayList<>();
        try {
            remoteDevice.add(new RemoteDevice(identity, type, devDetails, (RemoteService) null));
        } catch (ValidationException e1) {
            fail("ValidationException");
        }
        when(upnpService.getRegistry().getRemoteDevices()).thenReturn(remoteDevice);

        // check that it works
        assertNotNull(upnpService);
        List<RemoteDevice> result = new ArrayList<>(upnpService.getRegistry().getRemoteDevices());
        assertEquals(1, result.size());
        RemoteDevice device = result.get(0);
        assertEquals("manufacturer", device.getDetails().getManufacturerDetails().getManufacturer());
        assertEquals("serialNumber", device.getDetails().getSerialNumber());
    }

    @Test
    public void testGetAddons() {
        // give the scan tasks some time to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
        assertTrue(addonSuggestionFinderService.scanDone());
        List<Addon> addons = addonSuggestionFinderService.getAddons(Locale.US);
        assertEquals(2, addons.size());
    }
}
