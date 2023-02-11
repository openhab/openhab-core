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
package org.openhab.core.automation.internal.module.handler;

import static java.util.Map.entry;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.automation.Trigger;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.items.DateTimeItem;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.scheduler.CronScheduler;
import org.osgi.framework.BundleContext;

/**
 * Basic test cases for {@link DateTimeTriggerHandler}
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
public class DateTimeTriggerHandlerTest {
    private @NonNullByDefault({}) @Mock Trigger mockTrigger;
    private @NonNullByDefault({}) @Mock ItemRegistry mockItemRegistry;
    private @NonNullByDefault({}) @Mock BundleContext mockBundleContext;
    private @NonNullByDefault({}) @Mock CronScheduler mockScheduler;

    private static final String ITEM_NAME = "myItem";
    private final DateTimeItem item = new DateTimeItem(ITEM_NAME);

    @BeforeEach
    public void setup() throws ItemNotFoundException {
        when(mockItemRegistry.getItem(ITEM_NAME)).thenReturn(item);
        when(mockTrigger.getConfiguration())
                .thenReturn(new Configuration(Map.ofEntries(entry(DateTimeTriggerHandler.CONFIG_ITEM_NAME, ITEM_NAME),
                        entry(DateTimeTriggerHandler.CONFIG_TIME_ONLY, false))));
    }

    @Test
    public void testSameTimeZone() {
        ZonedDateTime zdt = ZonedDateTime.of(2022, 8, 11, 0, 0, 0, 0, ZoneId.systemDefault());
        item.setState(new DateTimeType(zdt));
        DateTimeTriggerHandler handler = new DateTimeTriggerHandler(mockTrigger, mockScheduler, mockItemRegistry,
                mockBundleContext);

        verify(mockScheduler).schedule(eq(handler), eq("0 0 0 11 8 * 2022"));
    }

    @Test
    public void testDifferentTimeZone() {
        ZonedDateTime zdt = ZonedDateTime.of(2022, 8, 11, 0, 0, 0, 0, ZoneId.systemDefault())
                .withZoneSameInstant(ZoneOffset.ofTotalSeconds(12345));
        item.setState(new DateTimeType(zdt));
        DateTimeTriggerHandler handler = new DateTimeTriggerHandler(mockTrigger, mockScheduler, mockItemRegistry,
                mockBundleContext);

        verify(mockScheduler).schedule(eq(handler), eq("0 0 0 11 8 * 2022"));
    }
}
