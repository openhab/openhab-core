/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.thing.internal.profiles.i18n;

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.i18n.I18nUtil;
import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.thing.profiles.Profile;
import org.openhab.core.thing.profiles.ProfileType;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.osgi.framework.Bundle;

/**
 * A utility service which localizes {@link Profile}s.
 * Falls back to a localized {@link ProfileType} for label and description when not given otherwise.
 *
 * @see {@link ProfileTypeI18nLocalizationService}
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class ProfileI18nUtil {

    private final TranslationProvider i18nProvider;

    /**
     * Create a new util instance and pass the appropriate dependencies.
     *
     * @param i18nProvider an instance of {@link TranslationProvider}.
     */
    public ProfileI18nUtil(TranslationProvider i18nProvider) {
        this.i18nProvider = i18nProvider;
    }

    public @Nullable String getProfileLabel(Bundle bundle, ProfileTypeUID profileTypeUID, String defaultLabel,
            @Nullable Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultLabel, () -> inferProfileTypeKey(profileTypeUID, "label"));
        return i18nProvider.getText(bundle, key, defaultLabel, locale);
    }

    private String inferProfileTypeKey(ProfileTypeUID profileTypeUID, String lastSegment) {
        return "profile-type." + profileTypeUID.getBindingId() + "." + profileTypeUID.getId() + "." + lastSegment;
    }
}
