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
package org.openhab.core.config.core.internal.metadata;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.openhab.core.config.core.internal.metadata.MetadataConfigDescriptionProviderImpl.*;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.config.core.ConfigDescriptionParameterBuilder;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.config.core.metadata.MetadataConfigDescriptionProvider;
import org.openhab.core.test.java.JavaTest;

/**
 *
 * @author Simon Kaufmann - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class MetadataConfigDescriptionProviderImplTest extends JavaTest {

    private static final String LIBERAL = "liberal";
    private static final String RESTRICTED = "restricted";

    private static final URI URI_RESTRICTED = URI.create(SCHEME + SEPARATOR + RESTRICTED);
    private static final URI URI_LIBERAL = URI.create(SCHEME + SEPARATOR + LIBERAL);

    private static final URI URI_RESTRICTED_DIMMER = URI.create(SCHEME + SEPARATOR + RESTRICTED + SEPARATOR + "dimmer");

    private @Mock @NonNullByDefault({}) MetadataConfigDescriptionProvider providerRestrictedMock;
    private @Mock @NonNullByDefault({}) MetadataConfigDescriptionProvider providerLiberalMock;

    private MetadataConfigDescriptionProviderImpl service = new MetadataConfigDescriptionProviderImpl();

    @BeforeEach
    public void setup() {
        service = new MetadataConfigDescriptionProviderImpl();

        when(providerRestrictedMock.getNamespace()).thenReturn(RESTRICTED);
        when(providerRestrictedMock.getDescription(any())).thenReturn("Restricted");
        when(providerRestrictedMock.getParameterOptions(any())).thenReturn(List.of( //
                new ParameterOption("dimmer", "Dimmer"), //
                new ParameterOption("switch", "Switch") //
        ));
        when(providerRestrictedMock.getParameters(eq("dimmer"), any())).thenReturn(List.of( //
                ConfigDescriptionParameterBuilder.create("width", Type.INTEGER).build(), //
                ConfigDescriptionParameterBuilder.create("height", Type.INTEGER).build() //
        ));

        when(providerLiberalMock.getNamespace()).thenReturn(LIBERAL);
        when(providerLiberalMock.getDescription(any())).thenReturn("Liberal");
        when(providerLiberalMock.getParameterOptions(any())).thenReturn(null);
    }

    @Test
    public void testGetConfigDescriptionsNoOptions() {
        service.addMetadataConfigDescriptionProvider(providerLiberalMock);

        Collection<ConfigDescription> res = service.getConfigDescriptions(Locale.ENGLISH);
        assertNotNull(res);
        assertEquals(1, res.size());

        ConfigDescription desc = res.iterator().next();
        assertEquals(URI_LIBERAL, desc.getUID());
        assertEquals(1, desc.getParameters().size());

        ConfigDescriptionParameter param = desc.getParameters().get(0);
        assertEquals("value", param.getName());
        assertEquals("Liberal", param.getDescription());
        assertFalse(param.getLimitToOptions());
    }

    @Test
    public void testGetConfigDescriptionsWithOptions() {
        service.addMetadataConfigDescriptionProvider(providerRestrictedMock);

        Collection<ConfigDescription> res = service.getConfigDescriptions(Locale.ENGLISH);
        assertNotNull(res);
        assertEquals(1, res.size());

        ConfigDescription desc = res.iterator().next();
        assertEquals(URI_RESTRICTED, desc.getUID());
        assertEquals(1, desc.getParameters().size());

        ConfigDescriptionParameter param = desc.getParameters().get(0);
        assertEquals("value", param.getName());
        assertEquals("Restricted", param.getDescription());
        assertTrue(param.getLimitToOptions());
        assertEquals("dimmer", param.getOptions().get(0).getValue());
        assertEquals("switch", param.getOptions().get(1).getValue());
    }

    @Test
    public void testGetConfigDescriptionWrongScheme() {
        service.addMetadataConfigDescriptionProvider(providerRestrictedMock);
        service.addMetadataConfigDescriptionProvider(providerLiberalMock);

        assertNull(service.getConfigDescription(URI.create("some:nonsense"), null));
    }

    @Test
    public void testGetConfigDescriptionValueDescription() {
        service.addMetadataConfigDescriptionProvider(providerRestrictedMock);
        service.addMetadataConfigDescriptionProvider(providerLiberalMock);

        ConfigDescription desc = service.getConfigDescription(URI_LIBERAL, null);
        assertNotNull(desc);
        assertEquals(URI_LIBERAL, desc.getUID());
        assertEquals(1, desc.getParameters().size());

        ConfigDescriptionParameter param = desc.getParameters().get(0);
        assertEquals("value", param.getName());
        assertEquals("Liberal", param.getDescription());
        assertFalse(param.getLimitToOptions());
    }

    @Test
    public void testGetConfigDescriptionValueDescriptionNonExistingNamespace() {
        service.addMetadataConfigDescriptionProvider(providerRestrictedMock);
        service.addMetadataConfigDescriptionProvider(providerLiberalMock);

        ConfigDescription desc = service.getConfigDescription(URI.create("metadata:nonsense"), null);
        assertNull(desc);
    }

    @Test
    public void testGetConfigDescriptionPropertiesDescription() {
        service.addMetadataConfigDescriptionProvider(providerRestrictedMock);
        service.addMetadataConfigDescriptionProvider(providerLiberalMock);

        ConfigDescription desc = service.getConfigDescription(URI_RESTRICTED_DIMMER, null);
        assertNotNull(desc);
        assertEquals(URI_RESTRICTED_DIMMER, desc.getUID());
        assertEquals(2, desc.getParameters().size());

        ConfigDescriptionParameter paramWidth = desc.getParameters().get(0);
        assertEquals("width", paramWidth.getName());

        ConfigDescriptionParameter paramHeight = desc.getParameters().get(1);
        assertEquals("height", paramHeight.getName());
    }

    @Test
    public void testGetConfigDescriptionPropertiesDescriptionNonExistingNamespace() {
        service.addMetadataConfigDescriptionProvider(providerRestrictedMock);
        service.addMetadataConfigDescriptionProvider(providerLiberalMock);

        ConfigDescription desc = service.getConfigDescription(URI.create("metadata:nonsense:nonsense"), null);
        assertNull(desc);
    }

    @Test
    public void testGetConfigDescriptionPropertiesDescriptionNonExistingValue() {
        service.addMetadataConfigDescriptionProvider(providerRestrictedMock);
        service.addMetadataConfigDescriptionProvider(providerLiberalMock);

        ConfigDescription desc = service.getConfigDescription(URI.create("metadata:foo:nonsense"), null);
        assertNull(desc);
    }
}
