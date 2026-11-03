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

import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.voice.security.ItemPermissionResolver;
import org.openhab.core.voice.text.interpreter.llm.LLMItemSerializer;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Service for enriching LLM system prompts with additional context, such as accessible items and their semantics.
 *
 * @author Florian Hotze - Initial contribution
 */
@Component(service = SystemPromptEnricher.class)
@NonNullByDefault
public class SystemPromptEnricher {

    private final ItemRegistry itemRegistry;
    private final MetadataRegistry metadataRegistry;
    private final ItemPermissionResolver itemPermissionResolver;

    @Activate
    public SystemPromptEnricher(final @Reference ItemRegistry itemRegistry,
            final @Reference MetadataRegistry metadataRegistry,
            final @Reference ItemPermissionResolver itemPermissionResolver) {
        this.itemRegistry = itemRegistry;
        this.metadataRegistry = metadataRegistry;
        this.itemPermissionResolver = itemPermissionResolver;
    }

    /**
     * Enriches the base system prompt with additional context, e.g., accessible items and their semantics.
     *
     * @param baseSystemPrompt the base system prompt
     * @param locale the locale to use
     * @return the enriched system prompt, or the original base system prompt if nothing was added
     */
    public @Nullable String enrichSystemPrompt(@Nullable String baseSystemPrompt, @Nullable Locale locale) {
        List<Item> accessibleItems = itemRegistry.getItems().stream().filter(itemPermissionResolver::isAccessible)
                .toList();
        String serializedItems = LLMItemSerializer.serialize(accessibleItems, metadataRegistry, locale);
        if (serializedItems.isEmpty()) {
            return baseSystemPrompt;
        }

        String itemsSection = "Available items:\n" + serializedItems;
        return baseSystemPrompt != null && !baseSystemPrompt.isBlank() ? baseSystemPrompt.trim() + "\n\n" + itemsSection
                : itemsSection;
    }
}
