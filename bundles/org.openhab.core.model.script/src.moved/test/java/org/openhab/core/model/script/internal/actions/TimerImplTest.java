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
package org.openhab.core.model.script.internal.actions;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.Before;
import org.junit.Test;
import org.openhab.core.internal.scheduler.SchedulerImpl;
import org.openhab.core.model.script.actions.Timer;
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

    @Before
    public void setUp() {
        subject = createTimer(ZonedDateTime.now().plusSeconds(DEFAULT_TIMEOUT_SECONDS), () -> {
            Thread.sleep(TimeUnit.SECONDS.toMillis(DEFAULT_RUNTIME_SECONDS));
        });
    }

    @Test
    public void testTimerIsActiveAndCancel() {
        assertThat(subject.isActive(), is(true));
        assertThat(subject.hasTerminated(), is(false));

        subject.cancel();
        assertThat(subject.isActive(), is(false));
        assertThat(subject.hasTerminated(), is(true));

        subject.reschedule(ZonedDateTime.now().plusSeconds(DEFAULT_TIMEOUT_SECONDS));
        assertThat(subject.isActive(), is(true));
        assertThat(subject.hasTerminated(), is(false));
    }

    @Test
    public void testTimerIsActiveAndTerminate() throws InterruptedException {
        assertThat(subject.isActive(), is(true));
        assertThat(subject.hasTerminated(), is(false));

        Thread.sleep(TimeUnit.SECONDS.toMillis(DEFAULT_TIMEOUT_SECONDS + DEFAULT_RUNTIME_SECONDS + 1));
        assertThat(subject.isActive(), is(false));
        assertThat(subject.hasTerminated(), is(true));
    }

    @Test
    public void testTimerIsRunning() throws InterruptedException {
        assertThat(subject.isRunning(), is(false));
        assertThat(subject.hasTerminated(), is(false));

        Thread.sleep(TimeUnit.SECONDS.toMillis(DEFAULT_TIMEOUT_SECONDS) + 500);
        assertThat(subject.isRunning(), is(true));
        assertThat(subject.hasTerminated(), is(false));

        Thread.sleep(TimeUnit.SECONDS.toMillis(DEFAULT_RUNTIME_SECONDS + 1));
        assertThat(subject.isRunning(), is(false));
        assertThat(subject.hasTerminated(), is(true));
    }

    private Timer createTimer(ZonedDateTime instant, SchedulerRunnable runnable) {
        return new TimerImpl(scheduler, instant, runnable);
    }
}
