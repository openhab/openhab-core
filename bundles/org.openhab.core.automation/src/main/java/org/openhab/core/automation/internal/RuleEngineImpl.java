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
package org.openhab.core.automation.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.ModuleHandlerCallback;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleManager;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.automation.RuleStatus;
import org.openhab.core.automation.RuleStatusDetail;
import org.openhab.core.automation.RuleStatusInfo;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.events.RuleStatusInfoEvent;
import org.openhab.core.automation.handler.ActionHandler;
import org.openhab.core.automation.handler.ConditionHandler;
import org.openhab.core.automation.handler.ModuleHandler;
import org.openhab.core.automation.handler.ModuleHandlerFactory;
import org.openhab.core.automation.handler.TriggerHandler;
import org.openhab.core.automation.handler.TriggerHandlerCallback;
import org.openhab.core.automation.internal.TriggerHandlerCallbackImpl.TriggerData;
import org.openhab.core.automation.internal.composite.CompositeModuleHandlerFactory;
import org.openhab.core.automation.internal.module.handler.SystemTriggerHandler;
import org.openhab.core.automation.internal.ruleengine.WrappedAction;
import org.openhab.core.automation.internal.ruleengine.WrappedCondition;
import org.openhab.core.automation.internal.ruleengine.WrappedModule;
import org.openhab.core.automation.internal.ruleengine.WrappedRule;
import org.openhab.core.automation.internal.ruleengine.WrappedTrigger;
import org.openhab.core.automation.type.ActionType;
import org.openhab.core.automation.type.CompositeActionType;
import org.openhab.core.automation.type.CompositeConditionType;
import org.openhab.core.automation.type.CompositeTriggerType;
import org.openhab.core.automation.type.ConditionType;
import org.openhab.core.automation.type.Input;
import org.openhab.core.automation.type.ModuleType;
import org.openhab.core.automation.type.ModuleTypeRegistry;
import org.openhab.core.automation.type.Output;
import org.openhab.core.automation.type.TriggerType;
import org.openhab.core.automation.util.ReferenceResolver;
import org.openhab.core.common.NamedThreadFactory;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyMarkerFilter;
import org.openhab.core.service.ReadyService;
import org.openhab.core.service.ReadyService.ReadyTracker;
import org.openhab.core.service.StartLevelService;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible to initialize and execute {@link Rule}s, when the {@link Rule}s are added in rule
 * engine. Each {@link Rule} has associated {@link RuleStatusInfo} object which shows its {@link RuleStatus} and
 * {@link RuleStatusDetail}. The states are self excluded and they are:
 * <LI><b>disabled</b> - the rule is temporary not available. This status is set by the user.
 * <LI><b>uninitialized</b> - the rule is enabled, but it is still not working, because some of the module handlers are
 * not available or its module types or template are not resolved. The initialization problem is described by the status
 * details.
 * <LI><b>idle</b> - the rule is enabled and initialized and it is waiting for triggering events.
 * <LI><b>running</b> - the rule is enabled and initialized and it is executing at the moment. When the execution is
 * finished, it goes to the <b>idle</b> state.
 *
 * @author Yordan Mihaylov - Initial contribution
 * @author Kai Kreuzer - refactored (managed) provider, registry implementation and customized modules
 * @author Benedikt Niehues - change behavior for unregistering ModuleHandler
 * @author Markus Rathgeb - use a managed rule
 * @author Ana Dimova - new reference syntax: list[index], map["key"], bean.field
 */
@Component(immediate = true, service = { RuleManager.class })
@NonNullByDefault
public class RuleEngineImpl implements RuleManager, RegistryChangeListener<ModuleType>, ReadyTracker {

    /**
     * Constant defining separator between module id and output name.
     */
    public static final char OUTPUT_SEPARATOR = '.';

    private static final String DISABLED_RULE_STORAGE = "automation_rules_disabled";

    private static final int RULE_INIT_DELAY = 500;

    private static final ReadyMarker MARKER = new ReadyMarker("ruleengine", "start");

    private final Map<String, WrappedRule> managedRules = new ConcurrentHashMap<>();

    /**
     * {@link Map} holding all created {@link TriggerHandlerCallback} instances, corresponding to each {@link Rule}.
     * There is only one {@link TriggerHandlerCallback} instance per {@link Rule}. The relation is
     * {@link Rule}'s UID to {@link TriggerHandlerCallback} instance.
     */
    private final Map<String, TriggerHandlerCallbackImpl> thCallbacks = new HashMap<>();

    /**
     * {@link Map} holding all {@link ModuleType} UIDs that are available in some rule's module definition. The relation
     * is {@link ModuleType}'s UID to {@link Set} of {@link Rule} UIDs.
     */
    private final Map<String, Set<String>> mapModuleTypeToRules = new HashMap<>();

    /**
     * {@link Map} holding all available {@link ModuleHandlerFactory}s linked with {@link ModuleType}s that they
     * supporting. The relation is {@link ModuleType}'s UID to {@link ModuleHandlerFactory} instance.
     */
    private final Map<String, ModuleHandlerFactory> moduleHandlerFactories;

    /**
     * {@link Set} holding all available {@link ModuleHandlerFactory}s.
     */
    private final Set<ModuleHandlerFactory> allModuleHandlerFactories = new CopyOnWriteArraySet<>();

    /**
     * The storage for the disable information
     */
    private final Storage<Boolean> disabledRulesStorage;

    /**
     * Locker which does not permit rule initialization when the rule engine is stopping.
     */
    private boolean isDisposed = false;

    /**
     * flag to check whether we have reached a start level where we want to start rule execution
     */
    private boolean started = false;

    protected final Logger logger = LoggerFactory.getLogger(RuleEngineImpl.class);

    private final RuleRegistry ruleRegistry;
    private final ReadyService readyService;

    /**
     * {@link Map} holding all Rule context maps. Rule context maps contain dynamic parameters used by the
     * {@link Rule}'s {@link ModuleImpl}s to communicate with each other during the {@link Rule}'s execution.
     * The context map of a {@link Rule} is cleaned when the execution is completed. The relation is
     * {@link Rule}'s UID to Rule context map.
     */
    private final Map<String, Map<String, Object>> contextMap;

    /**
     * This field holds reference to {@link ModuleTypeRegistry}. The {@link RuleEngineImpl} needs it to auto-map
     * connection between rule's modules and to determine module handlers.
     */
    private final ModuleTypeRegistry mtRegistry;

    /**
     * Provides all composite {@link ModuleHandler}s.
     */
    private final CompositeModuleHandlerFactory compositeFactory;

