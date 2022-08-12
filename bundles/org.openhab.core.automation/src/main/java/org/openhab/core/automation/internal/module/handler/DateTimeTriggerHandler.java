/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.ModuleHandlerCallback;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.handler.BaseTriggerModuleHandler;
import org.openhab.core.automation.handler.TimeBasedTriggerHandler;
import org.openhab.core.automation.handler.TriggerHandlerCallback;
import org.openhab.core.config.core.ConfigParser;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemCommandEvent;
import org.openhab.core.items.events.ItemEvent;
import org.openhab.core.items.events.ItemStateEvent;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.scheduler.CronAdjuster;
import org.openhab.core.scheduler.CronScheduler;
import org.openhab.core.scheduler.ScheduledCompletableFuture;
import org.openhab.core.scheduler.SchedulerRunnable;
import org.openhab.core.types.Type;
import org.openhab.core.types.UnDefType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an ModuleHandler implementation for Triggers which trigger the rule
 * based on a {@link org.openhab.core.library.types.DateTimeType} stored in an item
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class DateTimeTriggerHandler extends BaseTriggerModuleHandler
        implements SchedulerRunnable, TimeBasedTriggerHandler, EventSubscriber, EventFilter {

    public static final String MODULE_TYPE_ID = "timer.DateTimeTrigger";
    public static final String CONFIG_ITEM_NAME = "itemName";
    public static final String CONFIG_TIME_ONLY = "timeOnly";

    private static final DateTimeFormatter CRON_FORMATTER = DateTimeFormatter.ofPattern("s m H d M * uuuu");
    private static final DateTimeFormatter CRON_TIMEONLY_FORMATTER = DateTimeFormatter.ofPattern("s m H * * * *");

    private final Logger logger = LoggerFactory.getLogger(DateTimeTriggerHandler.class);

    private final CronScheduler scheduler;
    private final String itemName;
    private String cronExpression = CronAdjuster.REBOOT;
    private Boolean timeOnly = false;

    private @Nullable ScheduledCompletableFuture<?> schedule;
    private @Nullable ServiceRegistration<?> eventSubscriberRegistration;

    public DateTimeTriggerHandler(Trigger module, CronScheduler scheduler, ItemRegistry itemRegistry,
            BundleContext bundleContext) {
        super(module);
        this.scheduler = scheduler;
        this.itemName = ConfigParser.valueAsOrElse(module.getConfiguration().get(CONFIG_ITEM_NAME), String.class, "");
        if (this.itemName.isBlank()) {
            logger.warn("itemName is blank in module '{}', trigger will not work", module.getId());
            return;
        }
        this.timeOnly = ConfigParser.valueAsOrElse(module.getConfiguration().get(CONFIG_TIME_ONLY), Boolean.class,
                false);
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("event.topics", "openhab/items/" + itemName + "/*");
        eventSubscriberRegistration = bundleContext.registerService(EventSubscriber.class.getName(), this, properties);
        try {
            process(itemRegistry.getItem(itemName).getState());
        } catch (ItemNotFoundException e) {
            logger.info("Could not determine initial state for item '{}' in trigger '{}', waiting for event", itemName,
                    module.getId());
        }
    }

    @Override
    public void dispose() {
        ServiceRegistration<?> eventSubscriberRegistration = this.eventSubscriberRegistration;
        if (eventSubscriberRegistration != null) {
            eventSubscriberRegistration.unregister();
            this.eventSubscriberRegistration = null;
        }
        cancelScheduler();
        super.dispose();
    }

    @Override
    public synchronized void setCallback(ModuleHandlerCallback callback) {
        super.setCallback(callback);
        startScheduler();
    }

    @Override
    public void run() {
        ModuleHandlerCallback callback = this.callback;
        if (callback instanceof TriggerHandlerCallback) {
            ((TriggerHandlerCallback) callback).triggered(module);
        } else {
            logger.debug("Tried to trigger, but callback isn't available!");
        }
    }

    @Override
    public CronAdjuster getTemporalAdjuster() {
        return new CronAdjuster(cronExpression);
    }

    @Override
    public Set<String> getSubscribedEventTypes() {
        return Set.of(ItemStateEvent.TYPE, ItemCommandEvent.TYPE);
    }

    @Override
    public @Nullable EventFilter getEventFilter() {
        return this;
    }

    @Override
    public void receive(Event event) {
        if (event instanceof ItemEvent && (((ItemEvent) event).getItemName().equals(itemName))) {
            if (event instanceof ItemStateEvent) {
                process(((ItemStateEvent) event).getItemState());
            } else if (event instanceof ItemCommandEvent) {
                process(((ItemCommandEvent) event).getItemCommand());
            } else {
                logger.debug("Don't know how to process event {}, discarding", event);
            }
        }
    }

    @Override
    public boolean apply(Event event) {
        logger.trace("->FILTER: {}:{}", event.getTopic(), itemName);
        return event.getTopic().contains("openhab/items/" + itemName + "/");
    }

    private synchronized void startScheduler() {
        cancelScheduler();
        if (!CronAdjuster.REBOOT.equals(cronExpression)) {
            schedule = scheduler.schedule(this, cronExpression);
            logger.debug("Scheduled cron job '{}' for trigger '{}'.", module.getConfiguration().get(CONFIG_ITEM_NAME),
                    module.getId());
        }
    }

    private synchronized void cancelScheduler() {
        ScheduledCompletableFuture<?> schedule = this.schedule;
        if (schedule != null) {
            schedule.cancel(true);
            this.schedule = null;
            logger.debug("Cancelled job for trigger '{}'.", module.getId());
        }
    }

    private void process(Type value) {
        cancelScheduler();
        if (value instanceof UnDefType) {
            cronExpression = CronAdjuster.REBOOT;
        } else if (value instanceof DateTimeType) {
            boolean itemIsTimeOnly = ((DateTimeType) value).toString().startsWith("1970-01-01T");
            cronExpression = ((DateTimeType) value).getZonedDateTime().withZoneSameInstant(ZoneId.systemDefault())
                    .format(timeOnly || itemIsTimeOnly ? CRON_TIMEONLY_FORMATTER : CRON_FORMATTER);
            startScheduler();
        } else {
            logger.warn("Received {} which is not an accepted value for trigger of type '{}", value, MODULE_TYPE_ID);
        }
    }
}
