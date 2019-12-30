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
package org.openhab.core.thing.internal.profiles;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.profiles.ProfileCallback;

/**
 *
 * @author Simon Kaufmann - Initial contribution
 */
public class SystemDefaultProfileTest {

    @Mock
    private ProfileCallback mockCallback;

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void testOnCommand() {
        SystemDefaultProfile profile = new SystemDefaultProfile(mockCallback);

        profile.onCommandFromItem(OnOffType.ON);

        verify(mockCallback).handleCommand(eq(OnOffType.ON));
        verifyNoMoreInteractions(mockCallback);
    }

    @Test
    public void testOnUpdate() {
        SystemDefaultProfile profile = new SystemDefaultProfile(mockCallback);
        profile.onStateUpdateFromItem(OnOffType.ON);

        verify(mockCallback).handleUpdate(eq(OnOffType.ON));
        verifyNoMoreInteractions(mockCallback);
    }

    @Test
    public void testStateUpdated() {
        SystemDefaultProfile profile = new SystemDefaultProfile(mockCallback);
        profile.onStateUpdateFromHandler(OnOffType.ON);

        verify(mockCallback).sendUpdate(eq(OnOffType.ON));
        verifyNoMoreInteractions(mockCallback);
    }

    @Test
    public void testPostCommand() {
        SystemDefaultProfile profile = new SystemDefaultProfile(mockCallback);
        profile.onCommandFromHandler(OnOffType.ON);

        verify(mockCallback).sendCommand(eq(OnOffType.ON));
        verifyNoMoreInteractions(mockCallback);
    }

}
