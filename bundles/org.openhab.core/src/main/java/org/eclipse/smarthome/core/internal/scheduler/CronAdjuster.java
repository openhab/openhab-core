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
package org.eclipse.smarthome.core.internal.scheduler;

import java.time.DayOfWeek;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.scheduler.SchedulerTemporalAdjuster;

/**
 * This class creates a {@link TemporalAdjuster} that takes a temporal and adjust it to the next deadline based on a
 * cron specification.
 *
 * @See http://www.cronmaker.com/
 * @See http://www.quartz-scheduler.org/documentation/quartz-1.x/tutorials/crontrigger
 * @author Peter Kriens - initial contribution and API
 * @author Hilbrand Bouwkamp - code cleanup
 */
@NonNullByDefault
class CronAdjuster implements SchedulerTemporalAdjuster {

    /**
     * A function interface that we use to check a Temporal to see if it matches a part of the specification. These
     * checkers are combined in and and or expressions.
     */
    @FunctionalInterface
    interface Checker {
        boolean matches(Temporal t);
    }

    private static final Pattern WEEKDAY_PATTERN = Pattern
            .compile("(?<day>\\d+|MON|TUE|WED|THU|FRI|SAT|SUN)(#(?<nr>\\d+)|(?<l>L))?", Pattern.CASE_INSENSITIVE);
    private static final String[] MONTHS2 = { "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT",
            "NOV", "DEC" };
    private static final Map<String, Integer> MONTHS = IntStream.range(0, MONTHS2.length)
            .mapToObj(i -> new SimpleEntry<String, Integer>(MONTHS2[i], i))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    private static final String[] WEEK_DAYS_STRINGS = { "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN" };
    private static final Map<String, Integer> WEEK_DAYS = IntStream.range(0, WEEK_DAYS_STRINGS.length)
            .mapToObj(i -> new SimpleEntry<String, Integer>(WEEK_DAYS_STRINGS[i], i))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    private final List<Field> fields = new ArrayList<>(7);
    private final Map<String, String> environmentMap;
    private final boolean reboot;

    /**
     * Constructs the class with a cron specification. containing variables and a cron expression at the last line.
     */
    public CronAdjuster(String specification) {
        int x = 1;
        final String entries[] = specification.split("[\n\r]+");
        environmentMap = parseEnvironment(entries);

        String cronExpression = entries[entries.length - 1].trim();

        reboot = cronExpression.equals("@reboot");

        if (cronExpression.startsWith("@")) {
            cronExpression = preDeclared(cronExpression);
        }

        final String parts[] = cronExpression.trim().toUpperCase().split("\\s+");

        if (parts.length < 6 || parts.length > 7) {
            throw new IllegalArgumentException(
                    String.format("Invalid cron expression, too %s fields. 6 or 7 (with year) expected but was: '%s'",
                            (parts.length < 6 ? "little" : "many"), cronExpression));
        }
        // Parse parts and add to the fields list. The order added should be maintained!
        if (parts.length > 6) {
            parseAndAdd(cronExpression, parts[6], ChronoField.YEAR);
        }
        parse(cronExpression, parts[5], ChronoField.DAY_OF_WEEK, WEEK_DAYS);
        parse(cronExpression, parts[4], ChronoField.MONTH_OF_YEAR, MONTHS);
        parseAndAdd(cronExpression, parts[3], ChronoField.DAY_OF_MONTH);
        parseAndAdd(cronExpression, parts[2], ChronoField.HOUR_OF_DAY);
        parseAndAdd(cronExpression, parts[1], ChronoField.MINUTE_OF_HOUR);
        parseAndAdd(cronExpression, parts[0], ChronoField.SECOND_OF_MINUTE);
    }

    /**
     * @return Returns a map with variables passed in the specification.
     */
    public Map<String, String> getEnv() {
        return environmentMap;
    }

    /**
     * @return Returns true if this cron has been initialized with &#64;reboot.
     */
    public boolean isReboot() {
        return reboot;
    }

    @Override
    public boolean isDone(Temporal temporal) {
        return checkMaxYear(temporal);
    }

