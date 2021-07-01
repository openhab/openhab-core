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
package org.openhab.core.rulestest;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.items.GroupItem;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.library.items.ContactItem;
import org.eclipse.smarthome.core.library.items.DateTimeItem;
import org.eclipse.smarthome.core.library.items.DimmerItem;
import org.eclipse.smarthome.core.library.items.ImageItem;
import org.eclipse.smarthome.core.library.items.LocationItem;
import org.eclipse.smarthome.core.library.items.NumberItem;
import org.eclipse.smarthome.core.library.items.PlayerItem;
import org.eclipse.smarthome.core.library.items.RollershutterItem;
import org.eclipse.smarthome.core.library.items.StringItem;
import org.eclipse.smarthome.core.library.items.SwitchItem;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.link.ItemChannelLink;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.model.core.ModelRepository;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for user-space tests of openHAB automation rules.
 *
 * @author Florian Schmidt - Initial contribution
 *
 */
@NonNullByDefault
public abstract class RuleTest extends JavaOSGiTest {
    private Logger logger = LoggerFactory.getLogger(RuleTest.class);
    private static final String STARTUP_RULE = "rule \"Integration Test startup rule\"\n" + "when\n"
            + "    System started\n" + "then\n" + "    startupFinished.sendCommand(ON)\n" + "end";
    private static final String STARTUP_ITEM = "Switch startupFinished";

    @Nullable
    protected ItemRegistry itemRegistry;

    @Before
    public void setUp() {
        assertConfigVariablesSet();
        itemRegistry = getService(ItemRegistry.class);
        assertNotNull(itemRegistry);
        injectTestModels();

        List<String> requiredItems = new ArrayList<>();
        requiredItems.add("startupFinished");
        requiredItems.addAll(requestedItems());

        waitForAssert(() -> requiredItems.forEach(
                item -> assertThat("Requested item '" + item + "' does not exist!", getItem(item), not(nullValue()))));
        waitForAssert(() -> assertThat(getItem("startupFinished").getState(), is(OnOffType.ON)), 60000, 100);

        disableItemChannelAutoupdate();
    }

    private void assertConfigVariablesSet() {
        Arrays.asList(
            "openhab.conf",
            "smarthome.configdir",
            "org.quartz.properties",
            "smarthome.servicecfg"
        ).forEach((config) -> {
            assertThat(config + " should not be unset for these tests to run", System.getProperty(config), not(isEmptyString()));
        });
    }

    private void disableItemChannelAutoupdate() {
        ItemChannelLinkRegistry itemChannelLinkRegistry = getService(ItemChannelLinkRegistry.class);
        Collection<ItemChannelLink> itemChannelLinks = itemChannelLinkRegistry.getAll();
        for (ItemChannelLink itemChannelLink : itemChannelLinks) {
            logger.debug("Removing item channel link:" + itemChannelLink.getUID());
            // WTF - the ItemChannelLink is returned from getAll and #get() returns the same item, but remove returns null as if the item does not exist
            // working around this stupidness by adding the ItemChannelLink before removing it, even if that sounds stupid.
            itemChannelLinkRegistry.add(itemChannelLink);
            itemChannelLinkRegistry.remove(itemChannelLink.getUID());
        }
        assertTrue(itemChannelLinkRegistry.getAll().isEmpty());
    }

    private void injectTestModels() {
        ModelRepository modelRepository = getService(ModelRepository.class);
        assertNotNull(modelRepository);

        modelRepository.addOrRefreshModel("startup_finish_test.rules",
                new ByteArrayInputStream(STARTUP_RULE.getBytes()));
        modelRepository.addOrRefreshModel("startup_finish_test.items",
                new ByteArrayInputStream(STARTUP_ITEM.getBytes()));
    }
    
    protected void runTimerWhichRunsIn(long in, TemporalUnit unit) throws SchedulerException {
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        LocalDateTime executeIn = LocalDateTime.now().plus(in, unit).withNano(0);
        boolean jobExists = waitFor(() -> timedJob(executeIn, scheduler) != null);

        if (!jobExists) {
            logger.info("Could not find any job that starts in " + in + " " +
                unit + " therefore not executing any.");
            return;
        }
        JobKey timeJob = timedJob(executeIn, scheduler);
        if (timeJob == null) {
            return;
        }
        scheduler.triggerJob(timeJob);
        scheduler.deleteJob(timeJob);
    }

    @Nullable
    private JobKey timedJob(LocalDateTime executeIn, Scheduler scheduler) {
        Set<JobKey> jobKeys;
        try {
            jobKeys = scheduler.getJobKeys(GroupMatcher.anyGroup());
        } catch (SchedulerException e) {
            return null;
        }
        for (JobKey jobKey : jobKeys) {
            if (jobKey.getName().startsWith(executeIn.toString())) {                    
                return jobKey;
            }
        }
        return null;
    }

    protected void assertThatItemState(Item item, Matcher<State> matcher) {
        waitForAssert(() -> assertThat(item.getState(), matcher), 1000, 50);
    }

    protected Item getItem(String itemName) {
        Item item = itemRegistry.get(itemName);
        if (item == null) {
            throw new IllegalStateException("Requested item " + itemName + " which does not exist.");
        }
        return item;
    }
    
    private <T extends Item> T itemAs(String itemName, Class<T> clazz) {
        Item item = getItem(itemName);
        assertThat(item, instanceOf(clazz));
        
        return clazz.cast(item);
    }

    protected SwitchItem getSwitchItem(String itemName) {
        return itemAs(itemName, SwitchItem.class);
    }
    
    protected DimmerItem getDimmerItem(String itemName) {
        return itemAs(itemName, DimmerItem.class);
    }

    protected NumberItem getNumberItem(String itemName) {
        return itemAs(itemName, NumberItem.class);
    }
    
    protected GroupItem getGroupItem(String itemName) {
        return itemAs(itemName, GroupItem.class);
    }
    
    protected ContactItem getContactItem(String itemName) {
        return itemAs(itemName, ContactItem.class);
    }

    protected DateTimeItem getDateTimeItem(String itemName) {
        return itemAs(itemName, DateTimeItem.class);
    }

    protected ImageItem getImageItem(String itemName) {
        return itemAs(itemName, ImageItem.class);
    }

    protected LocationItem getLocationItem(String itemName) {
        return itemAs(itemName, LocationItem.class);
    }

    protected PlayerItem getPlayerItem(String itemName) {
        return itemAs(itemName, PlayerItem.class);
    }

    protected RollershutterItem getRollershutterItem(String itemName) {
        return itemAs(itemName, RollershutterItem.class);
    }

    protected StringItem getStringItem(String itemName) {
        return itemAs(itemName, StringItem.class);
    }

    protected List<String> requestedItems() {
        return Collections.emptyList();
    }
}
