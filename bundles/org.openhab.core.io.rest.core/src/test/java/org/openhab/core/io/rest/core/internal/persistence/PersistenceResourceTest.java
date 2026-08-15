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
package org.openhab.core.io.rest.core.internal.persistence;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.io.rest.LocaleService;
import org.openhab.core.io.rest.core.config.ConfigurationService;
import org.openhab.core.io.rest.core.internal.persistence.PersistenceResource.PersistenceItemInfoDTO;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.persistence.HistoricItem;
import org.openhab.core.persistence.ModifiablePersistenceService;
import org.openhab.core.persistence.PersistenceItemConfiguration;
import org.openhab.core.persistence.PersistenceItemInfo;
import org.openhab.core.persistence.PersistenceService;
import org.openhab.core.persistence.PersistenceServiceProblem;
import org.openhab.core.persistence.PersistenceServiceRegistry;
import org.openhab.core.persistence.config.PersistenceAllConfig;
import org.openhab.core.persistence.config.PersistenceConfig;
import org.openhab.core.persistence.config.PersistenceGroupConfig;
import org.openhab.core.persistence.config.PersistenceItemConfig;
import org.openhab.core.persistence.config.PersistenceItemExcludeConfig;
import org.openhab.core.persistence.dto.ItemHistoryDTO;
import org.openhab.core.persistence.dto.ItemHistoryDTO.HistoryDataBean;
import org.openhab.core.persistence.internal.PersistenceManagerImpl;
import org.openhab.core.persistence.registry.ManagedPersistenceServiceConfigurationProvider;
import org.openhab.core.persistence.registry.PersistenceServiceConfiguration;
import org.openhab.core.persistence.registry.PersistenceServiceConfigurationRegistry;
import org.openhab.core.persistence.strategy.PersistenceCronStrategy;
import org.openhab.core.persistence.strategy.PersistenceStrategy;
import org.openhab.core.types.State;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateOption;
import org.openhab.core.types.UnDefType;

import com.google.gson.Gson;

