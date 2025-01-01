/**
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.karaf.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.discovery.addon.AddonFinderService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * This service is an implementation of an openHAB {@link AddonFinderService} using the Karaf features
 * service. This service allows dynamic installation/removal of add-on suggestion finders.
 *
 * @author Mark Herwege - Initial contribution
 */
@Component(name = "org.openhab.core.karafaddonfinders", immediate = true)
@NonNullByDefault
public class KarafAddonFinderService implements AddonFinderService {
    private final FeatureInstaller featureInstaller;
    private boolean deactivated;

    @Activate
    public KarafAddonFinderService(final @Reference FeatureInstaller featureInstaller) {
        this.featureInstaller = featureInstaller;
    }

    @Deactivate
    protected void deactivate() {
        deactivated = true;
    }

    @Override
    public void install(String id) {
        if (!deactivated) {
            featureInstaller.addAddon(FeatureInstaller.FINDER_ADDON_TYPE, id);
        }
    }

    @Override
    public void uninstall(String id) {
        if (!deactivated) {
            featureInstaller.removeAddon(FeatureInstaller.FINDER_ADDON_TYPE, id);
        }
    }
}