    /**
     * Parses the environment variables in the entries. The last entry in ignored as it contains the cron expression.
     *
     * @param entries entries to parse
     * @return Map with environment variables
     */
    private Map<String, String> parseEnvironment(String[] entries) {
        final Map<String, String> map = new HashMap<String, String>();

        if (entries.length > 1) {
            // Skip the last entry it contains the cron expression no varables.
            for (int i = 0; i < entries.length - 1; i++) {
                final String entry = entries[i];

                if (entry.startsWith("#") || entry.isEmpty()) {
                    continue;
                }

                final int n = entry.indexOf('=');
                if (n >= 0) {
                    final String key = entry.substring(0, n).trim();
                    final String value = entry.substring(n + 1).trim();
                    map.put(key, value);
                } else {
                    map.put(entry.trim(), Boolean.TRUE.toString());
                }
            }
            return map;
        } else {
            return Collections.emptyMap();
        }
    }

    /**
     * Convert predeclared words into their representing cron expression.
     *
     * <pre>
     * &#64;yearly (or &#64;annually)  Run once a year at midnight on the morning of January 1                      0 0 0 1 1 *
     * &#64;monthly                Run once a month at midnight on the morning of the first day of the month    0 0 0 1 * *
     * &#64;weekly                 Run once a week at midnight on Sunday morning                                0 0 0 * * 0
     * &#64;daily                  Run once a day at midnight                                                   0 0 0 * * *
     * &#64;hourly                 Run once an hour at the beginning of the hour                                0 0 * * * *
     * &#64;reboot                 Run at startup                                                               0 0 0 1 1 ? 1900
     * </pre>
     */
    private String preDeclared(String expression) {
        switch (expression) {
            case "@annually":
            case "@yearly":
                return "0 0 0 1 1 *";

            case "@monthly":
                return "0 0 0 1 * *";

            case "@weekly":
                return "0 0 0 ? * MON";

            case "@daily":
                return "0 0 0 * * ?";

            case "@hourly":
                return "0 0 * * * ?";

            case "@reboot":
                return "0 0 0 1 1 ? 1900";
            default:
                throw new IllegalArgumentException("Unrecognized @ expression: '" + expression + "'");
        }
    }

    /**
     * A cron part consists of a number of sub expressions separated by a comma.
     * The sub expressions are parsed and combined. If this is a pattern that sets the time
     * a new {@link Field} object is created and added to the fields list.
     *
     * @param cronExpression The cronExpresion itself
     * @param part the part to be parsed
     * @param chronoField the chronoField is part belongs to
     */
    private void parseAndAdd(String cronExpression, String part, ChronoField chronoField) {
        parse(cronExpression, part, chronoField, Collections.emptyMap());
    }

    /**
     * A cron part consists of a number of sub expressions separated by a comma.
     * The sub expressions are parsed and combined. If this is a pattern that sets the time
     * a new {@link Field} object is created and added to the fields list.
     *
     * @param cronExpression The cronExpresion itself
     * @param part the part to be parsed
     * @param chronoField the chronoField is part belongs to
     * @param names a map with chronoField names that can be part of the pattern and are mapped to numbers
     */
    private void parse(String cronExpression, String part, ChronoField chronoField, Map<String, Integer> names) {
        // Check wild card.
        if ("*".equals(part) || "?".equals(part)) {
            return; // No field needed all values accepted
        }

        final List<Checker> checkers = new ArrayList<>();
        // Parse each sub expression
        final String[] split = part.split(",");
        for (String sub : split) {
            checkers.add(parseSub(cronExpression, chronoField, sub, names));
        }

        // If this is the year check, we create a conjunction with a check for the maximum year
        if (chronoField == ChronoField.YEAR) {
            checkers.add(CronAdjuster::checkMaxYear);
        }
        fields.add(new Field(chronoField, or(checkers)));
    }

