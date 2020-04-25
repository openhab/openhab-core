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
package org.openhab.core.thing.binding;

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.Nullable;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameterBuilder;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.type.BridgeType;
import org.openhab.core.thing.type.ChannelDefinition;
import org.openhab.core.thing.type.ChannelDefinitionBuilder;
import org.openhab.core.thing.type.ChannelGroupDefinition;
import org.openhab.core.thing.type.ChannelGroupType;
import org.openhab.core.thing.type.ChannelGroupTypeBuilder;
import org.openhab.core.thing.type.ChannelGroupTypeProvider;
import org.openhab.core.thing.type.ChannelGroupTypeUID;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeBuilder;
import org.openhab.core.thing.type.ChannelTypeProvider;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeBuilder;

/**
 * ThingFactoryTest is a test for the ThingFactory class.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Dennis Nobel - Added test for different default types
 * @author Alex Tugarev - Adapted for constructor modification of ConfigDescriptionParameter
 * @author Thomas Höfer - Thing type constructor modified because of thing properties introduction
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
public class ThingFactoryTest extends JavaOSGiTest {

    @Test
    public void createSimpleThing() {
        ThingType thingType = ThingTypeBuilder.instance("bindingId", "thingTypeId", "label").build();
        Configuration configuration = new Configuration();

        Thing thing = ThingFactory.createThing(thingType, new ThingUID(thingType.getUID(), "thingId"), configuration);

        assertThat(thing.getUID().toString(), is(equalTo("bindingId:thingTypeId:thingId")));
        assertThat(thing.getThingTypeUID().toString(), is(equalTo("bindingId:thingTypeId")));
        assertThat(thing.getConfiguration(), is(not(nullValue())));
        assertThat(thing.getProperties(), is(not(nullValue())));
    }

    @Test
    public void createSimpleBridge() {
        BridgeType thingType = ThingTypeBuilder.instance("bindingId", "thingTypeId", "label").buildBridge();
        Configuration configuration = new Configuration();

        Thing thing = ThingFactory.createThing(thingType, new ThingUID(thingType.getUID(), "thingId"), configuration);

        assertThat(thing, is(instanceOf(Bridge.class)));
        assertThat(thing.getProperties(), is(not(nullValue())));
    }

    @Test
    public void createThingWithBridge() {
        ThingUID bridgeUID = new ThingUID("binding:bridge:1");

        ThingType thingType = ThingTypeBuilder.instance("bindingId", "thingTypeId", "label").build();
        Configuration configuration = new Configuration();

        Thing thing = ThingFactory.createThing(thingType, new ThingUID(thingType.getUID(), "thingId"), configuration,
                bridgeUID);

        assertThat(thing.getBridgeUID(), is(equalTo(bridgeUID)));
    }

    private List<ChannelDefinition> getChannelDefinitions() throws Exception {
        ChannelType channelType1 = ChannelTypeBuilder
                .state(new ChannelTypeUID("bindingId:cd1"), "channelLabel", "itemType")
                .withConfigDescriptionURI(new URI("scheme", "channelType:cd1", null)).build();

        ChannelType channelType2 = ChannelTypeBuilder
                .state(new ChannelTypeUID("bindingId:cd2"), "channelLabel2", "itemType2")
                .withConfigDescriptionURI(new URI("scheme", "channelType:cd2", null)).build();

        registerChannelTypes(Stream.of(channelType1, channelType2).collect(toSet()), emptyList());

        ChannelDefinition cd1 = new ChannelDefinitionBuilder("channel1", channelType1.getUID()).build();
        ChannelDefinition cd2 = new ChannelDefinitionBuilder("channel2", channelType2.getUID()).build();

        return Stream.of(cd1, cd2).collect(toList());
    }

    @Test
    public void createThingWithDefaultValues() throws Exception {
        ThingType thingType = ThingTypeBuilder.instance(new ThingTypeUID("myThingType", "myThing"), "label")
                .withDescription("description").withChannelDefinitions(getChannelDefinitions())
                .withConfigDescriptionURI(new URI("scheme", "thingType", null)).build();
        Configuration configuration = new Configuration();

        ConfigDescriptionRegistry configDescriptionRegistry = mock(ConfigDescriptionRegistry.class);
        when(configDescriptionRegistry.getConfigDescription(any(URI.class)))
                .thenAnswer(new Answer<ConfigDescription>() {
                    @Override
                    public ConfigDescription answer(InvocationOnMock invocation) throws Throwable {
                        URI uri = (URI) invocation.getArgument(0);
                        List<ConfigDescriptionParameter> parameters = singletonList(ConfigDescriptionParameterBuilder
                                .create("testProperty", ConfigDescriptionParameter.Type.TEXT).withContext("context")
                                .withDefault("default").withDescription("description").withLimitToOptions(true)
                                .build());
                        return new ConfigDescription(uri, parameters);
                    }
                });

        Thing thing = ThingFactory.createThing(thingType, new ThingUID(thingType.getUID(), "thingId"), configuration,
                null, configDescriptionRegistry);
        assertThat(thing.getConfiguration(), is(not(nullValue())));
        assertThat(thing.getConfiguration().get("testProperty"), is(not(nullValue())));
        assertThat(thing.getConfiguration().get("testProperty"), is(equalTo("default")));
        assertThat(thing.getChannels().size(), is(equalTo(2)));
        assertThat(thing.getChannels().get(0).getConfiguration().get("testProperty"), is(equalTo("default")));
        assertThat(thing.getChannels().get(1).getConfiguration().get("testProperty"), is(equalTo("default")));
        assertThat(thing.getProperties().size(), is(0));
    }

    @Test
    public void createThingWithDifferentDefaultValueTypes() throws Exception {
        ThingType thingType = ThingTypeBuilder.instance(new ThingTypeUID("myThingType", "myThing"), "label")
                .withDescription("description").withConfigDescriptionURI(new URI("scheme", "thingType", null)).build();
        Configuration configuration = new Configuration();

        ConfigDescriptionRegistry configDescriptionRegistry = mock(ConfigDescriptionRegistry.class);
        when(configDescriptionRegistry.getConfigDescription(any(URI.class)))
                .thenAnswer(new Answer<ConfigDescription>() {
                    @Override
                    public ConfigDescription answer(InvocationOnMock invocation) throws Throwable {
                        URI uri = (URI) invocation.getArgument(0);

                        ConfigDescriptionParameter p1 = ConfigDescriptionParameterBuilder
                                .create("p1", ConfigDescriptionParameter.Type.BOOLEAN).withContext("context")
                                .withDefault("true").withLabel("label").withDescription("description")
                                .withLimitToOptions(true).build();

                        ConfigDescriptionParameter p2 = ConfigDescriptionParameterBuilder
                                .create("p2", ConfigDescriptionParameter.Type.INTEGER).withContext("context")
                                .withDefault("5").withLabel("label").withDescription("description")
                                .withLimitToOptions(true).build();

                        ConfigDescriptionParameter p3 = ConfigDescriptionParameterBuilder
                                .create("p3", ConfigDescriptionParameter.Type.DECIMAL).withContext("context")
                                .withDefault("2.3").withLabel("label").withDescription("description")
                                .withLimitToOptions(true).build();

                        ConfigDescriptionParameter p4 = ConfigDescriptionParameterBuilder
                                .create("p4", ConfigDescriptionParameter.Type.DECIMAL).withContext("context")
                                .withDefault("invalid").withLabel("label").withDescription("description")
                                .withLimitToOptions(true).build();

                        ConfigDescriptionParameter p5 = ConfigDescriptionParameterBuilder
                                .create("p5", ConfigDescriptionParameter.Type.DECIMAL).withContext("context")
                                .withDefault("2.3").withLabel("label").withDescription("description").withMultiple(true)
                                .withLimitToOptions(true).build();

                        ConfigDescriptionParameter p6 = ConfigDescriptionParameterBuilder
                                .create("p6", ConfigDescriptionParameter.Type.DECIMAL).withContext("context")
                                .withDefault("2.3,2.4,2.5").withLabel("label").withDescription("description")
                                .withMultiple(true).withLimitToOptions(true).build();

                        List<ConfigDescriptionParameter> parameters = Stream.of(p1, p2, p3, p4, p5, p6)
                                .collect(toList());

                        return new ConfigDescription(uri, parameters);
                    }
                });

        Thing thing = ThingFactory.createThing(thingType, new ThingUID(thingType.getUID(), "thingId"), configuration,
                null, configDescriptionRegistry);
        assertThat(thing.getConfiguration(), is(not(nullValue())));
        assertThat(thing.getConfiguration().get("p1"), is(equalTo(true)));
        assertThat(((BigDecimal) thing.getConfiguration().get("p2")).compareTo(new BigDecimal("5")), is(0));
        assertThat(((BigDecimal) thing.getConfiguration().get("p3")).compareTo(new BigDecimal("2.3")), is(0));
        assertThat(thing.getConfiguration().get("p4"), is(nullValue()));
        assertThat(thing.getConfiguration().get("p5"), is(instanceOf(List.class)));
        assertThat(((List<?>) thing.getConfiguration().get("p5")).size(), is(1));
        assertThat(((List<?>) thing.getConfiguration().get("p5")).get(0), is(instanceOf(BigDecimal.class)));
        assertThat(
                ((BigDecimal) ((List<?>) thing.getConfiguration().get("p5")).get(0)).compareTo(new BigDecimal("2.3")),
                is(0));
        assertThat(thing.getConfiguration().get("p6"), is(instanceOf(List.class)));
        assertThat(((List<?>) thing.getConfiguration().get("p6")).size(), is(3));
        assertThat(thing.getProperties().size(), is(0));
    }

    @Test
    public void createThingWithChannels() {
        ChannelType channelType1 = ChannelTypeBuilder
                .state(new ChannelTypeUID("bindingId:channelTypeId1"), "channelLabel", "Color")
                .withTags(Stream.of("tag1", "tag2").collect(toSet())).build();

        ChannelType channelType2 = ChannelTypeBuilder
                .state(new ChannelTypeUID("bindingId:channelTypeId2"), "channelLabel2", "Dimmer").withTag("tag3")
                .build();

        registerChannelTypes(Stream.of(channelType1, channelType2).collect(toSet()), emptyList());

        ChannelDefinition channelDef1 = new ChannelDefinitionBuilder("ch1", channelType1.getUID()).build();
        ChannelDefinition channelDef2 = new ChannelDefinitionBuilder("ch2", channelType2.getUID()).build();

        ThingType thingType = ThingTypeBuilder.instance(new ThingTypeUID("bindingId:thingType"), "label")
                .withSupportedBridgeTypeUIDs(emptyList())
                .withChannelDefinitions(Stream.of(channelDef1, channelDef2).collect(toList())).build();
        Configuration configuration = new Configuration();

        Thing thing = ThingFactory.createThing(thingType, new ThingUID(thingType.getUID(), "thingId"), configuration);

        assertThat(thing.getChannels().size(), is(2));
        assertThat(thing.getChannels().get(0).getUID().toString(), is(equalTo("bindingId:thingType:thingId:ch1")));
        assertThat(thing.getChannels().get(0).getAcceptedItemType(), is(equalTo("Color")));
        assertThat(thing.getChannels().get(0).getDefaultTags().contains("tag1"), is(true));
        assertThat(thing.getChannels().get(0).getDefaultTags().contains("tag2"), is(true));
        assertThat(thing.getChannels().get(0).getDefaultTags().contains("tag3"), is(false));
        assertThat(thing.getChannels().get(1).getDefaultTags().contains("tag1"), is(false));
        assertThat(thing.getChannels().get(1).getDefaultTags().contains("tag2"), is(false));
        assertThat(thing.getChannels().get(1).getDefaultTags().contains("tag3"), is(true));
    }

    @Test
    public void createThingWithChannelsGroups() {
        ChannelType channelType1 = ChannelTypeBuilder
                .state(new ChannelTypeUID("bindingId:channelTypeId1"), "channelLabel", "Color")
                .withTags(Stream.of("tag1", "tag2").collect(toSet())).build();

        ChannelType channelType2 = ChannelTypeBuilder
                .state(new ChannelTypeUID("bindingId:channelTypeId2"), "channelLabel2", "Dimmer").withTag("tag3")
                .build();

        ChannelDefinition channelDef1 = new ChannelDefinitionBuilder("ch1", channelType1.getUID()).build();
        ChannelDefinition channelDef2 = new ChannelDefinitionBuilder("ch2", channelType2.getUID()).build();

        ChannelGroupType channelGroupType1 = ChannelGroupTypeBuilder
                .instance(new ChannelGroupTypeUID("bindingid:groupTypeId1"), "label").withDescription("description")
                .withCategory("myCategory1")
                .withChannelDefinitions(Stream.of(channelDef1, channelDef2).collect(toList())).build();
        ChannelGroupType channelGroupType2 = ChannelGroupTypeBuilder
                .instance(new ChannelGroupTypeUID("bindingid:groupTypeId2"), "label").withDescription("description")
                .withCategory("myCategory2").withChannelDefinitions(singletonList(channelDef1)).build();

        ChannelGroupDefinition channelGroupDef1 = new ChannelGroupDefinition("group1", channelGroupType1.getUID());
        ChannelGroupDefinition channelGroupDef2 = new ChannelGroupDefinition("group2", channelGroupType2.getUID());

        registerChannelTypes(Stream.of(channelType1, channelType2).collect(toSet()),
                Stream.of(channelGroupType1, channelGroupType2).collect(toSet()));

        ThingType thingType = ThingTypeBuilder.instance(new ThingTypeUID("bindingId:thingType"), "label")
                .withSupportedBridgeTypeUIDs(emptyList())
                .withChannelGroupDefinitions(Stream.of(channelGroupDef1, channelGroupDef2).collect(toList())).build();
        Configuration configuration = new Configuration();

        Thing thing = ThingFactory.createThing(thingType, new ThingUID(thingType.getUID(), "thingId"), configuration);

        List<String> channelUIDs = Arrays.asList("bindingId:thingType:thingId:group1#ch1",
                "bindingId:thingType:thingId:group1#ch2", "bindingId:thingType:thingId:group2#ch1");
        assertThat(thing.getChannels().size(), is(3));
        for (Channel channel : thing.getChannels()) {
            assertThat(channelUIDs.contains(channel.getUID().toString()), is(true));
        }
    }

    @Test
    public void createThingWithProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put("key1", "value1");
        properties.put("key2", "value2");

        ThingType thingType = ThingTypeBuilder.instance(new ThingTypeUID("bindingId:thingType"), "label")
                .withProperties(properties).build();
        Thing thing = ThingFactory.createThing(thingType, new ThingUID(thingType.getUID(), "thingId"),
                new Configuration());

        assertThat(thing.getProperties().size(), is(2));
        assertThat(thing.getProperties().get("key1"), is("value1"));
        assertThat(thing.getProperties().get("key2"), is("value2"));
    }

    @Test
    public void createBridgeWithProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put("key1", "value1");
        properties.put("key2", "value2");

        ThingType thingType = ThingTypeBuilder.instance(new ThingTypeUID("bindingId", "thingTypeId"), "label")
                .withProperties(properties).buildBridge();
        Thing thing = ThingFactory.createThing(thingType, new ThingUID(thingType.getUID(), "thingId"),
                new Configuration());

        assertThat(thing.getProperties().size(), is(2));
        assertThat(thing.getProperties().get("key1"), is("value1"));
        assertThat(thing.getProperties().get("key2"), is("value2"));
    }

    private void registerChannelTypes(Collection<ChannelType> channelTypes,
            Collection<ChannelGroupType> channelGroupTypes) {
        ChannelTypeProvider channelTypeProvider = mock(ChannelTypeProvider.class);
        when(channelTypeProvider.getChannelTypes(nullable(Locale.class))).thenReturn(channelTypes);
        when(channelTypeProvider.getChannelType(any(ChannelTypeUID.class), nullable(Locale.class)))
                .thenAnswer(new Answer<@Nullable ChannelType>() {
                    @Override
                    public @Nullable ChannelType answer(InvocationOnMock invocation) throws Throwable {
                        ChannelTypeUID uid = (ChannelTypeUID) invocation.getArgument(0);
                        return channelTypes.stream().filter(t -> t.getUID().equals(uid)).findFirst().get();
                    }
                });
        registerService(channelTypeProvider);

        ChannelGroupTypeProvider channelGroupTypeProvider = mock(ChannelGroupTypeProvider.class);
        when(channelGroupTypeProvider.getChannelGroupTypes(nullable(Locale.class))).thenReturn(channelGroupTypes);
        when(channelGroupTypeProvider.getChannelGroupType(any(ChannelGroupTypeUID.class), nullable(Locale.class)))
                .thenAnswer(new Answer<@Nullable ChannelGroupType>() {
                    @Override
                    public @Nullable ChannelGroupType answer(InvocationOnMock invocation) throws Throwable {
                        ChannelGroupTypeUID uid = (ChannelGroupTypeUID) invocation.getArgument(0);
                        return channelGroupTypes.stream().filter(t -> t.getUID().equals(uid)).findFirst().get();
                    }
                });
        registerService(channelGroupTypeProvider);
    }
}
