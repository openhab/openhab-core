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

import org.eclipse.smarthome.core.library.types.StringListType;
import org.junit.Test;

/**
 *
 * @author Gaël L'hopital - Initial version
 * @author Stefan Triller - Added state tests
 *
 */
public class CallItemTest {

    @Test
    public void testSetStringListType() {

        StringListType callType1 = new StringListType("0699222222", "0179999998");
        CallItem callItem1 = new CallItem("testItem");

        callItem1.setState(callType1);
        assertEquals(callItem1.toString(),
                "testItem (Type=CallItem, State=0699222222,0179999998, Label=null, Category=null)");

        callType1 = new StringListType("0699222222,0179999998");
        callItem1.setState(callType1);
        assertEquals(callItem1.toString(),
                "testItem (Type=CallItem, State=0699222222,0179999998, Label=null, Category=null)");
    }

    @Test
    public void testSetUndefType() {
        CallItem callItem = new CallItem("testItem");
        StateUtil.testUndefStates(callItem);
    }

    @Test
    public void testAcceptedStates() {
        CallItem item = new CallItem("testItem");
        StateUtil.testAcceptedStates(item);
    }
}
