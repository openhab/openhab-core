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
package org.openhab.core.thing.profiles.i18n;

import static org.junit.Assert.*;
import static org.openhab.core.thing.profiles.SystemProfiles.*;

import java.util.Collection;
import java.util.Locale;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.internal.profiles.SystemProfileFactory;
import org.openhab.core.thing.profiles.ProfileType;
import org.openhab.core.thing.profiles.ProfileTypeProvider;

/**
 * Test cases for i18n of the {@link SystemProfileFactory} class.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
public class SystemProfileI18nOSGiTest extends JavaOSGiTest {

    private ProfileTypeProvider systemProfileTypeProvider;

    @Before
    public void setUp() {
        ProfileTypeProvider provider = getService(ProfileTypeProvider.class, SystemProfileFactory.class);
        assertTrue(provider instanceof SystemProfileFactory);
        systemProfileTypeProvider = provider;
    }

    @Test
    public void systemProfileTypesShouldHaveOriginalLabel() {
        Collection<ProfileType> localizedProfileTypes = systemProfileTypeProvider.getProfileTypes(Locale.ENGLISH);

        Optional<ProfileType> defaultProfileType = localizedProfileTypes.stream()
                .filter(it -> DEFAULT.equals(it.getUID())).findFirst();
        assertTrue(defaultProfileType.isPresent());
        assertEquals("Default", defaultProfileType.get().getLabel());

        Optional<ProfileType> followProfileType = localizedProfileTypes.stream()
                .filter(it -> FOLLOW.equals(it.getUID())).findFirst();
        assertTrue(followProfileType.isPresent());
        assertEquals("Follow", followProfileType.get().getLabel());

        Optional<ProfileType> offsetProfileType = localizedProfileTypes.stream()
                .filter(it -> OFFSET.equals(it.getUID())).findFirst();
        assertTrue(offsetProfileType.isPresent());
        assertEquals("Offset", offsetProfileType.get().getLabel());
    }

    @Test
    public void systemProfileTypesShouldHaveTranslatedLabel() {
        Collection<ProfileType> localizedProfileTypes = systemProfileTypeProvider.getProfileTypes(Locale.GERMAN);

        Optional<ProfileType> defaultProfileType = localizedProfileTypes.stream()
                .filter(it -> DEFAULT.equals(it.getUID())).findFirst();
        assertTrue(defaultProfileType.isPresent());
        assertEquals("Standard", defaultProfileType.get().getLabel());

        Optional<ProfileType> followProfileType = localizedProfileTypes.stream()
                .filter(it -> FOLLOW.equals(it.getUID())).findFirst();
        assertTrue(followProfileType.isPresent());
        assertEquals("Folgen", followProfileType.get().getLabel());

        Optional<ProfileType> offsetProfileType = localizedProfileTypes.stream()
                .filter(it -> OFFSET.equals(it.getUID())).findFirst();
        assertTrue(offsetProfileType.isPresent());
        assertEquals("Versatz", offsetProfileType.get().getLabel());
    }
}
