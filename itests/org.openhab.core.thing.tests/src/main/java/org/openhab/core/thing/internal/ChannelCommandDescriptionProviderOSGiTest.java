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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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
import org.openhab.core.library.items.NumberItem;
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
import org.openhab.core.thing.binding.BaseDynamicCommandDescriptionProvider;
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
import org.openhab.core.thing.type.ChannelTypeBuilder;
import org.openhab.core.thing.type.ChannelTypeProvider;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.type.DynamicCommandDescriptionProvider;
import org.openhab.core.thing.type.ThingTypeBuilder;
import org.openhab.core.types.Command;
import org.openhab.core.types.CommandDescription;
import org.openhab.core.types.CommandDescriptionBuilder;
import org.openhab.core.types.CommandOption;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.StateOption;
import org.openhab.core.util.BundleResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.ComponentContext;

/**
 * Tests for {@link ChannelCommandDescriptionProvider}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
public class ChannelCommandDescriptionProviderOSGiTest extends JavaOSGiTest {

    private static final String TEST_BUNDLE_NAME = "thingStatusInfoI18nTest.bundle";
    private static final ChannelTypeUID CHANNEL_TYPE_UID = new ChannelTypeUID("hue:dynamic");

    private ThingStatusInfoI18nLocalizationService thingStatusInfoI18nLocalizationService;
    private ItemRegistry itemRegistry;
    private ItemChannelLinkRegistry linkRegistry;

    @Mock
    private ComponentContext componentContext;

    private Bundle testBundle;

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

        final StateDescription state = StateDescriptionFragmentBuilder.create().withMinimum(BigDecimal.ZERO)
                .withMaximum(BigDecimal.valueOf(100)).withStep(BigDecimal.TEN).withPattern("%d Peek").withReadOnly(true)
                .withOption(new StateOption("SOUND", "My great sound.")).build().toStateDescription();
        final CommandDescription command = CommandDescriptionBuilder.create()
                .withCommandOption(new CommandOption("COMMAND", "My command.")).build();

        final ChannelType channelType1 = ChannelTypeBuilder
                .state(new ChannelTypeUID("hue:state-as-command"), " ", CoreItemFactory.NUMBER)
                .withStateDescription(state).build();
        final ChannelType channelType2 = ChannelTypeBuilder
                .state(new ChannelTypeUID("hue:static"), " ", CoreItemFactory.STRING).withTag("Light")
                .withCommandDescription(command).build();
        final ChannelType channelType3 = ChannelTypeBuilder.state(CHANNEL_TYPE_UID, " ", CoreItemFactory.STRING)
                .withTag("Light").build();

        List<ChannelType> channelTypes = new ArrayList<>();
        channelTypes.add(channelType1);
        channelTypes.add(channelType2);
        channelTypes.add(channelType3);

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

        testBundle = SyntheticBundleInstaller.install(bundleContext, TEST_BUNDLE_NAME);
        assertThat(testBundle, is(notNullValue()));

        thingStatusInfoI18nLocalizationService = getService(ThingStatusInfoI18nLocalizationService.class);
        assertThat(thingStatusInfoI18nLocalizationService, is(notNullValue()));

        thingStatusInfoI18nLocalizationService.setBundleResolver(new BundleResolverImpl());

        List<ChannelDefinition> channelDefinitions = new ArrayList<>();
        channelDefinitions.add(new ChannelDefinitionBuilder("1", channelType1.getUID()).build());
        channelDefinitions.add(new ChannelDefinitionBuilder("7_1", channelType2.getUID()).build());
        channelDefinitions.add(new ChannelDefinitionBuilder("7_2", channelType3.getUID()).build());

        registerService(new SimpleThingTypeProvider(Collections.singleton(ThingTypeBuilder
                .instance(new ThingTypeUID("hue:lamp"), "label").withChannelDefinitions(channelDefinitions).build())));

        List<Item> items = new ArrayList<>();
        items.add(new NumberItem("TestItem1"));
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
     * Assert that item's command description is present.
     */
    @Test
    public void presentItemCommandDescription() throws ItemNotFoundException {
        ThingRegistry thingRegistry = getService(ThingRegistry.class);
        assertNotNull(thingRegistry);
        ManagedThingProvider managedThingProvider = getService(ManagedThingProvider.class);
        assertNotNull(managedThingProvider);

        registerService(new TestDynamicCommandDescriptionProvider(), DynamicCommandDescriptionProvider.class.getName());

        Thing thing = thingRegistry.createThingOfType(new ThingTypeUID("hue:lamp"), new ThingUID("hue:lamp:lamp1"),
                null, "test thing", new Configuration());
        assertNotNull(thing);
        managedThingProvider.add(thing);
        ItemChannelLink link = new ItemChannelLink("TestItem1", getChannel(thing, "1").getUID());
        linkRegistry.add(link);
        link = new ItemChannelLink("TestItem7_1", getChannel(thing, "7_1").getUID());
        linkRegistry.add(link);
        link = new ItemChannelLink("TestItem7_2", getChannel(thing, "7_2").getUID());
        linkRegistry.add(link);
        //
        final Collection<Item> items = itemRegistry.getItems();
        assertEquals(false, items.isEmpty());

        Item item = itemRegistry.getItem("TestItem1");
        assertEquals(CoreItemFactory.NUMBER, item.getType());

        CommandDescription command = item.getCommandDescription();
        assertNotNull(command);

        List<CommandOption> opts = command.getCommandOptions();
        assertNotNull(opts);
        assertEquals(1, opts.size());
        final CommandOption opt0 = opts.get(0);
        assertNotNull(opt0);
        assertEquals("SOUND", opt0.getCommand());
        assertEquals("My great sound.", opt0.getLabel());

        item = itemRegistry.getItem("TestItem7_1");
        assertEquals(CoreItemFactory.NUMBER, item.getType());

        command = item.getCommandDescription();
        assertNotNull(command);

        opts = command.getCommandOptions();
        assertNotNull(opts);
        assertEquals(1, opts.size());
        final CommandOption opt1 = opts.get(0);
        assertNotNull(opt1);
        assertEquals("COMMAND", opt1.getCommand());
        assertEquals("My command.", opt1.getLabel());

        item = itemRegistry.getItem("TestItem7_2");
        assertEquals(CoreItemFactory.NUMBER, item.getType());

        command = item.getCommandDescription();
        assertNotNull(command);

        opts = command.getCommandOptions();
        assertNotNull(opts);
        assertEquals(1, opts.size());
        final CommandOption opt2 = opts.get(0);
        assertEquals("NEW COMMAND", opt2.getCommand());
        assertEquals("My new command.", opt2.getLabel());
    }

    @Test
    public void wrongItemCommandDescription() throws ItemNotFoundException {
        ThingRegistry thingRegistry = getService(ThingRegistry.class);
        assertNotNull(thingRegistry);
        ManagedThingProvider managedThingProvider = getService(ManagedThingProvider.class);
        assertNotNull(managedThingProvider);

        registerService(new MalfunctioningDynamicCommandDescriptionProvider(),
                DynamicCommandDescriptionProvider.class.getName());
        registerService(new TestDynamicCommandDescriptionProvider(), DynamicCommandDescriptionProvider.class.getName());

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

        CommandDescription command = item.getCommandDescription();
        assertNotNull(command);

        List<CommandOption> opts = command.getCommandOptions();
        assertNotNull(opts);
        assertEquals(1, opts.size());
        final CommandOption opt2 = opts.get(0);
        assertEquals("NEW COMMAND", opt2.getCommand());
        assertEquals("My new command.", opt2.getLabel());
    }

    /*
     * Helper
     */
    class TestDynamicCommandDescriptionProvider extends BaseDynamicCommandDescriptionProvider {
        final CommandDescription newCommand = CommandDescriptionBuilder.create()
                .withCommandOption(new CommandOption("NEW COMMAND", "My new command.")).build();

        @Override
        public @Nullable CommandDescription getCommandDescription(Channel channel,
                @Nullable CommandDescription originalCommandDescription, @Nullable Locale locale) {
            String id = channel.getUID().getIdWithoutGroup();
            if ("7_2".equals(id)) {
                assertEquals(channel.getChannelTypeUID(), CHANNEL_TYPE_UID);
                return newCommand;
            }
            return null;
        }
    }

    class MalfunctioningDynamicCommandDescriptionProvider extends BaseDynamicCommandDescriptionProvider {
        @Override
        public @Nullable CommandDescription getCommandDescription(Channel channel,
                @Nullable CommandDescription originalCommandDescription, @Nullable Locale locale) {
            return originalCommandDescription;
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
        protected ThingHandler createHandler(Thing thing) {
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
        public Bundle resolveBundle(Class<?> clazz) {
            if (clazz != null && clazz.equals(AbstractThingHandler.class)) {
                return testBundle;
            } else {
                return FrameworkUtil.getBundle(clazz);
            }
        }
    }
}
