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
package org.openhab.core.library.items;

import java.util.ArrayList;
import java.util.Collections;
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
import org.openhab.core.types.UnDefType;

/**
 * A LocationItem can be used to store GPS related informations, addresses...
 * This is useful for location awareness related functions
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public class LocationItem extends GenericItem {

    private static List<Class<? extends State>> acceptedDataTypes = new ArrayList<>();
    private static List<Class<? extends Command>> acceptedCommandTypes = new ArrayList<>();

    static {
        acceptedDataTypes.add(PointType.class);
        acceptedDataTypes.add(UnDefType.class);

        acceptedCommandTypes.add(RefreshType.class);
        acceptedCommandTypes.add(PointType.class);
    }

    public LocationItem(String name) {
        super(CoreItemFactory.LOCATION, name);
    }

    public void send(PointType command) {
        internalSend(command);
    }

    @Override
    public List<Class<? extends State>> getAcceptedDataTypes() {
        return Collections.unmodifiableList(acceptedDataTypes);
    }

    @Override
    public List<Class<? extends Command>> getAcceptedCommandTypes() {
        return Collections.unmodifiableList(acceptedCommandTypes);
    }

    /**
     * Compute the distance with another Point type,
     * http://stackoverflow.com/questions/837872/calculate-distance-in-meters-when-you-know-longitude-and-latitude-in-
     * java
     *
     * @param away : the point to calculate the distance with
     * @return distance between the two points in meters
     */
    public DecimalType distanceFrom(@Nullable LocationItem awayItem) {
        if (awayItem != null && awayItem.state instanceof PointType && this.state instanceof PointType) {
            PointType thisPoint = (PointType) this.state;
            PointType awayPoint = (PointType) awayItem.state;
            return thisPoint.distanceFrom(awayPoint);
        }
        return new DecimalType(-1);
    }

    @Override
    public void setState(State state) {
        if (isAcceptedState(acceptedDataTypes, state)) {
            super.setState(state);
        } else {
            logSetTypeError(state);
        }
    }
}
