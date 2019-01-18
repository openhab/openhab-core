/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.automation.internal.commands;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class contains methods for facilitating sorting and filtering lists stored in {@link Hashtable}s.
 *
 * @author Ana Dimova - Initial Contribution
 *
 */
public class Utils {

    /**
     * These constants are used for drawing the table borders.
     */
    static final String ROW_END = "\n";
    static final char FILLER = ' ';
    static final char TABLE_DELIMITER = '-';

    /**
     * This method puts in a map, the UIDs of the automation objects. Keys are corresponding to the index into the
     * array with strings, which is lexicographically sorted.
     *
     * @param strings holds the list with sorted lexicographically strings.
     * @return an indexed UIDs of the automation objects.
     */
    static Map<String, String> putInHastable(String[] strings) {
        Hashtable<String, String> sorted = new Hashtable<String, String>();
        for (int i = 0; i < strings.length; i++) {
            sorted.put(new Integer(i + 1).toString(), strings[i]);
        }
        return sorted;
    }

    /**
     * This method uses the map, indicated by the parameter <tt>listObjects</tt> for filtering the map, indicated by the
     * parameter <tt>listUIDs</tt>. After that the map with UIDs of the objects will correspond to the map with the
     * objects.
     *
     * @param listObjects holds the list with the objects for filter criteria.
     * @param listUIDs holds the list with UIDs of the objects for filtering.
     * @return filtered list with UIDs of the objects.
     */
    static Map<String, String> filterList(Map<String, ?> listObjects, Map<String, String> listUIDs) {
        Hashtable<String, String> filtered = new Hashtable<String, String>();
        for (final Entry<String, String> entry : listUIDs.entrySet()) {
            final String id = entry.getKey();
            final String uid = entry.getValue();
            Object obj = listObjects.get(uid);
            if (obj != null) {
                filtered.put(id, uid);
            }
        }
        return filtered;
    }

    /**
     * This method is responsible for the printing of a table with the number and width of the columns,
     * as indicated by the parameter of the method <tt>columnWidths</tt> and the content of the columns,
     * indicated by the parameter <tt>values</tt>. The table has title with rows, indicated by the
     * parameter of the method <tt>titleRows</tt>.
     *
     * @param columnWidths represents the number and width of the columns of the table.
     * @param values contain the rows of the table.
     * @param titleRow contain the rows representing the title of the table.
     * @return a string representing the table content
     */
    static String getTableContent(int width, int[] columnWidths, List<String> values, String titleRow) {
        StringBuilder sb = new StringBuilder();
        List<String> tableRows = collectTableRows(width, columnWidths, values, titleRow);
        for (String tableRow : tableRows) {
            sb.append(tableRow + ROW_END);
        }
        return sb.toString();
    }

    /**
     * This method is responsible for collecting the content of the rows of a table.
     *
     * @param width represents the table width.
     * @param columnWidths represents the number and width of the columns of the table.
     * @param values contain the rows of the table.
     * @param titleRow contain the title of the table.
     * @return a list with strings representing the rows of a table.
     */
    static List<String> collectTableRows(int width, int[] columnWidths, List<String> values, String titleRow) {
        List<String> tableRows = getTableTitle(titleRow, width);
        for (String value : values) {
            tableRows.add(value);
        }
        tableRows.add(getTableBottom(width));
        return tableRows;
    }

