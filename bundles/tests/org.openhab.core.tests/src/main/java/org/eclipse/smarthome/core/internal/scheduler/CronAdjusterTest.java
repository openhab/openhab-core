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
package org.eclipse.smarthome.core.internal.scheduler;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 *
 * @author Peter Kriens - initial contribution and API
 * @author Simon Kaufmann - adapted to Java 8
 * @author Hilbrand Bouwkamp - refactored to run as parameterized unit test and added missing cases to complete coverage
 */
@RunWith(Parameterized.class)
public class CronAdjusterTest {

    private static final String JAN_1ST_2000 = "2000-01-01T00:00:00";
    private static final String JAN_1ST_2015 = "2015-01-01T00:00:00";

    @Parameter(value = 0)
    public String in;
    @Parameter(value = 1)
    public String cron;
    @Parameter(value = 2)
    public String[] outs;

    @Parameters(name = "CronAdjusterTest pattern: {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { { JAN_1ST_2015, "@reboot", new String[] { "2200-01-01T00:00" } },
                { JAN_1ST_2015, "@hourly",
                        new String[] { "2015-01-01T01:00", "2015-01-01T02:00", "2015-01-01T03:00",
                                "2015-01-01T04:00" } },
                { JAN_1ST_2015, "@daily",
                        new String[] { "2015-01-02T00:00", "2015-01-03T00:00", "2015-01-04T00:00",
                                "2015-01-05T00:00" } },
                { JAN_1ST_2015, "@weekly",
                        new String[] { "2015-01-05T00:00", "2015-01-12T00:00", "2015-01-19T00:00", "2015-01-26T00:00",
                                "2015-02-02T00:00" } },
                { "2015-01-20T23:59:59", "@monthly",
                        new String[] { "2015-02-01T00:00", "2015-03-01T00:00", "2015-04-01T00:00",
                                "2015-05-01T00:00" } },
                { JAN_1ST_2015, "@annually",
                        new String[] { "2016-01-01T00:00", "2017-01-01T00:00", "2018-01-01T00:00" } },
                { JAN_1ST_2015, "@yearly",
                        new String[] { "2016-01-01T00:00", "2017-01-01T00:00", "2018-01-01T00:00" } },

                // Last weekday
                { JAN_1ST_2015, "0 15 10 LW * ?",
                        new String[] { "2015-01-30T10:15", "2015-02-27T10:15", "2015-03-31T10:15", "2015-04-30T10:15",
                                "2015-05-29T10:15" } },

                { JAN_1ST_2015, "0 15 10 WL * ?",
                        new String[] { "2015-01-30T10:15", "2015-02-27T10:15", "2015-03-31T10:15", "2015-04-30T10:15",
                                "2015-05-29T10:15" } },

                { JAN_1ST_2015, "0 15 10 3W * ?",
                        new String[] { "2015-01-02T10:15", "2015-02-03T10:15", "2015-03-03T10:15", "2015-04-03T10:15",
                                "2015-05-04T10:15" } },

                // Fire at 10.15 every 10 days every month, starting on the first day of the month.
                { JAN_1ST_2015, "0 15 10 1/10 * ?",
                        new String[] { "2015-01-01T10:15", "2015-01-11T10:15", "2015-01-21T10:15", "2015-01-31T10:15",
                                "2015-02-01T10:15" } },

                // Fire at 10:15am on the second Friday of every month
                { JAN_1ST_2015, "0 15 10 ? * FRI#2",
                        new String[] { "2015-01-09T10:15", "2015-02-13T10:15", "2015-03-13T10:15" } },

                // Fire at 10:15am on the last Friday of every month
                { JAN_1ST_2015, "0 15 10 ? * 5L",
                        new String[] { "2015-01-30T10:15", "2015-02-27T10:15", "2015-03-27T10:15" } },

                // Fire at 10:15am on the last day of every month
                { JAN_1ST_2015, "0 15 10 L * ?",
                        new String[] { "2015-01-31T10:15", "2015-02-28T10:15", "2015-03-31T10:15" } },

                // Fire at 10:15am on the 15th day of every month
                { JAN_1ST_2015, "0 15 10 15 * ?",
                        new String[] { "2015-01-15T10:15", "2015-02-15T10:15", "2015-03-15T10:15" } },

                // Fire at 10:15am every Monday, Tuesday, Wednesday
                { JAN_1ST_2015, "0 15 10 ? * MON-WED",
                        new String[] { "2015-01-05T10:15", "2015-01-06T10:15", "2015-01-07T10:15",
                                "2015-01-12T10:15" } },

                // Fire at 2:10pm and at 2:44pm every Wednesday in the month of January.
                { JAN_1ST_2015, "0 10,44 14 ? 1 WED",
                        new String[] { "2015-01-07T14:10", "2015-01-07T14:44", "2015-01-14T14:10" } },

                // Fire every minute starting at 2pm and ending at 2:02pm, every day
                { JAN_1ST_2015, "0 0-2 14 * * ?",
                        new String[] { "2015-01-01T14:00", "2015-01-01T14:01", "2015-01-01T14:02", "2015-01-02T14:00",
                                "2015-01-02T14:01", "2015-01-02T14:02" } },
                { JAN_1ST_2015, "0 0/15 14,18 * * ?",
                        new String[] { "2015-01-01T14:00", "2015-01-01T14:15", "2015-01-01T14:30", "2015-01-01T14:45",
                                "2015-01-01T18:00", "2015-01-01T18:15" } },

                { JAN_1ST_2015, "0 0 12 * * ?", new String[] { "2015-01-01T12:00" } },
                { JAN_1ST_2015, "0 15 10 ? * *", new String[] { "2015-01-01T10:15", "2015-01-02T10:15" } },
                { JAN_1ST_2015, "0 15 10 * * ?", new String[] { "2015-01-01T10:15", "2015-01-02T10:15" } },
                { JAN_1ST_2015, "0 15 10 * * ? *", new String[] { "2015-01-01T10:15", "2015-01-02T10:15" } },
                { JAN_1ST_2015, "0 15 10 * * ? 2015", new String[] { "2015-01-01T10:15", "2015-01-02T10:15" } },
                { JAN_1ST_2015, "0 * 14 * * ?",
                        new String[] { "2015-01-01T14:00", "2015-01-01T14:01", "2015-01-01T14:02" } },
                { JAN_1ST_2015, "0 0/5 14 * * ?",
                        new String[] { "2015-01-01T14:00", "2015-01-01T14:05", "2015-01-01T14:10" } },
                { JAN_1ST_2015, "0 0/30 14,18 * * ?",
                        new String[] { "2015-01-01T14:00", "2015-01-01T14:30", "2015-01-01T18:00" } },
                { JAN_1ST_2015, "0 0-2 14 * * ?",
                        new String[] { "2015-01-01T14:00", "2015-01-01T14:01", "2015-01-01T14:02",
                                "2015-01-02T14:00" } },

                { JAN_1ST_2015, "0 0 0 ? * SAT", new String[] { "2015-01-03T00:00" } },
                { JAN_1ST_2000, "10-14/2 * * * * *",
                        new String[] { "2000-01-01T00:00:10", "2000-01-01T00:00:12", "2000-01-01T00:00:14",
                                "2000-01-01T00:01:10" } },
                { JAN_1ST_2000, "1/15,3/15 * * * * *",
                        new String[] { "2000-01-01T00:00:01", "2000-01-01T00:00:03", "2000-01-01T00:00:16",
                                "2000-01-01T00:00:18" } },

                { JAN_1ST_2000, "0 0 0 1 FEB,APR,JUN ?",
                        new String[] { "2000-02-01T00:00", "2000-04-01T00:00", "2000-06-01T00:00" } },
                { JAN_1ST_2000, "0 0 0 1 FEB ?",
                        new String[] { "2000-02-01T00:00", "2001-02-01T00:00", "2002-02-01T00:00" } },
                { JAN_1ST_2000, "0 0 0 * FEB ?",
                        new String[] { "2000-02-01T00:00", "2000-02-02T00:00", "2000-02-03T00:00" } },

                { JAN_1ST_2000, "15 2 * * * * 2001", new String[] { "2001-01-01T00:02:15" } },
                { JAN_1ST_2000, "*/5 * * * * *",
                        new String[] { "2000-01-01T00:00:05", "2000-01-01T00:00:10", "2000-01-01T00:00:15" } },
                { JAN_1ST_2000, "3/5 * * * * *",
                        new String[] { "2000-01-01T00:00:03", "2000-01-01T00:00:08", "2000-01-01T00:00:13",
                                "2000-01-01T00:00:18" } },
                { JAN_1ST_2000, "3-13/5 * * * * *",
                        new String[] { "2000-01-01T00:00:03", "2000-01-01T00:00:08", "2000-01-01T00:00:13",
                                "2000-01-01T00:01:03" }, },

                { "2300-01-01T00:00:00", "0 0 0 1 1 *",
                        new String[] { "2301-01-01T00:00", "2302-01-01T00:00", "2303-01-01T00:00" }, },

                { "2019-10-10T00:00:00", "0 0 0 1 1 * 2020-2022", new String[] { "2020-01-01T00:00", "2021-01-01T00:00",
                        "2022-01-01T00:00", "2200-01-01T00:00" }, } });
    }

    @Test(timeout = 1000)
    public void testCronExpression() {
        CronAdjuster cronAdjuster = new CronAdjuster(cron);
        Temporal ldt = LocalDateTime.parse(in);

        for (String out : outs) {
            ldt = ldt.with(cronAdjuster);
            assertThat("CronAdjuster did return expected next cron string", ldt.toString(), equalTo(out));
        }
    }
}
