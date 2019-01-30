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

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.measure.quantity.Temperature;

import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.PlayPauseType;
import org.eclipse.smarthome.core.library.types.PointType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.types.RawType;
import org.eclipse.smarthome.core.library.types.RewindFastforwardType;
import org.eclipse.smarthome.core.library.types.StringListType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.library.types.UpDownType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;

/**
 * Utility class for testing states on items
 *
 * @author Stefan Triller - Initial version
 *
 */
public class StateUtil {

    public static List<State> getAllStates() {
        LinkedList<State> states = new LinkedList<>();

        DateTimeType dateTime = new DateTimeType();
        states.add(dateTime);

        DecimalType decimal = new DecimalType(23);
        states.add(decimal);

        PercentType percent = new PercentType(50);
        states.add(percent);

        HSBType hsb = new HSBType("50,75,42");
        states.add(hsb);

        states.add(OnOffType.ON);
        states.add(OnOffType.OFF);

        states.add(OpenClosedType.OPEN);
        states.add(OpenClosedType.CLOSED);

        states.add(PlayPauseType.PLAY);
        states.add(PlayPauseType.PAUSE);

        PointType point = new PointType("42.23,23.5");
        states.add(point);

        RawType raw = new RawType(new byte[0], "application/octet-stream");
        states.add(raw);

        states.add(RewindFastforwardType.REWIND);
        states.add(RewindFastforwardType.FASTFORWARD);

        StringListType stringList = new StringListType(new String[] { "foo", "bar" });
        states.add(stringList);

        StringType string = new StringType("foo");
        states.add(string);

        states.add(UnDefType.NULL);
        states.add(UnDefType.UNDEF);

        states.add(UpDownType.UP);
        states.add(UpDownType.DOWN);

        QuantityType<Temperature> quantityType = new QuantityType<Temperature>("12 °C");
        states.add(quantityType);

        return states;
    }

    public static void testAcceptedStates(GenericItem item) {
        HashSet<Class<? extends State>> successfullStates = new HashSet<>();

        for (State s : getAllStates()) {
            item.setState(s);
            if (item.isAcceptedState(item.getAcceptedDataTypes(), s)) {
                if (s.equals(UnDefType.NULL)) {
                    // if we set null, the item should stay null
                    assertEquals(UnDefType.NULL, item.getState());
                } else {
                    // the state should be set on the item now
                    assertNotEquals(UnDefType.NULL, item.getState());
                    successfullStates.add(s.getClass());
                }
                // reset item
                item.setState(UnDefType.NULL);
            } else {
                assertEquals(UnDefType.NULL, item.getState());
            }
        }

        // test if the item accepts a state that is not in our test state list
        for (Class<? extends State> acceptedState : item.getAcceptedDataTypes()) {
            if (!successfullStates.contains(acceptedState)) {
                fail("Item '" + item.getType() + "' accepts untested state: " + acceptedState);
            }
        }
    }

    public static void testUndefStates(GenericItem item) {
        item.setState(UnDefType.UNDEF);
        assertEquals(UnDefType.UNDEF, item.getState());

        item.setState(UnDefType.NULL);
        assertEquals(UnDefType.NULL, item.getState());
    }

}
