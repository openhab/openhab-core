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
package org.eclipse.smarthome.config.discovery;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.smarthome.config.discovery.inbox.Inbox;
import org.eclipse.smarthome.config.setup.test.discovery.DiscoveryServiceMock;
import org.eclipse.smarthome.config.setup.test.discovery.DiscoveryServiceMockOfBridge;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.osgi.framework.ServiceRegistration;

/**
 * The {@link DiscoveryServiceRegistryOSGITest} test checks if the concrete
 * {@link DiscoveryServiceRegistry} implementation tracks all registered
 * {@link DiscoveryService}s and if a registered listener is notified
 * about the events fired by the according{@link DiscoveryService}.
 * <p>
 * This implementation creates two {@link DiscoveryService} mocks and registers
 * them as service at the <i>OSGi</i> service registry. Since this test creates
 * {@link DiscoveryResult}s which are added to the {@link Inbox},
 * the {@link Inbox} is cleared again after this test returns.
 *
 * @author Michael Grammling - Initial Contribution
 * @author Simon Kaufmann - added tests for ExtendedDiscoveryService, ported to Java
 * @author Andre Fuechsel - added tests for removeOlderResults for a specific bridge only
 */
public class DiscoveryServiceRegistryOSGiTest extends JavaOSGiTest {

    private final String ANY_BINDING_ID_1 = "any2BindingId1";
    private final String ANY_THING_TYPE_1 = "any2ThingType1";

    private final String ANY_BINDING_ID_2 = "any2BindingId2";
    private final String ANY_THING_TYPE_2 = "any2ThingType2";

    private final String ANY_BINDING_ID_3 = "any2BindingId3";
    private final String ANY_THING_TYPE_3 = "any2ThingType3";

    private final ThingUID BRIDGE_UID_1 = new ThingUID("binding:bridge:1");
    private final ThingUID BRIDGE_UID_2 = new ThingUID("binding:bridge:2");

    private final String FAULTY_BINDING_ID = "faulty2BindingId";
    private final String FAULTY_THING_TYPE = "faulty2ThingType";

    private static class AnotherDiscoveryService extends DiscoveryServiceMock {
        public AnotherDiscoveryService(ThingTypeUID thingType, int timeout) {
            super(thingType, timeout);
        }
    }

    private DiscoveryServiceMock discoveryServiceMockForBinding1;
    private DiscoveryServiceMock discoveryServiceMockForBinding2;
    private DiscoveryServiceMockOfBridge discoveryServiceMockForBinding3Bridge1;
    private DiscoveryServiceMockOfBridge discoveryServiceMockForBinding3Bridge2;
    private DiscoveryServiceMock discoveryServiceFaultyMock;
    private DiscoveryServiceRegistry discoveryServiceRegistry;
    private final List<ServiceRegistration<?>> serviceRegs = new ArrayList<>();
    private ThingRegistry thingRegistry;
    private Inbox inbox;

    @Mock
    private DiscoveryListener mockDiscoveryListener;

    @Before
    public void setUp() {
        initMocks(this);
        registerVolatileStorageService();

        thingRegistry = getService(ThingRegistry.class);
        assertNotNull(thingRegistry);

        inbox = getService(Inbox.class);
        assertNotNull(inbox);

        discoveryServiceMockForBinding1 = new DiscoveryServiceMock(new ThingTypeUID(ANY_BINDING_ID_1, ANY_THING_TYPE_1),
                1);
        discoveryServiceMockForBinding2 = new DiscoveryServiceMock(new ThingTypeUID(ANY_BINDING_ID_2, ANY_THING_TYPE_2),
                3);

        discoveryServiceMockForBinding3Bridge1 = new DiscoveryServiceMockOfBridge(
                new ThingTypeUID(ANY_BINDING_ID_3, ANY_THING_TYPE_3), 1, BRIDGE_UID_1);
        discoveryServiceMockForBinding3Bridge2 = new DiscoveryServiceMockOfBridge(
                new ThingTypeUID(ANY_BINDING_ID_3, ANY_THING_TYPE_3), 1, BRIDGE_UID_2);

        discoveryServiceFaultyMock = new DiscoveryServiceMock(new ThingTypeUID(FAULTY_BINDING_ID, FAULTY_THING_TYPE), 1,
                true);

        serviceRegs.add(
                bundleContext.registerService(DiscoveryService.class.getName(), discoveryServiceMockForBinding1, null));
        serviceRegs.add(
                bundleContext.registerService(DiscoveryService.class.getName(), discoveryServiceMockForBinding2, null));
        serviceRegs.add(bundleContext.registerService(DiscoveryService.class.getName(),
                discoveryServiceMockForBinding3Bridge1, null));
        serviceRegs.add(bundleContext.registerService(DiscoveryService.class.getName(),
                discoveryServiceMockForBinding3Bridge2, null));
        serviceRegs
                .add(bundleContext.registerService(DiscoveryService.class.getName(), discoveryServiceFaultyMock, null));

        discoveryServiceRegistry = getService(DiscoveryServiceRegistry.class);
    }

