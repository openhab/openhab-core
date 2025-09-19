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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.addon.AddonInfoProvider;
import org.openhab.core.common.ThreadPoolManager;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a {@link AddonSuggestionService} which discovers suggested add-ons for the user to install.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 * @author Mark Herwege - Install/remove finders
 */
@NonNullByDefault
@Component(immediate = true, service = AddonSuggestionService.class, name = AddonSuggestionService.SERVICE_NAME, configurationPid = AddonSuggestionService.CONFIG_PID)
public class AddonSuggestionService implements AutoCloseable {

    public static final String SERVICE_NAME = "addon-suggestion-service";
    public static final String CONFIG_PID = "org.openhab.addons";

    private final Logger logger = LoggerFactory.getLogger(AddonSuggestionService.class);

    private final Set<AddonInfoProvider> addonInfoProviders = ConcurrentHashMap.newKeySet();
    private final List<AddonFinder> addonFinders = Collections.synchronizedList(new ArrayList<>());
    private final ConfigurationAdmin configurationAdmin;
    private final LocaleProvider localeProvider;
    private @Nullable AddonFinderService addonFinderService;
    private @Nullable Map<String, Object> config;
    private final ScheduledExecutorService scheduler;
    private final Map<String, Boolean> baseFinderConfig = new ConcurrentHashMap<>();
    private final ScheduledFuture<?> cfgRefreshTask;

    @Activate
    public AddonSuggestionService(final @Reference ConfigurationAdmin configurationAdmin,
            @Reference LocaleProvider localeProvider) {
        this.configurationAdmin = configurationAdmin;
        this.localeProvider = localeProvider;

        SUGGESTION_FINDERS.forEach(f -> baseFinderConfig.put(f, false));

        // Changes to the configuration are expected to call the {@link modified} method. This works well when running
        // in Eclipse. Running in Karaf, the method was not consistently called. Therefore regularly check for changes
        // in configuration.
        // This pattern and code was re-used from {@link org.openhab.core.karaf.internal.FeatureInstaller}
        scheduler = ThreadPoolManager.getScheduledPool(ThreadPoolManager.THREAD_POOL_NAME_COMMON);
        cfgRefreshTask = scheduler.scheduleWithFixedDelay(this::syncConfiguration, 1, 1, TimeUnit.MINUTES);
    }

    @Deactivate
    protected void deactivate() {
        cfgRefreshTask.cancel(true);
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void addAddonFinderService(AddonFinderService addonFinderService) {
        this.addonFinderService = addonFinderService;
        // We retrieve the configuration at this time so that it contains all the user settings defining what finder to
        // enable. When the service is started, the valid configuration is not yet loaded.
        modified(getConfiguration());
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
                        ? ConfigParser.valueAsOrElse(config.get(cfgParam), Boolean.class, true)
                        : cfg;
                if (cfg != enabled) {
                    String type = SUGGESTION_FINDER_TYPES.get(finder);
                    AddonFinderService finderService = addonFinderService;
                    if (type != null && finderService != null) {
                        logger.debug("baseFinderConfig {} {} = {} => updating from {} to {}", finder, cfgParam,
                                config == null ? "null config" : config.get(cfgParam), cfg, enabled);
                        baseFinderConfig.put(finder, enabled);
                        if (enabled) {
                            finderService.install(type);
                        } else {
                            finderService.uninstall(type);
                        }
                    }
                }
            }
        });
        this.config = config;
    }

    private void syncConfiguration() {
        try {
            final Map<String, Object> cfg = getConfiguration();
            if (cfg != null && !cfg.equals(config)) {
                modified(cfg);
            }
        } catch (IllegalStateException e) {
            logger.debug("Exception occurred while trying to sync the configuration: {}", e.getMessage());
        }
    }

    private @Nullable Map<String, Object> getConfiguration() {
        try {
            Dictionary<String, Object> cfg = configurationAdmin.getConfiguration(CONFIG_PID).getProperties();
            if (cfg != null) {
                List<String> keys = Collections.list(cfg.keys());
                return keys.stream().collect(Collectors.toMap(Function.identity(), cfg::get));
            }
        } catch (IOException | IllegalStateException e) {
            logger.debug("Exception occurred while trying to get the configuration: {}", e.getMessage());
        }
        return null;
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
        List<AddonInfo> candidates = addonInfoProviders.stream().map(p -> p.getAddonInfos(localeProvider.getLocale()))
                .flatMap(Collection::stream).toList();
        synchronized (addonFinders) {
            addonFinders.stream().filter(this::isFinderEnabled).forEach(f -> f.setAddonCandidates(candidates));
        }
    }

    @Deactivate
    @Override
    public void close() throws Exception {
        synchronized (addonFinders) {
            addonFinders.clear();
        }
        addonInfoProviders.clear();
    }

    public Set<AddonInfo> getSuggestedAddons(@Nullable Locale locale) {
        synchronized (addonFinders) {
            return addonFinders.stream().filter(this::isFinderEnabled).map(f -> f.getSuggestedAddons())
                    .flatMap(Collection::stream).collect(Collectors.toSet());
        }
    }
}
