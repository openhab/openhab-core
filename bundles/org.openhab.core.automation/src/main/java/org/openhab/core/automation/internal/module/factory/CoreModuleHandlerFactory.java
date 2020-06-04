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
package org.openhab.core.automation.internal.module.factory;

import java.util.Arrays;
import java.util.Collection;

import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.handler.BaseModuleHandlerFactory;
import org.openhab.core.automation.handler.ModuleHandler;
import org.openhab.core.automation.handler.ModuleHandlerFactory;
import org.openhab.core.automation.internal.module.handler.ChannelEventTriggerHandler;
import org.openhab.core.automation.internal.module.handler.CompareConditionHandler;
import org.openhab.core.automation.internal.module.handler.GenericEventConditionHandler;
import org.openhab.core.automation.internal.module.handler.GenericEventTriggerHandler;
import org.openhab.core.automation.internal.module.handler.GroupCommandTriggerHandler;
import org.openhab.core.automation.internal.module.handler.GroupStateTriggerHandler;
import org.openhab.core.automation.internal.module.handler.ItemCommandActionHandler;
import org.openhab.core.automation.internal.module.handler.ItemCommandTriggerHandler;
import org.openhab.core.automation.internal.module.handler.ItemStateConditionHandler;
import org.openhab.core.automation.internal.module.handler.ItemStateTriggerHandler;
import org.openhab.core.automation.internal.module.handler.RuleEnablementActionHandler;
import org.openhab.core.automation.internal.module.handler.RunRuleActionHandler;
import org.openhab.core.automation.internal.module.handler.SystemTriggerHandler;
import org.openhab.core.automation.internal.module.handler.ThingStatusTriggerHandler;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.ItemRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This HandlerFactory creates ModuleHandlers to control items within the
 * RuleManager. It contains basic Triggers, Conditions and Actions.
 *
 * @author Benedikt Niehues - Initial contribution
 * @author Kai Kreuzer - refactored and simplified customized module handling
 */
@Component
public class CoreModuleHandlerFactory extends BaseModuleHandlerFactory implements ModuleHandlerFactory {

    private final Logger logger = LoggerFactory.getLogger(CoreModuleHandlerFactory.class);

    private static final Collection<String> TYPES = Arrays.asList(ItemCommandTriggerHandler.MODULE_TYPE_ID,
            GroupCommandTriggerHandler.MODULE_TYPE_ID, ItemStateTriggerHandler.UPDATE_MODULE_TYPE_ID,
            ItemStateTriggerHandler.CHANGE_MODULE_TYPE_ID, GroupStateTriggerHandler.UPDATE_MODULE_TYPE_ID,
            GroupStateTriggerHandler.CHANGE_MODULE_TYPE_ID, ThingStatusTriggerHandler.UPDATE_MODULE_TYPE_ID,
            ThingStatusTriggerHandler.CHANGE_MODULE_TYPE_ID, ItemStateConditionHandler.ITEM_STATE_CONDITION,
            ItemCommandActionHandler.ITEM_COMMAND_ACTION, GenericEventTriggerHandler.MODULE_TYPE_ID,
            ChannelEventTriggerHandler.MODULE_TYPE_ID, GenericEventConditionHandler.MODULETYPE_ID,
            GenericEventConditionHandler.MODULETYPE_ID, CompareConditionHandler.MODULE_TYPE,
            SystemTriggerHandler.STARTLEVEL_MODULE_TYPE_ID, RuleEnablementActionHandler.UID, RunRuleActionHandler.UID);

    private ItemRegistry itemRegistry;
    private EventPublisher eventPublisher;

    private BundleContext bundleContext;

    @Activate
    protected void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public Collection<String> getTypes() {
        return TYPES;
    }

