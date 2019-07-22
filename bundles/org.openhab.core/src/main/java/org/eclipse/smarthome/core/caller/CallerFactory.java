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

/**
 * Factory for a caller
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public interface CallerFactory {

    /**
     * Creates a new caller.
     *
     * @param id the identifier
     * @param fixedThreadPoolSize the number of threads
     * @return a newly created caller
     */
    Caller create(final String id, int fixedThreadPoolSize);

}
