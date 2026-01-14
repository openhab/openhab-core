/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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

import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.handler.BaseConditionModuleHandler;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.events.TopicPrefixEventFilter;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.events.ThingAddedEvent;
import org.openhab.core.thing.events.ThingRemovedEvent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConditionHandler implementation to check the thing status
 *
 * @author Jörg Sautter - Initial contribution based on the ItemStateConditionHandler
 */
@NonNullByDefault
public class ThingStatusConditionHandler extends BaseConditionModuleHandler implements EventSubscriber {

    /**
     * Constants for Config-Parameters corresponding to Definition in ThingConditions.json
     */
    public static final String CFG_THING_UID = "thingUID";
    public static final String CFG_OPERATOR = "operator";
    public static final String CFG_STATUS = "status";

    private final Logger logger = LoggerFactory.getLogger(ThingStatusConditionHandler.class);

    public static final String THING_STATUS_CONDITION = "core.ThingStatusCondition";

    private final ThingRegistry thingRegistry;
    private final String ruleUID;
    private final String thingUID;
    private final EventFilter eventFilter;
    private final BundleContext bundleContext;
    private final Set<String> types;
    private final ServiceRegistration<?> eventSubscriberRegistration;

    public ThingStatusConditionHandler(Condition condition, String ruleUID, BundleContext bundleContext,
            ThingRegistry thingRegistry) {
        super(condition);
        this.thingRegistry = thingRegistry;
        this.bundleContext = bundleContext;
        this.thingUID = (String) module.getConfiguration().get(CFG_THING_UID);
        this.eventFilter = new TopicPrefixEventFilter("openhab/things/" + thingUID + "/");
        this.types = Set.of(ThingAddedEvent.TYPE, ThingRemovedEvent.TYPE);
        this.ruleUID = ruleUID;

        eventSubscriberRegistration = this.bundleContext.registerService(EventSubscriber.class.getName(), this, null);

        if (thingUID == null || thingRegistry.get(new ThingUID(thingUID)) == null) {
            logger.warn("Thing '{}' needed for rule '{}' is missing. Condition '{}' will not work.", thingUID, ruleUID,
                    module.getId());
        }
    }

    @Override
    public Set<String> getSubscribedEventTypes() {
        return types;
    }

    @Override
    public @Nullable EventFilter getEventFilter() {
        return eventFilter;
    }

    @Override
    public void receive(Event event) {
        if ((event instanceof ThingAddedEvent addedEvent) && thingUID.equals(addedEvent.getThing().UID)) {
            logger.info("Thing '{}' needed for rule '{}' added. Condition '{}' will now work.", thingUID, ruleUID,
                    module.getId());
            return;
        } else if ((event instanceof ThingRemovedEvent removedEvent) && thingUID.equals(removedEvent.getThing().UID)) {
            logger.warn("Thing '{}' needed for rule '{}' removed. Condition '{}' will no longer work.", thingUID,
                    ruleUID, module.getId());
            return;
        }
    }

    @Override
    public boolean isSatisfied(Map<String, Object> inputs) {
        String rawStatus = (String) module.getConfiguration().get(CFG_STATUS);
        ThingStatus status;

        try {
            status = ThingStatus.valueOf(rawStatus);
        } catch (IllegalArgumentException e) {
            status = null;
        }

        String operator = (String) module.getConfiguration().get(CFG_OPERATOR);
        if (operator == null || status == null || thingUID == null) {
            logger.error("Module is not well configured: thingUID={}  operator={}  status = {} for rule {}", thingUID,
                    operator, rawStatus, ruleUID);
            return false;
        }
        logger.debug("ThingStatusCondition '{}' checking if {} {} {} for rule {}", module.getId(), thingUID, operator,
                status, ruleUID);
        Thing thing = thingRegistry.get(new ThingUID(thingUID));

        if (thing == null) {
            logger.error("Thing with UID {} not found in ThingRegistry for condition of rule {}.", thingUID, ruleUID);
        } else
            switch (operator) {
                case "=":
                    return thing.getStatus().equals(status);
                case "!=":
                    return !thing.getStatus().equals(status);
                default:
                    logger.error("Thing status condition operator {} is not known of rule {}", operator, ruleUID);
            }
        return false;
    }

    @Override
    public void dispose() {
        super.dispose();
        eventSubscriberRegistration.unregister();
    }
}
