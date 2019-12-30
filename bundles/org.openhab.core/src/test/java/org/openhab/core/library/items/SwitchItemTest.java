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
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;

/**
 * @author Chris Jackson - Initial contribution
 * @author Stefan Triller - more tests for type conversions
 */
public class SwitchItemTest {

    @Test
    public void getAsPercentFromSwitch() {
        SwitchItem item = new SwitchItem("Test");
        item.setState(OnOffType.ON);
        assertEquals(new PercentType(100), item.getStateAs(PercentType.class));
        item.setState(OnOffType.OFF);
        assertEquals(new PercentType(0), item.getStateAs(PercentType.class));
    }

    @Test
    public void getAsDecimalFromSwitch() {
        SwitchItem item = new SwitchItem("Test");
        item.setState(OnOffType.ON);
        assertEquals(new DecimalType(1), item.getStateAs(DecimalType.class));
        item.setState(OnOffType.OFF);
        assertEquals(new DecimalType(0), item.getStateAs(DecimalType.class));
    }

    @Test
    public void getAsHSBFromSwitch() {
        SwitchItem item = new SwitchItem("Test");
        item.setState(OnOffType.ON);
        assertEquals(new HSBType("0,0,100"), item.getStateAs(HSBType.class));
        item.setState(OnOffType.OFF);
        assertEquals(new HSBType("0,0,0"), item.getStateAs(HSBType.class));
    }

    @Test
    public void testUndefType() {
        SwitchItem item = new SwitchItem("test");
        StateUtil.testUndefStates(item);
    }

    @Test
    public void testAcceptedStates() {
        SwitchItem item = new SwitchItem("test");
        StateUtil.testAcceptedStates(item);
    }

}
