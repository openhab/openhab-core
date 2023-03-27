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

import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.ScriptTransformationService;
import org.openhab.core.automation.module.script.ScriptTransformationServiceFactory;
import org.openhab.core.thing.profiles.Profile;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileFactory;
import org.openhab.core.thing.profiles.ProfileType;
import org.openhab.core.thing.profiles.ProfileTypeBuilder;
import org.openhab.core.thing.profiles.ProfileTypeProvider;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.transform.TransformationService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ScriptProfileFactory} creates {@link ScriptProfile} instances
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(service = { ProfileFactory.class, ProfileTypeProvider.class })
public class ScriptProfileFactory implements ProfileFactory, ProfileTypeProvider {
    private final Logger logger = LoggerFactory.getLogger(ScriptProfileFactory.class);

    private final ScriptTransformationServiceFactory scriptTransformationFactory;

    @Activate
    public ScriptProfileFactory(final @Reference ScriptTransformationServiceFactory scriptTransformationFactory) {
        this.scriptTransformationFactory = scriptTransformationFactory;
    }

    @Override
    public @Nullable Profile createProfile(ProfileTypeUID profileTypeUID, ProfileCallback callback,
            ProfileContext profileContext) {
        String scriptType = profileTypeUID.getId().toLowerCase();
        ScriptTransformationService transformationService = scriptTransformationFactory
                .getTransformationService(scriptType);
        return new ScriptProfile(profileTypeUID, callback, profileContext, transformationService);
    }

    @Override
    public Collection<ProfileTypeUID> getSupportedProfileTypeUIDs() {
        return getScriptTypes()
                .map(type -> new ProfileTypeUID(TransformationService.TRANSFORM_PROFILE_SCOPE, type.toUpperCase()))
                .toList();
    }

    @Override
    public Collection<ProfileType> getProfileTypes(@Nullable Locale locale) {
        return getSupportedProfileTypeUIDs().stream().map(uid -> {
            String id = uid.getId();
            String label = Optional.ofNullable(scriptTransformationFactory.getLanguageName(id.toLowerCase()))
                    .map(name -> id + " - " + name).orElse(id);
            return ProfileTypeBuilder.newState(uid, label).build();
        }).collect(Collectors.toList());
    }

    private Stream<String> getScriptTypes() {
        return scriptTransformationFactory.getScriptTypes();
    }
}
