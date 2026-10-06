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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateOption;
import org.openhab.core.voice.internal.text.interpreter.llm.LLMToolUtil.ItemAndPermission;
import org.openhab.core.voice.security.ItemPermission;
import org.openhab.core.voice.security.ItemPermissionResolver;
import org.openhab.core.voice.text.interpreter.llm.LLMToolException;

/**
 * Tests for {@link LLMToolUtil}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class LLMToolUtilTest {
    private static final String ITEM_NAME = "TestItem";
    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Berlin");

    private final ItemRegistry itemRegistry = mock(ItemRegistry.class);
    private final ItemPermissionResolver itemPermissionResolver = mock(ItemPermissionResolver.class);
    private final Item item = mock(Item.class);

    @BeforeEach
    public void setUp() throws ItemNotFoundException {
        when(itemRegistry.getItem(ITEM_NAME)).thenReturn(item);
    }

    @Test
    public void resolveAndValidateItemReturnsItemAndPermission() throws LLMToolException {
        when(itemPermissionResolver.getPermission(item)).thenReturn(ItemPermission.READ_ONLY);

        ItemAndPermission result = LLMToolUtil.resolveAndValidateItem(itemRegistry, itemPermissionResolver,
                Map.of("itemName", ITEM_NAME));

        assertEquals(item, result.item());
        assertEquals(ItemPermission.READ_ONLY, result.permission());
    }

    @Test
    public void resolveAndValidateItemWithReadWritePermission() throws LLMToolException {
        when(itemPermissionResolver.getPermission(item)).thenReturn(ItemPermission.READ_WRITE);

        ItemAndPermission result = LLMToolUtil.resolveAndValidateItem(itemRegistry, itemPermissionResolver,
                Map.of("itemName", ITEM_NAME));

        assertEquals(item, result.item());
        assertEquals(ItemPermission.READ_WRITE, result.permission());
    }

    @Test
    public void resolveAndValidateDotPrefixedItem() throws LLMToolException, ItemNotFoundException {
        when(itemPermissionResolver.getPermission(item)).thenReturn(ItemPermission.READ_ONLY);

        ItemAndPermission result = LLMToolUtil.resolveAndValidateItem(itemRegistry, itemPermissionResolver,
                Map.of("itemName", ".." + ITEM_NAME));

        verify(itemRegistry).getItem(ITEM_NAME);
        assertEquals(item, result.item());
        assertEquals(ItemPermission.READ_ONLY, result.permission());
    }

    @Test
    public void resolveAndValidateItemThrowsLTEOnMissingItemName() {
        LLMToolException exception = assertThrows(LLMToolException.class,
                () -> LLMToolUtil.resolveAndValidateItem(itemRegistry, itemPermissionResolver, Map.of()));
        assertEquals("Missing or invalid required parameter 'itemName'", exception.getMessage());
    }

    @Test
    @NonNullByDefault({})
    public void resolveAndValidateItemThrowsLTEOnNullItemName() {
        Map<String, Object> params = Collections.singletonMap("itemName", null);
        LLMToolException exception = assertThrows(LLMToolException.class,
                () -> LLMToolUtil.resolveAndValidateItem(itemRegistry, itemPermissionResolver, params));
        assertEquals("Missing or invalid required parameter 'itemName'", exception.getMessage());
    }

    @Test
    public void resolveAndValidateItemThrowsLTEOnInvalidParameterType() {
        LLMToolException exception = assertThrows(LLMToolException.class, () -> LLMToolUtil
                .resolveAndValidateItem(itemRegistry, itemPermissionResolver, Map.of("itemName", 123)));
        assertEquals("Missing or invalid required parameter 'itemName'", exception.getMessage());
    }

    @Test
    public void resolveAndValidateItemThrowsLTEOnItemNotFound() throws ItemNotFoundException {
        when(itemRegistry.getItem("UnknownItem")).thenThrow(new ItemNotFoundException("UnknownItem"));

        LLMToolException exception = assertThrows(LLMToolException.class, () -> LLMToolUtil
                .resolveAndValidateItem(itemRegistry, itemPermissionResolver, Map.of("itemName", "UnknownItem")));
        assertEquals("Item not found: UnknownItem", exception.getMessage());
        assertInstanceOf(ItemNotFoundException.class, exception.getCause());
    }

    @Test
    public void resolveAndValidateItemThrowsLTEOnNoAccess() {
        when(itemPermissionResolver.getPermission(item)).thenReturn(ItemPermission.NO_ACCESS);

        LLMToolException exception = assertThrows(LLMToolException.class, () -> LLMToolUtil
                .resolveAndValidateItem(itemRegistry, itemPermissionResolver, Map.of("itemName", ITEM_NAME)));
        assertEquals("Item not found: " + ITEM_NAME, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void parseTimeHandlesNow() throws LLMToolException {
        ZonedDateTime now = ZonedDateTime.of(2026, 10, 6, 12, 0, 0, 0, ZONE_ID);
        assertEquals(now, LLMToolUtil.parseTime("now", now, ZONE_ID));
        assertEquals(now, LLMToolUtil.parseTime("NOW", now, ZONE_ID));
        assertEquals(now, LLMToolUtil.parseTime("  now  ", now, ZONE_ID));
    }

    @Test
    public void parseTimeHandlesRelativeOffset() throws LLMToolException {
        ZonedDateTime now = ZonedDateTime.of(2026, 10, 6, 12, 0, 0, 0, ZONE_ID);
        assertEquals(now.minusDays(1), LLMToolUtil.parseTime("-P1D", now, ZONE_ID));
        assertEquals(now.plusDays(7), LLMToolUtil.parseTime("+P7D", now, ZONE_ID));
        assertEquals(now.plusMonths(1), LLMToolUtil.parseTime("+P1M", now, ZONE_ID));
        assertEquals(now.minusYears(1), LLMToolUtil.parseTime("-P1Y", now, ZONE_ID));
    }

    @Test
    public void parseTimeHandlesAbsoluteTimestamps() throws LLMToolException {
        ZonedDateTime now = ZonedDateTime.of(2026, 10, 6, 12, 0, 0, 0, ZONE_ID);

        // Absolute ISO-8601 with offset
        String isoWithOffset = "2026-10-06T12:00:00+02:00";
        assertEquals(ZonedDateTime.parse(isoWithOffset).withZoneSameInstant(ZONE_ID),
                LLMToolUtil.parseTime(isoWithOffset, now, ZONE_ID));

        // Absolute ISO-8601 with UTC Z
        String isoUtc = "2026-10-06T10:00:00Z";
        assertEquals(ZonedDateTime.parse(isoUtc).withZoneSameInstant(ZONE_ID),
                LLMToolUtil.parseTime(isoUtc, now, ZONE_ID));

        // Absolute ISO-8601 without offset uses provided zoneId
        String isoNoOffset = "2026-10-06T12:00:00";
        assertEquals(ZonedDateTime.of(2026, 10, 6, 12, 0, 0, 0, ZONE_ID),
                LLMToolUtil.parseTime(isoNoOffset, now, ZONE_ID));
    }

    @Test
    public void parseTimeThrowsOnInvalidDuration() {
        ZonedDateTime now = ZonedDateTime.now(ZONE_ID);
        LLMToolException exception = assertThrows(LLMToolException.class,
                () -> LLMToolUtil.parseTime("-INVALID", now, ZONE_ID));
        String message = exception.getMessage();
        assertNotNull(message);
        assertTrue(message.startsWith("Invalid ISO duration offset:"));
    }

    @Test
    public void parseTimeThrowsOnInvalidTimestamp() {
        ZonedDateTime now = ZonedDateTime.now(ZONE_ID);
        LLMToolException exception = assertThrows(LLMToolException.class,
                () -> LLMToolUtil.parseTime("not-a-date", now, ZONE_ID));
        String message = exception.getMessage();
        assertNotNull(message);
        assertTrue(message.startsWith("Failed to parse timestamp"));
    }

    @Test
    public void formatItemStateWithoutStateDescriptionReturnsRawState() {
        when(item.getStateDescription(Locale.ENGLISH)).thenReturn(null);

        String result = LLMToolUtil.formatItemState(item, new DecimalType(21.5), Locale.ENGLISH, ZONE_ID);
        assertEquals("21.5", result);
    }

    @Test
    public void formatItemStateWithPatternFormatting() {
        StateDescription stateDescription = mock(StateDescription.class);
        when(stateDescription.getPattern()).thenReturn("%.1f °C");
        when(stateDescription.getOptions()).thenReturn(List.of());
        when(item.getStateDescription(Locale.ENGLISH)).thenReturn(stateDescription);

        String result = LLMToolUtil.formatItemState(item, new DecimalType(21.5), Locale.ENGLISH, ZONE_ID);
        assertEquals("21.5 °C (21.5)", result);
    }

    @Test
    public void formatItemStateWhenDisplayStateEqualsRawState() {
        StateDescription stateDescription = mock(StateDescription.class);
        when(stateDescription.getPattern()).thenReturn("%s");
        when(stateDescription.getOptions()).thenReturn(List.of());
        when(item.getStateDescription(Locale.ENGLISH)).thenReturn(stateDescription);

        String result = LLMToolUtil.formatItemState(item, new StringType("hello"), Locale.ENGLISH, ZONE_ID);
        assertEquals("hello", result);
    }

    @Test
    public void formatItemStateWithOptions() {
        StateDescription stateDescription = mock(StateDescription.class);
        when(stateDescription.getPattern()).thenReturn(null);
        when(stateDescription.getOptions()).thenReturn(List.of(new StateOption("ON", "Active")));
        when(item.getStateDescription(Locale.ENGLISH)).thenReturn(stateDescription);

        String result = LLMToolUtil.formatItemState(item, OnOffType.ON, Locale.ENGLISH, ZONE_ID);
        assertEquals("Active (ON)", result);
    }

    @Test
    public void formatItemStateOverloadUsesItemState() {
        when(item.getState()).thenReturn(new DecimalType(42));
        StateDescription stateDescription = mock(StateDescription.class);
        when(stateDescription.getPattern()).thenReturn("%.0f %%");
        when(stateDescription.getOptions()).thenReturn(List.of());
        when(item.getStateDescription(Locale.ENGLISH)).thenReturn(stateDescription);

        String result = LLMToolUtil.formatItemState(item, Locale.ENGLISH, ZONE_ID);
        assertEquals("42 % (42)", result);
    }

    @Test
    public void formatItemStateFormatsPassedStateUsedOverItemState() {
        when(item.getState()).thenReturn(new DecimalType(100));
        StateDescription stateDescription = mock(StateDescription.class);
        when(stateDescription.getPattern()).thenReturn("%.1f °C");
        when(stateDescription.getOptions()).thenReturn(List.of());
        when(item.getStateDescription(Locale.ENGLISH)).thenReturn(stateDescription);

        String result = LLMToolUtil.formatItemState(item, new DecimalType(20.5), Locale.ENGLISH, ZONE_ID);
        assertEquals("20.5 °C (20.5)", result);
    }
}
