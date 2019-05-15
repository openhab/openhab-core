/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.io.rest.core.internal.discovery;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import javax.ws.rs.NotFoundException;

import org.eclipse.smarthome.config.discovery.inbox.Inbox;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Christoph Knauf - Initial contribution
 */
public class InboxResourceTest {

    private InboxResource resource;

    private final ThingTypeUID testTypeUID = new ThingTypeUID("binding", "type");
    private final ThingUID testUID = new ThingUID(testTypeUID, "id");
    private final Thing testThing = ThingBuilder.create(testTypeUID, testUID).build();
    private final String testThingLabel = "dummy_thing";

    @Before
    public void setup() throws Exception {
        resource = new InboxResource();
    }

    @Test
    public void assertThatApproveApprovesThingsWhichAreInTheInbox() {
        Inbox inbox = mock(Inbox.class);
        when(inbox.approve(any(), any())).thenReturn(testThing);

        resource.inbox = (inbox);
        resource.approve(null, testThing.getUID().toString(), testThingLabel);
    }

    @Test(expected = NotFoundException.class)
    public void assertThatApproveDoesntApproveThingsWhichAreNotInTheInbox() {
        Inbox inbox = mock(Inbox.class);
        when(inbox.approve(any(), any())).thenThrow(new IllegalArgumentException());

        resource.inbox = (inbox);
        resource.approve(null, testThing.getUID().toString(), testThingLabel);
    }
}