    /**
     * {@link Map} holding all scheduled {@link Rule} re-initialization tasks. The relation is {@link Rule}'s
     * UID to re-initialization task as a {@link Future} instance.
     */
    private final Map<String, Future<?>> scheduleTasks = new HashMap<>(31);

    /**
     * Performs the {@link Rule} re-initialization tasks.
     */
    private @Nullable ScheduledExecutorService executor;

    /**
     * This field holds {@link RegistryChangeListener} that listen for changes in the rule registry.
     * We cannot implement the interface ourselves as we are already a RegistryChangeListener for module types.
     */
    private final RegistryChangeListener<Rule> listener;

    /**
     * Posts an event through the event bus in an asynchronous way. {@link RuleEngineImpl} use it for posting the
     * {@link RuleStatusInfoEvent}.
     */
    private @Nullable EventPublisher eventPublisher;

    private static final String SOURCE = RuleEngineImpl.class.getSimpleName();

    private final ModuleHandlerCallback moduleHandlerCallback = new ModuleHandlerCallback() {

        @Override
        public @Nullable Boolean isEnabled(String ruleUID) {
            return RuleEngineImpl.this.isEnabled(ruleUID);
        }

        @Override
        public void setEnabled(String uid, boolean isEnabled) {
            RuleEngineImpl.this.setEnabled(uid, isEnabled);
        }

        @Override
        public @Nullable RuleStatusInfo getStatusInfo(String ruleUID) {
            return RuleEngineImpl.this.getStatusInfo(ruleUID);
        }

        @Override
        public @Nullable RuleStatus getStatus(String ruleUID) {
            return RuleEngineImpl.this.getStatus(ruleUID);
        }

        @Override
        public void runNow(String uid) {
            RuleEngineImpl.this.runNow(uid);
        }

        @Override
        public void runNow(String uid, boolean considerConditions, @Nullable Map<String, Object> context) {
            RuleEngineImpl.this.runNow(uid, considerConditions, context);
        }
    };

    /**
     * Constructor of {@link RuleEngineImpl}.
     */
    @Activate
    public RuleEngineImpl(final @Reference ModuleTypeRegistry moduleTypeRegistry,
            final @Reference RuleRegistry ruleRegistry, final @Reference StorageService storageService,
            final @Reference ReadyService readyService) {
        this.contextMap = new HashMap<>();
        this.moduleHandlerFactories = new HashMap<>(20);

        this.disabledRulesStorage = storageService.<Boolean> getStorage(DISABLED_RULE_STORAGE,
                this.getClass().getClassLoader());

        mtRegistry = moduleTypeRegistry;
        mtRegistry.addRegistryChangeListener(this);

        compositeFactory = new CompositeModuleHandlerFactory(mtRegistry, this);

        this.ruleRegistry = ruleRegistry;
        this.readyService = readyService;

        listener = new RegistryChangeListener<Rule>() {
            @Override
            public void added(Rule rule) {
                RuleEngineImpl.this.addRule(rule);
            }

            @Override
            public void removed(Rule rule) {
                RuleEngineImpl.this.removeRule(rule.getUID());
            }

            @Override
            public void updated(Rule oldRule, Rule rule) {
                removed(oldRule);
                added(rule);
            }
        };
        ruleRegistry.addRegistryChangeListener(listener);
        for (Rule rule : ruleRegistry.getAll()) {
            addRule(rule);
        }

        readyService.registerTracker(this, new ReadyMarkerFilter().withType(StartLevelService.STARTLEVEL_MARKER_TYPE)
                .withIdentifier(Integer.toString(StartLevelService.STARTLEVEL_RULES)));
    }

    /**
     * The method cleans used resources by rule engine when it is deactivated.
     */
    @Deactivate
    protected void deactivate() {
        synchronized (this) {
            if (isDisposed) {
                return;
            }
            isDisposed = true;
        }

        compositeFactory.deactivate();

        for (Future<?> f : scheduleTasks.values()) {
            f.cancel(true);
        }
        if (scheduleTasks.isEmpty() && executor != null) {
            executor.shutdown();
            executor = null;
        }
        scheduleTasks.clear();
        contextMap.clear();

        mtRegistry.removeRegistryChangeListener(this);

        ruleRegistry.removeRegistryChangeListener(listener);
    }

    @Override
    public void added(ModuleType moduleType) {
        String moduleTypeName = moduleType.getUID();
        for (ModuleHandlerFactory moduleHandlerFactory : allModuleHandlerFactories) {
            Collection<String> moduleTypes = moduleHandlerFactory.getTypes();
            if (moduleTypes.contains(moduleTypeName)) {
                synchronized (this) {
                    this.moduleHandlerFactories.put(moduleTypeName, moduleHandlerFactory);
                }
                break;
            }
        }
        Set<String> rules = null;
        synchronized (this) {
            Set<String> rulesPerModule = mapModuleTypeToRules.get(moduleTypeName);
            if (rulesPerModule != null) {
                rules = new HashSet<>();
                rules.addAll(rulesPerModule);
            }
        }
        if (rules != null) {
            for (String rUID : rules) {
                RuleStatus ruleStatus = getRuleStatus(rUID);
                if (ruleStatus == RuleStatus.UNINITIALIZED) {
                    scheduleRuleInitialization(rUID);
                }
            }
        }
    }

    @Override
    public void removed(ModuleType moduleType) {
        // removing module types does not effect the rule
    }

    @Override
    public void updated(ModuleType oldElement, ModuleType moduleType) {
        if (moduleType.equals(oldElement)) {
            return;
        }
        String moduleTypeName = moduleType.getUID();
        Set<String> rules = null;
        synchronized (this) {
            Set<String> rulesPerModule = mapModuleTypeToRules.get(moduleTypeName);
            if (rulesPerModule != null) {
                rules = new HashSet<>();
                rules.addAll(rulesPerModule);
            }
        }
        if (rules != null) {
            for (String rUID : rules) {
                final RuleStatus ruleStatus = getRuleStatus(rUID);
                if (ruleStatus == null) {
                    continue;
                }
                if (RuleStatus.IDLE.equals(ruleStatus) || RuleStatus.RUNNING.equals(ruleStatus)) {
                    unregister(getManagedRule(rUID), RuleStatusDetail.HANDLER_MISSING_ERROR,
                            "Update Module Type " + moduleType.getUID());
                    setStatus(rUID, new RuleStatusInfo(RuleStatus.INITIALIZING));
                }
            }
        }
    }

