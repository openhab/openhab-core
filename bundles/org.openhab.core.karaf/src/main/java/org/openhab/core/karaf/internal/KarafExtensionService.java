/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.openhab.core.extension.Extension;
import org.openhab.core.extension.ExtensionService;
import org.openhab.core.extension.ExtensionType;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service is an implementation of an openHAB {@link ExtensionService} using the Karaf features service. This
 * exposes all openHAB add-ons through the REST API and allows UIs to dynamically install and uninstall them.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@Component(name = "org.openhab.core.karafextension")
public class KarafExtensionService implements ExtensionService {

    private final Logger logger = LoggerFactory.getLogger(KarafExtensionService.class);

    private final List<ExtensionType> typeList = new ArrayList<>(FeatureInstaller.EXTENSION_TYPES.length);

    private final FeaturesService featuresService;
    private final FeatureInstaller featureInstaller;

    @Activate
    public KarafExtensionService(final @Reference FeatureInstaller featureInstaller,
            final @Reference FeaturesService featuresService) {
        this.featureInstaller = featureInstaller;
        this.featuresService = featuresService;
        typeList.add(new ExtensionType(FeatureInstaller.EXTENSION_TYPE_BINDING, "Bindings"));
        typeList.add(new ExtensionType(FeatureInstaller.EXTENSION_TYPE_MISC, "Misc"));
        typeList.add(new ExtensionType(FeatureInstaller.EXTENSION_TYPE_VOICE, "Voice"));
        if (!FeatureInstaller.SIMPLE_PACKAGE.equals(featureInstaller.getCurrentPackage())) {
            typeList.add(new ExtensionType(FeatureInstaller.EXTENSION_TYPE_ACTION, "Actions"));
            typeList.add(new ExtensionType(FeatureInstaller.EXTENSION_TYPE_PERSISTENCE, "Persistence"));
            typeList.add(new ExtensionType(FeatureInstaller.EXTENSION_TYPE_TRANSFORMATION, "Transformations"));
            typeList.add(new ExtensionType(FeatureInstaller.EXTENSION_TYPE_UI, "User Interfaces"));
        }
    }

    @Override
    public List<Extension> getExtensions(Locale locale) {
        List<Extension> extensions = new LinkedList<>();
        try {
            for (Feature feature : featuresService.listFeatures()) {
                if (feature.getName().startsWith(FeatureInstaller.PREFIX)
                        && Arrays.asList(FeatureInstaller.EXTENSION_TYPES).contains(getType(feature.getName()))) {
                    Extension extension = getExtension(feature);
                    // for simple packaging, we filter out all openHAB 1 add-ons as they cannot be used through the UI
                    if (!FeatureInstaller.SIMPLE_PACKAGE.equals(featureInstaller.getCurrentPackage())
                            || !extension.getVersion().startsWith("1.")) {
                        extensions.add(extension);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while retrieving features: {}", e.getMessage());
            return Collections.emptyList();
        }

        // let's sort the result alphabetically
        Collections.sort(extensions, new Comparator<Extension>() {
            @Override
            public int compare(Extension ext1, Extension ext2) {
                return ext1.getLabel().compareTo(ext2.getLabel());
            }
        });
        return extensions;
    }

    @Override
    public Extension getExtension(String id, Locale locale) {
        Feature feature;
        try {
            feature = featuresService.getFeature(FeatureInstaller.PREFIX + id);
            return getExtension(feature);
        } catch (Exception e) {
            logger.error("Exception while querying feature '{}'", id);
            return null;
        }
    }

    private Extension getExtension(Feature feature) {
        String name = getName(feature.getName());
        String type = getType(feature.getName());
        String extId = type + "-" + name;
        String label = feature.getDescription();
        String version = feature.getVersion();
        String link = null;
        switch (type) {
            case FeatureInstaller.EXTENSION_TYPE_ACTION:
                link = "https://www.openhab.org/addons/actions/" + name + "/";
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
        return new Extension(extId, type, label, version, link, installed);
    }

    @Override
    public List<ExtensionType> getTypes(Locale locale) {
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
    public String getExtensionId(URI extensionURI) {
        return null;
    }

    private String getType(String name) {
        return StringUtils.substringBefore(
                name.startsWith(FeatureInstaller.PREFIX) ? name.substring(FeatureInstaller.PREFIX.length()) : name,
                "-");
    }

    private String getName(String name) {
        return StringUtils.substringAfter(
                name.startsWith(FeatureInstaller.PREFIX) ? name.substring(FeatureInstaller.PREFIX.length()) : name,
                "-");
    }
}
