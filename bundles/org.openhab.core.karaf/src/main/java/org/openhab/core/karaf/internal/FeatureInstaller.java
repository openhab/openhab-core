/**
 * Copyright (c) 2015-2015 Kai Kreuzer and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.core.karaf.internal;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang.StringUtils;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service reads addons.cfg and installs listed addons (= Karaf features) and the selected package.
 *
 * @author Kai Kreuzer
 */
public class FeatureInstaller {

    public static final String PREFIX = "openhab-";

    public static final String PREFIX_PACKAGE = "package-";

    public static final String[] addonTypes = new String[] { "binding", "ui", "persistence", "action", "tts",
            "transformation", "misc" };

    private final Logger logger = LoggerFactory.getLogger(FeatureInstaller.class);

    private FeaturesService featuresService;

    protected void setFeaturesService(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }

    protected void unsetFeaturesService(FeaturesService featuresService) {
        this.featuresService = null;
    }

    protected void activate(final Map<String, Object> config) {
        modified(config);
    }

    protected void modified(final Map<String, Object> config) {
        ExecutorService scheduler = Executors.newSingleThreadExecutor();
        scheduler.execute(new Runnable() {
            @Override
            public void run() {
                installPackage(config);

                // install addons
                for (String type : addonTypes) {
                    Object install = config.get(type);
                    if (install instanceof String) {
                        installFeatures(type, (String) install);
                    }
                }
            }

        });
    }

    private void installFeatures(String type, String install) {
        for (String addon : install.split(",")) {
            if (StringUtils.isNotBlank(addon)) {
                String name = PREFIX + type + "-" + addon.trim();
                installFeature(name);
            }
        }
    }

    private void installFeature(String name) {
        try {
            featuresService.installFeature(name);
            logger.info("Installed '{}'", name);
        } catch (Exception e) {
            logger.error("Failed installing '{}': {}", name, e.getMessage());
        }
    }

    private void installPackage(final Map<String, Object> config) {
        Object packageName = config.get("package");
        String name = PREFIX + PREFIX_PACKAGE + ((String) packageName).trim();
        installFeature(name);

        // uninstall all other packages
        try {
            for (Feature feature : featuresService.listFeatures()) {
                if (feature.getName().startsWith(PREFIX + PREFIX_PACKAGE) && !feature.getName().equals(name)
                        && featuresService.isInstalled(feature)) {
                    try {
                        featuresService.uninstallFeature(feature.getName());
                    } catch (Exception e) {
                        logger.error("Failed uninstalling '{}': {}", feature.getName(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed retrieving features: {}", e.getMessage());
        }
    }

}