    /**
     * Bind the {@link ModuleHandlerFactory} service - called from DS.
     *
     * @param moduleHandlerFactory a {@link ModuleHandlerFactory} service.
     */
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addModuleHandlerFactory(ModuleHandlerFactory moduleHandlerFactory) {
        logger.debug("ModuleHandlerFactory added {}", moduleHandlerFactory.getClass().getSimpleName());
        allModuleHandlerFactories.add(moduleHandlerFactory);
        Collection<String> moduleTypes = moduleHandlerFactory.getTypes();
        Set<String> notInitializedRules = null;
        for (String moduleTypeName : moduleTypes) {
            Set<String> rules = null;
            synchronized (this) {
                moduleHandlerFactories.put(moduleTypeName, moduleHandlerFactory);
                Set<String> rulesPerModule = mapModuleTypeToRules.get(moduleTypeName);
                if (rulesPerModule != null) {
                    rules = new HashSet<>();
                    rules.addAll(rulesPerModule);
                }
            }
            if (rules != null) {
                for (String rUID : rules) {
                    RuleStatus ruleStatus = getRuleStatus(rUID);
                    if (ruleStatus == RuleStatus.UNINITIALIZED) {
                        notInitializedRules = notInitializedRules != null ? notInitializedRules : new HashSet<>(20);
                        notInitializedRules.add(rUID);
                    }
                }
            }
        }
        if (notInitializedRules != null) {
            for (final String rUID : notInitializedRules) {
                scheduleRuleInitialization(rUID);
            }
        }
    }

    /**
     * Unbind the {@link ModuleHandlerFactory} service - called from DS.
     *
     * @param moduleHandlerFactory a {@link ModuleHandlerFactory} service.
     */
    protected void removeModuleHandlerFactory(ModuleHandlerFactory moduleHandlerFactory) {
        allModuleHandlerFactories.remove(moduleHandlerFactory);
        Collection<String> moduleTypes = moduleHandlerFactory.getTypes();
        removeMissingModuleTypes(moduleTypes);
        for (String moduleTypeName : moduleTypes) {
            moduleHandlerFactories.remove(moduleTypeName);
        }
    }

    /**
     * This method add a new rule into rule engine. Scope identity of the Rule is the identity of the caller.
     *
     * @param rule a rule which has to be added.
     */
    protected void addRule(Rule newRule) {
        synchronized (this) {
            if (isDisposed) {
                throw new IllegalStateException("RuleEngineImpl is disposed!");
            }
        }
        final String rUID = newRule.getUID();
        final WrappedRule rule = new WrappedRule(newRule);
        managedRules.put(rUID, rule);
        RuleStatusInfo initStatusInfo = disabledRulesStorage.get(rUID) == null
                ? new RuleStatusInfo(RuleStatus.INITIALIZING)
                : new RuleStatusInfo(RuleStatus.UNINITIALIZED, RuleStatusDetail.DISABLED);
        rule.setStatusInfo(initStatusInfo);

        WrappedRule oldRule = getManagedRule(rUID);
        if (oldRule != null) {
            unregister(oldRule);
        }

        if (isEnabled(rUID) == Boolean.TRUE) {
            setRule(rule);
        }
    }

    /**
     * This method tries to initialize the rule. It uses available {@link ModuleHandlerFactory}s to create
     * {@link ModuleHandler}s for all {@link ModuleImpl}s of the {@link Rule} and to link them. When all the modules
     * have associated module handlers then the {@link Rule} is initialized and it is ready to working. It goes into
     * idle state. Otherwise the Rule stays into not initialized and continue to wait missing handlers, module types
     * or templates.
     *
     * @param rule the rule which tried to be initialized.
     */
    private void setRule(WrappedRule rule) {
        if (isDisposed) {
            return;
        }
        String rUID = rule.getUID();
        setStatus(rUID, new RuleStatusInfo(RuleStatus.INITIALIZING));
        try {
            for (final WrappedAction action : rule.getActions()) {
                updateMapModuleTypeToRule(rUID, action.unwrap().getTypeUID());
                action.setConnections(ConnectionValidator.getConnections(action.getInputs()));
            }
            for (final WrappedCondition condition : rule.getConditions()) {
                updateMapModuleTypeToRule(rUID, condition.unwrap().getTypeUID());
                condition.setConnections(ConnectionValidator.getConnections(condition.getInputs()));
            }
            for (final WrappedTrigger trigger : rule.getTriggers()) {
                updateMapModuleTypeToRule(rUID, trigger.unwrap().getTypeUID());
            }
            validateModuleIDs(rule);
            autoMapConnections(rule);
            ConnectionValidator.validateConnections(mtRegistry, rule.unwrap());
        } catch (IllegalArgumentException e) {
            // change status to UNINITIALIZED
            setStatus(rUID, new RuleStatusInfo(RuleStatus.UNINITIALIZED, RuleStatusDetail.INVALID_RULE,
                    "Validation of rule " + rUID + " has failed! " + e.getLocalizedMessage()));
            return;
        }
        final boolean activated = activateRule(rule);
        if (activated) {
            Future<?> f = scheduleTasks.remove(rUID);
            if (f != null) {
                if (!f.isDone()) {
                    f.cancel(true);
                }
            }
        }
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    protected void unsetEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = null;
    }

    /**
     * This method can be used in order to post events through the openHAB events bus. A common
     * use case is to notify event subscribers about the {@link Rule}'s status change.
     *
     * @param ruleUID the UID of the {@link Rule}, whose status is changed.
     * @param statusInfo the new {@link Rule}s status.
     */
    protected void postRuleStatusInfoEvent(String ruleUID, RuleStatusInfo statusInfo) {
        if (eventPublisher != null) {
            EventPublisher ep = eventPublisher;
            Event event = RuleEventFactory.createRuleStatusInfoEvent(statusInfo, ruleUID, SOURCE);
            try {
                ep.post(event);
            } catch (Exception ex) {
                logger.error("Could not post event of type '{}'.", event.getType(), ex);
            }
        }
    }

