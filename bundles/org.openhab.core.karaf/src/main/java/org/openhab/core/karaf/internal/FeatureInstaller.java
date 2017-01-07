/**
 * Copyright (c) 2015-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.core.karaf.internal;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.eclipse.smarthome.config.core.ConfigConstants;
import org.eclipse.smarthome.core.events.Event;
import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.extension.ExtensionEventFactory;
import org.openhab.core.OpenHAB;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service reads addons.cfg and installs listed addons (= Karaf features) and the selected package.
 * It furthermore allows configuration of the base package through the Paper UI as well as administrating Karaf to
 * access remote repos and certain feature repos like for legacy or experimental features.
 *
 * @author Kai Kreuzer - Initial contribution and API
 */
public class FeatureInstaller implements ConfigurationListener {

    private static final String CFG_REMOTE = "remote";
    private static final String CFG_LEGACY = "legacy";

    private static final String PAX_URL_PID = "org.ops4j.pax.url.mvn";
    private static final String PROPERTY_MVN_REPOS = "org.ops4j.pax.url.mvn.repositories";

    public static final String STANDARD_PACKAGE = "standard";
    public static final String PREFIX = "openhab-";
    public static final String PREFIX_PACKAGE = "package-";

    public static final String[] addonTypes = new String[] { "binding", "ui", "persistence", "action", "voice",
            "transformation", "misc" };

    private static final Logger logger = LoggerFactory.getLogger(FeatureInstaller.class);

    private String online_repo_url = null;
    private URI legacy_addons_url = null;

    private FeaturesService featuresService;
    private ConfigurationAdmin configurationAdmin;
    private static EventPublisher eventPublisher;

    private boolean paxCfgUpdated = true; // a flag used to check whether CM has already successfully updated the pax
                                          // configuration as this must be waited for before trying to add feature repos

    private static String currentPackage = null;

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

    protected void setEventPublisher(EventPublisher eventPublisher) {
        FeatureInstaller.eventPublisher = eventPublisher;
    }

    protected void unsetEventPublisher(EventPublisher eventPublisher) {
        FeatureInstaller.eventPublisher = null;
    }

