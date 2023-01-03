/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.io.transport.modbus.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.io.transport.modbus.ModbusFailureCallback;
import org.openhab.core.io.transport.modbus.ModbusWriteCallback;
import org.openhab.core.io.transport.modbus.ModbusWriteRequestBlueprint;
import org.openhab.core.io.transport.modbus.WriteTask;
import org.openhab.core.io.transport.modbus.endpoint.ModbusSlaveEndpoint;

/**
 * Simple implementation for Modbus write requests
 *
 * @author Sami Salonen - Initial contribution
 *
 */
@NonNullByDefault
public class BasicWriteTask implements WriteTask {

    private ModbusSlaveEndpoint endpoint;
    private ModbusWriteRequestBlueprint request;
    private ModbusWriteCallback resultCallback;
    private ModbusFailureCallback<ModbusWriteRequestBlueprint> failureCallback;

    public BasicWriteTask(ModbusSlaveEndpoint endpoint, ModbusWriteRequestBlueprint request,
            ModbusWriteCallback resultCallback, ModbusFailureCallback<ModbusWriteRequestBlueprint> failureCallback) {
        super();
        this.endpoint = endpoint;
        this.request = request;
        this.resultCallback = resultCallback;
        this.failureCallback = failureCallback;
    }

    @Override
    public ModbusSlaveEndpoint getEndpoint() {
        return endpoint;
    }

    @Override
    public ModbusWriteRequestBlueprint getRequest() {
        return request;
    }

    @Override
    public ModbusWriteCallback getResultCallback() {
        return resultCallback;
    }

    @Override
    public ModbusFailureCallback<ModbusWriteRequestBlueprint> getFailureCallback() {
        return failureCallback;
    }

    @Override
    public String toString() {
        return "BasicWriteTask [endpoint=" + endpoint + ", request=" + request + ", resultCallback=" + resultCallback
                + ", failureCallback=" + failureCallback + "]";
    }
}