    /**
     * This method links modules to corresponding module handlers.
     *
     * @param rUID id of rule containing these modules
     * @param modules list of modules
     * @return null when all modules are connected or list of RuleErrors for missing handlers.
     */
    private <T extends WrappedModule<?, ?>> @Nullable String setModuleHandlers(String rUID, List<T> modules) {
        StringBuilder sb = null;
        for (T mm : modules) {
            final Module m = mm.unwrap();
            try {
                ModuleHandler moduleHandler = getModuleHandler(m, rUID);
                if (moduleHandler != null) {
                    if (mm instanceof WrappedAction) {
                        ((WrappedAction) mm).setModuleHandler((ActionHandler) moduleHandler);
                    } else if (mm instanceof WrappedCondition) {
                        ((WrappedCondition) mm).setModuleHandler((ConditionHandler) moduleHandler);
                    } else if (mm instanceof WrappedTrigger) {
                        ((WrappedTrigger) mm).setModuleHandler((TriggerHandler) moduleHandler);
                    }
                } else {
                    if (sb == null) {
                        sb = new StringBuilder();
                    }
                    String message = "Missing handler '" + m.getTypeUID() + "' for module '" + m.getId() + "'";
                    sb.append(message).append("\n");
                    logger.trace(message);
                }
            } catch (Throwable t) {
                if (sb == null) {
                    sb = new StringBuilder();
                }
                String message = "Getting handler '" + m.getTypeUID() + "' for module '" + m.getId() + "' failed: "
                        + t.getMessage();
                sb.append(message).append("\n");
                logger.trace(message);
            }
        }
        return sb != null ? sb.toString() : null;
    }

    /**
     * Gets {@link TriggerHandlerCallback} for passed {@link Rule}. If it does not exists, a callback object is
     * created.
     *
     * @param rule rule object for which the callback is looking for.
     * @return a {@link TriggerHandlerCallback} corresponding to the passed {@link Rule} object.
     */
    private synchronized TriggerHandlerCallbackImpl getTriggerHandlerCallback(String ruleUID) {
        TriggerHandlerCallbackImpl result = thCallbacks.get(ruleUID);
        if (result == null) {
            result = new TriggerHandlerCallbackImpl(this, ruleUID);
            thCallbacks.put(ruleUID, result);
        }
        return result;
    }

    /**
     * Unlink module handlers from their modules. The method is called when the rule containing these modules goes into
     * {@link RuleStatus#UNINITIALIZED} state.
     *
     * @param modules list of modules which should be disconnected.
     */
    private <T extends WrappedModule<?, ?>> void removeModuleHandlers(List<T> modules, String ruleUID) {
        for (T mm : modules) {
            final Module m = mm.unwrap();
            ModuleHandler handler = mm.getModuleHandler();

            if (handler != null) {
                ModuleHandlerFactory factory = getModuleHandlerFactory(m.getTypeUID());
                if (factory != null) {
                    factory.ungetHandler(m, ruleUID, handler);
                }
                mm.setModuleHandler(null);
            }
        }
    }

    /**
     * This method register the Rule to start working. This is the final step of initialization process where
     * triggers received {@link TriggerHandlerCallback}s object and starts to notify the rule engine when they are
     * triggered. After activating all triggers the rule goes into IDLE state.
     *
     * @param rule an initialized rule which has to starts tracking the triggers.
     */
    private void register(WrappedRule rule) {
        final String ruleUID = rule.getUID();

        TriggerHandlerCallback thCallback = getTriggerHandlerCallback(ruleUID);
        rule.getTriggers().forEach(trigger -> {
            TriggerHandler triggerHandler = trigger.getModuleHandler();
            if (triggerHandler != null) {
                triggerHandler.setCallback(thCallback);
            }
        });
        rule.getConditions().forEach(condition -> {
            ConditionHandler conditionHandler = condition.getModuleHandler();
            if (conditionHandler != null) {
                conditionHandler.setCallback(moduleHandlerCallback);
            }
        });
        rule.getActions().forEach(action -> {
            ActionHandler actionHandler = action.getModuleHandler();
            if (actionHandler != null) {
                actionHandler.setCallback(moduleHandlerCallback);
            }
        });
    }

    /**
     * This method unregister a {@link Rule} and it stops working. It is called when some
     * {@link ModuleHandlerFactory} is disposed or some {@link ModuleType} is updated. The {@link Rule} is
     * available but its state should become {@link RuleStatus#UNINITIALIZED}.
     *
     * @param r rule that should be unregistered.
     * @param detail provides the {@link RuleStatusDetail}, corresponding to the new <b>uninitialized</b> status, should
     *            be {@code null} if the status will be skipped.
     * @param msg provides the {@link RuleStatusInfo} description, corresponding to the new <b>uninitialized</b>
     *            status, should be {@code null} if the status will be skipped.
     */
    private void unregister(@Nullable WrappedRule r, @Nullable RuleStatusDetail detail, @Nullable String msg) {
        if (r != null) {
            unregister(r);
            setStatus(r.getUID(), new RuleStatusInfo(RuleStatus.UNINITIALIZED, detail, msg));
        }
    }

    /**
     * This method unregister a {@link Rule} and it stops working. It is called when the {@link Rule} is
     * removed, updated or disabled. Also it is called when some {@link ModuleHandlerFactory} is disposed or some
     * {@link ModuleType} is updated.
     *
     * @param r rule that should be unregistered.
     */
    private void unregister(WrappedRule r) {
        String rUID = r.getUID();
        synchronized (this) {
            TriggerHandlerCallbackImpl callback = thCallbacks.remove(rUID);
            if (callback != null) {
                callback.dispose();
            }
        }
        removeModuleHandlers(r.getModules(), rUID);
    }

    /**
     * This method is used to obtain a {@link ModuleHandler} for the specified {@link ModuleImpl}.
     *
     * @param m the {@link ModuleImpl} which is looking for a handler.
     * @param ruleUID UID of the {@link Rule} that the specified {@link ModuleImpl} belongs to.
     * @return handler that processing this module. Could be {@code null} if the {@link ModuleHandlerFactory} is not
     *         available.
     */
    private @Nullable ModuleHandler getModuleHandler(Module m, String ruleUID) {
        String moduleTypeId = m.getTypeUID();
        ModuleHandlerFactory mhf = getModuleHandlerFactory(moduleTypeId);
        if (mhf == null || mtRegistry.get(moduleTypeId) == null) {
            return null;
        }
        return mhf.getHandler(m, ruleUID);
    }

    /**
     * Gets the {@link ModuleHandlerFactory} for the {@link ModuleType} with the specified UID.
     *
     * @param moduleTypeId the UID of the {@link ModuleType}.
     * @return the {@link ModuleHandlerFactory} responsible for the {@link ModuleType}.
     */
    public @Nullable ModuleHandlerFactory getModuleHandlerFactory(String moduleTypeId) {
        ModuleHandlerFactory mhf = null;
        synchronized (this) {
            mhf = moduleHandlerFactories.get(moduleTypeId);
        }
        if (mhf == null) {
            ModuleType mt = mtRegistry.get(moduleTypeId);
            if (mt instanceof CompositeTriggerType || //
                    mt instanceof CompositeConditionType || //
                    mt instanceof CompositeActionType) {
                mhf = compositeFactory;
            }
        }
        return mhf;
    }

