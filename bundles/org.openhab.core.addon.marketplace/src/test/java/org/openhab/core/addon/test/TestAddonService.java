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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.marketplace.AbstractRemoteAddonService;
import org.openhab.core.addon.marketplace.MarketplaceAddonHandler;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.storage.StorageService;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * The {@link TestAddonService} is a
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class TestAddonService extends AbstractRemoteAddonService {
    public static final String TEST_ADDON = "testAddon";
    public static final String INSTALL_EXCEPTION_ADDON = "installException";
    public static final String UNINSTALL_EXCEPTION_ADDON = "uninstallException";

    public static final String SERVICE_PID = "testAddonService";

    public static final Map<String, Addon> REMOTE_ADDONS = Stream
            .of(TEST_ADDON, INSTALL_EXCEPTION_ADDON, UNINSTALL_EXCEPTION_ADDON)
            .map(id -> Addon.create(SERVICE_PID + ":" + id).withType("binding")
                    .withContentType(VirtualAddonHandler.TEST_ADDON_CONTENT_TYPE).build())
            .collect(Collectors.toMap(Addon::getId, a -> a));

    private int remoteCalls = 0;

    public TestAddonService(EventPublisher eventPublisher, ConfigurationAdmin configurationAdmin,
            StorageService storageService) {
        super(eventPublisher, configurationAdmin, storageService, SERVICE_PID);
    }

    public void addAddonHandler(MarketplaceAddonHandler handler) {
        this.addonHandlers.add(handler);
    }

    public void removeAddonHandler(MarketplaceAddonHandler handler) {
        this.addonHandlers.remove(handler);
    }

    @Override
    protected List<Addon> getRemoteAddons() {
        remoteCalls++;
        return new ArrayList<>(REMOTE_ADDONS.values());
    }

    @Override
    public String getId() {
        return SERVICE_PID;
    }

    @Override
    public String getName() {
        return "Test Addon Service";
    }

    @Override
    public @Nullable Addon getAddon(String id, @Nullable Locale locale) {
        String remoteId = SERVICE_PID + ":" + id;
        return REMOTE_ADDONS.get(remoteId);
    }

    @Override
    public @Nullable String getAddonId(URI addonURI) {
        return null;
    }

    public int getRemoteCalls() {
        return remoteCalls;
    }
}
