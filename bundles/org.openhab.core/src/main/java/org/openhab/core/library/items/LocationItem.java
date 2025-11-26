/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.library.items;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.GenericItem;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.PointType;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.TimeSeries;
import org.openhab.core.types.UnDefType;

/**
 * A LocationItem can be used to store GPS related informations, addresses...
 * This is useful for location awareness related functions
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public class LocationItem extends GenericItem {

    private static final List<Class<? extends State>> ACCEPTED_DATA_TYPES = List.of(PointType.class, UnDefType.class);
    private static final List<Class<? extends Command>> ACCEPTED_COMMAND_TYPES = List.of(PointType.class,
            RefreshType.class);

    public LocationItem(String name) {
        super(CoreItemFactory.LOCATION, name);
    }

    /**
     * Send a PointType command to the item.
     *
     * @param command the command to be sent
     */
    public void send(PointType command) {
        internalSend(command, null);
    }

    /**
     * Send a PointType command to the item.
     *
     * @param command the command to be sent
     * @param source the source of the command. See
     *            https://www.openhab.org/docs/developer/utils/events.html#the-core-events
     */
    public void send(PointType command, @Nullable String source) {
        internalSend(command, source);
    }

    @Override
    public List<Class<? extends State>> getAcceptedDataTypes() {
        return ACCEPTED_DATA_TYPES;
    }

    @Override
    public List<Class<? extends Command>> getAcceptedCommandTypes() {
        return ACCEPTED_COMMAND_TYPES;
    }

    /**
     * Compute the distance with another Point type,
     * http://stackoverflow.com/questions/837872/calculate-distance-in-meters-when-you-know-longitude-and-latitude-in-
     * java
     *
     * @param awayItem the point to calculate the distance with
     * @return distance between the two points in meters
     */
    public DecimalType distanceFrom(@Nullable LocationItem awayItem) {
        if (awayItem != null && awayItem.state instanceof PointType awayPoint
                && this.state instanceof PointType thisPoint) {
            return thisPoint.distanceFrom(awayPoint);
        }
        return new DecimalType(-1);
    }

    @Override
    public void setState(State state, @Nullable String source) {
        if (isAcceptedState(ACCEPTED_DATA_TYPES, state)) {
            applyState(state, source);
        } else {
            logSetTypeError(state);
        }
    }

    @Override
    public void setTimeSeries(TimeSeries timeSeries) {
        if (timeSeries.getStates().allMatch(s -> s.state() instanceof PointType)) {
            applyTimeSeries(timeSeries);
        } else {
            logSetTypeError(timeSeries);
        }
    }
}
