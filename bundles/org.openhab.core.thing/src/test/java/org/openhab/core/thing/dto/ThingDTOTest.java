/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.thing.dto;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.openhab.core.config.core.ConfigUtil;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.builder.BridgeBuilder;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.internal.BridgeImpl;
import org.openhab.core.thing.internal.ThingImpl;

/**
 * This is the test class for {@link ThingDTO}.
 *
 * @author Christoph Weitkamp - Initial contribution
 * @author Andrew Fiddian-Green - Added semanticEquipmentTag
 */
@NonNullByDefault
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock(value = "org.openhab.core.config.core.ConfigUtil", mode = ResourceAccessMode.READ_WRITE)
public class ThingDTOTest {

    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID("binding-id", "thing-type-id");
    private static final ThingUID THING_UID = new ThingUID(THING_TYPE_UID, "thing-id");
    private final Map<String, String> properties = Map.of("key1", "value1");

    private static class ConfigUtilAccessor extends ConfigUtil {
        static void setEnv(Map<String, String> values) {
            setEnvProvider(values::get);
        }

        static void resetEnv() {
            setEnvProvider(System::getenv);
        }
    }

    @BeforeEach
    public void setUp() {
        ConfigUtilAccessor.setEnv(Map.of("HOSTNAME", "openhab-host"));
    }

    @AfterEach
    public void tearDown() {
        ConfigUtilAccessor.resetEnv();
    }

    @Test
    public void testThingDTOMappingIsBidirectional() {
        Thing subject = ThingBuilder.create(THING_TYPE_UID, THING_UID).withLabel("Test")
                .withBridge(new ThingUID(new ThingTypeUID("binding-id", "bridge-type-id"), "bridge-id"))
                .withChannels(
                        ChannelBuilder.create(new ChannelUID(THING_UID, "channel1"), CoreItemFactory.STRING).build(),
                        ChannelBuilder.create(new ChannelUID(THING_UID, "channel2"), CoreItemFactory.STRING).build())
                .withConfiguration(new Configuration(Map.of("param1", "value1"))).withProperties(properties)
                .withLocation("Somewhere over the rainbow").withSemanticEquipmentTag("MotionDetector").build();
        Thing result = ThingDTOMapper.map(ThingDTOMapper.map(subject), false);
        assertThat(result, is(instanceOf(ThingImpl.class)));
        assertThat(result.getThingTypeUID(), is(THING_TYPE_UID));
        assertThat(result.getUID(), is(THING_UID));
        assertThat(result.getLabel(), is(subject.getLabel()));
        assertThat(result.getBridgeUID(), is(subject.getBridgeUID()));
        assertThatChannelsArePresent(result.getChannels(), subject.getChannels());
        assertThat(result.getConfiguration(), is(subject.getConfiguration()));
        assertThat(result.getProperties().values(), hasSize(1));
        assertThat(result.getProperties(), is(subject.getProperties()));
        assertThat(result.getLocation(), is(subject.getLocation()));
        assertThat(result.getSemanticEquipmentTag(), is(subject.getSemanticEquipmentTag()));
    }

    @Test
    public void testBridgeDTOMappingIsBidirectional() {
        Bridge subject = BridgeBuilder.create(THING_TYPE_UID, THING_UID).build();
        Thing result = ThingDTOMapper.map(ThingDTOMapper.map(subject), true);
        assertThat(result, is(instanceOf(BridgeImpl.class)));
    }

    @Test
    public void testThingDTOMappingUsesRawConfigurationValues() {
        Thing subject = ThingBuilder.create(THING_TYPE_UID, THING_UID)
                .withConfiguration(new Configuration(Map.of("param1", "${ENV:HOSTNAME}")))
                .withChannels(ChannelBuilder.create(new ChannelUID(THING_UID, "channel1"), CoreItemFactory.STRING)
                        .withConfiguration(new Configuration(Map.of("param2", "${ENV:HOSTNAME}"))).build())
                .build();

        ThingDTO dto = ThingDTOMapper.map(subject);

        assertThat(dto.configuration.get("param1"), is("${ENV:HOSTNAME}"));
        assertThat(dto.channels.getFirst().configuration.get("param2"), is("${ENV:HOSTNAME}"));
    }

    @Test
    public void testThingDTOMappingFromDtoPreservesRawConfigurationValues() {
        ThingDTO dto = new ThingDTO();
        dto.thingTypeUID = THING_TYPE_UID.getAsString();
        dto.UID = THING_UID.getAsString();
        dto.configuration = Map.of("param1", "${ENV:HOSTNAME}");

        ChannelDTO channelDTO = new ChannelDTO();
        channelDTO.uid = new ChannelUID(THING_UID, "channel1").getAsString();
        channelDTO.itemType = CoreItemFactory.STRING;
        channelDTO.kind = "STATE";
        channelDTO.configuration = Map.of("param2", "${ENV:HOSTNAME}");
        channelDTO.properties = Map.of();
        channelDTO.defaultTags = Set.of();

        dto.channels = List.of(channelDTO);

        Thing thing = ThingDTOMapper.map(dto, false);

        assertThat(thing.getConfiguration().get("param1"), is("openhab-host"));
        assertThat(thing.getConfiguration().getRawProperties().get("param1"), is("${ENV:HOSTNAME}"));
        assertThat(thing.getChannels().getFirst().getConfiguration().get("param2"), is("openhab-host"));
        assertThat(thing.getChannels().getFirst().getConfiguration().getRawProperties().get("param2"),
                is("${ENV:HOSTNAME}"));
    }

    private void assertThatChannelsArePresent(List<Channel> actual, List<Channel> expected) {
        assertThat(actual, hasSize(expected.size()));
        actual.stream().map(channel -> channel.getUID()).forEach(uid -> {
            assertThat(expected.stream().filter(channel -> uid.equals(channel.getUID())).findFirst().orElse(null),
                    is(notNullValue()));
        });
    }
}
