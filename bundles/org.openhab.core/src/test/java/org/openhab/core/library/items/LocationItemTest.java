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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.PointType;

/**
 * @author Gaël L'hopital - Initial contribution
 * @author Stefan Triller - tests for undef and illegal states
 */
public class LocationItemTest {

    @Test
    public void testDistance() {
        PointType pointParis = new PointType("48.8566140,2.3522219");
        PointType pointBerlin = new PointType("52.5200066,13.4049540");

        LocationItem locationParis = new LocationItem("paris");
        locationParis.setState(pointParis);
        LocationItem locationBerlin = new LocationItem("berlin");
        locationBerlin.setState(pointBerlin);

        DecimalType distance = locationParis.distanceFrom(locationParis);
        assertEquals(distance.intValue(), 0);

        double parisBerlin = locationParis.distanceFrom(locationBerlin).doubleValue();
        assertEquals(parisBerlin, 878400, 50);
    }

    @Test
    public void testUndefType() {
        ImageItem item = new ImageItem("test");
        StateUtil.testUndefStates(item);
    }

    @Test
    public void testAcceptedStates() {
        DateTimeItem item = new DateTimeItem("test");
        StateUtil.testAcceptedStates(item);
    }

}
