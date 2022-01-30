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
package org.openhab.core.addon.marketplace.internal.json;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
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
import org.openhab.core.addon.marketplace.MarketplaceAddonHandler;
import org.openhab.core.addon.marketplace.MarketplaceHandlerException;
import org.openhab.core.addon.marketplace.internal.json.model.AddonEntryDTO;
import org.openhab.core.config.core.ConfigParser;
import org.openhab.core.config.core.ConfigurableService;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * This class is a {@link AddonService} retrieving JSON marketplace information.
 *
 * @author Yannick Schaus - Initial contribution
 * @author Jan N. Klug - Refactored for JSON marketplaces
 */
@Component(immediate = true, configurationPid = { "org.openhab.jsonaddonservice" }, //
        property = Constants.SERVICE_PID + "=org.openhab.jsonaddonservice")
@ConfigurableService(category = "system", label = JsonAddonService.SERVICE_NAME, description_uri = JsonAddonService.CONFIG_URI)
@NonNullByDefault
public class JsonAddonService implements AddonService {
    private final Logger logger = LoggerFactory.getLogger(JsonAddonService.class);

    static final String SERVICE_NAME = "Json 3rd Party Add-on Service";
    static final String CONFIG_URI = "system:jsonaddonservice";

    private static final String SERVICE_ID = "json";
    private static final String ADDON_ID_PREFIX = SERVICE_ID + ":";

    private static final String CONFIG_URLS = "urls";
    private static final String CONFIG_SHOW_UNSTABLE = "showUnstable";
    private static final String CFG_REMOTE = "remote";

    private static final Map<String, AddonType> TAG_ADDON_TYPE_MAP = Map.of( //
            "automation", new AddonType("automation", "Automation"), //
            "binding", new AddonType("binding", "Bindings"), //
            "misc", new AddonType("misc", "Misc"), //
            "persistence", new AddonType("persistence", "Persistence"), //
            "transformation", new AddonType("transformation", "Transformations"), //
            "ui", new AddonType("ui", "User Interfaces"), //
            "voice", new AddonType("voice", "Voice"));

    private final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").create();
    private final Set<MarketplaceAddonHandler> addonHandlers = new HashSet<>();

    private List<String> addonserviceUrls = List.of();
    private List<AddonEntryDTO> cachedAddons = List.of();

    private boolean showUnstable = false;

    private final EventPublisher eventPublisher;
    private final ConfigurationAdmin configurationAdmin;

    @Activate
    public JsonAddonService(@Reference EventPublisher eventPublisher, @Reference ConfigurationAdmin configurationAdmin,
            Map<String, Object> config) {
        this.eventPublisher = eventPublisher;
        this.configurationAdmin = configurationAdmin;
        modified(config);
    }

    @Modified
    public void modified(Map<String, Object> config) {
        String urls = Objects.requireNonNullElse((String) config.get(CONFIG_URLS), "");
        addonserviceUrls = Arrays.asList(urls.split("\\|"));
        showUnstable = (Boolean) config.getOrDefault(CONFIG_SHOW_UNSTABLE, false);
        refreshSource();
    }

    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC)
    protected void addAddonHandler(MarketplaceAddonHandler handler) {
        this.addonHandlers.add(handler);
    }

    protected void removeAddonHandler(MarketplaceAddonHandler handler) {
        this.addonHandlers.remove(handler);
    }

    @Override
    public String getId() {
        return SERVICE_ID;
    }

    @Override
    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void refreshSource() {
        if (!remoteEnabled()) {
            cachedAddons = List.of();
            return;
        }

        cachedAddons = (List<AddonEntryDTO>) addonserviceUrls.stream().map(urlString -> {
            try {
                URL url = new URL(urlString);
                URLConnection connection = url.openConnection();
                connection.addRequestProperty("Accept", "application/json");
                try (Reader reader = new InputStreamReader(connection.getInputStream())) {
                    Type type = TypeToken.getParameterized(List.class, AddonEntryDTO.class).getType();
                    return (List<AddonEntryDTO>) Objects.requireNonNull(gson.fromJson(reader, type));
                }
            } catch (IOException e) {
                return List.of();
            }
        }).flatMap(List::stream).filter(e -> showUnstable || "stable".equals(((AddonEntryDTO) e).maturity))
                .collect(Collectors.toList());
    }

    @Override
    public List<Addon> getAddons(@Nullable Locale locale) {
        refreshSource();
        return cachedAddons.stream().map(this::fromAddonEntry).collect(Collectors.toList());
    }

    @Override
    public @Nullable Addon getAddon(String id, @Nullable Locale locale) {
        String remoteId = id.replace(ADDON_ID_PREFIX, "");
        return cachedAddons.stream().filter(e -> remoteId.equals(e.id)).map(this::fromAddonEntry).findAny()
                .orElse(null);
    }

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
                            postUninstalledEvent(addon.getId());
                        } catch (MarketplaceHandlerException e) {
                            postFailureEvent(addon.getId(), e.getMessage());
                        }
                    } else {
                        postFailureEvent(addon.getId(), "Add-on is not installed.");
                    }
                    return;
                }
            }
        }
        postFailureEvent(id, "Add-on not known.");
    }

    @Override
    public @Nullable String getAddonId(URI addonURI) {
        return null;
    }

    private Addon fromAddonEntry(AddonEntryDTO addonEntry) {
        String fullId = ADDON_ID_PREFIX + addonEntry.id;
        boolean installed = addonHandlers.stream().anyMatch(
                handler -> handler.supports(addonEntry.type, addonEntry.contentType) && handler.isInstalled(fullId));

        Map<String, Object> properties = new HashMap<>();
        if (addonEntry.url.endsWith(".jar")) {
            properties.put("jar_download_url", addonEntry.url);
        } else if (addonEntry.url.endsWith(".kar")) {
            properties.put("kar_download_url", addonEntry.url);
        } else if (addonEntry.url.endsWith(".json")) {
            properties.put("json_download_url", addonEntry.url);
        } else if (addonEntry.url.endsWith(".yaml")) {
            properties.put("yaml_download_url", addonEntry.url);
        }

        return Addon.create(fullId).withType(addonEntry.type).withInstalled(installed)
                .withDetailedDescription(addonEntry.description).withContentType(addonEntry.contentType)
                .withAuthor(addonEntry.author).withVersion(addonEntry.version).withLabel(addonEntry.title)
                .withMaturity(addonEntry.maturity).withProperties(properties).withLink(addonEntry.link)
                .withConfigDescriptionURI(addonEntry.configDescriptionURI).build();
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

    private boolean remoteEnabled() {
        try {
            Configuration configuration = configurationAdmin.getConfiguration("org.openhab.addons", null);
            Dictionary<String, Object> properties = configuration.getProperties();
            if (properties == null) {
                // if we can't determine a set property, we use true (default is remote enabled)
                return true;
            }
            return ConfigParser.valueAsOrElse(properties.get(CFG_REMOTE), Boolean.class, true);
        } catch (IOException e) {
            return true;
        }
    }
}
