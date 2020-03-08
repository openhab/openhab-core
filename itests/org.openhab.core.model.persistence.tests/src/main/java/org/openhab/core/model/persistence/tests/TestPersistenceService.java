/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.model.persistence.tests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Item;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.persistence.FilterCriteria;
import org.openhab.core.persistence.FilterCriteria.Ordering;
import org.openhab.core.persistence.HistoricItem;
import org.openhab.core.persistence.PersistenceItemInfo;
import org.openhab.core.persistence.QueryablePersistenceService;
import org.openhab.core.persistence.strategy.PersistenceStrategy;
import org.openhab.core.types.State;

/**
 * A simple persistence service used for unit tests
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class TestPersistenceService implements QueryablePersistenceService {

    public static final String ID = "test";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void store(Item item) {
    }

    @Override
    public void store(Item item, @Nullable String alias) {
    }

    @SuppressWarnings("deprecation")
    @Override
    public Iterable<HistoricItem> query(FilterCriteria filter) {
        int startValue = 1950;
        int endValue = 2012;

        if (filter.getBeginDateZoned() != null) {
            startValue = filter.getBeginDateZoned().getYear();
        }
        if (filter.getEndDateZoned() != null) {
            endValue = filter.getEndDateZoned().getYear();
        }

        if (endValue <= startValue || startValue < 1950) {
            return Collections.emptyList();
        }

        List<HistoricItem> results = new ArrayList<>(endValue - startValue);
        for (int i = startValue; i <= endValue; i++) {
            final int year = i;
            results.add(new HistoricItem() {
                @Override
                public Date getTimestamp() {
                    return new Date(year - 1900, 0, 1);
                }

                @Override
                public State getState() {
                    return new DecimalType(year);
                }

                @Override
                public String getName() {
                    return "Test";
                }
            });
        }
        if (filter.getOrdering() == Ordering.DESCENDING) {
            Collections.reverse(results);
        }
        return results;
    }

    @Override
    public Set<PersistenceItemInfo> getItemInfo() {
        return Collections.emptySet();
    }

    @Override
    public String getLabel(@Nullable Locale locale) {
        return "Test Label";
    }

    @Override
    public List<PersistenceStrategy> getDefaultStrategies() {
        return Collections.emptyList();
    }

}
