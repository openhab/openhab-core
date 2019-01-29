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
package org.eclipse.smarthome.core.thing.internal;

import static org.junit.Assert.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.common.registry.ProviderChangeListener;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemNotFoundException;
import org.eclipse.smarthome.core.items.ItemProvider;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.library.items.ColorItem;
import org.eclipse.smarthome.core.library.items.DimmerItem;
import org.eclipse.smarthome.core.library.items.NumberItem;
import org.eclipse.smarthome.core.library.items.StringItem;
import org.eclipse.smarthome.core.library.items.SwitchItem;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ManagedThingProvider;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.eclipse.smarthome.core.thing.link.ItemChannelLink;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
import org.eclipse.smarthome.core.thing.type.ChannelDefinition;
import org.eclipse.smarthome.core.thing.type.ChannelDefinitionBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.core.thing.type.DynamicStateDescriptionProvider;
import org.eclipse.smarthome.core.thing.type.ThingTypeBuilder;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.StateDescription;
import org.eclipse.smarthome.core.types.StateOption;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.osgi.service.component.ComponentContext;

/**
 * Tests for {@link ChannelStateDescriptionProvider}.
 *
 * @author Alex Tugarev - Initial contribution
 * @author Thomas Höfer - Thing type constructor modified because of thing properties introduction
 * @author Markus Rathgeb - Migrated from Groovy to plain Java
 */
public class ChannelStateDescriptionProviderOSGiTest extends JavaOSGiTest {

    private ItemRegistry itemRegistry;
    private ItemChannelLinkRegistry linkRegistry;

    @Mock
    private ComponentContext componentContext;

    @Before
    public void setup() {
        initMocks(this);

        Mockito.when(componentContext.getBundleContext()).thenReturn(bundleContext);

        registerVolatileStorageService();

        itemRegistry = getService(ItemRegistry.class);
        assertNotNull(itemRegistry);

        final TestThingHandlerFactory thingHandlerFactory = new TestThingHandlerFactory();
        thingHandlerFactory.activate(componentContext);
        registerService(thingHandlerFactory, ThingHandlerFactory.class.getName());

        final StateDescription state = new StateDescription(BigDecimal.ZERO, BigDecimal.valueOf(100), BigDecimal.TEN,
                "%d Peek", true, Collections.singletonList(new StateOption("SOUND", "My great sound.")));

        final StateDescription state2 = new StateDescription(BigDecimal.ZERO, BigDecimal.valueOf(256),
                BigDecimal.valueOf(8), null, false, null);

        final ChannelType channelType = new ChannelType(new ChannelTypeUID("hue:alarm"), false, "Number", " ", "", null,
                null, state, null);
        final ChannelType channelType2 = new ChannelType(new ChannelTypeUID("hue:num"), false, "Number", " ", "", null,
                null, state2, null);
        final ChannelType channelType3 = new ChannelType(new ChannelTypeUID("hue:info"), true, "String", " ", "", null,
                null, null, null);
        final ChannelType channelType4 = new ChannelType(new ChannelTypeUID("hue:color"), false, "Color", "Color", "",
                "ColorLight", null, null, null);
        final ChannelType channelType5 = new ChannelType(new ChannelTypeUID("hue:brightness"), false, "Dimmer",
                "Brightness", "", "DimmableLight", null, null, null);
        final ChannelType channelType6 = new ChannelType(new ChannelTypeUID("hue:switch"), false, "Switch", "Switch",
                "", "Light", null, null, null);
        final ChannelType channelType7 = new ChannelType(new ChannelTypeUID("hue:num-dynamic"), false, "Number", " ",
                "", "Light", null, state, null);

        List<ChannelType> channelTypes = new ArrayList<>();
        channelTypes.add(channelType);
        channelTypes.add(channelType2);
        channelTypes.add(channelType3);
        channelTypes.add(channelType4);
        channelTypes.add(channelType5);
        channelTypes.add(channelType6);
        channelTypes.add(channelType7);

        registerService(new ChannelTypeProvider() {
            @Override
            public Collection<ChannelType> getChannelTypes(Locale locale) {
                return channelTypes;
            }

            @Override
            public ChannelType getChannelType(ChannelTypeUID channelTypeUID, Locale locale) {
                for (final ChannelType channelType : channelTypes) {
                    if (channelType.getUID().equals(channelTypeUID)) {
                        return channelType;
                    }
                }
                return null;
            }
        });

        registerService(new DynamicStateDescriptionProvider() {
            final StateDescription newState = new StateDescription(BigDecimal.valueOf(10), BigDecimal.valueOf(100),
                    BigDecimal.valueOf(5), "VALUE %d", false,
                    Arrays.asList(new StateOption("value0", "label0"), new StateOption("value1", "label1")));

            @Override
            public @Nullable StateDescription getStateDescription(@NonNull Channel channel,
                    @Nullable StateDescription original, @Nullable Locale locale) {
                String id = channel.getUID().getIdWithoutGroup();
                if ("7_1".equals(id)) {
                    assertEquals(channel.getChannelTypeUID(), channelType7.getUID());
                    return newState;
                } else if ("7_2".equals(id)) {
                    assertEquals(channel.getChannelTypeUID(), channelType7.getUID());
                    StateDescription newState2 = new StateDescription(original.getMinimum().add(BigDecimal.ONE),
                            original.getMaximum().add(BigDecimal.ONE), original.getStep().add(BigDecimal.TEN),
                            "NEW " + original.getPattern(), true, original.getOptions());
                    return newState2;
                }
                return null;
            }
        });

        List<ChannelDefinition> channelDefinitions = new ArrayList<>();
        channelDefinitions.add(new ChannelDefinitionBuilder("1", channelType.getUID()).build());
        channelDefinitions.add(new ChannelDefinitionBuilder("2", channelType2.getUID()).build());
        channelDefinitions.add(new ChannelDefinitionBuilder("3", channelType3.getUID()).build());
        channelDefinitions.add(new ChannelDefinitionBuilder("4", channelType4.getUID()).build());
        channelDefinitions.add(new ChannelDefinitionBuilder("5", channelType5.getUID()).build());
        channelDefinitions.add(new ChannelDefinitionBuilder("6", channelType6.getUID()).build());
        channelDefinitions.add(new ChannelDefinitionBuilder("7_1", channelType7.getUID()).build());
        channelDefinitions.add(new ChannelDefinitionBuilder("7_2", channelType7.getUID()).build());

        registerService(new SimpleThingTypeProvider(Collections.singleton(ThingTypeBuilder
                .instance(new ThingTypeUID("hue:lamp"), "label").withChannelDefinitions(channelDefinitions).build())));

        List<Item> items = new ArrayList<>();
        items.add(new NumberItem("TestItem"));
        items.add(new NumberItem("TestItem2"));
        items.add(new StringItem("TestItem3"));
        items.add(new ColorItem("TestItem4"));
        items.add(new DimmerItem("TestItem5"));
        items.add(new SwitchItem("TestItem6"));
        items.add(new NumberItem("TestItem7_1"));
        items.add(new NumberItem("TestItem7_2"));
        registerService(new TestItemProvider(items));

        linkRegistry = getService(ItemChannelLinkRegistry.class);
    }

