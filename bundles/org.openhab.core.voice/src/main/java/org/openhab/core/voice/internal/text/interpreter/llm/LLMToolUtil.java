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

import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.voice.security.ItemPermission;
import org.openhab.core.voice.security.ItemPermissionResolver;
import org.openhab.core.voice.text.interpreter.llm.LLMToolException;

/**
 * Utility class for LLM tool implementations.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
final class LLMToolUtil {
    private LLMToolUtil() {
    }

    /**
     * Resolves an item from the <code>itemName</code> parameter and validates that it exists and HLIs have access to
     * it.
     *
     * @param itemRegistry
     * @param itemPermissionResolver
     * @param params the {@link org.openhab.core.voice.text.interpreter.llm.LLMTool#call(Map, Locale)} parameters
     * @return the resolved item and its permission
     * @throws LLMToolException if the <code>itemName</code> parameter is missing, the Item does not exist, or HLIs have
     *             no access to it
     */
    public static ItemAndPermission resolveAndValidateItem(ItemRegistry itemRegistry,
            ItemPermissionResolver itemPermissionResolver, Map<String, Object> params) throws LLMToolException {
        Object itemNameObj = params.get("itemName");
        if (!(itemNameObj instanceof String itemName)) {
            throw new LLMToolException("Missing or invalid required parameter 'itemName'");
        }

        int lastDot = itemName.lastIndexOf('.');
        if (lastDot != -1) {
            itemName = itemName.substring(lastDot + 1);
        }

        Item item;
        try {
            item = itemRegistry.getItem(itemName);
        } catch (ItemNotFoundException e) {
            throw new LLMToolException("Item not found: " + itemName, e);
        }

        ItemPermission permission = itemPermissionResolver.getPermission(item);
        if (permission == ItemPermission.NO_ACCESS) {
            throw new LLMToolException("Item not found: " + itemName);
        }

        return new ItemAndPermission(item, permission);
    }

    public record ItemAndPermission(Item item, ItemPermission permission) {
    }
}
