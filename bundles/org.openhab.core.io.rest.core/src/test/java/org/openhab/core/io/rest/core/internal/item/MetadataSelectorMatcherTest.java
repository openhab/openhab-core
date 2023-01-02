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
package org.openhab.core.io.rest.core.internal.item;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsIterableContaining.hasItem;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;

/**
 * Test the {@link MetadataSelectorMatcher}.
 *
 * @author Henning Treu - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class MetadataSelectorMatcherTest {

    private @NonNullByDefault({}) MetadataSelectorMatcher matcher;

    private @Mock @NonNullByDefault({}) MetadataRegistry metadataRegistryMock;

    @BeforeEach
    public void setup() throws Exception {
        when(metadataRegistryMock.getAll())
                .thenReturn(List.of(new Metadata(new MetadataKey("magic", "test_item"), "test", Map.of()),
                        new Metadata(new MetadataKey("magic2", "test_item"), "test", Map.of()),
                        new Metadata(new MetadataKey("homekit", "test_item"), "test", Map.of()),
                        new Metadata(new MetadataKey("alexa", "test_item"), "test", Map.of())));
        when(metadataRegistryMock.isInternalNamespace(anyString())).thenReturn(false);

        matcher = new MetadataSelectorMatcher(metadataRegistryMock);
    }

    @Test
    public void nullSelectorShouldReturnEmptySet() {
        assertThat(matcher.filterNamespaces(null, null), is(Collections.emptySet()));
    }

    @Test
    public void emptySelectorShouldReturnEmptySet() {
        assertThat(matcher.filterNamespaces("", null), is(Collections.emptySet()));
    }

    @Test
    public void specificSelectorShouldReturnSpecificNamespace() {
        assertThat(matcher.filterNamespaces("alexa", null), hasSize(1));
        assertThat(matcher.filterNamespaces("alexa", null), hasItem("alexa"));
        assertThat(matcher.filterNamespaces("magic", null), hasSize(1));
        assertThat(matcher.filterNamespaces("magic", null), hasItem("magic"));
        assertThat(matcher.filterNamespaces("magic2", null), hasSize(1));
        assertThat(matcher.filterNamespaces("magic2", null), hasItem("magic2"));

        assertThat(matcher.filterNamespaces("unknown", null), hasSize(1));
    }

    @Test
    public void regularExpressionShouldMatchSubset() {
        assertThat(matcher.filterNamespaces(".*", null), hasSize(4));
        assertThat(matcher.filterNamespaces("magic.?", null), hasSize(2));
    }

    @Test
    public void nonConfigDescriptionSelectorShouldBeResult() {
        assertThat(matcher.filterNamespaces("magic, foo, bar", null), hasItems("magic", "foo", "bar"));
    }
}
