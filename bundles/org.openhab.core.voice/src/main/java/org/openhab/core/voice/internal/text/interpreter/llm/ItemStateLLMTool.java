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
package org.openhab.core.voice.internal.text.interpreter.llm;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.voice.security.ItemPermissionResolver;
import org.openhab.core.voice.text.interpreter.llm.LLMTool;
import org.openhab.core.voice.text.interpreter.llm.LLMToolException;
import org.openhab.core.voice.text.interpreter.llm.LLMToolParam;
import org.openhab.core.voice.text.interpreter.llm.LLMToolParamType;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link ItemStateLLMTool} is an {@link LLMTool} that allows to get the state of an item.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
@Component(service = LLMTool.class, immediate = true)
public class ItemStateLLMTool implements LLMTool {
    public static final String ID = "item-get-state";

    private final ItemRegistry itemRegistry;
    private final ItemPermissionResolver itemPermissionResolver;

    @Activate
    public ItemStateLLMTool(final @Reference ItemRegistry itemRegistry,
            final @Reference ItemPermissionResolver itemPermissionResolver) {
        this.itemRegistry = itemRegistry;
        this.itemPermissionResolver = itemPermissionResolver;
    }

    @Override
    public String getUID() {
        return ID;
    }

    @Override
    public String getLabel(@Nullable Locale locale) {
        return "Get Item State";
    }

    @Override
    public String getShortDescription(@Nullable Locale locale) {
        return "Gets the current state of a specific item.";
    }

    @Override
    public String getDescription(@Nullable Locale locale) {
        return "This tool allows to retrieve the current state of an item.";
    }

    @Override
    public List<LLMToolParam> getParamDescriptions(@Nullable Locale locale) {
        return List.of(new LLMToolParam("itemName", LLMToolParamType.STRING, "The name of the item to get the state of",
                List.of(), true));
    }

    @Override
    public String call(Map<String, Object> params, @Nullable Locale locale) throws LLMToolException {
        Object itemNameObj = params.get("itemName");

        if (!(itemNameObj instanceof String itemName)) {
            throw new LLMToolException("Missing or invalid required parameter 'itemName'");
        }

        Item item;
        try {
            item = itemRegistry.getItem(itemName);
        } catch (ItemNotFoundException e) {
            throw new LLMToolException("Item not found: " + itemName, e);
        }

        if (!itemPermissionResolver.isAccessible(item)) {
            throw new LLMToolException("Item not found: " + itemName);
        }

        return item.getState().toString();
    }
}