    @After
    public void cleanUp() {
        discoveryServiceFaultyMock.abortScan();
        discoveryServiceMockForBinding1.abortScan();
        discoveryServiceMockForBinding2.abortScan();
        discoveryServiceMockForBinding3Bridge1.abortScan();
        discoveryServiceMockForBinding3Bridge2.abortScan();

        serviceRegs.forEach(ServiceRegistration::unregister);

        Inbox inbox = getService(Inbox.class);
        List<DiscoveryResult> discoveryResults = inbox.getAll();
        discoveryResults.forEach(res -> inbox.remove(res.getThingUID()));
        discoveryServiceRegistry.removeDiscoveryListener(mockDiscoveryListener);
    }

    @Test
    public void testStartScan_nonExisting() {
        assertFalse(discoveryServiceRegistry.startScan(new ThingTypeUID("bindingId", "thingType"), null));
    }

    @Test
    public void testStartScan_existing() {
        assertTrue(discoveryServiceRegistry.startScan(new ThingTypeUID(ANY_BINDING_ID_1, ANY_THING_TYPE_1), null));
    }

    @Test
    public void testStan_faulty() {
        assertFalse(discoveryServiceRegistry.startScan(new ThingTypeUID(FAULTY_BINDING_ID, FAULTY_THING_TYPE), null));
    }

    @Test
    public void testAbortScan_nonExisting() {
        assertFalse(discoveryServiceRegistry.abortScan(new ThingTypeUID("bindingId", "thingType")));
    }

    @Test
    public void testAbortScan_known() {
        ScanListener mockScanListener = mock(ScanListener.class);

        assertTrue(discoveryServiceRegistry.startScan(new ThingTypeUID(ANY_BINDING_ID_1, ANY_THING_TYPE_1),
                mockScanListener));
        assertTrue(discoveryServiceRegistry.abortScan(new ThingTypeUID(ANY_BINDING_ID_1, ANY_THING_TYPE_1)));

        waitForAssert(() -> verify(mockScanListener, times(1)).onErrorOccurred(isA(Exception.class)));
        verifyNoMoreInteractions(mockScanListener);
    }

    @Test
    public void testThingDiscovered() {
        ScanListener mockScanListener = mock(ScanListener.class);

        discoveryServiceRegistry.addDiscoveryListener(mockDiscoveryListener);
        discoveryServiceRegistry.startScan(new ThingTypeUID(ANY_BINDING_ID_1, ANY_THING_TYPE_1), mockScanListener);

        waitForAssert(() -> verify(mockScanListener, times(1)).onFinished());
        verify(mockDiscoveryListener, times(1)).thingDiscovered(any(), any());
        verifyNoMoreInteractions(mockScanListener);
        verifyNoMoreInteractions(mockDiscoveryListener);
    }

    @Test
    public void testRemoveOlderResults() {
        discoveryServiceRegistry.addDiscoveryListener(mockDiscoveryListener);
        discoveryServiceMockForBinding1.removeOlderResults(discoveryServiceMockForBinding1.getTimestampOfLastScan());

        waitForAssert(() -> {
            verify(mockDiscoveryListener, times(1)).removeOlderResults(any(DiscoveryService.class), anyLong(), any(),
                    any());
        });
        verifyNoMoreInteractions(mockDiscoveryListener);
    }

    @Test
    public void testRemoveOlderResults_works() {
        ScanListener mockScanListener1 = mock(ScanListener.class);
        ScanListener mockScanListener2 = mock(ScanListener.class);

        discoveryServiceRegistry.addDiscoveryListener(mockDiscoveryListener);
        discoveryServiceRegistry.startScan(new ThingTypeUID(ANY_BINDING_ID_1, ANY_THING_TYPE_1), mockScanListener1);
        discoveryServiceRegistry.startScan(new ThingTypeUID(ANY_BINDING_ID_2, ANY_THING_TYPE_2), mockScanListener2);

        waitForAssert(() -> verify(mockScanListener1, times(1)).onFinished());
        waitForAssert(() -> verify(mockScanListener2, times(1)).onFinished());
        verify(mockDiscoveryListener, times(2)).thingDiscovered(any(), any());

        assertThat(inbox.getAll().size(), is(2));
        // should not remove anything
        discoveryServiceMockForBinding1.removeOlderResults(discoveryServiceMockForBinding1.getTimestampOfLastScan());
        assertThat(inbox.getAll().size(), is(2));

        // start discovery again
        discoveryServiceRegistry.startScan(new ThingTypeUID(ANY_BINDING_ID_1, ANY_THING_TYPE_1), mockScanListener1);
        waitForAssert(() -> verify(mockScanListener1, times(2)).onFinished());
        verify(mockDiscoveryListener, times(3)).thingDiscovered(any(), any());

        assertThat(inbox.getAll().size(), is(3));
        // should remove one entry
        discoveryServiceMockForBinding1.removeOlderResults(discoveryServiceMockForBinding1.getTimestampOfLastScan());
        assertThat(inbox.getAll().size(), is(2));
    }

