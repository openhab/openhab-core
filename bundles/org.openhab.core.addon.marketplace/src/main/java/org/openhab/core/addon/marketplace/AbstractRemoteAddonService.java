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
package org.openhab.core.addon.marketplace;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.AddonEventFactory;
import org.openhab.core.addon.AddonService;
import org.openhab.core.addon.AddonType;
import org.openhab.core.cache.ExpiringCache;
import org.openhab.core.config.core.ConfigParser;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The {@link AbstractRemoteAddonService} implements basic functionality of a remote add-on-service
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractRemoteAddonService implements AddonService {
    protected static final Map<String, AddonType> TAG_ADDON_TYPE_MAP = Map.of( //
            "automation", new AddonType("automation", "Automation"), //
            "binding", new AddonType("binding", "Bindings"), //
            "misc", new AddonType("misc", "Misc"), //
            "persistence", new AddonType("persistence", "Persistence"), //
            "transformation", new AddonType("transformation", "Transformations"), //
            "ui", new AddonType("ui", "User Interfaces"), //
            "voice", new AddonType("voice", "Voice"));

    protected final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").create();
    protected final Set<MarketplaceAddonHandler> addonHandlers = new HashSet<>();
    protected final Storage<String> installedAddonStorage;
    protected final EventPublisher eventPublisher;
    protected final ConfigurationAdmin configurationAdmin;
    protected final ExpiringCache<List<Addon>> cachedRemoteAddons = new ExpiringCache<>(Duration.ofMinutes(15),
            this::getRemoteAddons);
    protected List<Addon> cachedAddons = List.of();
    protected List<String> installedAddons = List.of();

    public AbstractRemoteAddonService(EventPublisher eventPublisher, ConfigurationAdmin configurationAdmin,
            StorageService storageService, String servicePid) {
        this.eventPublisher = eventPublisher;
        this.configurationAdmin = configurationAdmin;
        this.installedAddonStorage = storageService.getStorage(servicePid);
    }

    @Override
    public void refreshSource() {
        List<Addon> addons = new ArrayList<>();
        installedAddonStorage.stream().map(e -> Objects.requireNonNull(gson.fromJson(e.getValue(), Addon.class)))
                .forEach(addons::add);

        // create lookup list to make sure installed addons take precedence
        List<String> installedAddons = addons.stream().map(Addon::getId).collect(Collectors.toList());

        if (remoteEnabled()) {
            List<Addon> remoteAddons = Objects.requireNonNullElse(cachedRemoteAddons.getValue(), List.of());
            remoteAddons.stream().filter(a -> !installedAddons.contains(a.getId())).forEach(addons::add);
        }

        // check real installation status based on handlers
        addons.forEach(addon -> addon.setInstalled(addonHandlers.stream().anyMatch(h -> h.isInstalled(addon.getId()))));

        cachedAddons = addons;
        this.installedAddons = installedAddons;
    }

    /**
     * Add an {@link MarketplaceAddonHandler) to this service
     *
     * This needs to be implemented by the addon-services because the handlers are references to OSGi services and
     * the @Reference annotation is not inherited.
     * It is added here to make sure that implementations comply with that.
     *
     * @param handler the handler that shall be added
     */
    protected abstract void addAddonHandler(MarketplaceAddonHandler handler);

    /**
     * Remove an {@link MarketplaceAddonHandler) from this service
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
    public abstract @Nullable Addon getAddon(String id, @Nullable Locale locale);

    @Override
    public List<AddonType> getTypes(@Nullable Locale locale) {
        return new ArrayList<>(TAG_ADDON_TYPE_MAP.values());
    }

    @Override
    public void install(String id) {
        Addon addon = getAddon(id, null);
        if (addon != null) {
            for (MarketplaceAddonHandler handler : addonHandlers) {
                if (handler.supports(addon.getType(), addon.getContentType())) {
                    if (!handler.isInstalled(addon.getId())) {
                        try {
                            handler.install(addon);
                            installedAddonStorage.put(id, gson.toJson(addon));
                            refreshSource();
                            postInstalledEvent(addon.getId());
                        } catch (MarketplaceHandlerException e) {
                            postFailureEvent(addon.getId(), e.getMessage());
                        }
                    } else {
                        postFailureEvent(addon.getId(), "Add-on is already installed.");
                    }
                    return;
                }
            }
        }
        postFailureEvent(id, "Add-on not known.");
    }

    @Override
    public void uninstall(String id) {
        Addon addon = getAddon(id, null);
        if (addon != null) {
            for (MarketplaceAddonHandler handler : addonHandlers) {
                if (handler.supports(addon.getType(), addon.getContentType())) {
                    if (handler.isInstalled(addon.getId())) {
                        try {
                            handler.uninstall(addon);
                            installedAddonStorage.remove(id);
                            refreshSource();
                            postUninstalledEvent(addon.getId());
                        } catch (MarketplaceHandlerException e) {
                            postFailureEvent(addon.getId(), e.getMessage());
                        }
                    } else {
                        installedAddonStorage.remove(id);
                        postFailureEvent(addon.getId(), "Add-on is not installed.");
                    }
                    return;
                }
            }
        }
        postFailureEvent(id, "Add-on not known.");
    }

    @Override
    public abstract @Nullable String getAddonId(URI addonURI);

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
            return ConfigParser.valueAsOrElse(properties.get("remote"), Boolean.class, true);
        } catch (IOException e) {
            return true;
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
