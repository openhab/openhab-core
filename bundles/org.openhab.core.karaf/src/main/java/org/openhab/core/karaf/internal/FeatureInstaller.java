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

import static java.util.function.Predicate.not;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.kar.KarService;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.OpenHAB;
import org.openhab.core.addon.AddonEventFactory;
import org.openhab.core.addon.AddonType;
import org.openhab.core.common.NamedThreadFactory;
import org.openhab.core.config.core.ConfigParser;
import org.openhab.core.config.core.ConfigurableService;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service reads addons.cfg and installs listed add-ons (= Karaf features) and the selected package.
 * It furthermore allows configuration of the base package through the UI as well as administrating Karaf to
 * access remote repos and certain feature repos like for experimental features.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@Component(name = "org.openhab.addons", service = { FeatureInstaller.class, ConfigurationListener.class })
@ConfigurableService(category = "system", label = "Add-on Management", description_uri = FeatureInstaller.CONFIG_URI)
@NonNullByDefault
public class FeatureInstaller implements ConfigurationListener {

    protected static final String CONFIG_URI = "system:addons";

    public static final String PREFIX = "openhab-";
    public static final String PREFIX_PACKAGE = "package-";
    public static final String MINIMAL_PACKAGE = "minimal";

    private static final String CFG_REMOTE = "remote";
    private static final String PAX_URL_PID = "org.ops4j.pax.url.mvn";
    private static final String ADDONS_PID = "org.openhab.addons";
    private static final String PROPERTY_MVN_REPOS = "org.ops4j.pax.url.mvn.repositories";

    public static final List<String> ADDON_TYPES = AddonType.DEFAULT_TYPES.stream().map(AddonType::getId)
            .collect(Collectors.toList());

    private final Logger logger = LoggerFactory.getLogger(FeatureInstaller.class);

    private final ConfigurationAdmin configurationAdmin;
    private final FeaturesService featuresService;
    private final KarService karService;
    private final EventPublisher eventPublisher;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean processingConfigQueue = new AtomicBoolean();

    private final LinkedBlockingQueue<Map<String, Object>> configQueue = new LinkedBlockingQueue<>();

    private @Nullable String onlineRepoUrl = null;

    private boolean paxCfgUpdated = true; // a flag used to check whether CM has already successfully updated the pax
                                          // configuration as this must be waited for before trying to add feature repos
    private @Nullable Map<String, Object> configMapCache;

    @Activate
    public FeatureInstaller(final @Reference ConfigurationAdmin configurationAdmin,
            final @Reference FeaturesService featuresService, final @Reference KarService karService,
            final @Reference EventPublisher eventPublisher, Map<String, Object> config) {
        this.configurationAdmin = configurationAdmin;
        this.featuresService = featuresService;
        this.karService = karService;
        this.eventPublisher = eventPublisher;

        scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("karaf-addons"));
        setOnlineRepoUrl();
        modified(config);

