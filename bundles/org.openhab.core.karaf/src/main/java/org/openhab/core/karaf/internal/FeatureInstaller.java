/**
 * Copyright (c) 2015-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.core.karaf.internal;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang.StringUtils;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * This service reads addons.cfg and installs listed addons (= Karaf features) and the selected package.
 * It furthermore allows configuration of the base package through the Paper UI as well as administrating Karaf to
 * access remote repos and certain feature repos like for legacy or experimental features.
 *
 * @author Kai Kreuzer - Initial contribution and API
 */
public class FeatureInstaller implements ConfigurationListener {

    private static final String CFG_REMOTE = "remote";
    private static final String CFG_EXPERIMENTAL = "experimental";
    private static final String CFG_LEGACY = "legacy";

    private static final String ADDONS_SERVICE_PID = "org.openhab.addons";
    private static final String PAX_URL_PID = "org.ops4j.pax.url.mvn";
    private static final String PROPERTY_MVN_REPOS = "org.ops4j.pax.url.mvn.repositories";

    private static final String OH_SNAPSHOT_REPO = "http://oss.jfrog.org/libs-snapshot@id=oh-snapshot-repo@snapshots@noreleases";
    private static final String OH_RELEASES_REPO = "https://jcenter.bintray.com@id=oh-releases-repo";
    private static final String ESH_SNAPSHOT_REPO = "https://repo.eclipse.org/content/repositories/snapshots@id=esh-snapshot-repo@snapshots@noreleases";
    private static final String ESH_RELEASES_REPO = "https://repo.eclipse.org/content/repositories/releases@id=esh-release-repo";
    private static final Set<String> ONLINE_REPOS = Sets.newHashSet(OH_RELEASES_REPO, OH_SNAPSHOT_REPO,
            ESH_RELEASES_REPO, ESH_SNAPSHOT_REPO);

    private static final URI LEGACY_FEATURES_URI = URI
            .create("mvn:org.openhab.addons/openhab-addons-legacy/LATEST/xml/features");
    private static final URI EXPERIMENTAL_FEATURES_URI = URI
            .create("mvn:org.openhab.addons/openhab-addons-experimental/LATEST/xml/features");

    public static final String STANDARD_PACKAGE = "standard";
    public static final String PREFIX = "openhab-";
    public static final String PREFIX_PACKAGE = "package-";

    public static final String[] addonTypes = new String[] { "binding", "ui", "persistence", "action", "voice",
            "transformation", "misc" };

    private static final Logger logger = LoggerFactory.getLogger(FeatureInstaller.class);

    private FeaturesService featuresService;
    private ConfigurationAdmin configurationAdmin;

    private boolean paxCfgUpdated = true; // a flag used to check whether CM has already successfully updated the pax
                                          // configuration as this must be waited for before trying to add feature repos

    protected void setFeaturesService(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }

    protected void unsetFeaturesService(FeaturesService featuresService) {
        this.featuresService = null;
    }

