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
package org.openhab.core.addon.eclipse.internal;

import static java.util.Map.entry;
import static org.openhab.core.addon.AddonType.*;

import java.net.URI;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.addon.AddonInfoRegistry;
import org.openhab.core.addon.AddonService;
import org.openhab.core.addon.AddonType;
import org.openhab.core.config.core.ConfigurableService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * This is an implementation of an {@link AddonService} that can be used when debugging in Eclipse.
 *
 * @author Wouter Born - Initial contribution
 */
@Component(name = "org.openhab.addons")
@NonNullByDefault
@ConfigurableService(category = "system", label = "Add-on Management", description_uri = EclipseAddonService.CONFIG_URI)
public class EclipseAddonService implements AddonService {

    public static final String CONFIG_URI = "system:addons";

    private static final String SERVICE_ID = "eclipse";
    private static final String ADDON_ID_PREFIX = SERVICE_ID + ":";

    private static final String ADDONS_CONTENT_TYPE = "application/vnd.openhab.bundle";
    private static final String ADDONS_AUTHOR = "openHAB";
    private static final String BUNDLE_SYMBOLIC_NAME_PREFIX = "org.openhab.";

    private static final Map<String, String> ADDON_BUNDLE_TYPE_MAP = Map.ofEntries(
            entry(AUTOMATION.getId(), "automation"), //
            entry(BINDING.getId(), "binding"), //
            entry(MISC.getId(), "io"), //
            entry(PERSISTENCE.getId(), "persistence"), //
            entry(TRANSFORMATION.getId(), "transform"), //
            entry(UI.getId(), "ui"), //
            entry(VOICE.getId(), "voice"));

    private static final Map<String, String> BUNDLE_ADDON_TYPE_MAP = ADDON_BUNDLE_TYPE_MAP.entrySet().stream()
            .collect(Collectors.toMap(Entry::getValue, Entry::getKey));

    private static final String DOCUMENTATION_URL_PREFIX = "https://www.openhab.org/addons/";

    private static final Map<String, String> DOCUMENTATION_URL_FORMATS = Map.ofEntries(
            entry(AUTOMATION.getId(), DOCUMENTATION_URL_PREFIX + "automation/%s/"), //
            entry(BINDING.getId(), DOCUMENTATION_URL_PREFIX + "bindings/%s/"), //
            entry(MISC.getId(), DOCUMENTATION_URL_PREFIX + "integrations/%s/"), //
            entry(PERSISTENCE.getId(), DOCUMENTATION_URL_PREFIX + "persistence/%s/"), //
            entry(TRANSFORMATION.getId(), DOCUMENTATION_URL_PREFIX + "transformations/%s/"), //
            entry(UI.getId(), DOCUMENTATION_URL_PREFIX + "ui/%s/"), //
            entry(VOICE.getId(), DOCUMENTATION_URL_PREFIX + "voice/%s/"));

    private final BundleContext bundleContext;
    private final AddonInfoRegistry addonInfoRegistry;

    @Activate
    public EclipseAddonService(BundleContext bundleContext, @Reference AddonInfoRegistry addonInfoRegistry) {
        this.bundleContext = bundleContext;
        this.addonInfoRegistry = addonInfoRegistry;
    }

    @Deactivate
    protected void deactivate() {
    }

    @Override
    public String getId() {
        return SERVICE_ID;
    }

    @Override
    public String getName() {
        return "Eclipse Add-on Service";
    }

    @Override
    public void refreshSource() {
    }

    @Override
    public void install(String id) {
        throw new UnsupportedOperationException(getName() + " does not support installing add-ons");
    }

    @Override
    public void uninstall(String id) {
        throw new UnsupportedOperationException(getName() + " does not support uninstalling add-ons");
    }

    private boolean isAddon(Bundle bundle) {
        String symbolicName = bundle.getSymbolicName();
        String[] segments = symbolicName.split("\\.");
        return symbolicName.startsWith(BUNDLE_SYMBOLIC_NAME_PREFIX) && bundle.getState() == Bundle.ACTIVE
                && segments.length >= 4 && ADDON_BUNDLE_TYPE_MAP.containsValue(segments[2]);
    }

    private Addon getAddon(Bundle bundle, @Nullable Locale locale) {
        String symbolicName = bundle.getSymbolicName();
        String[] segments = symbolicName.split("\\.");

        String type = Objects.requireNonNull(BUNDLE_ADDON_TYPE_MAP.get(segments[2]));
        String name = segments[3];

        String uid = type + Addon.ADDON_SEPARATOR + name;

        Addon.Builder addon = Addon.create(ADDON_ID_PREFIX + uid).withType(type).withId(name)
                .withContentType(ADDONS_CONTENT_TYPE).withVersion(bundle.getVersion().toString())
                .withAuthor(ADDONS_AUTHOR, true).withInstalled(true);

        AddonInfo addonInfo = addonInfoRegistry.getAddonInfo(uid, locale);

        if (addonInfo != null) {
            // only enrich if this add-on is installed, otherwise wrong data might be added
            addon = addon.withLabel(addonInfo.getName()).withDescription(addonInfo.getDescription())
                    .withCountries(addonInfo.getCountries()).withLink(getDefaultDocumentationLink(type, name))
                    .withConfigDescriptionURI(addonInfo.getConfigDescriptionURI());
        } else {
            addon = addon.withLabel(name).withLink(getDefaultDocumentationLink(type, name));
        }

        addon.withLoggerPackages(List.of(symbolicName));

        return addon.build();
    }

    @Override
    public List<Addon> getAddons(@Nullable Locale locale) {
        return Arrays.stream(bundleContext.getBundles()) //
                .filter(this::isAddon) //
                .map(bundle -> getAddon(bundle, locale)) //
                .sorted(Comparator.comparing(Addon::getLabel)) //
                .toList();
    }

    private @Nullable String getDefaultDocumentationLink(String type, String name) {
        String format = DOCUMENTATION_URL_FORMATS.get(type);
        return format == null ? null : String.format(format, name);
    }

    @Override
    public @Nullable Addon getAddon(String uid, @Nullable Locale locale) {
        String id = uid.replaceFirst(ADDON_ID_PREFIX, "");
        String[] segments = id.split(Addon.ADDON_SEPARATOR);
        String symbolicName = BUNDLE_SYMBOLIC_NAME_PREFIX + ADDON_BUNDLE_TYPE_MAP.get(segments[0]) + "." + segments[1];
        return Arrays.stream(bundleContext.getBundles()) //
                .filter(bundle -> bundle.getSymbolicName().equals(symbolicName)) //
                .filter(this::isAddon) //
                .map(bundle -> getAddon(bundle, locale)) //
                .findFirst().orElse(null);
    }

    @Override
    public List<AddonType> getTypes(@Nullable Locale locale) {
        return AddonType.DEFAULT_TYPES;
    }

    @Override
    public @Nullable String getAddonId(URI extensionURI) {
        return null;
    }
}
