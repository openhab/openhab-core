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

/**
 * Poll task represents Modbus write request
 *
 * Unlike {@link PollTask}, this does not have to be hashable.
 *
 * @author Sami Salonen - Initial contribution
 *
 */
@NonNullByDefault
public interface WriteTask extends
        TaskWithEndpoint<ModbusWriteRequestBlueprint, ModbusWriteCallback, ModbusFailureCallback<ModbusWriteRequestBlueprint>> {
    @Override
    default int getMaxTries() {
        return getRequest().getMaxTries();
    }
}
