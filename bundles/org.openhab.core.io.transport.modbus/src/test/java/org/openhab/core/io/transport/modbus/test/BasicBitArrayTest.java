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

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.openhab.core.io.transport.modbus.BitArray;

/**
 * @author Sami Salonen - Initial contribution
 */
public class BasicBitArrayTest {

    @Test
    public void testGetBitAndSetBit() {
        BitArray data1 = new BitArray(true, false, true);
        assertThat(data1.size(), is(equalTo(3)));
        assertThat(data1.getBit(0), is(equalTo(true)));
        assertThat(data1.getBit(1), is(equalTo(false)));
        assertThat(data1.getBit(2), is(equalTo(true)));

        data1.setBit(1, true);
        data1.setBit(2, false);
        assertThat(data1.size(), is(equalTo(3)));
        assertThat(data1.getBit(0), is(equalTo(true)));
        assertThat(data1.getBit(1), is(equalTo(true)));
        assertThat(data1.getBit(2), is(equalTo(false)));
    }

    @Test
    public void testGetBitAndSetBit2() {
        BitArray data1 = new BitArray(3);
        assertThat(data1.size(), is(equalTo(3)));
        assertThat(data1.getBit(0), is(equalTo(false)));
        assertThat(data1.getBit(1), is(equalTo(false)));
        assertThat(data1.getBit(2), is(equalTo(false)));

        data1.setBit(1, true);
        assertThat(data1.size(), is(equalTo(3)));
        assertThat(data1.getBit(0), is(equalTo(false)));
        assertThat(data1.getBit(1), is(equalTo(true)));
        assertThat(data1.getBit(2), is(equalTo(false)));

        data1.setBit(1, false);
        assertThat(data1.size(), is(equalTo(3)));
        assertThat(data1.getBit(0), is(equalTo(false)));
        assertThat(data1.getBit(1), is(equalTo(false)));
        assertThat(data1.getBit(2), is(equalTo(false)));
    }

    @Test
    public void testOutOfBounds() {
        BitArray data1 = new BitArray(true, false, true);
        assertThrows(IndexOutOfBoundsException.class, () -> data1.getBit(3));
    }

    @Test
    public void testOutOfBounds2() {
        BitArray data1 = new BitArray(true, false, true);
        assertThrows(IndexOutOfBoundsException.class, () -> data1.getBit(-1));
    }

    @Test
    public void testOutOfBounds3() {
        BitArray data1 = new BitArray(3);
        assertThrows(IndexOutOfBoundsException.class, () -> data1.getBit(3));
    }

    @Test
    public void testOutOfBounds4() {
        BitArray data1 = new BitArray(3);
        assertThrows(IndexOutOfBoundsException.class, () -> data1.getBit(-1));
    }
}
