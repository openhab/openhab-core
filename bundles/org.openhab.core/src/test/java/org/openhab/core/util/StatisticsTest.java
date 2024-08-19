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

import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

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
        List<BigDecimal> randomList = new Random().doubles(100, 0, 100).mapToObj(v -> BigDecimal.valueOf(v)).toList();
        int iterations = 50;

        int size = randomList.size();
        int k = size / 2; // median

        long startTime = System.nanoTime();
        List<BigDecimal> baseList = randomList.stream().sorted().toList();
        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        int expected = baseList.get(k).intValue();
        int prevExpected = baseList.get(k - 1).intValue();

        long durationNoForcePrevious = 0;
        long durationForcePrevious = 0;
        // Iterate a few times with reshuffled list to exclude impact of initial ordering
        for (int i = 0; i < iterations; i++) {
            ArrayList<BigDecimal> bdList = new ArrayList<>(baseList);
            Collections.shuffle(bdList);
            startTime = System.nanoTime();
            BigDecimal bd = Statistics.quickSelect(bdList, 0, size - 1, k, false);
            endTime = System.nanoTime();
            durationNoForcePrevious += endTime - startTime;
            assertNotNull(bd);
            int result = bd.intValue();
            assertEquals(expected, result);

            bdList = new ArrayList<>(baseList); // recreate as order may have changed
            Collections.shuffle(bdList);
            startTime = System.nanoTime();
            bd = Statistics.quickSelect(bdList, 0, size - 1, k, true);
            endTime = System.nanoTime();
            durationForcePrevious += endTime - startTime;
            assertNotNull(bd);
            result = bd.intValue();
            assertEquals(expected, result);
            assertEquals(prevExpected, bdList.get(k - 1).intValue());
        }

        PrintStream out = System.out;
        if (out != null) {
            out.print("List size: ");
            out.print(size);
            out.print(", iterations: ");
            out.println(iterations);
            out.print("  Stream sort duration (ns): ");
            out.println(duration);
            out.print("  Quickselect average duration (ns): ");
            out.println(durationNoForcePrevious / iterations);
            out.print("  Quickselect force previous order average duration (ns): ");
            out.println(durationForcePrevious / iterations);
        }
    }
}