    /**
     * Updates the {@link ModuleType} to {@link Rule}s mapping. The method adds the {@link Rule}'s UID to the
     * list of
     * {@link Rule}s that use this {@link ModuleType}.
     *
     * @param rUID the UID of the {@link Rule}.
     * @param moduleTypeId the UID of the {@link ModuleType}.
     */
    public synchronized void updateMapModuleTypeToRule(String rUID, String moduleTypeId) {
        Set<String> rules = mapModuleTypeToRules.get(moduleTypeId);
        if (rules == null) {
            rules = new HashSet<>(11);
        }
        rules.add(rUID);
        mapModuleTypeToRules.put(moduleTypeId, rules);
    }

    /**
     * This method removes Rule from the rule engine.
     *
     * @param rUID id of removed {@link Rule}
     * @return true when a rule is deleted, false when there is no rule with such id.
     */
    protected boolean removeRule(String rUID) {
        final WrappedRule r = managedRules.remove(rUID);
        if (r != null) {
            unregister(r);
            synchronized (this) {
                for (Iterator<Map.Entry<String, Set<String>>> it = mapModuleTypeToRules.entrySet().iterator(); it
                        .hasNext();) {
                    Map.Entry<String, Set<String>> e = it.next();
                    Set<String> rules = e.getValue();
                    if (rules.contains(rUID)) {
                        rules.remove(rUID);
                        if (rules.size() < 1) {
                            it.remove();
                        }
                    }
                }
            }
            scheduleTasks.remove(rUID);
            return true;
        }
        return false;
    }

    /**
     * Gets {@link Rule} corresponding to the passed id. This method is used internally and it does not create a
     * copy of the rule.
     *
     * @param rUID unique id of the {@link Rule}
     * @return internal {@link Rule} object
     */
    private @Nullable WrappedRule getManagedRule(String rUID) {
        return managedRules.get(rUID);
    }

    protected @Nullable Rule getRule(String rUID) {
        final WrappedRule managedRule = getManagedRule(rUID);
        return managedRule != null ? managedRule.unwrap() : null;
    }

    @Override
    public synchronized void setEnabled(String uid, boolean enable) {
        final WrappedRule rule = managedRules.get(uid);
        if (rule == null) {
            throw new IllegalArgumentException(String.format("No rule with id=%s was found!", uid));
        }

        if (enable) {
            disabledRulesStorage.remove(uid);
            final RuleStatusInfo statusInfo = rule.getStatusInfo();
            if (statusInfo.getStatus() == RuleStatus.UNINITIALIZED) {
                activateRule(rule);
            }
        } else {
            disabledRulesStorage.put(uid, true);
            unregister(rule, RuleStatusDetail.DISABLED, null);
        }
    }

    /**
     * Activate an existing rule.
     *
     * <p>
     * This method should be called only if:
     * <ul>
     * <li>the rule has not been activated before.
     * <li>the rule has been disabled (uninitialized) and should be enabled now.
     *
     * <p>
     * This method behaves in this way:
     * <ul>
     * <li>Set the module handlers. If there are errors, set the rule status (handler error) and return with error
     * indication.
     * <li>Register the rule. Set the rule status and return with success indication.
     * </ul>
     *
     * @param rule the rule that should be activated
     * @return true if activation succeeded, otherwise false
     */
    private boolean activateRule(final WrappedRule rule) {
        // Check precondition.
        final RuleStatusInfo statusInfo = rule.getStatusInfo();
        final RuleStatus status = statusInfo.getStatus();
        if (status != RuleStatus.UNINITIALIZED && status != RuleStatus.INITIALIZING) {
            logger.warn(
                    "This method should be called only if the rule has not been activated before or has been disabled.");
            return false;
        }

        // Set the module handlers and so check if all handlers are available.
        final String ruleUID = rule.getUID();
        final String errMsgs = setModuleHandlers(ruleUID, rule.getModules());
        if (errMsgs != null) {
            setStatus(ruleUID,
                    new RuleStatusInfo(RuleStatus.UNINITIALIZED, RuleStatusDetail.HANDLER_INITIALIZING_ERROR, errMsgs));
            unregister(rule);
            return false;
        }

        // Register the rule and set idle status.
        register(rule);
        setStatus(ruleUID, new RuleStatusInfo(RuleStatus.IDLE));
        return true;
    }

    @Override
    public @Nullable RuleStatusInfo getStatusInfo(String ruleUID) {
        final WrappedRule rule = managedRules.get(ruleUID);
        if (rule == null) {
            return null;
        }
        return rule.getStatusInfo();
    }

    @Override
    public @Nullable RuleStatus getStatus(String ruleUID) {
        RuleStatusInfo statusInfo = getStatusInfo(ruleUID);
        return statusInfo == null ? null : statusInfo.getStatus();
    }

    @Override
    public @Nullable Boolean isEnabled(String ruleUID) {
        RuleStatusInfo statusInfo = getStatusInfo(ruleUID);
        return statusInfo == null ? null : !RuleStatusDetail.DISABLED.equals(statusInfo.getStatusDetail());
    }

    /**
     * This method updates the status of the {@link Rule}
     *
     * @param ruleUID unique id of the rule
     * @param newStatusInfo the new status of the rule
     */
    private void setStatus(String ruleUID, RuleStatusInfo newStatusInfo) {
        final WrappedRule rule = managedRules.get(ruleUID);
        if (rule == null) {
            return;
        }
        rule.setStatusInfo(newStatusInfo);
        postRuleStatusInfoEvent(ruleUID, newStatusInfo);
    }

    /**
     * Creates and schedules a re-initialization task for the {@link Rule} with the specified UID.
     *
     * @param rUID the UID of the {@link Rule}.
     */
    protected void scheduleRuleInitialization(final String rUID) {
        Future<?> f = scheduleTasks.get(rUID);
        if (f == null || f.isDone()) {
            scheduleTasks.put(rUID, getScheduledExecutor().schedule(() -> {
                final WrappedRule managedRule = getManagedRule(rUID);
                if (managedRule == null) {
                    return;
                }
                setRule(managedRule);
            }, RULE_INIT_DELAY, TimeUnit.MILLISECONDS));
        }
    }

