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
package org.openhab.core.model.script;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.RuleManager;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.script.engine.ScriptEngine;
import org.openhab.core.model.script.engine.action.ActionService;
import org.openhab.core.scheduler.Scheduler;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
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
 * @author Ravi Nadahar - added additional registries for retrieval
 */
@Component(immediate = true, service = ScriptServiceUtil.class)
public class ScriptServiceUtil {

    private final Logger logger = LoggerFactory.getLogger(ScriptServiceUtil.class);

    private static ScriptServiceUtil instance;

    private final ItemRegistry itemRegistry;
    private final ThingRegistry thingRegistry;
    private final EventPublisher eventPublisher;
    private final ModelRepository modelRepository;
    private final MetadataRegistry metadataRegistry;
    private final RuleRegistry ruleRegistry;
    private final ItemChannelLinkRegistry itemChannelLinkRegistry;
    private volatile @Nullable RuleManager ruleManager;
    private final Scheduler scheduler;

    private final AtomicReference<ScriptEngine> scriptEngine = new AtomicReference<>();
    public final List<ActionService> actionServices = new CopyOnWriteArrayList<>();
    public final List<ThingActions> thingActions = new CopyOnWriteArrayList<>();

    @Activate
    public ScriptServiceUtil(final @Reference ItemRegistry itemRegistry, final @Reference ThingRegistry thingRegistry,
            final @Reference EventPublisher eventPublisher, final @Reference ModelRepository modelRepository,
            final @Reference MetadataRegistry metadataRegistry, final @Reference RuleRegistry ruleRegistry,
            final @Reference ItemChannelLinkRegistry itemChannelLinkRegistry, final @Reference Scheduler scheduler) {
        this.itemRegistry = itemRegistry;
        this.thingRegistry = thingRegistry;
        this.eventPublisher = eventPublisher;
        this.modelRepository = modelRepository;
        this.metadataRegistry = metadataRegistry;
        this.ruleRegistry = ruleRegistry;
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
        this.scheduler = scheduler;

        if (instance != null) {
            throw new IllegalStateException("ScriptServiceUtil should only be activated once!");
        }
        instance = this;
        logger.debug("ScriptServiceUtil started");
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    void setRuleManager(RuleManager ruleManager) {
        this.ruleManager = ruleManager;
    }

    void unsetRuleManager(RuleManager ruleManager) {
        this.ruleManager = null;
    }

    @Deactivate
    public void deactivate() {
        logger.debug("ScriptServiceUtil stopped");
        instance = null;
    }

    private static ScriptServiceUtil getInstance() {
        return instance;
    }

    /**
     * @return The {@link ItemRegistry}.
     */
    public static ItemRegistry getItemRegistry() {
        return getInstance().itemRegistry;
    }

    public ItemRegistry getItemRegistryInstance() {
        return itemRegistry;
    }

    /**
     * @return The {@link ThingRegistry} instance.
     */
    public static ThingRegistry getThingRegistry() {
        return getInstance().thingRegistry;
    }

    public ThingRegistry getThingRegistryInstance() {
        return thingRegistry;
    }

    /**
     * @return The {@link EventPublisher} instance.
     */
    public static EventPublisher getEventPublisher() {
        return getInstance().eventPublisher;
    }

    /**
     * @return The {@link ModelRepository} instance.
     */
    public static ModelRepository getModelRepository() {
        return getInstance().modelRepository;
    }

    public ModelRepository getModelRepositoryInstance() {
        return modelRepository;
    }

    /**
     * @return The {@link MetadataRegistry} instance.
     */
    public static MetadataRegistry getMetadataRegistry() {
        return getInstance().metadataRegistry;
    }

    public MetadataRegistry getMetadataRegistryInstance() {
        return metadataRegistry;
    }

    /**
     * @return The {@link RuleRegistry} instance.
     */
    public static RuleRegistry getRuleRegistry() {
        return getInstance().ruleRegistry;
    }

    public RuleRegistry getRuleRegistryInstance() {
        return ruleRegistry;
    }

    /**
     * @return The {@link ItemChannelLinkRegistry} instance.
     */
    public static ItemChannelLinkRegistry getItemChannelLinkRegistry() {
        return getInstance().itemChannelLinkRegistry;
    }

    public ItemChannelLinkRegistry getItemChannelLinkRegistryInstance() {
        return itemChannelLinkRegistry;
    }

    /**
     * @return The {@link RuleManager} / rule engine instance or {@code null} if it doesn't exist.
     */
    public @Nullable static RuleManager getRuleManager() {
        return getInstance().ruleManager;
    }

    public @Nullable RuleManager getRuleManagerInstance() {
        return ruleManager;
    }

    /**
     * @return The {@link Scheduler} instance.
     */
    public static Scheduler getScheduler() {
        return getInstance().scheduler;
    }

    public Scheduler getSchedulerInstance() {
        return scheduler;
    }

    public static ScriptEngine getScriptEngine() {
        return getInstance().scriptEngine.get();
    }

    /**
     * @return A {@link List} of currently registered {@link ActionService} instances.
     */
    public static List<ActionService> getActionServices() {
        return List.copyOf(getInstance().actionServices);
    }

    /**
     * @return A {@link List} of currently registered {@link ThingActions} instances.
     */
    public static List<ThingActions> getThingActions() {
        return List.copyOf(getInstance().thingActions);
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

    public void setScriptEngine(ScriptEngine scriptEngine) {
        // injected as a callback from the script engine, not via DS as it is a circular dependency...
        this.scriptEngine.set(scriptEngine);
    }

    public void unsetScriptEngine(ScriptEngine scriptEngine) {
        // uninjected as a callback from the script engine, not via DS as it is a circular dependency...
        this.scriptEngine.compareAndSet(scriptEngine, null);
    }

    /**
     * Retrieve an OSGi instance of the specified {@link Class}, if it exists. The reference to the instance is
     * <i>unreserved</i>, which means that the instance might stop being valid at any time, for example if the service
     * or the containing bundle is stopped.
     * <p>
     * Returning an unreserved service isn't kosher in the world of OSGi, but the only alternative is that the scripts
     * that retrieve the instance are responsible for unregistering the reservation after use. That isn't a reasonable
     * thing to expect from user scripts. The chance that a service is stopped while OH is running is quite small, so
     * on balance, returning an unreserved service instance seems like the best way to do it. It isn't much different
     * from returning an instance to a registry that is reserved by {@link ScriptServiceUtil} - if the
     * {@link ScriptServiceUtil} itself is stopped, the instance might become invalid while the script is using it.
     *
     * @param <T> the class type.
     * @param clazz the class of the instance to get.
     * @return The instance or {@code null} if the instance wasn't found.
     */
    public static @Nullable <T> T getInstance(Class<T> clazz) {
        Bundle bundle = FrameworkUtil.getBundle(clazz);
        if (bundle != null) {
            BundleContext bc = bundle.getBundleContext();
            if (bc != null) {
                ServiceReference<T> ref = bc.getServiceReference(clazz);
                if (ref != null) {
                    T result = bc.getService(ref);
                    if (result != null) {
                        bc.ungetService(ref);
                    }
                    return result;
                }
            }
        }
        return null;
    }
}
