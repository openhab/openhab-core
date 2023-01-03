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
package org.openhab.core.thing.internal.profiles;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.CommonTriggerEvents;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.TriggerProfile;

/**
 * Tests for the system:trigger-event-string profile
 *
 * @author Jan N. Klug - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class TriggerEventStringProfileTest {

    private @Mock @NonNullByDefault({}) ProfileCallback callbackMock;

    @Test
    public void testEventStringItem() {
        TriggerProfile profile = new TriggerEventStringProfile((callbackMock));
        profile.onTriggerFromHandler(CommonTriggerEvents.PRESSED);
        verify(callbackMock, times(1)).sendCommand(eq(new StringType(CommonTriggerEvents.PRESSED)));
    }
}
