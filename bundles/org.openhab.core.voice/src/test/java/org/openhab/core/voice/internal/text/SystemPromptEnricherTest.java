/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.voice.internal.text;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.semantics.SemanticTags;
import org.openhab.core.voice.security.ItemPermissionResolver;

/**
 * Unit tests for {@link SystemPromptEnricher}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class SystemPromptEnricherTest {

    private @NonNullByDefault({}) ItemRegistry itemRegistry;
    private @NonNullByDefault({}) MetadataRegistry metadataRegistry;
    private @NonNullByDefault({}) ItemPermissionResolver itemPermissionResolver;

    private @NonNullByDefault({}) SystemPromptEnricher enricher;

    @BeforeEach
    public void setUp() {
        itemRegistry = mock(ItemRegistry.class);
        metadataRegistry = mock(MetadataRegistry.class);
        itemPermissionResolver = mock(ItemPermissionResolver.class);

        enricher = new SystemPromptEnricher(itemRegistry, metadataRegistry, itemPermissionResolver);
    }

    @Test
    public void enrichSystemPromptWithNullBasePromptAndNoItemsReturnsNull() {
        when(itemRegistry.getItems()).thenReturn(List.of());

        String result = enricher.enrichSystemPrompt(null, Locale.ENGLISH);

        assertNull(result);
    }

    @Test
    public void enrichSystemPromptWithEmptyBasePromptAndNoItemsReturnsEmpty() {
        when(itemRegistry.getItems()).thenReturn(List.of());

        String result = enricher.enrichSystemPrompt("", Locale.ENGLISH);

        assertEquals("", result);
    }

    @Test
    public void enrichSystemPromptWithBasePromptAndNoItemsReturnsBasePrompt() {
        when(itemRegistry.getItems()).thenReturn(List.of());

        String result = enricher.enrichSystemPrompt("You are an assistant.", Locale.ENGLISH);

        assertEquals("You are an assistant.", result);
    }

    @Test
    public void enrichSystemPromptWithNullBasePromptAndItemsReturnsOnlyItemsSection() {
        SwitchItem item = new SwitchItem("TestSwitch");
        item.setLabel("Test Switch Label");
        when(itemRegistry.getItems()).thenReturn(List.of(item));
        when(itemPermissionResolver.isAccessible(item)).thenReturn(true);

        String result = enricher.enrichSystemPrompt(null, Locale.ENGLISH);

        assertNotNull(result);
        assertTrue(result.startsWith("Available items:\n"));
        assertTrue(result.contains("TestSwitch"));
        assertTrue(result.contains("Test Switch Label"));
    }

    @Test
    public void enrichSystemPromptWithBlankBasePromptAndItemsReturnsOnlyItemsSection() {
        SwitchItem item = new SwitchItem("TestSwitch");
        item.setLabel("Test Switch Label");
        when(itemRegistry.getItems()).thenReturn(List.of(item));
        when(itemPermissionResolver.isAccessible(item)).thenReturn(true);

        String result = enricher.enrichSystemPrompt("   ", Locale.ENGLISH);

        assertNotNull(result);
        assertTrue(result.startsWith("Available items:\n"));
        assertTrue(result.contains("TestSwitch"));
        assertFalse(result.contains("   \n\n"));
    }

    @Test
    public void enrichSystemPromptWithBasePromptAndItemsReturnsCombinedPrompt() {
        SwitchItem item = new SwitchItem("TestSwitch");
        item.setLabel("Test Switch Label");
        when(itemRegistry.getItems()).thenReturn(List.of(item));
        when(itemPermissionResolver.isAccessible(item)).thenReturn(true);

        String result = enricher.enrichSystemPrompt("You are an assistant.", Locale.ENGLISH);

        assertNotNull(result);
        assertTrue(result.startsWith("You are an assistant.\n\nAvailable items:\n"));
        assertTrue(result.contains("TestSwitch"));
    }

    @Test
    public void enrichSystemPromptFiltersInaccessibleItems() {
        SwitchItem accessibleItem = new SwitchItem("AccessibleSwitch");
        SwitchItem inaccessibleItem = new SwitchItem("SecretSwitch");
        when(itemRegistry.getItems()).thenReturn(List.of(accessibleItem, inaccessibleItem));
        when(itemPermissionResolver.isAccessible(accessibleItem)).thenReturn(true);
        when(itemPermissionResolver.isAccessible(inaccessibleItem)).thenReturn(false);

        String result = enricher.enrichSystemPrompt("Base prompt", Locale.ENGLISH);

        assertNotNull(result);
        assertTrue(result.contains("AccessibleSwitch"));
        assertFalse(result.contains("SecretSwitch"));
    }

    @Test
    public void enrichSystemPromptWithAllItemsInaccessibleReturnsBasePrompt() {
        SwitchItem inaccessibleItem = new SwitchItem("SecretSwitch");
        when(itemRegistry.getItems()).thenReturn(List.of(inaccessibleItem));
        when(itemPermissionResolver.isAccessible(inaccessibleItem)).thenReturn(false);

        String result = enricher.enrichSystemPrompt("Base prompt", Locale.ENGLISH);

        assertEquals("Base prompt", result);
    }

    @Test
    public void enrichSystemPromptCachesSerializedItemsAcrossCalls() {
        SwitchItem item = new SwitchItem("CachedSwitch");
        when(itemRegistry.getItems()).thenReturn(List.of(item));
        when(itemPermissionResolver.isAccessible(item)).thenReturn(true);

        String result1 = enricher.enrichSystemPrompt("Prompt 1", Locale.ENGLISH);
        String result2 = enricher.enrichSystemPrompt("Prompt 2", Locale.ENGLISH);

        assertNotNull(result1);
        assertNotNull(result2);
        assertTrue(result1.contains("CachedSwitch"));
        assertTrue(result2.contains("CachedSwitch"));
        // itemRegistry.getItems() should be called only once due to caching
        verify(itemRegistry, times(1)).getItems();
    }

    @Test
    public void itemAccessChangeListenerInvalidatesCache() {
        ArgumentCaptor<Runnable> listenerCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(itemPermissionResolver).addItemAccessChangeListener(listenerCaptor.capture());
        Runnable listener = listenerCaptor.getValue();

        SwitchItem item = new SwitchItem("Switch1");
        when(itemRegistry.getItems()).thenReturn(List.of(item));
        when(itemPermissionResolver.isAccessible(item)).thenReturn(true);

        enricher.enrichSystemPrompt("Prompt 1", Locale.ENGLISH);
        verify(itemRegistry, times(1)).getItems();

        // Trigger item access change notification
        listener.run();

        enricher.enrichSystemPrompt("Prompt 2", Locale.ENGLISH);
        verify(itemRegistry, times(2)).getItems();
    }

    @Test
    public void semanticsMetadataChangeListenerInvalidatesCache() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<RegistryChangeListener<Metadata>> listenerCaptor = ArgumentCaptor
                .forClass(RegistryChangeListener.class);
        verify(metadataRegistry).addRegistryChangeListener(listenerCaptor.capture());
        RegistryChangeListener<Metadata> listener = listenerCaptor.getValue();

        SwitchItem item = new SwitchItem("Switch1");
        when(itemRegistry.getItems()).thenReturn(List.of(item));
        when(itemPermissionResolver.isAccessible(item)).thenReturn(true);

        enricher.enrichSystemPrompt("Prompt 1", Locale.ENGLISH);
        verify(itemRegistry, times(1)).getItems();

        // Non-semantics metadata does not invalidate
        Metadata otherMetadata = new Metadata(new MetadataKey("otherNamespace", "Switch1"), "val", Map.of());
        listener.added(otherMetadata);
        enricher.enrichSystemPrompt("Prompt 2", Locale.ENGLISH);
        verify(itemRegistry, times(1)).getItems();

        // Semantics metadata change invalidates
        Metadata semanticsMetadata = new Metadata(new MetadataKey(SemanticTags.METADATA_NAMESPACE, "Switch1"), "val",
                Map.of());
        listener.added(semanticsMetadata);
        enricher.enrichSystemPrompt("Prompt 3", Locale.ENGLISH);
        verify(itemRegistry, times(2)).getItems();
    }

    @Test
    public void deactivateRemovesListenersAndClearsCache() {
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(itemPermissionResolver).addItemAccessChangeListener(runnableCaptor.capture());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<RegistryChangeListener<Metadata>> metadataCaptor = ArgumentCaptor
                .forClass(RegistryChangeListener.class);
        verify(metadataRegistry).addRegistryChangeListener(metadataCaptor.capture());

        enricher.deactivate();

        verify(itemPermissionResolver).removeItemAccessChangeListener(runnableCaptor.getValue());
        verify(metadataRegistry).removeRegistryChangeListener(metadataCaptor.getValue());
    }
}
