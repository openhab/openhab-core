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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.thing.testsupport.hue.TestHueThingHandlerFactoryX;
import org.openhab.core.model.thing.testsupport.hue.TestHueThingTypeProvider;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyService;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.core.types.Command;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.ComponentContext;

/**
 * Test class for the GenericThingProvider.
 *
 * It focuses on the XML processing (i.e. ThingType loading) and verifies
 * that the loading of Things gets delayed until the XML processing is completed.
 *
 * @author Simon Kaufmann - Initial contribution and API.
 */
@RunWith(Parameterized.class)
public class GenericThingProviderTest4 extends JavaOSGiTest {
    private TestHueThingTypeProvider thingTypeProvider;
    private ReadyService readyService;
    private Bundle bundle;
    private ThingHandlerFactory hueThingHandlerFactory;
    private boolean finished;
    private int bridgeInitializeCounter;
    private int thingInitializeCounter;
    final boolean slowInit;

    private static final String TESTMODEL_NAME = "testModelX.things";

    ModelRepository modelRepository;
    ThingRegistry thingRegistry;

    @Parameters(name = "{index}: slowInit={0}")
    public static Object[] data() {
        return new Object[] { false, true };
    }

    public GenericThingProviderTest4(boolean slowInit) {
        this.slowInit = slowInit;
    }

    class TestBridgeHandler extends BaseBridgeHandler {
        public TestBridgeHandler(Bridge bridge) {
            super(bridge);
        }

        @Override
        public void handleCommand(ChannelUID channelUID, Command command) {
        }

