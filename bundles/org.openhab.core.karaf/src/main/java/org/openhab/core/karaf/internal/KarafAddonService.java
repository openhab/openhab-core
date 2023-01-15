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

import java.net.URI;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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
    private static final List<AddonType> ADDON_TYPES = List.of( //
            new AddonType(FeatureInstaller.EXTENSION_TYPE_AUTOMATION, "Automation"), //
            new AddonType(FeatureInstaller.EXTENSION_TYPE_BINDING, "Bindings"), //
            new AddonType(FeatureInstaller.EXTENSION_TYPE_MISC, "Misc"), //
            new AddonType(FeatureInstaller.EXTENSION_TYPE_VOICE, "Voice"), //
            new AddonType(FeatureInstaller.EXTENSION_TYPE_PERSISTENCE, "Persistence"), //
            new AddonType(FeatureInstaller.EXTENSION_TYPE_TRANSFORMATION, "Transformations"), //
            new AddonType(FeatureInstaller.EXTENSION_TYPE_UI, "User Interfaces"));

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
            return Arrays.stream(featuresService.listFeatures()).filter(this::isAddon).map(this::getAddon)
                    .sorted(Comparator.comparing(Addon::getLabel)).toList();
        } catch (Exception e) {
            logger.error("Exception while retrieving features: {}", e.getMessage());
            return List.of();
        }
    }

    private boolean isAddon(Feature feature) {
        return feature.getName().startsWith(FeatureInstaller.PREFIX)
                && FeatureInstaller.EXTENSION_TYPES.contains(getType(feature.getName()));
    }

    @Override
    public @Nullable Addon getAddon(String id, @Nullable Locale locale) {
        Feature feature;
        try {
            feature = featuresService.getFeature(FeatureInstaller.PREFIX + id);
            return getAddon(feature);
        } catch (Exception e) {
            logger.error("Exception while querying feature '{}'", id);
            return null;
        }
    }

    private @Nullable String getDefaultDocumentationLink(String type, String name) {
        return switch (type) {
            case FeatureInstaller.EXTENSION_TYPE_AUTOMATION -> "https://www.openhab.org/addons/automation/" + name
                    + "/";
            case FeatureInstaller.EXTENSION_TYPE_BINDING -> "https://www.openhab.org/addons/bindings/" + name + "/";
            case FeatureInstaller.EXTENSION_TYPE_PERSISTENCE -> "https://www.openhab.org/addons/persistence/" + name
                    + "/";
            case FeatureInstaller.EXTENSION_TYPE_TRANSFORMATION -> "https://www.openhab.org/addons/transformations/"
                    + name + "/";
            case FeatureInstaller.EXTENSION_TYPE_VOICE -> "https://www.openhab.org/addons/voice/" + name + "/";
            default -> null;
        };
    }

    private Addon getAddon(Feature feature) {
        String name = getName(feature.getName());
        String type = getType(feature.getName());
        String id = type + Addon.ADDON_SEPARATOR + name;
        boolean isInstalled = featuresService.isInstalled(feature);

        Addon.Builder addon = Addon.create(id).withContentType(ADDONS_CONTENT_TYPE).withType(type)
                .withVersion(feature.getVersion()).withAuthor(ADDONS_AUTHOR, true).withInstalled(isInstalled);

        AddonInfo addonInfo = addonInfoRegistry.getAddonInfo(id);

        if (isInstalled && addonInfo != null) {
            // only enrich if this add-on is installed, otherwise wrong data might be added
            addon = addon.withLabel(addonInfo.getName()).withDescription(addonInfo.getDescription())
                    .withCountries(addonInfo.getCountries()).withLink(getDefaultDocumentationLink(type, name))
                    .withConfigDescriptionURI(addonInfo.getConfigDescriptionURI());
        } else {
            addon = addon.withLabel(feature.getDescription()).withLink(getDefaultDocumentationLink(type, name));
        }

        return addon.build();
    }

    @Override
    public List<AddonType> getTypes(@Nullable Locale locale) {
        return ADDON_TYPES;
    }

    @Override
    public void install(String id) {
        featureInstaller.addAddon(getType(id), getName(id));
    }

    @Override
    public void uninstall(String id) {
        featureInstaller.removeAddon(getType(id), getName(id));
    }

    @Override
    public @Nullable String getAddonId(URI addonURI) {
        return null;
    }

    private String getType(String name) {
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
