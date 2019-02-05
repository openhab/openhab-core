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
package org.eclipse.smarthome.io.rest.core.internal.item;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.ConfigDescriptionRegistry;
import org.eclipse.smarthome.core.items.MetadataRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * Test the {@link MetadataSelectorMatcher}.
 *
 * @author Henning Treu - initial contribution
 *
 */
public class MetadataSelectorMatcherTest {

    private MetadataSelectorMatcher matcher;

    @Mock
    private ConfigDescriptionRegistry configDescriptionRegistry;

    @Mock
    private MetadataRegistry metadataRegistry;

    @Before
    public void setup() throws Exception {
        initMocks(this);

        when(configDescriptionRegistry.getConfigDescriptions(null)).thenReturn(mockConfigDescriptions());
        when(metadataRegistry.isInternalNamespace(anyString())).thenReturn(false);

        matcher = new MetadataSelectorMatcher();

        matcher.setConfigDescriptionRegistry(configDescriptionRegistry);
        matcher.setMetadataRegistry(metadataRegistry);
    }

    private Collection<ConfigDescription> mockConfigDescriptions() throws Exception {
        List<ConfigDescription> configDescriptions = new ArrayList<>();

        configDescriptions.add(new ConfigDescription(new URI("metadata:magic")));
        configDescriptions.add(new ConfigDescription(new URI("metadata:magic2")));
        configDescriptions.add(new ConfigDescription(new URI("metadata:homekit")));
        configDescriptions.add(new ConfigDescription(new URI("metadata:alexa")));

        return configDescriptions;
    }

    @Test
    public void nullSelector_shouldReturnEmptySet() {
        assertThat(matcher.filterNamespaces(null, null), is(Collections.emptySet()));
    }

    @Test
    public void emptySelector_shouldReturnEmptySet() {
        assertThat(matcher.filterNamespaces("", null), is(Collections.emptySet()));
    }

    @Test
    public void specificSelector_shouldReturnSpecificNamespace() {
        assertThat(matcher.filterNamespaces("alexa", null), hasSize(1));
        assertThat(matcher.filterNamespaces("alexa", null), hasItem("alexa"));
        assertThat(matcher.filterNamespaces("magic", null), hasSize(1));
        assertThat(matcher.filterNamespaces("magic", null), hasItem("magic"));
        assertThat(matcher.filterNamespaces("magic2", null), hasSize(1));
        assertThat(matcher.filterNamespaces("magic2", null), hasItem("magic2"));

        assertThat(matcher.filterNamespaces("unknown", null), hasSize(1));
    }

    @Test
    public void regularExpression_shouldMatchSubset() {
        assertThat(matcher.filterNamespaces(".*", null), hasSize(4));
        assertThat(matcher.filterNamespaces("magic.?", null), hasSize(2));
    }

    @Test
    public void nonConfigDescriptionSelector_shouldBeResult() {
        assertThat(matcher.filterNamespaces("magic, foo, bar", null), hasItems("magic", "foo", "bar"));
    }
}
