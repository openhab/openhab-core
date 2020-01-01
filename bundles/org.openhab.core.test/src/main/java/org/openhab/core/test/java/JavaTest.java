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
package org.openhab.core.test.java;

import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * {@link JavaTest} is an abstract base class for tests which are not necessarily based on OSGi.
 *
 * @author Simon Kaufmann - Initial contribution
 */
@NonNullByDefault
public class JavaTest {

    protected static final int DFL_TIMEOUT = 10000;
    protected static final int DFL_SLEEP_TIME = 50;

    /**
     * Wait until the condition is fulfilled or the timeout is reached.
     *
     * <p>
     * This method uses the default timing parameters.
     *
     * @param condition the condition to check
     * @return true on success, false on timeout
     */
    protected boolean waitFor(BooleanSupplier condition) {
        return waitFor(condition, DFL_TIMEOUT, DFL_SLEEP_TIME);
    }

    /**
     * Wait until the condition is fulfilled or the timeout is reached.
     *
     * @param condition the condition to check
     * @param timeout timeout
     * @param sleepTime interval for checking the condition
     * @return true on success, false on timeout
     */
    protected boolean waitFor(BooleanSupplier condition, long timeout, long sleepTime) {
        int waitingTime = 0;
        boolean rv;
        while (!(rv = condition.getAsBoolean()) && waitingTime < timeout) {
            waitingTime += sleepTime;
            internalSleep(sleepTime);
        }
        return rv;
    }

    /**
     * Wait until the assertion is fulfilled or the timeout is reached.
     *
     * <p>
     * This method uses the default timing parameters.
     *
     * @param assertion closure that must not have an argument
     */
    protected void waitForAssert(Runnable assertion) {
        waitForAssert(assertion, null, DFL_TIMEOUT, DFL_SLEEP_TIME);
    }

    /**
     * Wait until the assertion is fulfilled or the timeout is reached.
     *
     * @param assertion the logic to execute
     * @param timeout timeout
     * @param sleepTime interval for checking the condition
     */
    protected void waitForAssert(Runnable assertion, long timeout, long sleepTime) {
        waitForAssert(assertion, null, timeout, sleepTime);
    }

    /**
     * Wait until the assertion is fulfilled or the timeout is reached.
     *
     * <p>
     * This method uses the default timing parameters.
     *
     * @param assertion the logic to execute
     * @return the return value of the supplied assertion object's function on success
     */
    protected <T> T waitForAssert(Supplier<T> assertion) {
        return waitForAssert(assertion, null, DFL_TIMEOUT, DFL_SLEEP_TIME);
    }

    /**
     * Wait until the assertion is fulfilled or the timeout is reached.
     *
     * @param assertion the logic to execute
     * @param timeout timeout
     * @param sleepTime interval for checking the condition
     * @return the return value of the supplied assertion object's function on success
     */
    protected <T> T waitForAssert(Supplier<T> assertion, long timeout, long sleepTime) {
        return waitForAssert(assertion, null, timeout, sleepTime);
    }

    /**
     * Wait until the assertion is fulfilled or the timeout is reached.
     *
     * @param assertion the logic to execute
     * @param beforeLastCall logic to execute in front of the last call to ${code assertion}
     * @param sleepTime interval for checking the condition
     */
    protected void waitForAssert(Runnable assertion, @Nullable Runnable beforeLastCall, long timeout, long sleepTime) {
        waitForAssert(assertion, beforeLastCall, null, timeout, sleepTime);
    }

    /**
     * Wait until the assertion is fulfilled or the timeout is reached.
     *
     * @param assertion the logic to execute
     * @param beforeLastCall logic to execute in front of the last call to ${code assertion}
     * @param afterLastCall logic to execute after the last call to ${code assertion}
     * @param sleepTime interval for checking the condition
     */
    protected void waitForAssert(Runnable assertion, @Nullable Runnable beforeLastCall,
            @Nullable Runnable afterLastCall, long timeout, long sleepTime) {
        long waitingTime = 0;
        while (waitingTime < timeout) {
            try {
                assertion.run();

                if (afterLastCall != null) {
                    afterLastCall.run();
                }
                return;
            } catch (final Error error) {
                waitingTime += sleepTime;
                internalSleep(sleepTime);
            }
        }
        if (beforeLastCall != null) {
            beforeLastCall.run();
        }

        try {
            assertion.run();
        } finally {
            if (afterLastCall != null) {
                afterLastCall.run();
            }
        }
    }

    /**
     * Useful for testing @NonNull annotated parameters
     *
     * <p>
     * This method can be used if you want to check the behavior of a method if you supply null to a non-null marked
     * argument.
     * If you use null directly the compiler will raise an error. Using this method allows you to work around that
     * compiler check by using a null value that is marked as non-null.
     *
     * @return null for testing purpose
     */
    protected static <T> T giveNull() {
        return null;
    }

    /**
     * Wait until the assertion is fulfilled or the timeout is reached.
     *
     * @param assertion the logic to execute
     * @param beforeLastCall logic to execute in front of the last call to ${code assertion}
     * @param sleepTime interval for checking the condition
     * @return the return value of the supplied assertion object's function on success
     */
    private <T> T waitForAssert(Supplier<T> assertion, @Nullable Runnable beforeLastCall, long timeout,
            long sleepTime) {
        final long timeoutNs = TimeUnit.MILLISECONDS.toNanos(timeout);
        final long startingTime = System.nanoTime();
        while (System.nanoTime() - startingTime < timeoutNs) {
            try {
                return assertion.get();
            } catch (final Error | NullPointerException error) {
                internalSleep(sleepTime);
            }
        }
        if (beforeLastCall != null) {
            beforeLastCall.run();
        }
        return assertion.get();
    }

    private void internalSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new IllegalStateException("We shouldn't be interrupted while testing");
        }
    }

}
