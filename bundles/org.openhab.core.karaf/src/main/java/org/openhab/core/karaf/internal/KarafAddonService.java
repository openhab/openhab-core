/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.openhab.core.addon.Addon;
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
public class KarafAddonService implements AddonService {
    private static final String ADDONS_CONTENTTYPE = "application/java-archive";
    private static final String ADDONS_AUTHOR = "openHAB";

    private final Logger logger = LoggerFactory.getLogger(KarafAddonService.class);

    private final List<AddonType> typeList = new ArrayList<>(FeatureInstaller.EXTENSION_TYPES.length);

    private final FeaturesService featuresService;
    private final FeatureInstaller featureInstaller;

    @Activate
    public KarafAddonService(final @Reference FeatureInstaller featureInstaller,
            final @Reference FeaturesService featuresService) {
        this.featureInstaller = featureInstaller;
        this.featuresService = featuresService;
        typeList.add(new AddonType(FeatureInstaller.EXTENSION_TYPE_AUTOMATION, "Automation"));
        typeList.add(new AddonType(FeatureInstaller.EXTENSION_TYPE_BINDING, "Bindings"));
        typeList.add(new AddonType(FeatureInstaller.EXTENSION_TYPE_MISC, "Misc"));
        typeList.add(new AddonType(FeatureInstaller.EXTENSION_TYPE_VOICE, "Voice"));
        if (!FeatureInstaller.SIMPLE_PACKAGE.equals(featureInstaller.getCurrentPackage())) {
            typeList.add(new AddonType(FeatureInstaller.EXTENSION_TYPE_PERSISTENCE, "Persistence"));
            typeList.add(new AddonType(FeatureInstaller.EXTENSION_TYPE_TRANSFORMATION, "Transformations"));
            typeList.add(new AddonType(FeatureInstaller.EXTENSION_TYPE_UI, "User Interfaces"));
        }
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
    public List<Addon> getAddons(Locale locale) {
        List<Addon> addons = new LinkedList<>();
        try {
            for (Feature feature : featuresService.listFeatures()) {
                if (feature.getName().startsWith(FeatureInstaller.PREFIX)
                        && List.of(FeatureInstaller.EXTENSION_TYPES).contains(getType(feature.getName()))) {
                    Addon addon = getAddon(feature);
                    // for simple packaging, we filter out all openHAB 1 add-ons as they cannot be used through the UI
                    if (!FeatureInstaller.SIMPLE_PACKAGE.equals(featureInstaller.getCurrentPackage())
                            || !addon.getVersion().startsWith("1.")) {
                        addons.add(addon);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while retrieving features: {}", e.getMessage());
            return Collections.emptyList();
        }

        // let's sort the result alphabetically
        addons.sort(Comparator.comparing(Addon::getLabel));
        return addons;
    }

    @Override
    public Addon getAddon(String id, Locale locale) {
        Feature feature;
        try {
            feature = featuresService.getFeature(FeatureInstaller.PREFIX + id);
            return getAddon(feature);
        } catch (Exception e) {
            logger.error("Exception while querying feature '{}'", id);
            return null;
        }
    }

    private Addon getAddon(Feature feature) {
        String name = getName(feature.getName());
        String type = getType(feature.getName());
        String extId = type + "-" + name;
        String label = feature.getDescription();
        String version = feature.getVersion();
        String link = null;
        switch (type) {
            case FeatureInstaller.EXTENSION_TYPE_AUTOMATION:
                link = "https://www.openhab.org/addons/automation/" + name + "/";
                break;
            case FeatureInstaller.EXTENSION_TYPE_BINDING:
                link = "https://www.openhab.org/addons/bindings/" + name + "/";
                break;
            case FeatureInstaller.EXTENSION_TYPE_MISC:
                // Not possible to define URL
                break;
            case FeatureInstaller.EXTENSION_TYPE_PERSISTENCE:
                link = "https://www.openhab.org/addons/persistence/" + name + "/";
                break;
            case FeatureInstaller.EXTENSION_TYPE_TRANSFORMATION:
                link = "https://www.openhab.org/addons/transformations/" + name + "/";
                break;
            case FeatureInstaller.EXTENSION_TYPE_UI:
                // Not possible to define URL
                break;
            case FeatureInstaller.EXTENSION_TYPE_VOICE:
                link = "https://www.openhab.org/addons/voice/" + name + "/";
                break;
            default:
                break;
        }
        boolean installed = featuresService.isInstalled(feature);
        return new Addon(extId, type, label, version, ADDONS_CONTENTTYPE, link, ADDONS_AUTHOR, true, installed);
    }

    @Override
    public List<AddonType> getTypes(Locale locale) {
        return typeList;
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
    public String getAddonId(URI addonURI) {
        return null;
    }

    private String substringAfter(String str, String separator) {
        int index = str.indexOf(separator);
        return index == -1 ? "" : str.substring(index + separator.length());
    }

    private String substringBefore(String str, String separator) {
        int index = str.indexOf(separator);
        return index == -1 ? str : str.substring(0, index);
    }

    private String getType(String name) {
        return substringBefore(
                name.startsWith(FeatureInstaller.PREFIX) ? name.substring(FeatureInstaller.PREFIX.length()) : name,
                "-");
    }

    private String getName(String name) {
        return substringAfter(
                name.startsWith(FeatureInstaller.PREFIX) ? name.substring(FeatureInstaller.PREFIX.length()) : name,
                "-");
    }
}
