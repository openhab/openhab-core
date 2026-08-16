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
package org.openhab.core.automation.module.script;

import java.util.concurrent.locks.Lock;

import javax.script.ScriptEngine;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * This interface is used to indicate that a {@link ScriptEngine} is lockable, i.e. that it doesn't support
 * concurrency and should run scripts inside a lock.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public interface LockableScriptEngine extends ScriptEngine {

    /**
     * @return The {@link Lock} instance that should be used to guard script execution.
     */
    Lock getLock();

    /**
     * Get the lock acquisition timeout for use with {@link Lock#tryLock(long, java.util.concurrent.TimeUnit)}. The
     * lock should always be acquired with a timeout to avoid deadlocks.
     *
     * @return The timeout period to wait when trying to acquire the {@link Lock} in milliseconds before considering the
     *         acquisition a failure.
     *
     * @implNote A default implementation with a 5 seconds timeout exists.
     */
    default long getLockAcquisitionTimeoutMs() {
        return 5000L;
    }
}