    /**
     * the itemRegistry was added (called by serviceTracker)
     *
     * @param itemRegistry
     */
    @Reference
    protected void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
        for (ModuleHandler handler : getHandlers().values()) {
            if (handler instanceof ItemStateConditionHandler) {
                ((ItemStateConditionHandler) handler).setItemRegistry(this.itemRegistry);
            } else if (handler instanceof ItemCommandActionHandler) {
                ((ItemCommandActionHandler) handler).setItemRegistry(this.itemRegistry);
            } else if (handler instanceof GroupCommandTriggerHandler) {
                ((GroupCommandTriggerHandler) handler).setItemRegistry(this.itemRegistry);
            } else if (handler instanceof GroupStateTriggerHandler) {
                ((GroupStateTriggerHandler) handler).setItemRegistry(this.itemRegistry);
            }
        }
    }

    /**
     * unsetter for itemRegistry (called by serviceTracker)
     *
     * @param itemRegistry
     */
    protected void unsetItemRegistry(ItemRegistry itemRegistry) {
        for (ModuleHandler handler : getHandlers().values()) {
            if (handler instanceof ItemStateConditionHandler) {
                ((ItemStateConditionHandler) handler).unsetItemRegistry(this.itemRegistry);
            } else if (handler instanceof ItemCommandActionHandler) {
                ((ItemCommandActionHandler) handler).unsetItemRegistry(this.itemRegistry);
            } else if (handler instanceof GroupCommandTriggerHandler) {
                ((GroupCommandTriggerHandler) handler).unsetItemRegistry(this.itemRegistry);
            } else if (handler instanceof GroupStateTriggerHandler) {
                ((GroupStateTriggerHandler) handler).unsetItemRegistry(this.itemRegistry);
            }
        }
        this.itemRegistry = null;
    }

    /**
     * setter for the eventPublisher (called by serviceTracker)
     *
     * @param eventPublisher
     */
    @Reference
    protected void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        for (ModuleHandler handler : getHandlers().values()) {
            if (handler instanceof ItemCommandActionHandler) {
                ((ItemCommandActionHandler) handler).setEventPublisher(eventPublisher);
            }
        }
    }

    /**
     * unsetter for eventPublisher (called by serviceTracker)
     *
     * @param eventPublisher
     */
    protected void unsetEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = null;
        for (ModuleHandler handler : getHandlers().values()) {
            if (handler instanceof ItemCommandActionHandler) {
                ((ItemCommandActionHandler) handler).unsetEventPublisher(eventPublisher);
            }
        }
    }

    @Override
    protected synchronized ModuleHandler internalCreate(final Module module, final String ruleUID) {
        logger.trace("create {} -> {} : {}", module.getId(), module.getTypeUID(), ruleUID);
        final String moduleTypeUID = module.getTypeUID();
        if (module instanceof Trigger) {
            // Handle triggers
            if (GenericEventTriggerHandler.MODULE_TYPE_ID.equals(moduleTypeUID)) {
                return new GenericEventTriggerHandler((Trigger) module, bundleContext);
            } else if (ChannelEventTriggerHandler.MODULE_TYPE_ID.equals(moduleTypeUID)) {
                return new ChannelEventTriggerHandler((Trigger) module, bundleContext);
            } else if (ItemCommandTriggerHandler.MODULE_TYPE_ID.equals(moduleTypeUID)) {
                return new ItemCommandTriggerHandler((Trigger) module, bundleContext);
            } else if (SystemTriggerHandler.STARTLEVEL_MODULE_TYPE_ID.equals(moduleTypeUID)) {
                return new SystemTriggerHandler((Trigger) module, bundleContext);
            } else if (ThingStatusTriggerHandler.CHANGE_MODULE_TYPE_ID.equals(moduleTypeUID)
                    || ThingStatusTriggerHandler.UPDATE_MODULE_TYPE_ID.equals(moduleTypeUID)) {
                return new ThingStatusTriggerHandler((Trigger) module, bundleContext);
            } else if (ItemStateTriggerHandler.CHANGE_MODULE_TYPE_ID.equals(moduleTypeUID)
                    || ItemStateTriggerHandler.UPDATE_MODULE_TYPE_ID.equals(moduleTypeUID)) {
                return new ItemStateTriggerHandler((Trigger) module, bundleContext);
            } else if (GroupCommandTriggerHandler.MODULE_TYPE_ID.equals(moduleTypeUID)) {
                final GroupCommandTriggerHandler handler = new GroupCommandTriggerHandler((Trigger) module,
                        bundleContext);
                handler.setItemRegistry(itemRegistry);
                return handler;
            } else if (GroupStateTriggerHandler.CHANGE_MODULE_TYPE_ID.equals(moduleTypeUID)
                    || GroupStateTriggerHandler.UPDATE_MODULE_TYPE_ID.equals(moduleTypeUID)) {
                final GroupStateTriggerHandler handler = new GroupStateTriggerHandler((Trigger) module, bundleContext);
                handler.setItemRegistry(itemRegistry);
                return handler;
            }
        } else if (module instanceof Condition) {
            // Handle conditions
            if (ItemStateConditionHandler.ITEM_STATE_CONDITION.equals(moduleTypeUID)) {
                ItemStateConditionHandler handler = new ItemStateConditionHandler((Condition) module);
                handler.setItemRegistry(itemRegistry);
                return handler;
            } else if (GenericEventConditionHandler.MODULETYPE_ID.equals(moduleTypeUID)) {
                return new GenericEventConditionHandler((Condition) module);
            } else if (CompareConditionHandler.MODULE_TYPE.equals(moduleTypeUID)) {
                return new CompareConditionHandler((Condition) module);
            }
        } else if (module instanceof Action) {
            // Handle actions
            if (ItemCommandActionHandler.ITEM_COMMAND_ACTION.equals(moduleTypeUID)) {
                final ItemCommandActionHandler postCommandActionHandler = new ItemCommandActionHandler((Action) module);
                postCommandActionHandler.setEventPublisher(eventPublisher);
                postCommandActionHandler.setItemRegistry(itemRegistry);
                return postCommandActionHandler;
            } else if (RuleEnablementActionHandler.UID.equals(moduleTypeUID)) {
                return new RuleEnablementActionHandler((Action) module);
            } else if (RunRuleActionHandler.UID.equals(moduleTypeUID)) {
                return new RunRuleActionHandler((Action) module);
            }
        }

        logger.error("The ModuleHandler is not supported:{}", moduleTypeUID);
        return null;
    }
}
