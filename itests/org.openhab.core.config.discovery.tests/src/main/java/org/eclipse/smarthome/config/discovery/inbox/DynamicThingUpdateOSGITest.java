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
package org.eclipse.smarthome.config.discovery.inbox;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.config.discovery.internal.DiscoveryResultImpl;
import org.eclipse.smarthome.core.thing.ManagedThingProvider;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerCallback;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingTypeProvider;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingStatusInfoBuilder;
import org.eclipse.smarthome.core.thing.type.ThingType;
import org.eclipse.smarthome.core.thing.type.ThingTypeBuilder;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * The {@link DynamicThingUpdateTest} checks if a {@link Thing} configuration is updated
 * correctly when it was found via a {@link DiscoveryService}.
 * <p>
 * This implementation creates a {@link Thing} and adds it to the {@link ManagedThingProvider}.
 * A {@link DiscoveryResult} object is created and added to the {@link Inbox}. The {@link Inbox}
 * has to figure out if the configuration must be updated or not and triggers a further process
 * chain to update the {@link Thing} at the according {@link ThingHandler} if needed.
 * A dummy {@link ThingHandler} and {@link ThingHandlerFactory} is used to detect an updated event.
 *
 * @author Michael Grammling - Initial Contribution
 * @author Thomas Höfer - Added representation
 * @author Andre Fuechsel - Added tests for device id
 * @author Simon Kaufmann - ported to Java
 */
public class DynamicThingUpdateOSGITest extends JavaOSGiTest {

    private static final int DEFAULT_TTL = 60;

    private static final String BINDING_ID = "dynamicUpdateBindingId";
    private static final String THING_TYPE_ID = "dynamicUpdateThingType";
    private static final String THING_ID = "dynamicUpdateThingId";
    private static final String THING_ID2 = "dynamicUpdateThingId2";
    private static final String DEVICE_ID_KEY = "deviceIdKey";

    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID(BINDING_ID, THING_TYPE_ID);
    private static final ThingUID THING_UID = new ThingUID(THING_TYPE_UID, THING_ID);
    private static final ThingUID THING_UID2 = new ThingUID(THING_TYPE_UID, THING_ID2);
    private static final ThingType THING_TYPE = ThingTypeBuilder.instance(THING_TYPE_UID, "label")
            .withRepresentationProperty(DEVICE_ID_KEY).isListed(true).build();

    private Inbox inbox;
    private ManagedThingProvider managedThingProvider;
    private ThingHandler thingHandler;

    private volatile ThingHandlerCallback callback;

    @Before
    public void setUp() {
        registerVolatileStorageService();

        ThingTypeProvider thingTypeProvider = mock(ThingTypeProvider.class);
        when(thingTypeProvider.getThingType(eq(THING_TYPE_UID), any())).thenReturn(THING_TYPE);
        registerService(thingTypeProvider);

        inbox = getService(Inbox.class);
        managedThingProvider = getService(ManagedThingProvider.class);

        assertEquals(0, inbox.getAll().size());

        ThingHandlerFactory thingHandlerFactory = createThingHandlerFactory();
        registerService(thingHandlerFactory, ThingHandlerFactory.class.getName());
    }

    private ThingHandler createThingHandler(Thing thing) {
        ThingHandler thingHandler = mock(ThingHandler.class);
        when(thingHandler.getThing()).thenReturn(thing);
        doAnswer(answer -> {
            callback = answer.getArgument(0);
            return null;
        }).when(thingHandler).setCallback(isA(ThingHandlerCallback.class));
        return thingHandler;
    }

    private ThingHandlerFactory createThingHandlerFactory() {
        ThingHandlerFactory thingHandlerFactory = new BaseThingHandlerFactory() {

            @Override
            public boolean supportsThingType(@NonNull ThingTypeUID thingTypeUID) {
                return THING_TYPE_UID.equals(thingTypeUID);
            }

            @Override
            protected @Nullable ThingHandler createHandler(@NonNull Thing thing) {
                thingHandler = createThingHandler(thing);
                return thingHandler;
            }
        };
        return thingHandlerFactory;
    }

    @Test
    public void assertUpdateWithDifferentConfig() {
        final String CFG_IP_ADDRESS_KEY = "ipAddress";
        final String CFG_IP_ADDRESS_VALUE = "127.0.0.1";

        Thing thing = ThingBuilder.create(THING_TYPE_UID, THING_ID).build();
        thing.getConfiguration().put(CFG_IP_ADDRESS_KEY, null);
        managedThingProvider.add(thing);
        waitForAssert(() -> {
            assertNotNull(callback);
        });
        callback.statusUpdated(thing, ThingStatusInfoBuilder.create(ThingStatus.ONLINE).build());

        DiscoveryResult discoveryResult = new DiscoveryResultImpl(THING_TYPE_UID, THING_UID, null,
                Collections.singletonMap(CFG_IP_ADDRESS_KEY, CFG_IP_ADDRESS_VALUE), "DummyRepr", "DummyLabel1",
                DEFAULT_TTL);

        inbox.add(discoveryResult);

        ArgumentCaptor<Thing> captor = ArgumentCaptor.forClass(Thing.class);
        verify(thingHandler).thingUpdated(captor.capture());

        Thing updatedThing = captor.getValue();
        assertNotNull(updatedThing);
        assertEquals(CFG_IP_ADDRESS_VALUE, updatedThing.getConfiguration().get(CFG_IP_ADDRESS_KEY));

        assertEquals(0, inbox.getAll().size());
    }

    @Test
    public void assertNotNUpdatedWithSameConfig() {
        managedThingProvider.add(ThingBuilder.create(THING_TYPE_UID, THING_ID).build());

        DiscoveryResult discoveryResult = new DiscoveryResultImpl(THING_TYPE_UID, THING_UID, null,
                Collections.emptyMap(), null, "DummyLabel", DEFAULT_TTL);

        inbox.add(discoveryResult);

        assertEquals(0, inbox.getAll().size());
        verify(thingHandler, never()).thingUpdated(any());
    }

    @Test
    public void assertAdded() {
        managedThingProvider.add(ThingBuilder.create(THING_TYPE_UID, THING_ID).build());

        DiscoveryResult discoveryResult = new DiscoveryResultImpl(THING_TYPE_UID, THING_UID2, null,
                Collections.emptyMap(), null, "DummyLabel", DEFAULT_TTL);

        inbox.add(discoveryResult);

        assertEquals(1, inbox.getAll().size());
    }
}
