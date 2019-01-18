/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
import org.eclipse.smarthome.core.extension.Extension;
import org.eclipse.smarthome.core.extension.ExtensionService;
import org.eclipse.smarthome.core.extension.ExtensionType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service is an implementation of an ESH {@link ExtensionService} using the Karaf
 * features service. This exposes all openHAB addons through the rest api and allows
 * UIs to dynamically install and uninstall them.
 *
 * @author Kai Kreuzer
 *
 */
@Component(name = "org.openhab.core.karafextension")
public class KarafExtensionService implements ExtensionService {

    private final Logger logger = LoggerFactory.getLogger(KarafExtensionService.class);

    private FeaturesService featuresService;
    private FeatureInstaller featureInstaller;

    @Reference
    protected void setFeatureInstaller(FeatureInstaller featureInstaller) {
        this.featureInstaller = featureInstaller;
    }

    protected void unsetFeatureInstaller(FeatureInstaller featureInstaller) {
        this.featureInstaller = null;
    }

    @Reference
    protected void setFeaturesService(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }

    protected void unsetFeaturesService(FeaturesService featureService) {
        this.featuresService = null;
    }

    @Override
    public List<Extension> getExtensions(Locale locale) {
        List<Extension> extensions = new LinkedList<>();
        try {
            for (Feature feature : featuresService.listFeatures()) {
                if (feature.getName().startsWith(FeatureInstaller.PREFIX)
                        && Arrays.asList(FeatureInstaller.addonTypes).contains(getType(feature.getName()))) {
                    Extension extension = getExtension(feature);
                    // for simple packaging, we filter out all openHAB 1 add-ons as they cannot be used through the UI
                    if (!"simple".equals(featureInstaller.getCurrentPackage())
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
        if (type.equals("binding")) {
            link = "https://www.openhab.org/addons/bindings/" + name + "/";
        } else if (type.equals("action")) {
            link = "https://www.openhab.org/addons/actions/" + name + "/";
        } else if (type.equals("persistence")) {
            link = "https://www.openhab.org/addons/persistence/" + name + "/";
        }
        boolean installed = featuresService.isInstalled(feature);
        return new Extension(extId, type, label, version, link, installed);
    }

    @Override
    public List<ExtensionType> getTypes(Locale locale) {
        List<ExtensionType> typeList = new ArrayList<>(6);
        typeList.add(new ExtensionType("binding", "Bindings"));
        if (!"simple".equals(featureInstaller.getCurrentPackage())) {
            typeList.add(new ExtensionType("ui", "User Interfaces"));
            typeList.add(new ExtensionType("persistence", "Persistence"));
            typeList.add(new ExtensionType("action", "Actions"));
            typeList.add(new ExtensionType("transformation", "Transformations"));
        }
        typeList.add(new ExtensionType("voice", "Voice"));
        typeList.add(new ExtensionType("misc", "Misc"));
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
        if (name.startsWith(FeatureInstaller.PREFIX)) {
            name = name.substring(FeatureInstaller.PREFIX.length());
            return StringUtils.substringBefore(name, "-");
        }
        return StringUtils.substringBefore(name, "-");
    }

    private String getName(String name) {
        if (name.startsWith(FeatureInstaller.PREFIX)) {
            name = name.substring(FeatureInstaller.PREFIX.length());
        }
        return StringUtils.substringAfter(name, "-");
    }
}
