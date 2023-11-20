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
package org.openhab.core.karaf.internal;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.karaf.features.FeaturesService;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.NamedThreadFactory;
import org.openhab.core.config.discovery.addon.AddonFinderService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service is an implementation of an openHAB {@link AddonSuggestionFinderService} using the Karaf features
 * service. This service allows dynamic installation/removal of add-on suggestion finders.
 *
 * @author Mark Herwege - Initial contribution
 */
@Component(name = "org.openhab.core.karafaddonfinders", immediate = true)
@NonNullByDefault
public class KarafAddonFinderService implements AddonFinderService {
    private final Logger logger = LoggerFactory.getLogger(KarafAddonFinderService.class);

    private final ScheduledExecutorService scheduler;
    private final FeaturesService featuresService;

    @Activate
    public KarafAddonFinderService(final @Reference FeaturesService featuresService) {
        this.featuresService = featuresService;
        scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("karaf-addonfinders"));
    }

    @Override
    public void install(String id) {
        scheduler.execute(() -> {
            try {
                if (!featuresService.isInstalled(featuresService.getFeature(id))) {
                    featuresService.installFeature(id);
                }
            } catch (Exception e) {
                logger.error("Failed to install add-on suggestion finder {}", id, e);
            }
        });
    }

    @Override
    public void uninstall(String id) {
        scheduler.execute(() -> {
            try {
                if (featuresService.isInstalled(featuresService.getFeature(id))) {
                    featuresService.uninstallFeature(id);
                }
            } catch (Exception e) {
                logger.error("Failed to uninstall add-on suggestion finder {}", id, e);
            }
        });
    }
}
