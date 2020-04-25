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

import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Builder to create a safe-call wrapper for another object.
 *
 * @author Simon Kaufmann - Initial contribution
 *
 * @param <T>
 */
@NonNullByDefault
public interface SafeCallerBuilder<T> {

    /**
     * Creates a dynamic proxy with the according properties which guards the caller from hanging implementations in the
     * target object.
     *
     * @return the dynamic proxy wrapping the target object
     */
    T build();

    /**
     * Sets the timeout
     *
     * @param timeout the timeout in milliseconds.
     * @return the SafeCallerBuilder itself
     */
    SafeCallerBuilder<T> withTimeout(long timeout);

    /**
     * Specifies the identifier for the context in which only one thread may be occupied at the same time.
     *
     * @param identifier the identifier much must have a proper hashcode()/equals() implementation in order to
     *            distinguish different contexts.
     * @return the SafeCallerBuilder itself
     */
    SafeCallerBuilder<T> withIdentifier(Object identifier);

    /**
     * Specifies a callback in case of execution errors.
     *
     * @param exceptionHandler
     * @return the SafeCallerBuilder itself
     */
    SafeCallerBuilder<T> onException(Consumer<Throwable> exceptionHandler);

    /**
     * Specifies a callback in case of timeouts.
     *
     * @param timeoutHandler
     * @return the SafeCallerBuilder itself
     */
    SafeCallerBuilder<T> onTimeout(Runnable timeoutHandler);

    /**
     * Denotes that the calls should be executed asynchronously, i.e. that they should return immediately and not even
     * block until they reached the timeout.
     * <p>
     * By default, calls will be executed synchronously (i.e. blocking) until the timeout is reached.
     *
     * @return the SafeCallerBuilder itself
     */
    SafeCallerBuilder<T> withAsync();
}
