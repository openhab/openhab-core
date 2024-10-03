/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.io.rest.LocaleService;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.persistence.HistoricItem;
import org.openhab.core.persistence.ModifiablePersistenceService;
import org.openhab.core.persistence.PersistenceServiceRegistry;
import org.openhab.core.persistence.dto.ItemHistoryDTO;
import org.openhab.core.persistence.dto.ItemHistoryDTO.HistoryDataBean;
import org.openhab.core.persistence.internal.PersistenceManagerImpl;
import org.openhab.core.persistence.registry.ManagedPersistenceServiceConfigurationProvider;
import org.openhab.core.persistence.registry.PersistenceServiceConfigurationRegistry;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

/**
 * Tests for PersistenceItem Restresource
 *
 * @author Stefan Triller - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class PersistenceResourceTest {

    private static final String PERSISTENCE_SERVICE_ID = "TestServiceID";

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
    private @Mock @NonNullByDefault({}) Item itemMock;

    @BeforeEach
    public void beforeEach() {
        pResource = new PersistenceResource(itemRegistryMock, localeServiceMock, persistenceServiceRegistryMock,
                persistenceManagerMock, persistenceServiceConfigurationRegistryMock,
                managedPersistenceServiceConfigurationProviderMock, timeZoneProviderMock);

        int startValue = 2016;
        int endValue = 2018;
        items = new ArrayList<>(endValue - startValue);
        for (int i = startValue; i <= endValue; i++) {
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
                    return "Test";
                }
            });
        }

        when(pServiceMock.query(any())).thenReturn(items);

        when(persistenceServiceRegistryMock.get(PERSISTENCE_SERVICE_ID)).thenReturn(pServiceMock);
        when(timeZoneProviderMock.getTimeZone()).thenReturn(ZoneId.systemDefault());
    }

    @Test
    public void testGetPersistenceItemData() {
        ItemHistoryDTO dto = pResource.createDTO(PERSISTENCE_SERVICE_ID, "testItem", null, null, 1, 10, false, false);

        assertThat(Integer.parseInt(dto.datapoints), is(5));
        assertThat(dto.data, hasSize(5));

        // since we added binary state type elements, all except the first have to be repeated but with the timestamp of
        // the following item
        HistoryDataBean item0 = dto.data.get(0);
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
        ItemHistoryDTO dto = pResource.createDTO(PERSISTENCE_SERVICE_ID, "testItem", null, null, 1, 10, true, false);

        assertThat(Integer.parseInt(dto.datapoints), is(7));
        assertThat(dto.data, hasSize(7));
    }

    @Test
    public void testGetPersistenceItemDataWithItemState() throws ItemNotFoundException {
        when(itemRegistryMock.getItem("testItem")).thenReturn(itemMock);
        when(itemMock.getState()).thenReturn(DecimalType.ZERO);

        ItemHistoryDTO dto = pResource.createDTO(PERSISTENCE_SERVICE_ID, "testItem", null, null, 1, 10, false, true);

        assertThat(Integer.parseInt(dto.datapoints), is(6));
        assertThat(dto.data, hasSize(6));
        assertThat(dto.data.get(dto.data.size() - 1).state, is("0"));
    }

    @Test
    public void testGetPersistenceItemDataWithItemStateUndefined() throws ItemNotFoundException {
        when(itemRegistryMock.getItem("testItem")).thenReturn(itemMock);
        when(itemMock.getState()).thenReturn(UnDefType.UNDEF);

        ItemHistoryDTO dto = pResource.createDTO(PERSISTENCE_SERVICE_ID, "testItem", null, null, 1, 10, false, true);

        assertThat(Integer.parseInt(dto.datapoints), is(5));
        assertThat(dto.data, hasSize(5));
    }

    @Test
    public void testGetPersistenceItemDataWithItemStateNull() throws ItemNotFoundException {
        when(itemRegistryMock.getItem("testItem")).thenReturn(itemMock);
        when(itemMock.getState()).thenReturn(UnDefType.NULL);

        ItemHistoryDTO dto = pResource.createDTO(PERSISTENCE_SERVICE_ID, "testItem", null, null, 1, 10, false, true);

        assertThat(Integer.parseInt(dto.datapoints), is(5));
        assertThat(dto.data, hasSize(5));
    }

    @Test
    public void testGetPersistenceItemDataWithBoundaryAndItemStateButNoItemStateRequired()
            throws ItemNotFoundException {
        when(itemRegistryMock.getItem("testItem")).thenReturn(itemMock);
        when(itemMock.getState()).thenReturn(DecimalType.ZERO);

        ItemHistoryDTO dto = pResource.createDTO(PERSISTENCE_SERVICE_ID, "testItem", null, null, 1, 10, true, true);

        assertThat(Integer.parseInt(dto.datapoints), is(7));
        assertThat(dto.data, hasSize(7));
        assertThat(dto.data.get(dto.data.size() - 1).state, not("0"));
    }

    @Test
    public void testPutPersistenceItemData() throws ItemNotFoundException {
        HttpHeaders headersMock = mock(HttpHeaders.class);
        Item item = new NumberItem("itemName");
        when(itemRegistryMock.getItem("itemName")).thenReturn(item);

        pResource.httpPutPersistenceItemData(headersMock, PERSISTENCE_SERVICE_ID, "itemName",
                "2024-02-01T00:00:00.000Z", "0");

        verify(persistenceManagerMock).handleExternalPersistenceDataChange(eq(pServiceMock), eq(item));
    }
}
