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
import static org.openhab.core.semantics.SemanticTags.METADATA_NAMESPACE;
import static org.openhab.core.semantics.SemanticTags.REL_HAS_LOCATION;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.semantics.Location;
import org.openhab.core.semantics.Point;
import org.openhab.core.semantics.SemanticTags;
import org.openhab.core.voice.internal.VoiceManagerConfiguration;
import org.openhab.core.voice.internal.VoiceServiceResolver;
import org.openhab.core.voice.internal.text.interpreter.llm.FindItemsLLMTool;
import org.openhab.core.voice.security.ItemPermissionResolver;
import org.openhab.core.voice.text.interpreter.llm.LLMTool;

/**
 * Unit tests for {@link SystemPromptEnricher}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class SystemPromptEnricherTest {
    private interface MockKitchen extends Location {
    }

    private interface MockLight extends Point {
    }

    private @NonNullByDefault({}) ItemRegistry itemRegistry;
    private @NonNullByDefault({}) MetadataRegistry metadataRegistry;
    private @NonNullByDefault({}) ItemPermissionResolver itemPermissionResolver;
    private @NonNullByDefault({}) VoiceManagerConfiguration configuration;

    private @NonNullByDefault({}) SystemPromptEnricher enricher;

    @BeforeAll
    public static void setUpTags() {
        SemanticTags.addTagSet("Mock_Location_Room_Kitchen", MockKitchen.class);
        SemanticTags.addTagSet("Mock_Point_Control_Light", MockLight.class);
    }

    @AfterAll
    public static void tearDownTags() {
        SemanticTags.removeTagSet("Mock_Location_Room_Kitchen", MockKitchen.class);
        SemanticTags.removeTagSet("Mock_Point_Control_Light", MockLight.class);
    }

    @BeforeEach
    public void setUp() {
        itemRegistry = mock(ItemRegistry.class);
        metadataRegistry = mock(MetadataRegistry.class);
        itemPermissionResolver = mock(ItemPermissionResolver.class);

        configuration = mock(VoiceManagerConfiguration.class);
        VoiceServiceResolver voiceServiceResolver = mock(VoiceServiceResolver.class);
        when(voiceServiceResolver.getConfiguration()).thenReturn(configuration);

        enricher = new SystemPromptEnricher(itemRegistry, metadataRegistry, itemPermissionResolver,
                voiceServiceResolver);
    }

    @Test
    public void enrichSystemPromptWithNullBasePromptAndNoItemsReturnsNull() {
        when(itemRegistry.getItems()).thenReturn(List.of());

        String result = enricher.enrich(null, List.of(), Locale.ENGLISH).systemPrompt();

        assertNull(result);
    }

    @Test
    public void enrichSystemPromptWithEmptyBasePromptAndNoItemsReturnsEmpty() {
        when(itemRegistry.getItems()).thenReturn(List.of());

        String result = enricher.enrich("", List.of(), Locale.ENGLISH).systemPrompt();

        assertEquals("", result);
    }

    @Test
    public void enrichSystemPromptWithBasePromptAndNoItemsReturnsBasePrompt() {
        when(itemRegistry.getItems()).thenReturn(List.of());

        String result = enricher.enrich("You are an assistant.", List.of(), Locale.ENGLISH).systemPrompt();

        assertEquals("You are an assistant.", result);
    }

    @Test
    public void enrichSystemPromptWithNullBasePromptAndItemsReturnsOnlyItemsSection() {
        SwitchItem item = new SwitchItem("TestSwitch");
        item.setLabel("Test Switch Label");
        when(itemRegistry.getItems()).thenReturn(List.of(item));
        when(itemPermissionResolver.isAccessible(item)).thenReturn(true);

        String result = enricher.enrich(null, List.of(), Locale.ENGLISH).systemPrompt();

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

        String result = enricher.enrich("   ", List.of(), Locale.ENGLISH).systemPrompt();

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

        String result = enricher.enrich("You are an assistant.", List.of(), Locale.ENGLISH).systemPrompt();

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

        String result = enricher.enrich("Base prompt", List.of(), Locale.ENGLISH).systemPrompt();

        assertNotNull(result);
        assertTrue(result.contains("AccessibleSwitch"));
        assertFalse(result.contains("SecretSwitch"));
    }

    @Test
    public void enrichSystemPromptWithAllItemsInaccessibleReturnsBasePrompt() {
        SwitchItem inaccessibleItem = new SwitchItem("SecretSwitch");
        when(itemRegistry.getItems()).thenReturn(List.of(inaccessibleItem));
        when(itemPermissionResolver.isAccessible(inaccessibleItem)).thenReturn(false);

        String result = enricher.enrich("Base prompt", List.of(), Locale.ENGLISH).systemPrompt();

        assertEquals("Base prompt", result);
    }

    @Test
    public void enrichLimitedToSemanticModelListsOnlySemanticItemsAndFindItemsHint() {
        GroupItem kitchen = new GroupItem("Kitchen");
        kitchen.addTag("Mock_Location_Room_Kitchen");
        SwitchItem light = new SwitchItem("Kitchen_Light");
        light.addTag("Mock_Point_Control_Light");
        SwitchItem helper = new SwitchItem("Helper_Switch");
        when(itemRegistry.getItems()).thenReturn(List.of(kitchen, light, helper));
        when(itemPermissionResolver.isAccessible(any())).thenReturn(true);
        hasLocation(light, kitchen);
        when(configuration.isPromptLimitedToSemanticModel()).thenReturn(true);

        String result = enricher.enrich("Base prompt", List.of(tool(FindItemsLLMTool.ID)), Locale.ENGLISH)
                .systemPrompt();

        assertNotNull(result);
        assertTrue(result.contains("Kitchen :MockKitchen\n..Kitchen_Light Switch :MockLight"), result);
        assertFalse(result.contains("Helper_Switch"));
        assertTrue(result.contains(FindItemsLLMTool.ID));
    }

    @Test
    public void enrichLimitedToSemanticModelWithoutSemanticItemsStillAddsHint() {
        when(itemRegistry.getItems()).thenReturn(List.of(new SwitchItem("Helper_Switch")));
        when(itemPermissionResolver.isAccessible(any())).thenReturn(true);
        when(configuration.isPromptLimitedToSemanticModel()).thenReturn(true);

        String result = enricher.enrich("Base prompt", List.of(tool(FindItemsLLMTool.ID)), Locale.ENGLISH)
                .systemPrompt();

        assertNotNull(result);
        assertTrue(result.startsWith("Base prompt\n\nOnly items in the semantic model are listed."));
        assertFalse(result.contains("Helper_Switch"));
    }

    @Test
    public void enrichLimitedToSemanticModelWithoutFindItemsToolListsAllItems() {
        SwitchItem light = new SwitchItem("Kitchen_Light");
        light.addTag("Mock_Point_Control_Light");
        SwitchItem helper = new SwitchItem("Helper_Switch");
        when(itemRegistry.getItems()).thenReturn(List.of(light, helper));
        when(itemPermissionResolver.isAccessible(any())).thenReturn(true);
        when(configuration.isPromptLimitedToSemanticModel()).thenReturn(true);
        LLMTool other = tool("other");

        SystemPromptEnricher.EnrichedPrompt result = enricher.enrich("Base prompt", List.of(other), Locale.ENGLISH);

        String prompt = result.systemPrompt();
        assertNotNull(prompt);
        assertTrue(prompt.contains("Kitchen_Light"));
        assertTrue(prompt.contains("Helper_Switch"));
        assertFalse(prompt.contains(FindItemsLLMTool.ID));
        assertEquals(List.of(other), result.tools());
    }

    @Test
    public void enrichDropsFindItemsToolWhenAllItemsAreListed() {
        when(itemRegistry.getItems()).thenReturn(List.of());
        LLMTool findItems = tool(FindItemsLLMTool.ID);
        LLMTool other = tool("other");

        assertEquals(List.of(other), enricher.enrich(null, List.of(findItems, other), Locale.ENGLISH).tools());
    }

    @Test
    public void enrichKeepsFindItemsToolWhenLimitedToSemanticModel() {
        when(itemRegistry.getItems()).thenReturn(List.of());
        LLMTool findItems = tool(FindItemsLLMTool.ID);
        LLMTool other = tool("other");
        when(configuration.isPromptLimitedToSemanticModel()).thenReturn(true);

        assertEquals(List.of(findItems, other),
                enricher.enrich(null, List.of(findItems, other), Locale.ENGLISH).tools());
    }

    private void hasLocation(Item item, Item location) {
        MetadataKey key = new MetadataKey(METADATA_NAMESPACE, item.getName());
        when(metadataRegistry.get(key)).thenReturn(new Metadata(key, "", Map.of(REL_HAS_LOCATION, location.getName())));
    }

    private static LLMTool tool(String uid) {
        LLMTool tool = mock(LLMTool.class);
        when(tool.getUID()).thenReturn(uid);
        return tool;
    }
}