    private void removeMissingModuleTypes(Collection<String> moduleTypes) {
        Map<String, List<String>> mapMissingHandlers = null;
        for (String moduleTypeName : moduleTypes) {
            Set<String> rules = null;
            synchronized (this) {
                rules = mapModuleTypeToRules.get(moduleTypeName);
            }
            if (rules != null) {
                for (String rUID : rules) {
                    RuleStatus ruleStatus = getRuleStatus(rUID);
                    if (ruleStatus == null) {
                        continue;
                    }
                    switch (ruleStatus) {
                        case RUNNING:
                        case IDLE:
                            mapMissingHandlers = mapMissingHandlers != null ? mapMissingHandlers : new HashMap<>(20);
                            List<String> list = mapMissingHandlers.get(rUID);
                            if (list == null) {
                                list = new ArrayList<>(5);
                            }
                            list.add(moduleTypeName);
                            mapMissingHandlers.put(rUID, list);
                            break;
                        default:
                            break;
                    }
                }
            }
        } // for
        if (mapMissingHandlers != null) {
            for (Entry<String, List<String>> e : mapMissingHandlers.entrySet()) {
                String rUID = e.getKey();
                List<String> missingTypes = e.getValue();
                StringBuffer sb = new StringBuffer();
                sb.append("Missing handlers: ");
                for (String typeUID : missingTypes) {
                    sb.append(typeUID).append(", ");
                }
                unregister(getManagedRule(rUID), RuleStatusDetail.HANDLER_MISSING_ERROR,
                        sb.substring(0, sb.length() - 2));
            }
        }
    }

    /**
     * This method runs a {@link Rule}. It is called by the {@link TriggerHandlerCallback}'s thread when a new
     * {@link TriggerData} is available. This method switches
     *
     * @param ruleUID the {@link Rule} which has to evaluate new {@link TriggerData}.
     * @param td {@link TriggerData} object containing new values for {@link Trigger}'s {@link Output}s
     */
    protected void runRule(String ruleUID, TriggerHandlerCallbackImpl.TriggerData td) {
        if (thCallbacks.get(ruleUID) == null) {
            // the rule was unregistered
            return;
        }
        if (!started) {
            logger.debug("Rule engine not yet started - not executing rule '{}',", ruleUID);
            return;
        }
        synchronized (this) {
            final RuleStatus ruleStatus = getRuleStatus(ruleUID);
            if (ruleStatus != null && ruleStatus != RuleStatus.IDLE) {
                logger.error("Failed to execute rule ‘{}' with status '{}'", ruleUID, ruleStatus.name());
                return;
            }
            // change state to RUNNING
            setStatus(ruleUID, new RuleStatusInfo(RuleStatus.RUNNING));
        }
        try {
            clearContext(ruleUID);

            setTriggerOutputs(ruleUID, td);
            final WrappedRule rule = managedRules.get(ruleUID);
            if (rule != null) {
                boolean isSatisfied = calculateConditions(rule);
                if (isSatisfied) {
                    executeActions(rule, true);
                    logger.debug("The rule '{}' is executed.", ruleUID);
                } else {
                    logger.debug("The rule '{}' is NOT executed, since it has unsatisfied conditions.", ruleUID);
                }
            }
        } catch (Throwable t) {
            logger.error("Failed to execute rule '{}': {}", ruleUID, t.getMessage());
            logger.debug("", t);
        }
        // change state to IDLE only if the rule has not been DISABLED.
        synchronized (this) {
            if (getRuleStatus(ruleUID) == RuleStatus.RUNNING) {
                setStatus(ruleUID, new RuleStatusInfo(RuleStatus.IDLE));
            }
        }
    }

    @Override
    public void runNow(String ruleUID, boolean considerConditions, @Nullable Map<String, Object> context) {
        final WrappedRule rule = getManagedRule(ruleUID);
        if (rule == null) {
            logger.warn("Failed to execute rule '{}': Invalid Rule UID", ruleUID);
            return;
        }
        synchronized (this) {
            final RuleStatus ruleStatus = getRuleStatus(ruleUID);
            if (ruleStatus != null && ruleStatus != RuleStatus.IDLE) {
                logger.error("Failed to execute rule ‘{}' with status '{}'", ruleUID, ruleStatus.name());
                return;
            }
            // change state to RUNNING
            setStatus(ruleUID, new RuleStatusInfo(RuleStatus.RUNNING));
        }
        try {
            clearContext(ruleUID);
            if (context != null && !context.isEmpty()) {
                getContext(ruleUID, null).putAll(context);
            }
            if (considerConditions) {
                if (calculateConditions(rule)) {
                    executeActions(rule, false);
                }
            } else {
                executeActions(rule, false);
            }
            logger.debug("The rule '{}' is executed.", ruleUID);
        } catch (Throwable t) {
            logger.error("Failed to execute rule '{}': ", ruleUID, t);
        }
        // change state to IDLE only if the rule has not been DISABLED.
        synchronized (this) {
            if (getRuleStatus(ruleUID) == RuleStatus.RUNNING) {
                setStatus(ruleUID, new RuleStatusInfo(RuleStatus.IDLE));
            }
        }
    }

    @Override
    public void runNow(String ruleUID) {
        runNow(ruleUID, false, null);
    }

    /**
     * Clears all dynamic parameters from the {@link Rule}'s context.
     *
     * @param ruleUID the UID of the rule whose context must be cleared.
     */
    protected void clearContext(String ruleUID) {
        Map<String, Object> context = contextMap.get(ruleUID);
        if (context != null) {
            context.clear();
        }
    }

    /**
     * The method updates {@link Output} of the {@link Trigger} with a new triggered data.
     *
     * @param td new Triggered data.
     */
    private void setTriggerOutputs(String ruleUID, TriggerData td) {
        Trigger t = td.getTrigger();
        updateContext(ruleUID, t.getId(), td.getOutputs());
    }

    /**
     * Updates current context of rule engine.
     *
     * @param moduleUID uid of updated module.
     *
     * @param outputs new output values.
     */
    private void updateContext(String ruleUID, String moduleUID, @Nullable Map<String, ?> outputs) {
        Map<String, Object> context = getContext(ruleUID, null);
        if (outputs != null) {
            for (Map.Entry<String, ?> entry : outputs.entrySet()) {
                String key = moduleUID + OUTPUT_SEPARATOR + entry.getKey();
                context.put(key, entry.getValue());
            }
        }
    }

