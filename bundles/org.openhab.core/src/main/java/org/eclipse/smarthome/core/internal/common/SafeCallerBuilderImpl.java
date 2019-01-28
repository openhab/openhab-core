/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.internal.common;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.common.SafeCaller;
import org.eclipse.smarthome.core.common.SafeCallerBuilder;

/**
 * Builder implementation to create safe-call wrapper objects.
 *
 * @author Simon Kaufmann - initial contribution and API.
 *
 * @param <T>
 */
@NonNullByDefault
public class SafeCallerBuilderImpl<T> implements SafeCallerBuilder<T> {

    private final T target;
    private final Class<?>[] interfaceTypes;
    private long timeout;
    private Object identifier;
    @Nullable
    private Consumer<Throwable> exceptionHandler;
    @Nullable
    private Runnable timeoutHandler;
    private boolean async;
    private final SafeCallManager manager;

    public SafeCallerBuilderImpl(T target, Class<?>[] classes, SafeCallManager manager) {
        this.target = target;
        this.interfaceTypes = classes;
        this.manager = manager;
        this.timeout = SafeCaller.DEFAULT_TIMEOUT;
        this.identifier = target;
        this.async = false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T build() {
        return AccessController.doPrivileged((PrivilegedAction<T>) () -> {
            InvocationHandler handler;
            if (async) {
                handler = new InvocationHandlerAsync<T>(manager, target, identifier, timeout, exceptionHandler,
                        timeoutHandler);
            } else {
                handler = new InvocationHandlerSync<T>(manager, target, identifier, timeout, exceptionHandler,
                        timeoutHandler);
            }
            return (T) Proxy.newProxyInstance(target.getClass().getClassLoader(), interfaceTypes, handler);
        });
    }

    @Override
    public SafeCallerBuilder<T> withTimeout(long timeout) {
        this.timeout = timeout;
        return this;
    }

    @Override
    public SafeCallerBuilder<T> withIdentifier(Object identifier) {
        this.identifier = identifier;
        return this;
    }

    @Override
    public SafeCallerBuilder<T> onException(Consumer<Throwable> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        return this;
    }

    @Override
    public SafeCallerBuilder<T> onTimeout(Runnable timeoutHandler) {
        this.timeoutHandler = timeoutHandler;
        return this;
    }

    @Override
    public SafeCallerBuilder<T> withAsync() {
        this.async = true;
        return this;
    }

}
