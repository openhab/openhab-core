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
package org.openhab.core.ephemeris;

import java.io.FileNotFoundException;
import java.net.URL;
import java.time.ZonedDateTime;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This service provides functionality around days of the year and is the central
 * service to be used directly by others.
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public interface EphemerisManager {

    /**
     * Tests given day status against configured weekend days
     *
     * @param date observed day
     * @return whether the day is on weekend
     */
    boolean isWeekend(ZonedDateTime date);

    /**
     * Tests given day status against configured dayset
     *
     * @param daysetName name of the requested dayset, without prefix
     * @param date observed day
     * @return whether the day is in the dayset
     */
    boolean isInDayset(String daysetName, ZonedDateTime date);

    /**
     * Tests given day status
     *
     * @param date observed day
     * @return whether the day is bank holiday or not
     */
    boolean isBankHoliday(ZonedDateTime date);

    /**
     * Tests given day status against given userfile
     *
     * @param date observed day
     * @param url bundle resource file containing holiday definitions
     * @return whether the day is bank holiday or not
     */
    boolean isBankHoliday(ZonedDateTime date, URL resource);

    /**
     * Tests given day status against given userfile
     *
     * @param date observed day
     * @param source absolute or relative path to the file on local file system
     * @return whether the day is bank holiday or not
     * @throws FileNotFoundException
     */
    boolean isBankHoliday(ZonedDateTime date, String filename) throws FileNotFoundException;

    /**
     * Get given day name from given userfile
     *
     * @param date observed day
     * @return name of the day or null if no corresponding entry
     */
    @Nullable
    String getBankHolidayName(ZonedDateTime date);

    /**
     * Get given day name from given userfile
     *
     * @param date observed day
     * @param url bundle resource file containing holiday definitions
     * @return name of the day or null if no corresponding entry
     */
    @Nullable
    String getBankHolidayName(ZonedDateTime date, URL resource);

    /**
     * Get given day name from given userfile
     *
     * @param date observed day
     * @param source absolute or relative path to the file on local file system
     * @return name of the day or null if no corresponding entry
     * @throws FileNotFoundException
     */
    @Nullable
    String getBankHolidayName(ZonedDateTime date, String filename) throws FileNotFoundException;

    /**
     * Gets the first next to come holiday in a 1 year time window
     *
     * @param startDate first day of the time window
     * @return next coming holiday
     */
    @Nullable
    String getNextBankHoliday(ZonedDateTime startDate);

    /**
     * Gets the first next to come holiday in a 1 year time window
     *
     * @param startDate first day of the time window
     * @param url bundle resource file containing holiday definitions
     * @return next coming holiday
     */
    @Nullable
    String getNextBankHoliday(ZonedDateTime startDate, URL resource);

    /**
     * Gets the first next to come holiday in a 1 year time window
     *
     * @param startDate first day of the time window
     * @param source absolute or relative path to the file on local file system
     * @return next coming holiday
     * @throws FileNotFoundException
     */
    @Nullable
    String getNextBankHoliday(ZonedDateTime startDate, String filename) throws FileNotFoundException;

    /**
     * Gets the localized holiday description
     *
     * @param holidayName code of searched holiday
     * @return localized holiday description
     */
    @Nullable
    String getHolidayDescription(@Nullable String holiday);

    /**
     * Gets the number of days until searchedHoliday
     *
     * @param from first day of the time window
     * @param searchedHoliday name of the searched holiday
     * @return difference in days, -1 if not found
     */
    long getDaysUntil(ZonedDateTime from, String searchedHoliday);

    /**
     * Gets the number of days until searchedHoliday in user file
     *
     * @param from first day of the time window
     * @param searchedHoliday name of the searched holiday
     * @param url bundle resource file containing holiday definitions
     * @return difference in days, -1 if not found
     */
    long getDaysUntil(ZonedDateTime from, String searchedHoliday, URL resource);

    /**
     * Gets the number of days until searchedHoliday in user file
     *
     * @param from first day of the time window
     * @param searchedHoliday name of the searched holiday
     * @param source absolute or relative path to the file on local file system
     * @return difference in days, -1 if not found
     * @throws FileNotFoundException
     */
    long getDaysUntil(ZonedDateTime from, String searchedHoliday, String filename) throws FileNotFoundException;
}
