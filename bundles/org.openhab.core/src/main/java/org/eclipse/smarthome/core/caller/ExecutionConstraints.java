/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.core.caller;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Constraints for an execution.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public class ExecutionConstraints {

    public static final ExecutionConstraints NONE = new ExecutionConstraints(-1, null);

    private final long timeout;
    private final @Nullable Runnable onTimeout;

    /**
     * Constructor.
     *
     * @param timeout the timeout (use a negative number if no timeout is used)
     * @param onTimeout the function that should be called if the timeout has been exceeded (must be non null if timeout
     *            is non negative, ignored if timeout is negative)
     */
    public ExecutionConstraints(long timeout, @Nullable Runnable onTimeout) {
        if (timeout >= 0) {
            if (onTimeout != null) {
                this.onTimeout = onTimeout;
            } else {
                throw new IllegalArgumentException("If a timeout is used, a timeout function is mandatory.");
            }
        } else {
            this.onTimeout = null;
        }
        this.timeout = timeout;
    }

    /**
     * Indicates if a timeout is used.
     *
     * @return {@code true} if a timeout is used, {@code false} if not
     */
    public boolean useTimeout() {
        return timeout >= 0;
    }

    /**
     * Gets the timeout.
     *
     * <p>
     * This method should be called if {@link #useTimeout()} returns {@code true} only.
     *
     * @return the timeout
     * @throws IllegalStateException if timeout is not used
     */
    public long getTimeout() {
        if (!useTimeout()) {
            throw new IllegalStateException("timeout is not used");
        }
        return timeout;
    }

    /**
     * Gets the function that should be called on timeout.
     *
     * <p>
     * This method should be called if {@link #useTimeout()} returns {@code true} only.
     *
     * @return the timeout function
     * @throws IllegalStateException if timeout is not used
     */
    public Runnable onTimeout() {
        if (!useTimeout()) {
            throw new IllegalStateException("timeout is not used");
        }
        final Runnable onTimeout = this.onTimeout;
        if (onTimeout == null) {
            throw new IllegalStateException("if timeout if set, a function is mandatory");
        }
        return onTimeout;
    }

}
