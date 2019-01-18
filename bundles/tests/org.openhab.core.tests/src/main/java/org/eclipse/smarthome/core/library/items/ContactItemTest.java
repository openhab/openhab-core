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

import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.junit.Test;

/**
 *
 * @author Stefan Triller - Initial version
 *
 */
public class ContactItemTest {

    @Test
    public void testOpenCloseType() {
        ContactItem item = new ContactItem("test");
        item.setState(OpenClosedType.OPEN);
        assertEquals(OpenClosedType.OPEN, item.getState());

        item.setState(OpenClosedType.CLOSED);
        assertEquals(OpenClosedType.CLOSED, item.getState());
    }

    @Test
    public void testUndefType() {
        ContactItem item = new ContactItem("test");
        StateUtil.testUndefStates(item);
    }

    @Test
    public void testAcceptedStates() {
        ContactItem item = new ContactItem("test");
        StateUtil.testAcceptedStates(item);
    }
}
