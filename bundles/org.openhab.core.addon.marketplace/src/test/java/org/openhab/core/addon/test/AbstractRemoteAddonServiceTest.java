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

import static org.openhab.core.addon.test.TestAddonService.INSTALL_EXCEPTION_ADDON;
import static org.openhab.core.addon.test.TestAddonService.SERVICE_PID;
import static org.openhab.core.addon.test.TestAddonService.TEST_ADDON;
import static org.openhab.core.addon.test.TestAddonService.UNINSTALL_EXCEPTION_ADDON;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;

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
 * The {@link AbstractRemoteAddonServiceTest} contains tests for the
 * {@link org.openhab.core.addon.marketplace.AbstractRemoteAddonService}
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
    private @NonNullByDefault({}) TestAddonHandler addonHandler;
    private final Dictionary<String, Object> properties = new Hashtable<>();

    @BeforeEach
    public void initialize() throws IOException {
        storage = new VolatileStorage<>();
        Mockito.doReturn(storage).when(storageService).getStorage(SERVICE_PID);
        Mockito.doReturn(configuration).when(configurationAdmin).getConfiguration("org.openhab.addons", null);
        Mockito.doReturn(properties).when(configuration).getProperties();

        addonHandler = new TestAddonHandler();

        addonService = new TestAddonService(eventPublisher, configurationAdmin, storageService);
        addonService.addAddonHandler(addonHandler);
    }

    // general tests

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
    public void testAddonIsReportedAsInstalledIfStorageEntryMissing() {
        addonService.setInstalled(TEST_ADDON);
        List<Addon> addons = addonService.getAddons(null);
        Addon addon = addons.stream().filter(a -> getFullAddonId(TEST_ADDON).equals(a.getId())).findAny().orElse(null);

        Objects.requireNonNull(addon);
        Assertions.assertTrue(addon.isInstalled());
    }

    @Test
    public void testInstalledAddonIsStillPresentAfterRemoteIsDisabledOrMissing() {
        addonService.setInstalled(TEST_ADDON);
        addonService.addToStorage(TEST_ADDON);

        // check all addons are present
        List<Addon> addons = addonService.getAddons(null);
        Assertions.assertEquals(TestAddonService.REMOTE_ADDONS.size(), addons.size());

        // disable remote repo
        properties.put("remote", false);

        // check only the installed addon is present
        addons = addonService.getAddons(null);
        Assertions.assertEquals(1, addons.size());
        Assertions.assertEquals(getFullAddonId(TEST_ADDON), addons.get(0).getId());
    }

    // installation tests

    @Test
    public void testAddonInstall() {
        addonService.getAddons(null);

        addonService.install(TEST_ADDON);

        checkResult(TEST_ADDON, getFullAddonId(TEST_ADDON) + "/installed", true, true);
    }

    @Test
    public void testAddonInstallFailsWithHandlerException() {
        addonService.getAddons(null);

        addonService.install(INSTALL_EXCEPTION_ADDON);

        checkResult(INSTALL_EXCEPTION_ADDON, getFullAddonId(INSTALL_EXCEPTION_ADDON) + "/failed", false, true);
    }

    @Test
    public void testAddonInstallFailsOnInstalledAddon() {
        addonService.setInstalled(TEST_ADDON);
        addonService.addToStorage(TEST_ADDON);
        addonService.getAddons(null);

        addonService.install(TEST_ADDON);

        checkResult(TEST_ADDON, getFullAddonId(TEST_ADDON) + "/failed", true, true);
    }

    @Test
    public void testAddonInstallFailsOnUnknownAddon() {
        addonService.getAddons(null);

        addonService.install("unknown");

        checkResult("unknown", "unknown/failed", false, false);
    }

    // uninstallation tests

    @Test
    public void testAddonUninstall() {
        addonService.setInstalled(TEST_ADDON);
        addonService.addToStorage(TEST_ADDON);
        addonService.getAddons(null);

        addonService.uninstall(TEST_ADDON);

        checkResult(TEST_ADDON, getFullAddonId(TEST_ADDON) + "/uninstalled", false, true);
    }

    @Test
    public void testAddonUninstallFailsWithHandlerException() {
        addonService.setInstalled(UNINSTALL_EXCEPTION_ADDON);
        addonService.addToStorage(UNINSTALL_EXCEPTION_ADDON);
        addonService.getAddons(null);

        addonService.uninstall(UNINSTALL_EXCEPTION_ADDON);

        checkResult(UNINSTALL_EXCEPTION_ADDON, getFullAddonId(UNINSTALL_EXCEPTION_ADDON) + "/failed", true, true);
    }

    @Test
    public void testAddonUninstallFailsOnUninstalledAddon() {
        addonService.getAddons(null);

        addonService.uninstall(TEST_ADDON);

        checkResult(TEST_ADDON, getFullAddonId(TEST_ADDON) + "/failed", false, true);
    }

    @Test
    public void testAddonUninstallFailsOnUnknownAddon() {
        addonService.getAddons(null);

        addonService.uninstall("unknown");

        checkResult("unknown", "unknown/failed", false, false);
    }

    @Test
    public void testAddonUninstallRemovesStorageEntryOnUninstalledAddon() {
        addonService.addToStorage(TEST_ADDON);
        addonService.getAddons(null);

        addonService.uninstall(TEST_ADDON);

        checkResult(TEST_ADDON, getFullAddonId(TEST_ADDON) + "/failed", false, true);
    }

    /**
     * checks that a proper event is posted, the presence in storage and installation status in handler
     *
     * @param id add-on id (without service-prefix)
     * @param expectedEventTopic the expected event (e.g. installed)
     * @param installStatus the expected installation status of the add-on
     * @param present if the addon is expected to be present after the test
     */
    private void checkResult(String id, String expectedEventTopic, boolean installStatus, boolean present) {
        // assert expected event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        Mockito.verify(eventPublisher).post(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        String topic = "openhab/addons/" + expectedEventTopic;

        Assertions.assertEquals(topic, event.getTopic());

        // assert addon handler was called (by checking it's installed status)
        Assertions.assertEquals(installStatus, addonHandler.isInstalled(getFullAddonId(id)));

        // assert is present in storage if installed or missing if uninstalled
        Assertions.assertEquals(installStatus, storage.containsKey(id));

        // assert correct installation status is reported for addon
        Addon addon = addonService.getAddon(id, null);
        if (present) {
            Assertions.assertNotNull(addon);
            Objects.requireNonNull(addon);
            Assertions.assertEquals(installStatus, addon.isInstalled());
        } else {
            Assertions.assertNull(addon);
        }
    }

    private String getFullAddonId(String id) {
        return SERVICE_PID + ":" + id;
    }
}
