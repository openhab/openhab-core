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
package org.openhab.core.thing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.storage.StorageService;
import org.openhab.core.thing.binding.builder.BridgeBuilder;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.internal.ThingStorageEntity;

/**
 * The {@link ManagedThingProviderTest} contains tests for the {@link ManagedThingProvider}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ManagedThingProviderTest {

    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID("test", "test");
    private static final ThingUID THING_UID = new ThingUID(THING_TYPE_UID, "test");
    private static final String FIRST_CHANNEL_ID = "firstgroup#channel1";
    private static final ChannelUID FIRST_CHANNEL_UID = new ChannelUID(THING_UID, FIRST_CHANNEL_ID);

    private @Mock @NonNullByDefault({}) StorageService storageService;

    @Test
    public void testThingImplConversion() {
        Thing thing = ThingBuilder.create(THING_TYPE_UID, THING_UID)
                .withChannel(ChannelBuilder.create(FIRST_CHANNEL_UID, CoreItemFactory.STRING).build()).build();

        ManagedThingProvider managedThingProvider = new ManagedThingProvider(storageService);
        ThingStorageEntity persistableElement = managedThingProvider.toPersistableElement(thing);

        assertThat(persistableElement.isBridge, is(false));

        Thing thing1 = managedThingProvider.toElement("", persistableElement);
        assertThat(thing1, is(thing));
    }

    @Test
    public void testBridgeImplConversion() {
        Bridge thing = BridgeBuilder.create(THING_TYPE_UID, THING_UID)
                .withChannel(ChannelBuilder.create(FIRST_CHANNEL_UID, CoreItemFactory.STRING).build()).build();

        ManagedThingProvider managedThingProvider = new ManagedThingProvider(storageService);
        ThingStorageEntity persistableElement = managedThingProvider.toPersistableElement(thing);

        assertThat(persistableElement.isBridge, is(true));

        Thing thing1 = managedThingProvider.toElement("", persistableElement);
        assertThat(thing1, is(thing));
    }
}