    /**
     * @return copy of current context in rule engine
     */
    private Map<String, Object> getContext(String ruleUID, @Nullable Set<Connection> connections) {
        Map<String, Object> context = contextMap.get(ruleUID);
        if (context == null) {
            context = new HashMap<>();
            contextMap.put(ruleUID, context);
        }
        if (connections != null) {
            StringBuffer sb = new StringBuffer();
            for (Connection c : connections) {
                String outputModuleId = c.getOutputModuleId();
                if (outputModuleId != null) {
                    sb.append(outputModuleId).append(OUTPUT_SEPARATOR).append(c.getOutputName());
                    Object outputValue = context.get(sb.toString());
                    sb.setLength(0);
                    if (outputValue != null) {
                        if (c.getReference() == null) {
                            context.put(c.getInputName(), outputValue);
                        } else {
                            context.put(c.getInputName(), ReferenceResolver.resolveComplexDataReference(outputValue,
                                    ReferenceResolver.splitReferenceToTokens(c.getReference())));
                        }
                    }
                } else {
                    // get reference from context
                    String ref = c.getReference();
                    final Object value = ReferenceResolver.resolveReference(ref, context);

                    if (value != null) {
                        context.put(c.getInputName(), value);
                    }
                }
            }
        }
        return context;
    }

    /**
     * This method checks if all rule's condition are satisfied or not.
     *
     * @param rule the checked rule
     * @return true when all conditions of the rule are satisfied, false otherwise.
     */
    private boolean calculateConditions(WrappedRule rule) {
        List<WrappedCondition> conditions = rule.getConditions();
        if (conditions.isEmpty()) {
            return true;
        }
        final String ruleUID = rule.getUID();
        RuleStatus ruleStatus = null;
        for (WrappedCondition wrappedCondition : conditions) {
            ruleStatus = getRuleStatus(ruleUID);
            if (ruleStatus != RuleStatus.RUNNING) {
                return false;
            }
            final Condition condition = wrappedCondition.unwrap();
            ConditionHandler tHandler = wrappedCondition.getModuleHandler();
            Map<String, Object> context = getContext(ruleUID, wrappedCondition.getConnections());
            if (tHandler != null && !tHandler.isSatisfied(Collections.unmodifiableMap(context))) {
                logger.debug("The condition '{}' of rule '{}' is unsatisfied.", condition.getId(), ruleUID);
                return false;
            }
        }
        return true;
    }

    /**
     * This method evaluates actions of the {@link Rule} and set their {@link Output}s when they exists.
     *
     * @param rule executed rule.
     */
    private void executeActions(WrappedRule rule, boolean stopOnFirstFail) {
        final String ruleUID = rule.getUID();
        final Collection<WrappedAction> actions = rule.getActions();
        if (actions.isEmpty()) {
            return;
        }
        RuleStatus ruleStatus = null;
        for (WrappedAction wrappedAction : actions) {
            ruleStatus = getRuleStatus(ruleUID);
            if (ruleStatus != RuleStatus.RUNNING) {
                return;
            }
            final Action action = wrappedAction.unwrap();
            ActionHandler aHandler = wrappedAction.getModuleHandler();
            if (aHandler != null) {
                Map<String, Object> context = getContext(ruleUID, wrappedAction.getConnections());
                try {
                    Map<String, ?> outputs = aHandler.execute(Collections.unmodifiableMap(context));
                    if (outputs != null) {
                        context = getContext(ruleUID, null);
                        updateContext(ruleUID, action.getId(), outputs);
                    }
                } catch (Throwable t) {
                    String errMessage = "Fail to execute action: " + action.getId();
                    if (stopOnFirstFail) {
                        RuntimeException re = new RuntimeException(errMessage, t);
                        throw re;
                    } else {
                        logger.warn(errMessage, t);
                    }
                }
            }
        }
    }

    /**
     * This method gets rule's status object.
     *
     * @param rUID rule's UID
     * @return status of the rule or null when such rule does not exists.
     */
    protected @Nullable RuleStatus getRuleStatus(String rUID) {
        RuleStatusInfo info = getStatusInfo(rUID);
        if (info != null) {
            return info.getStatus();
        }
        return null;
    }

    private ScheduledExecutorService getScheduledExecutor() {
        final ScheduledExecutorService currentExecutor = executor;
        if (currentExecutor != null && !currentExecutor.isShutdown()) {
            return currentExecutor;
        }
        final ScheduledExecutorService newExecutor = Executors
                .newSingleThreadScheduledExecutor(new NamedThreadFactory("ruleengine"));
        executor = newExecutor;
        return newExecutor;
    }

    /**
     * Validates IDs of modules. The module ids must be alphanumeric with only underscores and dashes.
     *
     * @param rule the rule to validate
     * @throws IllegalArgumentException when a module id contains illegal characters
     */
    private void validateModuleIDs(WrappedRule rule) {
        for (final WrappedModule<?, ?> mm : rule.getModules()) {
            final Module m = mm.unwrap();
            String mId = m.getId();
            if (!mId.matches("[A-Za-z0-9_-]*")) {
                rule.setStatusInfo(new RuleStatusInfo(RuleStatus.UNINITIALIZED, RuleStatusDetail.INVALID_RULE,
                        "It is null or not fit to the pattern: [A-Za-z0-9_-]*"));
                throw new IllegalArgumentException(
                        "Invalid module uid: " + mId + ". It is null or not fit to the pattern: [A-Za-z0-9_-]*");
            }
        }
    }

