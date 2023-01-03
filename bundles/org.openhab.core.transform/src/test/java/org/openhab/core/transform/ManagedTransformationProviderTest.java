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
package org.openhab.core.transform;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.openhab.core.transform.Transformation.FUNCTION;

import java.util.Map;

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
 * The {@link ManagedTransformationProviderTest} includes tests for the
 * {@link ManagedTransformationProvider}
 *
 * @author Jan N. Klug - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class ManagedTransformationProviderTest {

    private @Mock @NonNullByDefault({}) ProviderChangeListener<@NonNull Transformation> listenerMock;

    private @NonNullByDefault({}) ManagedTransformationProvider provider;

    @BeforeEach
    public void setup() {
        VolatileStorageService storageService = new VolatileStorageService();
        provider = new ManagedTransformationProvider(storageService);
        provider.addProviderChangeListener(listenerMock);
    }

    @Test
    public void testValidConfigurationsAreAdded() {
        Transformation withoutLanguage = new Transformation("config:foo:identifier", "", "foo",
                Map.of(FUNCTION, "content"));
        provider.add(withoutLanguage);

        Transformation withLanguage = new Transformation("config:foo:identifier:de", "", "foo",
                Map.of(FUNCTION, "content"));
        provider.add(withLanguage);

        Mockito.verify(listenerMock).added(provider, withoutLanguage);
        Mockito.verify(listenerMock).added(provider, withLanguage);
    }

    @Test
    public void testValidConfigurationsIsUpdated() {
        Transformation configuration = new Transformation("config:foo:identifier", "", "foo",
                Map.of(FUNCTION, "content"));
        Transformation updatedConfiguration = new Transformation("config:foo:identifier", "", "foo",
                Map.of(FUNCTION, "updated"));

        provider.add(configuration);
        provider.update(updatedConfiguration);

        Mockito.verify(listenerMock).added(provider, configuration);
        Mockito.verify(listenerMock).updated(provider, configuration, updatedConfiguration);
    }

    @Test
    public void testUidFormatValidation() {
        Transformation inValidUid = new Transformation("invalid:foo:identifier", "", "foo",
                Map.of(FUNCTION, "content"));

        assertThrows(IllegalArgumentException.class, () -> provider.add(inValidUid));
    }

    @Test
    public void testTypeValidation() {
        Transformation typeNotMatching = new Transformation("config:foo:identifier", "", "bar",
                Map.of(FUNCTION, "content"));

        assertThrows(IllegalArgumentException.class, () -> provider.add(typeNotMatching));
    }

    @Test
    public void testSerializationDeserializationResultsInSameConfiguration() {
        Transformation configuration = new Transformation("config:foo:identifier", "", "foo",
                Map.of(FUNCTION, "content"));
        provider.add(configuration);

        Transformation configuration1 = provider.get("config:foo:identifier");

        assertThat(configuration, is(configuration1));
    }
}
