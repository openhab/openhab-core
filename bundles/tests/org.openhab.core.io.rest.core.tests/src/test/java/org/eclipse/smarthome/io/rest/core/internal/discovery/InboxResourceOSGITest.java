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
package org.eclipse.smarthome.io.rest.core.internal.discovery;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.smarthome.config.core.ConfigDescriptionRegistry;
import org.eclipse.smarthome.config.discovery.inbox.Inbox;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.Before;
import org.junit.Test;

public class InboxResourceOSGITest extends JavaOSGiTest {

    private InboxResource resource;

    private final ThingTypeUID testTypeUID = new ThingTypeUID("binding", "type");
    private final ThingUID testUID = new ThingUID(testTypeUID, "id");
    private final Thing testThing = ThingBuilder.create(testTypeUID, testUID).build();
    private final String testThingLabel = "dummy_thing";

    @Before
    public void setup() throws Exception {
        ConfigDescriptionRegistry configDescRegistry = getService(ConfigDescriptionRegistry.class);
        assertFalse(configDescRegistry == null);

        registerService(new InboxResource(), InboxResource.class.getName());
        resource = getService(InboxResource.class);
        assertFalse(resource == null);
    }

    @Test
    public void assertThatApproveApprovesThingsWhichAreInTheInbox() {
        Inbox inbox = mock(Inbox.class);
        when(inbox.approve(any(), any())).thenReturn(testThing);

        resource.setInbox(inbox);
        Response reponse = resource.approve(null, testThing.getUID().toString(), testThingLabel);
        assertTrue(reponse.getStatusInfo() == Status.OK);
    }

    @Test
    public void assertThatApproveDoesntApproveThingsWhichAreNotInTheInbox() {
        Inbox inbox = mock(Inbox.class);
        when(inbox.approve(any(), any())).thenThrow(new IllegalArgumentException());

        resource.setInbox(inbox);
        Response reponse = resource.approve(null, testThing.getUID().toString(), testThingLabel);
        assertTrue(reponse.getStatusInfo() == Status.NOT_FOUND);
    }
}
