/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.core.util;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * The {@link StatisticsTest} is a test class for the statistics helper methods
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public class StatisticsTest {

    @Test
    public void testQuickSelect() {
        List<BigDecimal> baseList = List.of(10, 11, 9, 7, 24, 18, 33, 18).stream().map(v -> new BigDecimal(v)).toList();
        int repeats = 20; // quickSelect depends on a random pivot index. To make sure the random index does not
                          // influence test results, repeat the test several times.

        int expected = 18;
        int prevExpected = 11;
        int size = baseList.size();
        int k = size / 2; // median

        // First test without randomQuickSelectSeed (default value)
        ArrayList<BigDecimal> bdList = new ArrayList<>(baseList);
        BigDecimal qs = Statistics.quickSelect(bdList, 0, size - 1, k, false);
        assertNotNull(qs);
        int result = qs.intValue();
        assertEquals(expected, result);

        bdList = new ArrayList<>(baseList); // recreate as order may have changed
        qs = Statistics.quickSelect(bdList, 0, size - 1, k, true);
        assertNotNull(qs);
        result = qs.intValue();
        assertEquals(expected, result);
        assertEquals(prevExpected, bdList.get(k - 1).intValue());

        // Test with randomQuickSelectSeed
        Statistics.randomQuickSelectSeed = true;
        for (int pivotIndex = 0; pivotIndex < repeats; pivotIndex++) {
            bdList = new ArrayList<>(baseList);
            qs = Statistics.quickSelect(bdList, 0, size - 1, k, false);
            assertNotNull(qs);
            result = qs.intValue();
            assertEquals(expected, result);

            bdList = new ArrayList<>(baseList); // recreate as order may have changed
            qs = Statistics.quickSelect(bdList, 0, size - 1, k, true);
            assertNotNull(qs);
            result = qs.intValue();
            assertEquals(expected, result);
            assertEquals(prevExpected, bdList.get(k - 1).intValue());
        }
        Statistics.randomQuickSelectSeed = false;
    }
}
