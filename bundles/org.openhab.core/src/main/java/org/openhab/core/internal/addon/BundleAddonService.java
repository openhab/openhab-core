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
package org.openhab.core.internal.addon;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.addon.AddonInfoRegistry;
import org.openhab.core.addon.AddonService;
import org.openhab.core.addon.AddonType;
import org.openhab.core.common.ThreadPoolManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.tracker.BundleTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link BundleAddonService} is a
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = AddonService.class, name = BundleAddonService.SERVICE_NAME)
public class BundleAddonService extends BundleTracker<Bundle> implements AddonService {
    public static final String SERVICE_ID = "file";
    public static final String SERVICE_NAME = "File based add-on service";

    private static final Map<String, AddonType> TAG_ADDON_TYPE_MAP = Map.of( //
            "automation", new AddonType("automation", "Automation"), //
            "binding", new AddonType("binding", "Bindings"), //
            "misc", new AddonType("misc", "Misc"), //
            "persistence", new AddonType("persistence", "Persistence"), //
            "transformation", new AddonType("transformation", "Transformations"), //
            "ui", new AddonType("ui", "User Interfaces"), //
            "voice", new AddonType("voice", "Voice"));
    private static final String ADDON_ID_PREFIX = SERVICE_ID + ":";
    private final Logger logger = LoggerFactory.getLogger(BundleAddonService.class);

    private final AddonInfoRegistry addonInfoRegistry;
    private final ScheduledExecutorService scheduler;

    private final Set<Bundle> trackedBundles = new HashSet<>();
    private Map<String, Addon> addons = Map.of();

    @Activate
    public BundleAddonService(final @Reference AddonInfoRegistry addonInfoRegistry, BundleContext context) {
        super(context, Bundle.ACTIVE, null);

        this.addonInfoRegistry = addonInfoRegistry;
        this.scheduler = ThreadPoolManager.getScheduledPool(ThreadPoolManager.THREAD_POOL_NAME_COMMON);

        open();

        Arrays.stream(context.getBundles()).filter(this::isRelevant).forEach(trackedBundles::add);
        scheduler.execute(this::refreshSource);
    }

    @Deactivate
    public void deactivate() {
        close();
    }

    /**
     * Checks if a bundle is loaded from a file and if add-on information is available
     *
     * @param bundle the bundle to check
     * @return <code>true</code> if bundle is considered, <code>false</code> otherwise
     */
    public boolean isRelevant(Bundle bundle) {
        return bundle.getLocation().startsWith("file:") && bundle.getEntry("OH-INF/binding/binding.xml") != null;
    }

    @Override
    public final synchronized Bundle addingBundle(@NonNullByDefault({}) Bundle bundle,
            @NonNullByDefault({}) BundleEvent event) {
        if (isRelevant(bundle) && trackedBundles.add(bundle)) {
            logger.debug("Added {} to add-on list", bundle.getSymbolicName());
        }

        scheduler.execute(this::refreshSource);
        return bundle;
    }

    @Override
    public final synchronized void modifiedBundle(@NonNullByDefault({}) Bundle bundle, @Nullable BundleEvent event,
            @NonNullByDefault({}) Bundle object) {
        if (isRelevant(bundle)) {
            scheduler.execute(this::refreshSource);
        }
    }

    @Override
    public final synchronized void removedBundle(@NonNullByDefault({}) Bundle bundle,
            @NonNullByDefault({}) BundleEvent event, Bundle object) {
        if (trackedBundles.remove(bundle)) {
            logger.debug("Removed {} from add-on list", bundle.getSymbolicName());
        }
        scheduler.execute(this::refreshSource);
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
    public synchronized void refreshSource() {
        addons = trackedBundles.stream().map(this::toAddon).filter(Objects::nonNull).map(Objects::requireNonNull)
                .collect(Collectors.toMap(Addon::getId, addon -> addon));
    }

    private @Nullable Addon toAddon(Bundle bundle) {
        return addonInfoRegistry.getAddonInfos().stream()
                .filter(info -> bundle.getSymbolicName().equals(info.getSourceBundle())).findAny()
                .map(info -> toAddon(bundle, info)).orElse(null);
    }

    private Addon toAddon(Bundle bundle, AddonInfo addonInfo) {
        String fullId = ADDON_ID_PREFIX + addonInfo.getUID();
        Addon.Builder addon = Addon.create(fullId).withType("binding").withInstalled(true)
                .withVersion(bundle.getVersion().toString()).withLabel(addonInfo.getName())
                .withDescription(Objects.requireNonNullElse(addonInfo.getDescription(), bundle.getSymbolicName()));
        String author = addonInfo.getAuthor();
        if (author != null) {
            addon = addon.withAuthor(author);
        }
        String configDescriptionURI = addonInfo.getConfigDescriptionURI();
        if (configDescriptionURI != null) {
            addon = addon.withConfigDescriptionURI(configDescriptionURI);
        }

        return addon.build();
    }

    @Override
    public List<Addon> getAddons(@Nullable Locale locale) {
        return new ArrayList<>(addons.values());
    }

    @Override
    public @Nullable Addon getAddon(String id, @Nullable Locale locale) {
        return addons.get(ADDON_ID_PREFIX + id);
    }

    @Override
    public List<AddonType> getTypes(@Nullable Locale locale) {
        return new ArrayList<>(TAG_ADDON_TYPE_MAP.values());
    }

    @Override
    public void install(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void uninstall(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable String getAddonId(URI addonURI) {
        return null;
    }
}
