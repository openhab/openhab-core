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
package org.openhab.core.addon.internal;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
 * The {@link JarFileAddonService} is an add-on service that provides add-ons that are placed a .jar files in the
 * openHAB addons folder
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = AddonService.class, name = JarFileAddonService.SERVICE_NAME)
public class JarFileAddonService extends BundleTracker<Bundle> implements AddonService {
    public static final String SERVICE_ID = "jar";
    public static final String SERVICE_NAME = "JAR-File add-on service";

    private static final Map<String, AddonType> ADDON_TYPE_MAP = Map.of( //
            "automation", new AddonType("automation", "Automation"), //
            "binding", new AddonType("binding", "Bindings"), //
            "misc", new AddonType("misc", "Misc"), //
            "persistence", new AddonType("persistence", "Persistence"), //
            "transformation", new AddonType("transformation", "Transformations"), //
            "ui", new AddonType("ui", "User Interfaces"), //
            "voice", new AddonType("voice", "Voice"));
    private static final String ADDON_ID_PREFIX = SERVICE_ID + ":";
    private final Logger logger = LoggerFactory.getLogger(JarFileAddonService.class);

    private final AddonInfoRegistry addonInfoRegistry;
    private final ScheduledExecutorService scheduler;

    private final Set<Bundle> trackedBundles = ConcurrentHashMap.newKeySet();
    private Map<String, Addon> addons = Map.of();

    @Activate
    public JarFileAddonService(final @Reference AddonInfoRegistry addonInfoRegistry, BundleContext context) {
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
     * Checks if a bundle is loaded from a file and add-on information is available
     *
     * @param bundle the bundle to check
     * @return <code>true</code> if bundle is considered, <code>false</code> otherwise
     */
    public boolean isRelevant(Bundle bundle) {
        return bundle.getLocation().startsWith("file:") && bundle.getEntry("OH-INF/addon/addon.xml") != null;
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
                .collect(Collectors.toMap(Addon::getUid, addon -> addon));
    }

    private @Nullable Addon toAddon(Bundle bundle) {
        return addonInfoRegistry.getAddonInfos().stream()
                .filter(info -> bundle.getSymbolicName().equals(info.getSourceBundle())).findAny()
                .map(info -> toAddon(bundle, info)).orElse(null);
    }

    private Addon toAddon(Bundle bundle, AddonInfo addonInfo) {
        String uid = ADDON_ID_PREFIX + addonInfo.getUID();
        return Addon.create(uid).withId(addonInfo.getId()).withType(addonInfo.getType()).withInstalled(true)
                .withVersion(bundle.getVersion().toString()).withLabel(addonInfo.getName())
                .withConfigDescriptionURI(addonInfo.getConfigDescriptionURI())
                .withDescription(Objects.requireNonNullElse(addonInfo.getDescription(), bundle.getSymbolicName()))
                .build();
    }

    @Override
    public List<Addon> getAddons(@Nullable Locale locale) {
        if (trackedBundles.size() != addons.size()) {
            refreshSource();
        }
        return List.copyOf(addons.values());
    }

    @Override
    public @Nullable Addon getAddon(String id, @Nullable Locale locale) {
        return addons.get(id);
    }

    @Override
    public List<AddonType> getTypes(@Nullable Locale locale) {
        return List.copyOf(ADDON_TYPE_MAP.values());
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
