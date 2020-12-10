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
 * <p>
 * ModbusWriteRequestBlueprintVisitor interface.
 * </p>
 *
 * @author Sami Salonen - Initial contribution
 */
@NonNullByDefault
public interface ModbusWriteRequestBlueprintVisitor {

    /**
     * Visit request writing coil data
     *
     * @param blueprint
     */
    public void visit(ModbusWriteCoilRequestBlueprint blueprint);

    /**
     * Visit request writing register data
     *
     * @param blueprint
     */
    public void visit(ModbusWriteRegisterRequestBlueprint blueprint);
}
