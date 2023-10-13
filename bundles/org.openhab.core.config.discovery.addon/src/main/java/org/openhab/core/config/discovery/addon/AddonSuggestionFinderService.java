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
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.addon.AddonInfoProvider;
import org.openhab.core.addon.AddonService;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.config.discovery.addon.finders.AddonSuggestionFinder;
import org.openhab.core.i18n.LocaleProvider;
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

    public static final String SERVICE_NAME = "addon-suggestion-finder-service";

    private final ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool(SERVICE_NAME);
    private final Set<AddonInfoProvider> addonInfoProviders = ConcurrentHashMap.newKeySet();
    private final Set<AddonService> addonServices = ConcurrentHashMap.newKeySet();
    private final List<AddonSuggestionFinder> addonSuggestionFinders = Collections.synchronizedList(new ArrayList<>());
    private final List<Future<?>> addonSuggestionFinderTasks = Collections.synchronizedList(new ArrayList<>());
    private final LocaleProvider localeProvider;

    @Activate
    public AddonSuggestionFinderService(@Reference LocaleProvider localeProvider) {
        this.localeProvider = localeProvider;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addAddonInfoProvider(AddonInfoProvider addonInfoProvider) {
        if (!addonInfoProviders.contains(addonInfoProvider)) {
            addonInfoProviders.add(addonInfoProvider);
            scanStart();
        }
    }

    public void removeAddonInfoProvider(AddonInfoProvider addonInfoProvider) {
        if (addonInfoProviders.contains(addonInfoProvider)) {
            addonInfoProviders.remove(addonInfoProvider);
            scanStart();
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addAddonSuggestionFinder(AddonSuggestionFinder addonSuggestionFinder) {
        if (!addonSuggestionFinders.contains(addonSuggestionFinder)) {
            addonSuggestionFinders.add(addonSuggestionFinder);
            scanStart();
        }
    }

    public void removeAddonSuggestionFinder(AddonSuggestionFinder addonSuggestionFinder) {
        if (addonSuggestionFinders.contains(addonSuggestionFinder)) {
            addonSuggestionFinders.remove(addonSuggestionFinder);
            scanStart();
        }
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
        addonSuggestionFinderTasks.forEach(t -> t.cancel(true));
        addonSuggestionFinderTasks.clear();
        addonSuggestionFinders.forEach(f -> f.reset());
        addonSuggestionFinders.clear();
        addonInfoProviders.clear();
        addonServices.clear();
    }

    public List<Addon> getSuggestedAddons(@Nullable Locale locale) {
        Set<String> uids = addonSuggestionFinders.stream().map(f -> f.getAddonSuggestionUIDs())
                .flatMap(Collection::stream).collect(Collectors.toSet());

        return addonServices.stream().map(s -> s.getAddons(locale)).flatMap(Collection::stream)
                .filter(a -> uids.contains(a.getUid())).toList();
    }

    public boolean scanDone() {
        return addonSuggestionFinders.stream().allMatch(f -> f.scanDone());
    }

    private void scanStart() {
        addonSuggestionFinderTasks.forEach(t -> t.cancel(false));
        addonSuggestionFinderTasks.clear();

        List<AddonInfo> candidates = addonInfoProviders.stream().map(p -> p.getAddonInfos(localeProvider.getLocale()))
                .flatMap(Collection::stream).toList();

        addonSuggestionFinders.forEach(f -> {
            f.setAddonCandidates(candidates);
            addonSuggestionFinderTasks.add(scheduler.submit(() -> f.scanTask()));
        });
    }
}
