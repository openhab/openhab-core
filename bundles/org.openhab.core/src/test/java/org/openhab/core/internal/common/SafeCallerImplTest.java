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
package org.openhab.core.internal.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

import java.lang.Thread.State;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.JavaTest;
import org.openhab.core.common.QueueingThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Simon Kaufmann - Initial contribution and API.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class SafeCallerImplTest extends JavaTest {

    private static final int THREAD_POOL_SIZE = 3;

    // the duration that the called object should block for
    private static final int BLOCK = 5000;

    // the standard timeout for the safe-caller used in most tests
    private static final int TIMEOUT = 500;

    // the grace period allowed for processing before a timing assertion should fail
    private static final int GRACE = 300;

    private final Logger logger = LoggerFactory.getLogger(SafeCallerImplTest.class);

    private final List<AssertingThread> threads = new LinkedList<>();

    private @NonNullByDefault({}) SafeCallerImpl safeCaller;
    private @NonNullByDefault({}) QueueingThreadPoolExecutor scheduler;
    private @NonNullByDefault({}) TestInfo testInfo;

    private @NonNullByDefault({}) @Mock Runnable timeoutHandlerMock;
    private @NonNullByDefault({}) @Mock Consumer<Throwable> errorHandlerMock;

    @FunctionalInterface
    public interface ITarget {
        String method();
    }

    public static class Target implements ITarget {
        @Override
        public String method() {
            return "Hello";
        }
    }

    public static class DerivedTarget extends Target implements ITarget {
    }

    @BeforeEach
    public void setup(TestInfo testInfo) {
        this.testInfo = testInfo;

        scheduler = QueueingThreadPoolExecutor.createInstance(testInfo.getTestMethod().get().getName(),
                THREAD_POOL_SIZE);
        safeCaller = new SafeCallerImpl(null) {
            @Override
            protected ExecutorService getScheduler() {
                return scheduler;
            }
        };

        assertThat(BLOCK, is(greaterThan(TIMEOUT + GRACE)));
        assertThat(GRACE, is(lessThan(TIMEOUT)));
    }

    @AfterEach
    public void afterEach() throws Exception {
        // ensure all "inner" assertion errors are heard
        joinAll();

        scheduler.shutdownNow();
        safeCaller.deactivate();
    }

    @Test
    public void testSimpleCall() {
        Target target = new Target();
        String result = safeCaller.create(target, ITarget.class).build().method();
        assertThat(result, is("Hello"));
    }

    @Test
    public void testInterfaceDetection() {
        ITarget target = new Target();
        String result = safeCaller.create(target, ITarget.class).build().method();
        assertThat(result, is("Hello"));
    }

    @Test
    public void testExceptionHandler() {
        Runnable mock = mock(Runnable.class);
        doThrow(RuntimeException.class).when(mock).run();

        safeCaller.create(mock, Runnable.class).onException(errorHandlerMock).build().run();
        waitForAssert(() -> verify(errorHandlerMock).accept(isA(Throwable.class)));
    }

    @Test
    public void testTimeoutHandler() {
        Runnable mock = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock).run();

        safeCaller.create(mock, Runnable.class).withTimeout(TIMEOUT).onTimeout(timeoutHandlerMock).build().run();
        waitForAssert(() -> verify(timeoutHandlerMock).run());
    }

    @Test
    public void testTimeoutReturnsEarly() {
        Runnable mock = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock).run();

        assertDurationBetween(TIMEOUT - GRACE, BLOCK - GRACE,
                () -> safeCaller.create(mock, Runnable.class).withTimeout(TIMEOUT).build().run());
    }

    @Test
    public void testMultiThreadSync() {
        Runnable mock = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock).run();

        spawn(() -> assertDurationBetween(TIMEOUT - GRACE, BLOCK - GRACE,
                () -> safeCaller.create(mock, Runnable.class).withTimeout(TIMEOUT).build().run()));
        sleep(GRACE); // give it a chance to start
        spawn(() -> assertDurationBetween(TIMEOUT - GRACE, BLOCK - GRACE,
                () -> safeCaller.create(mock, Runnable.class).withTimeout(TIMEOUT).build().run()));
        waitForAssert(() -> verify(mock, times(2)).run());
    }

    @Test
    public void testSingleThreadSyncSecondCallWhileInTimeout() {
        Runnable mock = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock).run();
        configureSingleThread();

        assertDurationBetween(TIMEOUT - GRACE, BLOCK - GRACE,
                () -> safeCaller.create(mock, Runnable.class).withTimeout(TIMEOUT).build().run());
        assertDurationBelow(2 * GRACE,
                () -> safeCaller.create(mock, Runnable.class).withTimeout(TIMEOUT).build().run());
        assertDurationBetween(TIMEOUT - GRACE, BLOCK + GRACE, () -> waitForAssert(() -> verify(mock, times(2)).run()));
    }

    @Test
    public void testSingleThreadSyncParallel() {
        Runnable mock = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock).run();
        configureSingleThread();

        spawn(() -> assertDurationBetween(0, BLOCK - GRACE,
                () -> safeCaller.create(mock, Runnable.class).withTimeout(TIMEOUT).build().run()));
        sleep(GRACE); // give it a chance to start
        spawn(() -> assertDurationBelow(3 * GRACE,
                () -> safeCaller.create(mock, Runnable.class).withTimeout(TIMEOUT).build().run()));
        assertDurationBetween(BLOCK - 2 * GRACE, BLOCK + TIMEOUT + 4 * GRACE,
                () -> waitForAssert(() -> verify(mock, times(2)).run()));
    }

    @Test
    public void testMultiThreadAsync() {
        Runnable mock = mock(Runnable.class);
        doAnswer(a -> sleep(TIMEOUT)).when(mock).run();

        assertDurationBelow(GRACE,
                () -> safeCaller.create(mock, Runnable.class).withTimeout(TIMEOUT).withAsync().build().run());
        assertDurationBelow(GRACE,
                () -> safeCaller.create(mock, Runnable.class).withTimeout(TIMEOUT).withAsync().build().run());
        waitForAssert(() -> verify(mock, times(2)).run());
    }

    @Test
    public void testSingleThreadAsync() {
        Runnable mock = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock).run();
        configureSingleThread();

        assertDurationBelow(GRACE,
                () -> safeCaller.create(mock, Runnable.class).withTimeout(TIMEOUT).withAsync().build().run());
        assertDurationBelow(GRACE,
                () -> safeCaller.create(mock, Runnable.class).withTimeout(TIMEOUT).withAsync().build().run());
        waitForAssert(() -> verify(mock, times(2)).run());
    }

    @Test
    public void testSecondCallGetsRefusedSameIdentifier() {
        Runnable mock1 = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock1).run();
        Runnable mock2 = mock(Runnable.class);

        assertDurationBetween(TIMEOUT - GRACE, BLOCK - GRACE,
                () -> safeCaller.create(mock1, Runnable.class).withTimeout(TIMEOUT).withIdentifier("id").build().run());
        assertDurationBelow(4 * GRACE,
                () -> safeCaller.create(mock2, Runnable.class).withTimeout(TIMEOUT).withIdentifier("id").build().run());
    }

    @Test
    public void testSecondCallGetsAcceptedDifferentIdentifier() {
        Runnable mock1 = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock1).run();
        Runnable mock2 = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock2).run();

        assertDurationBetween(TIMEOUT - GRACE, BLOCK - GRACE, () -> safeCaller.create(mock1, Runnable.class)
                .withTimeout(TIMEOUT).withIdentifier(new Object()).build().run());
        assertDurationBetween(TIMEOUT - GRACE, BLOCK - GRACE, () -> safeCaller.create(mock2, Runnable.class)
                .withTimeout(TIMEOUT).withIdentifier(new Object()).build().run());
    }

    @Test
    public void testTimeoutConfiguration() {
        Runnable mock = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock).run();

        assertDurationAbove(BLOCK - GRACE, () -> safeCaller.create(mock, Runnable.class).withTimeout(BLOCK + GRACE * 2)
                .onTimeout(timeoutHandlerMock).build().run());
        verifyNoMoreInteractions(timeoutHandlerMock);
    }

    @Test
    public void testCallWrapped() {
        AtomicReference<String> outerThreadName = new AtomicReference<>();
        AtomicReference<String> middleThreadName = new AtomicReference<>();
        AtomicReference<String> innerThreadName = new AtomicReference<>();

        safeCaller.create(new Runnable() {
            @Override
            public void run() {
                outerThreadName.set(Thread.currentThread().getName());
                safeCaller.create(() -> {
                }, Runnable.class).build().run();
                safeCaller.create(new Runnable() {
                    @Override
                    public void run() {
                        middleThreadName.set(Thread.currentThread().getName());
                        safeCaller.create(() -> {
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
    @SuppressWarnings("unchecked")
    public void testLambdas() throws Exception {
        ITarget thingHandler = new Target();

        safeCaller.create(() -> {
            thingHandler.method();
            return null;
        }, Callable.class).build().call();

        safeCaller.create(thingHandler::method, Runnable.class).build().run();

        Object res = safeCaller.create((Function<String, String>) name -> ("Hello " + name + "!"), Function.class)
                .build().apply("World");
        assertThat(res, is("Hello World!"));
    }

    @Test
    public void testAsyncReturnsImmediately() {
        Runnable mock1 = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock1).run();
        assertDurationBelow(GRACE,
                () -> safeCaller.create(mock1, Runnable.class).withTimeout(TIMEOUT).withAsync().build().run());
        waitForAssert(() -> verify(mock1, timeout(1000).times(1)).run());
    }

    @Test
    public void testAsyncTimeoutHandler() {
        Object identifier = new Object();
        Runnable mock1 = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock1).run();

        assertDurationBelow(GRACE, () -> safeCaller.create(mock1, Runnable.class).withTimeout(TIMEOUT).withAsync()
                .withIdentifier(identifier).onTimeout(timeoutHandlerMock).onException(errorHandlerMock).build().run());
        waitForAssert(() -> verify(mock1, times(1)).run());
        waitForAssert(() -> verify(timeoutHandlerMock, times(1)).run());
        verifyNoMoreInteractions(errorHandlerMock);
    }

    @Test
    public void testAsyncExceptionHandler() {
        Object identifier = new Object();
        Runnable mock1 = mock(Runnable.class);
        doThrow(RuntimeException.class).when(mock1).run();

        assertDurationBelow(2 * GRACE, () -> safeCaller.create(mock1, Runnable.class).withTimeout(TIMEOUT).withAsync()
                .withIdentifier(identifier).onTimeout(timeoutHandlerMock).onException(errorHandlerMock).build().run());
        waitForAssert(() -> verify(mock1, times(1)).run());
        waitForAssert(() -> verify(errorHandlerMock, times(1)).accept(isA(Exception.class)));
        verifyNoMoreInteractions(errorHandlerMock);
        verifyNoMoreInteractions(timeoutHandlerMock);
    }

    @Test
    public void testAsyncDoesNotTimeoutDifferentIdentifiers() {
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
        sleep(GRACE);
        verifyNoMoreInteractions(mock1, mock2);
    }

    @Test
    public void testAsyncDoesNotTimeoutDefaultIdentifiers() {
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
    public void testAsyncRunsSubsequentAndDoesNotTimeoutSameIdentifier() {
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
    public void testAsyncSequentialSameIdentifier() {
        Runnable mock = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock).run();

        for (int i = 0; i < THREAD_POOL_SIZE; i++) {
            assertDurationBelow(GRACE,
                    () -> safeCaller.create(mock, Runnable.class).withTimeout(TIMEOUT).withAsync().build().run());
        }

        assertDurationBetween(BLOCK * (THREAD_POOL_SIZE - 1) - GRACE, (BLOCK + GRACE) * (THREAD_POOL_SIZE),
                () -> waitForAssert(() -> verify(mock, times(THREAD_POOL_SIZE)).run()));
    }

    @Test
    public void testAsyncExceedingThreadPoolDifferentIdentifier() {
        Runnable mock = mock(Runnable.class);
        doAnswer(a -> sleep(BLOCK)).when(mock).run();

        for (int i = 0; i < THREAD_POOL_SIZE * 2; i++) {
            assertDurationBelow(GRACE, () -> safeCaller.create(mock, Runnable.class).withTimeout(TIMEOUT)
                    .withIdentifier(new Object()).withAsync().build().run());
        }
        assertDurationBetween(BLOCK - GRACE, BLOCK + TIMEOUT + THREAD_POOL_SIZE * 2 * GRACE,
                () -> waitForAssert(() -> verify(mock, times(THREAD_POOL_SIZE * 2)).run()));
    }

    @Test
    public void testAsyncExecutionOrder() {
        Queue<Integer> q = new ConcurrentLinkedQueue<>();
        final Random r = new Random();

        for (int i = 0; i < THREAD_POOL_SIZE * 10; i++) {
            final int j = i;
            safeCaller.create(() -> {
                q.add(j);
                sleep(r.nextInt(GRACE));
            }, Runnable.class).withTimeout(TIMEOUT).withAsync().withIdentifier(q).build().run();
        }

        waitForAssert(() -> assertThat(q.size(), is(THREAD_POOL_SIZE * 10)));

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
        Instant start = Instant.now();
        try {
            runnable.run();
        } finally {
            Duration duration = Duration.between(start, Instant.now());
            assertDurationBetween(low, high, duration.toMillis());
        }
    }

    private void assertDurationBetween(long low, long high, long durationMillis) throws AssertionError {
        try {
            if (low > -1) {
                assertTrue(durationMillis >= low,
                        MessageFormat.format("Duration should have been above {0} but was {1}", low, durationMillis));
            }
            if (high > -1) {
                assertTrue(durationMillis < high,
                        MessageFormat.format("Duration should have been below {0} but was {1}", high, durationMillis));
            }
        } catch (AssertionError e) {
            logger.debug("{}", createThreadDump(testInfo.getTestMethod().get().getName()));
            throw e;
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

    private static @Nullable Object sleep(int duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            // okay
        }
        return null;
    }

    private void configureSingleThread() {
        safeCaller.modified(new HashMap<>() {
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
     * @param runnable The runnable to execute
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
     * @throws InterruptedException if interrupted
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
        private @Nullable AssertionError assertionError;
        private @Nullable RuntimeException runtimeException;

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
