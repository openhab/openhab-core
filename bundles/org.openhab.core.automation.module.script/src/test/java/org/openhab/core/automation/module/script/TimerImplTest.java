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
package org.openhab.core.automation.module.script;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.automation.module.script.action.Timer;
import org.openhab.core.automation.module.script.internal.action.TimerImpl;
import org.openhab.core.internal.scheduler.SchedulerImpl;
import org.openhab.core.scheduler.SchedulerRunnable;

/**
 * This are tests for {@link TimerImpl}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class TimerImplTest {

    // timer expires in 2 sec
    private static final long DEFAULT_TIMEOUT_SECONDS = 2;
    // runnable is running for 1 sec
    private static final long DEFAULT_RUNTIME_SECONDS = 1;

    private final SchedulerImpl scheduler = new SchedulerImpl();
    private @NonNullByDefault({}) Timer subject;

    @BeforeEach
    public void setUp() {
        subject = createTimer(ZonedDateTime.now().plusSeconds(DEFAULT_TIMEOUT_SECONDS), () -> {
            Thread.sleep(TimeUnit.SECONDS.toMillis(DEFAULT_RUNTIME_SECONDS));
        });
    }

    @Test
    public void testTimerIsActiveAndCancel() {
        assertThat(subject.isActive(), is(true));
        assertThat(subject.hasTerminated(), is(false));
        assertThat(subject.isCancelled(), is(false));

        subject.cancel();
        assertThat(subject.isActive(), is(false));
        assertThat(subject.hasTerminated(), is(true));
        assertThat(subject.isCancelled(), is(true));

        subject.reschedule(ZonedDateTime.now().plusSeconds(DEFAULT_TIMEOUT_SECONDS));
        assertThat(subject.isActive(), is(true));
        assertThat(subject.hasTerminated(), is(false));
        assertThat(subject.isCancelled(), is(false));
    }

    @Test
    public void testTimerIsActiveAndTerminate() throws InterruptedException {
        assertThat(subject.isActive(), is(true));
        assertThat(subject.hasTerminated(), is(false));
        assertThat(subject.isCancelled(), is(false));

        Thread.sleep(TimeUnit.SECONDS.toMillis(DEFAULT_TIMEOUT_SECONDS + DEFAULT_RUNTIME_SECONDS + 3));
        assertThat(subject.isActive(), is(false));
        assertThat(subject.hasTerminated(), is(true));
        assertThat(subject.isCancelled(), is(false));
    }

    @Test
    public void testTimerHasTerminatedAndReschedule() throws InterruptedException {
        Thread.sleep(TimeUnit.SECONDS.toMillis(DEFAULT_TIMEOUT_SECONDS + DEFAULT_RUNTIME_SECONDS + 3));
        assertThat(subject.isActive(), is(false));
        assertThat(subject.hasTerminated(), is(true));
        assertThat(subject.isCancelled(), is(false));

        subject.reschedule(ZonedDateTime.now().plusSeconds(DEFAULT_TIMEOUT_SECONDS));
        assertThat(subject.isActive(), is(true));
        assertThat(subject.hasTerminated(), is(false));
        assertThat(subject.isCancelled(), is(false));

        Thread.sleep(TimeUnit.SECONDS.toMillis(DEFAULT_TIMEOUT_SECONDS + DEFAULT_RUNTIME_SECONDS + 3));
        assertThat(subject.isActive(), is(false));
        assertThat(subject.hasTerminated(), is(true));
        assertThat(subject.isCancelled(), is(false));
    }

    @Test
    public void testTimerIsRunning() throws InterruptedException {
        assertThat(subject.isRunning(), is(false));
        assertThat(subject.hasTerminated(), is(false));
        assertThat(subject.isCancelled(), is(false));

        Thread.sleep(TimeUnit.SECONDS.toMillis(DEFAULT_TIMEOUT_SECONDS) + 500);
        assertThat(subject.isRunning(), is(true));
        assertThat(subject.hasTerminated(), is(false));
        assertThat(subject.isCancelled(), is(false));

        Thread.sleep(TimeUnit.SECONDS.toMillis(DEFAULT_RUNTIME_SECONDS + 3));
        assertThat(subject.isRunning(), is(false));
        assertThat(subject.hasTerminated(), is(true));
        assertThat(subject.isCancelled(), is(false));
    }

    private Timer createTimer(ZonedDateTime instant, SchedulerRunnable runnable) {
        return new TimerImpl(scheduler, instant, runnable);
    }
}
