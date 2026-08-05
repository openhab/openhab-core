/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.concurrent.locks.Lock;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Static utility class for Hamcrest Matchers.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class Matchers {

    /**
     * Not to be instantiated.
     */
    private Matchers() {
    }

    /**
     * Creates a matcher that polls until the provided matcher succeeds or timeout expires.
     * <p>
     * The matcher will repeatedly evaluate the provided value against the delegate matcher, sleeping 50ms between
     * attempts, until either the matcher succeeds or the timeout is exceeded.
     * <p>
     * Example:
     *
     * <pre>
     * assertThat(collection, waitUntil(hasSize(3), 5000));
     * </pre>
     *
     * @param <T> the type of the value being matched.
     * @param matcher the delegate {@link Matcher} to evaluate repeatedly.
     * @param timeoutMs the maximum time to poll, in milliseconds.
     * @return A {@link Matcher} that polls the value until it matches or timeout expires.
     */
    public static <T> Matcher<T> waitUntil(Matcher<T> matcher, long timeoutMs) {
        return new WaitUntil<>(matcher, timeoutMs);
    }

    /**
     * Creates a matcher that extracts a property from the value and polls until the provided matcher succeeds on that
     * property or timeout expires.
     * <p>
     * The matcher will repeatedly extract the named property from the provided object and evaluate it against the
     * delegate matcher, sleeping between attempts, until either the matcher succeeds or the timeout is exceeded.
     * <p>
     * The property is resolved by attempting the following in order:
     * <ol>
     * <li>A parameterless method with the given name</li>
     * <li>A getter method named "get" + capitalized property name</li>
     * <li>A public field with the given name</li>
     * <li>A private/protected field with the given name</li>
     * </ol>
     * <p>
     * Example:
     *
     * <pre>
     * assertThat(myObject, waitUntil("status", equalTo("READY"), 5000));
     * </pre>
     *
     * @param <T> the type of the property value being matched.
     * @param property the name of the property to extract from the object.
     * @param matcher the delegate {@link Matcher} to evaluate repeatedly against the property.
     * @param timeoutMs the maximum time to poll, in milliseconds.
     * @return A {@link Matcher} that polls the property value until it matches or timeout expires.
     * @throws RuntimeException If the property cannot be found or accessed on the value.
     */
    @SuppressWarnings("unchecked")
    public static <T> Matcher<Object> waitUntil(String property, Matcher<T> matcher, long timeoutMs) {
        return (Matcher<Object>) new WaitUntil<>(property, matcher, timeoutMs);
    }

    /**
     * A matcher that evaluates another matcher while holding a lock using the {@code synchronized} keyword.
     * <p>
     * This is useful for testing asynchronous code where a collection or other shared state is being modified by a
     * background thread. By acquiring the lock only during the actual evaluation, the matcher allows the background
     * thread to acquire the lock and make changes between polling attempts.
     *
     * @param <T> the type of the value being matched.
     * @param synchronizationObject the object to synchronize on during evaluation.
     * @param matcher the delegate {@link Matcher} to evaluate while holding the lock.
     * @return A {@link Matcher} that acquires the lock during evaluation.
     */
    public static <T> Matcher<T> synchronizedMatcher(Object synchronizationObject, Matcher<T> matcher) {
        return new SynchronizedMatcher<>(synchronizationObject, matcher);
    }

    /**
     * A matcher that evaluates another matcher while holding a {@link Lock}.
     * <p>
     * This is useful for testing asynchronous code where a collection or other shared state is being modified by a
     * background thread. By acquiring the lock only during the actual evaluation, the matcher allows the background
     * thread to acquire the lock and make changes between polling attempts.
     * <p>
     * The lock is acquired before evaluation and released in a finally block to ensure it is always released, even if
     * the delegate matcher throws an exception.
     *
     * @param <T> the type of the value being matched.
     * @param lock the {@link Lock} to acquire during evaluation.
     * @param matcher the delegate {@link Matcher} to evaluate while holding the lock.
     * @return A {@link Matcher} that acquires the lock during evaluation.
     */
    public static <T> Matcher<T> lockingMatcher(Lock lock, Matcher<T> matcher) {
        return new LockingMatcher<>(lock, matcher);
    }

    /**
     * A {@link Matcher} that polls a value until it matches the delegate {@link Matcher} or a timeout expires.
     * <p>
     * This {@link Matcher} is designed for testing asynchronous code. It repeatedly evaluates the delegate
     * {@link Matcher} against the provided value, sleeping briefly between attempts, until either the {@link Matcher}
     * succeeds or the timeout is exceeded.
     * <p>
     * Two constructors are provided:
     * <ul>
     * <li>Direct matching: evaluates the provided value directly against the {@link Matcher}</li>
     * <li>Property extraction: extracts a property from the provided object and evaluates it</li>
     * </ul>
     * <p>
     * Property extraction supports methods (with or without "get" prefix), public fields, and private/protected fields
     * (via reflection).
     * <p>
     * If the polling thread is interrupted, the matcher returns {@code false} and restores the interrupt status.
     * <p>
     * Example usage:
     *
     * <pre>
     * // Direct matching
     * assertThat(collection, new WaitUntil<>(hasSize(3), 5000L));
     *
     * // Property extraction
     * assertThat(myObject, new WaitUntil<>("status", equalTo("READY"), 5000L));
     * </pre>
     *
     * @param <T> the type of the value being matched.
     */
    public static class WaitUntil<T> extends BaseMatcher<T> {

        private final Matcher<T> matcher;
        private final long timeoutMs;
        private final @Nullable String propertyName;
        private @Nullable Object lastValue;

        /**
         * Create a {@link Matcher} that polls until the provided {@link Matcher} succeeds or timeout expires.
         *
         * @param matcher the delegate {@link Matcher} to evaluate repeatedly.
         * @param timeoutMs the maximum time to poll, in milliseconds.
         */
        public WaitUntil(Matcher<T> matcher, long timeoutMs) {
            this.matcher = matcher;
            this.timeoutMs = timeoutMs;
            this.propertyName = null;
        }

        /**
         * Create a {@link Matcher} that extracts a property from the value and polls until the provided
         * {@link Matcher} succeeds on that property or timeout expires.
         * <p>
         * The property is resolved by attempting the following in order:
         * <ol>
         * <li>A parameterless method with the given name</li>
         * <li>A getter method named "get" + capitalized property name</li>
         * <li>A public field with the given name</li>
         * <li>A private/protected field with the given name</li>
         * </ol>
         *
         * @param propertyName the name of the property to extract from the value.
         * @param matcher the delegate {@link Matcher} to evaluate repeatedly against the property.
         * @param timeoutMs the maximum time to poll, in milliseconds.
         * @throws RuntimeException if the property cannot be found or accessed on the value.
         */
        public WaitUntil(String propertyName, Matcher<T> matcher, long timeoutMs) {
            this.matcher = matcher;
            this.timeoutMs = timeoutMs;
            this.propertyName = propertyName;
        }

        @Override
        public boolean matches(@Nullable Object actual) {
            long deadline = System.currentTimeMillis() + timeoutMs;
            Object valueToEvaluate = null;
            String name;
            while (System.currentTimeMillis() < deadline) {
                valueToEvaluate = actual;
                if (actual != null && (name = propertyName) != null) {
                    valueToEvaluate = extractPropertyValue(actual, name);
                }

                if (matcher.matches(valueToEvaluate)) {
                    return true;
                }

                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    lastValue = valueToEvaluate;
                    return false;
                }
            }
            lastValue = valueToEvaluate;
            return false;
        }

        private @Nullable Object extractPropertyValue(Object obj, String property) {
            try {
                try {
                    Method method = obj.getClass().getMethod(property);
                    return method.invoke(obj);
                } catch (NoSuchMethodException e) {
                    try {
                        String getterName = "get" + property.substring(0, 1).toUpperCase(Locale.ROOT)
                                + property.substring(1);
                        Method method = obj.getClass().getMethod(getterName);
                        return method.invoke(obj);
                    } catch (NoSuchMethodException e2) {
                        try {
                            Field field = obj.getClass().getField(property);
                            return field.get(obj);
                        } catch (NoSuchFieldException e3) {
                            Field field = obj.getClass().getDeclaredField(property);
                            field.setAccessible(true);
                            return field.get(obj);
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(
                        "waitUntil could not extract method or field '" + property + "' from " + obj.getClass(), e);
            }
        }

        @Override
        public void describeTo(@Nullable Description description) {
            if (description != null) {
                description.appendText("waitUntil ").appendDescriptionOf(matcher);
            }
        }

        @Override
        public void describeMismatch(@Nullable Object item, @Nullable Description mismatchDescription) {
            if (mismatchDescription != null) {
                mismatchDescription.appendText("the last evaluated value was ").appendValue(lastValue);
            }
        }
    }

    /**
     * A {@link Matcher} that evaluates another {@link Matcher} while holding a lock using the {@code synchronized}
     * keyword.
     * <p>
     * This is useful for testing asynchronous code where a collection or other shared state is being modified by a
     * background thread. By acquiring the lock only during the actual evaluation, the matcher allows the background
     * thread to acquire the lock and make changes between polling attempts.
     *
     * @param <T> the type of the value being matched.
     */
    public static class SynchronizedMatcher<T> extends TypeSafeMatcher<T> {
        private final Object synchronizationObject;
        private final Matcher<T> matcher;

        /**
         * Create a {@link Matcher} that evaluates another {@link Matcher} while holding a lock using the
         * {@code synchronized} keyword.
         *
         * @param synchronizationObject the object to synchronize on during evaluation.
         * @param matcher the delegate {@link Matcher} to evaluate while holding the lock.
         */
        public SynchronizedMatcher(Object synchronizationObject, Matcher<T> matcher) {
            this.synchronizationObject = synchronizationObject;
            this.matcher = matcher;
        }

        @Override
        protected boolean matchesSafely(T item) {
            synchronized (synchronizationObject) {
                return matcher.matches(item);
            }
        }

        @Override
        public void describeTo(@Nullable Description description) {
            if (description != null) {
                description.appendText("synchronized matcher: ");
                matcher.describeTo(description);
            }
        }
    }

    /**
     * A {@link Matcher} that evaluates another {@link Matcher} while holding a {@link Lock}.
     * <p>
     * This is useful for testing asynchronous code where a collection or other shared state is being modified by a
     * background thread. By acquiring the lock only during the actual evaluation, the matcher allows the background
     * thread to acquire the lock and make changes between polling attempts.
     * <p>
     * The lock is acquired before evaluation and released in a finally block to ensure it is always released, even if
     * the delegate matcher throws an exception.
     *
     * @param <T> the type of the value being matched.
     */
    public static class LockingMatcher<T> extends TypeSafeMatcher<T> {
        private final Lock lock;
        private final Matcher<T> matcher;

        /**
         * Create a {@link Matcher} that evaluates another {@link Matcher} while holding a {@link Lock}.
         *
         * @param lock the {@link Lock} to acquire during evaluation.
         * @param matcher the delegate {@link Matcher} to evaluate while holding the lock.
         */
        public LockingMatcher(Lock lock, Matcher<T> matcher) {
            this.lock = lock;
            this.matcher = matcher;
        }

        @Override
        protected boolean matchesSafely(T item) {
            lock.lock();
            try {
                return matcher.matches(item);
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void describeTo(@Nullable Description description) {
            if (description != null) {
                description.appendText("locking matcher: ");
                matcher.describeTo(description);
            }
        }
    }
}