    /**
     * This method is responsible for the printing of a row of a table with the number and width of the columns,
     * as indicated by the parameter of the method <tt>columnWidths</tt> and the content of the columns, indicated by
     * the parameter <tt>values</tt>.
     *
     * @param columnWidths indicates the number and width of the columns of the table.
     * @param values indicates the content of the columns of the table.
     * @return a string representing the row of the table.
     */
    static String getRow(int[] columnWidths, List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columnWidths.length; i++) {
            sb.append(getColumn(columnWidths[i], values.get(i)));
        }
        return sb.toString();
    }

    /**
     * This method is responsible for the printing a title of a table with width specified by the parameter of
     * the method - <tt>width</tt> and content specified by the parameter of the method - <tt>titleRows</tt>.
     *
     * @param titleRow specifies the content of the title.
     * @param width specifies the width of the table.
     * @return a string representing the title of the table.
     */
    static List<String> getTableTitle(String titleRow, int width) {
        List<String> res = new ArrayList<String>();
        res.add(printChars(TABLE_DELIMITER, width));
        res.add(titleRow);
        res.add(printChars(TABLE_DELIMITER, width));
        return res;
    }

    /**
     * This method is responsible for the printing a bottom of a table with width specified by the parameter of
     * the method - <tt>width</tt>.
     *
     * @param width specifies the width of the table.
     * @return a string representing the bottom of the table.
     */
    static String getTableBottom(int width) {
        return printChars(TABLE_DELIMITER, width);
    }

    /**
     * This method is responsible for the printing a Column with width specified by the parameter of
     * the method - <tt>width</tt> and content specified by the parameter of the method - <tt>value</tt>.
     *
     * @param width specifies the width of the column.
     * @param value specifies the content of the column.
     * @return a string representing the column value of the table.
     */
    static String getColumn(int width, String value) {
        value = value + FILLER;
        return value + printChars(FILLER, width - value.length());
    }

    /**
     * This method is responsible for the printing a symbol - <tt>ch</tt> as many times as specified by the parameter of
     * the method - <tt>count</tt>.
     *
     * @param ch the specified symbol.
     * @param count specifies how many times to append the specified symbol.
     * @return a string containing the symbol - <tt>ch</tt> as many times is specified.
     */
    static String printChars(char ch, int count) {
        if (count < 1) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }

    /**
     * This method sorts lexicographically the strings.
     *
     * @param strings holds the list with strings for sorting and indexing.
     */
    static void quickSort(String[] strings, int begin, int length) {
        int i, j, leftLength, rightLength, t;
        String x;
        while (length >= 3) {
            t = length - 1;
            j = t + begin;
            i = (t >> 1) + begin;
            sort3(strings, begin, i, j);
            if (length == 3) {
                return;
            }
            x = strings[i];
            i = begin + 1;
            j--;
            do {
                while (strings[i].compareTo(x) < 0) {
                    i++;
                }
                while (strings[j].compareTo(x) > 0) {
                    j--;
                }
                if (i < j) {
                    swap(strings, i, j);
                } else {
                    if (i == j) {
                        i++;
                        j--;
                    }
                    break;
                }
            } while (++i <= --j);
            leftLength = (j - begin) + 1;
            rightLength = (begin - i) + length;
            if (leftLength < rightLength) {
                if (leftLength > 1) {
                    quickSort(strings, begin, leftLength);
                }
                begin = i;
                length = rightLength;
            } else {
                if (rightLength > 1) {
                    quickSort(strings, i, rightLength);
                }
                length = leftLength;
            }
        }
        if (length == 2 && strings[begin].compareTo(strings[begin + 1]) > 0) {
            swap(strings, begin, begin + 1);
        }
    }

    /**
     * Auxiliary method for sorting lexicographically the strings at the positions x, y and z.
     *
     * @param a represents the array with the strings for sorting.
     * @param x position of the first string.
     * @param y position of the second string.
     * @param z position of the third string.
     */
    private static void sort3(String[] a, int x, int y, int z) {
        if (a[x].compareTo(a[y]) > 0) {
            if (a[x].compareTo(a[z]) > 0) {
                if (a[y].compareTo(a[z]) > 0) {
                    swap(a, x, z);
                } else {
                    swap3(a, x, y, z);
                }
            } else {
                swap(a, x, y);
            }
        } else if (a[x].compareTo(a[z]) > 0) {
            swap3(a, x, z, y);
        } else if (a[y].compareTo(a[z]) > 0) {
            swap(a, y, z);
        }
    }

    /**
     * Auxiliary method for sorting lexicographically the strings. Shuffling strings on positions x and y, as the string
     * at the position x, goes to the position y, the string at the position y, goes to the position x.
     *
     * @param a represents the array with the strings for sorting.
     * @param x position of the first string.
     * @param y position of the second string.
     */
    private static void swap(String[] a, int x, int y) {
        String t = a[x];
        a[x] = a[y];
        a[y] = t;
    }

    /**
     * Auxiliary method for sorting lexicographically the strings. Shuffling strings on positions x, y and z, as the
     * string
     * at the position x, goes to the position z, the string at the position y, goes to the position x and the string
     * at the position z, goes to the position y.
     *
     * @param a represents the array with the strings for sorting.
     * @param x position of the first string.
     * @param y position of the second string.
     * @param z position of the third string.
     */
    private static void swap3(String[] a, int x, int y, int z) {
        String t = a[x];
        a[x] = a[y];
        a[y] = a[z];
        a[z] = t;
    }

}
