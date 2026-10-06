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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.persistence.HistoricItem;
import org.openhab.core.persistence.PersistenceManager;
import org.openhab.core.persistence.PersistenceService;
import org.openhab.core.persistence.PersistenceServiceRegistry;
import org.openhab.core.persistence.QueryablePersistenceService;
import org.openhab.core.persistence.extensions.PersistenceExtensions;
import org.openhab.core.persistence.registry.PersistenceServiceConfigurationRegistry;
import org.openhab.core.types.State;
import org.openhab.core.types.StateDescription;
import org.openhab.core.voice.security.ItemPermission;
import org.openhab.core.voice.security.ItemPermissionResolver;
import org.openhab.core.voice.text.interpreter.llm.LLMToolException;

/**
 * Test class for {@link ItemQueryPersistenceLLMTool}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class ItemQueryPersistenceLLMToolTest {
    private static final String ITEM_NAME = "TestItem";
    private static final String DEFAULT_SERVICE_ID = "defaultService";
    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Berlin");

    private final ItemRegistry itemRegistry = mock(ItemRegistry.class);
    private final ItemPermissionResolver itemPermissionResolver = mock(ItemPermissionResolver.class);
    private final TimeZoneProvider timeZoneProvider = mock(TimeZoneProvider.class);
    private final QueryablePersistenceService queryablePersistenceService = mock(QueryablePersistenceService.class);
    private final PersistenceManager persistenceManager = mock(PersistenceManager.class);
    private final PersistenceServiceConfigurationRegistry configRegistry = mock(
            PersistenceServiceConfigurationRegistry.class);
    private final Item item = mock(Item.class);

    private @NonNullByDefault({}) ItemQueryPersistenceLLMTool tool;

    private static class SimpleHistoricItem implements HistoricItem {
        private final String name;
        private final State state;
        private final ZonedDateTime timestamp;

        public SimpleHistoricItem(String name, State state, ZonedDateTime timestamp) {
            this.name = name;
            this.state = state;
            this.timestamp = timestamp;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public State getState() {
            return state;
        }

        @Override
        public ZonedDateTime getTimestamp() {
            return timestamp;
        }
    }

    @BeforeEach
    public void setUp() throws ItemNotFoundException {
        when(timeZoneProvider.getTimeZone()).thenReturn(ZONE_ID);
        when(itemRegistry.getItem(ITEM_NAME)).thenReturn(item);
        when(item.getName()).thenReturn(ITEM_NAME);
        when(item.getState()).thenReturn(new DecimalType(20));
        when(itemPermissionResolver.getPermission(item)).thenReturn(ItemPermission.READ_ONLY);

        PersistenceServiceRegistry serviceRegistry = new PersistenceServiceRegistry() {
            @Override
            public @Nullable PersistenceService getDefault() {
                return queryablePersistenceService;
            }

            @Override
            public @Nullable PersistenceService get(@Nullable String serviceId) {
                return queryablePersistenceService;
            }

            @Override
            public @Nullable String getDefaultId() {
                return DEFAULT_SERVICE_ID;
            }

            @Override
            public Set<PersistenceService> getAll() {
                return Set.of(queryablePersistenceService);
            }
        };

        new PersistenceExtensions(persistenceManager, serviceRegistry, configRegistry, timeZoneProvider);

        tool = new ItemQueryPersistenceLLMTool(itemRegistry, itemPermissionResolver, timeZoneProvider);
    }

    @Test
    public void getUIDReturnsCorrectId() {
        assertEquals(ItemQueryPersistenceLLMTool.ID, tool.getUID());
    }

    @Test
    public void callThrowsLTEOnMissingParameters() {
        assertThrows(LLMToolException.class, () -> tool.call(Map.of(), Locale.ENGLISH));
        assertThrows(LLMToolException.class, () -> tool.call(Map.of("itemName", ITEM_NAME), Locale.ENGLISH));
        assertThrows(LLMToolException.class,
                () -> tool.call(Map.of("itemName", ITEM_NAME, "queryType", "INVALID_QUERY"), Locale.ENGLISH));
    }

    @Test
    public void callThrowsLTEOnItemNotFound() throws ItemNotFoundException {
        when(itemRegistry.getItem(ITEM_NAME)).thenThrow(new ItemNotFoundException("Item not found"));
        assertThrows(LLMToolException.class,
                () -> tool.call(Map.of("itemName", ITEM_NAME, "queryType", "LAST_CHANGE"), Locale.ENGLISH));
    }

    @Test
    public void callThrowsLTEOnNotAccessible() {
        when(itemPermissionResolver.getPermission(item)).thenReturn(ItemPermission.NO_ACCESS);
        assertThrows(LLMToolException.class,
                () -> tool.call(Map.of("itemName", ITEM_NAME, "queryType", "LAST_CHANGE"), Locale.ENGLISH));
    }

    @Test
    public void callStripsLeadingPrefixFromItemName() throws LLMToolException {
        when(queryablePersistenceService.query(any(), any())).thenReturn(List.of());
        String result = tool.call(Map.of("itemName", "..TestItem", "queryType", "LAST_CHANGE"), Locale.ENGLISH);
        assertEquals("No state change recorded for item '" + ITEM_NAME + "'.", result);
    }

    @Test
    public void callValidatesTimeRanges() {
        // STATE_AT requires 'start'
        assertThrows(LLMToolException.class,
                () -> tool.call(Map.of("itemName", ITEM_NAME, "queryType", "STATE_AT"), Locale.ENGLISH));

        // Range query with neither start nor end
        assertThrows(LLMToolException.class,
                () -> tool.call(Map.of("itemName", ITEM_NAME, "queryType", "MINIMUM"), Locale.ENGLISH));

        // Range query with start in future but no end
        assertThrows(LLMToolException.class, () -> tool
                .call(Map.of("itemName", ITEM_NAME, "queryType", "AVERAGE", "start", "+PT4H"), Locale.ENGLISH));

        // Range query with end in past but no start
        assertThrows(LLMToolException.class,
                () -> tool.call(Map.of("itemName", ITEM_NAME, "queryType", "AVERAGE", "end", "-PT4H"), Locale.ENGLISH));

        // Range query with start after end
        assertThrows(LLMToolException.class,
                () -> tool.call(Map.of("itemName", ITEM_NAME, "queryType", "MINIMUM", "start", "-PT1H", "end", "-PT2H"),
                        Locale.ENGLISH));
    }

    @Test
    public void callReturnsLastChangeWhenItemStateChanged() throws LLMToolException {
        ZonedDateTime changeTime = ZonedDateTime.of(2026, 10, 6, 9, 30, 0, 0, ZONE_ID);
        when(item.getLastStateChange()).thenReturn(changeTime);
        when(queryablePersistenceService.query(any(), any()))
                .thenReturn(List.of(new SimpleHistoricItem(ITEM_NAME, new DecimalType(10), changeTime)));

        String result = tool.call(Map.of("itemName", ITEM_NAME, "queryType", "LAST_CHANGE"), Locale.ENGLISH);

        String expectedFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG).withLocale(Locale.ENGLISH)
                .format(changeTime);
        assertTrue(result.contains(expectedFormat),
                "Expected output to contain " + expectedFormat + " but was " + result);
    }

    @Test
    public void callHandlesNoPersistenceDataGracefully() throws LLMToolException {
        when(item.getState()).thenReturn(org.openhab.core.types.UnDefType.NULL);
        when(queryablePersistenceService.query(any(), any())).thenReturn(List.of());

        String resultStateAt = tool.call(Map.of("itemName", ITEM_NAME, "queryType", "STATE_AT", "start", "-PT1H"),
                Locale.ENGLISH);
        assertEquals("No persistence data found for item 'TestItem' at the specified time.", resultStateAt);

        String resultMin = tool.call(Map.of("itemName", ITEM_NAME, "queryType", "MINIMUM", "start", "-PT1H"),
                Locale.ENGLISH);
        assertEquals("No minimum data found for item 'TestItem' in the specified timeframe.", resultMin);

        String resultMax = tool.call(Map.of("itemName", ITEM_NAME, "queryType", "MAXIMUM", "start", "-PT1H"),
                Locale.ENGLISH);
        assertEquals("No maximum data found for item 'TestItem' in the specified timeframe.", resultMax);

        String resultAvg = tool.call(Map.of("itemName", ITEM_NAME, "queryType", "AVERAGE", "start", "-PT1H"),
                Locale.ENGLISH);
        assertEquals("No average data found for item 'TestItem' in the specified timeframe.", resultAvg);

        String resultDelta = tool.call(Map.of("itemName", ITEM_NAME, "queryType", "DELTA", "start", "-PT1H"),
                Locale.ENGLISH);
        assertEquals("No delta data found for item 'TestItem' in the specified timeframe.", resultDelta);

        String resultSum = tool.call(Map.of("itemName", ITEM_NAME, "queryType", "SUM", "start", "-PT1H"),
                Locale.ENGLISH);
        assertEquals("0", resultSum);

        String resultNextChange = tool.call(Map.of("itemName", ITEM_NAME, "queryType", "NEXT_CHANGE"), Locale.ENGLISH);
        assertEquals("No future state change scheduled for item 'TestItem'.", resultNextChange);

        String resultNextUpdate = tool.call(Map.of("itemName", ITEM_NAME, "queryType", "NEXT_UPDATE"), Locale.ENGLISH);
        assertEquals("No future update scheduled for item 'TestItem'.", resultNextUpdate);
    }
}
