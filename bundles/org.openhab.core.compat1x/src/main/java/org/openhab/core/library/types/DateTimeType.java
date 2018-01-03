/**
 * Copyright (c) 2015-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.core.library.types;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.openhab.core.types.Command;
import org.openhab.core.types.PrimitiveType;
import org.openhab.core.types.State;

/**
 *
 * @author Kai Kreuzer - Initial contribution
 */
public class DateTimeType implements PrimitiveType, State, Command {

    public static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String DATE_PATTERN_WITH_TZ = "yyyy-MM-dd'T'HH:mm:ssz";

    protected Calendar calendar;

    public DateTimeType() {
        this(Calendar.getInstance());
    }

    public DateTimeType(Calendar calendar) {
        this.calendar = calendar;
    }

    public DateTimeType(String calendarValue) {
        Date date = null;

        try {
            try {
                date = new SimpleDateFormat(DATE_PATTERN_WITH_TZ).parse(calendarValue);
            } catch (ParseException fpe2) {
                date = new SimpleDateFormat(DATE_PATTERN).parse(calendarValue);
            }
        } catch (ParseException fpe) {
            throw new IllegalArgumentException(calendarValue + " is not in a valid format.", fpe);
        }

        if (date != null) {
            calendar = Calendar.getInstance();
            calendar.setTime(date);
        }
    }

    public Calendar getCalendar() {
        return calendar;
    }

    public static DateTimeType valueOf(String value) {
        return new DateTimeType(value);
    }

    @Override
    public String format(String pattern) {
        try {
            return String.format(pattern, calendar);
        } catch (NullPointerException npe) {
            return new SimpleDateFormat(DATE_PATTERN).format(calendar.getTime());
        }
    }

    public String format(Locale locale, String pattern) {
        return String.format(locale, pattern, calendar);
    }

    @Override
    public String toString() {
        return new SimpleDateFormat(DATE_PATTERN).format(calendar.getTime());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((calendar == null) ? 0 : calendar.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DateTimeType other = (DateTimeType) obj;
        if (calendar == null) {
            if (other.calendar != null) {
                return false;
            }
        } else if (!calendar.equals(other.calendar)) {
            return false;
        }
        return true;
    }

}
