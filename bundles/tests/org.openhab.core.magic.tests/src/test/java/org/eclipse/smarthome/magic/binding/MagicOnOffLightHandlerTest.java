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
package org.eclipse.smarthome.magic.binding;

import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerCallback;
import org.eclipse.smarthome.magic.binding.handler.MagicOnOffLightHandler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

/**
 * Tests cases for {@link MagicONOffLightHandler}. The tests provide mocks for supporting entities using Mockito.
 *
 * @author Henning Treu - Initial contribution
 */
public class MagicOnOffLightHandlerTest {

    private ThingHandler handler;

    @Mock
    private ThingHandlerCallback callback;

    @Mock
    private Thing thing;

    @Before
    public void setUp() {
        initMocks(this);
        handler = new MagicOnOffLightHandler(thing);
        handler.setCallback(callback);
    }

    @Test
    public void initializeShouldCallTheCallback() {
        handler.initialize();

        ArgumentCaptor<ThingStatusInfo> statusInfoCaptor = ArgumentCaptor.forClass(ThingStatusInfo.class);

        verify(callback).statusUpdated(eq(thing), statusInfoCaptor.capture());

        ThingStatusInfo thingStatusInfo = statusInfoCaptor.getValue();
        Assert.assertThat(thingStatusInfo.getStatus(), is(equalTo(ThingStatus.ONLINE)));
    }

}
