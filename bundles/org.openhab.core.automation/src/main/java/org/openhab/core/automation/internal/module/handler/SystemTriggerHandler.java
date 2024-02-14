/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.ModuleHandlerCallback;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.handler.BaseTriggerModuleHandler;
import org.openhab.core.automation.handler.TriggerHandlerCallback;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.events.system.StartlevelEvent;
import org.openhab.core.service.StartLevelService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a ModuleHandler implementation for Triggers which trigger the rule if a certain system event occurs.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class SystemTriggerHandler extends BaseTriggerModuleHandler implements EventSubscriber {

    public static final String STARTLEVEL_MODULE_TYPE_ID = "core.SystemStartlevelTrigger";
    public static final String CFG_STARTLEVEL = "startlevel";
    public static final String OUT_STARTLEVEL = "startlevel";

    private final Logger logger = LoggerFactory.getLogger(SystemTriggerHandler.class);

    private final Integer startlevel;
    private final Set<String> types;

    private boolean triggered = false;

    private final ServiceRegistration<?> eventSubscriberRegistration;

    public SystemTriggerHandler(Trigger module, BundleContext bundleContext) {
        super(module);
        this.startlevel = ((BigDecimal) module.getConfiguration().get(CFG_STARTLEVEL)).intValue();
        if (STARTLEVEL_MODULE_TYPE_ID.equals(module.getTypeUID())) {
            this.types = Set.of(StartlevelEvent.TYPE);
        } else {
            logger.warn("Module type '{}' is not (yet) handled by this class.", module.getTypeUID());
            throw new IllegalArgumentException(module.getTypeUID() + " is no valid module type.");
        }
        eventSubscriberRegistration = bundleContext.registerService(EventSubscriber.class.getName(), this, null);
    }

    @Override
    public void setCallback(ModuleHandlerCallback callback) {
        super.setCallback(callback);
    }

    @Override
    public Set<String> getSubscribedEventTypes() {
        return types;
    }

    @Override
    public void receive(Event event) {
        if (triggered) {
            // this trigger only works once
            return;
        }
        logger.trace("Received Event: Source: {} Topic: {} Type: {}  Payload: {}", event.getSource(), event.getTopic(),
                event.getType(), event.getPayload());
        if (event instanceof StartlevelEvent startlevelEvent && STARTLEVEL_MODULE_TYPE_ID.equals(module.getTypeUID())) {
            Integer sl = startlevelEvent.getStartlevel();
            if (startlevel <= sl && startlevel > StartLevelService.STARTLEVEL_RULEENGINE) {
                // only execute rules if their start level is higher than the rule engine activation level, since
                // otherwise the rule engine takes care of the execution already
                trigger(event);
            }
        }
    }

    /**
     * do the cleanup: unregistering eventSubscriber...
     */
    @Override
    public void dispose() {
        eventSubscriberRegistration.unregister();
        super.dispose();
    }

    private void trigger(Event event) {
        final ModuleHandlerCallback callback = this.callback;
        if (!(callback instanceof TriggerHandlerCallback)) {
            return;
        }

        TriggerHandlerCallback thCallback = (TriggerHandlerCallback) callback;
        Map<String, Object> values = Map.of(OUT_STARTLEVEL, startlevel, "event", event);
        thCallback.triggered(module, values);
        triggered = true;
    }
}
