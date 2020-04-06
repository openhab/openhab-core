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
package org.openhab.core.thing.internal;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemProvider;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.items.ColorItem;
import org.openhab.core.library.items.DimmerItem;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.test.SyntheticBundleInstaller;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ManagedThingProvider;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseDynamicStateDescriptionProvider;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.core.thing.i18n.ThingStatusInfoI18nLocalizationService;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.type.ChannelDefinition;
import org.openhab.core.thing.type.ChannelDefinitionBuilder;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeProvider;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.type.DynamicStateDescriptionProvider;
import org.openhab.core.thing.type.ThingTypeBuilder;
import org.openhab.core.types.Command;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.StateOption;
import org.openhab.core.util.BundleResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.ComponentContext;

/**
 * Tests for {@link ChannelStateDescriptionProvider}.
 *
 * @author Alex Tugarev - Initial contribution
 * @author Thomas Höfer - Thing type constructor modified because of thing properties introduction
 * @author Markus Rathgeb - Migrated from Groovy to plain Java
 */
@NonNullByDefault 
public class ChannelStateDescriptionProviderOSGiTest extends JavaOSGiTest {

    private static final String TEST_BUNDLE_NAME = "thingStatusInfoI18nTest.bundle";
    private static final ChannelTypeUID CHANNEL_TYPE_7_UID = new ChannelTypeUID("hue:num-dynamic");

    private @NonNullByDefault({}) ThingStatusInfoI18nLocalizationService thingStatusInfoI18nLocalizationService;
    private @NonNullByDefault({}) ItemRegistry itemRegistry;
    private @NonNullByDefault({}) ItemChannelLinkRegistry linkRegistry;

    @Mock
    private @NonNullByDefault({}) ComponentContext componentContext;

    private @NonNullByDefault({}) Bundle testBundle;