    @After
    public void teardown() {
        ManagedThingProvider managedThingProvider = getService(ManagedThingProvider.class);
        managedThingProvider.getAll().forEach(thing -> {
            managedThingProvider.remove(thing.getUID());
        });
        linkRegistry.getAll().forEach(link -> {
            linkRegistry.remove(link.getUID());
        });
    }

    private static @NonNull Channel getChannel(final @NonNull Thing thing, final @NonNull String channelId) {
        final Channel channel = thing.getChannel(channelId);
        if (channel == null) {
            throw new IllegalArgumentException(String.format("The thing '%s' does not seems to contain a channel '%s'.",
                    thing.getUID(), channelId));
        } else {
            return channel;
        }
    }

    /**
     * Assert that item's state description is present.
     */
    @Test
    public void presentItemStateDescription() throws ItemNotFoundException {
        ThingRegistry thingRegistry = getService(ThingRegistry.class);
        ManagedThingProvider managedThingProvider = getService(ManagedThingProvider.class);

        Thing thing = thingRegistry.createThingOfType(new ThingTypeUID("hue:lamp"), new ThingUID("hue:lamp:lamp1"),
                null, "test thing", new Configuration());
        assertNotNull(thing);
        managedThingProvider.add(thing);
        ItemChannelLink link = new ItemChannelLink("TestItem", getChannel(thing, "1").getUID());
        linkRegistry.add(link);
        link = new ItemChannelLink("TestItem2", getChannel(thing, "2").getUID());
        linkRegistry.add(link);
        link = new ItemChannelLink("TestItem3", getChannel(thing, "3").getUID());
        linkRegistry.add(link);
        link = new ItemChannelLink("TestItem4", getChannel(thing, "4").getUID());
        linkRegistry.add(link);
        link = new ItemChannelLink("TestItem5", getChannel(thing, "5").getUID());
        linkRegistry.add(link);
        link = new ItemChannelLink("TestItem6", getChannel(thing, "6").getUID());
        linkRegistry.add(link);
        link = new ItemChannelLink("TestItem7_1", getChannel(thing, "7_1").getUID());
        linkRegistry.add(link);
        link = new ItemChannelLink("TestItem7_2", getChannel(thing, "7_2").getUID());
        linkRegistry.add(link);
        //
        final Collection<Item> items = itemRegistry.getItems();
        assertEquals(false, items.isEmpty());

        Item item = itemRegistry.getItem("TestItem");
        assertEquals("Number", item.getType());

        StateDescription state = item.getStateDescription();
        assertNotNull(state);

        assertEquals(BigDecimal.ZERO, state.getMinimum());
        assertEquals(BigDecimal.valueOf(100), state.getMaximum());
        assertEquals(BigDecimal.TEN, state.getStep());
        assertEquals("%d Peek", state.getPattern());
        assertEquals(true, state.isReadOnly());
        List<StateOption> opts = state.getOptions();
        assertEquals(1, opts.size());
        final StateOption opt = opts.get(0);
        assertEquals("SOUND", opt.getValue());
        assertEquals("My great sound.", opt.getLabel());

        item = itemRegistry.getItem("TestItem2");
        assertEquals("Number", item.getType());

        state = item.getStateDescription();
        assertNotNull(state);

        assertEquals(BigDecimal.ZERO, state.getMinimum());
        assertEquals(BigDecimal.valueOf(256), state.getMaximum());
        assertEquals(BigDecimal.valueOf(8), state.getStep());
        assertEquals("%.0f", state.getPattern());
        assertEquals(false, state.isReadOnly());
        opts = state.getOptions();
        assertEquals(0, opts.size());

        item = itemRegistry.getItem("TestItem3");
        assertEquals("String", item.getType());

        state = item.getStateDescription();
        assertNotNull(state);

        assertNull(state.getMinimum());
        assertNull(state.getMaximum());
        assertNull(state.getStep());
        assertEquals("%s", state.getPattern());
        assertEquals(false, state.isReadOnly());
        opts = state.getOptions();
        assertEquals(0, opts.size());

        item = itemRegistry.getItem("TestItem4");
        assertEquals("Color", item.getType());

        state = item.getStateDescription();
        assertNull(state);

        item = itemRegistry.getItem("TestItem5");
        assertEquals("Dimmer", item.getType());

        state = item.getStateDescription();
        assertNull(state);

        item = itemRegistry.getItem("TestItem6");
        assertEquals("Switch", item.getType());

        state = item.getStateDescription();
        assertNull(state);

        item = itemRegistry.getItem("TestItem7_1");
        assertEquals("Number", item.getType());

        state = item.getStateDescription();
        assertNotNull(state);

        assertEquals(BigDecimal.valueOf(10), state.getMinimum());
        assertEquals(BigDecimal.valueOf(100), state.getMaximum());
        assertEquals(BigDecimal.valueOf(5), state.getStep());
        assertEquals("VALUE %d", state.getPattern());
        assertEquals(false, state.isReadOnly());

        opts = state.getOptions();
        assertNotNull(opts);
        assertEquals(2, opts.size());
        final StateOption opt0 = opts.get(0);
        assertNotNull(opt0);
        assertEquals(opt0.getValue(), "value0");
        assertEquals(opt0.getLabel(), "label0");
        final StateOption opt1 = opts.get(1);
        assertNotNull(opt1);
        assertEquals(opt1.getValue(), "value1");
        assertEquals(opt1.getLabel(), "label1");

        item = itemRegistry.getItem("TestItem7_2");
        assertEquals("Number", item.getType());

        state = item.getStateDescription();
        assertNotNull(state);

        assertEquals(BigDecimal.valueOf(1), state.getMinimum());
        assertEquals(BigDecimal.valueOf(101), state.getMaximum());
        assertEquals(BigDecimal.valueOf(20), state.getStep());
        assertEquals("NEW %d Peek", state.getPattern());
        assertEquals(true, state.isReadOnly());

        opts = state.getOptions();
        assertNotNull(opts);
        assertEquals(1, opts.size());
        final StateOption opt2 = opts.get(0);
        assertEquals("SOUND", opt2.getValue());
        assertEquals("My great sound.", opt2.getLabel());
    }

    /*
     * Helper
     */

    class TestThingHandlerFactory extends BaseThingHandlerFactory {

        @Override
        public void activate(final ComponentContext ctx) {
            super.activate(ctx);
        }

        @Override
        public boolean supportsThingType(ThingTypeUID thingTypeUID) {
            return true;
        }

        @Override
        protected ThingHandler createHandler(Thing thing) {
            return new BaseThingHandler(thing) {
                @Override
                public void handleCommand(ChannelUID channelUID, Command command) {
                }

                @Override
                public void initialize() {
                    updateStatus(ThingStatus.ONLINE);
                }
            };
        }
    }

    class TestItemProvider implements ItemProvider {
        private final Collection<Item> items;

        TestItemProvider(Collection<Item> items) {
            this.items = items;
        }

        @Override
        public void addProviderChangeListener(ProviderChangeListener<Item> listener) {
        }

        @Override
        public Collection<Item> getAll() {
            return items;
        }

        @Override
        public void removeProviderChangeListener(ProviderChangeListener<Item> listener) {
        }
    }
}
