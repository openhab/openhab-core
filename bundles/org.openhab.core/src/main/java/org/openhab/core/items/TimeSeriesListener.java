/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.items;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.types.TimeSeries;

/**
 * <p>
 * This interface must be implemented by all classes that want to be notified about |@link TimeSeries} updates of an
 * item.
 *
 * <p>
 * The {@link GenericItem} class provides the possibility to register such listeners.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public interface TimeSeriesListener {

    /**
     * This method is called, if a time series update was sent to the item.
     *
     * @param item the item the timeseries was updated for
     * @param timeSeries the time series
     */
    void timeSeriesUpdated(Item item, TimeSeries timeSeries);
}
