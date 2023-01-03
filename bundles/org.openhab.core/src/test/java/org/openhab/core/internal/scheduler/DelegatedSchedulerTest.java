/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.internal.scheduler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.scheduler.ScheduledCompletableFuture;

/**
 * Test class for {@link DelegatedSchedulerImpl}.
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class DelegatedSchedulerTest {

    private @NonNullByDefault({}) DelegatedSchedulerImpl delegatedscheduler;

    private @Mock @NonNullByDefault({}) SchedulerImpl schedulerMock;
    private @Mock @NonNullByDefault({}) ScheduledCompletableFuture<Instant> temporalScheduledFutureMock;
    private @Mock @NonNullByDefault({}) CompletableFuture<Instant> completableFutureMock;

    @BeforeEach
    public void setUp() {
        when(schedulerMock.after(any())).thenReturn(temporalScheduledFutureMock);
        when(temporalScheduledFutureMock.getPromise()).thenReturn(completableFutureMock);
        delegatedscheduler = new DelegatedSchedulerImpl(schedulerMock);
    }

    @Test
    public void testAddAndDeactivate() throws InterruptedException, ExecutionException {
        final AtomicBoolean check = new AtomicBoolean();
        when(completableFutureMock.handle(any())).thenAnswer(a -> null);
        doAnswer(a -> {
            check.set(true);
            return null;
        }).when(temporalScheduledFutureMock).cancel(true);
        delegatedscheduler.after(Duration.ofMillis(100));
        assertFalse(check.get(), "Check if cancel was not called");
        delegatedscheduler.deactivate();
        assertTrue(check.get(), "Cancel should be called on deactivation");
    }

    @Test
    public void testAddRemoveAndDeactivate() throws InterruptedException, ExecutionException {
        final AtomicBoolean check = new AtomicBoolean();
        doAnswer(a -> {
            check.set(true);
            return null;
        }).when(temporalScheduledFutureMock).cancel(true);
        when(completableFutureMock.handle(any())).thenAnswer(a -> {
            ((BiFunction<?, ?, ?>) a.getArgument(0)).apply(null, null);
            return null;
        });
        delegatedscheduler.after(Duration.ofMillis(100));
        assertFalse(check.get(), "Check if cancel was not called");
        delegatedscheduler.deactivate();
        assertFalse(check.get(),
                "When job handled, cancel should not be called on deactivation. Because is job already gone.");
    }
}
