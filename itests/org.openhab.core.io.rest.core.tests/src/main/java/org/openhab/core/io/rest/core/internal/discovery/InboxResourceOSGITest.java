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
package org.openhab.core.io.rest.core.internal.discovery;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class InboxResourceOSGITest extends JavaOSGiTest {

    private final ThingTypeUID testTypeUID = new ThingTypeUID("binding", "type");
    private final ThingUID testUID = new ThingUID(testTypeUID, "id");
    private final Thing testThing = ThingBuilder.create(testTypeUID, testUID).build();
    private final String testThingLabel = "dummy_thing";

    private @NonNullByDefault({}) InboxResource resource;

    private @Mock @NonNullByDefault({}) Inbox inboxMock;

    @BeforeEach
    public void beforeEach() throws Exception {
        ConfigDescriptionRegistry configDescRegistry = getService(ConfigDescriptionRegistry.class);
        assertNotNull(configDescRegistry);

        registerService(new InboxResource(inboxMock), InboxResource.class.getName());
        resource = getService(InboxResource.class);
        assertNotNull(resource);
    }

    @Test
    public void assertThatApproveApprovesThingsWhichAreInTheInbox() {
        when(inboxMock.approve(any(), any(), any())).thenReturn(testThing);

        Response reponse = resource.approve(null, testThing.getUID().toString(), testThingLabel, null);
        assertTrue(reponse.getStatusInfo().getStatusCode() == Status.OK.getStatusCode());
    }

    @Test
    public void assertThatApproveDoesntApproveThingsWhichAreNotInTheInbox() {
        when(inboxMock.approve(any(), any(), any())).thenThrow(new IllegalArgumentException());

        Response reponse = resource.approve(null, testThing.getUID().toString(), testThingLabel, null);
        assertTrue(reponse.getStatusInfo().getStatusCode() == Status.NOT_FOUND.getStatusCode());
    }
}
