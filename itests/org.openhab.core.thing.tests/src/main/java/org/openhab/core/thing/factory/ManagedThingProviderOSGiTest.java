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
package org.openhab.core.thing.factory;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.test.AsyncResultWrapper;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.ManagedThingProvider;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.builder.ThingBuilder;

/**
 * Tests for {@link ManagedThingProvider}.
 *
 * @author Oliver Libutzki - Initial contribution
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
public class ManagedThingProviderOSGiTest extends JavaOSGiTest {

    private static final String BINDIND_ID = "testBinding";
    private static final String THING_TYPE_ID = "testThingType";
    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID(BINDIND_ID, THING_TYPE_ID);
    private static final String THING1_ID = "testThing1";
    private static final String THING2_ID = "testThing2";

    private ManagedThingProvider managedThingProvider;
    private ProviderChangeListener<Thing> thingChangeListener;

    @Before
    public void setup() {
        registerVolatileStorageService();
        managedThingProvider = getService(ManagedThingProvider.class);
        assertThat(managedThingProvider, is(notNullValue()));
        unregisterCurrentThingsChangeListener();
    }

    @After
    public void teardown() {
        unregisterCurrentThingsChangeListener();
        managedThingProvider.getAll().forEach(t -> managedThingProvider.remove(t.getUID()));
    }

    private void registerThingsChangeListener(ProviderChangeListener<Thing> thingChangeListener) {
        unregisterCurrentThingsChangeListener();
        this.thingChangeListener = thingChangeListener;
        managedThingProvider.addProviderChangeListener(this.thingChangeListener);
    }

    private void unregisterCurrentThingsChangeListener() {
        if (this.thingChangeListener != null) {
            managedThingProvider.removeProviderChangeListener(this.thingChangeListener);
        }
    }

    @Test
    public void assertThatAddedThingIsReturnedByGetThings() {
        Thing thing1 = ThingBuilder.create(THING_TYPE_UID, THING1_ID).build();
        managedThingProvider.add(thing1);
        Collection<Thing> things = managedThingProvider.getAll();
        assertThat(things.size(), is(1));
        assertThat(things.iterator().next(), is(thing1));
        Thing thing2 = ThingBuilder.create(THING_TYPE_UID, THING2_ID).build();
        managedThingProvider.add(thing2);
        things = managedThingProvider.getAll();
        // Check for exact size and if the collection contains every element.
        // So, the order of the elements is ignored.
        assertThat(things.size(), is(2));
        assertTrue(things.contains(thing1));
        assertTrue(things.contains(thing2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void assertThatTwiceAddedThingThrowsException() {
        Thing thing1 = ThingBuilder.create(THING_TYPE_UID, THING1_ID).build();
        Thing thing2 = ThingBuilder.create(THING_TYPE_UID, THING1_ID).build();
        managedThingProvider.add(thing1);
        managedThingProvider.add(thing2);
    }

    @Test
    public void assertThatRemovedThingIsNotReturnedByGetThings() {
        Thing thing = ThingBuilder.create(THING_TYPE_UID, THING1_ID).build();
        managedThingProvider.add(thing);
        managedThingProvider.remove(thing.getUID());
        Collection<Thing> things = managedThingProvider.getAll();
        assertThat(things.size(), is(0));
    }

    @Test
    public void assertThatThingsChangeListenerIsNotifiedAboutAddedThing() {
        AsyncResultWrapper<Provider<Thing>> thingProviderWrapper = new AsyncResultWrapper<>();
        AsyncResultWrapper<Thing> thingWrapper = new AsyncResultWrapper<>();

        registerThingsChangeListener(new ProviderChangeListener<Thing>() {
            @Override
            public void added(Provider<Thing> provider, Thing thing) {
                thingProviderWrapper.set(provider);
                thingWrapper.set(thing);
            }

            @Override
            public void removed(Provider<Thing> provider, Thing thing) {
            }

            @Override
            public void updated(Provider<Thing> provider, Thing oldThing, Thing thing) {
            }
        });

        Thing thing1 = ThingBuilder.create(THING_TYPE_UID, THING1_ID).build();
        managedThingProvider.add(thing1);

        waitForAssert(() -> assertTrue(thingProviderWrapper.isSet()));
        waitForAssert(() -> assertTrue(thingWrapper.isSet()));

        assertThat(thingProviderWrapper.getWrappedObject(), is(managedThingProvider));
        assertThat(thingWrapper.getWrappedObject(), is(thing1));
    }

    @Test
    public void assertThatThingsChangeListenerIsNotifiedAboutUpdatedThing() {
        AsyncResultWrapper<Provider<Thing>> updatedThingProviderWrapper = new AsyncResultWrapper<>();
        AsyncResultWrapper<Thing> oldThingWrapper = new AsyncResultWrapper<>();
        AsyncResultWrapper<Thing> updatedThingWrapper = new AsyncResultWrapper<>();

        Thing thing1 = ThingBuilder.create(THING_TYPE_UID, THING1_ID).build();
        managedThingProvider.add(thing1);

        registerThingsChangeListener(new ProviderChangeListener<Thing>() {
            @Override
            public void added(Provider<Thing> provider, Thing thing) {
            }

            @Override
            public void removed(Provider<Thing> provider, Thing thing) {
            }

            @Override
            public void updated(Provider<Thing> provider, Thing oldThing, Thing thing) {
                updatedThingProviderWrapper.set(provider);
                oldThingWrapper.set(oldThing);
                updatedThingWrapper.set(thing);
            }
        });

        Thing thing2 = ThingBuilder.create(THING_TYPE_UID, THING1_ID).build();
        managedThingProvider.update(thing2);

        waitForAssert(() -> assertTrue(updatedThingProviderWrapper.isSet()));
        waitForAssert(() -> assertTrue(oldThingWrapper.isSet()));
        waitForAssert(() -> assertTrue(updatedThingWrapper.isSet()));

        assertThat(updatedThingProviderWrapper.getWrappedObject(), is(managedThingProvider));
        assertThat(oldThingWrapper.getWrappedObject(), is(thing1));
        assertThat(updatedThingWrapper.getWrappedObject(), is(thing2));
    }

}
