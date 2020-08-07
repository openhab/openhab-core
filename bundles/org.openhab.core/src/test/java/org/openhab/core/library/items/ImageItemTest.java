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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.openhab.core.library.types.RawType;
import org.openhab.core.types.State;

/**
 *
 * @author Stefan Triller - Initial contribution
 */
public class ImageItemTest {

    @Test
    public void testRawType() {
        ImageItem item = new ImageItem("test");
        State state = new RawType(new byte[0], "application/octet-stream");
        item.setState(state);
        assertEquals(state, item.getState());
    }

    @Test
    public void testUndefType() {
        ImageItem item = new ImageItem("test");
        StateUtil.testUndefStates(item);
    }

    @Test
    public void testAcceptedStates() {
        DateTimeItem item = new DateTimeItem("test");
        StateUtil.testAcceptedStates(item);
    }
}
