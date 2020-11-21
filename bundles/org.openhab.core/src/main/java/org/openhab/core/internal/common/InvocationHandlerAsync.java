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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Asynchronuous {@link InvocationHandler} implementation.
 *
 * Instead of directly invoking the called method it rather queues it in the {@link SafeCallManager} for asynchronous
 * execution.
 *
 * @author Simon Kaufmann - Initial contribution
 *
 * @param <T>
 */
@NonNullByDefault
class InvocationHandlerAsync<T> extends AbstractInvocationHandler<T> implements InvocationHandler {

    InvocationHandlerAsync(SafeCallManager manager, T target, Object identifier, long timeout,
            @Nullable Consumer<Throwable> exceptionHandler, @Nullable Runnable timeoutHandler) {
        super(manager, target, identifier, timeout, exceptionHandler, timeoutHandler);
    }

    @Override
    public @Nullable Object invoke(Object proxy, @Nullable Method method, @Nullable Object @Nullable [] args)
            throws Throwable {
        if (method != null) {
            try {
                getManager().enqueue(new Invocation(this, method, args));
            } catch (DuplicateExecutionException e) {
                handleDuplicate(method, e);
            }
        }
        return null;
    }
}
