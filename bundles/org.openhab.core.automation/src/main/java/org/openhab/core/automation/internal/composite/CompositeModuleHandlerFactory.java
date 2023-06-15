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
package org.openhab.core.automation.internal.composite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.handler.ActionHandler;
import org.openhab.core.automation.handler.BaseModuleHandlerFactory;
import org.openhab.core.automation.handler.ConditionHandler;
import org.openhab.core.automation.handler.ModuleHandler;
import org.openhab.core.automation.handler.ModuleHandlerFactory;
import org.openhab.core.automation.handler.TriggerHandler;
import org.openhab.core.automation.internal.ModuleImpl;
import org.openhab.core.automation.internal.RuleEngineImpl;
import org.openhab.core.automation.type.CompositeActionType;
import org.openhab.core.automation.type.CompositeConditionType;
import org.openhab.core.automation.type.CompositeTriggerType;
import org.openhab.core.automation.type.ModuleType;
import org.openhab.core.automation.type.ModuleTypeRegistry;
import org.openhab.core.automation.util.ReferenceResolver;
import org.openhab.core.config.core.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a factory for system module handler for modules of composite module types: {@link CompositeTriggerType}
 * , {@link CompositeConditionType} and {@link CompositeActionType}. The composite module type is a type which contains
 * one or more internal (child) modules and these modules have access to configuration properties and inputs of
 * composite module. The outputs of module of composite type (if they exists) are set these handlers and they are base
 * on the values of child module outputs.
 * The {@link CompositeModuleHandlerFactory} is a system handler factory and it is not registered as service in OSGi
 * framework, but it will be used by the rule engine to serve composite module types without any action of the user.
 *
 *
 * @author Yordan Mihaylov - Initial contribution
 */
@NonNullByDefault
public class CompositeModuleHandlerFactory extends BaseModuleHandlerFactory implements ModuleHandlerFactory {

    private final ModuleTypeRegistry mtRegistry;
    private final RuleEngineImpl ruleEngine;
    private final Logger logger = LoggerFactory.getLogger(CompositeModuleHandlerFactory.class);

    /**
     * The constructor of system handler factory for composite module types.
     *
     * @param context is a bundle context
     * @param mtManager is a module type manager
     * @param re is a rule engine
     */
    public CompositeModuleHandlerFactory(ModuleTypeRegistry mtRegistry, RuleEngineImpl re) {
        this.mtRegistry = mtRegistry;
        this.ruleEngine = re;
    }

    @Override
    public void deactivate() {
        super.deactivate();
    }

