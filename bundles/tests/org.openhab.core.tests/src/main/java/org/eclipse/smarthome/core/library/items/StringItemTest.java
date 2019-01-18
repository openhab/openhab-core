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

import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.types.State;
import org.junit.Test;

/**
 *
 * @author Stefan Triller - Initial version
 *
 */
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
