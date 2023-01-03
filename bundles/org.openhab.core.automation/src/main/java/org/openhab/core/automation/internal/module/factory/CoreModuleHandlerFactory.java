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
package org.openhab.core.automation.internal.module.factory;

import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
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
import org.openhab.core.automation.internal.module.handler.ItemStateUpdateActionHandler;
import org.openhab.core.automation.internal.module.handler.RuleEnablementActionHandler;
import org.openhab.core.automation.internal.module.handler.RunRuleActionHandler;
import org.openhab.core.automation.internal.module.handler.SystemTriggerHandler;
import org.openhab.core.automation.internal.module.handler.ThingStatusTriggerHandler;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.i18n.TimeZoneProvider;
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
@NonNullByDefault
public class CoreModuleHandlerFactory extends BaseModuleHandlerFactory implements ModuleHandlerFactory {

    private final Logger logger = LoggerFactory.getLogger(CoreModuleHandlerFactory.class);

    private static final Collection<String> TYPES = List.of(ItemCommandTriggerHandler.MODULE_TYPE_ID,
            GroupCommandTriggerHandler.MODULE_TYPE_ID, ItemStateTriggerHandler.UPDATE_MODULE_TYPE_ID,
            ItemStateTriggerHandler.CHANGE_MODULE_TYPE_ID, GroupStateTriggerHandler.UPDATE_MODULE_TYPE_ID,
            GroupStateTriggerHandler.CHANGE_MODULE_TYPE_ID, ThingStatusTriggerHandler.UPDATE_MODULE_TYPE_ID,
            ThingStatusTriggerHandler.CHANGE_MODULE_TYPE_ID, ItemStateConditionHandler.ITEM_STATE_CONDITION,
            ItemCommandActionHandler.ITEM_COMMAND_ACTION, ItemStateUpdateActionHandler.ITEM_STATE_UPDATE_ACTION,
            GenericEventTriggerHandler.MODULE_TYPE_ID, ChannelEventTriggerHandler.MODULE_TYPE_ID,
            GenericEventConditionHandler.MODULETYPE_ID, GenericEventConditionHandler.MODULETYPE_ID,
            CompareConditionHandler.MODULE_TYPE, SystemTriggerHandler.STARTLEVEL_MODULE_TYPE_ID,
            RuleEnablementActionHandler.UID, RunRuleActionHandler.UID);

    private final ItemRegistry itemRegistry;
    private final TimeZoneProvider timeZoneProvider;
    private final EventPublisher eventPublisher;
    private final BundleContext bundleContext;

    @Activate
    public CoreModuleHandlerFactory(BundleContext bundleContext, final @Reference EventPublisher eventPublisher,
            final @Reference ItemRegistry itemRegistry, final @Reference TimeZoneProvider timeZoneProvider) {
        this.bundleContext = bundleContext;
        this.eventPublisher = eventPublisher;
        this.itemRegistry = itemRegistry;
        this.timeZoneProvider = timeZoneProvider;
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

    @Override
    protected synchronized @Nullable ModuleHandler internalCreate(final Module module, final String ruleUID) {
        logger.trace("create {} -> {} : {}", module.getId(), module.getTypeUID(), ruleUID);
        final String moduleTypeUID = module.getTypeUID();
        if (module instanceof Trigger) {
            // Handle triggers
            if (GenericEventTriggerHandler.MODULE_TYPE_ID.equals(moduleTypeUID)) {
                return new GenericEventTriggerHandler((Trigger) module, bundleContext);
            } else if (ChannelEventTriggerHandler.MODULE_TYPE_ID.equals(moduleTypeUID)) {
                return new ChannelEventTriggerHandler((Trigger) module, bundleContext);
            } else if (ItemCommandTriggerHandler.MODULE_TYPE_ID.equals(moduleTypeUID)) {
                return new ItemCommandTriggerHandler((Trigger) module, ruleUID, bundleContext, itemRegistry);
            } else if (SystemTriggerHandler.STARTLEVEL_MODULE_TYPE_ID.equals(moduleTypeUID)) {
                return new SystemTriggerHandler((Trigger) module, bundleContext);
            } else if (ThingStatusTriggerHandler.CHANGE_MODULE_TYPE_ID.equals(moduleTypeUID)
                    || ThingStatusTriggerHandler.UPDATE_MODULE_TYPE_ID.equals(moduleTypeUID)) {
                return new ThingStatusTriggerHandler((Trigger) module, bundleContext);
            } else if (ItemStateTriggerHandler.CHANGE_MODULE_TYPE_ID.equals(moduleTypeUID)
                    || ItemStateTriggerHandler.UPDATE_MODULE_TYPE_ID.equals(moduleTypeUID)) {
                return new ItemStateTriggerHandler((Trigger) module, ruleUID, bundleContext, itemRegistry);
            } else if (GroupCommandTriggerHandler.MODULE_TYPE_ID.equals(moduleTypeUID)) {
                return new GroupCommandTriggerHandler((Trigger) module, ruleUID, bundleContext, itemRegistry);
            } else if (GroupStateTriggerHandler.CHANGE_MODULE_TYPE_ID.equals(moduleTypeUID)
                    || GroupStateTriggerHandler.UPDATE_MODULE_TYPE_ID.equals(moduleTypeUID)) {
                return new GroupStateTriggerHandler((Trigger) module, ruleUID, bundleContext, itemRegistry);
            }
        } else if (module instanceof Condition) {
            // Handle conditions
            if (ItemStateConditionHandler.ITEM_STATE_CONDITION.equals(moduleTypeUID)) {
                return new ItemStateConditionHandler((Condition) module, ruleUID, bundleContext, itemRegistry,
                        timeZoneProvider);
            } else if (GenericEventConditionHandler.MODULETYPE_ID.equals(moduleTypeUID)) {
                return new GenericEventConditionHandler((Condition) module);
            } else if (CompareConditionHandler.MODULE_TYPE.equals(moduleTypeUID)) {
                return new CompareConditionHandler((Condition) module);
            }
        } else if (module instanceof Action) {
            // Handle actions
            if (ItemCommandActionHandler.ITEM_COMMAND_ACTION.equals(moduleTypeUID)) {
                return new ItemCommandActionHandler((Action) module, eventPublisher, itemRegistry);
            } else if (ItemStateUpdateActionHandler.ITEM_STATE_UPDATE_ACTION.equals(moduleTypeUID)) {
                return new ItemStateUpdateActionHandler((Action) module, eventPublisher, itemRegistry);
            } else if (RuleEnablementActionHandler.UID.equals(moduleTypeUID)) {
                return new RuleEnablementActionHandler((Action) module);
            } else if (RunRuleActionHandler.UID.equals(moduleTypeUID)) {
                return new RunRuleActionHandler((Action) module);
            }
        }

        logger.error("The ModuleHandler is not supported: {}", moduleTypeUID);
        return null;
    }
}