/**
 * Tests for PersistenceItem REST resource
 *
 * @author Stefan Triller - Initial contribution
 * @author Mark Herwege - Implement aliases
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class PersistenceResourceTest {

    private static final String PERSISTENCE_SERVICE_ID = "TestServiceID";
    private static final String PERSISTENCE_SERVICE_ID_2 = "TestServiceID2";
    private static final String ITEM_NAME = "testItem";
    private static final String RESTORE_ONLY_GROUP = "gRestoreOnStartup";

    private @NonNullByDefault({}) PersistenceResource pResource;
    private @NonNullByDefault({}) List<HistoricItem> items;

    private @Mock @NonNullByDefault({}) ItemRegistry itemRegistryMock;
    private @Mock @NonNullByDefault({}) LocaleService localeServiceMock;
    private @Mock @NonNullByDefault({}) PersistenceServiceRegistry persistenceServiceRegistryMock;
    private @Mock @NonNullByDefault({}) ModifiablePersistenceService pServiceMock;
    private @Mock @NonNullByDefault({}) PersistenceManagerImpl persistenceManagerMock;
    private @Mock @NonNullByDefault({}) PersistenceServiceConfigurationRegistry persistenceServiceConfigurationRegistryMock;
    private @Mock @NonNullByDefault({}) ManagedPersistenceServiceConfigurationProvider managedPersistenceServiceConfigurationProviderMock;
    private @Mock @NonNullByDefault({}) TimeZoneProvider timeZoneProviderMock;
    private @Mock @NonNullByDefault({}) ConfigurationService configurationServiceMock;
    private @Mock @NonNullByDefault({}) PersistenceServiceConfiguration persistenceServiceConfigurationMock;
    private @Mock @NonNullByDefault({}) Item itemMock;

    private static final String ITEM = "Test";
    private static final int START_VALUE = 2016;
    private static final int END_VALUE = 2018;
    private static final int VALUE_COUNT = END_VALUE - START_VALUE + 1;

    @BeforeEach
    public void beforeEach() throws ItemNotFoundException {
        pResource = new PersistenceResource(itemRegistryMock, localeServiceMock, persistenceServiceRegistryMock,
                persistenceManagerMock, persistenceServiceConfigurationRegistryMock,
                managedPersistenceServiceConfigurationProviderMock, timeZoneProviderMock, configurationServiceMock);

        items = new ArrayList<>(VALUE_COUNT);
        for (int i = START_VALUE; i <= END_VALUE; i++) {
            final int year = i;
            items.add(new HistoricItem() {
                @Override
                public ZonedDateTime getTimestamp() {
                    return ZonedDateTime.of(year, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
                }

                @Override
                public State getState() {
                    if (year % 2 == 0) {
                        return OnOffType.ON;
                    } else {
                        return OnOffType.OFF;
                    }
                }

                @Override
                public String getName() {
                    return ITEM;
                }
            });
        }

        when(persistenceServiceRegistryMock.get(PERSISTENCE_SERVICE_ID)).thenReturn(pServiceMock);
        when(pServiceMock.getId()).thenReturn(PERSISTENCE_SERVICE_ID);
        when(pServiceMock.query(any(), any())).thenReturn(items);
        when(timeZoneProviderMock.getTimeZone()).thenReturn(ZoneId.systemDefault());
        when(itemRegistryMock.getItem(ITEM_NAME)).thenReturn(itemMock);
    }

    @Test
    public void testGetPersistenceItemData() {
        ItemHistoryDTO dto = pResource.createDTO(pServiceMock, ITEM_NAME, null, null, 1, 10, false, false, false, null);

        assertNotNull(dto);
        assertThat(Integer.parseInt(dto.datapoints), is(5));
        assertThat(dto.data, hasSize(5));

        // since we added binary state type elements, all except the first have to be repeated but with the timestamp of
        // the following item
        HistoryDataBean item0 = dto.data.getFirst();
        HistoryDataBean item1 = dto.data.get(1);

        assertEquals(item0.state, item1.state);
        assertNotEquals(item0.time, item1.time);

        HistoryDataBean item2 = dto.data.get(2);

        assertEquals(item1.time, item2.time);
        assertNotEquals(item1.state, item2.state);

        HistoryDataBean item3 = dto.data.get(3);

        assertEquals(item2.state, item3.state);
        assertNotEquals(item2.time, item3.time);

        HistoryDataBean item4 = dto.data.get(4);

        assertEquals(item3.time, item4.time);
        assertNotEquals(item3.state, item4.state);
    }

    @Test
    public void testGetPersistenceItemDataWithBoundery() {
        ItemHistoryDTO dto = pResource.createDTO(pServiceMock, ITEM_NAME, null, null, 1, 10, true, false, false, null);

        assertNotNull(dto);
        assertThat(Integer.parseInt(dto.datapoints), is(7));
        assertThat(dto.data, hasSize(7));
    }

    @Test
    public void testGetPersistenceItemDataWithItemState() {
        when(itemMock.getState()).thenReturn(DecimalType.ZERO);

        ItemHistoryDTO dto = pResource.createDTO(pServiceMock, ITEM_NAME, null, null, 1, 10, false, true, false, null);

        assertNotNull(dto);
        assertThat(Integer.parseInt(dto.datapoints), is(6));
        assertThat(dto.data, hasSize(6));
        assertThat(dto.data.get(dto.data.size() - 1).state, is("0"));
    }

    @Test
    public void testGetPersistenceItemDataWithItemStateUndefined() {
        when(itemMock.getState()).thenReturn(UnDefType.UNDEF);

        ItemHistoryDTO dto = pResource.createDTO(pServiceMock, ITEM_NAME, null, null, 1, 10, false, true, false, null);

        assertNotNull(dto);
        assertThat(Integer.parseInt(dto.datapoints), is(5));
        assertThat(dto.data, hasSize(5));
    }

    @Test
    public void testGetPersistenceItemDataWithItemStateNull() {
        when(itemMock.getState()).thenReturn(UnDefType.NULL);

        ItemHistoryDTO dto = pResource.createDTO(pServiceMock, ITEM_NAME, null, null, 1, 10, false, true, false, null);

        assertNotNull(dto);
        assertThat(Integer.parseInt(dto.datapoints), is(5));
        assertThat(dto.data, hasSize(5));
    }

    @Test
    public void testGetPersistenceItemDataWithBoundaryAndItemStateButNoItemStateRequired() {
        when(itemMock.getState()).thenReturn(DecimalType.ZERO);

        ItemHistoryDTO dto = pResource.createDTO(pServiceMock, ITEM_NAME, null, null, 1, 10, true, true, false, null);

        assertNotNull(dto);
        assertThat(Integer.parseInt(dto.datapoints), is(7));
        assertThat(dto.data, hasSize(7));
        assertThat(dto.data.get(dto.data.size() - 1).state, not("0"));
    }

    @Test
    public void testGetPersistenceItemDataWithPersistedUnitDifferentThanItemUnit() throws ItemNotFoundException {
        NumberItem item = mock(NumberItem.class);
        doReturn(SIUnits.CELSIUS).when(item).getUnit();
        when(itemRegistryMock.getItem(ITEM_NAME)).thenReturn(item);

        List<HistoricItem> historicItems = new ArrayList<>();
        historicItems.add(new HistoricItem() {
            @Override
            public ZonedDateTime getTimestamp() {
                return ZonedDateTime.now();
            }

            @Override
            public State getState() {
                return new QuantityType<>("68 °F");
            }

            @Override
            public String getName() {
                return ITEM_NAME;
            }
        });
        assertEquals(1, historicItems.size());
        when(pServiceMock.query(any(), any())).thenReturn(historicItems);

        ItemHistoryDTO dto = pResource.createDTO(pServiceMock, ITEM_NAME, null, null, 0, 0, false, false, false, null);

        assertNotNull(dto);
        assertEquals("°C", dto.unit);
        assertThat(dto.data, hasSize(1));
        // 68 °F = 20 °C, we expect only the numeric value in the state field
        assertEquals(20, Float.parseFloat(dto.data.get(0).state), 1e-16);
    }

    @Test
    public void testGetPersistenceItemDataWithDisplayStateUnitConversion() throws ItemNotFoundException {
        NumberItem item = mock(NumberItem.class);
        when(itemRegistryMock.getItem(ITEM_NAME)).thenReturn(item);
        StateDescription stateDescriptionMock = mock(StateDescription.class);
        when(item.getStateDescription(null)).thenReturn(stateDescriptionMock);
        when(stateDescriptionMock.getPattern()).thenReturn("%.1f °F");

        List<HistoricItem> historicItems = new ArrayList<>();
        historicItems.add(new HistoricItem() {
            @Override
            public ZonedDateTime getTimestamp() {
                return ZonedDateTime.now();
            }

            @Override
            public State getState() {
                return new QuantityType<>("20.5 °C");
            }

            @Override
            public String getName() {
                return ITEM_NAME;
            }
        });
        assertEquals(1, historicItems.size());
        when(pServiceMock.query(any(), any())).thenReturn(historicItems);

        ItemHistoryDTO dto = pResource.createDTO(pServiceMock, ITEM_NAME, null, null, 0, 0, false, false, true, null);

        assertNotNull(dto);
        assertEquals("°F", dto.unit);
        assertThat(dto.data, hasSize(1));
        // 20.5 °C = 68.9 °F, we expect only the numeric value in the state field
        assertEquals("68.900", dto.data.get(0).state);
    }

    @Test
    public void testGetPersistenceItemDataWithDisplayStateOptions() throws ItemNotFoundException {
        SwitchItem item = mock(SwitchItem.class);
        when(itemRegistryMock.getItem(ITEM_NAME)).thenReturn(item);
        StateDescription stateDescriptionMock = mock(StateDescription.class);
        when(item.getStateDescription(null)).thenReturn(stateDescriptionMock);
        when(stateDescriptionMock.getOptions())
                .thenReturn(List.of(new StateOption("ON", "an"), new StateOption("OFF", "aus")));

        ItemHistoryDTO dto = pResource.createDTO(pServiceMock, ITEM_NAME, null, null, 0, 0, false, false, true, null);

        assertNotNull(dto);
        assertNull(dto.unit);
        assertThat(Integer.parseInt(dto.datapoints), is(5));
        assertThat(dto.data, hasSize(5));
        // dto.data with raw state would be: [ON, ON, OFF, OFF, ON]
        assertEquals("an", dto.data.get(0).state);
        assertEquals("an", dto.data.get(1).state);
        assertEquals("aus", dto.data.get(2).state);
        assertEquals("aus", dto.data.get(3).state);
        assertEquals("an", dto.data.get(4).state);
    }

    @Test
    public void testPutPersistenceItemData() throws ItemNotFoundException {
        HttpHeaders headersMock = mock(HttpHeaders.class);
        Item item = new NumberItem(ITEM_NAME);
        when(itemRegistryMock.getItem(ITEM_NAME)).thenReturn(item);

        pResource.httpPutPersistenceItemData(headersMock, PERSISTENCE_SERVICE_ID, ITEM_NAME, "2024-02-01T00:00:00.000Z",
                "0");

        verify(persistenceManagerMock).handleExternalPersistenceDataChange(eq(pServiceMock), eq(item));
    }

    @Test
    public void testGetPersistenceItemInfoNotImplemented() throws UnsupportedOperationException {
        // Test method not supported
        when(pServiceMock.getItemInfo()).thenThrow(UnsupportedOperationException.class);
        assertThrows(UnsupportedOperationException.class, () -> pResource.createDTO(pServiceMock, null));
    }

    @Test
    public void testGetPersistenceItemInfo() throws UnsupportedOperationException {
        when(pServiceMock.getItemInfo()).thenReturn(Set.of(new PersistenceItemInfo() {

            @Override
            public String getName() {
                return ITEM;
            }

            @Override
            public @Nullable Integer getCount() {
                return VALUE_COUNT;
            }

            @Override
            public @Nullable Date getEarliest() {
                return Date.from(ZonedDateTime.of(START_VALUE, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()).toInstant());
            }

            @Override
            public @Nullable Date getLatest() {
                return Date.from(ZonedDateTime.of(END_VALUE, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()).toInstant());
            }
        }));

        // Testing with a specific implementation
        Set<PersistenceItemInfoDTO> dto = pResource.createDTO(pServiceMock, null);
        assertNotNull(dto);
        PersistenceItemInfoDTO itemInfo = dto.iterator().next();
        assertThat(itemInfo.name(), is(ITEM));
        assertThat(itemInfo.earliest(),
                is(Date.from(ZonedDateTime.of(START_VALUE, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()).toInstant())));
        assertThat(itemInfo.latest(),
                is(Date.from(ZonedDateTime.of(END_VALUE, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()).toInstant())));
        assertThat(itemInfo.count(), is(VALUE_COUNT));
    }

    @Test
    public void testGetPersistenceItemInfoWithItemDefault() throws UnsupportedOperationException {
        when(pServiceMock.getItemInfo(any(), any())).thenReturn(new PersistenceItemInfo() {

            @Override
            public String getName() {
                return ITEM;
            }

            @Override
            public @Nullable Integer getCount() {
                return null;
            }

            @Override
            public @Nullable Date getEarliest() {
                return null;
            }

            @Override
            public @Nullable Date getLatest() {
                return Date.from(ZonedDateTime.of(END_VALUE, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()).toInstant());
            }
        });

        // This is testing the default behavior when no specific implementation exists in the service
        Set<PersistenceItemInfoDTO> dto = pResource.createDTO(pServiceMock, ITEM);
        assertNotNull(dto);
        assertThat(dto.size(), is(1));
        PersistenceItemInfoDTO itemInfo = dto.iterator().next();
        assertThat(itemInfo.name(), is(ITEM));
        assertNull(itemInfo.earliest());
        assertThat(itemInfo.latest(),
                is(Date.from(ZonedDateTime.of(END_VALUE, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()).toInstant())));
        assertNull(itemInfo.count());
    }

    @Test
    public void testGetPersistenceItemInfoWithItem() throws UnsupportedOperationException {
        when(pServiceMock.getItemInfo(any(), any())).thenAnswer(invocation -> {
            String firstArg = invocation.getArgument(0);
            String secondArg = invocation.getArgument(1);
            if (!firstArg.equals(ITEM)) {
                return null;
            }
            return new PersistenceItemInfo() {

                @Override
                public String getName() {
                    return secondArg != null ? secondArg : firstArg;
                }

                @Override
                public @Nullable Integer getCount() {
                    return VALUE_COUNT;
                }

                @Override
                public @Nullable Date getEarliest() {
                    return Date
                            .from(ZonedDateTime.of(START_VALUE, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()).toInstant());
                }

                @Override
                public @Nullable Date getLatest() {
                    return Date.from(ZonedDateTime.of(END_VALUE, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()).toInstant());
                }
            };
        });

        // Testing when ITEM does not exist
        Set<PersistenceItemInfoDTO> dto = pResource.createDTO(pServiceMock, "NotFoundTest");
        assertNull(dto);

        // Test when specific implementation exists and no alias is used
        dto = pResource.createDTO(pServiceMock, ITEM);
        assertNotNull(dto);
        assertThat(dto.size(), is(1));
        PersistenceItemInfoDTO itemInfo = dto.iterator().next();
        assertThat(itemInfo.name(), is(ITEM));
        assertThat(itemInfo.earliest(),
                is(Date.from(ZonedDateTime.of(START_VALUE, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()).toInstant())));
        assertThat(itemInfo.latest(),
                is(Date.from(ZonedDateTime.of(END_VALUE, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()).toInstant())));
        assertThat(itemInfo.count(), is(VALUE_COUNT));

        // Test when an alias exists
        when(persistenceServiceConfigurationRegistryMock.get(any())).thenReturn(persistenceServiceConfigurationMock);
        when(persistenceServiceConfigurationMock.getAliases()).thenReturn(Map.of(ITEM, "TestAlias"));
        dto = pResource.createDTO(pServiceMock, ITEM);
        assertNotNull(dto);
        assertThat(dto.size(), is(1));
        itemInfo = dto.iterator().next();
        assertThat(itemInfo.name(), is("TestAlias"));
        assertThat(itemInfo.earliest(),
                is(Date.from(ZonedDateTime.of(START_VALUE, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()).toInstant())));
        assertThat(itemInfo.latest(),
                is(Date.from(ZonedDateTime.of(END_VALUE, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()).toInstant())));
        assertThat(itemInfo.count(), is(VALUE_COUNT));
    }

    @Test
    public void testHealthStandardPatternNotReported() throws IOException {
        when(persistenceServiceRegistryMock.getAll()).thenReturn(Set.of(pServiceMock));
        PersistenceItemConfiguration everyChangeAll = itemConfiguration(List.of(PersistenceStrategy.Globals.CHANGE),
                new PersistenceAllConfig());
        PersistenceItemConfiguration restoreOnlyGroup = itemConfiguration(List.of(PersistenceStrategy.Globals.RESTORE),
                new PersistenceGroupConfig(RESTORE_ONLY_GROUP));
        mockServiceConfiguration(PERSISTENCE_SERVICE_ID, everyChangeAll, restoreOnlyGroup);

        List<PersistenceServiceProblem> problems = getPersistenceProblems();

        assertThat(countNoStoreStrategyProblems(problems, PERSISTENCE_SERVICE_ID), is(0L));
    }

    @Test
    public void testHealthRestoreOnlyWithoutCoverageIsReported() throws IOException {
        when(persistenceServiceRegistryMock.getAll()).thenReturn(Set.of(pServiceMock));
        PersistenceItemConfiguration restoreOnlyGroup = itemConfiguration(List.of(PersistenceStrategy.Globals.RESTORE),
                new PersistenceGroupConfig(RESTORE_ONLY_GROUP));
        mockServiceConfiguration(PERSISTENCE_SERVICE_ID, restoreOnlyGroup);

        List<PersistenceServiceProblem> problems = getPersistenceProblems();

        assertThat(countNoStoreStrategyProblems(problems, PERSISTENCE_SERVICE_ID), is(1L));
    }

    @Test
    public void testHealthAllConfigWithExcludeStillReported() throws IOException {
        when(persistenceServiceRegistryMock.getAll()).thenReturn(Set.of(pServiceMock));
        PersistenceItemConfiguration everyChangeAllWithExclude = itemConfiguration(
                List.of(PersistenceStrategy.Globals.CHANGE), new PersistenceAllConfig(),
                new PersistenceItemExcludeConfig("excludedItem"));
        PersistenceItemConfiguration restoreOnlyGroup = itemConfiguration(List.of(PersistenceStrategy.Globals.RESTORE),
                new PersistenceGroupConfig(RESTORE_ONLY_GROUP));
        mockServiceConfiguration(PERSISTENCE_SERVICE_ID, everyChangeAllWithExclude, restoreOnlyGroup);

        List<PersistenceServiceProblem> problems = getPersistenceProblems();

        assertThat(countNoStoreStrategyProblems(problems, PERSISTENCE_SERVICE_ID), is(1L));
    }

    @Test
    public void testHealthCoveredByCronStrategyNotReported() throws IOException {
        when(persistenceServiceRegistryMock.getAll()).thenReturn(Set.of(pServiceMock));
        PersistenceItemConfiguration cronGroup = itemConfiguration(
                List.of(new PersistenceCronStrategy("everyHour", "0 0 * * * ?")),
                new PersistenceGroupConfig(RESTORE_ONLY_GROUP));
        PersistenceItemConfiguration restoreOnlyGroup = itemConfiguration(List.of(PersistenceStrategy.Globals.RESTORE),
                new PersistenceGroupConfig(RESTORE_ONLY_GROUP));
        mockServiceConfiguration(PERSISTENCE_SERVICE_ID, cronGroup, restoreOnlyGroup);

        List<PersistenceServiceProblem> problems = getPersistenceProblems();

        assertThat(countNoStoreStrategyProblems(problems, PERSISTENCE_SERVICE_ID), is(0L));
    }

    @Test
    public void testHealthCoveredOnlyByForecastStillReported() throws IOException {
        when(persistenceServiceRegistryMock.getAll()).thenReturn(Set.of(pServiceMock));
        PersistenceItemConfiguration forecastGroup = itemConfiguration(List.of(PersistenceStrategy.Globals.FORECAST),
                new PersistenceGroupConfig(RESTORE_ONLY_GROUP));
        PersistenceItemConfiguration restoreOnlyGroup = itemConfiguration(List.of(PersistenceStrategy.Globals.RESTORE),
                new PersistenceGroupConfig(RESTORE_ONLY_GROUP));
        mockServiceConfiguration(PERSISTENCE_SERVICE_ID, forecastGroup, restoreOnlyGroup);

        List<PersistenceServiceProblem> problems = getPersistenceProblems();

        assertThat(countNoStoreStrategyProblems(problems, PERSISTENCE_SERVICE_ID), is(1L));
    }

    @Test
    public void testHealthDoesNotBleedAcrossServices() throws IOException {
        PersistenceService serviceAMock = mock(PersistenceService.class);
        when(serviceAMock.getId()).thenReturn(PERSISTENCE_SERVICE_ID);
        PersistenceService serviceBMock = mock(PersistenceService.class);
        when(serviceBMock.getId()).thenReturn(PERSISTENCE_SERVICE_ID_2);
        when(persistenceServiceRegistryMock.getAll()).thenReturn(Set.of(serviceAMock, serviceBMock));

        PersistenceItemConfiguration everyChangeAll = itemConfiguration(List.of(PersistenceStrategy.Globals.CHANGE),
                new PersistenceAllConfig());
        mockServiceConfiguration(PERSISTENCE_SERVICE_ID, everyChangeAll);

        PersistenceItemConfiguration restoreOnlyGroup = itemConfiguration(List.of(PersistenceStrategy.Globals.RESTORE),
                new PersistenceGroupConfig(RESTORE_ONLY_GROUP));
        mockServiceConfiguration(PERSISTENCE_SERVICE_ID_2, restoreOnlyGroup);

        List<PersistenceServiceProblem> problems = getPersistenceProblems();

        assertThat(countNoStoreStrategyProblems(problems, PERSISTENCE_SERVICE_ID), is(0L));
        assertThat(countNoStoreStrategyProblems(problems, PERSISTENCE_SERVICE_ID_2), is(1L));
    }

    @Test
    public void testHealthExactSelectorCoverageNotReported() throws IOException {
        when(persistenceServiceRegistryMock.getAll()).thenReturn(Set.of(pServiceMock));
        PersistenceItemConfiguration restoreOnlyGroup = itemConfiguration(List.of(PersistenceStrategy.Globals.RESTORE),
                new PersistenceGroupConfig(RESTORE_ONLY_GROUP));
        PersistenceItemConfiguration everyUpdateSameGroup = itemConfiguration(
                List.of(PersistenceStrategy.Globals.UPDATE), new PersistenceGroupConfig(RESTORE_ONLY_GROUP));
        mockServiceConfiguration(PERSISTENCE_SERVICE_ID, restoreOnlyGroup, everyUpdateSameGroup);

        List<PersistenceServiceProblem> problems = getPersistenceProblems();

        assertThat(countNoStoreStrategyProblems(problems, PERSISTENCE_SERVICE_ID), is(0L));
    }

    @Test
    public void testHealthArbitraryNamedStrategyDoesNotCountAsStoring() throws IOException {
        when(persistenceServiceRegistryMock.getAll()).thenReturn(Set.of(pServiceMock));
        PersistenceItemConfiguration restoreOnlyGroup = itemConfiguration(List.of(PersistenceStrategy.Globals.RESTORE),
                new PersistenceGroupConfig(RESTORE_ONLY_GROUP));
        // A file-based configuration may name any strategy; PersistenceModelManager turns it into a plain
        // PersistenceStrategy that PersistenceManagerImpl never acts on, so it stores nothing.
        PersistenceItemConfiguration namedStrategySameGroup = itemConfiguration(
                List.of(new PersistenceStrategy("someNamedStrategy")), new PersistenceGroupConfig(RESTORE_ONLY_GROUP));
        mockServiceConfiguration(PERSISTENCE_SERVICE_ID, restoreOnlyGroup, namedStrategySameGroup);

        List<PersistenceServiceProblem> problems = getPersistenceProblems();

        assertThat(countNoStoreStrategyProblems(problems, PERSISTENCE_SERVICE_ID), is(1L));
    }

    @Test
    public void testHealthCoverageMayBeSplitAcrossStoreConfigurations() throws IOException {
        when(persistenceServiceRegistryMock.getAll()).thenReturn(Set.of(pServiceMock));
        // Coverage is additive as well: neither store entry covers both items on its own.
        PersistenceItemConfiguration restoreTwoItems = itemConfiguration(List.of(PersistenceStrategy.Globals.RESTORE),
                new PersistenceItemConfig("ItemA"), new PersistenceItemConfig("ItemB"));
        PersistenceItemConfiguration storeItemA = itemConfiguration(List.of(PersistenceStrategy.Globals.CHANGE),
                new PersistenceItemConfig("ItemA"));
        PersistenceItemConfiguration storeItemB = itemConfiguration(List.of(PersistenceStrategy.Globals.UPDATE),
                new PersistenceItemConfig("ItemB"));
        mockServiceConfiguration(PERSISTENCE_SERVICE_ID, restoreTwoItems, storeItemA, storeItemB);

        List<PersistenceServiceProblem> problems = getPersistenceProblems();

        assertThat(countNoStoreStrategyProblems(problems, PERSISTENCE_SERVICE_ID), is(0L));
    }

    @Test
    public void testHealthPartiallySplitCoverageStillReported() throws IOException {
        when(persistenceServiceRegistryMock.getAll()).thenReturn(Set.of(pServiceMock));
        // Only one of the two selectors is covered - the entry must still be reported.
        PersistenceItemConfiguration restoreTwoItems = itemConfiguration(List.of(PersistenceStrategy.Globals.RESTORE),
                new PersistenceItemConfig("ItemA"), new PersistenceItemConfig("ItemB"));
        PersistenceItemConfiguration storeItemA = itemConfiguration(List.of(PersistenceStrategy.Globals.CHANGE),
                new PersistenceItemConfig("ItemA"));
        mockServiceConfiguration(PERSISTENCE_SERVICE_ID, restoreTwoItems, storeItemA);

        List<PersistenceServiceProblem> problems = getPersistenceProblems();

        assertThat(countNoStoreStrategyProblems(problems, PERSISTENCE_SERVICE_ID), is(1L));
    }

    private static PersistenceItemConfiguration itemConfiguration(List<PersistenceStrategy> strategies,
            PersistenceConfig... selectors) {
        return new PersistenceItemConfiguration(List.of(selectors), strategies, List.of());
    }

    private void mockServiceConfiguration(String serviceId, PersistenceItemConfiguration... configs) {
        when(persistenceServiceConfigurationRegistryMock.get(serviceId)).thenReturn(
                new PersistenceServiceConfiguration(serviceId, List.of(configs), Map.of(), List.of(), List.of()));
    }

    private List<PersistenceServiceProblem> getPersistenceProblems() throws IOException {
        HttpHeaders headersMock = mock(HttpHeaders.class);
        Response response = pResource.httpGetPersistenceHealth(headersMock);
        assertThat(response.getStatus(), is(200));
        PersistenceServiceProblem[] problems = new Gson().fromJson(toString(response.getEntity()),
                PersistenceServiceProblem[].class);
        return List.of(problems);
    }

    private long countNoStoreStrategyProblems(List<PersistenceServiceProblem> problems, String serviceId) {
        return problems.stream()
                .filter(problem -> PersistenceServiceProblem.PERSISTENCE_NO_STORE_STRATEGY.equals(problem.reason())
                        && serviceId.equals(problem.serviceId()))
                .count();
    }

    private String toString(Object entity) throws IOException {
        byte[] bytes;
        if (entity instanceof StreamingOutput streaming) {
            try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                streaming.write(buffer);
                bytes = buffer.toByteArray();
            }
        } else {
            bytes = ((InputStream) entity).readAllBytes();
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
