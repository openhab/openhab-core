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
package org.openhab.core.transform.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.transform.ManagedTransformationConfigurationProvider;
import org.openhab.core.transform.TransformationConfiguration;

/**
 * The {@link TransformationConfigurationRegistryImplTest} includes tests for the
 * * {@link TransformationConfigurationRegistryImpl}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class TransformationConfigurationRegistryImplTest {
    private static final String SERVICE = "foo";

    private static final String MANAGED_WITHOUT_LANGUAGE_UID = "config:" + SERVICE + ":managed";
    private static final String MANAGED_WITH_EN_LANGUAGE_UID = "config:" + SERVICE + ":managed:en";
    private static final String MANAGED_WITH_DE_LANGUAGE_UID = "config:" + SERVICE + ":managed:de";

    private static final TransformationConfiguration MANAGED_WITHOUT_LANGUAGE = new TransformationConfiguration(
            MANAGED_WITHOUT_LANGUAGE_UID, "", SERVICE, null, MANAGED_WITHOUT_LANGUAGE_UID);
    private static final TransformationConfiguration MANAGED_WITH_EN_LANGUAGE = new TransformationConfiguration(
            MANAGED_WITH_EN_LANGUAGE_UID, "", SERVICE, "en", MANAGED_WITH_EN_LANGUAGE_UID);
    private static final TransformationConfiguration MANAGED_WITH_DE_LANGUAGE = new TransformationConfiguration(
            MANAGED_WITH_DE_LANGUAGE_UID, "", SERVICE, "de", MANAGED_WITH_DE_LANGUAGE_UID);

    private static final String LEGACY_WITHOUT_LANGUAGE_UID = "foo/legacy." + SERVICE;
    private static final String LEGACY_WITH_EN_LANGUAGE_UID = "foo/legacy_en." + SERVICE;
    private static final String LEGACY_WITH_DE_LANGUAGE_UID = "foo/legacy_de." + SERVICE;

    private static final TransformationConfiguration LEGACY_WITHOUT_LANGUAGE = new TransformationConfiguration(
            LEGACY_WITHOUT_LANGUAGE_UID, "", SERVICE, null, LEGACY_WITHOUT_LANGUAGE_UID);
    private static final TransformationConfiguration LEGACY_WITH_EN_LANGUAGE = new TransformationConfiguration(
            LEGACY_WITH_EN_LANGUAGE_UID, "", SERVICE, "en", LEGACY_WITH_EN_LANGUAGE_UID);
    private static final TransformationConfiguration LEGACY_WITH_DE_LANGUAGE = new TransformationConfiguration(
            LEGACY_WITH_DE_LANGUAGE_UID, "", SERVICE, "de", LEGACY_WITH_DE_LANGUAGE_UID);

    @Mock
    private @NonNullByDefault({}) LocaleProvider localeProvider;

    @Mock
    private @NonNullByDefault({}) ManagedTransformationConfigurationProvider provider;

    private @NonNullByDefault({}) TransformationConfigurationRegistryImpl registry;

    @BeforeEach
    public void setup() {
        Mockito.when(localeProvider.getLocale()).thenReturn(Locale.US);

        registry = new TransformationConfigurationRegistryImpl(localeProvider);
        registry.addProvider(provider);
        registry.added(provider, MANAGED_WITHOUT_LANGUAGE);
        registry.added(provider, MANAGED_WITH_EN_LANGUAGE);
        registry.added(provider, MANAGED_WITH_DE_LANGUAGE);
        registry.added(provider, LEGACY_WITHOUT_LANGUAGE);
        registry.added(provider, LEGACY_WITH_EN_LANGUAGE);
        registry.added(provider, LEGACY_WITH_DE_LANGUAGE);
    }

    @Test
    public void testManagedReturnsCorrectLanguage() {
        // language contained in uid, default requested (explicit uid takes precedence)
        assertEquals(MANAGED_WITH_DE_LANGUAGE, registry.get(MANAGED_WITH_DE_LANGUAGE_UID, null));
        // language contained in uid, other requested (explicit uid takes precedence)
        assertEquals(MANAGED_WITH_DE_LANGUAGE, registry.get(MANAGED_WITH_DE_LANGUAGE_UID, Locale.FRANCE));
        // no language in uid, default requested
        assertEquals(MANAGED_WITH_EN_LANGUAGE, registry.get(MANAGED_WITHOUT_LANGUAGE_UID, null));
        // no language in uid, other requested
        assertEquals(MANAGED_WITH_DE_LANGUAGE, registry.get(MANAGED_WITHOUT_LANGUAGE_UID, Locale.GERMANY));
        // no language in uid, unknown requested
        assertEquals(MANAGED_WITHOUT_LANGUAGE, registry.get(MANAGED_WITHOUT_LANGUAGE_UID, Locale.FRANCE));
    }

    @Test
    public void testLegacyReturnsCorrectLanguage() {
        // language contained in uid, default requested (explicit uid takes precedence)
        assertEquals(LEGACY_WITH_DE_LANGUAGE, registry.get(LEGACY_WITH_DE_LANGUAGE_UID, null));
        // language contained in uid, other requested (explicit uid takes precedence)
        assertEquals(LEGACY_WITH_DE_LANGUAGE, registry.get(LEGACY_WITH_DE_LANGUAGE_UID, Locale.FRANCE));
        // no language in uid, default requested
        assertEquals(LEGACY_WITH_EN_LANGUAGE, registry.get(LEGACY_WITHOUT_LANGUAGE_UID, null));
        // no language in uid, other requested
        assertEquals(LEGACY_WITH_DE_LANGUAGE, registry.get(LEGACY_WITHOUT_LANGUAGE_UID, Locale.GERMANY));
        // no language in uid, unknown requested
        assertEquals(LEGACY_WITHOUT_LANGUAGE, registry.get(LEGACY_WITHOUT_LANGUAGE_UID, Locale.FRANCE));
    }
}
