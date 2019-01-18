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
package org.eclipse.smarthome.model.persistence.extensions;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.persistence.HistoricItem;
import org.eclipse.smarthome.core.persistence.PersistenceService;
import org.eclipse.smarthome.core.persistence.PersistenceServiceRegistry;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.model.persistence.tests.TestPersistenceService;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Kai Kreuzer - Initial contribution and API
 * @author Chris Jackson
 * @author Jan N. Klug
 */
@SuppressWarnings("deprecation")
public class PersistenceExtensionsTest {

    private final PersistenceServiceRegistry registry = new PersistenceServiceRegistry() {

        private final PersistenceService testPersistenceService = new TestPersistenceService();

        @Override
        public String getDefaultId() {
            return null;
        }

        @Override
        public PersistenceService getDefault() {
            return testPersistenceService;
        }

        @Override
        public Set<PersistenceService> getAll() {
            return null;
        }

        @Override
        public PersistenceService get(String serviceId) {
            return testPersistenceService;
        }
    };

    private PersistenceExtensions ext;
    private GenericItem item;

    @Before
    public void setUp() {
        ext = new PersistenceExtensions();
        ext.setPersistenceServiceRegistry(registry);
        item = new GenericItem("Test", "Test") {
            @Override
            public List<Class<? extends State>> getAcceptedDataTypes() {
                return Collections.emptyList();
            }

            @Override
            public List<Class<? extends Command>> getAcceptedCommandTypes() {
                return Collections.emptyList();
            }
        };
    }

    @After
    public void tearDown() {
        ext.unsetPersistenceServiceRegistry(registry);
    }

    @Test
    public void testHistoricState() {
        HistoricItem historicItem = PersistenceExtensions.historicState(item, new DateMidnight(2012, 1, 1), "test");
        assertEquals("2012", historicItem.getState().toString());

        historicItem = PersistenceExtensions.historicState(item, new DateMidnight(2011, 12, 31), "test");
        assertEquals("2011", historicItem.getState().toString());

        historicItem = PersistenceExtensions.historicState(item, new DateMidnight(2011, 1, 1), "test");
        assertEquals("2011", historicItem.getState().toString());

        historicItem = PersistenceExtensions.historicState(item, new DateMidnight(2000, 1, 1), "test");
        assertEquals("2000", historicItem.getState().toString());
    }

    @Test
    public void testMinimumSince() {
        item.setState(new DecimalType(5000));
        HistoricItem historicItem = PersistenceExtensions.minimumSince(item, new DateMidnight(1940, 1, 1), "test");
        assertNotNull(historicItem);
        assertEquals("5000", historicItem.getState().toString());

        historicItem = PersistenceExtensions.minimumSince(item, new DateMidnight(2005, 1, 1), "test");
        assertEquals("2005", historicItem.getState().toString());
        assertEquals(new DateMidnight(2005, 1, 1).toDate(), historicItem.getTimestamp());
    }

    @Test
    public void testMaximumSince() {
        item.setState(new DecimalType(1));
        HistoricItem historicItem = PersistenceExtensions.maximumSince(item, new DateMidnight(2012, 1, 1), "test");
        assertNotNull(historicItem);
        assertEquals("1", historicItem.getState().toString());

        historicItem = PersistenceExtensions.maximumSince(item, new DateMidnight(2005, 1, 1), "test");
        assertEquals("2012", historicItem.getState().toString());
        assertEquals(new DateMidnight(2012, 1, 1).toDate(), historicItem.getTimestamp());
    }

    @Test
    public void testAverageSince() {
        item.setState(new DecimalType(3025));
        DateMidnight startStored = new DateMidnight(2003, 1, 1);
        DateMidnight endStored = new DateMidnight(2012, 1, 1);
        long storedInterval = endStored.getMillis() - startStored.getMillis();
        long recentInterval = DateTime.now().getMillis() - endStored.getMillis();
        double expected = (2007.4994 * storedInterval + 2518.5 * recentInterval) / (storedInterval + recentInterval);
        DecimalType average = PersistenceExtensions.averageSince(item, startStored, "test");
        assertEquals(expected, average.doubleValue(), 0.01);
    }

    @Test
    public void testPreviousStateNoSkip() {
        item.setState(new DecimalType(4321));
        HistoricItem prevStateItem = PersistenceExtensions.previousState(item, false, "test");
        assertNotNull(prevStateItem);
        assertEquals("2012", prevStateItem.getState().toString());

        item.setState(new DecimalType(2012));
        prevStateItem = PersistenceExtensions.previousState(item, false, "test");
        assertNotNull(prevStateItem);
        assertEquals("2012", prevStateItem.getState().toString());
    }

    @Test
    public void testPreviousStateSkip() {
        item.setState(new DecimalType(2012));
        HistoricItem prevStateItem = PersistenceExtensions.previousState(item, true, "test");
        assertNotNull(prevStateItem);
        assertEquals("2011", prevStateItem.getState().toString());
    }
}
