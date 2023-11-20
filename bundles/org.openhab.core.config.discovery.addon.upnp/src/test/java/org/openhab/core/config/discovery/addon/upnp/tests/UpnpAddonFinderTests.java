package org.openhab.core.config.discovery.addon.upnp.tests;

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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
import org.openhab.core.addon.AddonDiscoveryMethod;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.addon.AddonMatchProperty;
import org.openhab.core.config.discovery.addon.AddonFinder;
import org.openhab.core.config.discovery.addon.AddonFinderConstants;
import org.openhab.core.config.discovery.addon.upnp.UpnpAddonFinder;

/**
 * JUnit tests for the {@link UpnpAddonFinder}.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 * @author Mark Herwege - Adapted to finders in separate packages
 */
@NonNullByDefault
@TestInstance(Lifecycle.PER_CLASS)
public class UpnpAddonFinderTests {

    private @NonNullByDefault({}) UpnpService upnpService;
    private @NonNullByDefault({}) AddonFinder addonFinder;
    private List<AddonInfo> addonInfos = new ArrayList<>();

    @BeforeAll
    public void setup() {
        setupMockUpnpService();
        setupAddonInfos();
        createAddonFinder();
    }

    private void createAddonFinder() {
        UpnpAddonFinder upnpAddonFinder = new UpnpAddonFinder(upnpService);
        assertNotNull(upnpAddonFinder);

        addonFinder = upnpAddonFinder;
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

    private void setupAddonInfos() {
        AddonDiscoveryMethod hue = new AddonDiscoveryMethod().setServiceType(AddonFinderConstants.SERVICE_TYPE_UPNP)
                .setMatchProperties(List.of(new AddonMatchProperty("modelName", "Philips hue bridge")));
        addonInfos.add(AddonInfo.builder("hue", "binding").withName("Hue").withDescription("Hue Bridge")
                .withDiscoveryMethods(List.of(hue)).build());
    }

    @Test
    public void testGetSuggestedAddons() {
        addonFinder.setAddonCandidates(addonInfos);
        Set<AddonInfo> addons = addonFinder.getSuggestedAddons();
        assertEquals(1, addons.size());
        assertFalse(addons.stream().anyMatch(a -> "aardvark".equals(a.getUID())));
        assertTrue(addons.stream().anyMatch(a -> "binding-hue".equals(a.getUID())));
    }
}
