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
package org.eclipse.smarthome.core.internal.service;

import static org.junit.Assert.*;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

import org.eclipse.smarthome.core.service.ReadyMarker;
import org.eclipse.smarthome.core.service.ReadyMarkerFilter;
import org.eclipse.smarthome.core.service.ReadyService.ReadyTracker;
import org.junit.Test;

/**
 *
 * @author Simon Kaufmann - initial contribution and API.
 *
 */
public class ReadyServiceImplTest {

    @Test
    public void testDifferentReadyMarkerInstances() {
        ReadyServiceImpl rs = new ReadyServiceImpl();
        assertFalse(rs.isReady(new ReadyMarker("test", "id")));
        rs.markReady(new ReadyMarker("test", "id"));
        assertTrue(rs.isReady(new ReadyMarker("test", "id")));
        rs.unmarkReady(new ReadyMarker("test", "id"));
        assertFalse(rs.isReady(new ReadyMarker("test", "id")));
    }

    @Test
    public void testTrackersAreInformedInitially() {
        ReadyTracker tracker = mock(ReadyTracker.class);
        ReadyServiceImpl rs = new ReadyServiceImpl();
        rs.markReady(new ReadyMarker("test", "id"));
        rs.registerTracker(tracker);
        verify(tracker).onReadyMarkerAdded(isA(ReadyMarker.class));
        verifyNoMoreInteractions(tracker);
    }

    @Test
    public void testTrackersAreInformedOnChange() {
        ReadyTracker tracker = mock(ReadyTracker.class);
        ReadyServiceImpl rs = new ReadyServiceImpl();
        rs.registerTracker(tracker);
        rs.markReady(new ReadyMarker("test", "id"));
        verify(tracker).onReadyMarkerAdded(isA(ReadyMarker.class));
        rs.unmarkReady(new ReadyMarker("test", "id"));
        verify(tracker).onReadyMarkerRemoved(isA(ReadyMarker.class));
        verifyNoMoreInteractions(tracker);
    }

    @Test
    public void testTrackersAreInformedOnlyOnMatch() {
        ReadyTracker tracker = mock(ReadyTracker.class);
        ReadyServiceImpl rs = new ReadyServiceImpl();
        rs.registerTracker(tracker, new ReadyMarkerFilter().withType("test"));
        rs.markReady(new ReadyMarker("foo", "id"));
        verifyNoMoreInteractions(tracker);
        rs.markReady(new ReadyMarker("test", "id"));
        verify(tracker).onReadyMarkerAdded(isA(ReadyMarker.class));
        verifyNoMoreInteractions(tracker);
    }

    @Test
    public void testUnregisterTracker() {
        ReadyTracker tracker = mock(ReadyTracker.class);
        ReadyServiceImpl rs = new ReadyServiceImpl();
        rs.markReady(new ReadyMarker("foo", "id"));
        rs.registerTracker(tracker, new ReadyMarkerFilter());
        verify(tracker).onReadyMarkerAdded(isA(ReadyMarker.class));
        rs.unregisterTracker(tracker);
        verify(tracker).onReadyMarkerRemoved(isA(ReadyMarker.class));
        verifyNoMoreInteractions(tracker);
    }

}
