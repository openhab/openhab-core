/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.thing.binding;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.thing.events.ChannelDescriptionChangedEvent;
import org.openhab.core.thing.i18n.ChannelTypeI18nLocalizationService;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AbstractDynamicDescriptionProvider} provides a base implementation for dynamic description providers.
 * <p>
 * It holds a reference to the {@link ChannelTypeI18nLocalizationService} to provide localized descriptions. Therefore
 * the inheriting class has to request a reference for the {@link ChannelTypeI18nLocalizationService} on its own.
 * <p>
 * It posts {@link ChannelDescriptionChangedEvent}s through the openHAB events bus about a changed dynamic description.
 * Therefore the subclass has to request references for the {@link EventPublisher} and
 * {@link ItemChannelLinkRegistry}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractDynamicDescriptionProvider {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected @NonNullByDefault({}) BundleContext bundleContext;
    protected @Nullable EventPublisher eventPublisher;
    protected @Nullable ChannelTypeI18nLocalizationService channelTypeI18nLocalizationService;
    protected @Nullable ItemChannelLinkRegistry itemChannelLinkRegistry;

    /**
     * This method can be used in a subclass in order to post events through the openHAB events bus. A common use case
     * is to notify event subscribers about a changed dynamic description.
     *
     * @param event the {@link Event}
     */
    protected void postEvent(Event event) {
        if (eventPublisher != null) {
            try {
                eventPublisher.post(event);
            } catch (RuntimeException e) {
                logger.error("Cannot post '{}' event: {}", event.getType(), e.getMessage(), e);
            }
        } else {
            logger.debug("Cannot post event as EventPublisher is missing");
        }
    }

    @Activate
    protected void activate(ComponentContext componentContext) {
        bundleContext = componentContext.getBundleContext();
    }

    @Deactivate
    public void deactivate() {
        bundleContext = null;
    }
}
