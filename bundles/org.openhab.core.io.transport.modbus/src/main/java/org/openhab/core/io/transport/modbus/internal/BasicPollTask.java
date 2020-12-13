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
package org.openhab.core.io.transport.modbus.internal;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.StandardToStringStyle;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.transport.modbus.ModbusFailureCallback;
import org.openhab.core.io.transport.modbus.ModbusReadCallback;
import org.openhab.core.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.core.io.transport.modbus.PollTask;
import org.openhab.core.io.transport.modbus.endpoint.ModbusSlaveEndpoint;

/**
 * Implementation of {@link PollTask} that differentiates tasks using endpoint, request and callbacks.
 *
 * Note: Two differentiate poll tasks are considered unequal if their callbacks are unequal.
 *
 * HashCode and equals should be defined such that two poll tasks considered the same only if their request,
 * maxTries, endpoint and callback are the same.
 *
 * @author Sami Salonen - Initial contribution
 *
 */
@NonNullByDefault
public class BasicPollTask implements PollTask {

    static StandardToStringStyle toStringStyle = new StandardToStringStyle();
    static {
        toStringStyle.setUseShortClassName(true);
    }

    private ModbusSlaveEndpoint endpoint;
    private ModbusReadRequestBlueprint request;
    private ModbusReadCallback resultCallback;
    private ModbusFailureCallback<ModbusReadRequestBlueprint> failureCallback;

    public BasicPollTask(ModbusSlaveEndpoint endpoint, ModbusReadRequestBlueprint request,
            ModbusReadCallback resultCallback, ModbusFailureCallback<ModbusReadRequestBlueprint> failureCallback) {
        this.endpoint = endpoint;
        this.request = request;
        this.resultCallback = resultCallback;
        this.failureCallback = failureCallback;
    }

    @Override
    public ModbusReadRequestBlueprint getRequest() {
        return request;
    }

    @Override
    public ModbusSlaveEndpoint getEndpoint() {
        return endpoint;
    }

    @Override
    public ModbusReadCallback getResultCallback() {
        return resultCallback;
    }

    @Override
    public ModbusFailureCallback<ModbusReadRequestBlueprint> getFailureCallback() {
        return failureCallback;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(69, 5).append(request).append(getEndpoint()).append(getResultCallback())
                .append(getFailureCallback()).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, toStringStyle).append("request", request).append("endpoint", endpoint)
                .append("resultCallback", getResultCallback()).append("failureCallback", getFailureCallback())
                .toString();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        BasicPollTask rhs = (BasicPollTask) obj;
        return new EqualsBuilder().append(request, rhs.request).append(endpoint, rhs.endpoint)
                .append(getResultCallback(), rhs.getResultCallback())
                .append(getFailureCallback(), rhs.getFailureCallback()).isEquals();
    }
}
