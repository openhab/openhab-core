/*
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
package org.openhab.core.addon.marketplace;

import static org.openhab.core.common.ThreadPoolManager.THREAD_POOL_NAME_COMMON;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.OpenHAB;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.AddonEventFactory;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.addon.AddonInfoRegistry;
import org.openhab.core.addon.AddonService;
import org.openhab.core.addon.AddonType;
import org.openhab.core.cache.ExpiringCache;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.config.core.ConfigParser;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * The {@link AbstractRemoteAddonService} implements basic functionality of a remote add-on-service
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractRemoteAddonService implements AddonService {
    static final String CONFIG_REMOTE_ENABLED = "remote";
    static final String CONFIG_INCLUDE_INCOMPATIBLE = "includeIncompatible";
    static final Comparator<Addon> BY_COMPATIBLE_AND_VERSION = (addon1, addon2) -> {
        // prefer compatible to incompatible
        int compatible = Boolean.compare(addon2.getCompatible(), addon1.getCompatible());
        if (compatible != 0) {
            return compatible;
        }
        try {
            // Add-on versions often contain a dash instead of a dot as separator for the qualifier (e.g. -SNAPSHOT)
            // This is not a valid format and everything after the dash needs to be removed.
            BundleVersion version1 = new BundleVersion(addon1.getVersion().replaceAll("-.*", ".0"));
            BundleVersion version2 = new BundleVersion(addon2.getVersion().replaceAll("-.*", ".0"));

            // prefer newer version over older
            return version2.compareTo(version1);
        } catch (IllegalArgumentException e) {
            // assume they are equal (for ordering) if we can't compare the versions
            return 0;
        }
    };

    protected final BundleVersion coreVersion;

    protected final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").create();
    protected final Set<MarketplaceAddonHandler> addonHandlers = new HashSet<>();
    protected final Storage<String> installedAddonStorage;
    protected final EventPublisher eventPublisher;
    protected final ConfigurationAdmin configurationAdmin;
    protected final ExpiringCache<List<Addon>> cachedRemoteAddons = new ExpiringCache<>(Duration.ofMinutes(15),
            this::getRemoteAddons);
    protected final AddonInfoRegistry addonInfoRegistry;
    protected List<Addon> cachedAddons = List.of();
    protected List<String> installedAddonIds = List.of();

    private final Logger logger = LoggerFactory.getLogger(AbstractRemoteAddonService.class);
    private final ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool(THREAD_POOL_NAME_COMMON);

    protected AbstractRemoteAddonService(EventPublisher eventPublisher, ConfigurationAdmin configurationAdmin,
            StorageService storageService, AddonInfoRegistry addonInfoRegistry, String servicePid) {
        this.addonInfoRegistry = addonInfoRegistry;
        this.eventPublisher = eventPublisher;
        this.configurationAdmin = configurationAdmin;
        this.installedAddonStorage = storageService.getStorage(servicePid);
        this.coreVersion = getCoreVersion();
    }

    protected BundleVersion getCoreVersion() {
        return new BundleVersion(FrameworkUtil.getBundle(OpenHAB.class).getVersion().toString());
    }

    private Addon convertFromStorage(Map.Entry<String, @Nullable String> entry) {
        Addon storedAddon = Objects.requireNonNull(gson.fromJson(entry.getValue(), Addon.class));
        AddonInfo addonInfo = addonInfoRegistry.getAddonInfo(storedAddon.getType() + "-" + storedAddon.getId());
        if (addonInfo != null && storedAddon.getConfigDescriptionURI().isBlank()) {
            return Addon.create(storedAddon).withConfigDescriptionURI(addonInfo.getConfigDescriptionURI()).build();
        } else {
            return storedAddon;
        }
    }

    @Override
    public void refreshSource() {
        if (!addonHandlers.stream().allMatch(MarketplaceAddonHandler::isReady)) {
            logger.debug("Add-on service '{}' tried to refresh source before add-on handlers ready. Exiting.",
                    getClass());
            return;
        }

        List<Addon> addons = new ArrayList<>();

        // retrieve add-ons that should be available from storage and check if they are really installed
        // this is safe, because the {@link AddonHandler}s only report ready when they installed everything from the
        // cache
        try {
            installedAddonStorage.stream().map(this::convertFromStorage).forEach(addon -> {
                setInstalled(addon);
                addons.add(addon);
            });
        } catch (JsonSyntaxException e) {
            List.copyOf(installedAddonStorage.getKeys()).forEach(installedAddonStorage::remove);
            logger.error(
                    "Failed to read JSON database, trying to purge it. You might need to re-install {} from the '{}' service.",
                    installedAddonStorage.getKeys(), getId());
            refreshSource();
        }

        // remove not installed add-ons from the add-ons list, but remember their UIDs to re-install them
        List<String> missingAddons = addons.stream().filter(addon -> !addon.isInstalled()).map(Addon::getUid).toList();
        missingAddons.forEach(installedAddonStorage::remove);
        addons.removeIf(addon -> missingAddons.contains(addon.getUid()));

        // create lookup list to make sure installed addons take precedence
        List<String> currentAddonIds = addons.stream().map(Addon::getUid).toList();

        // get the remote addons
        if (remoteEnabled()) {
            List<Addon> remoteAddons = Objects.requireNonNullElse(cachedRemoteAddons.getValue(), List.of());
            remoteAddons.stream().filter(a -> !currentAddonIds.contains(a.getUid())).forEach(addon -> {
                setInstalled(addon);
                addons.add(addon);
            });
        }

        // remove incompatible add-ons if not enabled
        boolean showIncompatible = includeIncompatible();
        addons.removeIf(addon -> !addon.isInstalled() && !addon.getCompatible() && !showIncompatible);

        // check and remove duplicate uids
        Map<String, List<Addon>> addonMap = new HashMap<>();
        addons.forEach(a -> addonMap.computeIfAbsent(a.getUid(), k -> new ArrayList<>()).add(a));
        for (List<Addon> partialAddonList : addonMap.values()) {
            if (partialAddonList.size() > 1) {
                partialAddonList.stream().sorted(BY_COMPATIBLE_AND_VERSION).skip(1).forEach(addons::remove);
            }
        }

        cachedAddons = addons;
        this.installedAddonIds = currentAddonIds;

        if (!missingAddons.isEmpty()) {
            logger.info("Re-installing missing add-ons from remote repository: {}", missingAddons);
            scheduler.execute(() -> missingAddons.forEach(this::install));
        }
    }

    private void setInstalled(Addon addon) {
        addon.setInstalled(addonHandlers.stream().anyMatch(h -> h.isInstalled(addon.getUid())));
    }

    /**
     * Add a {@link MarketplaceAddonHandler} to this service
     *
     * This needs to be implemented by the addon-services because the handlers are references to OSGi services and
     * the @Reference annotation is not inherited.
     * It is added here to make sure that implementations comply with that.
     *
     * @param handler the handler that shall be added
     */
    protected abstract void addAddonHandler(MarketplaceAddonHandler handler);

    /**
     * Remove a {@link MarketplaceAddonHandler} from this service
     *
     * This needs to be implemented by the addon-services because the handlers are references to OSGi services and
     * unbind methods can't be inherited.
     * It is added here to make sure that implementations comply with that.
     *
     * @param handler the handler that shall be removed
     */
    protected abstract void removeAddonHandler(MarketplaceAddonHandler handler);

    /**
     * get all addons from remote
     *
     * @return a list of {@link Addon} that are available on the remote side
     */
    protected abstract List<Addon> getRemoteAddons();

    @Override
    public List<Addon> getAddons(@Nullable Locale locale) {
        refreshSource();
        return cachedAddons;
    }

    @Override
    public List<AddonType> getTypes(@Nullable Locale locale) {
        return AddonType.DEFAULT_TYPES;
    }

    @Override
    public void install(String id) {
        Addon addon = getAddon(id, null);
        if (addon == null) {
            postFailureEvent(id, "Add-on can't be installed because it is not known.");
            return;
        }
        for (MarketplaceAddonHandler handler : addonHandlers) {
            String type = addon.getType();
            String contentType = addon.getContentType();
            if (type != null && contentType != null && handler.supports(type, contentType)) {
                if (!handler.isInstalled(addon.getUid())) {
                    try {
                        handler.install(addon);
                        addon.setInstalled(true);
                        installedAddonStorage.put(id, gson.toJson(addon));
                        cachedRemoteAddons.invalidateValue();
                        refreshSource();
                        postInstalledEvent(addon.getUid());
                    } catch (MarketplaceHandlerException e) {
                        postFailureEvent(addon.getUid(), e.getMessage());
                    }
                } else {
                    postFailureEvent(addon.getUid(), "Add-on is already installed.");
                }
                return;
            }
        }
        postFailureEvent(id, "Add-on can't be installed because there is no handler for it.");
    }

    @Override
    public void uninstall(String id) {
        Addon addon = getAddon(id, null);
        if (addon == null) {
            postFailureEvent(id, "Add-on can't be uninstalled because it is not known.");
            return;
        }
        for (MarketplaceAddonHandler handler : addonHandlers) {
            String type = addon.getType();
            String contentType = addon.getContentType();
            if (type != null && contentType != null && handler.supports(type, contentType)) {
                if (handler.isInstalled(addon.getUid())) {
                    try {
                        handler.uninstall(addon);
                        installedAddonStorage.remove(id);
                        cachedRemoteAddons.invalidateValue();
                        refreshSource();
                        postUninstalledEvent(addon.getUid());
                    } catch (MarketplaceHandlerException e) {
                        postFailureEvent(addon.getUid(), e.getMessage());
                    }
                } else {
                    installedAddonStorage.remove(id);
                    postFailureEvent(addon.getUid(), "Add-on is not installed.");
                }
                return;
            }
        }
        postFailureEvent(id, "Add-on can't be uninstalled because there is no handler for it.");
    }

    /**
     * check if remote services are enabled
     *
     * @return true if network access is allowed
     */
    protected boolean remoteEnabled() {
        try {
            Configuration configuration = configurationAdmin.getConfiguration("org.openhab.addons", null);
            Dictionary<String, Object> properties = configuration.getProperties();
            if (properties == null) {
                // if we can't determine a set property, we use true (default is remote enabled)
                return true;
            }
            return ConfigParser.valueAsOrElse(properties.get(CONFIG_REMOTE_ENABLED), Boolean.class, true);
        } catch (IOException e) {
            return true;
        }
    }

    protected boolean includeIncompatible() {
        try {
            Configuration configuration = configurationAdmin.getConfiguration("org.openhab.addons", null);
            Dictionary<String, Object> properties = configuration.getProperties();
            if (properties == null) {
                // if we can't determine a set property, we use false (default is show compatible only)
                return false;
            }
            return ConfigParser.valueAsOrElse(properties.get(CONFIG_INCLUDE_INCOMPATIBLE), Boolean.class, false);
        } catch (IOException e) {
            return false;
        }
    }

    private void postInstalledEvent(String extensionId) {
        Event event = AddonEventFactory.createAddonInstalledEvent(extensionId);
        eventPublisher.post(event);
    }

    private void postUninstalledEvent(String extensionId) {
        Event event = AddonEventFactory.createAddonUninstalledEvent(extensionId);
        eventPublisher.post(event);
    }

    private void postFailureEvent(String extensionId, @Nullable String msg) {
        Event event = AddonEventFactory.createAddonFailureEvent(extensionId, msg);
        eventPublisher.post(event);
    }
}
