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
package org.openhab.core.model.script.scheduler.test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.time.Instant;

import org.eclipse.xtext.xbase.lib.Procedures.Procedure0;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.openhab.core.model.script.actions.Timer;

/**
 * Mock implementations of Procedure0 and Procedure1
 *
 * @author Jon Evans - Initial contribution
 */
public class MockClosure {
    public static class MockClosure0 implements Procedure0 {
        int rescheduleCount;
        private int applyCount;
        private Timer timer;

        public MockClosure0() {
            this(0);
        }

        public MockClosure0(int rescheduleCount) {
            this.rescheduleCount = rescheduleCount;
        }

        @Override
        public void apply() {
            this.applyCount++;
            // Timer#isRunning() should return true
            // from within the body of the closure
            assertThat(timer.isRunning(), is(equalTo(true)));

            if (this.rescheduleCount > 0) {
                this.rescheduleCount--;
                boolean rescheduled = timer.reschedule(Instant.now());
                assertThat(rescheduled, is(equalTo(true)));
            }
        }

        public void setTimer(Timer timer) {
            this.timer = timer;
        }

        public int getApplyCount() {
            return applyCount;
        }
    }

    public static class MockClosure1 extends MockClosure0 implements Procedure1<Object> {
        private final Object expectedArgument;

        public MockClosure1(Object expectedArgument) {
            this(expectedArgument, 0);
        }

        public MockClosure1(Object expectedArgument, int rescheduleCount) {
            super(rescheduleCount);
            this.expectedArgument = expectedArgument;
        }

        @Override
        public void apply(Object arg) {
            assertThat(arg, is(sameInstance(expectedArgument)));
            apply();
        }

    }
}
