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

import org.apache.commons.lang.builder.StandardToStringStyle;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Implementation for writing coils
 *
 * @author Sami Salonen - Initial contribution
 *
 */
@NonNullByDefault
public class ModbusWriteCoilRequestBlueprint extends ModbusWriteRequestBlueprint {

    private static StandardToStringStyle toStringStyle = new StandardToStringStyle();

    static {
        toStringStyle.setUseShortClassName(true);
    }

    private final int slaveId;
    private final int reference;
    private final BitArray bits;
    private final boolean writeMultiple;
    private final int maxTries;

    /**
     * Construct coil write request with single bit of data
     *
     * @param slaveId slave id to write to
     * @param reference reference address
     * @param data bit to write
     * @param writeMultiple whether to use {@link ModbusWriteFunctionCode.WRITE_MULTIPLE_COILS} over
     *            {@link ModbusWriteFunctionCode.WRITE_COIL}
     * @param maxTries maximum number of tries in case of errors, should be at least 1
     */
    public ModbusWriteCoilRequestBlueprint(int slaveId, int reference, boolean data, boolean writeMultiple,
            int maxTries) {
        this(slaveId, reference, new BitArray(data), writeMultiple, maxTries);
    }

    /**
     * Construct coil write request with many bits of data
     *
     * @param slaveId slave id to write to
     * @param reference reference address
     * @param data bit(s) to write
     * @param writeMultiple whether to use {@link ModbusWriteFunctionCode.WRITE_MULTIPLE_COILS} over
     *            {@link ModbusWriteFunctionCode.WRITE_COIL}. Useful with single bit of data.
     * @param maxTries maximum number of tries in case of errors, should be at least 1
     * @throws IllegalArgumentException in case <code>data</code> is empty, <code>writeMultiple</code> is
     *             <code>false</code> but there are many bits to write.
     */
    public ModbusWriteCoilRequestBlueprint(int slaveId, int reference, BitArray data, boolean writeMultiple,
            int maxTries) {
        super();
        this.slaveId = slaveId;
        this.reference = reference;
        this.bits = data;
        this.writeMultiple = writeMultiple;
        this.maxTries = maxTries;

        if (!writeMultiple && bits.size() > 1) {
            throw new IllegalArgumentException("With multiple coils, writeMultiple must be true");
        }
        if (bits.size() == 0) {
            throw new IllegalArgumentException("Must have at least one bit");
        }
        if (maxTries <= 0) {
            throw new IllegalArgumentException("maxTries should be positive, was " + maxTries);
        }
    }

    @Override
    public int getUnitID() {
        return slaveId;
    }

    @Override
    public int getReference() {
        return reference;
    }

    @Override
    public ModbusWriteFunctionCode getFunctionCode() {
        return writeMultiple ? ModbusWriteFunctionCode.WRITE_MULTIPLE_COILS : ModbusWriteFunctionCode.WRITE_COIL;
    }

    public BitArray getCoils() {
        return bits;
    }

    @Override
    public int getMaxTries() {
        return maxTries;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, toStringStyle).append("slaveId", slaveId).append("reference", reference)
                .append("functionCode", getFunctionCode()).append("bits", bits).append("maxTries", maxTries).toString();
    }

    @Override
    public void accept(ModbusWriteRequestBlueprintVisitor visitor) {
        visitor.visit(this);
    }
}
