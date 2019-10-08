/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
import java.util.List;

import org.openhab.core.items.GenericItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.PointType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

/**
 * A LocationItem can be used to store GPS related informations, addresses...
 * This is useful for location awareness related functions
 *
 * @author Gaël L'hopital
 */
public class LocationItem extends GenericItem {
    private static List<Class<? extends State>> acceptedDataTypes = new ArrayList<>();
    private static List<Class<? extends Command>> acceptedCommandTypes = new ArrayList<>();

    static {
        acceptedDataTypes.add(PointType.class);
        acceptedDataTypes.add(UnDefType.class);
    }

    public LocationItem(String name) {
        super(name);
    }

    @Override
    public List<Class<? extends State>> getAcceptedDataTypes() {
        return acceptedDataTypes;
    }

    @Override
    public List<Class<? extends Command>> getAcceptedCommandTypes() {
        return acceptedCommandTypes;
    }

    /**
     * Compute the distance with another Point type,
     * http://stackoverflow.com/questions/837872/calculate-distance-in-meters-when-you-know-longitude-and-latitude-in-java
     *
     * @return distance between the two points in meters
     */
    public DecimalType distanceFrom(PointType away) {

        double dist = -1;

        if ((away != null) && (this.state instanceof PointType)) {

            PointType me = (PointType) this.state;

            double dLat = Math.pow(
                    Math.sin(Math.toRadians(away.getLatitude().doubleValue() - me.getLatitude().doubleValue()) / 2), 2);
            double dLng = Math.pow(
                    Math.sin(Math.toRadians(away.getLongitude().doubleValue() - me.getLongitude().doubleValue()) / 2),
                    2);
            double a = dLat + Math.cos(Math.toRadians(me.getLatitude().doubleValue()))
                    * Math.cos(Math.toRadians(away.getLatitude().doubleValue())) * dLng;

            dist = PointType.WGS84_A * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        }

        return new DecimalType(dist);
    }

}
