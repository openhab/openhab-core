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
package org.openhab.core.model.script.actions;

import java.io.FileNotFoundException;
import java.time.ZonedDateTime;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.model.script.engine.action.ActionDoc;
import org.openhab.core.model.script.internal.engine.action.EphemerisActionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The static methods of this class are made available as functions in the scripts.
 * This allows a script to use ephemeris features.
 *
 * @author Gaël L'hopital - Initial contribution
 */
public class Ephemeris {
    private static final Logger LOGGER = LoggerFactory.getLogger(Ephemeris.class);

    @ActionDoc(text = "checks if today is a weekend day")
    public static boolean isWeekend() {
        return isWeekend(0);
    }

    @ActionDoc(text = "checks if today plus or minus a given offset is a weekend day")
    public static boolean isWeekend(int offset) {
        return isWeekend(ZonedDateTime.now().plusDays(offset));
    }

    @ActionDoc(text = "checks if a given day is a weekend day")
    public static boolean isWeekend(ZonedDateTime day) {
        return EphemerisActionService.ephemerisManager.isWeekend(day);
    }

    @ActionDoc(text = "checks if today is defined in a given dayset")
    public static boolean isInDayset(String daysetName) {
        return isInDayset(daysetName, 0);
    }

    @ActionDoc(text = "checks if today plus or minus a given offset is defined in a given dayset")
    public static boolean isInDayset(String daysetName, int offset) {
        return isInDayset(daysetName, ZonedDateTime.now().plusDays(offset));
    }

    @ActionDoc(text = "checks a given day is defined in a given dayset")
    public static boolean isInDayset(String daysetName, ZonedDateTime day) {
        return EphemerisActionService.ephemerisManager.isInDayset(daysetName, day);
    }

    @ActionDoc(text = "checks if today is bank holiday")
    public static boolean isBankHoliday() {
        return isBankHoliday(0);
    }

    @ActionDoc(text = "checks if today plus or minus a given offset is bank holiday")
    public static boolean isBankHoliday(int offset) {
        return isBankHoliday(ZonedDateTime.now().plusDays(offset));
    }

    @ActionDoc(text = "checks a given day is bank holiday")
    public static boolean isBankHoliday(ZonedDateTime day) {
        return EphemerisActionService.ephemerisManager.isBankHoliday(day);
    }

    @ActionDoc(text = "checks if today plus or minus a given offset is bank holiday from a given userfile")
    public static boolean isBankHoliday(int offset, String filename) {
        return isBankHoliday(ZonedDateTime.now().plusDays(offset), filename);
    }

    @ActionDoc(text = "checks a given day is bank holiday from a given userfile")
    public static boolean isBankHoliday(ZonedDateTime day, String filename) {
        try {
            return EphemerisActionService.ephemerisManager.isBankHoliday(day, filename);
        } catch (FileNotFoundException e) {
            LOGGER.error("Error reading holiday user file {} : {}", filename, e.getMessage());
            return false;
        }
    }

    @ActionDoc(text = "get todays bank holiday name")
    public static @Nullable String getBankHolidayName() {
        return getBankHolidayName(0);
    }

    @ActionDoc(text = "get bank holiday name for today plus or minus a given offset")
    public static @Nullable String getBankHolidayName(int offset) {
        return getBankHolidayName(ZonedDateTime.now().plusDays(offset));
    }

    @ActionDoc(text = "get bank holiday name for a given day")
    public static @Nullable String getBankHolidayName(ZonedDateTime day) {
        return EphemerisActionService.ephemerisManager.getBankHolidayName(day);
    }

    @ActionDoc(text = "get holiday for today from a given userfile")
    public static @Nullable String getBankHolidayName(String filename) {
        return getBankHolidayName(0, filename);
    }

    @ActionDoc(text = "get holiday for today plus or minus an offset from a given userfile")
    public static @Nullable String getBankHolidayName(int offset, String filename) {
        return getBankHolidayName(ZonedDateTime.now().plusDays(offset), filename);
    }

    @ActionDoc(text = "get holiday for a given day from a given userfile")
    public static @Nullable String getBankHolidayName(ZonedDateTime day, String filename) {
        try {
            return EphemerisActionService.ephemerisManager.getBankHolidayName(day, filename);
        } catch (FileNotFoundException e) {
            LOGGER.error("Error reading holiday user file {} : {}", filename, e.getMessage());
            return null;
        }
    }

    @ActionDoc(text = "get next bank holiday")
    public static @Nullable String getNextBankHoliday() {
        return getNextBankHoliday(0);
    }

    @ActionDoc(text = "get next bank holiday plus or minus an offset")
    public static @Nullable String getNextBankHoliday(int offset) {
        return getNextBankHoliday(ZonedDateTime.now().plusDays(offset));
    }

    @ActionDoc(text = "get next bank holiday holiday from a given day")
    public static @Nullable String getNextBankHoliday(ZonedDateTime day) {
        return EphemerisActionService.ephemerisManager.getNextBankHoliday(day);
    }

    @ActionDoc(text = "get next bank holiday from a given userfile")
    public static @Nullable String getNextBankHoliday(String filename) {
        return getNextBankHoliday(0, filename);
    }

    @ActionDoc(text = "get next bank holiday plus or minus an offset from a given userfile")
    public static @Nullable String getNextBankHoliday(int offset, String filename) {
        return getNextBankHoliday(ZonedDateTime.now().plusDays(offset), filename);
    }

    @ActionDoc(text = "get next bank holiday a given day from a given userfilee")
    public static @Nullable String getNextBankHoliday(ZonedDateTime day, String filename) {
        try {
            return EphemerisActionService.ephemerisManager.getNextBankHoliday(day, filename);
        } catch (FileNotFoundException e) {
            LOGGER.error("Error reading holiday user file {} : {}", filename, e.getMessage());
            return null;
        }
    }

    @ActionDoc(text = "gets the localized description of a holiday key name")
    public static @Nullable String getHolidayDescription(@Nullable String holiday) {
        return EphemerisActionService.ephemerisManager.getHolidayDescription(holiday);
    }

    @ActionDoc(text = "gets the number of days between today and a given holiday")
    public static long getDaysUntil(String searchedHoliday) {
        return getDaysUntil(ZonedDateTime.now(), searchedHoliday);
    }

    @ActionDoc(text = "gets the number of days between today and a given holiday")
    public static long getDaysUntil(ZonedDateTime day, String searchedHoliday) {
        return EphemerisActionService.ephemerisManager.getDaysUntil(day, searchedHoliday);
    }

    @ActionDoc(text = "gets the number of days between today and a given holiday from a given userfile")
    public static long getDaysUntil(String searchedHoliday, String filename) {
        return getDaysUntil(ZonedDateTime.now(), searchedHoliday, filename);
    }

    @ActionDoc(text = "gets the number of days between a given day and a given holiday from a given userfile")
    public static long getDaysUntil(ZonedDateTime day, String searchedHoliday, String filename) {
        try {
            return EphemerisActionService.ephemerisManager.getDaysUntil(day, searchedHoliday, filename);
        } catch (FileNotFoundException e) {
            LOGGER.error("Error reading holiday user file {} : {}", filename, e.getMessage());
            return -1;
        }
    }

}
