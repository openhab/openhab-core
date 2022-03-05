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
package org.openhab.core.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.test.storage.VolatileStorageService;

/**
 * The {@link ManagedTransformationConfigurationProviderTest} includes tests for the
 * {@link org.openhab.core.transform.ManagedTransformationConfigurationProvider}
 *
 * @author Jan N. Klug - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
@NonNullByDefault
public class ManagedTransformationConfigurationProviderTest {

    @Mock
    private @NonNullByDefault({}) ProviderChangeListener<@NonNull TransformationConfiguration> listener;

    private @NonNullByDefault({}) ManagedTransformationConfigurationProvider provider;

    @BeforeEach
    public void setup() {
        VolatileStorageService storageService = new VolatileStorageService();
        provider = new ManagedTransformationConfigurationProvider(storageService);
        provider.addProviderChangeListener(listener);
    }

    @Test
    public void testValidConfigurationsAreAdded() {
        TransformationConfiguration withoutLanguage = new TransformationConfiguration("config:foo:identifier", "",
                "foo", null, "content");
        provider.add(withoutLanguage);

        TransformationConfiguration withLanguage = new TransformationConfiguration("config:foo:identifier:de", "",
                "foo", "de", "content");
        provider.add(withLanguage);

        Mockito.verify(listener).added(provider, withoutLanguage);
        Mockito.verify(listener).added(provider, withLanguage);
    }

    @Test
    public void testValidConfigurationsIsUpdated() {
        TransformationConfiguration configuration = new TransformationConfiguration("config:foo:identifier", "", "foo",
                null, "content");
        TransformationConfiguration updatedConfiguration = new TransformationConfiguration("config:foo:identifier", "",
                "foo", null, "updated");

        provider.add(configuration);
        provider.update(updatedConfiguration);

        Mockito.verify(listener).added(provider, configuration);
        Mockito.verify(listener).updated(provider, configuration, updatedConfiguration);
    }

    @Test
    public void testUidFormatValidation() {
        TransformationConfiguration inValidUid = new TransformationConfiguration("invalid:foo:identifier", "", "foo",
                null, "content");

        assertThrows(IllegalArgumentException.class, () -> provider.add(inValidUid));
    }

    @Test
    public void testLanguageValidations() {
        TransformationConfiguration languageMissingInUid = new TransformationConfiguration("config:foo:identifier", "",
                "foo", "de", "content");

        assertThrows(IllegalArgumentException.class, () -> provider.add(languageMissingInUid));

        TransformationConfiguration languageMissingInConfiguration = new TransformationConfiguration(
                "config:foo:identifier:de", "", "foo", null, "content");

        assertThrows(IllegalArgumentException.class, () -> provider.add(languageMissingInConfiguration));

        TransformationConfiguration languageNotMatching = new TransformationConfiguration("config:foo:identifier:en",
                "", "foo", "de", "content");

        assertThrows(IllegalArgumentException.class, () -> provider.add(languageNotMatching));
    }

    @Test
    public void testTypeValidation() {
        TransformationConfiguration typeNotMatching = new TransformationConfiguration("config:foo:identifier", "",
                "bar", null, "content");

        assertThrows(IllegalArgumentException.class, () -> provider.add(typeNotMatching));
    }

    @Test
    public void testSerializationDeserializationResultsInSameConfiguration() {
        TransformationConfiguration configuration = new TransformationConfiguration("config:foo:identifier", "", "foo",
                null, "content");
        provider.add(configuration);

        TransformationConfiguration configuration1 = provider.get("config:foo:identifier");

        assertEquals(configuration, configuration1);
    }
}
