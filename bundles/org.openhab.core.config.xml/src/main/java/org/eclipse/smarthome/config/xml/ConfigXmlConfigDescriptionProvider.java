/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.config.xml;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.ConfigDescriptionProvider;
import org.eclipse.smarthome.config.core.i18n.ConfigI18nLocalizationService;
import org.eclipse.smarthome.config.xml.internal.ConfigDescriptionReader;
import org.eclipse.smarthome.config.xml.internal.ConfigDescriptionXmlProvider;
import org.eclipse.smarthome.config.xml.osgi.XmlDocumentBundleTracker;
import org.eclipse.smarthome.config.xml.osgi.XmlDocumentProvider;
import org.eclipse.smarthome.config.xml.osgi.XmlDocumentProviderFactory;
import org.eclipse.smarthome.config.xml.util.XmlDocumentReader;
import org.eclipse.smarthome.core.common.ThreadPoolManager;
import org.eclipse.smarthome.core.service.ReadyService;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * Provides {@link ConfigDescription}s for configurations which are read from XML files.
 *
 * @author Simon Kaufmann - initial contribution and API
 *
 */
@Component(service = ConfigDescriptionProvider.class, immediate = true, property = { "esh.scope=core.xml.config" })
public class ConfigXmlConfigDescriptionProvider extends AbstractXmlConfigDescriptionProvider
        implements XmlDocumentProviderFactory<List<ConfigDescription>> {

    private static final String XML_DIRECTORY = "/ESH-INF/config/";
    public static final String READY_MARKER = "esh.xmlConfig";

    private XmlDocumentBundleTracker<List<ConfigDescription>> configDescriptionTracker;

    private ConfigI18nLocalizationService configI18nLocalizerService;
    private ReadyService readyService;

    private ScheduledExecutorService scheduler = ThreadPoolManager
            .getScheduledPool(XmlDocumentBundleTracker.THREAD_POOL_NAME);
    private Future<?> trackerJob;

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
        if (trackerJob != null && !trackerJob.isDone()) {
            trackerJob.cancel(true);
            trackerJob = null;
        }
        configDescriptionTracker.close();
        configDescriptionTracker = null;
    }

    @Reference
    public void setConfigI18nLocalizerService(ConfigI18nLocalizationService configI18nLocalizerService) {
        this.configI18nLocalizerService = configI18nLocalizerService;
    }

    public void unsetConfigI18nLocalizerService(ConfigI18nLocalizationService configI18nLocalizerService) {
        this.configI18nLocalizerService = null;
    }

    @Reference
    public void setReadyService(ReadyService readyService) {
        this.readyService = readyService;
    }

    public void unsetReadyService(ReadyService readyService) {
        this.readyService = null;
    }

    @Override
    protected ConfigI18nLocalizationService getConfigI18nLocalizerService() {
        return configI18nLocalizerService;
    }

    @Override
    public XmlDocumentProvider<List<ConfigDescription>> createDocumentProvider(Bundle bundle) {
        return new ConfigDescriptionXmlProvider(bundle, this);
    }

}