        scheduler.scheduleWithFixedDelay(this::syncConfiguration, 1, 1, TimeUnit.MINUTES);
    }

    @Deactivate
    protected void deactivate() {
        scheduler.shutdown();
    }

    @Modified
    protected void modified(final Map<String, Object> config) {
        configQueue.add(config);
        if (processingConfigQueue.compareAndSet(false, true)) {
            scheduler.execute(this::processConfigQueue);
        }
    }

    private void syncConfiguration() {
        logger.debug("Running scheduled sync job");
        try {
            Dictionary<String, Object> cfg = configurationAdmin.getConfiguration(ADDONS_PID).getProperties();
            if (cfg == null) {
                logger.debug("Configuration has no properties yet. Skipping update.");
                return;
            }
            final Map<String, Object> cfgMap = new HashMap<>();
            final Enumeration<String> enumeration = cfg.keys();
            while (enumeration.hasMoreElements()) {
                final String key = enumeration.nextElement();
                cfgMap.put(key, cfg.get(key));
            }
            if (!cfgMap.equals(configMapCache) && !processingConfigQueue.get()) {
                modified(cfgMap);
            }
        } catch (IOException e) {
            logger.debug("Failed to retrieve the addons configuration from configuration admin: {}", e.getMessage());
        }
    }

    private synchronized void processConfigQueue() {
        if (!allKarsInstalled()) {
            // some kars are not installed, delay installation for 15s, we keep the processing flag
            // because further updates will be added to the queue and are therefore not interfering
            // with our order
            // we don't need to keep the job, if the service is shutdown, the scheduler is also shutting
            // down and in all other cases we are protected by the processing flag
            logger.info("Some .kar files are not installed yet. Delaying add-on installation by 15s.");
            scheduler.schedule(this::processConfigQueue, 15, TimeUnit.SECONDS);
            return;
        }

        Map<String, Object> config;
        boolean changed = false;

        while ((config = configQueue.poll()) != null) {
            // cache the last processed config
            configMapCache = config;

            // online mode is either determined by the configuration or by the status of the online repository
            boolean onlineMode = ConfigParser.valueAsOrElse(config.get(CFG_REMOTE), Boolean.class,
                    getOnlineRepositoryMode());
            boolean repoConfigurationChanged = getOnlineRepositoryMode() != onlineMode
                    && setOnlineRepositoryMode(onlineMode);

            if (repoConfigurationChanged) {
                waitForConfigUpdateEvent();
            }

            if (installPackage(config)) {
                changed = true;
                // our package selection has changed, so let's wait for the values to be available in config admin
                // which we will receive as another call to modified
                continue;
            }

            if (installAddons(config)) {
                changed = true;
            }
        }

        processingConfigQueue.set(false);

        try {
            if (changed) {
                featuresService.refreshFeatures(EnumSet.noneOf(FeaturesService.Option.class));
            }
        } catch (Exception e) {
            logger.error("Failed to refresh bundles after processing config update", e);
        }
    }

    public void addAddon(String type, String id) {
        try {
            changeAddonConfig(type, id, Collection::add);
        } catch (IOException e) {
            logger.warn("Adding add-on 'openhab-{}-{}' failed: {}", type, id, e.getMessage(), debugException(e));
        }
    }

    public void removeAddon(String type, String id) {
        try {
            changeAddonConfig(type, id, Collection::remove);
        } catch (IOException e) {
            logger.warn("Removing add-on 'openhab-{}-{}' failed: {}", type, id, e.getMessage(), debugException(e));
        }
    }

    @Override
    public void configurationEvent(@Nullable ConfigurationEvent event) {
        if (event != null && PAX_URL_PID.equals(event.getPid()) && event.getType() == ConfigurationEvent.CM_UPDATED) {
            paxCfgUpdated = true;
        }
    }

    private @Nullable Exception debugException(Exception e) {
        return logger.isDebugEnabled() ? e : null;
    }

    private boolean allKarsInstalled() {
        try {
            List<String> karRepos = karService.list();
            Configuration[] configurations = configurationAdmin
                    .listConfigurations("(service.factoryPid=org.apache.felix.fileinstall)");
            if (configurations.length > 0) {
                Dictionary<String, Object> felixProperties = configurations[0].getProperties();
                String addonsDirectory = (String) felixProperties.get("felix.fileinstall.dir");
                if (addonsDirectory != null) {
                    try (Stream<Path> files = Files.list(Path.of(addonsDirectory))) {
                        return files.map(Path::getFileName).map(Path::toString).filter(file -> file.endsWith(".kar"))
                                .map(karFileName -> karFileName.substring(0, karFileName.lastIndexOf(".")))
                                .allMatch(karRepos::contains);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        logger.warn("Could not determine addons folder, its content or the list of installed repositories!");
        return false;
    }

    private void waitForConfigUpdateEvent() {
        int counter = 0;
        // wait up to 5 seconds for the config update event
        while (!paxCfgUpdated && counter++ < 50) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
        logger.warn("Waited for 5s to receive config update, but configuration was not updated. Proceeding anyway.");
    }

    private void changeAddonConfig(String type, String id, BiFunction<Collection<String>, String, Boolean> method)
            throws IOException {
        Configuration cfg = configurationAdmin.getConfiguration(OpenHAB.ADDONS_SERVICE_PID, null);
        Dictionary<String, Object> props = cfg.getProperties();
        Object typeProp = props.get(type);
        String[] addonIds = typeProp != null ? typeProp.toString().split(",") : new String[0];
        Set<String> normalizedIds = new HashSet<>(); // sets don't allow duplicates
        Arrays.stream(addonIds).map(String::strip).forEach(normalizedIds::add);
        if (method.apply(normalizedIds, id)) {
            // collection changed
            props.put(type, String.join(",", normalizedIds));
            cfg.update(props);
        }
    }

    private void setOnlineRepoUrl() {
        Path versionFilePath = Paths.get(OpenHAB.getUserDataFolder(), "etc", "version.properties");
        try (BufferedReader reader = Files.newBufferedReader(versionFilePath)) {
            Properties prop = new Properties();
            prop.load(reader);

            String repo = prop.getProperty("online-repo", "").strip();
            if (!repo.isEmpty()) {
                this.onlineRepoUrl = repo + "@id=openhab@snapshots";
            } else {
                logger.warn("Cannot determine online repo url - online repo support will be disabled.");
            }
        } catch (Exception e) {
            logger.warn("Cannot determine online repo url - online repo support will be disabled. Error: {}",
                    e.getMessage(), debugException(e));
        }
    }

    /**
     * Checks if the online repository is part of the maven repository list
     *
     * @return <code>true</code> if present, <code>false</code> otherwise
     */
    private boolean getOnlineRepositoryMode() {
        if (onlineRepoUrl != null) {
            try {
                Configuration paxCfg = configurationAdmin.getConfiguration(PAX_URL_PID, null);
                Dictionary<String, Object> properties = paxCfg.getProperties();
                if (properties == null) {
                    return false;
                }
                Object repos = properties.get(PROPERTY_MVN_REPOS);
                if (repos instanceof String) {
                    return List.of(((String) repos).split(",")).contains(onlineRepoUrl);
                }
            } catch (IOException e) {
                logger.error("Failed getting the add-on management online/offline mode: {}", e.getMessage(),
                        debugException(e));
            }
        }
        return false;
    }

    /**
     * Enables or disables the online repository in the maven repository list
     *
     * @param enabled the requested setting
     * @return <code>true</code> if the configuration was changed, <code>false</code> otherwise
     */
    private boolean setOnlineRepositoryMode(boolean enabled) {
        boolean changed = false;
        String onlineRepoUrl = this.onlineRepoUrl;
        if (onlineRepoUrl != null) {
            try {
                Configuration paxCfg = configurationAdmin.getConfiguration(PAX_URL_PID, null);
                paxCfg.setBundleLocation("?");
                Dictionary<String, Object> properties = Objects.requireNonNullElse(paxCfg.getProperties(),
                        new Hashtable<>());
                List<String> repoCfg = new ArrayList<>();
                Object repos = properties.get(PROPERTY_MVN_REPOS);
                if (repos instanceof String) {
                    repoCfg.addAll(Arrays.asList(((String) repos).split(",")));
                    repoCfg.remove("");
                }
                if (enabled && !repoCfg.contains(onlineRepoUrl)) {
                    repoCfg.add(onlineRepoUrl);
                    changed = true;
                    logger.debug("Added repo '{}' to feature repo list.", onlineRepoUrl);
                } else if (!enabled && repoCfg.contains(onlineRepoUrl)) {
                    repoCfg.remove(onlineRepoUrl);
                    changed = true;
                    logger.debug("Removed repo '{}' from feature repo list.", onlineRepoUrl);
                }
                if (changed) {
                    properties.put(PROPERTY_MVN_REPOS, String.join(",", repoCfg));
                    paxCfgUpdated = true;
                    paxCfg.update(properties);
                }
            } catch (IOException e) {
                logger.error("Failed setting the add-on management online/offline mode: {}", e.getMessage(),
                        debugException(e));
            }
        }
        return changed;
    }

    private boolean installAddons(final Map<String, Object> config) {
        final Set<String> currentAddons = new HashSet<>(); // the currently installed ones
        final Set<String> targetAddons = new HashSet<>(); // the target we want to have installed afterwards
        final Set<String> installAddons = new HashSet<>(); // the ones to be installed (the diff)

        for (String type : ADDON_TYPES) {
            Object configValue = config.get(type);
            if (configValue instanceof String addonString) {
                try {
                    Feature[] features = featuresService.listInstalledFeatures();
                    String typePrefix = PREFIX + type + "-";
                    Set<String> configFeatureNames = Arrays.stream(addonString.split(",")) //
                            .map(String::strip) //
                            .filter(not(String::isEmpty)) //
                            .map(addon -> typePrefix + addon) //
                            .collect(Collectors.toSet());

                    for (String name : configFeatureNames) {
                        if (featuresService.getFeature(name) != null) {
                            targetAddons.add(name);
                            if (!anyMatchingFeature(features, withName(name))) {
                                installAddons.add(name);
                            }
                        } else {
                            logger.warn("The {} add-on '{}' does not exist - ignoring it.", type,
                                    name.substring(typePrefix.length()));
                        }
                    }

                    // we collect all installed add-ons of this type
                    getAllFeatureNamesWithPrefix(typePrefix).stream()
                            .filter(name -> anyMatchingFeature(features, withName(name))).forEach(currentAddons::add);
                } catch (Exception e) {
                    logger.error("Failed retrieving features: {}", e.getMessage(), debugException(e));
                }
            }
        }

        // now calculate what we have to uninstall: all current ones that are not part of the target anymore
        Set<String> uninstallAddons = currentAddons.stream().filter(addon -> !targetAddons.contains(addon))
                .collect(Collectors.toSet());

        // do the installation
        if (!installAddons.isEmpty()) {
            installFeatures(installAddons);
        }

        // do the de-installation
        uninstallAddons.forEach(this::uninstallFeature);

        return !installAddons.isEmpty() || !uninstallAddons.isEmpty();
    }

    private Set<String> getAllFeatureNamesWithPrefix(String prefix) {
        try {
            return Arrays.stream(featuresService.listFeatures()) //
                    .map(Feature::getName) //
                    .filter(name -> name.startsWith(prefix)) //
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            logger.error("Failed retrieving features: {}", e.getMessage(), debugException(e));
            return Set.of();
        }
    }

    private void installFeatures(Set<String> addons) {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Installing '{}'", String.join(", ", addons));
            }
            featuresService.installFeatures(addons, EnumSet.of(FeaturesService.Option.NoAutoRefreshBundles,
                    FeaturesService.Option.Upgrade, FeaturesService.Option.NoFailOnFeatureNotFound));
            try {
                Feature[] features = featuresService.listInstalledFeatures();
                Set<String> installed = new HashSet<>();
                Set<String> failed = new HashSet<>();

                for (String addon : addons) {
                    if (anyMatchingFeature(features, withName(addon))) {
                        installed.add(addon);
                    } else {
                        failed.add(addon);
                    }
                }

                if (!installed.isEmpty() && logger.isDebugEnabled()) {
                    logger.debug("Installed '{}'", String.join(", ", installed));
                }
                if (!failed.isEmpty()) {
                    logger.error("Failed installing '{}'", String.join(", ", failed));
                    configMapCache = null; // make sure we retry the installation
                }
                installed.forEach(this::postInstalledEvent);
            } catch (Exception e) {
                logger.error("Failed retrieving features: {}", e.getMessage(), debugException(e));
                configMapCache = null; // make sure we retry the installation
            }
        } catch (Exception e) {
            logger.error("Failed installing '{}': {}", String.join(", ", addons), e.getMessage(), debugException(e));
            configMapCache = null; // make sure we retry the installation
        }
    }

    private boolean installFeature(String name) {
        try {
            Feature[] features = featuresService.listInstalledFeatures();
            if (!anyMatchingFeature(features, withName(name))) {
                featuresService.installFeature(name,
                        EnumSet.of(FeaturesService.Option.Upgrade, FeaturesService.Option.NoAutoRefreshBundles));
                features = featuresService.listInstalledFeatures();
                if (anyMatchingFeature(features, withName(name))) {
                    logger.debug("Installed '{}'", name);
                    postInstalledEvent(name);
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("Failed installing '{}': {}", name, e.getMessage(), debugException(e));
            configMapCache = null; // make sure we retry the installation
        }
        return false;
    }

    private void uninstallFeature(String name) {
        try {
            Feature[] features = featuresService.listInstalledFeatures();
            if (anyMatchingFeature(features, withName(name))) {
                featuresService.uninstallFeature(name);
                logger.debug("Uninstalled '{}'", name);
                postUninstalledEvent(name);
            }
        } catch (Exception e) {
            logger.debug("Failed uninstalling '{}': {}", name, e.getMessage());
        }
    }

    private boolean installPackage(final Map<String, Object> config) {
        boolean configChanged = false;
        Object packageName = config.get(OpenHAB.CFG_PACKAGE);
        if (packageName instanceof String currentPackage) {
            String fullName = PREFIX + PREFIX_PACKAGE + currentPackage.strip();
            if (!MINIMAL_PACKAGE.equals(currentPackage)) {
                configChanged = installFeature(fullName);
            }

            // uninstall all other packages
            try {
                Stream.of(featuresService.listFeatures()).map(Feature::getName)
                        .filter(feature -> feature.startsWith(PREFIX + PREFIX_PACKAGE) && !feature.equals(fullName))
                        .forEach(this::uninstallFeature);
            } catch (Exception e) {
                logger.error("Failed retrieving features: {}", e.getMessage(), debugException(e));
            }
        }
        return configChanged;
    }

    private void postInstalledEvent(String featureName) {
        String extensionId = featureName.substring(PREFIX.length());
        Event event = AddonEventFactory.createAddonInstalledEvent(extensionId);
        eventPublisher.post(event);
    }

    private void postUninstalledEvent(String featureName) {
        String extensionId = featureName.substring(PREFIX.length());
        Event event = AddonEventFactory.createAddonUninstalledEvent(extensionId);
        eventPublisher.post(event);
    }

    private static boolean anyMatchingFeature(Feature[] features, Predicate<Feature> predicate) {
        return Arrays.stream(features).anyMatch(predicate);
    }

    private static Predicate<Feature> withName(final String name) {
        return feature -> feature.getName().equals(name);
    }
}
