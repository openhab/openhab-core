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
package org.openhab.core.library.items;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.State;

/**
 *
 * @author Stefan Triller - Initial contribution
 */
@NonNullByDefault
public class StringItemTest {

    @Test
    public void setStringType() {
        StringItem item = new StringItem("test");
        State state = new StringType("foobar");
        item.setState(state);
        assertEquals(state, item.getState());
    }

    @Test
    public void setDateTimeTypeType() {
        StringItem item = new StringItem("test");
        State state = new DateTimeType();
        item.setState(state);
        assertEquals(state, item.getState());
    }

    @Test
    public void testUndefType() {
        StringItem item = new StringItem("test");
        StateUtil.testUndefStates(item);
    }

    @Test
    public void testAcceptedStates() {
        StringItem item = new StringItem("test");
        StateUtil.testAcceptedStates(item);
    }
}