    protected void activate(final Map<String, Object> config) {
        setOnlineRepoUrl();
        setLegacyRepoUrl();
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
                installPackage(service, config);
                installAddons(service, config);
            }
        });
    }

    public boolean addAddon(String type, String id) {
        try {
            Configuration cfg = configurationAdmin.getConfiguration(OpenHAB.ADDONS_SERVICE_PID, null);
            Dictionary<String, Object> props = cfg.getProperties();
            Object typeProp = props.get(type);
            String[] addonIds = typeProp != null ? typeProp.toString().split(",") : new String[0];
            if (!ArrayUtils.contains(addonIds, id)) {
                ArrayList<String> newAddonIds = new ArrayList<>(addonIds.length + 1);
                newAddonIds.addAll(Arrays.asList(addonIds));
                newAddonIds.add(id);
                props.put(type, StringUtils.join(newAddonIds, ','));
                cfg.update(props);
                return true;
            } else {
                // it is already contained
                return false;
            }
        } catch (IOException e) {
            logger.warn("Adding add-on 'openhab-{}-{}' failed: {}", type, id, e.getMessage());
            return false;
        }
    }

    public boolean removeAddon(String type, String id) {
        try {
            Configuration cfg = configurationAdmin.getConfiguration(OpenHAB.ADDONS_SERVICE_PID, null);
            Dictionary<String, Object> props = cfg.getProperties();
            Object typeProp = props.get(type);
            String[] addonIds = typeProp != null ? typeProp.toString().split(",") : new String[0];
            if (ArrayUtils.contains(addonIds, id)) {
                ArrayList<String> newAddonIds = new ArrayList<>(Arrays.asList(addonIds));
                boolean success = newAddonIds.remove(id);
                props.put(type, StringUtils.join(newAddonIds, ','));
                cfg.update(props);
                return success;
            } else {
                // it is not contained, so we cannot remove it
                return false;
            }
        } catch (IOException e) {
            logger.warn("Removing add-on 'openhab-{}-{}' failed: {}", type, id, e.getMessage());
            return false;
        }
    }

    private void setOnlineRepoUrl() {
        Properties prop = new Properties();

        Path versionFilePath = Paths.get(ConfigConstants.getUserDataFolder(), "etc", "version.properties");
        try (FileInputStream fis = new FileInputStream(versionFilePath.toFile())) {
            prop.load(fis);
        } catch (Exception e) {
            logger.warn("Cannot determine online repo url - online repo support will be disabled. Error: {}",
                    e.getMessage());
        }

        String repo = prop.getProperty("online-repo");
        if (repo != null && !repo.trim().isEmpty()) {
            this.online_repo_url = repo.trim() + "@id=openhab@snapshots";
        } else {
            logger.warn("Cannot determine online repo url - online repo support will be disabled.");
        }
    }

    private void setLegacyRepoUrl() {
        Properties prop = new Properties();

        Path versionFilePath = Paths.get(ConfigConstants.getUserDataFolder(), "etc", "version.properties");
        try (FileInputStream fis = new FileInputStream(versionFilePath.toFile())) {
            prop.load(fis);
        } catch (Exception e) {
            logger.warn("Cannot determine distro version - legacy addon support will be disabled. Error: {}",
                    e.getMessage());
        }

        String version = prop.getProperty("openhab-distro");
        if (version != null && !version.trim().isEmpty()) {
            this.legacy_addons_url = URI
                    .create("mvn:org.openhab.distro/openhab-addons-legacy/" + version.trim() + "/xml/features");
        } else {
            logger.warn("Cannot determine distro version - legacy addon support will be disabled.");
        }
    }

    private void setLegacyExtensions(FeaturesService service, Map<String, Object> config) {
        if (legacy_addons_url != null) {
            if (config.get(CFG_LEGACY) != null && "true".equals(config.get(CFG_LEGACY).toString())) {
                try {
                    service.addRepository(legacy_addons_url);
                } catch (Exception e) {
                    logger.debug("Failed adding feature repo for legacy features - we might be offline: {}",
                            e.getMessage());
                }
            } else {
                try {
                    service.removeRepository(legacy_addons_url);
                } catch (Exception e) {
                    // This usually throws an error like
                    // "Feature named 'openhab-binding-homematic1/1.9.0.SNAPSHOT' is not installed"
                    // as Karaf seems to try to uninstall all features for whatever reason
                    // the repo is removed nonetheless in such a case
                    logger.debug("Failed removing feature repo of legacy features: {}", e.getMessage());
                }
            }
        }
    }

    private boolean getOnlineStatus() {
        if (online_repo_url != null) {
            try {
                Configuration paxCfg = configurationAdmin.getConfiguration(PAX_URL_PID, null);
                Dictionary<String, Object> properties = paxCfg.getProperties();
                if (properties == null) {
                    return false;
                }
                Object repos = properties.get(PROPERTY_MVN_REPOS);
                List<String> repoCfg;
                if (repos instanceof String) {
                    repoCfg = Arrays.asList(((String) repos).split(","));
                    return repoCfg.contains(online_repo_url);
                }
            } catch (IOException e) {
                logger.error("Failed getting the add-on management online/offline mode: {}", e.getMessage());
            }
        }
        return false;

    }

    private void setOnlineStatus(boolean status) {
        if (online_repo_url != null) {
            try {
                Configuration paxCfg = configurationAdmin.getConfiguration(PAX_URL_PID, null);
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
                    if (!repoCfg.contains(online_repo_url)) {
                        repoCfg.add(online_repo_url);
                    }
                } else {
                    if (repoCfg.contains(online_repo_url)) {
                        repoCfg.remove(online_repo_url);
                    }
                }
                properties.put(PROPERTY_MVN_REPOS, StringUtils.join(repoCfg.toArray(), ","));
                paxCfgUpdated = false;
                paxCfg.update(properties);
            } catch (IOException e) {
                logger.error("Failed setting the add-on management online/offline mode: {}", e.getMessage());
            }
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
                    Configuration cfg = service.getConfiguration(OpenHAB.ADDONS_SERVICE_PID);
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
                    logger.error("Failed updating the remote configuration: {}", e.getMessage());
                }
            }
        });
    }

    private synchronized void installAddons(final FeaturesService service, final Map<String, Object> config) {
        Set<String> installAddons = new HashSet<>();
        Set<String> installedAddons = new HashSet<>();
        Set<String> uninstallAddons = new HashSet<>();
        for (String type : addonTypes) {
            Object install = config.get(type);
            if (install instanceof String) {
                String[] entries = ((String) install).split(",");
                for (String addon : entries) {
                    if (!StringUtils.isEmpty(addon)) {
                        String id = PREFIX + type + "-" + addon.trim();
                        installedAddons.add(id);
                        if (!isInstalled(service, id)) {
                            installAddons.add(id);
                        }
                    }
                }
                // we collect all possible addons first
                for (String addon : getAllAddonsOfType(type)) {
                    if (!StringUtils.isEmpty(addon)) {
                        uninstallAddons.add(PREFIX + type + "-" + addon.trim());
                    }
                }
            }
        }
        // now remove everything from the list that we want to keep installed
        for (String addon : installedAddons) {
            uninstallAddons.remove(addon);
        }
        if (!installAddons.isEmpty()) {
            installFeatures(service, installAddons);
        }
        if (!uninstallAddons.isEmpty()) {
            for (String addon : uninstallAddons) {
                uninstallFeature(service, addon);
            }
        }
    }

    private Set<String> getAllAddonsOfType(String type) {
        Set<String> addons = new HashSet<>();
        String prefix = FeatureInstaller.PREFIX + type + "-";
        try {
            for (Feature feature : featuresService.listFeatures()) {
                if (feature.getName().startsWith(prefix)) {
                    addons.add(feature.getName().substring(prefix.length()));
                }
            }
        } catch (Exception e) {
            logger.error("Failed retrieving features: {}", e.getMessage());
        }
        return addons;
    }

    private synchronized void installFeatures(FeaturesService featuresService, Set<String> addons) {
        try {
            featuresService.installFeatures(addons,
                    EnumSet.of(FeaturesService.Option.Upgrade, FeaturesService.Option.NoFailOnFeatureNotFound));
            logger.debug("Installed '{}'", StringUtils.join(addons, ", "));
            for (String addon : addons) {
                if (isInstalled(featuresService, addon)) {
                    postInstalledEvent(addon);
                }
            }
        } catch (Exception e) {
            logger.error("Failed installing '{}': {}", StringUtils.join(addons, ", "), e.getMessage());
        }
    }

    private static synchronized void installFeature(FeaturesService featuresService, String name) {
        try {
            if (!isInstalled(featuresService, name)) {
                featuresService.installFeature(name);
                if (isInstalled(featuresService, name)) {
                    logger.debug("Installed '{}'", name);
                    postInstalledEvent(name);
                }
            }
        } catch (Exception e) {
            logger.error("Failed installing '{}': {}", name, e.getMessage());
        }
    }

    private static synchronized void uninstallFeature(FeaturesService featuresService, String name) {
        try {
            if (isInstalled(featuresService, name)) {
                featuresService.uninstallFeature(name);
                logger.info("Uninstalled '{}'", name);
                postUninstalledEvent(name);
            }
        } catch (Exception e) {
            logger.error("Failed uninstalling '{}': {}", name, e.getMessage());
        }
    }

    private static synchronized void installPackage(FeaturesService featuresService, final Map<String, Object> config) {
        Object packageName = config.get(OpenHAB.CFG_PACKAGE);
        if (packageName instanceof String) {
            currentPackage = (String) packageName;
            String name = PREFIX + PREFIX_PACKAGE + ((String) packageName).trim();
            installFeature(featuresService, name);

            // uninstall all other packages
            try {
                for (Feature feature : featuresService.listFeatures()) {
                    if (feature.getName().startsWith(PREFIX + PREFIX_PACKAGE) && !feature.getName().equals(name)) {
                        uninstallFeature(featuresService, feature.getName());
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

    private static void postInstalledEvent(String featureName) {
        String extensionId = featureName.substring(PREFIX.length());
        if (eventPublisher != null) {
            Event event = ExtensionEventFactory.createExtensionInstalledEvent(extensionId);
            eventPublisher.post(event);
        }
    }

    private static void postUninstalledEvent(String featureName) {
        String extensionId = featureName.substring(PREFIX.length());
        if (eventPublisher != null) {
            Event event = ExtensionEventFactory.createExtensionUninstalledEvent(extensionId);
            eventPublisher.post(event);
        }
    }

    String getCurrentPackage() {
        return currentPackage;
    }
}
