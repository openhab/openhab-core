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

import org.eclipse.smarthome.core.library.types.PlayPauseType;
import org.eclipse.smarthome.core.library.types.RewindFastforwardType;
import org.junit.Test;

/**
 *
 * @author Stefan Triller - Initial version
 *
 */
public class PlayerItemTest {

    @Test
    public void setPlayPause() {
        PlayerItem item = new PlayerItem("test");
        item.setState(PlayPauseType.PLAY);
        assertEquals(PlayPauseType.PLAY, item.getState());

        item.setState(PlayPauseType.PAUSE);
        assertEquals(PlayPauseType.PAUSE, item.getState());
    }

    @Test
    public void setRewindFastforward() {
        PlayerItem item = new PlayerItem("test");
        item.setState(RewindFastforwardType.REWIND);
        assertEquals(RewindFastforwardType.REWIND, item.getState());

        item.setState(RewindFastforwardType.FASTFORWARD);
        assertEquals(RewindFastforwardType.FASTFORWARD, item.getState());
    }

    @Test
    public void testUndefType() {
        PlayerItem item = new PlayerItem("test");
        StateUtil.testUndefStates(item);
    }

    @Test
    public void testAcceptedStates() {
        PlayerItem item = new PlayerItem("test");
        StateUtil.testAcceptedStates(item);
    }

}
