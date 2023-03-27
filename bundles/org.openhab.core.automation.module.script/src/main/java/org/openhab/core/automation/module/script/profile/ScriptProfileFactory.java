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
package org.openhab.core.automation.module.script.profile;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.ScriptTransformationService;
import org.openhab.core.automation.module.script.ScriptTransformationServiceFactory;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionBuilder;
import org.openhab.core.config.core.ConfigDescriptionProvider;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.ConfigOptionProvider;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.thing.profiles.Profile;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileFactory;
import org.openhab.core.thing.profiles.ProfileType;
import org.openhab.core.thing.profiles.ProfileTypeBuilder;
import org.openhab.core.thing.profiles.ProfileTypeProvider;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.transform.TransformationRegistry;
import org.openhab.core.transform.TransformationService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link ScriptProfileFactory} creates {@link ScriptProfile} instances
 *
 * @author Jan N. Klug - Initial contribution
 */
@Component(service = { ScriptProfileFactory.class, ProfileFactory.class, ProfileTypeProvider.class,
        ConfigDescriptionProvider.class, ConfigOptionProvider.class })
@NonNullByDefault
public class ScriptProfileFactory
        implements ProfileFactory, ProfileTypeProvider, ConfigOptionProvider, ConfigDescriptionProvider {
    public static final String PROFILE_CONFIG_URI_PREFIX = "profile:transform:";
    private static final URI CONFIG_DESCRIPTION_TEMPLATE_URI = URI.create(PROFILE_CONFIG_URI_PREFIX + "SCRIPT");

    private final TransformationRegistry transformationRegistry;
    private final ScriptTransformationServiceFactory scriptTransformationFactory;
    private final ConfigDescriptionRegistry configDescRegistry;

    @Activate
    public ScriptProfileFactory(final @Reference ScriptTransformationServiceFactory scriptTransformationFactory,
            final @Reference ConfigDescriptionRegistry configDescRegistry,
            final @Reference TransformationRegistry transformationRegistry) {
        this.transformationRegistry = transformationRegistry;
        this.scriptTransformationFactory = scriptTransformationFactory;
        this.configDescRegistry = configDescRegistry;
    }

    @Override
    public @Nullable Profile createProfile(ProfileTypeUID profileTypeUID, ProfileCallback callback,
            ProfileContext profileContext) {
        if (!profileTypeUID.getScope().equals(TransformationService.TRANSFORM_PROFILE_SCOPE)) {
            return null;
        }

        if (getSupportedProfileTypeUIDs().contains(profileTypeUID)) {
            String scriptType = profileTypeUID.getId().toLowerCase();
            ScriptTransformationService transformationService = scriptTransformationFactory
                    .getTransformationService(scriptType);
            return new ScriptProfile(profileTypeUID, callback, profileContext, transformationService);
        }
        return null;
    }

    @Override
    public Collection<ProfileTypeUID> getSupportedProfileTypeUIDs() {
        return getScriptTypes()
                .map(type -> new ProfileTypeUID(TransformationService.TRANSFORM_PROFILE_SCOPE, type.toUpperCase()))
                .toList();
    }

    @Override
    public Collection<ProfileType> getProfileTypes(@Nullable Locale locale) {
        return getSupportedProfileTypeUIDs().stream().map(uid -> ProfileTypeBuilder.newState(uid, uid.getId()).build())
                .collect(Collectors.toList());
    }

    @Override
    public Collection<ConfigDescription> getConfigDescriptions(@Nullable Locale locale) {
        ConfigDescription template = configDescRegistry.getConfigDescription(CONFIG_DESCRIPTION_TEMPLATE_URI, locale);
        if (template == null) {
            return Collections.emptyList();
        }

        return getScriptTypes().map(type -> ConfigDescriptionBuilder
                .create(URI.create(PROFILE_CONFIG_URI_PREFIX + type.toUpperCase()))
                .withParameters(template.getParameters()).withParameterGroups(template.getParameterGroups()).build())
                .toList();
    }

    private Stream<String> getScriptTypes() {
        return scriptTransformationFactory.getScriptTypes().stream();
    }

    private Set<String> getProfileConfigURIs() {
        return getScriptTypes().map(type -> PROFILE_CONFIG_URI_PREFIX + type.toUpperCase()).collect(Collectors.toSet());
    }

    /**
     * Provides a {@link ConfigDescription} for the given URI.
     *
     * @param uri uri of the config description
     * @param locale locale
     * @return config description or null if no config description could be found
     */
    @Override
    public @Nullable ConfigDescription getConfigDescription(URI uri, @Nullable Locale locale) {
        String uriString = uri.toString();
        if (!uriString.startsWith(PROFILE_CONFIG_URI_PREFIX) || !getProfileConfigURIs().contains(uriString)) {
            return null;
        }

        ConfigDescription template = configDescRegistry.getConfigDescription(CONFIG_DESCRIPTION_TEMPLATE_URI, locale);
        if (template == null) {
            return null;
        }
        return ConfigDescriptionBuilder.create(uri).withParameters(template.getParameters())
                .withParameterGroups(template.getParameterGroups()).build();
    }

    @Override
    public @Nullable Collection<ParameterOption> getParameterOptions(URI uri, String param, @Nullable String context,
            @Nullable Locale locale) {
        String uriString = uri.toString();
        if (!uriString.startsWith(PROFILE_CONFIG_URI_PREFIX) || !getProfileConfigURIs().contains(uriString)) {
            return null;
        }

        String[] uri_parts = uri.toString().split(":");
        if (uri_parts.length == 0) {
            return null;
        }

        String scriptType = uri_parts[uri_parts.length - 1].toLowerCase();

        if (ScriptProfile.CONFIG_TO_HANDLER_SCRIPT.equals(param) || ScriptProfile.CONFIG_TO_ITEM_SCRIPT.equals(param)) {
            var scriptTypes = scriptType.equals("dsl") ? List.of(scriptType, "rules") : List.of(scriptType);
            return transformationRegistry.getTransformations(scriptTypes).stream()
                    .map(c -> new ParameterOption(c.getUID(), c.getLabel())).collect(Collectors.toList());
        }
        return null;
    }
}
