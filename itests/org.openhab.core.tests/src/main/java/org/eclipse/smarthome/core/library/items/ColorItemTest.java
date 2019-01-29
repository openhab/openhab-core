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
package org.eclipse.smarthome.core.library.items;

import static org.junit.Assert.assertEquals;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.junit.Test;

/**
 *
 * @author Simon Kaufmann - initial contribution and API
 * @author Stefan Triller - test with on/off type
 *
 */
public class ColorItemTest {

    @Test
    public void testSetStateWithHSBType() {
        ColorItem item = new ColorItem("test");
        item.setState(new HSBType(new DecimalType(75), new PercentType(75), new PercentType(75)));
        assertEquals(new HSBType(new DecimalType(75), new PercentType(75), new PercentType(75)), item.getState());
    }

    @Test
    public void testSetStateWithPercentType() {
        ColorItem item = new ColorItem("test");
        item.setState(new PercentType(50));
        assertEquals(new HSBType(new DecimalType(0), new PercentType(0), new PercentType(50)), item.getState());
    }

    @Test
    public void testSetStateWithOnOffType() {
        ColorItem item = new ColorItem("test");
        item.setState(OnOffType.ON);
        assertEquals(new HSBType("0,0,100"), item.getState());
        item.setState(OnOffType.OFF);
        assertEquals(new HSBType("0,0,0"), item.getState());
    }

    @Test
    public void testUndefType() {
        ColorItem item = new ColorItem("test");
        StateUtil.testUndefStates(item);
    }

    @Test
    public void testAcceptedStates() {
        ColorItem item = new ColorItem("test");
        StateUtil.testAcceptedStates(item);
    }

    @Test
    public void testUpdateStateWithPercentType() {
        ColorItem item = new ColorItem("test");
        item.setState(new HSBType(new DecimalType(75), new PercentType(75), new PercentType(75)));

        item.setState(new PercentType(50));
        assertEquals(new HSBType(new DecimalType(75), new PercentType(75), new PercentType(50)), item.getState());
    }

}
