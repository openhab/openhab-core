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
package org.openhab.core.thing.xml.internal;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.config.core.ConfigDescriptionProvider;
import org.openhab.core.config.core.xml.AbstractXmlBasedProvider;
import org.openhab.core.config.core.xml.AbstractXmlConfigDescriptionProvider;
import org.openhab.core.config.core.xml.osgi.XmlDocumentBundleTracker;
import org.openhab.core.config.core.xml.osgi.XmlDocumentProvider;
import org.openhab.core.config.core.xml.osgi.XmlDocumentProviderFactory;
import org.openhab.core.config.core.xml.util.XmlDocumentReader;
import org.openhab.core.service.ReadyService;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.UID;
import org.openhab.core.thing.binding.ThingTypeProvider;
import org.openhab.core.thing.i18n.ThingTypeI18nLocalizationService;
import org.openhab.core.thing.type.ChannelGroupTypeProvider;
import org.openhab.core.thing.type.ChannelTypeProvider;
import org.openhab.core.thing.type.ThingType;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link XmlThingTypeProvider} is a concrete implementation of the {@link ThingTypeProvider} service interface.
 * <p>
 * This implementation manages any {@link ThingType} objects associated to specific modules. If a specific module
 * disappears, any registered {@link ThingType} objects associated with that module are released.
 *
 * @author Michael Grammling - Initial contribution
 * @author Dennis Nobel - Added locale support, Added cache for localized thing types
 * @author Ivan Iliev - Added support for system wide channel types
 * @author Kai Kreuzer - fixed concurrency issues
 * @author Simon Kaufmann - factored out common aspects into {@link AbstractXmlBasedProvider}
 */
@NonNullByDefault
@Component(property = { "openhab.scope=core.xml.thing" })
public class XmlThingTypeProvider extends AbstractXmlBasedProvider<UID, ThingType>
        implements ThingTypeProvider, XmlDocumentProviderFactory<List<?>> {

    private static final String XML_DIRECTORY = "/OH-INF/thing/";
    public static final String READY_MARKER = "openhab.xmlThingTypes";

    private final ThingTypeI18nLocalizationService thingTypeI18nLocalizationService;
    private final XmlChannelTypeProvider channelTypeProvider;
    private final XmlChannelGroupTypeProvider channelGroupTypeProvider;
    private final AbstractXmlConfigDescriptionProvider configDescriptionProvider;
    private @Nullable XmlDocumentBundleTracker<List<?>> thingTypeTracker;
    private final ReadyService readyService;
    private final ScheduledExecutorService scheduler = ThreadPoolManager
            .getScheduledPool(XmlDocumentBundleTracker.THREAD_POOL_NAME);
    private @Nullable Future<?> trackerJob;

    @Activate
    public XmlThingTypeProvider(
            final @Reference(target = "(openhab.scope=core.xml.channelGroups)") ChannelGroupTypeProvider channelGroupTypeProvider,
            final @Reference(target = "(openhab.scope=core.xml.channels)") ChannelTypeProvider channelTypeProvider,
            final @Reference(target = "(openhab.scope=core.xml.thing)") ConfigDescriptionProvider configDescriptionProvider,
            final @Reference ReadyService readyService,
            final @Reference ThingTypeI18nLocalizationService thingTypeI18nLocalizationService) {
        this.channelGroupTypeProvider = (XmlChannelGroupTypeProvider) channelGroupTypeProvider;
        this.channelTypeProvider = (XmlChannelTypeProvider) channelTypeProvider;
        this.configDescriptionProvider = (AbstractXmlConfigDescriptionProvider) configDescriptionProvider;
        this.readyService = readyService;
        this.thingTypeI18nLocalizationService = thingTypeI18nLocalizationService;
    }

    @Activate
    protected void activate(BundleContext bundleContext) {
        XmlDocumentReader<List<?>> thingTypeReader = new ThingDescriptionReader();
        thingTypeTracker = new XmlDocumentBundleTracker<>(bundleContext, XML_DIRECTORY, thingTypeReader, this,
                READY_MARKER, readyService);
        trackerJob = scheduler.submit(() -> {
            thingTypeTracker.open();
        });
    }

    @Deactivate
    protected void deactivate() {
        Future<?> localTrackerJob = trackerJob;
        if (localTrackerJob != null && !localTrackerJob.isDone()) {
            localTrackerJob.cancel(true);
            trackerJob = null;
        }
        XmlDocumentBundleTracker<List<?>> localThingTypeTracker = thingTypeTracker;
        if (localThingTypeTracker != null) {
            localThingTypeTracker.close();
            thingTypeTracker = null;
        }
    }

    @Override
    public @Nullable ThingType getThingType(ThingTypeUID thingTypeUID, @Nullable Locale locale) {
        return get(thingTypeUID, locale);
    }

    @Override
    public synchronized Collection<ThingType> getThingTypes(@Nullable Locale locale) {
        return getAll(locale);
    }

    @Override
    protected @Nullable ThingType localize(Bundle bundle, ThingType thingType, @Nullable Locale locale) {
        return thingTypeI18nLocalizationService.createLocalizedThingType(bundle, thingType, locale);
    }

    @Override
    public XmlDocumentProvider<List<?>> createDocumentProvider(Bundle bundle) {
        return new ThingTypeXmlProvider(bundle, configDescriptionProvider, this, channelTypeProvider,
                channelGroupTypeProvider);
    }
}
