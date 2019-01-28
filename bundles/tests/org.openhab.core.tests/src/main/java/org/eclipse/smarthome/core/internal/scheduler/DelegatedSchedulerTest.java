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
package org.eclipse.smarthome.core.internal.scheduler;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import org.eclipse.smarthome.core.scheduler.ScheduledCompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * Test class for {@link DelegatedSchedulerImpl}.
 *
 * @author Hilbrand Bouwkamp - initial contribution
 */
public class DelegatedSchedulerTest {

    private DelegatedSchedulerImpl delegatedscheduler = new DelegatedSchedulerImpl();

    @Mock
    private SchedulerImpl scheduler;
    @Mock
    private ScheduledCompletableFuture<Instant> temporalScheduledFuture;
    @Mock
    private CompletableFuture<Instant> completableFuture;

    @Before
    public void setUp() {
        initMocks(this);
        when(scheduler.after(any())).thenReturn(temporalScheduledFuture);
        when(temporalScheduledFuture.getPromise()).thenReturn(completableFuture);
        delegatedscheduler.setDelegate(scheduler);
    }

    @Test
    public void testAddAndDeactivate() throws InterruptedException, ExecutionException {
        final AtomicBoolean check = new AtomicBoolean();
        when(completableFuture.handle(any())).thenAnswer(a -> null);
        doAnswer(a -> {
            check.set(true);
            return null;
        }).when(temporalScheduledFuture).cancel(true);
        delegatedscheduler.after(Duration.ofMillis(100));
        assertFalse("Check if cancel was not called", check.get());
        delegatedscheduler.deactivate();
        assertTrue("Cancel should be called on deactivation", check.get());
    }

    @Test
    public void testAddRemoveAndDeactivate() throws InterruptedException, ExecutionException {
        final AtomicBoolean check = new AtomicBoolean();
        doAnswer(a -> {
            check.set(true);
            return null;
        }).when(temporalScheduledFuture).cancel(true);
        when(completableFuture.handle(any())).thenAnswer(a -> {
            ((BiFunction<?, ?, ?>) a.getArgument(0)).apply(null, null);
            return null;
        });
        delegatedscheduler.after(Duration.ofMillis(100));
        assertFalse("Check if cancel was not called", check.get());
        delegatedscheduler.deactivate();
        assertFalse("When job handled, cancel should not be called on deactivation. Because is job already gone.",
                check.get());
    }
}
