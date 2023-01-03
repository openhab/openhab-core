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
import org.openhab.core.thing.type.DynamicStateDescriptionProvider;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.StateOption;
import org.osgi.service.component.annotations.Deactivate;

/**
 * The {@link BaseDynamicStateDescriptionProvider} provides a base implementation for the
 * {@link DynamicStateDescriptionProvider}.
 * <p>
 * It provides localized patterns and dynamic {@link StateOption}s while leaving other {@link StateDescription} fields
 * as original. Therefore the inheriting class has to request a reference for the
 * {@link ChannelTypeI18nLocalizationService} on its own.
 *
 * @author Christoph Weitkamp - Initial contribution
 * @author Christoph Weitkamp - Added ChannelStateDescriptionChangedEvent
 */
@NonNullByDefault
public abstract class BaseDynamicStateDescriptionProvider extends AbstractDynamicDescriptionProvider
        implements DynamicStateDescriptionProvider {

    protected final Map<ChannelUID, String> channelPatternMap = new ConcurrentHashMap<>();
    protected final Map<ChannelUID, List<StateOption>> channelOptionsMap = new ConcurrentHashMap<>();

    /**
     * For a given {@link ChannelUID}, set a pattern that should be used for the channel, instead of the one defined
     * statically in the {@link ChannelType}.
     *
     * @param channelUID the {@link ChannelUID} of the channel
     * @param pattern a pattern
     */
    public void setStatePattern(ChannelUID channelUID, String pattern) {
        String oldPattern = channelPatternMap.get(channelUID);
        if (!pattern.equals(oldPattern)) {
            channelPatternMap.put(channelUID, pattern);
            postEvent(ThingEventFactory.createChannelDescriptionPatternChangedEvent(channelUID,
                    itemChannelLinkRegistry != null ? itemChannelLinkRegistry.getLinkedItemNames(channelUID) : Set.of(),
                    pattern, oldPattern));
        }
    }

    /**
     * For a given {@link ChannelUID}, set a {@link List} of {@link StateOption}s that should be used for the channel,
     * instead of the one defined statically in the {@link ChannelType}.
     *
     * @param channelUID the {@link ChannelUID} of the channel
     * @param options a {@link List} of {@link StateOption}s
     */
    public void setStateOptions(ChannelUID channelUID, List<StateOption> options) {
        List<StateOption> oldOptions = channelOptionsMap.get(channelUID);
        if (!options.equals(oldOptions)) {
            channelOptionsMap.put(channelUID, options);
            postEvent(ThingEventFactory.createChannelDescriptionStateOptionsChangedEvent(channelUID,
                    itemChannelLinkRegistry != null ? itemChannelLinkRegistry.getLinkedItemNames(channelUID) : Set.of(),
                    options, oldOptions));
        }
    }

    @Override
    public @Nullable StateDescription getStateDescription(Channel channel, @Nullable StateDescription original,
            @Nullable Locale locale) {
        // can be overridden by subclasses
        ChannelUID channelUID = channel.getUID();
        String pattern = channelPatternMap.get(channelUID);
        List<StateOption> options = channelOptionsMap.get(channelUID);
        if (pattern == null && options == null) {
            return null;
        }

        StateDescriptionFragmentBuilder builder = (original == null) ? StateDescriptionFragmentBuilder.create()
                : StateDescriptionFragmentBuilder.create(original);

        if (pattern != null) {
            String localizedPattern = localizeStatePattern(pattern, channel, locale);
            if (localizedPattern != null) {
                builder.withPattern(localizedPattern);
            }
        }

        if (options != null) {
            builder.withOptions(localizedStateOptions(options, channel, locale));
        }

        return builder.build().toStateDescription();
    }

    /**
     * Localizes a pattern that should be used for the channel.
     *
     * @param pattern a pattern
     * @param channel the channel
     * @param locale a locale
     * @return the localized pattern
     */
    protected @Nullable String localizeStatePattern(String pattern, Channel channel, @Nullable Locale locale) {
        // can be overridden by subclasses
        ChannelTypeUID channelTypeUID = channel.getChannelTypeUID();
        if (channelTypeI18nLocalizationService != null && channelTypeUID != null) {
            return channelTypeI18nLocalizationService.createLocalizedStatePattern(bundleContext.getBundle(), pattern,
                    channelTypeUID, locale);
        }
        return pattern;
    }

    /**
     * Localizes a {@link List} of {@link StateOption}s that should be used for the channel.
     *
     * @param options a {@link List} of {@link StateOption}s
     * @param channel the channel
     * @param locale a locale
     * @return the localized {@link List} of {@link StateOption}s
     */
    protected List<StateOption> localizedStateOptions(List<StateOption> options, Channel channel,
            @Nullable Locale locale) {
        // can be overridden by subclasses
        ChannelTypeUID channelTypeUID = channel.getChannelTypeUID();
        if (channelTypeI18nLocalizationService != null && channelTypeUID != null) {
            return channelTypeI18nLocalizationService.createLocalizedStateOptions(bundleContext.getBundle(), options,
                    channelTypeUID, locale);
        }
        return options;
    }

    @Override
    @Deactivate
    public void deactivate() {
        channelPatternMap.clear();
        channelOptionsMap.clear();
        super.deactivate();
    }
}
