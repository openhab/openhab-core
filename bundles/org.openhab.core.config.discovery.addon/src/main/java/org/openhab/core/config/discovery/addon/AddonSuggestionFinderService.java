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
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.addon.AddonInfoProvider;
import org.openhab.core.config.discovery.addon.finders.AddonSuggestionFinder;
import org.openhab.core.i18n.LocaleProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * This is a {@link AddonSuggestionFinderService} which discovers suggested Addons for the user to install.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = AddonSuggestionFinderService.class, name = AddonSuggestionFinderService.SERVICE_NAME)
public class AddonSuggestionFinderService implements AutoCloseable {

    public static final String SERVICE_NAME = "addon-suggestion-finder-service";

    private final Set<AddonInfoProvider> addonInfoProviders = ConcurrentHashMap.newKeySet();
    private final List<AddonSuggestionFinder> addonSuggestionFinders = Collections.synchronizedList(new ArrayList<>());
    private final LocaleProvider localeProvider;

    @Activate
    public AddonSuggestionFinderService(@Reference LocaleProvider localeProvider) {
        this.localeProvider = localeProvider;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addAddonInfoProvider(AddonInfoProvider addonInfoProvider) {
        addonInfoProviders.add(addonInfoProvider);
        changed();
    }

    public void removeAddonInfoProvider(AddonInfoProvider addonInfoProvider) {
        if (addonInfoProviders.remove(addonInfoProvider)) {
            changed();
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addAddonSuggestionFinder(AddonSuggestionFinder addonSuggestionFinder) {
        addonSuggestionFinders.add(addonSuggestionFinder);
        changed();
    }

    public void removeAddonSuggestionFinder(AddonSuggestionFinder addonSuggestionFinder) {
        if (addonSuggestionFinders.remove(addonSuggestionFinder)) {
            changed();
        }
    }

    private void changed() {
        List<AddonInfo> candidates = addonInfoProviders.stream().map(p -> p.getAddonInfos(localeProvider.getLocale()))
                .flatMap(Collection::stream).toList();
        addonSuggestionFinders.forEach(f -> f.setAddonCandidates(candidates));
    }

    @Deactivate
    @Override
    public void close() throws Exception {
        addonSuggestionFinders.clear();
        addonInfoProviders.clear();
    }

    /**
     * The OH framework calls this method to enable/disable the finder with the given service id (if any).
     * 
     * @param finderServiceId the service id of the finder to be enabled/disabled.
     * @param enable true to enable, false to disable
     */
    public void enable(String finderServiceId, boolean enable) {
        addonSuggestionFinders.stream().filter(f -> finderServiceId.equals(f.getServiceType())).findFirst()
                .ifPresent(f -> f.enable(enable));
    }

    public Set<AddonInfo> getSuggestedAddons(@Nullable Locale locale) {
        return addonSuggestionFinders.stream().map(f -> f.getSuggestedAddons()).flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }
}
