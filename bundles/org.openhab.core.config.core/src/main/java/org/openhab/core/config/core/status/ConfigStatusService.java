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
package org.openhab.core.config.core.status;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.config.core.status.events.ConfigStatusInfoEvent;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.util.BundleResolver;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ConfigStatusService} provides the {@link ConfigStatusInfo} for a specific entity. For this purpose
 * it loops over all registered {@link ConfigStatusProvider}s and returns the {@link ConfigStatusInfo} for the matching
 * {@link ConfigStatusProvider}.
 *
 * @author Thomas Höfer - Initial contribution
 * @author Chris Jackson - Allow null messages
 * @author Markus Rathgeb - Add locale provider support
 */
@Component(immediate = true, service = ConfigStatusService.class)
@NonNullByDefault
public final class ConfigStatusService implements ConfigStatusCallback {

    private final Logger logger = LoggerFactory.getLogger(ConfigStatusService.class);

    private final EventPublisher eventPublisher;
    private final LocaleProvider localeProvider;
    private final TranslationProvider translationProvider;
    private final BundleResolver bundleResolver;

    private final List<ConfigStatusProvider> configStatusProviders = new CopyOnWriteArrayList<>();
    private final ExecutorService executorService = ThreadPoolManager
            .getPool(ConfigStatusService.class.getSimpleName());

    @Activate
    public ConfigStatusService(final @Reference EventPublisher eventPublisher, //
            final @Reference LocaleProvider localeProvider, //
            final @Reference TranslationProvider i18nProvider, //
            final @Reference BundleResolver bundleResolver) {
        this.eventPublisher = eventPublisher;
        this.localeProvider = localeProvider;
        this.translationProvider = i18nProvider;
        this.bundleResolver = bundleResolver;
    }

    /**
     * Retrieves the {@link ConfigStatusInfo} of the entity by using the registered
     * {@link ConfigStatusProvider} that supports the given entity.
     *
     * @param entityId the id of the entity whose configuration status information is to be retrieved (must not
     *            be null or empty)
     * @param locale the locale to be used for the corresponding configuration status messages; if null then the
     *            default local will be used
     * @return the {@link ConfigStatusInfo} or null if there is no {@link ConfigStatusProvider} registered that
     *         supports the given entity
     * @throws IllegalArgumentException if given entityId is null or empty
     */
    public @Nullable ConfigStatusInfo getConfigStatus(String entityId, final @Nullable Locale locale) {
        if (entityId == null || entityId.isEmpty()) {
            throw new IllegalArgumentException("EntityId must not be null or empty");
        }

        final Locale loc = locale != null ? locale : localeProvider.getLocale();

        for (ConfigStatusProvider configStatusProvider : configStatusProviders) {
            if (configStatusProvider.supportsEntity(entityId)) {
                return getConfigStatusInfo(configStatusProvider, entityId, loc);
            }
        }

        logger.debug("There is no config status provider for entity {} available.", entityId);

        return null;
    }

    @Override
    public void configUpdated(final ConfigStatusSource configStatusSource) {
        executorService.submit(() -> {
            final ConfigStatusInfo info = getConfigStatus(configStatusSource.entityId, null);
            if (info != null) {
                eventPublisher.post(new ConfigStatusInfoEvent(configStatusSource.getTopic(), info));
            }
        });
    }

    private @Nullable ConfigStatusInfo getConfigStatusInfo(ConfigStatusProvider configStatusProvider, String entityId,
            @Nullable Locale locale) {
        Collection<ConfigStatusMessage> configStatusMessages = configStatusProvider.getConfigStatus();
        if (configStatusMessages == null) {
            logger.debug("Cannot provide config status for entity {} because its config status provider returned null.",
                    entityId);
            return null;
        }

        Bundle bundle = bundleResolver.resolveBundle(configStatusProvider.getClass());
        if (!configStatusMessages.isEmpty()) {
            ConfigStatusInfo info = new ConfigStatusInfo();
            for (ConfigStatusMessage configStatusMessage : configStatusMessages) {
                String message = null;
                if (configStatusMessage.messageKey != null) {
                    message = translationProvider.getText(bundle, configStatusMessage.messageKey, null, locale,
                            configStatusMessage.arguments);
                    if (message == null) {
                        logger.warn(
                                "No translation found for key {} and config status provider {}. Will ignore the config status message.",
                                configStatusMessage.messageKey, configStatusProvider.getClass().getSimpleName());
                        continue;
                    }
                }
                info.add(new ConfigStatusMessage(configStatusMessage.parameterName, configStatusMessage.type, message,
                        configStatusMessage.statusCode));
            }
            return info;
        }
        return null;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addConfigStatusProvider(ConfigStatusProvider configStatusProvider) {
        configStatusProvider.setConfigStatusCallback(this);
        configStatusProviders.add(configStatusProvider);
    }

    protected void removeConfigStatusProvider(ConfigStatusProvider configStatusProvider) {
        configStatusProvider.setConfigStatusCallback(null);
        configStatusProviders.remove(configStatusProvider);
    }
}
