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
package org.openhab.core.model.core;

import java.util.function.Supplier;

/**
 * Service interface to execute EMF methods in a single based thread.
 *
 * @author Markus Rathgeb - Initial contribution
 */
public interface SafeEMF {

    /**
     * Calls the given function.
     *
     * @param <T> the return type of the calling function
     * @param func the function to call
     * @return the return value of the called function
     */
    <T> T call(Supplier<T> func);

    /**
     * Calls the given function.
     *
     * @param func the function to call
     */
    void call(Runnable func);

}
