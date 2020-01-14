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
package org.openhab.core.binding.xml.internal;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.binding.BindingInfo;
import org.openhab.core.binding.BindingInfoProvider;
import org.openhab.core.binding.i18n.BindingI18nLocalizationService;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.config.core.ConfigDescriptionProvider;
import org.openhab.core.config.xml.AbstractXmlBasedProvider;
import org.openhab.core.config.xml.AbstractXmlConfigDescriptionProvider;
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
 * The {@link XmlBindingInfoProvider} is a concrete implementation of the {@link BindingInfoProvider} service interface.
 * <p>
 * This implementation manages any {@link BindingInfo} objects associated to specific modules. If a specific module
 * disappears, any registered {@link BindingInfo} objects associated with that module are released.
 *
 * @author Michael Grammling - Initial contribution
 * @author Michael Grammling - Refactoring: Provider/Registry pattern is used, added locale support
 * @author Simon Kaufmann - factored out common aspects into {@link AbstractXmlBasedProvider}
 */
@Component
public class XmlBindingInfoProvider extends AbstractXmlBasedProvider<String, BindingInfo>
        implements BindingInfoProvider, XmlDocumentProviderFactory<BindingInfoXmlResult> {

    private static final String XML_DIRECTORY = "/OH-INF/binding/";
    public static final String READY_MARKER = "esh.xmlBindingInfo";

    private final BindingI18nLocalizationService bindingI18nService;
    private AbstractXmlConfigDescriptionProvider configDescriptionProvider;
    private @Nullable XmlDocumentBundleTracker<BindingInfoXmlResult> bindingInfoTracker;
    private final ReadyService readyService;
    private final ScheduledExecutorService scheduler = ThreadPoolManager
            .getScheduledPool(XmlDocumentBundleTracker.THREAD_POOL_NAME);
    private @Nullable Future<?> trackerJob;

    @Activate
    public XmlBindingInfoProvider(final @Reference BindingI18nLocalizationService bindingI18nService,
            final @Reference ReadyService readyService) {
        this.bindingI18nService = bindingI18nService;
        this.readyService = readyService;
    }

    @Activate
    public void activate(ComponentContext componentContext) {
        XmlDocumentReader<BindingInfoXmlResult> bindingInfoReader = new BindingInfoReader();
        bindingInfoTracker = new XmlDocumentBundleTracker<>(componentContext.getBundleContext(), XML_DIRECTORY,
                bindingInfoReader, this, READY_MARKER, readyService);
        trackerJob = scheduler.submit(() -> {
            bindingInfoTracker.open();
        });
    }

    @Deactivate
    public void deactivate(ComponentContext componentContext) {
        Future<?> localTrackerJob = trackerJob;
        if (localTrackerJob != null && !localTrackerJob.isDone()) {
            localTrackerJob.cancel(true);
            trackerJob = null;
        }
        XmlDocumentBundleTracker<BindingInfoXmlResult> localBindingInfoTracker = bindingInfoTracker;
        if (localBindingInfoTracker != null) {
            localBindingInfoTracker.close();
            bindingInfoTracker = null;
        }
    }

    @Override
    public synchronized @Nullable BindingInfo getBindingInfo(@Nullable String id, @Nullable Locale locale) {
        return id == null ? null : get(id, locale);
    }

    @Override
    public synchronized Set<BindingInfo> getBindingInfos(@Nullable Locale locale) {
        return new HashSet<>(getAll(locale));
    }

    @Reference(target = "(esh.scope=core.xml.binding)")
    public void setConfigDescriptionProvider(ConfigDescriptionProvider configDescriptionProvider) {
        this.configDescriptionProvider = (AbstractXmlConfigDescriptionProvider) configDescriptionProvider;
    }

    public void unsetConfigDescriptionProvider(ConfigDescriptionProvider configDescriptionProvider) {
        this.configDescriptionProvider = null;
    }

    @Override
    protected @Nullable BindingInfo localize(Bundle bundle, BindingInfo bindingInfo, @Nullable Locale locale) {
        return bindingI18nService.createLocalizedBindingInfo(bundle, bindingInfo, locale);
    }

    @Override
    public XmlDocumentProvider<BindingInfoXmlResult> createDocumentProvider(Bundle bundle) {
        return new BindingInfoXmlProvider(bundle, this, configDescriptionProvider);
    }
}
