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
package org.openhab.core.addon.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.AddonService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * The {@link AddonsInfoProviderInstaller} component to install the special AddonsInfoProvider addon.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, name = AddonsInfoProviderInstaller.SERVICE_NAME)
public class AddonsInfoProviderInstaller {

    public static final String SERVICE_NAME = "Installer for the AddonsInfoProvider special addon";

    private static final String KARAF_ADDONS_SERVICE_ID = "karaf";
    private static final String ADDONS_INFO_PROVIDER_UID = "misc" + Addon.ADDON_SEPARATOR + "addonsinfoprovider";

    private boolean addonInstalled;

    public AddonsInfoProviderInstaller() {
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addAddonService(AddonService addonService) {
        if (!addonInstalled && KARAF_ADDONS_SERVICE_ID.equals(addonService.getId())) {
            addonService.install(ADDONS_INFO_PROVIDER_UID);
            addonInstalled = true;
        }
    }

    public void removeAddonService(AddonService addonService) {
        if (addonInstalled && KARAF_ADDONS_SERVICE_ID.equals(addonService.getId())) {
            addonService.uninstall(ADDONS_INFO_PROVIDER_UID);
            addonInstalled = false;
        }
    }
}
