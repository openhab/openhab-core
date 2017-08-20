/**
 * Copyright (c) 2015-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.core.persistence.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

import org.eclipse.smarthome.core.persistence.FilterCriteria;
import org.eclipse.smarthome.core.persistence.HistoricItem;
import org.eclipse.smarthome.core.persistence.PersistenceItemInfo;
import org.eclipse.smarthome.core.persistence.QueryablePersistenceService;
import org.eclipse.smarthome.core.types.State;
import org.openhab.core.compat1x.internal.TypeMapper;
import org.openhab.core.persistence.FilterCriteria.Operator;
import org.openhab.core.persistence.FilterCriteria.Ordering;

/**
 * This class serves as a mapping from the "old" org.openhab namespace to the new org.eclipse.smarthome
 * namespace for the queryable persistence service. It wraps an instance with the old interface
 * into a class with the new interface.
 *
 * @author Kai Kreuzer - Initial contribution and API
 */
public class QueryablePersistenceServiceDelegate extends PersistenceServiceDelegate
        implements QueryablePersistenceService {

    public QueryablePersistenceServiceDelegate(org.openhab.core.persistence.PersistenceService persistenceService) {
        super(persistenceService);
    }

    @Override
    public Iterable<HistoricItem> query(FilterCriteria filter) {
        org.openhab.core.persistence.FilterCriteria mappedFilter = new org.openhab.core.persistence.FilterCriteria()
                .setBeginDate(filter.getBeginDate()).setEndDate(filter.getEndDate()).setItemName(filter.getItemName())
                .setOperator(mapOperator(filter.getOperator())).setOrdering(mapOrdering(filter.getOrdering()))
                .setPageNumber(filter.getPageNumber()).setPageSize(filter.getPageSize())
                .setState(mapState(filter.getState()));
        org.openhab.core.persistence.QueryablePersistenceService pService = (org.openhab.core.persistence.QueryablePersistenceService) service;
        Iterable<org.openhab.core.persistence.HistoricItem> historicItems = pService.query(mappedFilter);
        ArrayList<HistoricItem> result = new ArrayList<>();
        for (final org.openhab.core.persistence.HistoricItem item : historicItems) {
            result.add(new HistoricItem() {
                @Override
                public Date getTimestamp() {
                    return item.getTimestamp();
                }

                @Override
                public State getState() {
                    return (State) TypeMapper.mapToESHType(item.getState());
                }

                @Override
                public String getName() {
                    return item.getName();
                }
            });
        }
        return result;
    }

    private org.openhab.core.types.State mapState(State state) {
        return (org.openhab.core.types.State) TypeMapper.mapToOpenHABType(state);
    }

    private Ordering mapOrdering(FilterCriteria.Ordering ordering) {
        if (ordering == null) {
            return null;
        }

        return org.openhab.core.persistence.FilterCriteria.Ordering.valueOf(ordering.toString());
    }

    private Operator mapOperator(FilterCriteria.Operator operator) {
        if (operator == null) {
            return null;
        }
        return org.openhab.core.persistence.FilterCriteria.Operator.valueOf(operator.toString());
    }

    @Override
    public Set<PersistenceItemInfo> getItemInfo() {
        return Collections.emptySet();
    }

}
