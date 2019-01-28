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

/**
 * Denotes that there already is a thread occupied by the same context.
 *
 * @author Simon Kaufmann - initial contribution and API.
 *
 */
class DuplicateExecutionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final Invocation callable;

    DuplicateExecutionException(Invocation invocation) {
        this.callable = invocation;
    }

    public Invocation getCallable() {
        return callable;
    }

}
