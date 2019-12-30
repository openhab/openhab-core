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
package org.openhab.core.internal.common;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.lang.Thread.State;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.mockito.Mock;
import org.openhab.core.common.QueueingThreadPoolExecutor;
import org.openhab.core.test.java.JavaTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Simon Kaufmann - Initial contribution and API.
 */
public class SafeCallerImplTest extends JavaTest {

    private static final int THREAD_POOL_SIZE = 3;

    // the duration that the called object should block for
    private static final int BLOCK = 1000;

    // the standard timeout for the safe-caller used in most tests
    private static final int TIMEOUT = 500;

    // the grace period allowed for processing before a timing assertion should fail
    private static final int GRACE = 300;

    private final Logger logger = LoggerFactory.getLogger(SafeCallerImplTest.class);

    public @Rule TestName name = new TestName();

    private @Mock Runnable mockRunnable;
    private @Mock Runnable mockTimeoutHandler;
    private @Mock Consumer<Throwable> mockErrorHandler;

    private QueueingThreadPoolExecutor scheduler;
    private final List<AssertingThread> threads = new LinkedList<>();

    private SafeCallerImpl safeCaller;

    public static interface ITarget {
        public String method();
    }

    public static class Target implements ITarget {
        @Override
        public String method() {
            return "Hello";
        }
    }

    public static class DerivedTarget extends Target implements ITarget {
    }

    @Before
    public void setup() {
        initMocks(this);
        scheduler = QueueingThreadPoolExecutor.createInstance(name.getMethodName(), THREAD_POOL_SIZE);
        safeCaller = new SafeCallerImpl() {
            @Override
            protected ExecutorService getScheduler() {
                return scheduler;
            }
        };
        safeCaller.activate(null);

        assertTrue(BLOCK > TIMEOUT + GRACE);
        assertTrue(GRACE < TIMEOUT);
    }

    @After
    public void tearDown() throws Exception {
        // ensure all "inner" assertion errors are heard
        joinAll();

        scheduler.shutdownNow();
        safeCaller.deactivate();
    }

    @Test
    public void testSimpleCall() throws Exception {
        Target target = new Target();
        String result = safeCaller.create(target, ITarget.class).build().method();
        assertThat(result, is("Hello"));
    }

    @Test
    public void testInterfaceDetection() throws Exception {
        ITarget target = new Target();
        String result = safeCaller.create(target, ITarget.class).build().method();
        assertThat(result, is("Hello"));
    }

    @Test
    public void testExceptionHandler() throws Exception {
        Runnable mock = mock(Runnable.class);
        doThrow(RuntimeException.class).when(mock).run();

        safeCaller.create(mock, Runnable.class).onException(mockErrorHandler).build().run();
        waitForAssert(() -> {
            verify(mockErrorHandler).accept(isA(Throwable.class));
        });
    }

    @Test
    public void testTimeoutHandler() throws Exception {
        Runnable mock = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock).run();

