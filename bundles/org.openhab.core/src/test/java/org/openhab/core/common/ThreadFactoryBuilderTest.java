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
package org.openhab.core.common;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;

import org.junit.Test;

/**
 * Unit tests for the {@link ThreadFactoryBuilder}.
 *
 * @author Henning Sudbrock - Initial contribution
 */
public class ThreadFactoryBuilderTest {

    private static final Runnable TEST_RUNNABLE = () -> {
    };

    @Test
    public void testDefaults() {
        ThreadFactory threadFactory = ThreadFactoryBuilder.create().build();
        Thread thread = threadFactory.newThread(TEST_RUNNABLE);

        assertThat(thread.getName(), is(notNullValue()));
        assertThat(thread.isDaemon(), is(false));
        assertThat(thread.getUncaughtExceptionHandler(), is(notNullValue()));
    }

    @Test
    public void testWithNamePrefixAndWithName() {
        ThreadFactory threadFactory = ThreadFactoryBuilder.create().withNamePrefix("prefix").withName("hello").build();

        assertThat(threadFactory.newThread(TEST_RUNNABLE).getName(), is("prefix-hello-1"));
        assertThat(threadFactory.newThread(TEST_RUNNABLE).getName(), is("prefix-hello-2"));
        assertThat(threadFactory.newThread(TEST_RUNNABLE).getName(), is("prefix-hello-3"));
    }

    @Test
    public void testWithDefaultNamePrefix() {
        ThreadFactory threadFactory = ThreadFactoryBuilder.create().withName("hello").build();

        assertThat(threadFactory.newThread(TEST_RUNNABLE).getName(), is("ESH-hello-1"));
        assertThat(threadFactory.newThread(TEST_RUNNABLE).getName(), is("ESH-hello-2"));
        assertThat(threadFactory.newThread(TEST_RUNNABLE).getName(), is("ESH-hello-3"));
    }

    @Test
    public void testWithNamePrefixWithoutName() {
        ThreadFactory threadFactory = ThreadFactoryBuilder.create().withNamePrefix("prefix").build();

        assertThat(threadFactory.newThread(TEST_RUNNABLE).getName(), startsWith("prefix-"));
    }

    @Test
    public void testWithoutNamePrefixWithName() {
        ThreadFactory threadFactory = ThreadFactoryBuilder.create().withNamePrefix(null).withName("hello").build();

        assertThat(threadFactory.newThread(TEST_RUNNABLE).getName(), is("hello-1"));
        assertThat(threadFactory.newThread(TEST_RUNNABLE).getName(), is("hello-2"));
        assertThat(threadFactory.newThread(TEST_RUNNABLE).getName(), is("hello-3"));
    }

    @Test
    public void testWithoutNamePrefixWithoutName() {
        ThreadFactory threadFactory = ThreadFactoryBuilder.create().withNamePrefix(null).withName(null).build();

        // Create a thread, to check that there are no exceptions.
        threadFactory.newThread(TEST_RUNNABLE);

        // No more assertions on the name, as it depends only on the wrapped thread factory's naming strategy.
    }

    @Test
    public void testWithDaemonThreads() {
        ThreadFactory threadFactory = ThreadFactoryBuilder.create().withDaemonThreads(true).build();
        assertThat(threadFactory.newThread(TEST_RUNNABLE).isDaemon(), is(true));

        threadFactory = ThreadFactoryBuilder.create().withDaemonThreads(false).build();
        assertThat(threadFactory.newThread(TEST_RUNNABLE).isDaemon(), is(false));
    }

    @Test
    public void testWithUncaughtExceptionHandler() {
        UncaughtExceptionHandler handler = new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
            }
        };

        ThreadFactory threadFactory = ThreadFactoryBuilder.create().withUncaughtExceptionHandler(handler).build();

        assertThat(threadFactory.newThread(TEST_RUNNABLE).getUncaughtExceptionHandler(), is(handler));
    }

    @Test
    public void testWithPriority() {
        ThreadFactory threadFactory = ThreadFactoryBuilder.create().withPriority(Thread.MIN_PRIORITY).build();
        assertThat(threadFactory.newThread(TEST_RUNNABLE).getPriority(), is(Thread.MIN_PRIORITY));

        threadFactory = ThreadFactoryBuilder.create().withPriority(Thread.MAX_PRIORITY).build();
        assertThat(threadFactory.newThread(TEST_RUNNABLE).getPriority(), is(Thread.MAX_PRIORITY));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithPriorityValidationTooLow() {
        ThreadFactoryBuilder.create().withPriority(Thread.MIN_PRIORITY - 1).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithPriorityValidationTooHigh() {
        ThreadFactoryBuilder.create().withPriority(Thread.MAX_PRIORITY + 1).build();
    }

    @Test
    public void testWithWrappedThreadFactory() {
        String testThreadName = "i-am-test";

        ThreadFactory wrappedThreadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread result = new Thread(r);
                result.setName(testThreadName);
                return result;
            }
        };

        ThreadFactory threadFactory = ThreadFactoryBuilder.create().withWrappedThreadFactory(wrappedThreadFactory)
                .build();
        Thread testThread = threadFactory.newThread(TEST_RUNNABLE);

        assertThat(testThread.getName(), is("ESH-" + testThreadName));
    }
}
