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
package org.eclipse.smarthome.core.binding.xml.internal;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.smarthome.config.core.ConfigDescriptionProvider;
import org.eclipse.smarthome.config.xml.AbstractXmlBasedProvider;
import org.eclipse.smarthome.config.xml.AbstractXmlConfigDescriptionProvider;
import org.eclipse.smarthome.config.xml.osgi.XmlDocumentBundleTracker;
import org.eclipse.smarthome.config.xml.osgi.XmlDocumentProvider;
import org.eclipse.smarthome.config.xml.osgi.XmlDocumentProviderFactory;
import org.eclipse.smarthome.config.xml.util.XmlDocumentReader;
import org.eclipse.smarthome.core.binding.BindingInfo;
import org.eclipse.smarthome.core.binding.BindingInfoProvider;
import org.eclipse.smarthome.core.common.ThreadPoolManager;
import org.eclipse.smarthome.core.i18n.BindingI18nUtil;
import org.eclipse.smarthome.core.i18n.TranslationProvider;
import org.eclipse.smarthome.core.service.ReadyService;
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
 * @author Michael Grammling - Initial Contribution
 * @author Michael Grammling - Refactoring: Provider/Registry pattern is used, added locale support
 * @author Simon Kaufmann - factored out common aspects into {@link AbstractXmlBasedProvider}
 */
@Component
public class XmlBindingInfoProvider extends AbstractXmlBasedProvider<String, BindingInfo>
        implements BindingInfoProvider, XmlDocumentProviderFactory<BindingInfoXmlResult> {

    private static final String XML_DIRECTORY = "/ESH-INF/binding/";
    public static final String READY_MARKER = "esh.xmlBindingInfo";

    private BindingI18nUtil bindingI18nUtil;
    private AbstractXmlConfigDescriptionProvider configDescriptionProvider;
    private XmlDocumentBundleTracker<BindingInfoXmlResult> bindingInfoTracker;
    private ReadyService readyService;

    private ScheduledExecutorService scheduler = ThreadPoolManager
            .getScheduledPool(XmlDocumentBundleTracker.THREAD_POOL_NAME);
    private Future<?> trackerJob;

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
        if (trackerJob != null && !trackerJob.isDone()) {
            trackerJob.cancel(true);
            trackerJob = null;
        }
        bindingInfoTracker.close();
        bindingInfoTracker = null;
    }

    @Override
    public synchronized BindingInfo getBindingInfo(String id, Locale locale) {
        return get(id, locale);
    }

    @Override
    public synchronized Set<BindingInfo> getBindingInfos(Locale locale) {
        return new HashSet<>(getAll(locale));
    }

    @Reference
    public void setTranslationProvider(TranslationProvider i18nProvider) {
        this.bindingI18nUtil = new BindingI18nUtil(i18nProvider);
    }

    public void unsetTranslationProvider(TranslationProvider i18nProvider) {
        this.bindingI18nUtil = null;
    }

    @Reference(target = "(esh.scope=core.xml.binding)")
    public void setConfigDescriptionProvider(ConfigDescriptionProvider configDescriptionProvider) {
        this.configDescriptionProvider = (AbstractXmlConfigDescriptionProvider) configDescriptionProvider;
    }

    public void unsetConfigDescriptionProvider(ConfigDescriptionProvider configDescriptionProvider) {
        this.configDescriptionProvider = null;
    }

    @Reference
    public void setReadyService(ReadyService readyService) {
        this.readyService = readyService;
    }

    public void unsetReadyService(ReadyService readyService) {
        this.readyService = null;
    }

    @Override
    protected BindingInfo localize(Bundle bundle, BindingInfo bindingInfo, Locale locale) {
        if (this.bindingI18nUtil == null) {
            return null;
        }

        String name = this.bindingI18nUtil.getName(bundle, bindingInfo.getUID(), bindingInfo.getName(), locale);
        String description = this.bindingI18nUtil.getDescription(bundle, bindingInfo.getUID(),
                bindingInfo.getDescription(), locale);

        return new BindingInfo(bindingInfo.getUID(), name, description, bindingInfo.getAuthor(),
                bindingInfo.getServiceId(), bindingInfo.getConfigDescriptionURI());
    }

    @Override
    public XmlDocumentProvider<BindingInfoXmlResult> createDocumentProvider(Bundle bundle) {
        return new BindingInfoXmlProvider(bundle, this, configDescriptionProvider);
    }

}
