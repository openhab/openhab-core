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
package org.openhab.core.model.rule.runtime.internal.engine;

import static org.openhab.core.model.rule.runtime.internal.engine.RuleTriggerManager.TriggerTypes.*;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.ItemRegistryChangeListener;
import org.openhab.core.items.StateChangeListener;
import org.openhab.core.items.events.ItemCommandEvent;
import org.openhab.core.items.events.ItemStateEvent;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.events.ChannelTriggeredEvent;
import org.openhab.core.thing.events.ThingStatusInfoChangedEvent;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.model.core.EventType;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.core.ModelRepositoryChangeListener;
import org.openhab.core.model.rule.RulesStandaloneSetup;
import org.openhab.core.model.rule.jvmmodel.RulesJvmModelInferrer;
import org.openhab.core.model.rule.rules.Rule;
import org.openhab.core.model.rule.rules.RuleModel;
import org.openhab.core.model.rule.runtime.RuleEngine;
import org.openhab.core.model.rule.runtime.internal.RuleRuntimeActivator;
import org.openhab.core.model.script.engine.Script;
import org.openhab.core.model.script.engine.ScriptEngine;
import org.openhab.core.model.script.engine.ScriptExecutionException;
import org.eclipse.xtext.naming.QualifiedName;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Injector;

/**
 * This class is the core of the openHAB rule engine.
 * It listens to changes to the rules folder, evaluates the trigger conditions of the rules and
 * schedules them for execution dependent on their triggering conditions.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Oliver Libutzki - Bugfixing
 */
