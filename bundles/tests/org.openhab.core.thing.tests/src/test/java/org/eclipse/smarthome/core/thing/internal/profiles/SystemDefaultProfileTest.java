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
package org.eclipse.smarthome.core.thing.internal.profiles;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.profiles.ProfileCallback;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 *
 * @author Simon Kaufmann - initial contribution and API.
 *
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
