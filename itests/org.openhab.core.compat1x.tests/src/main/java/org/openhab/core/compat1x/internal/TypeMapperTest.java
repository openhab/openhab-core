/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.core.compat1x.internal;

import static org.junit.Assert.assertEquals;

import javax.measure.quantity.Temperature;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.junit.Test;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.library.tel.types.CallType;

/**
 * @author Kai Kreuzer - Initial contribution
 */
public class TypeMapperTest {

    @Test
    public void testDateTypeType() {
        DateTimeType ohType1 = new DateTimeType();
        DateTimeType ohType2 = (DateTimeType) TypeMapper.mapToOpenHABType(TypeMapper.mapToESHType(ohType1));
        assertEquals(ohType1.toString(), ohType2.toString());
    }

    @Test
    public void testCallType() {
        CallType ohType1 = new CallType("123##456");
        CallType ohType2 = (CallType) TypeMapper.mapToOpenHABType(TypeMapper.mapToESHType(ohType1));
        assertEquals(ohType1, ohType2);
    }

    @Test
    public void testQuantityType() {
        QuantityType<Temperature> eshType1 = new QuantityType<>("10 °C");
        DecimalType eshType2 = (DecimalType) TypeMapper.mapToESHType(TypeMapper.mapToOpenHABType(eshType1));
        assertEquals(eshType1.as(DecimalType.class), eshType2);
    }
}