@Component(immediate = true, service = { EventSubscriber.class, RuleEngine.class })
@NonNullByDefault
public class RuleEngineImpl implements ItemRegistryChangeListener, StateChangeListener, ModelRepositoryChangeListener,
        RuleEngine, EventSubscriber {

    private final Logger logger = LoggerFactory.getLogger(RuleEngineImpl.class);

    private static final String THREAD_POOL_NAME = "ruleEngine";

    protected final ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool(THREAD_POOL_NAME);

    private final ItemRegistry itemRegistry;
    private final ModelRepository modelRepository;
    private final ScriptEngine scriptEngine;

    private final Injector injector;
    private final RuleTriggerManager triggerManager;

    private @Nullable ScheduledFuture<?> startupJob;

    // This flag is used to signal that items are still being added and that we hence do not consider the rule engine
    // ready to be operational.
    // This field is package private to allow access for unit tests.
    boolean starting = true;

    @Activate
    public RuleEngineImpl(final @Reference ItemRegistry itemRegistry, final @Reference ModelRepository modelRepository,
            final @Reference ScriptEngine scriptEngine) {
        this.itemRegistry = itemRegistry;
        this.modelRepository = modelRepository;
        this.scriptEngine = scriptEngine;

        this.injector = RulesStandaloneSetup.getInjector();
        this.triggerManager = new RuleTriggerManager(injector);
    }

    @Activate
    public void activate() {
        logger.debug("Started rule engine");

        // read all rule files
        for (String ruleModelName : modelRepository.getAllModelNamesOfType("rules")) {
            EObject model = modelRepository.getModel(ruleModelName);
            if (model instanceof RuleModel) {
                RuleModel ruleModel = (RuleModel) model;
                triggerManager.addRuleModel(ruleModel);
            }
        }

        // register us as listeners
        itemRegistry.addRegistryChangeListener(this);
        modelRepository.addModelRepositoryChangeListener(this);

        // register us on all items which are already available in the registry
        for (Item item : itemRegistry.getItems()) {
            internalItemAdded(item);
        }
        scheduleStartupRules();
    }

    @Deactivate
    public void deactivate() {
        // unregister listeners
        for (Item item : itemRegistry.getItems()) {
            internalItemRemoved(item);
        }
        modelRepository.removeModelRepositoryChangeListener(this);
        itemRegistry.removeRegistryChangeListener(this);

        // execute all scripts that were registered for system shutdown
        executeRules(triggerManager.getRules(SHUTDOWN));
        triggerManager.clearAll();
    }

    @Reference
    protected void setRuleRuntimeActivator(RuleRuntimeActivator ruleRuntimeActivator) {
        // noop - only make sure RuleRuntimeActivator gets "used", hence activated
    }

    protected void unsetRuleRuntimeActivator(RuleRuntimeActivator ruleRuntimeActivator) {
    }

    @Override
    public void allItemsChanged(Collection<String> oldItemNames) {
        // add the current items again
        Collection<Item> items = itemRegistry.getItems();
        for (Item item : items) {
            internalItemAdded(item);
        }
        scheduleStartupRules();
    }

    @Override
    public void added(Item item) {
        internalItemAdded(item);
        scheduleStartupRules();
    }

    @Override
    public void removed(Item item) {
        internalItemRemoved(item);
    }

    @Override
    public void stateChanged(Item item, State oldState, State newState) {
        if (!starting) {
            Iterable<Rule> rules = triggerManager.getRules(CHANGE, item, oldState, newState);

            executeRules(rules, item, oldState, newState);
        }
    }

    @Override
    public void stateUpdated(Item item, State state) {
        if (!starting) {
            Iterable<Rule> rules = triggerManager.getRules(UPDATE, item, state);
            executeRules(rules, item, state);
        }
    }

    private void receiveCommand(ItemCommandEvent commandEvent) {
        if (!starting) {
            String itemName = commandEvent.getItemName();
            Command command = commandEvent.getItemCommand();
            try {
                Item item = itemRegistry.getItem(itemName);
                Iterable<Rule> rules = triggerManager.getRules(COMMAND, item, command);

                executeRules(rules, item, command);
            } catch (ItemNotFoundException e) {
                // ignore commands for non-existent items
            }
        }
    }

    private void receiveThingTrigger(ChannelTriggeredEvent event) {
        String triggerEvent = event.getEvent();
        String channel = event.getChannel().getAsString();

        Iterable<Rule> rules = triggerManager.getRules(TRIGGER, channel, triggerEvent);
        executeRules(rules, event);
    }

    private void receiveThingStatus(ThingStatusInfoChangedEvent event) {
        String thingUid = event.getThingUID().getAsString();
        ThingStatus oldStatus = event.getOldStatusInfo().getStatus();
        ThingStatus newStatus = event.getStatusInfo().getStatus();

        Iterable<Rule> rules = triggerManager.getRules(THINGUPDATE, thingUid, newStatus);
        executeRules(rules);

        if (oldStatus != newStatus) {
            rules = triggerManager.getRules(THINGCHANGE, thingUid, oldStatus, newStatus);
            executeRules(rules, oldStatus);
        }
    }

    private void internalItemAdded(Item item) {
        if (item instanceof GenericItem) {
            GenericItem genericItem = (GenericItem) item;
            genericItem.addStateChangeListener(this);
        }
    }

    private void internalItemRemoved(Item item) {
        if (item instanceof GenericItem) {
            GenericItem genericItem = (GenericItem) item;
            genericItem.removeStateChangeListener(this);
        }
    }

    @Override
    public void modelChanged(String modelName, EventType type) {
        if (modelName.endsWith("rules")) {
            RuleModel model = (RuleModel) modelRepository.getModel(modelName);

            // remove the rules from the trigger sets
            if (type == EventType.REMOVED || type == EventType.MODIFIED) {
                triggerManager.removeRuleModel(model);
            }

            // add new and modified rules to the trigger sets
            if (model != null && (type == EventType.ADDED || type == EventType.MODIFIED)) {
                triggerManager.addRuleModel(model);
                // now execute all rules that are meant to trigger at startup
                scheduleStartupRules();
            }
        }
    }

    private void scheduleStartupRules() {
        ScheduledFuture<?> job = startupJob;
        if (job != null && !job.isCancelled() && !job.isDone()) {
            job.cancel(true);
        }
        startupJob = scheduler.schedule(() -> {
            runStartupRules();
        }, 5, TimeUnit.SECONDS);
    }

    private void runStartupRules() {
        for (Rule rule : triggerManager.getRules(STARTUP)) {
            scheduler.execute(() -> {
                try {
                    Script script = scriptEngine.newScriptFromXExpression(rule.getScript());
                    logger.debug("Executing startup rule '{}'", rule.getName());
                    RuleEvaluationContext context = new RuleEvaluationContext();
                    context.setGlobalContext(RuleContextHelper.getContext(rule, injector));
                    script.execute(context);
                    triggerManager.removeRule(STARTUP, rule);
                } catch (ScriptExecutionException e) {
                    if (!e.getMessage().contains("cannot be resolved to an item or type")) {
                        if (e.getCause() != null) {
                            logger.error("Error during the execution of startup rule '{}': {}", rule.getName(),
                                    e.getCause().getMessage());
                        } else {
                            logger.error("Error during the execution of startup rule '{}': {}", rule.getName(),
                                    e.getMessage());
                        }
                        triggerManager.removeRule(STARTUP, rule);
                    } else {
                        logger.debug("Execution of startup rule '{}' has been postponed as items are still missing: {}",
                                rule.getName(), e.getMessage());
                    }
                }
            });
        }
        // now that we have scheduled the startup rules, we are ready for others as well
        starting = false;
        triggerManager.startTimerRuleExecution();
    }

    protected synchronized void executeRule(Rule rule, RuleEvaluationContext context) {
        scheduler.execute(() -> {
            Script script = scriptEngine.newScriptFromXExpression(rule.getScript());

            logger.debug("Executing rule '{}'", rule.getName());
            context.setGlobalContext(RuleContextHelper.getContext(rule, injector));
            try {
                script.execute(context);
            } catch (Exception e) {
                String msg = e.getMessage();
                if (msg == null) {
                    logger.error("Rule '{}'", rule.getName(), e.getCause());
                } else {
                    logger.error("Rule '{}': {}", rule.getName(), msg);
                }
            }
        });
    }

    protected synchronized void executeRules(Iterable<Rule> rules) {
        for (Rule rule : rules) {
            RuleEvaluationContext context = new RuleEvaluationContext();
            executeRule(rule, context);
        }
    }

    protected synchronized void executeRules(Iterable<Rule> rules, ChannelTriggeredEvent event) {
        for (Rule rule : rules) {
            RuleEvaluationContext context = new RuleEvaluationContext();
            context.newValue(QualifiedName.create(RulesJvmModelInferrer.VAR_RECEIVED_EVENT), event);
            executeRule(rule, context);
        }
    }

    protected synchronized void executeRules(Iterable<Rule> rules, Item item, State state) {
        for (Rule rule : rules) {
            RuleEvaluationContext context = new RuleEvaluationContext();
            context.newValue(QualifiedName.create(RulesJvmModelInferrer.VAR_TRIGGERING_ITEM), item);
            context.newValue(QualifiedName.create(RulesJvmModelInferrer.VAR_NEW_STATE), state);
            executeRule(rule, context);
        }
    }

    protected synchronized void executeRules(Iterable<Rule> rules, Item item, Command command) {
        for (Rule rule : rules) {
            RuleEvaluationContext context = new RuleEvaluationContext();
            context.newValue(QualifiedName.create(RulesJvmModelInferrer.VAR_TRIGGERING_ITEM), item);
            context.newValue(QualifiedName.create(RulesJvmModelInferrer.VAR_RECEIVED_COMMAND), command);
            executeRule(rule, context);
        }
    }

    protected synchronized void executeRules(Iterable<Rule> rules, Item item, State oldState, State newState) {
        for (Rule rule : rules) {
            RuleEvaluationContext context = new RuleEvaluationContext();
            context.newValue(QualifiedName.create(RulesJvmModelInferrer.VAR_TRIGGERING_ITEM), item);
            context.newValue(QualifiedName.create(RulesJvmModelInferrer.VAR_PREVIOUS_STATE), oldState);
            context.newValue(QualifiedName.create(RulesJvmModelInferrer.VAR_NEW_STATE), newState);
            executeRule(rule, context);
        }
    }

    protected synchronized void executeRules(Iterable<Rule> rules, ThingStatus oldThingStatus) {
        for (Rule rule : rules) {
            RuleEvaluationContext context = new RuleEvaluationContext();
            context.newValue(QualifiedName.create(RulesJvmModelInferrer.VAR_PREVIOUS_STATE), oldThingStatus.toString());
            executeRule(rule, context);
        }
    }

    @Override
    public void updated(Item oldItem, Item item) {
        removed(oldItem);
        added(item);
    }

    private final Set<String> subscribedEventTypes = ImmutableSet.of(ItemStateEvent.TYPE, ItemCommandEvent.TYPE,
            ChannelTriggeredEvent.TYPE, ThingStatusInfoChangedEvent.TYPE);

    @Override
    public Set<String> getSubscribedEventTypes() {
        return subscribedEventTypes;
    }

    @Override
    public @Nullable EventFilter getEventFilter() {
        return null;
    }

    @Override
    public void receive(Event event) {
        if (event instanceof ItemCommandEvent) {
            receiveCommand((ItemCommandEvent) event);
        } else if (event instanceof ChannelTriggeredEvent) {
            receiveThingTrigger((ChannelTriggeredEvent) event);
        } else if (event instanceof ThingStatusInfoChangedEvent) {
            receiveThingStatus((ThingStatusInfoChangedEvent) event);
        }
    }

    RuleTriggerManager getTriggerManager() {
        return triggerManager;
    }
}
