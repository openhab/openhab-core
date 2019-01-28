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
package org.eclipse.smarthome.model.script.scheduler.test;

import static org.hamcrest.CoreMatchers.*;
import static org.joda.time.Instant.now;
import static org.junit.Assert.assertThat;

import org.eclipse.smarthome.model.script.actions.Timer;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure0;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;

/**
 * Mock implementations of Procedure0 and Procedure1
 *
 * @author Jon Evans - initial contribution
 *
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
                boolean rescheduled = timer.reschedule(now());
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
