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
package org.openhab.core.io.rest.core.internal.discovery;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Before;
import org.junit.Test;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.discovery.inbox.Inbox;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.builder.ThingBuilder;

/**
 * @author Christoph Knauf - Initial contribution
 */
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
        assertTrue(reponse.getStatusInfo().getStatusCode() == Status.OK.getStatusCode());
    }

    @Test
    public void assertThatApproveDoesntApproveThingsWhichAreNotInTheInbox() {
        Inbox inbox = mock(Inbox.class);
        when(inbox.approve(any(), any())).thenThrow(new IllegalArgumentException());

        resource.setInbox(inbox);
        Response reponse = resource.approve(null, testThing.getUID().toString(), testThingLabel);
        assertTrue(reponse.getStatusInfo().getStatusCode() == Status.NOT_FOUND.getStatusCode());
    }
}
