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
package org.openhab.core.library.dimension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.Units;

/**
 * The {@link DataAmountTest} holds tests for {@link DataAmount}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class DataAmountTest {

    @Test
    public void testBToMB() {
        QuantityType<DataAmount> quantityType = new QuantityType("1000 B");
        QuantityType<DataAmount> converted = quantityType.toUnit("MB");

        assertThat(converted.toString(), is(equalTo("0.001 MB")));
    }

    @Test
    public void testKBToMB() {
        QuantityType<DataAmount> quantityType = new QuantityType("1000 kB");
        QuantityType<DataAmount> converted = quantityType.toUnit("MB");

        assertThat(converted.toString(), is(equalTo("1 MB")));
    }

    @Test
    public void testInvertibleUnit() {
        QuantityType<DataAmount> quantityType = new QuantityType("1000 kB");
        QuantityType<?> inverted = quantityType.toInvertibleUnit(Units.MEGABYTE);

        assertThat(quantityType, is(equalTo(inverted)));
    }
}
