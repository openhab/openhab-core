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
package org.openhab.core.io.transport.modbus.test;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.StreamSupport;

import org.hamcrest.Description;
import org.openhab.core.io.transport.modbus.ModbusWriteCoilRequestBlueprint;
import org.openhab.core.io.transport.modbus.ModbusWriteFunctionCode;

/**
 * @author Sami Salonen - Initial contribution
 */
class CoilMatcher extends AbstractRequestComparer<ModbusWriteCoilRequestBlueprint> {

    private Boolean[] expectedCoils;

    public CoilMatcher(int expectedUnitId, int expectedAddress, int expectedMaxTries,
            ModbusWriteFunctionCode expectedFunctionCode, Boolean... expectedCoils) {
        super(expectedUnitId, expectedAddress, expectedFunctionCode, expectedMaxTries);
        this.expectedCoils = expectedCoils;
    }

    @Override
    public void describeTo(Description description) {
        super.describeTo(description);
        description.appendText(" coils=");
        description.appendValue(Arrays.toString(expectedCoils));
    }

    @Override
    protected boolean doMatchData(ModbusWriteCoilRequestBlueprint item) {
        Object[] actual = StreamSupport.stream(item.getCoils().spliterator(), false).toArray();
        return Objects.deepEquals(actual, expectedCoils);
    }
}
