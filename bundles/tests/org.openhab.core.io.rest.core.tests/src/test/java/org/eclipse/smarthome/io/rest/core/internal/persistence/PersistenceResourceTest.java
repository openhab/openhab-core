/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.io.rest.core.internal.persistence;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import org.eclipse.smarthome.core.i18n.TimeZoneProvider;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.persistence.HistoricItem;
import org.eclipse.smarthome.core.persistence.PersistenceServiceRegistry;
import org.eclipse.smarthome.core.persistence.QueryablePersistenceService;
import org.eclipse.smarthome.core.persistence.dto.ItemHistoryDTO;
import org.eclipse.smarthome.core.persistence.dto.ItemHistoryDTO.HistoryDataBean;
import org.eclipse.smarthome.core.types.State;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for PersistenceItem Restresource
 *
 * @author Stefan Triller - initial contribution
 *
 */
public class PersistenceResourceTest {

    private final static String PERSISTENCE_SERVICE_ID = "TestServiceID";

    private PersistenceResource pResource;
    private ArrayList<HistoricItem> items;

    @Before
    public void setup() {
        pResource = new PersistenceResource();

        int startValue = 2016;
        int endValue = 2018;
        items = new ArrayList<HistoricItem>(endValue - startValue);
        for (int i = startValue; i <= endValue; i++) {
            final int year = i;
            items.add(new HistoricItem() {
                @Override
                public Date getTimestamp() {
                    return new Date(year - 1900, 0, 1);
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

        QueryablePersistenceService pService = mock(QueryablePersistenceService.class);
        when(pService.query(any())).thenReturn(items);

        TimeZoneProvider timeZoneProvider = mock(TimeZoneProvider.class);
        when(timeZoneProvider.getTimeZone()).thenReturn(TimeZone.getDefault().toZoneId());

        PersistenceServiceRegistry pServiceRegistry = mock(PersistenceServiceRegistry.class);
        when(pServiceRegistry.get(PERSISTENCE_SERVICE_ID)).thenReturn(pService);

        pResource.setPersistenceServiceRegistry(pServiceRegistry);
        pResource.setTimeZoneProvider(timeZoneProvider);
    }

    @Test
    public void testGetPersistenceItemData() {
        // pResource.httpGetPersistenceItemData(headers, serviceId, itemName, startTime, endTime, pageNumber,
        // pageLength, boundary)
        ItemHistoryDTO dto = pResource.createDTO(PERSISTENCE_SERVICE_ID, "testItem", null, null, 1, 5, false);

        assertEquals(5, Integer.parseInt(dto.datapoints));

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
}
