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
package org.openhab.core.magic.binding.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.thing.binding.BaseDynamicStateDescriptionProvider;
import org.openhab.core.thing.i18n.ChannelTypeI18nLocalizationService;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.type.DynamicStateDescriptionProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.LoggerFactory;

/**
 * Dynamic provider of state options while leaving other state description fields as original.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@Component(service = { DynamicStateDescriptionProvider.class, MagicDynamicStateDescriptionProvider.class })
@NonNullByDefault
public class MagicDynamicStateDescriptionProvider extends BaseDynamicStateDescriptionProvider {
    @Activate
    public MagicDynamicStateDescriptionProvider(final @Reference EventPublisher eventPublisher, //
            final @Reference ChannelTypeI18nLocalizationService channelTypeI18nLocalizationService, //
            final @Reference ItemChannelLinkRegistry itemChannelLinkRegistry) {
        this.eventPublisher = eventPublisher;
        this.channelTypeI18nLocalizationService = channelTypeI18nLocalizationService;
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;

        LoggerFactory.getLogger(getClass()).error("called activate constructor");
    }

    public void activate() {
        LoggerFactory.getLogger(getClass()).error("called activate method");
    }
}