    /**
     * It is system factory and must not be registered as service. This method is not used.
     *
     * @see org.openhab.core.automation.handler.ModuleHandlerFactory#getTypes()
     */
    @Override
    public Collection<String> getTypes() {
        return new ArrayList<>();
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public void ungetHandler(Module module, String childModulePrefix, ModuleHandler handler) {
        ModuleHandler handlerOfModule = getHandlers().get(getModuleIdentifier(childModulePrefix, module.getId()));
        if (handlerOfModule instanceof AbstractCompositeModuleHandler) {
            AbstractCompositeModuleHandler<ModuleImpl, ?, ?> h = (AbstractCompositeModuleHandler<ModuleImpl, ?, ?>) handlerOfModule;
            Set<ModuleImpl> modules = h.moduleHandlerMap.keySet();
            for (ModuleImpl child : modules) {
                ModuleHandler childHandler = h.moduleHandlerMap.get(child);
                if (childHandler == null) {
                    continue;
                }
                ModuleHandlerFactory mhf = ruleEngine.getModuleHandlerFactory(child.getTypeUID());
                if (mhf == null) {
                    continue;
                }
                mhf.ungetHandler(child, childModulePrefix + ":" + module.getId(), childHandler);
            }
        }
        String ruleId = getRuleId(childModulePrefix);
        super.ungetHandler(module, ruleId, handler);
    }

    private String getRuleId(String childModulePrefix) {
        int i = childModulePrefix.indexOf(':');
        return i != -1 ? childModulePrefix.substring(0, i) : childModulePrefix;
    }

    @Override
    public @Nullable ModuleHandler internalCreate(Module module, String ruleUID) {
        ModuleHandler handler = null;
        String moduleType = module.getTypeUID();
        ModuleType mt = mtRegistry.get(moduleType);
        if (mt instanceof CompositeTriggerType type) {
            List<Trigger> childModules = type.getChildren();
            LinkedHashMap<Trigger, @Nullable TriggerHandler> mapModuleToHandler = getChildHandlers(module.getId(),
                    module.getConfiguration(), childModules, ruleUID);
            if (mapModuleToHandler != null) {
                handler = new CompositeTriggerHandler((Trigger) module, type, mapModuleToHandler, ruleUID);
            }
        } else if (mt instanceof CompositeConditionType type) {
            List<Condition> childModules = type.getChildren();
            LinkedHashMap<Condition, @Nullable ConditionHandler> mapModuleToHandler = getChildHandlers(module.getId(),
                    module.getConfiguration(), childModules, ruleUID);
            if (mapModuleToHandler != null) {
                handler = new CompositeConditionHandler((Condition) module, type, mapModuleToHandler, ruleUID);
            }
        } else if (mt instanceof CompositeActionType type) {
            List<Action> childModules = type.getChildren();
            LinkedHashMap<Action, @Nullable ActionHandler> mapModuleToHandler = getChildHandlers(module.getId(),
                    module.getConfiguration(), childModules, ruleUID);
            if (mapModuleToHandler != null) {
                handler = new CompositeActionHandler((Action) module, type, mapModuleToHandler, ruleUID);
            }
        }
        if (handler != null) {
            logger.debug("Set module handler: {}  -> {} of rule {}.", module.getId(),
                    handler.getClass().getSimpleName() + "(" + moduleType + ")", ruleUID);
        } else {
            logger.debug("Not found module handler {} for moduleType {} of rule {}.", module.getId(), moduleType,
                    ruleUID);
        }
        return handler;
    }

    /**
     * This method associates module handlers to the child modules of composite module types. It links module types of
     * child modules to the rule which contains this composite module. It also resolve links between child configuration
     * properties and configuration of composite module see:
     * {@link ReferenceResolver#updateConfiguration(Configuration, Map, Logger)}.
     *
     * @param compositeConfig configuration values of composite module.
     * @param childModules list of child modules
     * @param childModulePrefix defines UID of child module. The rule id is not enough for prefix when a composite type
     *            is used more then one time in one and the same rule. For example the prefix can be:
     *            ruleId:compositeModuleId:compositeModileId2.
     * @return map of pairs of module and its handler. Return null when some of the child modules can not find its
     *         handler.
     */
    @SuppressWarnings("unchecked")
    private <T extends Module, @Nullable MT extends ModuleHandler> @Nullable LinkedHashMap<T, MT> getChildHandlers(
            String compositeModuleId, Configuration compositeConfig, List<T> childModules, String childModulePrefix) {
        LinkedHashMap<T, MT> mapModuleToHandler = new LinkedHashMap<>();
        for (T child : childModules) {
            String ruleId = getRuleId(childModulePrefix);
            ruleEngine.updateMapModuleTypeToRule(ruleId, child.getTypeUID());
            ModuleHandlerFactory childMhf = ruleEngine.getModuleHandlerFactory(child.getTypeUID());
            if (childMhf == null) {
                mapModuleToHandler.clear();
                mapModuleToHandler = null;
                return null;
            }
            ReferenceResolver.updateConfiguration(child.getConfiguration(), compositeConfig.getProperties(), logger);
            MT childHandler = (MT) childMhf.getHandler(child, childModulePrefix + ":" + compositeModuleId);

            if (childHandler == null) {
                mapModuleToHandler.clear();
                mapModuleToHandler = null;
                return null;
            }
            mapModuleToHandler.put(child, childHandler);
        }
        return mapModuleToHandler;
    }
}
