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
package org.openhab.core.thing.binding;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.storage.StorageService;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.type.BridgeType;
import org.openhab.core.thing.type.ChannelGroupType;
import org.openhab.core.thing.type.ChannelGroupTypeBuilder;
import org.openhab.core.thing.type.ChannelGroupTypeUID;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeBuilder;

/**
 * The {@link AbstractStorageBasedTypeProviderOSGiTest} contains tests for storing and providing {@link ChannelType}s
 * and {@link ThingType}s
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class AbstractStorageBasedTypeProviderOSGiTest extends JavaOSGiTest {

    private static final String BINDING_ID = "testBinding";
    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID(BINDING_ID, "testThingType");
    private static final ChannelTypeUID CHANNEL_TYPE_UID = new ChannelTypeUID(BINDING_ID, "testChannelType");
    private static final ChannelGroupTypeUID CHANNEL_GROUP_TYPE_UID = new ChannelGroupTypeUID(BINDING_ID,
            "testChannelGroupType");

    private @NonNullByDefault({}) AbstractStorageBasedTypeProvider typeProvider;

    @BeforeEach
    public void setup() {
        registerVolatileStorageService();

        StorageService storageService = getService(StorageService.class);
        assertThat(storageService, is(notNullValue()));

        typeProvider = new AbstractStorageBasedTypeProvider(storageService) {
        };

        assertThat(typeProvider.getThingTypes(null), hasSize(0));
        assertThat(typeProvider.getChannelTypes(null), hasSize(0));
    }

    @Test
    public void testSingleThingTypeIsRestoredAsThingType() {
        ThingType thingType = ThingTypeBuilder.instance(THING_TYPE_UID, "label").build();

        typeProvider.putThingType(thingType);
        assertThat(typeProvider.getThingTypes(null), hasSize(1));

        ThingType registryThingType = typeProvider.getThingType(THING_TYPE_UID, null);
        assertThat(registryThingType, is(notNullValue()));

        assertThat(registryThingType, not(instanceOf(BridgeType.class)));
    }

    @Test
    public void testSingleBridgeTypeIsRestoredAsBridgeType() {
        BridgeType bridgeType = ThingTypeBuilder.instance(THING_TYPE_UID, "label").buildBridge();

        typeProvider.putThingType(bridgeType);
        assertThat(typeProvider.getThingTypes(null), hasSize(1));

        ThingType registryThingType = typeProvider.getThingType(THING_TYPE_UID, null);
        assertThat(registryThingType, is(notNullValue()));

        assertThat(registryThingType, is(instanceOf(BridgeType.class)));
    }

    @Test
    public void testTwoThingTypesCanBeRemovedIndependently() {
        ThingType thingType1 = ThingTypeBuilder.instance(BINDING_ID, "type1", "label1").build();
        ThingType thingType2 = ThingTypeBuilder.instance(BINDING_ID, "type2", "label2").build();

        typeProvider.putThingType(thingType1);
        typeProvider.putThingType(thingType2);
        assertThat(typeProvider.getThingType(thingType1.getUID(), null), is(notNullValue()));
        assertThat(typeProvider.getThingType(thingType2.getUID(), null), is(notNullValue()));

        typeProvider.removeThingType(thingType1.getUID());
        assertThat(typeProvider.getThingType(thingType1.getUID(), null), is(nullValue()));
        assertThat(typeProvider.getThingType(thingType2.getUID(), null), is(notNullValue()));
    }

    @Test
    public void testThingTypeCanBeUpdated() {
        String originalLabel = "original Label";
        String updatedLabel = "updated Label";
        ThingType thingTypeOriginal = ThingTypeBuilder.instance(THING_TYPE_UID, originalLabel).build();
        ThingType thingTypeUpdated = ThingTypeBuilder.instance(THING_TYPE_UID, updatedLabel).build();

        typeProvider.putThingType(thingTypeOriginal);
        ThingType registryThingType = typeProvider.getThingType(THING_TYPE_UID, null);
        assertThat(registryThingType, is(notNullValue()));
        assertThat(registryThingType.getLabel(), is(originalLabel));

        typeProvider.putThingType(thingTypeUpdated);
        registryThingType = typeProvider.getThingType(THING_TYPE_UID, null);
        assertThat(registryThingType, is(notNullValue()));
        assertThat(registryThingType.getLabel(), is(updatedLabel));
    }

    @Test
    public void testSingleChannelTypeIsAddedAndRestored() {
        ChannelType channelType = ChannelTypeBuilder.state(CHANNEL_TYPE_UID, "label", "Switch").build();

        typeProvider.putChannelType(channelType);
        assertThat(typeProvider.getChannelTypes(null), hasSize(1));

        ChannelType registryChannelType = typeProvider.getChannelType(CHANNEL_TYPE_UID, null);
        assertThat(registryChannelType, is(notNullValue()));
        assertThat(registryChannelType.getKind(), is(ChannelKind.STATE));
    }

    @Test
    public void testTwoChannelTypesAreAddedAndCanBeRemovedIndependently() {
        ChannelType channelType1 = ChannelTypeBuilder.trigger(new ChannelTypeUID(BINDING_ID, "type1"), "label1")
                .build();
        ChannelType channelType2 = ChannelTypeBuilder.trigger(new ChannelTypeUID(BINDING_ID, "type2"), "label2")
                .build();

        typeProvider.putChannelType(channelType1);
        typeProvider.putChannelType(channelType2);

        assertThat(typeProvider.getChannelType(channelType1.getUID(), null), is(notNullValue()));
        assertThat(typeProvider.getChannelType(channelType2.getUID(), null), is(notNullValue()));

        typeProvider.removeChannelType(channelType1.getUID());

        assertThat(typeProvider.getChannelType(channelType1.getUID(), null), is(nullValue()));
        assertThat(typeProvider.getChannelType(channelType2.getUID(), null), is(notNullValue()));
    }

    @Test
    public void testChannelTypeCanBeUpdated() {
        String originalLabel = "original Label";
        String updatedLabel = "updated Label";
        ChannelType channelTypeOriginal = ChannelTypeBuilder.trigger(CHANNEL_TYPE_UID, originalLabel).build();
        ChannelType channelTypeUpdated = ChannelTypeBuilder.trigger(CHANNEL_TYPE_UID, updatedLabel).build();

        typeProvider.putChannelType(channelTypeOriginal);
        ChannelType registryChannelType = typeProvider.getChannelType(CHANNEL_TYPE_UID, null);
        assertThat(registryChannelType, is(notNullValue()));
        assertThat(registryChannelType.getLabel(), is(originalLabel));

        typeProvider.putChannelType(channelTypeUpdated);
        registryChannelType = typeProvider.getChannelType(CHANNEL_TYPE_UID, null);
        assertThat(registryChannelType, is(notNullValue()));
        assertThat(registryChannelType.getLabel(), is(updatedLabel));
    }

    @Test
    public void testSingleChannelGroupTypeIsAddedAndRestored() {
        ChannelGroupType channelGroupType = ChannelGroupTypeBuilder.instance(CHANNEL_GROUP_TYPE_UID, "label").build();

        typeProvider.putChannelGroupType(channelGroupType);
        assertThat(typeProvider.getChannelGroupTypes(null), hasSize(1));

        ChannelGroupType registryChannelGroupType = typeProvider.getChannelGroupType(CHANNEL_GROUP_TYPE_UID, null);
        assertThat(registryChannelGroupType, is(notNullValue()));
    }

    @Test
    public void testTwoChannelGroupTypesAreAddedAndCanBeRemovedIndependently() {
        ChannelGroupType channelGroupType1 = ChannelGroupTypeBuilder
                .instance(new ChannelGroupTypeUID(BINDING_ID, "type1"), "label1").build();
        ChannelGroupType channelGroupType2 = ChannelGroupTypeBuilder
                .instance(new ChannelGroupTypeUID(BINDING_ID, "type2"), "label2").build();

        typeProvider.putChannelGroupType(channelGroupType1);
        typeProvider.putChannelGroupType(channelGroupType2);

        assertThat(typeProvider.getChannelGroupType(channelGroupType1.getUID(), null), is(notNullValue()));
        assertThat(typeProvider.getChannelGroupType(channelGroupType2.getUID(), null), is(notNullValue()));

        typeProvider.removeChannelGroupType(channelGroupType1.getUID());

        assertThat(typeProvider.getChannelGroupType(channelGroupType1.getUID(), null), is(nullValue()));
        assertThat(typeProvider.getChannelGroupType(channelGroupType2.getUID(), null), is(notNullValue()));
    }

    @Test
    public void testChannelGroupTypeCanBeUpdated() {
        String originalLabel = "original Label";
        String updatedLabel = "updated Label";
        ChannelGroupType channelGroupTypeOriginal = ChannelGroupTypeBuilder
                .instance(CHANNEL_GROUP_TYPE_UID, originalLabel).build();
        ChannelGroupType channelGroupTypeUpdated = ChannelGroupTypeBuilder
                .instance(CHANNEL_GROUP_TYPE_UID, updatedLabel).build();

        typeProvider.putChannelGroupType(channelGroupTypeOriginal);
        ChannelGroupType registryChannelGroupType = typeProvider.getChannelGroupType(CHANNEL_GROUP_TYPE_UID, null);
        assertThat(registryChannelGroupType, is(notNullValue()));
        assertThat(registryChannelGroupType.getLabel(), is(originalLabel));

        typeProvider.putChannelGroupType(channelGroupTypeUpdated);
        registryChannelGroupType = typeProvider.getChannelGroupType(CHANNEL_GROUP_TYPE_UID, null);
        assertThat(registryChannelGroupType, is(notNullValue()));
        assertThat(registryChannelGroupType.getLabel(), is(updatedLabel));
    }
}
