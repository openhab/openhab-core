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
package org.openhab.core.thing.profiles;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.types.TimeSeries;

/**
 * The {@link TimeSeriesProfile} extends the {@link StateProfile} to support {@link TimeSeries} updates
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public interface TimeSeriesProfile extends StateProfile {

    /**
     * If a binding sends a time-series to a channel, this method will be called for each linked item.
     *
     * @param timeSeries the time-series
     */
    void onTimeSeriesFromHandler(TimeSeries timeSeries);
}