    /*
     * Parse a sub expression.
     */
    private Checker parseSub(String cronExpression, ChronoField chronoField, String sub, Map<String, Integer> names) {
        // Max and min for the current type
        final int min = (int) chronoField.range().getMinimum();
        final int max = (int) chronoField.range().getMaximum();

        if (chronoField == ChronoField.DAY_OF_WEEK) {
            if ("L".equals(sub)) {
                return parseSub(cronExpression, chronoField, "SUN", names);
            } else {
                final Matcher m = WEEKDAY_PATTERN.matcher(sub);
                if (m.matches()) {
                    final int day = parseInt(cronExpression, chronoField, m.group("day"), min, max, names);
                    final Checker c = temporal -> temporal.get(ChronoField.DAY_OF_WEEK) == day;

                    if (m.group("nr") != null) {
                        final int n = parseInt(cronExpression, chronoField, m.group("nr"));
                        return and(c, temporal -> isNthWeekDayInMonth(temporal, n));

                    } else if (m.group("l") != null) {
                        return and(c, CronAdjuster::isLastOfThisWeekDayInMonth);
                    } else {
                        return c;
                    }
                }
                // No matches fall through to parse as a normal expression
            }
        } else if (chronoField == ChronoField.DAY_OF_MONTH) {
            if ("L".equals(sub)) {
                return CronAdjuster::isLastDayInMonth;
            } else if ("LW".equals(sub) || "WL".equals(sub)) {
                return CronAdjuster::isLastWorkingDayInMonth;
            } else if (sub.endsWith("W")) {
                int n = parseInt(cronExpression, chronoField, sub.substring(0, sub.length() - 1));
                return (temporal) -> isNearestWorkDay(temporal, n);
            }
            // fall through, it is a normal expression
        }

        // Parse range and step expressions
        final String[] increments = sub.split("/");
        final int[] range = parseRange(cronExpression, chronoField, increments[0], min, max, names);

        if (increments.length == 2) {
            // we had a / expression
            final int increment = parseInt(cronExpression, chronoField, increments[1]);

            if (range[0] == range[1]) {
                range[1] = max;
            }
            return temporal -> {
                final int n = temporal.get(chronoField);

                return n >= range[0] && n <= range[1] && ((n - range[0]) % increment) == 0;
            };
        }

        // simple range/value check
        return temporal -> {
            final int n = temporal.get(chronoField);

            return n >= range[0] && n <= range[1];
        };
    }

    /**
     * This is the # syntax. We must check that the given weekday is the nth one
     * in the current month. So we take the day of the month and divide it by 7.
     *
     * @param temporal temporal to check
     * @param nDayInMonth the nth day in the current month to check
     * @return true if temporal matches nth day in month
     */
    private static boolean isNthWeekDayInMonth(Temporal temporal, int nDayInMonth) {
        final int day = temporal.get(ChronoField.DAY_OF_MONTH);
        final int occurrences = 1 + (day - 1) / 7;

        return nDayInMonth == occurrences;
    }

    /**
     * @param temporal temporal to check
     * @return true if temporal is the last week day in this month. I.e. the last Saturday
     */
    private static boolean isLastOfThisWeekDayInMonth(Temporal temporal) {
        final int day = temporal.get(ChronoField.DAY_OF_MONTH);
        final int max = (int) ChronoField.DAY_OF_MONTH.rangeRefinedBy(temporal).getMaximum();

        return day + 7 > max;
    }

    /**
     * @param temporal temporal to check
     * @return true if temporal is the last day in the month
     */
    private static boolean isLastDayInMonth(Temporal temporal) {
        final int day = temporal.get(ChronoField.DAY_OF_MONTH);
        final int max = (int) ChronoField.DAY_OF_MONTH.rangeRefinedBy(temporal).getMaximum();

        return day == max;
    }

    /**
     * @param temporal temporal to check
     * @return true if temporal is the last working day in the month
     */
    private static boolean isLastWorkingDayInMonth(Temporal temporal) {
        final int day = temporal.get(ChronoField.DAY_OF_MONTH);
        final DayOfWeek type = DayOfWeek.of(temporal.get(ChronoField.DAY_OF_WEEK));
        final int max = (int) ChronoField.DAY_OF_MONTH.rangeRefinedBy(temporal).getMaximum();

        switch (type) {
            case MONDAY:
            case TUESDAY:
            case WEDNESDAY:
            case THURSDAY:
                return day == max;

            case FRIDAY:
                return day + 2 >= max;

            default:
            case SATURDAY:
            case SUNDAY:
                return false;
        }
    }

    /**
     * Check for the nearest working day. E.g. 15W is the nearest working day around the 15th.
     *
     * @param temporal temporal to check
     * @return true if temporal is nearest to working day
     */
    static boolean isNearestWorkDay(Temporal temporal, int target) {
        final int day = temporal.get(ChronoField.DAY_OF_MONTH);
        final DayOfWeek type = DayOfWeek.of(temporal.get(ChronoField.DAY_OF_WEEK));

        switch (type) {
            case MONDAY:
                return day == target // the actual day
                        || day == target + 1 // target was on a Sunday
                        || (day == target + 2 && day == 3); // target was Saturday 1

            case TUESDAY:
            case WEDNESDAY:
            case THURSDAY:
                return day == target;

            case FRIDAY:
                return day == target || day + 1 == target;

            // not a work day
            default:
            case SATURDAY:
            case SUNDAY:
                return false;
        }
    }

    /**
     * A check that we do not go ballistic with the year
     */
    private static boolean checkMaxYear(Temporal temporal) {
        return temporal.get(ChronoField.YEAR) >= 2200;
    }

