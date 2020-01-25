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
package org.openhab.core.model.script.actions;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.time.Instant;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openhab.core.model.script.scheduler.test.MockClosure.MockClosure0;
import org.openhab.core.model.script.scheduler.test.MockClosure.MockClosure1;
import org.openhab.core.model.script.scheduler.test.MockScheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.SchedulerRepository;
import org.quartz.impl.StdSchedulerFactory;

/**
 * Tests for {@link ScriptExecution}
 *
 * @author Jon Evans - Initial contribution
 */
public class ScriptExecutionTest {
    private static MockScheduler scheduler;

    /**
     * Set up Quartz to use our mock scheduler class
     *
     * @throws SchedulerException
     */
    @BeforeClass
    public static void setUp() throws SchedulerException {
        scheduler = new MockScheduler();
        System.setProperty(StdSchedulerFactory.PROPERTIES_FILE, "../../../../../target/classes/quartz-test.properties");
        SchedulerRepository.getInstance().bind(scheduler);

        assertThat(StdSchedulerFactory.getDefaultScheduler(), sameInstance(scheduler));
    }

    private Timer createTimer(MockClosure0 closure) {
        Timer timer = ScriptExecution.createTimer(Instant.now(), closure);
        // The code in our mock closure needs access to the timer object
        closure.setTimer(timer);
        return timer;
    }

    private Timer createTimer(Object arg, MockClosure1 closure) {
        Timer timer = ScriptExecution.createTimerWithArgument(Instant.now(), arg, closure);
        // The code in our mock closure needs access to the timer object
        closure.setTimer(timer);
        return timer;
    }

    /**
     * Test that a running Timer can be rescheduled from within its closure
     *
     * @throws Exception
     */
    @Test
    public void testRescheduleTimerDuringExecution() throws Exception {
        MockClosure0 closure = new MockClosure0(1);
        Timer t = createTimer(closure);

        assertThat(t.isRunning(), is(equalTo(false)));
        assertThat(t.hasTerminated(), is(equalTo(false)));
        assertThat(closure.getApplyCount(), is(equalTo(0)));
        assertThat(scheduler.getPendingJobCount(), is(equalTo(1)));

        // Run the scheduler twice
        scheduler.run();
        assertThat(scheduler.getPendingJobCount(), is(equalTo(1)));
        scheduler.run();
        assertThat(scheduler.getPendingJobCount(), is(equalTo(0)));

        // Check that the Timer ran
        assertThat(closure.getApplyCount(), is(equalTo(2)));
        assertThat(t.isRunning(), is(equalTo(false)));
        assertThat(t.hasTerminated(), is(equalTo(true)));
    }

    /**
     * Tests that a Timer can be rescheduled after it has terminated
     *
     * @throws Exception
     */
    @Test
    public void testRescheduleTimerAfterExecution() throws Exception {
        MockClosure0 closure = new MockClosure0();
        Timer t = createTimer(closure);

        assertThat(t.isRunning(), is(equalTo(false)));
        assertThat(t.hasTerminated(), is(equalTo(false)));
        assertThat(closure.getApplyCount(), is(equalTo(0)));
        assertThat(scheduler.getPendingJobCount(), is(equalTo(1)));

        // Run the scheduler
        scheduler.run();
        assertThat(scheduler.getPendingJobCount(), is(equalTo(0)));

        // Check that the Timer ran
        assertThat(closure.getApplyCount(), is(equalTo(1)));
        assertThat(t.isRunning(), is(equalTo(false)));
        assertThat(t.hasTerminated(), is(equalTo(true)));

        // Now try to reschedule the Timer to run again after 10ms
        boolean rescheduled = t.reschedule(Instant.now());
        assertThat(rescheduled, is(equalTo(true)));
        assertThat(t.hasTerminated(), is(equalTo(false)));
        assertThat(scheduler.getPendingJobCount(), is(equalTo(1)));

        // Run the scheduler
        scheduler.run();
        assertThat(scheduler.getPendingJobCount(), is(equalTo(0)));

        // Check that the Timer ran again
        assertThat(closure.getApplyCount(), is(equalTo(2)));
        assertThat(t.isRunning(), is(equalTo(false)));
        assertThat(t.hasTerminated(), is(equalTo(true)));
    }

    @Test
    public void testClosureWithOneArgument() throws Exception {
        Object arg = Integer.valueOf(42);
        MockClosure1 closure = new MockClosure1(arg, 1);
        Timer t = createTimer(arg, closure);

        assertThat(t.isRunning(), is(equalTo(false)));
        assertThat(t.hasTerminated(), is(equalTo(false)));
        assertThat(closure.getApplyCount(), is(equalTo(0)));
        assertThat(scheduler.getPendingJobCount(), is(equalTo(1)));

        // Run the scheduler twice
        scheduler.run();
        assertThat(scheduler.getPendingJobCount(), is(equalTo(1)));
        scheduler.run();
        assertThat(scheduler.getPendingJobCount(), is(equalTo(0)));

        // Check that the Timer ran
        assertThat(closure.getApplyCount(), is(equalTo(2)));
        assertThat(t.isRunning(), is(equalTo(false)));
        assertThat(t.hasTerminated(), is(equalTo(true)));
    }
}
