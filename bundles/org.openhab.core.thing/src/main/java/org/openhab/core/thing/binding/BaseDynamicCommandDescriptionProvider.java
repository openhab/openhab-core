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
package org.openhab.core.thing.binding;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.events.ThingEventFactory;
import org.openhab.core.thing.i18n.ChannelTypeI18nLocalizationService;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.type.DynamicCommandDescriptionProvider;
import org.openhab.core.types.CommandDescription;
import org.openhab.core.types.CommandDescriptionBuilder;
import org.openhab.core.types.CommandOption;
import org.osgi.service.component.annotations.Deactivate;

/**
 * The {@link BaseDynamicCommandDescriptionProvider} provides a base implementation for the
 * {@link DynamicCommandDescriptionProvider}.
 * <p>
 * It provides localized dynamic {@link CommandOption}s. Therefore the inheriting class has to request a reference for
 * the {@link ChannelTypeI18nLocalizationService} on its own.
 *
 * @author Christoph Weitkamp - Initial contribution
 * @author Christoph Weitkamp - Added ChannelStateDescriptionChangedEvent
 */
@NonNullByDefault
public abstract class BaseDynamicCommandDescriptionProvider extends AbstractDynamicDescriptionProvider
        implements DynamicCommandDescriptionProvider {

    protected final Map<ChannelUID, List<CommandOption>> channelOptionsMap = new ConcurrentHashMap<>();

    /**
     * For a given {@link ChannelUID}, set a {@link List} of {@link CommandOption}s that should be used for the channel,
     * instead of the one defined statically in the {@link ChannelType}.
     *
     * @param channelUID the {@link ChannelUID} of the channel
     * @param options a {@link List} of {@link CommandOption}s
     */
    public void setCommandOptions(ChannelUID channelUID, List<CommandOption> options) {
        List<CommandOption> oldOptions = channelOptionsMap.get(channelUID);
        if (!options.equals(oldOptions)) {
            channelOptionsMap.put(channelUID, options);
            postEvent(ThingEventFactory.createChannelDescriptionCommandOptionsChangedEvent(channelUID,
                    itemChannelLinkRegistry != null ? itemChannelLinkRegistry.getLinkedItemNames(channelUID) : Set.of(),
                    options, oldOptions));
        }
    }

    @Override
    public @Nullable CommandDescription getCommandDescription(Channel channel,
            @Nullable CommandDescription originalCommandDescription, @Nullable Locale locale) {
        List<CommandOption> options = channelOptionsMap.get(channel.getUID());
        if (options == null) {
            return null;
        }

        return CommandDescriptionBuilder.create().withCommandOptions(localizedCommandOptions(options, channel, locale))
                .build();
    }

    /**
     * Localizes a {@link List} of {@link CommandOption}s that should be used for the channel.
     *
     * @param options a {@link List} of {@link CommandOption}s
     * @param channel the channel
     * @param locale a locale
     * @return the localized {@link List} of {@link CommandOption}s
     */
    protected List<CommandOption> localizedCommandOptions(List<CommandOption> options, Channel channel,
            @Nullable Locale locale) {
        // can be overridden by subclasses
        ChannelTypeUID channelTypeUID = channel.getChannelTypeUID();
        if (channelTypeI18nLocalizationService != null && channelTypeUID != null) {
            return channelTypeI18nLocalizationService.createLocalizedCommandOptions(bundleContext.getBundle(), options,
                    channelTypeUID, locale);
        }
        return options;
    }

    @Override
    @Deactivate
    public void deactivate() {
        channelOptionsMap.clear();
        super.deactivate();
    }
}