    private int[] parseRange(String cronExpression, ChronoField chronoField, String range, int min, int max,
            Map<String, Integer> names) {
        final int[] r = { 0, max };
        if ("*".equals(range)) {
            return r;
        }

        final String parts[] = range.split("-");
        r[0] = r[1] = parseInt(cronExpression, chronoField, parts[0], min, max, names);
        if (parts.length == 2) {
            r[1] = parseInt(cronExpression, chronoField, parts[1], min, max, names);
        }

        if (r[0] < min) {
            throw new IllegalArgumentException(String.format(
                    "Value too small in range in cron expression '%s' in field '%s': value %s, minimum: %s",
                    cronExpression, chronoField, r[0], min));
        }
        if (r[1] > max) {
            throw new IllegalArgumentException(String.format(
                    "Value too high in range in cron expression '%s' in field '%s': value %s, minimum: %s",
                    cronExpression, chronoField, r[1], max));

        }
        if (r[0] > r[1]) {
            throw new IllegalArgumentException(String.format(
                    "Minimum higher than maximum range in cron expression '%s' in field '%s': min: %s, max: %s",
                    cronExpression, chronoField, r[0], r[1]));
        }

        return r;
    }

    private int parseInt(String cronExpression, ChronoField chronoField, String name, int min, int max,
            Map<String, Integer> names) {
        if (name.isEmpty()) {
            return 0;
        }

        final Integer nameIndex = names.get(name);
        if (nameIndex == null) {
            return parseInt(cronExpression, chronoField, name);
        } else {
            return min + nameIndex;
        }
    }

    private int parseInt(String cronExpression, ChronoField chronoField, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    String.format("Value not a number in cron expression '%s' in field '%s': %s", cronExpression,
                            chronoField, e.getMessage()));
        }
    }

    @Override
    public Temporal adjustInto(@Nullable Temporal temporal) {

        // Never match the actual time, so since our basic
        // unit is seconds, we add one second.
        Temporal ret = temporal.plus(1, ChronoUnit.SECONDS);

        // We loop through the fields until they all match. If
        // one of them does not match, its type is incremented
        // and all lower fields are reset to their minimum. And
        // we start over with this new time.

        int index = 0;
        int length = fields.size();

        while (index < length) {
            final Field field = fields.get(index);

            final Temporal out = field.isOk(ret);

            if (out == null) {
                index++;
            } else {
                ret = out;
                index = 0;
            }
        }

        // All fields match!
        return ret;
    }

    /**
     * Helper to create an or expression Checkers of a list of checkers.
     */
    private Checker or(List<Checker> checkers) {
        return checkers.size() > 1 //
                ? temporal -> checkers.stream().anyMatch(c -> c.matches(temporal))
                : checkers.get(0);
    }

    /**
     * Helper to create an and expression of 2 checkers.
     */
    private Checker and(Checker a, Checker b) {
        return temporal -> a.matches(temporal) && b.matches(temporal);
    }

    /**
     * Maintains the type and the combined checker. It can verify if a specific part of the temporal is ok, and if not,
     * it will reset it to the next higher temporal with the lower fields set to their minimum value.
     */
    private static class Field {
        final ChronoField type;
        final Checker checker;

        public Field(ChronoField type, Checker checker) {
            this.type = type;
            this.checker = checker;
        }

        @Nullable
        Temporal isOk(Temporal t) {
            if (checker.matches(t)) {
                return null;
            }

            Temporal out = t.plus(1, type.getBaseUnit());

            // Fall-through switch case. for example if type is year all cases below must also be handled.
            switch (type) {
                case YEAR:
                    out = out.with(ChronoField.MONTH_OF_YEAR, 1);
                case MONTH_OF_YEAR:
                    out = out.with(ChronoField.DAY_OF_MONTH, 1);
                case DAY_OF_WEEK:
                case DAY_OF_MONTH:
                    out = out.with(ChronoField.HOUR_OF_DAY, 0);
                case HOUR_OF_DAY:
                    out = out.with(ChronoField.MINUTE_OF_HOUR, 0);
                case MINUTE_OF_HOUR:
                    out = out.with(ChronoField.SECOND_OF_MINUTE, 0);
                case SECOND_OF_MINUTE:
                    return out;
                default:
                    throw new IllegalArgumentException("Invalid field type " + type);
            }
        }

        @Override
        public String toString() {
            return "Field [type=" + type + "]";
        }
    }
}