    protected void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    protected void unsetConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = null;
    }

    protected void activate(final Map<String, Object> config) {
        modified(config);
    }

    protected void modified(final Map<String, Object> config) {
        if (config.get(CFG_REMOTE) == null && getOnlineStatus() == true) {
            // we seem to have an online distro and no configuration set
            updateOnlineConfigFlag(true);
            return;
        } else {
            boolean online = config.get(CFG_REMOTE) != null && "true".equals(config.get(CFG_REMOTE).toString());
            if (getOnlineStatus() != online) {
                setOnlineStatus(online);
            }
        }

        final FeaturesService service = featuresService;
        ExecutorService scheduler = Executors.newSingleThreadExecutor();

        scheduler.execute(new Runnable() {
            @Override
            public void run() {
                int counter = 0;
                // wait up to 5 seconds for the config update event
                while (!paxCfgUpdated && counter++ < 50) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }
                }
                setLegacyExtensions(service, config);
                setExperimentalExtensions(service, config);
                installPackage(service, config);
                installAddons(service, config);
            }
        });
    }

    private void setLegacyExtensions(FeaturesService service, Map<String, Object> config) {
        if (config.get(CFG_LEGACY) != null && "true".equals(config.get(CFG_LEGACY).toString())) {
            try {
                service.addRepository(LEGACY_FEATURES_URI);
            } catch (Exception e) {
                logger.debug("Failed adding feature repo for legacy features - we might be offline: {}",
                        e.getMessage());
            }
        } else {
            try {
                service.removeRepository(LEGACY_FEATURES_URI);
            } catch (Exception e) {
                logger.error("Failed removing feature repo of legacy features: {}", e.getMessage());
            }
        }
    }

    private void setExperimentalExtensions(FeaturesService service, Map<String, Object> config) {
        if (config.get(CFG_EXPERIMENTAL) != null && "true".equals(config.get(CFG_EXPERIMENTAL).toString())) {
            try {
                service.addRepository(EXPERIMENTAL_FEATURES_URI);
            } catch (Exception e) {
                logger.debug("Failed adding feature repo for experimental features - we might be offline: {}",
                        e.getMessage());
            }
        } else {
            try {
                service.removeRepository(EXPERIMENTAL_FEATURES_URI);
            } catch (Exception e) {
                logger.error("Failed removing feature repo of experimental features: {}", e.getMessage());
            }
        }
    }

    private boolean getOnlineStatus() {
        try {
            Configuration paxCfg = configurationAdmin.getConfiguration(PAX_URL_PID);
            Dictionary<String, Object> properties = paxCfg.getProperties();
            if (properties == null) {
                return false;
            }
            Object repos = properties.get(PROPERTY_MVN_REPOS);
            List<String> repoCfg;
            if (repos instanceof String) {
                repoCfg = Arrays.asList(((String) repos).split(","));
                for (String r : ONLINE_REPOS) {
                    if (!repoCfg.contains(r)) {
                        return false;
                    }
                }
                return true;
            }
        } catch (IOException e) {
            logger.error("Failed setting the extension management online/offline mode: {}", e.toString());
        }
        return false;

    }

    private void setOnlineStatus(boolean status) {
        try {
            Configuration paxCfg = configurationAdmin.getConfiguration(PAX_URL_PID);
            paxCfg.setBundleLocation("?");
            Dictionary<String, Object> properties = paxCfg.getProperties();
            if (properties == null) {
                properties = new Hashtable<>();
            }
            List<String> repoCfg = new ArrayList<>();
            Object repos = properties.get(PROPERTY_MVN_REPOS);
            if (repos instanceof String) {
                repoCfg = new ArrayList<>(Arrays.asList(((String) repos).split(",")));
                repoCfg.remove("");
            }
            if (status) {
                for (String r : ONLINE_REPOS) {
                    if (!repoCfg.contains(r)) {
                        repoCfg.add(r);
                    }
                }
            } else {
                for (String r : ONLINE_REPOS) {
                    if (repoCfg.contains(r)) {
                        repoCfg.remove(r);
                    }
                }
            }
            properties.put(PROPERTY_MVN_REPOS, StringUtils.join(repoCfg.toArray(), ","));
            paxCfgUpdated = false;
            paxCfg.update(properties);
        } catch (IOException e) {
            logger.error("Failed setting the extension management online/offline mode: {}", e.toString());
        }
    }

    private void updateOnlineConfigFlag(final Boolean online) {
        // let's do this asynchronous to not block the current config dispatching
        ExecutorService scheduler = Executors.newSingleThreadExecutor();
        final ConfigurationAdmin service = configurationAdmin;

        scheduler.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Configuration cfg = service.getConfiguration(ADDONS_SERVICE_PID);
                    Dictionary<String, Object> properties = cfg.getProperties();
                    if (properties == null) {
                        properties = new Hashtable<>();
                    }
                    if (properties.get(CFG_REMOTE) == null
                            || !online.toString().equals(properties.get(CFG_REMOTE).toString())) {
                        // configuration is out of sync, so let's update it
                        properties.put(CFG_REMOTE, online);
                        cfg.update(properties);
                    }
                } catch (IOException e) {
                    logger.error("Failed updating the remote configuration: {}", e.toString());
                }
            }
        });
    }

    private void installAddons(final FeaturesService service, final Map<String, Object> config) {
        for (String type : addonTypes) {
            Object install = config.get(type);
            if (install instanceof String) {
                installFeatures(service, type, (String) install);
            }
        }
    }

    private void installFeatures(FeaturesService featuresService, String type, String install) {
        for (String addon : install.split(",")) {
            if (StringUtils.isNotBlank(addon)) {
                String name = PREFIX + type + "-" + addon.trim();
                installFeature(featuresService, name);
            }
        }
    }

    private static void installFeature(FeaturesService featuresService, String name) {
        try {
            if (!isInstalled(featuresService, name)) {
                featuresService.installFeature(name);
                logger.info("Installed '{}'", name);
            }
        } catch (Exception e) {
            logger.error("Failed installing '{}': {}", name, e.getMessage());
        }
    }

    private static void installPackage(FeaturesService featuresService, final Map<String, Object> config) {
        Object packageName = config.get("package") != null ? config.get("package") : STANDARD_PACKAGE;
        if (packageName instanceof String) {
            String name = PREFIX + PREFIX_PACKAGE + ((String) packageName).trim();
            installFeature(featuresService, name);

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

    private static boolean isInstalled(FeaturesService featuresService, String name) {
        try {
            for (Feature feature : featuresService.listInstalledFeatures()) {
                if (feature.getName().equals(name)) {
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("Failed retrieving features: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public void configurationEvent(ConfigurationEvent event) {
        if (event.getPid().equals(PAX_URL_PID) && event.getType() == ConfigurationEvent.CM_UPDATED) {
            paxCfgUpdated = true;
        }
    }
}
