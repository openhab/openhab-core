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

import static org.openhab.core.config.discovery.addon.AddonFinderConstants.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.addon.AddonInfoProvider;
import org.openhab.core.common.NamedThreadFactory;
import org.openhab.core.config.core.ConfigParser;
import org.openhab.core.i18n.LocaleProvider;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

/**
 * This is a {@link AddonSuggestionService} which discovers suggested Addons for the user to install.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 * @author Mark Herwege - Install/remove finders
 */
@NonNullByDefault
@Component(immediate = true, service = AddonSuggestionService.class, name = AddonSuggestionService.SERVICE_NAME, configurationPid = AddonSuggestionService.CONFIG_PID)
public class AddonSuggestionService implements AutoCloseable {

    public static final String SERVICE_NAME = "addon-suggestion-service";
    public static final String CONFIG_PID = "org.openhab.addons";

    private final Set<AddonInfoProvider> addonInfoProviders = ConcurrentHashMap.newKeySet();
    private final List<AddonFinder> addonFinders = Collections.synchronizedList(new ArrayList<>());
    private final ConfigurationAdmin configurationAdmin;
    private final LocaleProvider localeProvider;
    private @Nullable AddonFinderService addonFinderService;
    private @Nullable Map<String, Object> config;
    private final ScheduledExecutorService scheduler;

    private Map<String, Boolean> baseFinderConfig = new ConcurrentHashMap<>();

    @Activate
    public AddonSuggestionService(final @Reference ConfigurationAdmin configurationAdmin,
            @Reference LocaleProvider localeProvider, @Nullable Map<String, Object> config) {
        this.configurationAdmin = configurationAdmin;
        this.localeProvider = localeProvider;

        SUGGESTION_FINDERS.forEach(f -> baseFinderConfig.put(f, true));
        modified(config);
        changed();

        // Changes to the configuration are expected to call the {@link Modified} method. This works well when running
        // in Eclipse. Running in Karaf, the method was not consistently called. Therefore regularly check for changes
        // in configuration.
        // This pattern and code was re-used from {@link org.openhab.core.karaf.internal.FeatureInstaller}
        scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("addon-suggestion-finder"));
        scheduler.scheduleWithFixedDelay(this::syncConfiguration, 1, 1, TimeUnit.MINUTES);
    }

    @Deactivate
    protected void deactivate() {
        scheduler.shutdown();
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void addAddonFinderService(AddonFinderService addonFinderService) {
        this.addonFinderService = addonFinderService;
        modified(config);
    }

    protected void removeAddonFinderService(AddonFinderService addonFinderService) {
        AddonFinderService finderService = this.addonFinderService;
        if ((finderService != null) && addonFinderService.getClass().isAssignableFrom(finderService.getClass())) {
            this.addonFinderService = null;
        }
    }

    @Modified
    public void modified(@Nullable final Map<String, Object> config) {
        baseFinderConfig.forEach((finder, cfg) -> {
            String cfgParam = SUGGESTION_FINDER_CONFIGS.get(finder);
            if (cfgParam != null) {
                boolean enabled = (config != null)
                        ? ConfigParser.valueAsOrElse(config.get(cfgParam), Boolean.class, cfg)
                        : cfg;
                baseFinderConfig.put(finder, enabled);
                String feature = SUGGESTION_FINDER_FEATURES.get(finder);
                AddonFinderService finderService = addonFinderService;
                if (feature != null && finderService != null) {
                    if (enabled) {
                        finderService.install(feature);
                    } else {
                        finderService.uninstall(feature);
                    }
                }
            }
        });
        this.config = config;
    }

    private void syncConfiguration() {
        try {
            Dictionary<String, Object> cfg = configurationAdmin.getConfiguration(CONFIG_PID).getProperties();
            if (cfg == null) {
                return;
            }
            final Map<String, Object> cfgMap = new HashMap<>();
            final Enumeration<String> enumeration = cfg.keys();
            while (enumeration.hasMoreElements()) {
                final String key = enumeration.nextElement();
                cfgMap.put(key, cfg.get(key));
            }
            if (!cfgMap.equals(config)) {
                modified(cfgMap);
            }
        } catch (IOException e) {
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
        addonFinders.add(addonFinder);
        changed();
    }

    public void removeAddonFinder(AddonFinder addonFinder) {
        if (addonFinders.remove(addonFinder)) {
            changed();
        }
    }

    private void changed() {
        List<AddonInfo> candidates = addonInfoProviders.stream().map(p -> p.getAddonInfos(localeProvider.getLocale()))
                .flatMap(Collection::stream).toList();
        addonFinders.stream().filter(this::isFinderEnabled).forEach(f -> f.setAddonCandidates(candidates));
    }

    @Deactivate
    @Override
    public void close() throws Exception {
        addonFinders.clear();
        addonInfoProviders.clear();
    }

    public Set<AddonInfo> getSuggestedAddons(@Nullable Locale locale) {
        return addonFinders.stream().filter(this::isFinderEnabled).map(f -> f.getSuggestedAddons())
                .flatMap(Collection::stream).collect(Collectors.toSet());
    }
}