    @Before
    public void setup() throws Exception {
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

        final ChannelType channelType = new ChannelType(new ChannelTypeUID("hue:alarm"), false, CoreItemFactory.NUMBER,
                " ", "", null, null, state, null);
        final ChannelType channelType2 = new ChannelType(new ChannelTypeUID("hue:num"), false, CoreItemFactory.NUMBER,
                " ", "", null, null, state2, null);
        final ChannelType channelType3 = new ChannelType(new ChannelTypeUID("hue:info"), true, CoreItemFactory.STRING,
                " ", "", null, null, (StateDescription) null, null);
        final ChannelType channelType4 = new ChannelType(new ChannelTypeUID("hue:color"), false, CoreItemFactory.COLOR,
                "Color", "", "ColorLight", null, (StateDescription) null, null);
        final ChannelType channelType5 = new ChannelType(new ChannelTypeUID("hue:brightness"), false,
                CoreItemFactory.DIMMER, "Brightness", "", "DimmableLight", null, (StateDescription) null, null);
        final ChannelType channelType6 = new ChannelType(new ChannelTypeUID("hue:switch"), false,
                CoreItemFactory.SWITCH, "Switch", "", "Light", null, (StateDescription) null, null);
        final ChannelType channelType7 = new ChannelType(CHANNEL_TYPE_7_UID, false, CoreItemFactory.NUMBER, " ", "",
                "Light", null, state, null);

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
            public Collection<ChannelType> getChannelTypes(@Nullable Locale locale) {
                return channelTypes;
            }

            @Override
            public @Nullable ChannelType getChannelType(ChannelTypeUID channelTypeUID, @Nullable Locale locale) {
                for (final ChannelType channelType : channelTypes) {
                    if (channelType.getUID().equals(channelTypeUID)) {
                        return channelType;
                    }
                }
                return null;
            }
        });

        testBundle = SyntheticBundleInstaller.install(bundleContext, TEST_BUNDLE_NAME);
        assertThat(testBundle, is(notNullValue()));

        thingStatusInfoI18nLocalizationService = getService(ThingStatusInfoI18nLocalizationService.class);
        assertThat(thingStatusInfoI18nLocalizationService, is(notNullValue()));

        thingStatusInfoI18nLocalizationService.setBundleResolver(new BundleResolverImpl());

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
    public void teardown() throws BundleException {
        testBundle.uninstall();
        ManagedThingProvider managedThingProvider = getService(ManagedThingProvider.class);
        assertNotNull(managedThingProvider);
        managedThingProvider.getAll().forEach(thing -> {
            managedThingProvider.remove(thing.getUID());
        });
        linkRegistry.getAll().forEach(link -> {
            linkRegistry.remove(link.getUID());
        });
    }

    private static Channel getChannel(final Thing thing, final String channelId) {
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
        assertNotNull(thingRegistry);
        ManagedThingProvider managedThingProvider = getService(ManagedThingProvider.class);
        assertNotNull(managedThingProvider);

        registerService(new TestDynamicStateDescriptionProvider(), DynamicStateDescriptionProvider.class.getName());

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
        assertEquals(CoreItemFactory.NUMBER, item.getType());

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
        assertEquals(CoreItemFactory.NUMBER, item.getType());

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
        assertEquals(CoreItemFactory.STRING, item.getType());

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
        assertEquals(CoreItemFactory.COLOR, item.getType());

        state = item.getStateDescription();
        assertNull(state);

        item = itemRegistry.getItem("TestItem5");
        assertEquals(CoreItemFactory.DIMMER, item.getType());

        state = item.getStateDescription();
        assertNull(state);

        item = itemRegistry.getItem("TestItem6");
        assertEquals(CoreItemFactory.SWITCH, item.getType());

        state = item.getStateDescription();
        assertNull(state);

        item = itemRegistry.getItem("TestItem7_1");
        assertEquals(CoreItemFactory.NUMBER, item.getType());

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
        assertEquals(CoreItemFactory.NUMBER, item.getType());

        state = item.getStateDescription();
        assertNotNull(state);

        assertEquals(BigDecimal.valueOf(1), state.getMinimum());
        assertEquals(BigDecimal.valueOf(101), state.getMaximum());
        assertEquals(BigDecimal.valueOf(20), state.getStep());
        assertEquals("NEW %d Peek", state.getPattern());
        assertEquals(false, state.isReadOnly());

        opts = state.getOptions();
        assertNotNull(opts);
        assertEquals(1, opts.size());
        final StateOption opt2 = opts.get(0);
        assertEquals("NEW SOUND", opt2.getValue());
        assertEquals("My great new sound.", opt2.getLabel());
    }

    @Test
    public void wrongItemStateDescription() throws ItemNotFoundException {
        ThingRegistry thingRegistry = getService(ThingRegistry.class);
        assertNotNull(thingRegistry);
        ManagedThingProvider managedThingProvider = getService(ManagedThingProvider.class);
        assertNotNull(managedThingProvider);

        registerService(new TestMalfunctioningDynamicStateDescriptionProvider(),
                DynamicStateDescriptionProvider.class.getName());
        registerService(new TestDynamicStateDescriptionProvider(), DynamicStateDescriptionProvider.class.getName());

        Thing thing = thingRegistry.createThingOfType(new ThingTypeUID("hue:lamp"), new ThingUID("hue:lamp:lamp1"),
                null, "test thing", new Configuration());
        assertNotNull(thing);
        managedThingProvider.add(thing);
        ItemChannelLink link = new ItemChannelLink("TestItem7_2", getChannel(thing, "7_2").getUID());
        linkRegistry.add(link);
        //
        final Collection<Item> items = itemRegistry.getItems();
        assertEquals(false, items.isEmpty());

        Item item = itemRegistry.getItem("TestItem7_2");

        StateDescription state = item.getStateDescription();
        assertNotNull(state);

        assertEquals(BigDecimal.valueOf(1), state.getMinimum());
        assertEquals(BigDecimal.valueOf(101), state.getMaximum());
        assertEquals(BigDecimal.valueOf(20), state.getStep());
        assertEquals("NEW %d Peek", state.getPattern());
        assertEquals(false, state.isReadOnly());

        List<StateOption> opts = state.getOptions();
        assertNotNull(opts);
        assertEquals(1, opts.size());
        final StateOption opt2 = opts.get(0);
        assertEquals("NEW SOUND", opt2.getValue());
        assertEquals("My great new sound.", opt2.getLabel());
    }

    /*
     * Helper
     */
    class TestDynamicStateDescriptionProvider extends BaseDynamicStateDescriptionProvider {
        final @Nullable StateDescription newState = StateDescriptionFragmentBuilder.create()
                .withMinimum(BigDecimal.valueOf(10)).withMaximum(BigDecimal.valueOf(100))
                .withStep(BigDecimal.valueOf(5)).withPattern("VALUE %d").withReadOnly(false)
                .withOptions(Arrays.asList(new StateOption("value0", "label0"), new StateOption("value1", "label1")))
                .build().toStateDescription();

        @Override
        public @Nullable StateDescription getStateDescription(Channel channel, @Nullable StateDescription original,
                @Nullable Locale locale) {
            String id = channel.getUID().getIdWithoutGroup();
            if ("7_1".equals(id)) {
                assertEquals(channel.getChannelTypeUID(), CHANNEL_TYPE_7_UID);
                return newState;
            } else if ("7_2".equals(id)) {
                assertEquals(channel.getChannelTypeUID(), CHANNEL_TYPE_7_UID);
                StateDescriptionFragmentBuilder builder = (original == null) ? StateDescriptionFragmentBuilder.create()
                        : StateDescriptionFragmentBuilder.create(original);
                return builder.withMinimum(original.getMinimum().add(BigDecimal.ONE))
                        .withMaximum(original.getMaximum().add(BigDecimal.ONE))
                        .withStep(original.getStep().add(BigDecimal.TEN)).withPattern("NEW " + original.getPattern())
                        .withReadOnly(!original.isReadOnly())
                        .withOptions(Collections.singletonList(new StateOption("NEW SOUND", "My great new sound.")))
                        .build().toStateDescription();
            }
            return null;
        }
    }

    class TestMalfunctioningDynamicStateDescriptionProvider extends BaseDynamicStateDescriptionProvider {
        @Override
        public @Nullable StateDescription getStateDescription(Channel channel, @Nullable StateDescription original,
                @Nullable Locale locale) {
            return original;
        }
    }

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
        protected @Nullable ThingHandler createHandler(Thing thing) {
            return new AbstractThingHandler(thing) {
                @Override
                public void handleCommand(ChannelUID channelUID, Command command) {
                    // do nothing
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

    private abstract class AbstractThingHandler extends BaseThingHandler {
        public AbstractThingHandler(Thing thing) {
            super(thing);
        }
    }

    /**
     * Use this for simulating that the {@link AbstractThingHandler} class does come from another bundle than this test
     * bundle.
     */
    private class BundleResolverImpl implements BundleResolver {
        @Override
        public Bundle resolveBundle(@Nullable Class<?> clazz) {
            if (clazz != null && clazz.equals(AbstractThingHandler.class)) {
                return testBundle;
            } else {
                return FrameworkUtil.getBundle(clazz);
            }
        }
    }
}
