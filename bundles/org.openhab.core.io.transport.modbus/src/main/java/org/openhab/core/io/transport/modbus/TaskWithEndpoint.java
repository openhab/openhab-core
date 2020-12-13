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
package org.openhab.core.io.transport.modbus;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.io.transport.modbus.endpoint.ModbusSlaveEndpoint;

/**
 * Common base interface for read and write tasks.
 *
 * @author Sami Salonen - Initial contribution
 *
 * @param <R> request type
 * @param <C> callback type
 */
@NonNullByDefault
public interface TaskWithEndpoint<R, C extends ModbusResultCallback, F extends ModbusFailureCallback<R>> {
    /**
     * Gets endpoint associated with this task
     *
     * @return
     */
    ModbusSlaveEndpoint getEndpoint();

    /**
     * Gets request associated with this task
     *
     * @return
     */
    R getRequest();

    /**
     * Gets the result callback associated with this task, will be called with response
     *
     * @return
     */
    C getResultCallback();

    /**
     * Gets the failure callback associated with this task, will be called in case of an error
     *
     * @return
     */
    F getFailureCallback();

    int getMaxTries();
}
