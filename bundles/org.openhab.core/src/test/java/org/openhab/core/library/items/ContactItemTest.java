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
import org.openhab.core.library.types.OpenClosedType;

/**
 *
 * @author Stefan Triller - Initial contribution
 */
@NonNullByDefault
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
