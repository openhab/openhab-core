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

import java.util.concurrent.ExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Interface to the safe call manager which handles queuing and tracking of safe-call executions.
 *
 * @author Simon Kaufmann - Initial contribution
 */
@NonNullByDefault
public interface SafeCallManager {

    /**
     * Track that the call to the target method starts.
     *
     * @param invocation the wrapper around the actual call
     */
    void recordCallStart(Invocation invocation);

    /**
     * Track that the call to the target method finished.
     *
     * @param invocation the wrapper around the actual call
     */
    void recordCallEnd(Invocation invocation);

    /**
     * Queue the given invocation for asynchronous execution.
     *
     * @param invocation the call to the proxy
     */
    void enqueue(Invocation invocation);

    /**
     * Get the safe-caller's executor service instance
     *
     * @return the safe-caller's executor service
     */
    ExecutorService getScheduler();

    /**
     * Get the active invocation if the current thread already is a safe-call thread.
     *
     * @return the active invocation or {@code null}
     */
    @Nullable
    Invocation getActiveInvocation();

}
