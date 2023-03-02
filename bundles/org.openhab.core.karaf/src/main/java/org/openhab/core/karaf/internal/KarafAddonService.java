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

import static java.util.Map.entry;
import static org.openhab.core.addon.AddonType.*;

import java.net.URI;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.addon.AddonInfoRegistry;
import org.openhab.core.addon.AddonService;
import org.openhab.core.addon.AddonType;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service is an implementation of an openHAB {@link AddonService} using the Karaf features service. This
 * exposes all openHAB add-ons through the REST API and allows UIs to dynamically install and uninstall them.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@Component(name = "org.openhab.core.karafaddons")
@NonNullByDefault
public class KarafAddonService implements AddonService {
    private static final String ADDONS_CONTENT_TYPE = "application/vnd.openhab.feature;type=karaf";
    private static final String ADDONS_AUTHOR = "openHAB";

    private static final String DOCUMENTATION_URL_PREFIX = "https://www.openhab.org/addons/";

    private static final Map<String, String> DOCUMENTATION_URL_FORMATS = Map.ofEntries(
            entry(AUTOMATION.getId(), DOCUMENTATION_URL_PREFIX + "automation/%s/"), //
            entry(BINDING.getId(), DOCUMENTATION_URL_PREFIX + "bindings/%s/"), //
            entry(MISC.getId(), DOCUMENTATION_URL_PREFIX + "integrations/%s/"), //
            entry(PERSISTENCE.getId(), DOCUMENTATION_URL_PREFIX + "persistence/%s/"), //
            entry(TRANSFORMATION.getId(), DOCUMENTATION_URL_PREFIX + "transformations/%s/"), //
            entry(UI.getId(), DOCUMENTATION_URL_PREFIX + "ui/%s/"), //
            entry(VOICE.getId(), DOCUMENTATION_URL_PREFIX + "voice/%s/"));

    private final Logger logger = LoggerFactory.getLogger(KarafAddonService.class);

    private final FeaturesService featuresService;
    private final FeatureInstaller featureInstaller;

    private final AddonInfoRegistry addonInfoRegistry;

    @Activate
    public KarafAddonService(final @Reference FeatureInstaller featureInstaller,
            final @Reference FeaturesService featuresService, @Reference AddonInfoRegistry addonInfoRegistry) {
        this.featureInstaller = featureInstaller;
        this.featuresService = featuresService;
        this.addonInfoRegistry = addonInfoRegistry;
    }

    @Override
    public String getId() {
        return "karaf";
    }

    @Override
    public String getName() {
        return "openHAB Distribution";
    }

    @Override
    public void refreshSource() {
    }

    @Override
    public List<Addon> getAddons(@Nullable Locale locale) {
        try {
            return Arrays.stream(featuresService.listFeatures()).filter(this::isAddon).map(f -> getAddon(f, locale))
                    .sorted(Comparator.comparing(Addon::getLabel)).toList();
        } catch (Exception e) {
            logger.error("Exception while retrieving features: {}", e.getMessage());
            return List.of();
        }
    }

    private boolean isAddon(Feature feature) {
        return feature.getName().startsWith(FeatureInstaller.PREFIX)
                && FeatureInstaller.ADDON_TYPES.contains(getAddonType(feature.getName()));
    }

    @Override
    public @Nullable Addon getAddon(String id, @Nullable Locale locale) {
        Feature feature;
        try {
            feature = featuresService.getFeature(FeatureInstaller.PREFIX + id);
            return getAddon(feature, locale);
        } catch (Exception e) {
            logger.error("Exception while querying feature '{}'", id);
            return null;
        }
    }

    private @Nullable String getDefaultDocumentationLink(String type, String name) {
        String format = DOCUMENTATION_URL_FORMATS.get(type);
        return format == null ? null : String.format(format, name);
    }

    private Addon getAddon(Feature feature, @Nullable Locale locale) {
        String name = getName(feature.getName());
        String type = getAddonType(feature.getName());
        String uid = type + Addon.ADDON_SEPARATOR + name;
        boolean isInstalled = featuresService.isInstalled(feature);

        Addon.Builder addon = Addon.create(uid).withType(type).withId(name).withContentType(ADDONS_CONTENT_TYPE)
                .withVersion(feature.getVersion()).withAuthor(ADDONS_AUTHOR, true).withInstalled(isInstalled);

        AddonInfo addonInfo = addonInfoRegistry.getAddonInfo(uid, locale);

        if (isInstalled && addonInfo != null) {
            // only enrich if this add-on is installed, otherwise wrong data might be added
            addon = addon.withLabel(addonInfo.getName()).withDescription(addonInfo.getDescription())
                    .withCountries(addonInfo.getCountries()).withLink(getDefaultDocumentationLink(type, name))
                    .withConfigDescriptionURI(addonInfo.getConfigDescriptionURI());
        } else {
            addon = addon.withLabel(feature.getDescription()).withLink(getDefaultDocumentationLink(type, name));
        }

        List<String> packages = feature.getBundles().stream().filter(bundle -> !bundle.isDependency()).map(bundle -> {
            String location = bundle.getLocation();
            location = location.substring(0, location.lastIndexOf("/")); // strip version
            location = location.substring(location.lastIndexOf("/") + 1); // strip groupId and protocol
            return location;
        }).toList();
        addon.withLoggerPackages(packages);

        return addon.build();
    }

    @Override
    public List<AddonType> getTypes(@Nullable Locale locale) {
        return AddonType.DEFAULT_TYPES;
    }

    @Override
    public void install(String id) {
        featureInstaller.addAddon(getAddonType(id), getName(id));
    }

    @Override
    public void uninstall(String id) {
        featureInstaller.removeAddon(getAddonType(id), getName(id));
    }

    @Override
    public @Nullable String getAddonId(URI addonURI) {
        return null;
    }

    private String getAddonType(String name) {
        String str = name.startsWith(FeatureInstaller.PREFIX) ? name.substring(FeatureInstaller.PREFIX.length()) : name;
        int index = str.indexOf(Addon.ADDON_SEPARATOR);
        return index == -1 ? str : str.substring(0, index);
    }

    private String getName(String name) {
        String str = name.startsWith(FeatureInstaller.PREFIX) ? name.substring(FeatureInstaller.PREFIX.length()) : name;
        int index = str.indexOf(Addon.ADDON_SEPARATOR);
        return index == -1 ? "" : str.substring(index + Addon.ADDON_SEPARATOR.length());
    }
}
