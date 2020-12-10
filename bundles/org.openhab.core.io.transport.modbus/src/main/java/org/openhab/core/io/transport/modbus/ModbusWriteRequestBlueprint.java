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

import net.wimpi.modbus.Modbus;

/**
 * Base interface for Modbus write requests
 *
 * @author Sami Salonen - Initial contribution
 *
 */
@NonNullByDefault
public abstract class ModbusWriteRequestBlueprint {

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

    /**
     * Returns the reference of the register/coil/discrete input to to start
     * writing with this request
     * <p>
     *
     * @return the reference of the register
     *         to start reading from as <tt>int</tt>.
     */
    public abstract int getReference();

    /**
     * Returns the unit identifier of this
     * <tt>ModbusMessage</tt> as <tt>int</tt>.<br>
     * The identifier is a 1-byte non negative
     * integer value valid in the range of 0-255.
     * <p>
     *
     * @return the unit identifier as <tt>int</tt>.
     */
    public abstract int getUnitID();

    /**
     * Returns the function code of this
     * <tt>ModbusMessage</tt> as <tt>int</tt>.<br>
     * The function code is a 1-byte non negative
     * integer value valid in the range of 0-127.<br>
     * Function codes are ordered in conformance
     * classes their values are specified in
     * <tt>net.wimpi.modbus.Modbus</tt>.
     * <p>
     *
     * @return the function code as <tt>int</tt>.
     *
     * @see net.wimpi.modbus.Modbus
     */
    public abstract ModbusWriteFunctionCode getFunctionCode();

    /**
     * Get maximum number of tries, in case errors occur. Should be at least 1.
     */
    public abstract int getMaxTries();

    /**
     * Accept visitor
     *
     * @param visitor
     */
    public abstract void accept(ModbusWriteRequestBlueprintVisitor visitor);
}
