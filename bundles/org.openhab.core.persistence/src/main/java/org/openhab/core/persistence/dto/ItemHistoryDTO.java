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
package org.openhab.core.persistence.dto;

import java.util.ArrayList;
import java.util.List;

import org.openhab.core.library.types.QuantityType;
import org.openhab.core.types.State;

/**
 * This is a java bean that is used to serialize items to JSON.
 *
 * @author Chris Jackson - Initial contribution
 */
public class ItemHistoryDTO {

    public String name;
    public String totalrecords;
    public String datapoints;

    public List<HistoryDataBean> data = new ArrayList<>();

    public ItemHistoryDTO() {
    };

    /**
     * Add a new record to the data history.
     * This method returns a double value equal to the state. This may be used for comparison by the caller.
     *
     * @param time the time of the record
     * @param state the state at this time
     */
    @SuppressWarnings("rawtypes")
    public void addData(Long time, State state) {
        HistoryDataBean newVal = new HistoryDataBean();
        newVal.time = time;
        if (state instanceof QuantityType) {
            // we strip the unit from the state, since historic item states are expected to be all in the default unit
            newVal.state = ((QuantityType) state).toBigDecimal().toString();
        } else {
            newVal.state = state.toString();
        }
        data.add(newVal);
    }

    public static class HistoryDataBean {
        public Long time;
        public String state;
    }
}
