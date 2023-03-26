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
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.ScriptTransformationService;
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

/**
 * The {@link ScriptProfileFactory} creates {@link ScriptProfile} instances
 *
 * @author Jan N. Klug - Initial contribution
 */
@Component(service = { ScriptProfileFactory.class, ProfileFactory.class, ProfileTypeProvider.class })
@NonNullByDefault
public class ScriptProfileFactory implements ProfileFactory, ProfileTypeProvider {

    public static final ProfileTypeUID SCRIPT_PROFILE_UID = new ProfileTypeUID(
            TransformationService.TRANSFORM_PROFILE_SCOPE, "SCRIPT");

    private static final ProfileType PROFILE_TYPE_SCRIPT = ProfileTypeBuilder.newState(SCRIPT_PROFILE_UID, "Script")
            .build();

    private final ScriptTransformationService transformationService;

    @Activate
    public ScriptProfileFactory(final @Reference ScriptTransformationService transformationService) {
        this.transformationService = transformationService;
    }

    @Override
    public @Nullable Profile createProfile(ProfileTypeUID profileTypeUID, ProfileCallback callback,
            ProfileContext profileContext) {
        if (SCRIPT_PROFILE_UID.equals(profileTypeUID)) {
            return new ScriptProfile(callback, profileContext, transformationService);
        }
        return null;
    }

    @Override
    public Collection<ProfileTypeUID> getSupportedProfileTypeUIDs() {
        return Set.of(SCRIPT_PROFILE_UID);
    }

    @Override
    public Collection<ProfileType> getProfileTypes(@Nullable Locale locale) {
        return Set.of(PROFILE_TYPE_SCRIPT);
    }
}