    @Test
    public void testRemoveOlderResults_onlySameService() {
        mockDiscoveryListener = mock(DiscoveryListener.class);
        ScanListener mockScanListener1 = mock(ScanListener.class);

        discoveryServiceRegistry.addDiscoveryListener(mockDiscoveryListener);
        discoveryServiceRegistry.startScan(new ThingTypeUID(ANY_BINDING_ID_1, ANY_THING_TYPE_1), mockScanListener1);

        waitForAssert(() -> verify(mockScanListener1, times(1)).onFinished());
        verify(mockDiscoveryListener, times(1)).thingDiscovered(any(), any());

        assertThat(inbox.getAll().size(), is(1));

        // register another discovery service for the same thing type
        AnotherDiscoveryService anotherDiscoveryServiceMockForBinding1 = new AnotherDiscoveryService(
                new ThingTypeUID(ANY_BINDING_ID_1, ANY_THING_TYPE_1), 1);
        serviceRegs.add(bundleContext.registerService(DiscoveryService.class.getName(),
                anotherDiscoveryServiceMockForBinding1, null));

        // start discovery again
        discoveryServiceRegistry.startScan(new ThingTypeUID(ANY_BINDING_ID_1, ANY_THING_TYPE_1), mockScanListener1);
        waitForAssert(() -> verify(mockScanListener1, times(2)).onFinished());
        verify(mockDiscoveryListener, times(3)).thingDiscovered(any(), any());

        assertThat(inbox.getAll().size(), is(3));

        // should remove no entry, as there is no older entry discovery of this specific discovery service
        anotherDiscoveryServiceMockForBinding1
                .removeOlderResults(anotherDiscoveryServiceMockForBinding1.getTimestampOfLastScan());
        assertThat(inbox.getAll().size(), is(3));

        // should remove only one entry
        discoveryServiceMockForBinding1.removeOlderResults(discoveryServiceMockForBinding1.getTimestampOfLastScan());
        assertThat(inbox.getAll().size(), is(2));

        anotherDiscoveryServiceMockForBinding1.abortScan();
    }

    @Test
    public void testRemoveOlderResults_onlyOfSpecificBridge() {
        mockDiscoveryListener = mock(DiscoveryListener.class);
        ScanListener mockScanListener1 = mock(ScanListener.class);

        discoveryServiceRegistry.addDiscoveryListener(mockDiscoveryListener);
        discoveryServiceRegistry.startScan(new ThingTypeUID(ANY_BINDING_ID_3, ANY_THING_TYPE_3), mockScanListener1);

        waitForAssert(() -> verify(mockScanListener1, times(1)).onFinished());
        verify(mockDiscoveryListener, times(2)).thingDiscovered(any(), any());

        // 2 discovery services for the same thing type with different bridges - inbox must contain 2 elements
        assertThat(inbox.getAll().size(), is(2));
        assertThat(inbox.getAll().stream().filter(r -> BRIDGE_UID_1.equals(r.getBridgeUID())).count(), is(1L));
        assertThat(inbox.getAll().stream().filter(r -> BRIDGE_UID_2.equals(r.getBridgeUID())).count(), is(1L));

        // should not remove anything
        discoveryServiceMockForBinding3Bridge1.removeOlderResults(
                discoveryServiceMockForBinding3Bridge1.getTimestampOfLastScan(),
                discoveryServiceMockForBinding3Bridge1.getBridge());
        assertThat(inbox.getAll().size(), is(2));

        // should not remove anything
        discoveryServiceMockForBinding3Bridge2.removeOlderResults(
                discoveryServiceMockForBinding3Bridge2.getTimestampOfLastScan(),
                discoveryServiceMockForBinding3Bridge2.getBridge());
        assertThat(inbox.getAll().size(), is(2));

        // start discovery again
        discoveryServiceRegistry.startScan(new ThingTypeUID(ANY_BINDING_ID_3, ANY_THING_TYPE_3), mockScanListener1);

        waitForAssert(() -> verify(mockScanListener1, times(1)).onFinished());
        verify(mockDiscoveryListener, times(4)).thingDiscovered(any(), any());

        // 2 discovery services for the same thing type with different bridges - inbox must now contain 4 elements
        assertThat(inbox.getAll().size(), is(4));
        assertThat(inbox.getAll().stream().filter(r -> BRIDGE_UID_1.equals(r.getBridgeUID())).count(), is(2L));
        assertThat(inbox.getAll().stream().filter(r -> BRIDGE_UID_2.equals(r.getBridgeUID())).count(), is(2L));

        // should remove only 1 entry (of bridge1)
        discoveryServiceMockForBinding3Bridge1.removeOlderResults(
                discoveryServiceMockForBinding3Bridge1.getTimestampOfLastScan(),
                discoveryServiceMockForBinding3Bridge1.getBridge());
        assertThat(inbox.getAll().size(), is(3));
        assertThat(inbox.getAll().stream().filter(r -> BRIDGE_UID_1.equals(r.getBridgeUID())).count(), is(1L));
        assertThat(inbox.getAll().stream().filter(r -> BRIDGE_UID_2.equals(r.getBridgeUID())).count(), is(2L));

        // should remove only 1 entry (of bridge2)
        discoveryServiceMockForBinding3Bridge2.removeOlderResults(
                discoveryServiceMockForBinding3Bridge2.getTimestampOfLastScan(),
                discoveryServiceMockForBinding3Bridge2.getBridge());
        assertThat(inbox.getAll().size(), is(2));
        assertThat(inbox.getAll().stream().filter(r -> BRIDGE_UID_1.equals(r.getBridgeUID())).count(), is(1L));
        assertThat(inbox.getAll().stream().filter(r -> BRIDGE_UID_2.equals(r.getBridgeUID())).count(), is(1L));

    }

