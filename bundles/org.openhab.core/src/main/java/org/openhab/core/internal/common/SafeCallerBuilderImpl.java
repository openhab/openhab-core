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
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.SafeCaller;
import org.openhab.core.common.SafeCallerBuilder;

/**
 * Builder implementation to create safe-call wrapper objects.
 *
 * @author Simon Kaufmann - Initial contribution
 *
 * @param <T>
 */
@NonNullByDefault
public class SafeCallerBuilderImpl<T> implements SafeCallerBuilder<T> {

    private final T target;
    private final Class<?>[] interfaceTypes;
    private long timeout;
    private Object identifier;
    private @Nullable Consumer<Throwable> exceptionHandler;
    private @Nullable Runnable timeoutHandler;
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
                handler = new InvocationHandlerAsync<>(manager, target, identifier, timeout, exceptionHandler,
                        timeoutHandler);
            } else {
                handler = new InvocationHandlerSync<>(manager, target, identifier, timeout, exceptionHandler,
                        timeoutHandler);
            }
            return (T) Proxy.newProxyInstance(
                    CombinedClassLoader.fromClasses(getClass().getClassLoader(),
                            Stream.concat(Stream.of(target.getClass()), Arrays.stream(interfaceTypes))),
                    interfaceTypes, handler);
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
