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

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.transform.util.ItemDisplayStateUtil;
import org.openhab.core.types.State;
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

    /**
     * Formats a timestamp using the given locale. If the locale is null, the default locale is used.
     * 
     * @param timestamp the timestamp to format
     * @param locale the locale to use for formatting the timestamp
     * @return the formatted timestamp
     */
    public static String formatTimestamp(ZonedDateTime timestamp, @Nullable Locale locale) {
        Locale effectiveLocale = locale != null ? locale : Locale.getDefault();
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG)
                .withLocale(effectiveLocale);
        return timestamp.format(formatter);
    }

    /**
     * Formats an Item's state:
     *
     * <ul>
     * <li>if displayState is not null and differs rawState: <code>displayState (rawState)</code></li>
     * <li>else: <code>rawState</code></li>
     * </ul>
     *
     * @param item the item to get the state description from
     * @param state the state to format
     * @param locale the locale to use for formatting the displayState
     * @param zoneId the timezone to use for formatting the displayState
     * @return the formatted state
     */
    public static String formatItemState(Item item, State state, @Nullable Locale locale, ZoneId zoneId) {
        String rawState = item.getState().toString();
        String displayState = ItemDisplayStateUtil.getDisplayState(item, state, locale, zoneId);

        if (displayState != null && !displayState.equals(rawState)) {
            return displayState + " (" + rawState + ")";
        }

        return rawState;
    }

    /**
     * Formats an Item's state:
     *
     * <ul>
     * <li>if displayState is not null and differs rawState: <code>displayState (rawState)</code></li>
     * <li>else: <code>rawState</code></li>
     * </ul>
     *
     * @param item the item which state to format depending on its state description
     * @param locale the locale to use for formatting the displayState
     * @param zoneId the timezone to use for formatting the displayState
     * @return the formatted state
     */
    public static String formatItemState(Item item, @Nullable Locale locale, ZoneId zoneId) {
        return formatItemState(item, item.getState(), locale, zoneId);
    }

    public record ItemAndPermission(Item item, ItemPermission permission) {
    }
}
