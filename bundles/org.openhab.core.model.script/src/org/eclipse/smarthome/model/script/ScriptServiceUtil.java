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
package org.eclipse.smarthome.model.script;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.binding.ThingActions;
import org.eclipse.smarthome.model.core.ModelRepository;
import org.eclipse.smarthome.model.script.engine.ScriptEngine;
import org.eclipse.smarthome.model.script.engine.action.ActionService;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for providing easy access to script services.
 *
 * @author Davy Vanherbergen - Initial contribution
 * @author Kai Kreuzer - renamed and removed interface
 */
@Component(immediate = true, service = ScriptServiceUtil.class)
public class ScriptServiceUtil {

    private final Logger logger = LoggerFactory.getLogger(ScriptServiceUtil.class);

    private static ScriptServiceUtil instance;

    private ItemRegistry itemRegistry;

    private ThingRegistry thingRegistry;

    private EventPublisher eventPublisher;

    private ModelRepository modelRepository;

    private final AtomicReference<ScriptEngine> scriptEngine = new AtomicReference<>();

    public List<ActionService> actionServices = new CopyOnWriteArrayList<>();

    public List<ThingActions> thingActions = new CopyOnWriteArrayList<>();

    @Activate
    public void activate(final BundleContext bc) {
        if (instance != null) {
            throw new IllegalStateException("ScriptServiceUtil should only be activated once!");
        }
        instance = this;
        logger.debug("ScriptServiceUtil started");
    }

    @Deactivate
    public void deactivate() {
        logger.debug("ScriptServiceUtil stopped");
        instance = null;
    }

    private static ScriptServiceUtil getInstance() {
        return instance;
    }

    public static ItemRegistry getItemRegistry() {
        return getInstance().itemRegistry;
    }

    public ItemRegistry getItemRegistryInstance() {
        return itemRegistry;
    }

    public ThingRegistry getThingRegistryInstance() {
        return thingRegistry;
    }

    public static EventPublisher getEventPublisher() {
        return getInstance().eventPublisher;
    }

    public static ModelRepository getModelRepository() {
        return getInstance().modelRepository;
    }

    public ModelRepository getModelRepositoryInstance() {
        return modelRepository;
    }

    public static ScriptEngine getScriptEngine() {
        return getInstance().scriptEngine.get();
    }

    public static List<ActionService> getActionServices() {
        return getInstance().actionServices;
    }

    public static List<ThingActions> getThingActions() {
        return getInstance().thingActions;
    }

    public List<ActionService> getActionServiceInstances() {
        return actionServices;
    }

    public List<ThingActions> getThingActionsInstances() {
        return thingActions;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addActionService(ActionService actionService) {
        this.actionServices.add(actionService);
    }

    public void removeActionService(ActionService actionService) {
        this.actionServices.remove(actionService);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addThingActions(ThingActions thingActions) {
        this.thingActions.add(thingActions);
    }

    public void removeThingActions(ThingActions thingActions) {
        this.thingActions.remove(thingActions);
    }

    @Reference
    public void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    public void unsetItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = null;
    }

    @Reference
    public void setThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = thingRegistry;
    }

    public void unsetThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = null;
    }

    @Reference
    public void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void unsetEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = null;
    }

    @Reference
    public void setModelRepository(ModelRepository modelRepository) {
        this.modelRepository = modelRepository;
    }

    public void unsetModelRepository(ModelRepository modelRepository) {
        this.modelRepository = null;
    }

    public void setScriptEngine(ScriptEngine scriptEngine) {
        // injected as a callback from the script engine, not via DS as it is a circular dependency...
        this.scriptEngine.set(scriptEngine);
    }

    public void unsetScriptEngine(ScriptEngine scriptEngine) {
        // uninjected as a callback from the script engine, not via DS as it is a circular dependency...
        this.scriptEngine.compareAndSet(scriptEngine, null);
    }

}