    /**
     * The auto mapping tries to link not connected module inputs to output of other modules. The auto mapping will link
     * input to output only when following criteria are done: 1) input must not be connected. The auto mapping will not
     * overwrite explicit connections done by the user. 2) input tags must be subset of the output tags. 3) condition
     * inputs can be connected only to triggers' outputs 4) action outputs can be connected to both conditions and
     * actions
     * outputs 5) There is only one output, based on previous criteria, where the input can connect to. If more then one
     * candidate outputs exists for connection, this is a conflict and the auto mapping leaves the input unconnected.
     * Auto mapping is always applied when the rule is added or updated. It changes initial value of inputs of
     * conditions and actions participating in the rule. If an "auto map" connection has to be removed, the tags of
     * corresponding input/output have to be changed.
     *
     * @param rule updated rule
     */
    private void autoMapConnections(WrappedRule rule) {
        Map<Set<String>, OutputRef> triggerOutputTags = new HashMap<>(11);
        for (WrappedTrigger mt : rule.getTriggers()) {
            final Trigger t = mt.unwrap();
            TriggerType tt = (TriggerType) mtRegistry.get(t.getTypeUID());
            if (tt != null) {
                initTagsMap(t.getId(), tt.getOutputs(), triggerOutputTags);
            }
        }
        Map<Set<String>, OutputRef> actionOutputTags = new HashMap<>(11);
        for (WrappedAction ma : rule.getActions()) {
            final Action a = ma.unwrap();
            ActionType at = (ActionType) mtRegistry.get(a.getTypeUID());
            if (at != null) {
                initTagsMap(a.getId(), at.getOutputs(), actionOutputTags);
            }
        }
        // auto mapping of conditions
        if (!triggerOutputTags.isEmpty()) {
            for (WrappedCondition mc : rule.getConditions()) {
                final Condition c = mc.unwrap();
                boolean isConnectionChanged = false;
                ConditionType ct = (ConditionType) mtRegistry.get(c.getTypeUID());
                if (ct != null) {
                    Set<Connection> connections = copyConnections(mc.getConnections());

                    for (Input input : ct.getInputs()) {
                        if (isConnected(input, connections)) {
                            continue; // the input is already connected. Skip it.
                        }
                        if (addAutoMapConnections(input, triggerOutputTags, connections)) {
                            isConnectionChanged = true;
                        }
                    }
                    if (isConnectionChanged) {
                        // update condition inputs
                        Map<String, String> connectionMap = getConnectionMap(connections);
                        mc.setInputs(connectionMap);
                        mc.setConnections(connections);
                    }
                }
            }
        }
        // auto mapping of actions
        if (!triggerOutputTags.isEmpty() || !actionOutputTags.isEmpty()) {
            for (final WrappedAction ma : rule.getActions()) {
                final Action a = ma.unwrap();
                boolean isConnectionChanged = false;
                ActionType at = (ActionType) mtRegistry.get(a.getTypeUID());
                if (at != null) {
                    Set<Connection> connections = copyConnections(ma.getConnections());
                    for (Input input : at.getInputs()) {
                        if (isConnected(input, connections)) {
                            continue; // the input is already connected. Skip it.
                        }
                        if (addAutoMapConnections(input, triggerOutputTags, connections)) {
                            isConnectionChanged = true;
                        }
                        if (addAutoMapConnections(input, actionOutputTags, connections)) {
                            isConnectionChanged = true;
                        }
                    }
                    if (isConnectionChanged) {
                        // update condition inputs
                        Map<String, String> connectionMap = getConnectionMap(connections);
                        ma.setInputs(connectionMap);
                        ma.setConnections(connections);
                    }
                }
            }
        }
    }

    /**
     * Try to connect a free input to available outputs.
     *
     * @param input a free input which has to be connected
     * @param outputTagMap a map of set of tags to outptu references
     * @param currentConnections current connections of this module
     * @return true when only one output which meets auto mapping criteria is found. False otherwise.
     */
    private boolean addAutoMapConnections(Input input, Map<Set<String>, OutputRef> outputTagMap,
            Set<Connection> currentConnections) {
        boolean result = false;
        Set<String> inputTags = input.getTags();
        OutputRef outputRef = null;
        boolean conflict = false;
        if (!inputTags.isEmpty()) {
            for (Set<String> outTags : outputTagMap.keySet()) {
                if (outTags.containsAll(inputTags)) { // input tags must be subset of the output ones
                    if (outputRef == null) {
                        outputRef = outputTagMap.get(outTags);
                    } else {
                        conflict = true; // already exist candidate for autoMap
                        break;
                    }
                }
            }
            if (!conflict && outputRef != null) {
                currentConnections
                        .add(new Connection(input.getName(), outputRef.getModuleId(), outputRef.getOutputName(), null));
                result = true;
            }
        }
        return result;
    }

    private void initTagsMap(String moduleId, List<Output> outputs, Map<Set<String>, OutputRef> tagMap) {
        for (Output output : outputs) {
            Set<String> tags = output.getTags();
            if (!tags.isEmpty()) {
                if (tagMap.get(tags) != null) {
                    // this set of output tags already exists. (conflict)
                    tagMap.remove(tags);
                } else {
                    tagMap.put(tags, new OutputRef(moduleId, output.getName()));
                }
            }
        }
    }

    private boolean isConnected(Input input, Set<Connection> connections) {
        for (Connection connection : connections) {
            if (connection.getInputName().equals(input.getName())) {
                return true;
            }
        }
        return false;
    }

    private Map<String, String> getConnectionMap(Set<Connection> connections) {
        Map<String, String> connectionMap = new HashMap<>();
        for (Connection connection : connections) {
            connectionMap.put(connection.getInputName(),
                    connection.getOutputModuleId() + "." + connection.getOutputName());
        }
        return connectionMap;
    }

    /**
     * Utility method creating deep copy of passed connection set.
     *
     * @param connections connections used by this module.
     * @return copy of passed connections.
     */
    private Set<Connection> copyConnections(Set<Connection> connections) {
        Set<Connection> result = new HashSet<>(connections.size());
        for (Connection c : connections) {
            result.add(new Connection(c.getInputName(), c.getOutputModuleId(), c.getOutputName(), c.getReference()));
        }
        return result;
    }

    class OutputRef {

        private final String moduleId;
        private final String outputName;

        public OutputRef(String moduleId, String outputName) {
            this.moduleId = moduleId;
            this.outputName = outputName;
        }

        public String getModuleId() {
            return moduleId;
        }

        public String getOutputName() {
            return outputName;
        }
    }

    @Override
    public void onReadyMarkerAdded(ReadyMarker readyMarker) {
        executeRulesWithStartLevel();
    }

    @Override
    public void onReadyMarkerRemoved(ReadyMarker readyMarker) {
        started = false;
    }

    private void executeRulesWithStartLevel() {
        getScheduledExecutor().submit(() -> {
            ruleRegistry.getAll().stream() //
                    .filter(r -> mustTrigger(r)) //
                    .forEach(r -> runNow(r.getUID(), true,
                            Map.of(SystemTriggerHandler.OUT_STARTLEVEL, StartLevelService.STARTLEVEL_RULES)));
            started = true;
            readyService.markReady(MARKER);
            logger.info("Rule engine started.");
        });
    }

    private boolean mustTrigger(Rule r) {
        for (Trigger t : r.getTriggers()) {
            if (t.getTypeUID() == SystemTriggerHandler.STARTLEVEL_MODULE_TYPE_ID) {
                Object slObj = t.getConfiguration().get(SystemTriggerHandler.CFG_STARTLEVEL);
                try {
                    Integer sl = Integer.valueOf(slObj.toString());
                    if (sl < StartLevelService.STARTLEVEL_RULEENGINE) {
                        return true;
                    }
                } catch (NumberFormatException e) {
                    logger.warn("Configuration '{}' is not a valid start level!", slObj);
                }
            }
        }
        return false;
    }

    /**
     * Returns whether the rule engine has been started
     *
     * @return true, if the rule engine has been started
     */
    public boolean isStarted() {
        return started;
    }
}