    @Test
    public void testThingDiscovered_removedListener() {
        ScanListener mockScanListener1 = mock(ScanListener.class);
        discoveryServiceRegistry.addDiscoveryListener(mockDiscoveryListener);
        discoveryServiceRegistry.removeDiscoveryListener(mockDiscoveryListener);
        discoveryServiceRegistry.startScan(new ThingTypeUID(ANY_BINDING_ID_1, ANY_THING_TYPE_1), mockScanListener1);

        waitForAssert(() -> verify(mockScanListener1, times(1)).onFinished());
        verifyNoMoreInteractions(mockDiscoveryListener);
    }

    @Test
    public void testStartScan_twoDiscoveryServices() {
        ScanListener mockScanListener1 = mock(ScanListener.class);
        DiscoveryService anotherDiscoveryServiceMock = new DiscoveryServiceMock(
                new ThingTypeUID(ANY_BINDING_ID_1, ANY_THING_TYPE_1), 1);
        serviceRegs.add(
                bundleContext.registerService(DiscoveryService.class.getName(), anotherDiscoveryServiceMock, null));
        discoveryServiceRegistry.addDiscoveryListener(mockDiscoveryListener);
        discoveryServiceRegistry.startScan(new ThingTypeUID(ANY_BINDING_ID_1, ANY_THING_TYPE_1), mockScanListener1);

        waitForAssert(() -> mockScanListener1.onFinished());
        verify(mockDiscoveryListener, times(2)).thingDiscovered(any(), any());
    }

    @Test
    public void testStartScan_bindingId() {
        ScanListener mockScanListener1 = mock(ScanListener.class);
        discoveryServiceRegistry.startScan(ANY_BINDING_ID_1, mockScanListener1);

        waitForAssert(() -> verify(mockScanListener1, times(1)).onFinished());
    }

    @Test
    public void testSupportsDiscovery() {
        assertTrue(discoveryServiceRegistry.supportsDiscovery(new ThingTypeUID(ANY_BINDING_ID_1, ANY_THING_TYPE_1)));
        assertFalse(discoveryServiceRegistry.supportsDiscovery(new ThingTypeUID(ANY_BINDING_ID_1, "unknownType")));

        assertTrue(discoveryServiceRegistry.supportsDiscovery(ANY_BINDING_ID_1));
        assertFalse(discoveryServiceRegistry.supportsDiscovery("unknownBindingId"));
    }

    @Test
    public void testGetMaxScanTimeout() {
        assertEquals(1,
                discoveryServiceRegistry.getMaxScanTimeout(new ThingTypeUID(ANY_BINDING_ID_1, ANY_THING_TYPE_1)));
        assertEquals(0, discoveryServiceRegistry.getMaxScanTimeout(new ThingTypeUID(ANY_BINDING_ID_1, "unknownType")));

        assertEquals(3, discoveryServiceRegistry.getMaxScanTimeout(ANY_BINDING_ID_2));
        assertEquals(0, discoveryServiceRegistry.getMaxScanTimeout("unknownBindingId"));
    }

}
