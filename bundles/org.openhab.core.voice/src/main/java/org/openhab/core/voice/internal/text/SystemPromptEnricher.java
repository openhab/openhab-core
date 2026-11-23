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

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.semantics.SemanticTags;
import org.openhab.core.voice.internal.VoiceServiceResolver;
import org.openhab.core.voice.internal.text.interpreter.llm.FindItemsLLMTool;
import org.openhab.core.voice.security.ItemPermissionResolver;
import org.openhab.core.voice.text.interpreter.llm.LLMItemSerializer;
import org.openhab.core.voice.text.interpreter.llm.LLMTool;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Service for enriching LLM system prompts with additional context, such as accessible items and their semantics.
 *
 * @author Florian Hotze - Initial contribution
 * @author Dan Cunningham - Limit the prompt to the semantic model
 */
@Component(service = SystemPromptEnricher.class)
@NonNullByDefault
public class SystemPromptEnricher {

    private final ItemRegistry itemRegistry;
    private final MetadataRegistry metadataRegistry;
    private final ItemPermissionResolver itemPermissionResolver;
    private final VoiceServiceResolver voiceServiceResolver;

    @Activate
    public SystemPromptEnricher(final @Reference ItemRegistry itemRegistry,
            final @Reference MetadataRegistry metadataRegistry,
            final @Reference ItemPermissionResolver itemPermissionResolver,
            final @Reference VoiceServiceResolver voiceServiceResolver) {
        this.itemRegistry = itemRegistry;
        this.metadataRegistry = metadataRegistry;
        this.itemPermissionResolver = itemPermissionResolver;
        this.voiceServiceResolver = voiceServiceResolver;
    }

    /**
     * The system prompt to pass to the LLM and the tools to offer with it.
     *
     * @param systemPrompt the enriched system prompt, or the original base system prompt if nothing was added
     * @param tools the tools to offer
     */
    public record EnrichedPrompt(@Nullable String systemPrompt, Collection<LLMTool> tools) {
    }

    /**
     * Enriches the base system prompt with additional context, e.g., accessible items and their semantics, and picks
     * the LLM tools to go with it.
     * <p>
     * If the prompt is limited to the semantic model and the {@link FindItemsLLMTool} is available, only the
     * locations, equipment and points get listed, and the LLM is told to search for anything else. Otherwise every
     * accessible item is listed and the {@link FindItemsLLMTool} is left out.
     *
     * @param baseSystemPrompt the base system prompt
     * @param tools the requested tools
     * @param locale the locale to use
     * @return the enriched system prompt and the tools to offer with it
     */
    public EnrichedPrompt enrich(@Nullable String baseSystemPrompt, Collection<LLMTool> tools,
            @Nullable Locale locale) {
        boolean semanticOnly = voiceServiceResolver.getConfiguration().isPromptLimitedToSemanticModel()
                && tools.stream().anyMatch(SystemPromptEnricher::isFindItemsTool);
        List<Item> promptItems = itemRegistry.getItems().stream().filter(itemPermissionResolver::isAccessible)
                .filter(item -> !semanticOnly || SemanticTags.getSemanticType(item) != null).toList();
        Collection<LLMTool> offeredTools = semanticOnly ? tools
                : tools.stream().filter(tool -> !isFindItemsTool(tool)).toList();

        String serializedItems = LLMItemSerializer.serialize(promptItems, metadataRegistry, locale);
        String itemsSection = serializedItems.isEmpty() ? "" : "Available items:\n" + serializedItems;
        if (semanticOnly) {
            String hint = "Only items in the semantic model are listed. If the user refers to a device that is not "
                    + "listed, use the \"" + FindItemsLLMTool.ID
                    + "\" tool to search for it before saying it doesn't exist.";
            itemsSection = itemsSection.isEmpty() ? hint : hint + "\n" + itemsSection;
        }
        if (itemsSection.isEmpty()) {
            return new EnrichedPrompt(baseSystemPrompt, offeredTools);
        }

        return new EnrichedPrompt(baseSystemPrompt != null && !baseSystemPrompt.isBlank()
                ? baseSystemPrompt.trim() + "\n\n" + itemsSection
                : itemsSection, offeredTools);
    }

    private static boolean isFindItemsTool(LLMTool tool) {
        return FindItemsLLMTool.ID.equals(tool.getUID());
    }
}
