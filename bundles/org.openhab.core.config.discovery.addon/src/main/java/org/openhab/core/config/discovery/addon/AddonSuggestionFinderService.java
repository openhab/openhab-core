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
package org.openhab.core.config.discovery.addon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.jupnp.UpnpService;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.addon.AddonInfoProvider;
import org.openhab.core.addon.AddonService;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.config.discovery.addon.finders.AddonSuggestionFinder;
import org.openhab.core.config.discovery.addon.finders.MDNSAddonSuggestionFinder;
import org.openhab.core.config.discovery.addon.finders.UpnpAddonSuggestionFinder;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.io.transport.mdns.MDNSClient;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * This is a {@link AddonSuggestionFinderService} which discovers suggested
 * addons for the user to install.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = AddonSuggestionFinderService.class, name = AddonSuggestionFinderService.SERVICE_NAME)
public class AddonSuggestionFinderService implements AutoCloseable {

    public static final String SERVICE_NAME = "suggested-addon-finder";

    private final Set<AddonService> addonServices = ConcurrentHashMap.newKeySet();
    private final Set<AddonInfoProvider> addonInfoProviders = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool(SERVICE_NAME);
    private final List<AddonSuggestionFinder> finders = Collections.synchronizedList(new ArrayList<>());
    private final List<Future<?>> finderTasks = Collections.synchronizedList(new ArrayList<>());
    private final LocaleProvider localeProvider;

    @Activate
    public AddonSuggestionFinderService(@Reference LocaleProvider localeProvider, @Reference MDNSClient mdnsClient,
            @Reference UpnpService upnpService) {
        this.localeProvider = localeProvider;
        finders.add(new MDNSAddonSuggestionFinder(mdnsClient));
        finders.add(new UpnpAddonSuggestionFinder(upnpService));
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addAddonInfoProvider(AddonInfoProvider addonInfoProvider) {
        if (!addonInfoProviders.contains(addonInfoProvider)) {
            addonInfoProviders.add(addonInfoProvider);
            scanStart();
        }
    }

    public void removeAddonInfoProvider(AddonInfoProvider addonInfoProvider) {
        addonInfoProviders.remove(addonInfoProvider);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addAddonService(AddonService addonService) {
        addonServices.add(addonService);
    }

    public void removeAddonService(AddonService addonService) {
        addonServices.remove(addonService);
    }

    @Deactivate
    @Override
    public void close() throws Exception {
        finderTasks.forEach(t -> t.cancel(true));
        finderTasks.clear();
        finders.forEach(f -> f.reset());
        finders.clear();
    }

    public List<Addon> getSuggestedAddons(@Nullable Locale locale) {
        Set<String> uids = finders.stream().map(f -> f.getAddonSuggestionUIDs()).flatMap(Collection::stream)
                .collect(Collectors.toSet());

        return addonServices.stream().map(s -> s.getAddons(locale)).flatMap(Collection::stream)
                .filter(a -> uids.contains(a.getUid())).toList();
    }

    public boolean scanDone() {
        return finders.stream().allMatch(f -> f.scanDone());
    }

    private void scanStart() {
        finderTasks.forEach(t -> t.cancel(false));
        finderTasks.clear();

        List<AddonInfo> candidates = addonInfoProviders.stream().map(p -> p.getAddonInfos(localeProvider.getLocale()))
                .flatMap(Collection::stream).toList();

        finders.forEach(f -> {
            f.setAddonCandidates(candidates);
            finderTasks.add(scheduler.submit(() -> f.scanTask()));
        });
    }
}
