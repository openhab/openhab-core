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
package org.openhab.core.magic.binding.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.thing.binding.BaseDynamicCommandDescriptionProvider;
import org.openhab.core.thing.i18n.ChannelTypeI18nLocalizationService;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.type.DynamicCommandDescriptionProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Dynamic provider of command options.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@Component(service = { DynamicCommandDescriptionProvider.class, MagicDynamicCommandDescriptionProvider.class })
@NonNullByDefault
public class MagicDynamicCommandDescriptionProvider extends BaseDynamicCommandDescriptionProvider {

    @Activate
    public MagicDynamicCommandDescriptionProvider(final @Reference EventPublisher eventPublisher, //
            final @Reference ChannelTypeI18nLocalizationService channelTypeI18nLocalizationService, //
            final @Reference ItemChannelLinkRegistry itemChannelLinkRegistry) {
        this.eventPublisher = eventPublisher;
        this.channelTypeI18nLocalizationService = channelTypeI18nLocalizationService;
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
    }
}
