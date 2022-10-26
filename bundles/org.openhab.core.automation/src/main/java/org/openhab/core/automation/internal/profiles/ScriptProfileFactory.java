/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.core.automation.internal.profiles;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.RuleManager;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.i18n.LocalizedKey;
import org.openhab.core.thing.profiles.Profile;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileFactory;
import org.openhab.core.thing.profiles.ProfileType;
import org.openhab.core.thing.profiles.ProfileTypeBuilder;
import org.openhab.core.thing.profiles.ProfileTypeProvider;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.i18n.ProfileTypeI18nLocalizationService;
import org.openhab.core.util.BundleResolver;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * ProfileFactory that creates the script profile.
 *
 * @author Cody Cutrer - initial contribution
 *
 */
@NonNullByDefault
@Component(service = { ProfileFactory.class, ProfileTypeProvider.class })
public class ScriptProfileFactory implements ProfileFactory, ProfileTypeProvider {

    private final ProfileTypeI18nLocalizationService profileTypeI18nLocalizationService;
    private final RuleManager ruleManager;
    private final RuleRegistry ruleRegistry;

    private final Map<LocalizedKey, ProfileType> localizedProfileTypeCache = new ConcurrentHashMap<>();
    private final Bundle bundle;

    @Activate
    public ScriptProfileFactory(final @Reference ProfileTypeI18nLocalizationService profileTypeI18nLocalizationService,
            final @Reference RuleManager ruleManager, final @Reference RuleRegistry ruleRegistry,
            final @Reference BundleResolver bundleResolver) {
        this.profileTypeI18nLocalizationService = profileTypeI18nLocalizationService;
        this.ruleManager = ruleManager;
        this.ruleRegistry = ruleRegistry;
        this.bundle = bundleResolver.resolveBundle(ScriptProfileFactory.class);
    }

    @Override
    public Collection<ProfileType> getProfileTypes(@Nullable Locale locale) {
        return List.of(ProfileTypeBuilder
                .newState(ScriptProfile.PROFILE_TYPE_UID, ScriptProfile.PROFILE_TYPE_UID.getId()).build()).stream()
                .map(p -> createLocalizedProfileType(p, locale)).collect(Collectors.toList());
    }

    @Override
    public @Nullable Profile createProfile(ProfileTypeUID profileTypeUID, ProfileCallback callback,
            ProfileContext profileContext) {
        return new ScriptProfile(callback, profileContext, ruleManager, ruleRegistry);
    }

    @Override
    public Collection<ProfileTypeUID> getSupportedProfileTypeUIDs() {
        return List.of(ScriptProfile.PROFILE_TYPE_UID);
    }

    private ProfileType createLocalizedProfileType(ProfileType profileType, @Nullable Locale locale) {
        LocalizedKey localizedKey = new LocalizedKey(profileType.getUID(),
                locale != null ? locale.toLanguageTag() : null);

        ProfileType cachedEntry = localizedProfileTypeCache.get(localizedKey);
        if (cachedEntry != null) {
            return cachedEntry;
        }

        ProfileType localizedProfileType = profileTypeI18nLocalizationService.createLocalizedProfileType(bundle,
                profileType, locale);
        if (localizedProfileType != null) {
            localizedProfileTypeCache.put(localizedKey, localizedProfileType);
            return localizedProfileType;
        } else {
            return profileType;
        }
    }
}
