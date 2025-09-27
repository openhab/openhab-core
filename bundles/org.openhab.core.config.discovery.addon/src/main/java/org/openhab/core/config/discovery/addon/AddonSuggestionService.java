/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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

import static org.openhab.core.config.discovery.addon.AddonFinderConstants.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.OpenHAB;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.addon.AddonInfoProvider;
import org.openhab.core.config.core.ConfigParser;
import org.openhab.core.i18n.LocaleProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a {@link AddonSuggestionService} which discovers suggested add-ons for the user to install.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 * @author Mark Herwege - Install/remove finders
 */
@NonNullByDefault
@Component(immediate = true, service = AddonSuggestionService.class, name = AddonSuggestionService.SERVICE_NAME, configurationPid = OpenHAB.ADDONS_SERVICE_PID)
public class AddonSuggestionService {

    public static final String SERVICE_NAME = "addon-suggestion-service";

    private final Logger logger = LoggerFactory.getLogger(AddonSuggestionService.class);

    private final Set<AddonInfoProvider> addonInfoProviders = ConcurrentHashMap.newKeySet();

    // All access must be guarded by "addonFinders"
    private final List<AddonFinder> addonFinders = new ArrayList<>();
    private final LocaleProvider localeProvider;
    private volatile @Nullable AddonFinderService addonFinderService;
    private final Map<String, Boolean> baseFinderConfig = new ConcurrentHashMap<>();

    @Activate
    public AddonSuggestionService(@Reference LocaleProvider localeProvider, Map<String, Object> config) {
        this.localeProvider = localeProvider;
        SUGGESTION_FINDERS.forEach(f -> baseFinderConfig.put(f, false));
        modified(config);
    }

    @Deactivate
    public void deactivate() {
        synchronized (addonFinders) {
            addonFinders.clear();
        }
        addonInfoProviders.clear();
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void addAddonFinderService(AddonFinderService addonFinderService) {
        this.addonFinderService = addonFinderService;
        initAddonFinderService();
    }

    protected void removeAddonFinderService(AddonFinderService addonFinderService) {
        AddonFinderService finderService = this.addonFinderService;
        if ((finderService != null) && addonFinderService.getClass().isAssignableFrom(finderService.getClass())) {
            this.addonFinderService = null;
        }
    }

    @Modified
    public void modified(@Nullable final Map<String, Object> config) {
        if (config != null) {
            AddonFinderService finderService = addonFinderService;
            baseFinderConfig.forEach((finder, currentEnabled) -> {
                String cfgParam = SUGGESTION_FINDER_CONFIGS.get(finder);
                if (cfgParam != null) {
                    boolean newEnabled = ConfigParser.valueAsOrElse(config.get(cfgParam), Boolean.class, true);
                    if (currentEnabled != newEnabled) {
                        String type = SUGGESTION_FINDER_TYPES.get(finder);
                        if (type != null) {
                            logger.debug("baseFinderConfig {} {} = {} => updating from {} to {}", finder, cfgParam,
                                    config.get(cfgParam), currentEnabled, newEnabled);
                            baseFinderConfig.put(finder, newEnabled);
                            if (finderService != null) {
                                if (newEnabled) {
                                    finderService.install(type);
                                } else {
                                    finderService.uninstall(type);
                                }
                            }
                        } else {
                            logger.warn("Failed to resolve addon suggestion finder type for suggestion finder {}",
                                    finder);
                        }
                    }
                }
            });
        }
    }

    private void initAddonFinderService() {
        AddonFinderService finderService = this.addonFinderService;
        if (finderService == null) {
            return;
        }

        String type;
        for (Entry<String, Boolean> entry : baseFinderConfig.entrySet()) {
            type = SUGGESTION_FINDER_TYPES.get(entry.getKey());
            if (type != null) {
                if (entry.getValue() instanceof Boolean enabled) {
                    if (enabled) {
                        finderService.install(type);
                    } else {
                        finderService.uninstall(type);
                    }
                }
            } else {
                logger.warn("Failed to resolve addon suggestion finder type for suggestion finder {}", entry.getKey());
            }
        }
    }

    private boolean isFinderEnabled(AddonFinder finder) {
        if (finder instanceof BaseAddonFinder baseFinder) {
            return baseFinderConfig.getOrDefault(baseFinder.getServiceName(), true);
        }
        return true;
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
    public void addAddonFinder(AddonFinder addonFinder) {
        synchronized (addonFinders) {
            addonFinders.add(addonFinder);
        }
        changed();
    }

    public void removeAddonFinder(AddonFinder addonFinder) {
        synchronized (addonFinders) {
            addonFinders.remove(addonFinder);
        }
    }

    private void changed() {
        Locale locale = localeProvider.getLocale();
        List<AddonInfo> candidates = addonInfoProviders.stream().map(p -> p.getAddonInfos(locale))
                .flatMap(Collection::stream).toList();
        synchronized (addonFinders) {
            addonFinders.stream().filter(this::isFinderEnabled).forEach(f -> f.setAddonCandidates(candidates));
        }
    }

    public Set<AddonInfo> getSuggestedAddons(@Nullable Locale locale) {
        synchronized (addonFinders) {
            return addonFinders.stream().filter(this::isFinderEnabled).map(f -> f.getSuggestedAddons())
                    .flatMap(Collection::stream).collect(Collectors.toSet());
        }
    }
}
