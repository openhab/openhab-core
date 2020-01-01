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
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.UpDownType;
import org.openhab.core.types.State;

/**
 *
 * @author Hans Hazelius - Initial contribution
 * @author Stefan Triller - Tests for type conversions
 */
public class RollershutterItemTest {

    @Test
    public void setStateStateDownReturnPercent100() {
        RollershutterItem sut = new RollershutterItem("Test");
        State state = UpDownType.DOWN;
        sut.setState(state);
        assertEquals(PercentType.HUNDRED, sut.getState());
    }

    @Test
    public void setStateStateUpReturnPercent0() {
        RollershutterItem sut = new RollershutterItem("Test");
        State state = UpDownType.UP;
        sut.setState(state);
        assertEquals(PercentType.ZERO, sut.getState());
    }

    @Test
    public void setStateStatePercent50ReturnPercent50() {
        RollershutterItem sut = new RollershutterItem("Test");
        State state = new PercentType(50);
        sut.setState(state);
        assertEquals(state, sut.getState());
    }

    @Test
    public void setStateStateHSB50ReturnPercent50() {
        // HSB supported because it is a sub-type of PercentType
        RollershutterItem sut = new RollershutterItem("Test");
        State state = new HSBType("5,23,42");
        sut.setState(state);
        assertEquals(new PercentType(42), sut.getState());
    }

    @Test
    public void setStateStateUndef() {
        RollershutterItem sut = new RollershutterItem("Test");
        StateUtil.testUndefStates(sut);
    }

    @Test
    public void testAcceptedStates() {
        RollershutterItem item = new RollershutterItem("testItem");
        StateUtil.testAcceptedStates(item);
    }
}
