/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import net.wimpi.modbus.Modbus;

/**
 * Implementation of immutable representation of modbus read request
 *
 * Equals and hashCode implemented keeping {@link PollTask} in mind: two instances of this class are considered the same
 * if they have
 * the equal parameters (same slave id, start, length, function code and maxTries).
 *
 * @author Sami Salonen - Initial contribution
 *
 */
@NonNullByDefault
public class ModbusReadRequestBlueprint {

    private final int slaveId;
    private final ModbusReadFunctionCode functionCode;
    private final int start;
    private final int length;
    private final int maxTries;

    public ModbusReadRequestBlueprint(int slaveId, ModbusReadFunctionCode functionCode, int start, int length,
            int maxTries) {
        this.slaveId = slaveId;
        this.functionCode = functionCode;
        this.start = start;
        this.length = length;
        this.maxTries = maxTries;
    }

    /**
     * Returns the unit identifier of this
     * <tt>ModbusMessage</tt> as <tt>int</tt>.<br>
     * The identifier is a 1-byte non negative
     * integer value valid in the range of 0-255.
     * <p>
     *
     * @return the unit identifier as <tt>int</tt>.
     */
    public int getUnitID() {
        return slaveId;
    }

    public int getReference() {
        return start;
    }

    public ModbusReadFunctionCode getFunctionCode() {
        return functionCode;
    }

    public int getDataLength() {
        return length;
    }

    /**
     * Maximum number of tries to execute the request, when request fails
     *
     * For example, number 1 means on try only with no re-tries.
     *
     * @return number of maximum tries
     */
    public int getMaxTries() {
        return maxTries;
    }

    /**
     * Returns the protocol identifier of this
     * <tt>ModbusMessage</tt> as <tt>int</tt>.<br>
     * The identifier is a 2-byte (short) non negative
     * integer value valid in the range of 0-65535.
     * <p>
     *
     * @return the protocol identifier as <tt>int</tt>.
     */
    public int getProtocolID() {
        return Modbus.DEFAULT_PROTOCOL_ID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(functionCode, length, maxTries, slaveId, start);
    }

    @Override
    public String toString() {
        return "ModbusReadRequestBlueprint [slaveId=" + slaveId + ", functionCode=" + functionCode + ", start=" + start
                + ", length=" + length + ", maxTries=" + maxTries + "]";
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
        ModbusReadRequestBlueprint rhs = (ModbusReadRequestBlueprint) obj;
        return functionCode == rhs.functionCode && length == rhs.length && slaveId == rhs.slaveId && start == rhs.start;
    }
}
