/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.addon.Addon;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.openhab.core.test.storage.VolatileStorage;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * The {@link AbstractRemoteAddonServiceTest} is a
 *
 * @author Jan N. Klug - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
@NonNullByDefault
public class AbstractRemoteAddonServiceTest {

    private @Mock @NonNullByDefault({}) StorageService storageService;
    private @Mock @NonNullByDefault({}) ConfigurationAdmin configurationAdmin;
    private @Mock @NonNullByDefault({}) EventPublisher eventPublisher;
    private @Mock @NonNullByDefault({}) Configuration configuration;

    private @NonNullByDefault({}) Storage<String> storage;
    private @NonNullByDefault({}) TestAddonService addonService;

    private final Dictionary<String, Object> properties = new Hashtable<>();

    @BeforeEach
    public void initialize() throws IOException {
        storage = new VolatileStorage<>();
        Mockito.doReturn(storage).when(storageService).getStorage(TestAddonService.SERVICE_PID);
        Mockito.doReturn(configuration).when(configurationAdmin).getConfiguration("org.openhab.addons", null);
        Mockito.doReturn(properties).when(configuration).getProperties();

        addonService = new TestAddonService(eventPublisher, configurationAdmin, storageService);
        addonService.addAddonHandler(new VirtualAddonHandler());
    }

    @Test
    public void testRemoteDisabledBlocksRemoteCalls() {
        properties.put("remote", false);
        List<Addon> addons = addonService.getAddons(null);
        Assertions.assertEquals(0, addons.size());
        Assertions.assertEquals(0, addonService.getRemoteCalls());
    }

    @Test
    public void testAddonResultsAreCached() {
        List<Addon> addons = addonService.getAddons(null);
        Assertions.assertEquals(TestAddonService.REMOTE_ADDONS.size(), addons.size());
        addons = addonService.getAddons(null);
        Assertions.assertEquals(TestAddonService.REMOTE_ADDONS.size(), addons.size());
        Assertions.assertEquals(1, addonService.getRemoteCalls());
    }

    @Test
    public void testAddonInstallation() {
        addonService.install(TestAddonService.TEST_ADDON);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        Mockito.verify(eventPublisher).post(eventCaptor.capture());

        Event postInstallationEvent = eventCaptor.getValue();
        Assertions.assertEquals("openhab/addons/" + getFullAddonId(TestAddonService.TEST_ADDON) + "/installed",
                postInstallationEvent.getTopic());
    }

    private String getFullAddonId(String id) {
        return TestAddonService.SERVICE_PID + ":" + id;
    }
}
