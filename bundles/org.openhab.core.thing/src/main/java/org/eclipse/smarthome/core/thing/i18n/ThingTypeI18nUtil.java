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
package org.eclipse.smarthome.core.thing.i18n;

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.i18n.I18nUtil;
import org.eclipse.smarthome.core.i18n.TranslationProvider;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.type.ChannelDefinition;
import org.eclipse.smarthome.core.thing.type.ChannelGroupDefinition;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeUID;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.osgi.framework.Bundle;

/**
 * {@link ThingTypeI18nUtil} uses the {@link TranslationProvider} to resolve
 * the localized texts. It automatically infers the key if the default text is
 * not a constant.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Laurent Garnier - add translation for channel group label and channel group description
 * @author Christoph Weitkamp - fix localized label and description for channel definition
 */
@NonNullByDefault
public class ThingTypeI18nUtil {

    private final TranslationProvider i18nProvider;

    public ThingTypeI18nUtil(TranslationProvider i18nProvider) {
        this.i18nProvider = i18nProvider;
    }

    public @Nullable String getChannelDescription(Bundle bundle, ChannelTypeUID channelTypeUID,
            @Nullable String defaultDescription, @Nullable Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultDescription, () -> inferChannelKey(channelTypeUID, "description"));
        return i18nProvider.getText(bundle, key, defaultDescription, locale);
    }

    public @Nullable String getChannelDescription(Bundle bundle, ThingTypeUID thingTypeUID, ChannelDefinition channel,
            @Nullable String defaultDescription, @Nullable Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultDescription,
                () -> inferThingTypeKey(thingTypeUID, channel, "description"));
        return i18nProvider.getText(bundle, key, defaultDescription, locale);
    }

    public @Nullable String getChannelGroupDescription(Bundle bundle, ChannelGroupTypeUID channelGroupTypeUID,
            @Nullable String defaultDescription, @Nullable Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultDescription,
                () -> inferChannelGroupKey(channelGroupTypeUID, "description"));
        return i18nProvider.getText(bundle, key, defaultDescription, locale);
    }

    public @Nullable String getChannelDescription(Bundle bundle, ChannelGroupTypeUID channelGroupTypeUID,
            ChannelDefinition channel, @Nullable String defaultLabel, @Nullable Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultLabel,
                () -> inferChannelGroupKey(channelGroupTypeUID, channel, "description"));
        return i18nProvider.getText(bundle, key, defaultLabel, locale);
    }

    public @Nullable String getChannelGroupDescription(Bundle bundle, ThingTypeUID thingTypeUID,
            ChannelGroupDefinition channelGroup, String defaultDescription, @Nullable Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultDescription,
                () -> inferThingTypeKey(thingTypeUID, channelGroup, "description"));
        return i18nProvider.getText(bundle, key, defaultDescription, locale);
    }

    public @Nullable String getChannelLabel(Bundle bundle, ChannelTypeUID channelTypeUID, String defaultLabel,
            @Nullable Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultLabel, () -> inferChannelKey(channelTypeUID, "label"));
        return i18nProvider.getText(bundle, key, defaultLabel, locale);
    }

    public @Nullable String getChannelLabel(Bundle bundle, ChannelGroupTypeUID channelGroupTypeUID,
            ChannelDefinition channel, @Nullable String defaultLabel, @Nullable Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultLabel,
                () -> inferChannelGroupKey(channelGroupTypeUID, channel, "label"));
        return i18nProvider.getText(bundle, key, defaultLabel, locale);
    }

    public @Nullable String getChannelLabel(Bundle bundle, ThingTypeUID thingTypeUID, ChannelDefinition channel,
            @Nullable String defaultLabel, @Nullable Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultLabel, () -> inferThingTypeKey(thingTypeUID, channel, "label"));
        return i18nProvider.getText(bundle, key, defaultLabel, locale);
    }

    public @Nullable String getChannelGroupLabel(Bundle bundle, ChannelGroupTypeUID channelGroupTypeUID,
            String defaultLabel, @Nullable Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultLabel, () -> inferChannelGroupKey(channelGroupTypeUID, "label"));
        return i18nProvider.getText(bundle, key, defaultLabel, locale);
    }

    public @Nullable String getChannelGroupLabel(Bundle bundle, ThingTypeUID thingTypeUID,
            ChannelGroupDefinition channelGroup, String defaultLabel, @Nullable Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultLabel,
                () -> inferThingTypeKey(thingTypeUID, channelGroup, "label"));
        return i18nProvider.getText(bundle, key, defaultLabel, locale);
    }

    public @Nullable String getChannelStateOption(Bundle bundle, ChannelTypeUID channelTypeUID, String optionValue,
            String defaultOptionLabel, @Nullable Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultOptionLabel,
                () -> inferChannelKey(channelTypeUID, "state.option." + optionValue));
        return i18nProvider.getText(bundle, key, defaultOptionLabel, locale);
    }

    public @Nullable String getChannelStatePattern(Bundle bundle, ChannelTypeUID channelTypeUID, String defaultPattern,
            @Nullable Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultPattern, () -> inferChannelKey(channelTypeUID, "state.pattern"));
        return i18nProvider.getText(bundle, key, defaultPattern, locale);
    }

    public @Nullable String getDescription(Bundle bundle, ThingTypeUID thingTypeUID,
            @Nullable String defaultDescription, @Nullable Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultDescription, () -> inferThingTypeKey(thingTypeUID, "description"));
        return i18nProvider.getText(bundle, key, defaultDescription, locale);
    }

    public @Nullable String getLabel(Bundle bundle, ThingTypeUID thingTypeUID, String defaultLabel,
            @Nullable Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultLabel, () -> inferThingTypeKey(thingTypeUID, "label"));
        return i18nProvider.getText(bundle, key, defaultLabel, locale);
    }

    private String inferChannelKey(ChannelTypeUID channelTypeUID, String lastSegment) {
        return "channel-type." + channelTypeUID.getBindingId() + "." + channelTypeUID.getId() + "." + lastSegment;
    }

    private String inferChannelGroupKey(ChannelGroupTypeUID channelGroupTypeUID, String lastSegment) {
        return "channel-group-type." + channelGroupTypeUID.getBindingId() + "." + channelGroupTypeUID.getId() + "."
                + lastSegment;
    }

    private String inferChannelGroupKey(ChannelGroupTypeUID channelGroupTypeUID, ChannelDefinition channel,
            String lastSegment) {
        return "channel-group-type." + channelGroupTypeUID.getBindingId() + "." + channelGroupTypeUID.getId()
                + ".channel." + channel.getId() + "." + lastSegment;
    }

    private String inferThingTypeKey(ThingTypeUID thingTypeUID, String lastSegment) {
        return "thing-type." + thingTypeUID.getBindingId() + "." + thingTypeUID.getId() + "." + lastSegment;
    }

    private String inferThingTypeKey(ThingTypeUID thingTypeUID, ChannelGroupDefinition channelGroup,
            String lastSegment) {
        return "thing-type." + thingTypeUID.getBindingId() + "." + thingTypeUID.getId() + ".group."
                + channelGroup.getId() + "." + lastSegment;
    }

    private String inferThingTypeKey(ThingTypeUID thingTypeUID, ChannelDefinition channel, String lastSegment) {
        return "thing-type." + thingTypeUID.getBindingId() + "." + thingTypeUID.getId() + ".channel." + channel.getId()
                + "." + lastSegment;
    }

}
