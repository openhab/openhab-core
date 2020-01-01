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
package org.openhab.core.common;

import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * OSGi service to obtain a {@link SafeCallerBuilder}.
 *
 * Safe-calls are used within the framework in order to protect it from hanging/blocking binding code and log meaningful
 * messages to detect and identify such hanging code.
 *
 * @author Simon Kaufmann - Initial contribution
 */
@NonNullByDefault
public interface SafeCaller {

    /**
     * Default timeout for actions in milliseconds.
     */
    long DEFAULT_TIMEOUT = TimeUnit.SECONDS.toMillis(5);

    /**
     * Create a safe call builder for the given object.
     *
     * @param target the object on which calls should be protected by the safe caller
     * @param interfaceType the interface which defines the relevant methods
     * @return a safe call builder instance.
     */
    <T> SafeCallerBuilder<T> create(T target, Class<T> interfaceType);

}
