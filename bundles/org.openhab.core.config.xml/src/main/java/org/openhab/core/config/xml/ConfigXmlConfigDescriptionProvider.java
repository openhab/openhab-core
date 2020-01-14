/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.config.xml;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionProvider;
import org.openhab.core.config.core.i18n.ConfigI18nLocalizationService;
import org.openhab.core.config.xml.internal.ConfigDescriptionReader;
import org.openhab.core.config.xml.internal.ConfigDescriptionXmlProvider;
import org.openhab.core.config.xml.osgi.XmlDocumentBundleTracker;
import org.openhab.core.config.xml.osgi.XmlDocumentProvider;
import org.openhab.core.config.xml.osgi.XmlDocumentProviderFactory;
import org.openhab.core.config.xml.util.XmlDocumentReader;
import org.openhab.core.service.ReadyService;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * Provides {@link ConfigDescription}s for configurations which are read from XML files.
 *
 * @author Simon Kaufmann - Initial contribution
 */
@Component(service = ConfigDescriptionProvider.class, immediate = true, property = { "esh.scope=core.xml.config" })
@NonNullByDefault
public class ConfigXmlConfigDescriptionProvider extends AbstractXmlConfigDescriptionProvider
        implements XmlDocumentProviderFactory<List<ConfigDescription>> {

    private static final String XML_DIRECTORY = "/OH-INF/config/";
    public static final String READY_MARKER = "esh.xmlConfig";

    private final ConfigI18nLocalizationService configI18nService;
    private @Nullable XmlDocumentBundleTracker<List<ConfigDescription>> configDescriptionTracker;
    private final ReadyService readyService;
    private final ScheduledExecutorService scheduler = ThreadPoolManager
            .getScheduledPool(XmlDocumentBundleTracker.THREAD_POOL_NAME);
    private @Nullable Future<?> trackerJob;

    @Activate
    public ConfigXmlConfigDescriptionProvider(final @Reference ConfigI18nLocalizationService configI18nService,
            final @Reference ReadyService readyService) {
        this.configI18nService = configI18nService;
        this.readyService = readyService;
    }

    @Activate
    public void activate(ComponentContext componentContext) {
        XmlDocumentReader<List<ConfigDescription>> configDescriptionReader = new ConfigDescriptionReader();
        configDescriptionTracker = new XmlDocumentBundleTracker<>(componentContext.getBundleContext(), XML_DIRECTORY,
                configDescriptionReader, this, READY_MARKER, readyService);
        trackerJob = scheduler.submit(() -> {
            configDescriptionTracker.open();
        });
    }

    @Deactivate
    public void deactivate() {
        Future<?> localTrackerJob = trackerJob;
        if (localTrackerJob != null && !localTrackerJob.isDone()) {
            localTrackerJob.cancel(true);
            trackerJob = null;
        }
        XmlDocumentBundleTracker<List<ConfigDescription>> localConfigDescriptionTracker = configDescriptionTracker;
        if (localConfigDescriptionTracker != null) {
            localConfigDescriptionTracker.close();
            configDescriptionTracker = null;
        }
    }

    @Override
    protected ConfigI18nLocalizationService getConfigI18nLocalizerService() {
        return configI18nService;
    }

    @Override
    public XmlDocumentProvider<List<ConfigDescription>> createDocumentProvider(Bundle bundle) {
        return new ConfigDescriptionXmlProvider(bundle, this);
    }
}
