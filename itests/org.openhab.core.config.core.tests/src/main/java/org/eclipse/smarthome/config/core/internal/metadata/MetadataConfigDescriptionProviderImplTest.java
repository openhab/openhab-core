/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.config.core.internal.metadata;

import static org.eclipse.smarthome.config.core.internal.metadata.MetadataConfigDescriptionProviderImpl.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter.Type;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameterBuilder;
import org.eclipse.smarthome.config.core.ParameterOption;
import org.eclipse.smarthome.config.core.internal.metadata.MetadataConfigDescriptionProviderImpl;
import org.eclipse.smarthome.config.core.metadata.MetadataConfigDescriptionProvider;
import org.eclipse.smarthome.test.java.JavaTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 *
 * @author Simon Kaufmann - initial contribution and API
 *
 */
public class MetadataConfigDescriptionProviderImplTest extends JavaTest {

    private static final String LIBERAL = "liberal";
    private static final String RESTRICTED = "restricted";

    private static final URI URI_RESTRICTED = URI.create(SCHEME + SEPARATOR + RESTRICTED);
    private static final URI URI_LIBERAL = URI.create(SCHEME + SEPARATOR + LIBERAL);

    private static final URI URI_RESTRICTED_DIMMER = URI.create(SCHEME + SEPARATOR + RESTRICTED + SEPARATOR + "dimmer");

    private @Mock MetadataConfigDescriptionProvider mockProviderRestricted;
    private @Mock MetadataConfigDescriptionProvider mockProviderLiberal;

    private MetadataConfigDescriptionProviderImpl service;

    @Before
    public void setup() {
        initMocks(this);
        service = new MetadataConfigDescriptionProviderImpl();

        when(mockProviderRestricted.getNamespace()).thenReturn(RESTRICTED);
        when(mockProviderRestricted.getDescription(any())).thenReturn("Restricted");
        when(mockProviderRestricted.getParameterOptions(any())).thenReturn(Arrays.asList( //
                new ParameterOption("dimmer", "Dimmer"), //
                new ParameterOption("switch", "Switch") //
        ));
        when(mockProviderRestricted.getParameters(eq("dimmer"), any())).thenReturn(Arrays.asList( //
                ConfigDescriptionParameterBuilder.create("width", Type.INTEGER).build(), //
                ConfigDescriptionParameterBuilder.create("height", Type.INTEGER).build() //
        ));

        when(mockProviderLiberal.getNamespace()).thenReturn(LIBERAL);
        when(mockProviderLiberal.getDescription(any())).thenReturn("Liberal");
        when(mockProviderLiberal.getParameterOptions(any())).thenReturn(null);
    }

    @Test
    public void testGetConfigDescriptions_noOptions() {
        service.addMetadataConfigDescriptionProvider(mockProviderLiberal);

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
    public void testGetConfigDescriptions_withOptions() {
        service.addMetadataConfigDescriptionProvider(mockProviderRestricted);

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
    public void testGetConfigDescription_wrongScheme() {
        service.addMetadataConfigDescriptionProvider(mockProviderRestricted);
        service.addMetadataConfigDescriptionProvider(mockProviderLiberal);

        assertNull(service.getConfigDescription(URI.create("some:nonsense"), null));
    }

    @Test
    public void testGetConfigDescription_valueDescription() {
        service.addMetadataConfigDescriptionProvider(mockProviderRestricted);
        service.addMetadataConfigDescriptionProvider(mockProviderLiberal);

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
    public void testGetConfigDescription_valueDescriptionNonExistingNamespace() {
        service.addMetadataConfigDescriptionProvider(mockProviderRestricted);
        service.addMetadataConfigDescriptionProvider(mockProviderLiberal);

        ConfigDescription desc = service.getConfigDescription(URI.create("metadata:nonsense"), null);
        assertNull(desc);
    }

    @Test
    public void testGetConfigDescription_propertiesDescription() {
        service.addMetadataConfigDescriptionProvider(mockProviderRestricted);
        service.addMetadataConfigDescriptionProvider(mockProviderLiberal);

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
    public void testGetConfigDescription_propertiesDescriptionNonExistingNamespace() {
        service.addMetadataConfigDescriptionProvider(mockProviderRestricted);
        service.addMetadataConfigDescriptionProvider(mockProviderLiberal);

        ConfigDescription desc = service.getConfigDescription(URI.create("metadata:nonsense:nonsense"), null);
        assertNull(desc);
    }

    @Test
    public void testGetConfigDescription_propertiesDescriptionNonExistingValue() {
        service.addMetadataConfigDescriptionProvider(mockProviderRestricted);
        service.addMetadataConfigDescriptionProvider(mockProviderLiberal);

        ConfigDescription desc = service.getConfigDescription(URI.create("metadata:foo:nonsense"), null);
        assertNull(desc);
    }

}
