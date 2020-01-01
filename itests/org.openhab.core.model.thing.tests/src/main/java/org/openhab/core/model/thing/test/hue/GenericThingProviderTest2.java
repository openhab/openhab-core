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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.thing.testsupport.hue.TestHueThingHandlerFactoryX;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.ComponentContext;

/**
 * @author Henning Treu - Initial contribution
 */
public class GenericThingProviderTest2 extends JavaOSGiTest {

    private static final String TESTMODEL_NAME = "testModelX.things";

    private ModelRepository modelRepository;
    private ThingRegistry thingRegistry;

    @Before
    public void setUp() {
        thingRegistry = getService(ThingRegistry.class);
        assertThat(thingRegistry, is(notNullValue()));
        modelRepository = getService(ModelRepository.class);
        assertThat(modelRepository, is(notNullValue()));
        modelRepository.removeModel(TESTMODEL_NAME);
    }

    @After
    public void tearDown() {
        modelRepository.removeModel(TESTMODEL_NAME);
    }

    @Test
    public void assertThatThingsAreCreatedAndNotRemovedOnAddingRemovingTriggerHandlerFactories() {
        ComponentContext componentContextMock = mock(ComponentContext.class);
        when(componentContextMock.getBundleContext()).thenReturn(bundleContext);

        TestHueThingHandlerFactoryX hueThingHandlerFactory = new TestHueThingHandlerFactoryX(componentContextMock);

        Collection<Thing> things = thingRegistry.getAll();
        assertThat(things.size(), is(0));

        String model = "Bridge Xhue:Xbridge:myBridge [ XipAddress = \"1.2.3.4\", XuserName = \"123\" ] {" + //
                "    XLCT001 bulb1 [ XlightId = \"1\" ] { Switch : notification }" + //
                "    Bridge Xbridge myBridge2 [ ] {" + //
                "        XLCT001 bulb2 [ ]" + //
                "    }" + //
                "}" + //
                "Xhue:XTEST:bulb4 [ XlightId = \"5\"]{" + //
                "    Switch : notification [ duration = \"5\" ]" + //
                "}" + //
                "" + //
                "Xhue:XLCT001:bulb3 [ XlightId = \"4\" ] {" + //
                "    Switch : notification [ duration = \"5\" ]" + //
                "}";

        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));
        Collection<Thing> actualThings = thingRegistry.getAll();

        assertThat(actualThings.size(), is(0));

        registerService(hueThingHandlerFactory, ThingHandlerFactory.class.getName());

        actualThings = thingRegistry.getAll();
        assertThat(thingRegistry.getAll().size(), is(6));

        unregisterService(hueThingHandlerFactory);

        assertThat(thingRegistry.getAll().size(), is(6));

        registerService(hueThingHandlerFactory, ThingHandlerFactory.class.getName());

        assertThat(thingRegistry.getAll().size(), is(6));

        TestHueThingHandlerFactoryX thingHF2 = new TestHueThingHandlerFactoryX(componentContextMock);

        registerService(thingHF2, ThingHandlerFactory.class.getName());

        assertThat(thingRegistry.getAll().size(), is(6));
    }
}
