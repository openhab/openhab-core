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
package org.openhab.core.transform.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.openhab.core.transform.Transformation.FUNCTION;

import java.util.Locale;
import java.util.Map;

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
import org.openhab.core.transform.ManagedTransformationProvider;
import org.openhab.core.transform.Transformation;

/**
 * The {@link TransformationRegistryImplTest} includes tests for the
 * {@link TransformationRegistryImpl}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TransformationRegistryImplTest {
    private static final String SERVICE = "foo";

    private static final String MANAGED_WITHOUT_LANGUAGE_UID = "config:" + SERVICE + ":managed";
    private static final String MANAGED_WITH_EN_LANGUAGE_UID = "config:" + SERVICE + ":managed:en";
    private static final String MANAGED_WITH_DE_LANGUAGE_UID = "config:" + SERVICE + ":managed:de";

    private static final Transformation MANAGED_WITHOUT_LANGUAGE = new Transformation(MANAGED_WITHOUT_LANGUAGE_UID, "",
            SERVICE, Map.of(FUNCTION, MANAGED_WITHOUT_LANGUAGE_UID));
    private static final Transformation MANAGED_WITH_EN_LANGUAGE = new Transformation(MANAGED_WITH_EN_LANGUAGE_UID, "",
            SERVICE, Map.of(FUNCTION, MANAGED_WITH_EN_LANGUAGE_UID));
    private static final Transformation MANAGED_WITH_DE_LANGUAGE = new Transformation(MANAGED_WITH_DE_LANGUAGE_UID, "",
            SERVICE, Map.of(FUNCTION, MANAGED_WITH_DE_LANGUAGE_UID));

    private static final String FILE_WITHOUT_LANGUAGE_UID = "foo/FILE." + SERVICE;
    private static final String FILE_WITH_EN_LANGUAGE_UID = "foo/FILE_en." + SERVICE;
    private static final String FILE_WITH_DE_LANGUAGE_UID = "foo/FILE_de." + SERVICE;

    private static final Transformation FILE_WITHOUT_LANGUAGE = new Transformation(FILE_WITHOUT_LANGUAGE_UID, "",
            SERVICE, Map.of(FUNCTION, FILE_WITHOUT_LANGUAGE_UID));
    private static final Transformation FILE_WITH_EN_LANGUAGE = new Transformation(FILE_WITH_EN_LANGUAGE_UID, "",
            SERVICE, Map.of(FUNCTION, FILE_WITH_EN_LANGUAGE_UID));
    private static final Transformation FILE_WITH_DE_LANGUAGE = new Transformation(FILE_WITH_DE_LANGUAGE_UID, "",
            SERVICE, Map.of(FUNCTION, FILE_WITH_DE_LANGUAGE_UID));

    private @Mock @NonNullByDefault({}) LocaleProvider localeProviderMock;

    private @Mock @NonNullByDefault({}) ManagedTransformationProvider providerMock;

    private @NonNullByDefault({}) TransformationRegistryImpl registry;

    @BeforeEach
    public void setup() {
        Mockito.when(localeProviderMock.getLocale()).thenReturn(Locale.US);

        registry = new TransformationRegistryImpl(localeProviderMock);
        registry.addProvider(providerMock);
        registry.added(providerMock, MANAGED_WITHOUT_LANGUAGE);
        registry.added(providerMock, MANAGED_WITH_EN_LANGUAGE);
        registry.added(providerMock, MANAGED_WITH_DE_LANGUAGE);
        registry.added(providerMock, FILE_WITHOUT_LANGUAGE);
        registry.added(providerMock, FILE_WITH_EN_LANGUAGE);
        registry.added(providerMock, FILE_WITH_DE_LANGUAGE);
    }

    @Test
    public void testManagedReturnsCorrectLanguage() {
        // language contained in uid, default requested (explicit uid takes precedence)
        assertThat(registry.get(MANAGED_WITH_DE_LANGUAGE_UID, null), is(MANAGED_WITH_DE_LANGUAGE));
        // language contained in uid, other requested (explicit uid takes precedence)
        assertThat(registry.get(MANAGED_WITH_DE_LANGUAGE_UID, Locale.FRANCE), is(MANAGED_WITH_DE_LANGUAGE));
        // no language in uid, default requested
        assertThat(registry.get(MANAGED_WITHOUT_LANGUAGE_UID, null), is(MANAGED_WITH_EN_LANGUAGE));
        // no language in uid, other requested
        assertThat(registry.get(MANAGED_WITHOUT_LANGUAGE_UID, Locale.GERMANY), is(MANAGED_WITH_DE_LANGUAGE));
        // no language in uid, unknown requested
        assertThat(registry.get(MANAGED_WITHOUT_LANGUAGE_UID, Locale.FRANCE), is(MANAGED_WITHOUT_LANGUAGE));
    }

    @Test
    public void testFileReturnsCorrectLanguage() {
        // language contained in uid, default requested (explicit uid takes precedence)
        assertThat(registry.get(FILE_WITH_DE_LANGUAGE_UID, null), is(FILE_WITH_DE_LANGUAGE));
        // language contained in uid, other requested (explicit uid takes precedence)
        assertThat(registry.get(FILE_WITH_DE_LANGUAGE_UID, Locale.FRANCE), is(FILE_WITH_DE_LANGUAGE));
        // no language in uid, default requested
        assertThat(registry.get(FILE_WITHOUT_LANGUAGE_UID, null), is(FILE_WITH_EN_LANGUAGE));
        // no language in uid, other requested
        assertThat(registry.get(FILE_WITHOUT_LANGUAGE_UID, Locale.GERMANY), is(FILE_WITH_DE_LANGUAGE));
        // no language in uid, unknown requested
        assertThat(registry.get(FILE_WITHOUT_LANGUAGE_UID, Locale.FRANCE), is(FILE_WITHOUT_LANGUAGE));
    }
}
