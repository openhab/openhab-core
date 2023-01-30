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
package org.openhab.core.addon.marketplace.test;

import static org.openhab.core.addon.marketplace.test.TestAddonService.INSTALL_EXCEPTION_ADDON;
import static org.openhab.core.addon.marketplace.test.TestAddonService.UNINSTALL_EXCEPTION_ADDON;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.marketplace.MarketplaceAddonHandler;
import org.openhab.core.addon.marketplace.MarketplaceHandlerException;

/**
 * The {@link TestAddonHandler} is a
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class TestAddonHandler implements MarketplaceAddonHandler {
    private static final Set<String> SUPPORTED_ADDON_TYPES = Set.of("binding", "automation");
    public static final String TEST_ADDON_CONTENT_TYPE = "testAddonContentType";

    private final Set<String> installedAddons = new HashSet<>();

    private boolean isReady = true;

    public void setReady(boolean ready) {
        isReady = ready;
    }

    @Override
    public boolean isReady() {
        return isReady;
    }

    @Override
    public boolean supports(String type, String contentType) {
        return SUPPORTED_ADDON_TYPES.contains(type) && TEST_ADDON_CONTENT_TYPE.equals(contentType);
    }

    @Override
    public boolean isInstalled(String id) {
        if (!isReady) {
            // this is to catch illegal calls to the service in tests
            throw new IllegalStateException();
        }

        return installedAddons.contains(id);
    }

    @Override
    public void install(Addon addon) throws MarketplaceHandlerException {
        if (addon.getUid().endsWith(":" + INSTALL_EXCEPTION_ADDON)) {
            throw new MarketplaceHandlerException("Installation failed", null);
        }
        installedAddons.add(addon.getUid());
    }

    @Override
    public void uninstall(Addon addon) throws MarketplaceHandlerException {
        if (addon.getUid().endsWith(":" + UNINSTALL_EXCEPTION_ADDON)) {
            throw new MarketplaceHandlerException("Uninstallation failed", null);
        }
        installedAddons.remove(addon.getUid());
    }
}
