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
package org.openhab.core.addon.internal.xml;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.AddonI18nLocalizationService;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.addon.AddonInfoProvider;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.config.core.ConfigDescriptionProvider;
import org.openhab.core.config.core.xml.AbstractXmlBasedProvider;
import org.openhab.core.config.core.xml.AbstractXmlConfigDescriptionProvider;
import org.openhab.core.config.core.xml.osgi.XmlDocumentBundleTracker;
import org.openhab.core.config.core.xml.osgi.XmlDocumentProvider;
import org.openhab.core.config.core.xml.osgi.XmlDocumentProviderFactory;
import org.openhab.core.config.core.xml.util.XmlDocumentReader;
import org.openhab.core.service.ReadyService;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link XmlAddonInfoProvider} is a concrete implementation of the {@link AddonInfoProvider} service interface.
 * <p>
 * This implementation manages any {@link AddonInfo} objects associated to specific modules. If a specific module
 * disappears, any registered {@link AddonInfo} objects associated with that module are released.
 *
 * @author Michael Grammling - Initial contribution
 * @author Michael Grammling - Refactoring: Provider/Registry pattern is used, added locale support
 * @author Simon Kaufmann - factored out common aspects into {@link AbstractXmlBasedProvider}
 * @author Jan N. Klug - Refactored to cover all add-ons
 */
@NonNullByDefault
@Component
public class XmlAddonInfoProvider extends AbstractXmlBasedProvider<String, AddonInfo>
        implements AddonInfoProvider, XmlDocumentProviderFactory<AddonInfoXmlResult> {

    private static final String XML_DIRECTORY = "/OH-INF/addon/";
    public static final String READY_MARKER = "openhab.xmlAddonInfo";

    private final AddonI18nLocalizationService addonI18nService;
    private final AbstractXmlConfigDescriptionProvider configDescriptionProvider;
    private final XmlDocumentBundleTracker<AddonInfoXmlResult> addonInfoTracker;
    private final Future<?> trackerJob;

    @Activate
    public XmlAddonInfoProvider(final @Reference AddonI18nLocalizationService addonI18nService,
            final @Reference(target = "(openhab.scope=core.xml.addon)") ConfigDescriptionProvider configDescriptionProvider,
            final @Reference ReadyService readyService, ComponentContext componentContext) {
        this.addonI18nService = addonI18nService;
        this.configDescriptionProvider = (AbstractXmlConfigDescriptionProvider) configDescriptionProvider;

        XmlDocumentReader<AddonInfoXmlResult> addonInfoReader = new AddonInfoReader();
        addonInfoTracker = new XmlDocumentBundleTracker<>(componentContext.getBundleContext(), XML_DIRECTORY,
                addonInfoReader, this, READY_MARKER, readyService);

        ScheduledExecutorService scheduler = ThreadPoolManager
                .getScheduledPool(XmlDocumentBundleTracker.THREAD_POOL_NAME);
        trackerJob = scheduler.submit(addonInfoTracker::open);
    }

    @Deactivate
    public void deactivate() {
        trackerJob.cancel(true);
        addonInfoTracker.close();
    }

    @Override
    public synchronized @Nullable AddonInfo getAddonInfo(@Nullable String id, @Nullable Locale locale) {
        return id == null ? null : get(id, locale);
    }

    @Override
    public synchronized Set<AddonInfo> getAddonInfos(@Nullable Locale locale) {
        return new HashSet<>(getAll(locale));
    }

    @Override
    protected @Nullable AddonInfo localize(Bundle bundle, AddonInfo bindingInfo, @Nullable Locale locale) {
        return addonI18nService.createLocalizedAddonInfo(bundle, bindingInfo, locale);
    }

    @Override
    public XmlDocumentProvider<AddonInfoXmlResult> createDocumentProvider(Bundle bundle) {
        return new AddonInfoXmlProvider(bundle, this, configDescriptionProvider);
    }
}
