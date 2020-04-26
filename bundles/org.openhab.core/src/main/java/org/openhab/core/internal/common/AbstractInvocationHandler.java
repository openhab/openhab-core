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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common base class for synchronous and ansynchronous invocation handlers.
 *
 * @author Simon Kaufmann - Initial contribution
 *
 * @param <T>
 */
@NonNullByDefault
abstract class AbstractInvocationHandler<T> {

    private static final String MSG_TIMEOUT_R = "Timeout of {}ms exceeded while calling\n{}\nThread '{}' ({}) is in state '{}'\n{}";
    private static final String MSG_TIMEOUT_Q = "Timeout of {}ms exceeded while calling\n{}\nThe task was still queued.";
    private static final String MSG_DUPLICATE = "Thread occupied while calling method '{}' on '{}' because of another blocking call.\n\tThe other call was to '{}'.\n\tIt's thread '{}' ({}) is in state '{}'\n{}";
    private static final String MSG_ERROR = "An error occurred while calling method '{}' on '{}': {}";

    private final Logger logger = LoggerFactory.getLogger(AbstractInvocationHandler.class);

    private final SafeCallManager manager;
    private final T target;
    private final Object identifier;
    private final long timeout;

    private final @Nullable Consumer<Throwable> exceptionHandler;
    private final @Nullable Runnable timeoutHandler;

    AbstractInvocationHandler(SafeCallManager manager, T target, Object identifier, long timeout,
            @Nullable Consumer<Throwable> exceptionHandler, @Nullable Runnable timeoutHandler) {
        this.manager = manager;
        this.target = target;
        this.identifier = identifier;
        this.timeout = timeout;
        this.exceptionHandler = exceptionHandler;
        this.timeoutHandler = timeoutHandler;
    }

    SafeCallManager getManager() {
        return manager;
    }

    T getTarget() {
        return target;
    }

    Object getIdentifier() {
        return identifier;
    }

    long getTimeout() {
        return timeout;
    }

    @Nullable
    Consumer<Throwable> getExceptionHandler() {
        return exceptionHandler;
    }

    @Nullable
    Runnable getTimeoutHandler() {
        return timeoutHandler;
    }

    void handleExecutionException(Method method, ExecutionException e) {
        if (e.getCause() instanceof DuplicateExecutionException) {
            handleDuplicate(method, (DuplicateExecutionException) e.getCause());
        } else if (e.getCause() instanceof InvocationTargetException) {
            handleException(method, (InvocationTargetException) e.getCause());
        }
    }

    void handleException(Method method, InvocationTargetException e) {
        logger.error(MSG_ERROR, toString(method), target, e.getCause().getMessage(), e.getCause());
        if (exceptionHandler != null) {
            exceptionHandler.accept(e.getCause());
        }
    }

    void handleDuplicate(Method method, DuplicateExecutionException e) {
        Thread thread = e.getCallable().getThread();
        logger.debug(MSG_DUPLICATE, toString(method), target, toString(e.getCallable().getMethod()), thread.getName(),
                thread.getId(), thread.getState().toString(), getStacktrace(thread));
    }

    void handleTimeout(Method method, Invocation invocation) {
        final Thread thread = invocation.getThread();
        if (thread != null) {
            logger.debug(MSG_TIMEOUT_R, timeout, toString(invocation.getInvocationStack()), thread.getName(),
                    thread.getId(), thread.getState().toString(), getStacktrace(thread));
        } else {
            logger.debug(MSG_TIMEOUT_Q, timeout, toString(invocation.getInvocationStack()));
        }
        if (timeoutHandler != null) {
            timeoutHandler.run();
        }
    }

    private String toString(Collection<Invocation> invocationStack) {
        return invocationStack.stream().map(invocation -> "\t'" + toString(invocation.getMethod()) + "' on '"
                + invocation.getInvocationHandler().getTarget() + "'").collect(Collectors.joining(" via\n"));
    }

    private String getStacktrace(final Thread thread) {
        StackTraceElement[] elements = AccessController.doPrivileged(new PrivilegedAction<StackTraceElement[]>() {
            @Override
            public StackTraceElement[] run() {
                return thread.getStackTrace();
            }
        });
        return Arrays.stream(elements).map(element -> "\tat " + element.toString()).collect(Collectors.joining("\n"));
    }

    String toString(Method method) {
        return method.getDeclaringClass().getSimpleName() + "." + method.getName() + "()";
    }

    @Nullable
    Object invokeDirect(Invocation invocation) throws IllegalAccessException, IllegalArgumentException {
        try {
            manager.recordCallStart(invocation);
        } catch (DuplicateExecutionException e) {
            return null;
        }
        try {
            return invocation.getMethod().invoke(target, invocation.getArgs());
        } catch (InvocationTargetException e) {
            handleException(invocation.getMethod(), e);
            return null;
        } finally {
            manager.recordCallEnd(invocation);
        }
    }
}
