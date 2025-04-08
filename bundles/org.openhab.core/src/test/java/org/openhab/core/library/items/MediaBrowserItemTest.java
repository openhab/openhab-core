/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
import org.openhab.core.library.types.PlayPauseType;
import org.openhab.core.library.types.RewindFastforwardType;

/**
 *
 * @author Laurent Arnal - Initial contribution
 */
@NonNullByDefault
public class MediaBrowserItemTest {

    @Test
    public void setPlayPause() {
        MediaBrowserItem item = new MediaBrowserItem("test");
        item.setState(PlayPauseType.PLAY);
        assertEquals(PlayPauseType.PLAY, item.getState());

        item.setState(PlayPauseType.PAUSE);
        assertEquals(PlayPauseType.PAUSE, item.getState());
    }

    @Test
    public void setRewindFastforward() {
        MediaBrowserItem item = new MediaBrowserItem("test");
        item.setState(RewindFastforwardType.REWIND);
        assertEquals(RewindFastforwardType.REWIND, item.getState());

        item.setState(RewindFastforwardType.FASTFORWARD);
        assertEquals(RewindFastforwardType.FASTFORWARD, item.getState());
    }

    @Test
    public void testUndefType() {
        MediaBrowserItem item = new MediaBrowserItem("test");
        StateUtil.testUndefStates(item);
    }

    @Test
    public void testAcceptedStates() {
        MediaBrowserItem item = new MediaBrowserItem("test");
        StateUtil.testAcceptedStates(item);
    }
}
