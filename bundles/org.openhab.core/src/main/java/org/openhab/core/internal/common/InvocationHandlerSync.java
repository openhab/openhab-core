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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Synchronous invocation handler implementation.
 *
 * @author Simon Kaufmann - Initial contribution
 *
 * @param <T>
 */
@NonNullByDefault
public class InvocationHandlerSync<T> extends AbstractInvocationHandler<T> implements InvocationHandler {

    private static final String MSG_CONTEXT = "Already in a safe-call context, executing '{}' directly on '{}'.";

    private final Logger logger = LoggerFactory.getLogger(InvocationHandlerSync.class);

    public InvocationHandlerSync(SafeCallManager manager, T target, Object identifier, long timeout,
            @Nullable Consumer<Throwable> exceptionHandler, @Nullable Runnable timeoutHandler) {
        super(manager, target, identifier, timeout, exceptionHandler, timeoutHandler);
    }

    @Override
    public @Nullable Object invoke(@Nullable Object proxy, @Nullable Method method, Object @Nullable [] args)
            throws Throwable {
        if (method != null) {
            Invocation invocation = new Invocation(this, method, args);
            Invocation activeInvocation = getManager().getActiveInvocation();
            if (activeInvocation != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug(MSG_CONTEXT, toString(method), getTarget());
                }
                try {
                    activeInvocation.getInvocationStack().push(invocation);
                    return invokeDirect(invocation);
                } finally {
                    activeInvocation.getInvocationStack().poll();
                }
            }
            try {
                Future<Object> future = getManager().getScheduler().submit(invocation);
                return future.get(getTimeout(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                handleTimeout(method, invocation);
            } catch (ExecutionException e) {
                handleExecutionException(method, e);
            }
        }
        return null;
    }

}