        @Override
        public void initialize() {
            bridgeInitializeCounter++;
            if (slowInit) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new IllegalStateException("Interrupted while sleeping", e);
                }
            }
            super.initialize();
        }

        @Override
        public void dispose() {
            super.dispose();
        }
    }

    @Before
    public void setUp() {
        registerVolatileStorageService();

        readyService = getService(ReadyService.class);
        assertThat(readyService, is(notNullValue()));
        thingRegistry = getService(ThingRegistry.class);
        assertThat(thingRegistry, is(notNullValue()));
        modelRepository = getService(ModelRepository.class);
        assertThat(modelRepository, is(notNullValue()));
        modelRepository.removeModel(TESTMODEL_NAME);

        ComponentContext componentContextMock = mock(ComponentContext.class);
        when(componentContextMock.getBundleContext()).thenReturn(bundleContext);

        hueThingHandlerFactory = new TestHueThingHandlerFactoryX(componentContextMock) {
            @Override
            protected ThingHandler createHandler(final Thing thing) {
                if (thing instanceof Bridge) {
                    return new TestBridgeHandler((Bridge) thing);
                } else {
                    return new BaseThingHandler(thing) {
                        @Override
                        public void handleCommand(ChannelUID arg0, Command arg1) {
                        };

                        @Override
                        public void initialize() {
                            updateStatus(ThingStatus.ONLINE);
                        }
                    };
                }
            }
        };

        finished = false;
        bundle = FrameworkUtil.getBundle(TestHueThingHandlerFactoryX.class);

        removeReadyMarker();
    }

    private void removeReadyMarker() {
        waitForAssert(() -> {
            // wait for the XML processing to be finished, then remove the ready marker again
            ReadyMarker marker = new ReadyMarker("esh.xmlThingTypes", bundle.getSymbolicName());
            assertThat(readyService.isReady(marker), is(true));
            readyService.unmarkReady(marker);
        });
    }

    @After
    public void tearDown() {
        modelRepository.removeModel(TESTMODEL_NAME);
        if (thingTypeProvider != null) {
            unregisterService(thingTypeProvider);
        }
    }

    private void prepareThingWithShutDownBundle() {
        updateModel();
        registerThingTypeProvider();
        finishLoading();
        registerService(hueThingHandlerFactory, ThingHandlerFactory.class.getName());
        assertThatAllIsGood();
        unload();
        unregisterService(ThingHandlerFactory.class.getName());

        waitForAssert(() -> {
            assertThat(thingRegistry.getAll().size(), is(1));
            Thing myBridge = thingRegistry.getAll().stream()
                    .filter(t -> "Xhue:Xbridge:myBridge".equals(t.getUID().getAsString())).findFirst().get();
            assertThat(myBridge.getStatus(), is(equalTo(ThingStatus.UNINITIALIZED)));
            assertThat(myBridge.getHandler(), is(nullValue()));
        });
        removeReadyMarker();
    }

    private void updateModel() {
        String model = "Xhue:Xbridge:myBridge [ XipAddress = \"1.2.3.4\", XuserName = \"123\" ]";
        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));
    }

    private void registerThingTypeProvider() {
        thingTypeProvider = new TestHueThingTypeProvider();
        registerService(thingTypeProvider);
    }

    private void finishLoading() {
        finished = true;
        assertThat(bridgeInitializeCounter, is(0));
        readyService.markReady(new ReadyMarker("esh.xmlThingTypes", bundle.getSymbolicName()));
    }

    private void unload() {
        unregisterService(thingTypeProvider);
        finished = false;
    }

    @SuppressWarnings("null")
    private void assertThatAllIsGood() {
        waitForAssert(() -> {
            assertThat(thingRegistry.getAll().size(), is(1));
            Thing bridge = thingRegistry.get(new ThingUID("Xhue:Xbridge:myBridge"));
            assertThat(bridge, is(notNullValue()));
            assertThat(bridge, is(instanceOf(Bridge.class)));
            assertThat(bridge.getStatus(), is(equalTo(ThingStatus.ONLINE)));
        });
    }

    @Test
    public void assertThatThingsAreCreatedOnlyOnceTheBundleFinishedLoadingWithUpdateFactoryLoaded() {
        assertThat(thingRegistry.getAll().size(), is(0));
        updateModel();
        assertThat(thingRegistry.getAll().size(), is(0));
        registerService(hueThingHandlerFactory, ThingHandlerFactory.class.getName());
        assertThat(thingRegistry.getAll().size(), is(0));
        registerThingTypeProvider();
        finishLoading();
        assertThatAllIsGood();
    }

    @Test
    public void assertThatThingsAreCreatedOnlyOnceTheBundleFinishedLoadingWithFactoryUpdateLoaded() {
        assertThat(thingRegistry.getAll().size(), is(0));
        registerService(hueThingHandlerFactory, ThingHandlerFactory.class.getName());
        assertThat(thingRegistry.getAll().size(), is(0));
        updateModel();
        assertThat(thingRegistry.getAll().size(), is(0));
        registerThingTypeProvider();
        finishLoading();
        assertThatAllIsGood();
    }

    @Test
    public void assertThatThingsAreCreatedOnlyOnceTheBundleFinishedLoadingWithLoadedFactoryUpdate() {
        assertThat(thingRegistry.getAll().size(), is(0));
        registerThingTypeProvider();
        finishLoading();
        assertThat(thingRegistry.getAll().size(), is(0));
        registerService(hueThingHandlerFactory, ThingHandlerFactory.class.getName());
        assertThat(thingRegistry.getAll().size(), is(0));
        updateModel();
        assertThatAllIsGood();
    }

    @Test
    public void assertThatThingsAreCreatedOnlyOnceTheBundleFinishedLoadingWithLoadedUpdateFactory() {
        assertThat(thingRegistry.getAll().size(), is(0));
        registerThingTypeProvider();
        finishLoading();
        assertThat(thingRegistry.getAll().size(), is(0));
        updateModel();
        assertThat(thingRegistry.getAll().size(), is(0));
        registerService(hueThingHandlerFactory, ThingHandlerFactory.class.getName());
        assertThatAllIsGood();
    }

    @Test
    public void assertThatThingsAreCreatedOnlyOnceTheBundleFinishedLoadingWithFactoryLoadedUpdate() {
        assertThat(thingRegistry.getAll().size(), is(0));
        registerService(hueThingHandlerFactory, ThingHandlerFactory.class.getName());
        assertThat(thingRegistry.getAll().size(), is(0));
        registerThingTypeProvider();
        finishLoading();
        assertThat(thingRegistry.getAll().size(), is(0));
        updateModel();
        assertThatAllIsGood();
    }

    @Test
    public void assertThatThingsAreCreatedOnlyOnceTheBundleFinishedLoadingWithUpdateLoadedFactory() {
        assertThat(thingRegistry.getAll().size(), is(0));
        updateModel();
        assertThat(thingRegistry.getAll().size(), is(0));
        registerThingTypeProvider();
        finishLoading();
        assertThat(thingRegistry.getAll().size(), is(0));
        registerService(hueThingHandlerFactory, ThingHandlerFactory.class.getName());
        assertThatAllIsGood();
    }

    @Test
    @SuppressWarnings("null")
    public void assertThatThingHandlersAreManagedCorrectlyOnUpdateWithFactoryLoaded() {
        prepareThingWithShutDownBundle();

        bridgeInitializeCounter = 0;
        registerService(hueThingHandlerFactory, ThingHandlerFactory.class.getName());
        registerThingTypeProvider();
        assertThat(thingRegistry.get(new ThingUID("Xhue:Xbridge:myBridge")).getHandler(), is(nullValue()));
        finishLoading();
        waitForAssert(() -> assertThat(bridgeInitializeCounter >= 1, is(true)));
        assertThatAllIsGood();
    }
}