        safeCaller.create(mock, Runnable.class).withTimeout(TIMEOUT).onTimeout(mockTimeoutHandler).build().run();
        waitForAssert(() -> {
            verify(mockTimeoutHandler).run();
        });
    }

    @Test
    public void testTimeoutReturnsEarly() throws Exception {
        Runnable mock = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock).run();

        assertDurationBetween(TIMEOUT - GRACE, BLOCK - GRACE, () -> {
            safeCaller.create(mock, Runnable.class).withTimeout(TIMEOUT).build().run();
        });
    }

    @Test
    public void testMultiThreadSync() {
        Runnable mock = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock).run();

        spawn(() -> {
            assertDurationBetween(TIMEOUT - GRACE, BLOCK - GRACE, () -> {
                safeCaller.create(mock, Runnable.class).withTimeout(TIMEOUT).build().run();
            });
        });
        sleep(GRACE); // give it a chance to start
        spawn(() -> {
            assertDurationBetween(TIMEOUT - GRACE, BLOCK - GRACE, () -> {
                safeCaller.create(mock, Runnable.class).withTimeout(TIMEOUT).build().run();
            });
        });
        waitForAssert(() -> {
            verify(mock, times(2)).run();
        });
    }

    @Test
    public void testSingleThreadSyncSecondCallWhileInTimeout() throws Exception {
        Runnable mock = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock).run();
        configureSingleThread();

        assertDurationBetween(TIMEOUT - GRACE, BLOCK - GRACE, () -> {
            safeCaller.create(mock, Runnable.class).withTimeout(TIMEOUT).build().run();
        });
        assertDurationBelow(GRACE, () -> {
            safeCaller.create(mock, Runnable.class).withTimeout(TIMEOUT).build().run();
        });
        assertDurationBetween(TIMEOUT - GRACE, BLOCK + GRACE, () -> {
            waitForAssert(() -> {
                verify(mock, times(2)).run();
            });
        });
    }

    @Test
    public void testSingleThreadSyncParallel() throws Exception {
        Runnable mock = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock).run();
        configureSingleThread();

        spawn(() -> {
            assertDurationBetween(TIMEOUT - GRACE, BLOCK - GRACE, () -> {
                safeCaller.create(mock, Runnable.class).withTimeout(TIMEOUT).build().run();
            });
        });
        sleep(GRACE); // give it a chance to start
        spawn(() -> {
            assertDurationBelow(GRACE, () -> {
                safeCaller.create(mock, Runnable.class).withTimeout(TIMEOUT).build().run();
            });
        });
        assertDurationBetween(BLOCK - 2 * GRACE, BLOCK + TIMEOUT + GRACE, () -> {
            waitForAssert(() -> {
                verify(mock, times(2)).run();
            });
        });
    }

    @Test
    public void testMultiThreadAsync() throws Exception {
        Runnable mock = mock(Runnable.class);
        doAnswer(a -> sleep(TIMEOUT)).when(mock).run();

        assertDurationBelow(GRACE, () -> {
            safeCaller.create(mock, Runnable.class).withTimeout(TIMEOUT).withAsync().build().run();
        });
        assertDurationBelow(GRACE, () -> {
            safeCaller.create(mock, Runnable.class).withTimeout(TIMEOUT).withAsync().build().run();
        });
        waitForAssert(() -> {
            verify(mock, times(2)).run();
        });
    }

    @Test
    public void testSingleThreadAsync() throws Exception {
        Runnable mock = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock).run();
        configureSingleThread();

        assertDurationBelow(GRACE, () -> {
            safeCaller.create(mock, Runnable.class).withTimeout(TIMEOUT).withAsync().build().run();
        });
        assertDurationBelow(GRACE, () -> {
            safeCaller.create(mock, Runnable.class).withTimeout(TIMEOUT).withAsync().build().run();
        });
        waitForAssert(() -> {
            verify(mock, times(2)).run();
        });
    }

    @Test
    public void testSecondCallGetsRefusedSameIdentifier() throws Exception {
        Runnable mock1 = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock1).run();
        Runnable mock2 = mock(Runnable.class);

        assertDurationBetween(TIMEOUT - GRACE, BLOCK - GRACE, () -> {
            safeCaller.create(mock1, Runnable.class).withTimeout(TIMEOUT).withIdentifier("id").build().run();
        });
        assertDurationBelow(GRACE, () -> {
            safeCaller.create(mock2, Runnable.class).withTimeout(TIMEOUT).withIdentifier("id").build().run();
        });
    }

    @Test
    public void testSecondCallGetsAcceptedDifferentIdentifier() throws Exception {
        Runnable mock1 = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock1).run();
        Runnable mock2 = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock2).run();

        assertDurationBetween(TIMEOUT - GRACE, BLOCK - GRACE, () -> {
            safeCaller.create(mock1, Runnable.class).withTimeout(TIMEOUT).withIdentifier(new Object()).build().run();
        });
        assertDurationBetween(TIMEOUT - GRACE, BLOCK - GRACE, () -> {
            safeCaller.create(mock2, Runnable.class).withTimeout(TIMEOUT).withIdentifier(new Object()).build().run();
        });
    }

    @Test
    public void testTimeoutConfiguration() throws Exception {
        Runnable mock = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock).run();

        assertDurationAbove(BLOCK - GRACE, () -> {
            safeCaller.create(mock, Runnable.class).withTimeout(BLOCK + GRACE * 2).onTimeout(mockTimeoutHandler).build()
                    .run();
        });
        verifyNoMoreInteractions(mockTimeoutHandler);
    }

    @Test
    public void testCallWrapped() throws Exception {
        AtomicReference<String> outerThreadName = new AtomicReference<>();
        AtomicReference<String> middleThreadName = new AtomicReference<>();
        AtomicReference<String> innerThreadName = new AtomicReference<>();

        safeCaller.create(new Runnable() {
            @Override
            public void run() {
                outerThreadName.set(Thread.currentThread().getName());
                safeCaller.create((Runnable) () -> {
                }, Runnable.class).build().run();
                safeCaller.create(new Runnable() {
                    @Override
                    public void run() {
                        middleThreadName.set(Thread.currentThread().getName());
                        safeCaller.create((Runnable) () -> {
                        }, Runnable.class).build().run();
                        safeCaller.create(new Runnable() {
                            @Override
                            public void run() {
                                innerThreadName.set(Thread.currentThread().getName());
                                sleep(BLOCK);
                            }

                            @Override
                            public String toString() {
                                return "inner";
                            }
                        }, Runnable.class).build().run();
                    }

                    @Override
                    public String toString() {
                        return "middle";
                    }
                }, Runnable.class).build().run();
            }

            @Override
            public String toString() {
                return "outer";
            }
        }, Runnable.class).withTimeout(TIMEOUT).build().run();
        assertThat(innerThreadName.get(), is(notNullValue()));
        assertThat(middleThreadName.get(), is(notNullValue()));
        assertThat(outerThreadName.get(), is(notNullValue()));
        assertThat(middleThreadName.get(), is(outerThreadName.get()));
        assertThat(innerThreadName.get(), is(outerThreadName.get()));
    }

    @Test
    public void testLambdas() throws Exception {
        ITarget thingHandler = new Target();

        safeCaller.create(() -> {
            thingHandler.method();
            return null;
        }, Callable.class).build().call();

        safeCaller.create(() -> {
            thingHandler.method();
        }, Runnable.class).build().run();

        Object res = safeCaller.create((Function<String, String>) name -> {
            return "Hello " + name + "!";
        }, Function.class).build().apply("World");
        assertThat(res, is("Hello World!"));
    }

    @Test
    public void testAsyncReturnsImmediately() throws Exception {
        Runnable mock1 = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock1).run();
        assertDurationBelow(GRACE, () -> {
            safeCaller.create(mock1, Runnable.class).withTimeout(TIMEOUT).withAsync().build().run();
        });
        waitForAssert(() -> verify(mock1, times(1)).run());
    }

    @Test
    public void testAsyncTimeoutHandler() throws Exception {
        Object identifier = new Object();
        Runnable mock1 = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock1).run();

        assertDurationBelow(GRACE, () -> {
            safeCaller.create(mock1, Runnable.class).withTimeout(TIMEOUT).withAsync().withIdentifier(identifier)
                    .onTimeout(mockTimeoutHandler).onException(mockErrorHandler).build().run();
        });
        waitForAssert(() -> verify(mock1, times(1)).run());
        waitForAssert(() -> verify(mockTimeoutHandler, times(1)).run());
        verifyNoMoreInteractions(mockErrorHandler);
    }

    @Test
    public void testAsyncExceptionHandler() throws Exception {
        Object identifier = new Object();
        Runnable mock1 = mock(Runnable.class);
        doThrow(RuntimeException.class).when(mock1).run();

        assertDurationBelow(GRACE, () -> {
            safeCaller.create(mock1, Runnable.class).withTimeout(TIMEOUT).withAsync().withIdentifier(identifier)
                    .onTimeout(mockTimeoutHandler).onException(mockErrorHandler).build().run();
        });
        waitForAssert(() -> verify(mock1, times(1)).run());
        waitForAssert(() -> verify(mockErrorHandler, times(1)).accept(isA(Exception.class)));
        verifyNoMoreInteractions(mockErrorHandler);
        verifyNoMoreInteractions(mockTimeoutHandler);
    }

    @Test
    public void testAsyncDoesNotTimeoutDifferentIdentifiers() throws Exception {
        Runnable mock1 = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock1).run();
        Runnable mock2 = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock2).run();

        assertDurationBelow(GRACE, () -> {
            safeCaller.create(mock1, Runnable.class).withTimeout(TIMEOUT).withAsync().withIdentifier(new Object())
                    .build().run();
            safeCaller.create(mock2, Runnable.class).withTimeout(TIMEOUT).withAsync().withIdentifier(new Object())
                    .build().run();
        });
        waitForAssert(() -> verify(mock1, times(1)).run());
        waitForAssert(() -> verify(mock2, times(1)).run());
        verifyNoMoreInteractions(mock1, mock2);
    }

    @Test
    public void testAsyncDoesNotTimeoutDefaultIdentifiers() throws Exception {
        Runnable mock1 = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock1).run();
        Runnable mock2 = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock2).run();

        assertDurationBelow(GRACE, () -> {
            safeCaller.create(mock1, Runnable.class).withTimeout(TIMEOUT).withAsync().build().run();
            safeCaller.create(mock2, Runnable.class).withTimeout(TIMEOUT).withAsync().build().run();
        });
        waitForAssert(() -> verify(mock1, times(1)).run());
        waitForAssert(() -> verify(mock2, times(1)).run());
        verifyNoMoreInteractions(mock1, mock2);
    }

    @Test
    public void testAsyncRunsSubsequentAndDoesNotTimeoutSameIdentifier() throws Exception {
        Object identifier = new Object();
        Runnable mock1 = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock1).run();
        Runnable mock2 = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock2).run();

        assertDurationBelow(GRACE, () -> {
            safeCaller.create(mock1, Runnable.class).withAsync().withIdentifier(identifier).withTimeout(BLOCK + TIMEOUT)
                    .build().run();
            safeCaller.create(mock2, Runnable.class).withAsync().withIdentifier(identifier).withTimeout(BLOCK + TIMEOUT)
                    .build().run();
        });
        waitForAssert(() -> verify(mock1, times(1)).run());
        waitForAssert(() -> verify(mock2, times(1)).run());
        verifyNoMoreInteractions(mock1, mock2);
    }

    @Test
    public void testAsyncSequentialSameIdentifier() throws Exception {
        Runnable mock = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock).run();

        for (int i = 0; i < THREAD_POOL_SIZE; i++) {
            assertDurationBelow(GRACE, () -> {
                safeCaller.create(mock, Runnable.class).withTimeout(TIMEOUT).withAsync().build().run();
            });
        }

        assertDurationBetween(BLOCK * (THREAD_POOL_SIZE - 1) - GRACE, (BLOCK + GRACE) * (THREAD_POOL_SIZE), () -> {
            waitForAssert(() -> {
                verify(mock, times(THREAD_POOL_SIZE)).run();
            });
        });
    }

    @Test
    public void testAsyncExceedingThreadPoolDifferentIdentifier() throws Exception {
        Runnable mock = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock).run();

        for (int i = 0; i < THREAD_POOL_SIZE * 2; i++) {
            assertDurationBelow(GRACE, () -> {
                safeCaller.create(mock, Runnable.class).withTimeout(TIMEOUT).withIdentifier(new Object()).withAsync()
                        .build().run();
            });
        }
        assertDurationBetween(BLOCK - GRACE, BLOCK + TIMEOUT + GRACE, () -> {
            waitForAssert(() -> {
                verify(mock, times(THREAD_POOL_SIZE * 2)).run();
            });
        });
    }

    @Test
    public void testAsyncExecutionOrder() throws Exception {
        Queue<Integer> q = new ConcurrentLinkedQueue<>();
        final Random r = new Random();

        for (int i = 0; i < THREAD_POOL_SIZE * 10; i++) {
            final int j = i;
            safeCaller.create(() -> {
                q.add(j);
                sleep(r.nextInt(GRACE));
            }, Runnable.class).withTimeout(TIMEOUT).withAsync().withIdentifier(q).build().run();
        }

        waitForAssert(() -> {
            assertThat(q.size(), is(THREAD_POOL_SIZE * 10));
        });

        int expected = 0;
        for (int actual : q) {
            assertThat(actual, is(expected++));
        }
    }

    @Test
    public void testDuplicateInterface() {
        ITarget target = new DerivedTarget();
        safeCaller.create(target, ITarget.class).build().method();
    }

    private void assertDurationBelow(long high, Runnable runnable) {
        assertDurationBetween(-1, high, runnable);
    }

    private void assertDurationAbove(long low, Runnable runnable) {
        assertDurationBetween(low, -1, runnable);
    }

    private void assertDurationBetween(long low, long high, Runnable runnable) {
        long startNanos = System.nanoTime();
        try {
            runnable.run();
        } finally {
            long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            try {
                if (low > -1) {
                    assertTrue(MessageFormat.format("Duration should have been above {0} but was {1}", low,
                            durationMillis), durationMillis >= low);
                }
                if (high > -1) {
                    assertTrue(MessageFormat.format("Duration should have been below {0} but was {1}", high,
                            durationMillis), durationMillis < high);
                }
            } catch (AssertionError e) {
                logger.debug("{}", createThreadDump(name.getMethodName()));
                throw e;
            }
        }
    }

    private static String createThreadDump(String threadNamePrefix) {
        final StringBuilder sb = new StringBuilder();
        final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        for (ThreadInfo threadInfo : threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), Integer.MAX_VALUE)) {
            if (!threadInfo.getThreadName().startsWith(threadNamePrefix)) {
                continue;
            }
            sb.append("\"");
            sb.append(threadInfo.getThreadName());
            sb.append("\" ");
            sb.append(State.class.getName());
            sb.append(": ");
            sb.append(threadInfo.getThreadState());
            for (final StackTraceElement stackTraceElement : threadInfo.getStackTrace()) {
                sb.append("\n    at ");
                sb.append(stackTraceElement);
            }
        }
        return sb.toString();
    }

    private static Object sleep(int duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            // okay
        }
        return null;
    }

    private void configureSingleThread() {
        safeCaller.modified(new HashMap<String, Object>() {
            private static final long serialVersionUID = 1L;
            {
                put("singleThread", "true");
            }
        });
    }

    /**
     * Executes the given runnable in another thread.
     * <p>
     * Returns immediately.
     *
     * @param runnable
     */
    private void spawn(Runnable runnable) {
        AssertingThread t = new AssertingThread(runnable);
        threads.add(t);
        t.start();
    }

    /**
     * Waits for all the threads which were created by {@link #spawn(Runnable)} in order to finish.
     * <p>
     * This is required in order to catch exceptions and especially {@link AssertionError}s which happened inside.
     *
     * @throws InterruptedException
     */
    private void joinAll() throws InterruptedException {
        while (!threads.isEmpty()) {
            AssertingThread t = threads.remove(0);
            t.join();
            if (t.assertionError != null) {
                throw t.assertionError;
            }
            if (t.runtimeException != null) {
                throw t.runtimeException;
            }
        }
    }

    private static class AssertingThread extends Thread {
        private AssertionError assertionError;
        private RuntimeException runtimeException;

        public AssertingThread(Runnable runnable) {
            super(runnable);
        }

        @Override
        public void run() {
            try {
                super.run();
            } catch (AssertionError e) {
                AssertingThread.this.assertionError = e;
            } catch (RuntimeException e) {
                AssertingThread.this.runtimeException = e;
            }
        }

    }

}
