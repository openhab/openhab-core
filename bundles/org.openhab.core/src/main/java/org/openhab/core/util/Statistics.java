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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link Statistics} is a class with statistics helper methods.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public class Statistics {

    public static boolean randomQuickSelectSeed = false; // Can be enabled to always create a random pivot index for the
                                                         // quickSelect algorithm, false will start from first value in
                                                         // list as pivot and avoid random number generation

    /**
     * Find the median in a list of values
     *
     * @param inputList
     * @return median of the values, null if the list is empty
     */
    public static @Nullable BigDecimal median(List<BigDecimal> inputList) {
        ArrayList<BigDecimal> bdList = new ArrayList<>(inputList); // Make a copy that will get reordered
        int size = bdList.size();
        if (size >= 0) {
            int k = size / 2;
            BigDecimal median = null;
            if (size % 2 == 1) {
                median = Statistics.quickSelect(bdList, 0, size - 1, k, false);
            } else {
                median = Statistics.quickSelect(bdList, 0, size - 1, k, true);
                if (median != null) {
                    // quickSelect has forced the k-1 element to be in the right place
                    median = median.add(bdList.get(k - 1)).divide(new BigDecimal(2));
                }
            }
            return median;
        }
        return null;
    }

    /**
     * Find the k-smallest element between indexes l and r in a list. This is an implementation of the quickSelect
     * algorithm. If the forcePreviousOrder parameter is set to true, put the element before the k-smallest element at
     * position k-1 in bdList. This is useful to calculate the median or percentile on a list with an uneven number of
     * elements. The median will then be the sum of the returned value and the element at position k-1 divided by 2.
     *
     * See https://en.wikipedia.org/wiki/Quickselect and https://gist.github.com/unnikked/14c19ba13f6a4bfd00a3
     *
     * @param bdList, list elements will be reordered in place
     * @param l index of left most element in list to consider
     * @param r index of right most element in list to consider
     * @param k
     * @param forcePreviousOrder positions the k-1 element in the right place if true, useful to calculate median on
     *            list with even length
     * @return
     */
    static @Nullable BigDecimal quickSelect(ArrayList<BigDecimal> bdList, int l, int r, int k,
            boolean forcePreviousOrder) {
        if (r < 0) {
            return null;
        } else if (r == 0) {
            return bdList.get(r);
        }

        int left = l;
        int right = r;
        for (;;) {
            int pivotIndex = randomPivot(left, right);
            pivotIndex = partition(bdList, left, right, pivotIndex, forcePreviousOrder);

            if (k == pivotIndex) {
                return bdList.get(k);
            } else if (k < pivotIndex) {
                right = pivotIndex - 1;
            } else {
                left = pivotIndex + 1;
            }
        }
    }

    private static int partition(ArrayList<BigDecimal> bdList, int left, int right, int pivotIndex,
            boolean forcePreviousOrder) {
        BigDecimal pivotValue = bdList.get(pivotIndex);
        swap(bdList, pivotIndex, right); // Move pivot to end
        int beforePivotIndex = left;
        int storeIndex = left;
        for (int i = left; i < right; i++) {
            if (bdList.get(i).compareTo(pivotValue) < 0) {
                if (forcePreviousOrder && (bdList.get(i).compareTo(bdList.get(beforePivotIndex)) > 0)) {
                    beforePivotIndex = storeIndex;
                }
                swap(bdList, storeIndex, i);
                storeIndex++;
            }
        }
        swap(bdList, right, storeIndex); // Move pivot to its final place
        if (forcePreviousOrder && (storeIndex > beforePivotIndex)) {
            swap(bdList, beforePivotIndex, storeIndex - 1);
        }
        return storeIndex;
    }

    private static void swap(ArrayList<BigDecimal> bdList, int i, int j) {
        if (i != j) {
            BigDecimal tmp = bdList.get(i);
            bdList.set(i, bdList.get(j));
            bdList.set(j, tmp);
        }
    }

    private static int randomPivot(int left, int right) {
        if (!randomQuickSelectSeed) {
            // The overhead of random may reduce algorithm performance, therefore just start with the first value as
            // pivot
            return left;
        }
        return left + (int) Math.floor(Math.random() * (right - left + 1));
    }
}
