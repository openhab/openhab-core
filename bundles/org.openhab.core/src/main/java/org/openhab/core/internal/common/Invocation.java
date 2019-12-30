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

import java.lang.reflect.Method;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.Callable;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Represents a call to the dynamic proxy which wraps a {@link Callable} and tracks the executing thread.
 *
 * @author Simon Kaufmann - Initial contribution
 */
class Invocation implements Callable<Object> {

    private final Method method;
    private final @Nullable Object @Nullable [] args;
    private final AbstractInvocationHandler<?> invocationHandler;
    private final Deque<Invocation> invocationStack = new LinkedList<>();

    @Nullable
    private Thread thread;

    Invocation(AbstractInvocationHandler<?> invocationHandler, Method method, @Nullable Object @Nullable [] args) {
        this.method = method;
        this.args = args;
        this.invocationHandler = invocationHandler;
        this.invocationStack.push(this);
    }

    @Nullable
    Thread getThread() {
        return thread;
    }

    @Override
    public Object call() throws Exception {
        thread = Thread.currentThread();
        return invocationHandler.invokeDirect(this);
    }

    Method getMethod() {
        return method;
    }

    Object[] getArgs() {
        return args;
    }

    long getTimeout() {
        return invocationHandler.getTimeout();
    }

    Object getIdentifier() {
        return invocationHandler.getIdentifier();
    }

    AbstractInvocationHandler<?> getInvocationHandler() {
        return invocationHandler;
    }

    @Override
    public String toString() {
        return "invocation of '" + method.getName() + "()' on '" + invocationHandler.getTarget() + "'";
    }

    Deque<Invocation> getInvocationStack() {
        return invocationStack;
    }

}
