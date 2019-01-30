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

import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.UpDownType;
import org.eclipse.smarthome.core.types.State;
import org.junit.Test;

/**
 *
 * @author Hans Hazelius - Initial version
 * @author Stefan Triller - Tests for type conversions
 *
 */
public class RollershutterItemTest {

    @Test
    public void setState_stateDown_returnPercent100() {
        RollershutterItem sut = new RollershutterItem("Test");
        State state = UpDownType.DOWN;
        sut.setState(state);
        assertEquals(PercentType.HUNDRED, sut.getState());
    }

    @Test
    public void setState_stateUp_returnPercent0() {
        RollershutterItem sut = new RollershutterItem("Test");
        State state = UpDownType.UP;
        sut.setState(state);
        assertEquals(PercentType.ZERO, sut.getState());
    }

    @Test
    public void setState_statePercent50_returnPercent50() {
        RollershutterItem sut = new RollershutterItem("Test");
        State state = new PercentType(50);
        sut.setState(state);
        assertEquals(state, sut.getState());
    }

    @Test
    public void setState_stateHSB50_returnPercent50() {
        // HSB supported because it is a sub-type of PercentType
        RollershutterItem sut = new RollershutterItem("Test");
        State state = new HSBType("5,23,42");
        sut.setState(state);
        assertEquals(new PercentType(42), sut.getState());
    }

    @Test
    public void setState_stateUndef() {
        RollershutterItem sut = new RollershutterItem("Test");
        StateUtil.testUndefStates(sut);
    }

    @Test
    public void testAcceptedStates() {
        RollershutterItem item = new RollershutterItem("testItem");
        StateUtil.testAcceptedStates(item);
    }
}
