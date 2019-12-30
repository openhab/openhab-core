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
package org.openhab.core.model.thing.test.hue;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameterBuilder;
import org.openhab.core.config.core.ConfigDescriptionProvider;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.thing.testsupport.hue.DumbThingHandlerFactory;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeBuilder;
import org.openhab.core.thing.type.ChannelTypeProvider;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.osgi.service.component.ComponentContext;

/**
 * Test asynchronous loading behavior of DSL based thing descriptions.
 *
 * @author Simon Kaufmann - Initial contribution and API
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
public class GenericThingProviderTest3 extends JavaOSGiTest {
    private DumbThingHandlerFactory dumbThingHandlerFactory;

    private static final String TESTMODEL_NAME = "testModel3.things";

    private ModelRepository modelRepository;
    private ThingRegistry thingRegistry;

    @Before
    public void setUp() throws Exception {
        thingRegistry = getService(ThingRegistry.class);
        assertThat(thingRegistry, is(notNullValue()));

        modelRepository = getService(ModelRepository.class);
        assertThat(modelRepository, is(notNullValue()));

        modelRepository.removeModel(TESTMODEL_NAME);

        ComponentContext componentContextMock = mock(ComponentContext.class);
        when(componentContextMock.getBundleContext()).thenReturn(bundleContext);

        // create a "dumb" thing handler that acts as if the XML config was not yet loaded
        dumbThingHandlerFactory = new DumbThingHandlerFactory(componentContextMock, true);

        Collection<Thing> things = thingRegistry.getAll();
        assertThat(things.size(), is(0));

        String model = "dumb:DUMB:boo \"Test Label\" @ \"Test Location\" [" + //
                "    testConf=\"foo\"" + //
                "]" + //
                "{" + //
                "    Switch : manual [ duration = \"5\" ]" + //
                "}";

        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));
        registerService(dumbThingHandlerFactory, ThingHandlerFactory.class.getName());

        ConfigDescription configDescription = new ConfigDescription(new URI("test:test"),
                Stream.of(
                        ConfigDescriptionParameterBuilder.create("testAdditional", ConfigDescriptionParameter.Type.TEXT)
                                .withRequired(false).withDefault("hello world").build(),
                        ConfigDescriptionParameterBuilder.create("testConf", ConfigDescriptionParameter.Type.TEXT)
                                .withRequired(false).withDefault("bar").build())
                        .collect(toList()));

        ConfigDescriptionProvider configDescriptionProvider = mock(ConfigDescriptionProvider.class);
        when(configDescriptionProvider.getConfigDescription(any(), nullable(Locale.class)))
                .thenReturn(configDescription);

        registerService(configDescriptionProvider);
    }

    @After
    public void tearDown() {
        unregisterService(dumbThingHandlerFactory);
        modelRepository.removeModel(TESTMODEL_NAME);
    }

    @Test
    public void assertThatThingsAreUpdatedOnceTheXMLfilesHaveBeenProcessed() {
        assertThat(thingRegistry.getAll().size(), is(1));

        Thing thing1 = thingRegistry.getAll().iterator().next();
        assertThat(thing1.getUID().toString(), is("dumb:DUMB:boo"));
        assertThat(thing1.getChannels().size(), is(1));
        assertThat(thing1.getChannel("manual"), is(notNullValue()));
        assertThat(thing1.getLabel(), is("Test Label"));
        assertThat(thing1.getLocation(), is("Test Location"));
        assertThat(thing1.getConfiguration().getProperties().get("testConf"), is("foo"));

        // now become smart again...
        dumbThingHandlerFactory.setDumb(false);
        ChannelType channelType1 = ChannelTypeBuilder
                .state(new ChannelTypeUID(DumbThingHandlerFactory.BINDING_ID, "channel1"), "Channel 1", "String")
                .build();

        ChannelTypeProvider channelTypeProvider = mock(ChannelTypeProvider.class);
        when(channelTypeProvider.getChannelType(any(), nullable(Locale.class))).thenAnswer(invocation -> {
            if (channelType1.getUID().getId().equals("channel1")) {
                return channelType1;
            }
            return null;
        });

        registerService(channelTypeProvider);

        // ensure thing type was considered and manual and predefined values are there.
        waitForAssert(() -> {
            Thing thing2 = thingRegistry.getAll().iterator().next();
            assertThat(thing2.getLabel(), is("Test Label"));
            assertThat(thing2.getLocation(), is("Test Location"));
            assertThat(thing2.getChannels().size(), is(2));
            assertThat(thing2.getChannel("manual"), is(notNullValue()));
            assertThat(thing2.getChannel("channel1"), is(notNullValue()));

            // there is a default, so make sure the manually configured one (from the DSL) wins
            assertThat(thing2.getConfiguration().getProperties().get("testConf"), is("foo"));

            // it's not manually configured, but the thing type defines a default, so ensure it's in
            assertThat(thing2.getConfiguration().getProperties().get("testAdditional"), is("hello world"));
        });
    }
}
