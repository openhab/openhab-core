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
import org.openhab.core.library.types.StringListType;

/**
 *
 * @author Gaël L'hopital - Initial contribution
 * @author Stefan Triller - Added state tests
 */
@NonNullByDefault
public class CallItemTest {

    @Test
    public void testSetStringListType() {
        StringListType callType1 = new StringListType("0699222222", "0179999998");
        CallItem callItem1 = new CallItem("testItem");

        callItem1.setState(callType1);
        assertEquals("testItem (Type=CallItem, State=0699222222,0179999998, Label=null, Category=null)",
                callItem1.toString());

        callType1 = new StringListType("0699222222,0179999998");
        callItem1.setState(callType1);
        assertEquals("testItem (Type=CallItem, State=0699222222,0179999998, Label=null, Category=null)",
                callItem1.toString());
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
