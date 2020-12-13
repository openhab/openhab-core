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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.ManagedRuleProvider;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleProvider;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.automation.RuleStatus;
import org.openhab.core.automation.RuleStatusInfo;
import org.openhab.core.automation.internal.template.RuleTemplateRegistry;
import org.openhab.core.automation.template.RuleTemplate;
import org.openhab.core.automation.template.TemplateRegistry;
import org.openhab.core.automation.type.ModuleTypeRegistry;
import org.openhab.core.automation.util.ConfigurationNormalizer;
import org.openhab.core.automation.util.ReferenceResolver;
import org.openhab.core.automation.util.RuleBuilder;
import org.openhab.core.common.registry.AbstractRegistry;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.service.ReadyService;
import org.openhab.core.storage.StorageService;
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
 * This is the main implementation of the {@link RuleRegistry}, which is registered as a service.
 * The {@link RuleRegistryImpl} provides basic functionality for managing {@link Rule}s.
 * It can be used to
 * <ul>
 * <li>Add Rules with the {@link #add(Rule)}, {@link #added(Provider, Rule)}, {@link #addProvider(RuleProvider)}
 * methods.</li>
 * <li>Get the existing rules with the {@link #get(String)}, {@link #getAll()}, {@link #getByTag(String)},
 * {@link #getByTags(String[])} methods.</li>
 * <li>Update the existing rules with the {@link #update(Rule)}, {@link #updated(Provider, Rule, Rule)} methods.</li>
 * <li>Remove Rules with the {@link #remove(String)} method.</li>
 * </ul>
 * <p>
 * This class also persists the rules into the {@link StorageService} service and restores
 * them when the system is restarted.
 * <p>
 * The {@link RuleRegistry} manages the state (<b>enabled</b> or <b>disabled</b>) of the Rules:
 * <ul>
 * <li>A newly added Rule is always <b>enabled</b>.</li>
 * <li>To check a Rule's state, use the {@link #isEnabled(String)} method.</li>
 * <li>To change a Rule's state, use the {@link #setEnabled(String, boolean)} method.</li>
 * </ul>
 * <p>
 * The {@link RuleRegistry} manages the status of the Rules:
 * <ul>
 * <li>To check a Rule's status info, use the {@link #getStatusInfo(String)} method.</li>
 * <li>The status of a newly added Rule, or a Rule enabled with {@link #setEnabled(String, boolean)}, or an updated
 * Rule, is first set to {@link RuleStatus#UNINITIALIZED}.</li>
 * <li>After a Rule is added or enabled, or updated, a verification procedure is initiated. If the verification of the
 * modules IDs, connections between modules and configuration values of the modules is successful, and the module
 * handlers are correctly set, the status is set to {@link RuleStatus#IDLE}.</li>
 * <li>If some of the module handlers disappear, the Rule will become {@link RuleStatus#UNINITIALIZED} again.</li>
 * <li>If one of the Rule's Triggers is triggered, the Rule becomes {@link RuleStatus#RUNNING}.
 * When the execution is complete, it will become {@link RuleStatus#IDLE} again.</li>
 * <li>If a Rule is disabled with {@link #setEnabled(String, boolean)}, it's status is set to
 * {@link RuleStatus#DISABLED}.</li>
 * </ul>
 *
 * @author Yordan Mihaylov - Initial contribution
 * @author Ana Dimova - Persistence implementation & updating rules from providers
 * @author Kai Kreuzer - refactored (managed) provider and registry implementation and other fixes
 * @author Benedikt Niehues - added events for rules
 * @author Victor Toni - return only copies of {@link Rule}s
 */
@NonNullByDefault
@Component(service = RuleRegistry.class, immediate = true)
public class RuleRegistryImpl extends AbstractRegistry<Rule, String, RuleProvider>
        implements RuleRegistry, RegistryChangeListener<RuleTemplate> {

    private static final String SOURCE = RuleRegistryImpl.class.getSimpleName();

    private final Logger logger = LoggerFactory.getLogger(RuleRegistryImpl.class.getName());

    private @NonNullByDefault({}) ModuleTypeRegistry moduleTypeRegistry;
    private @NonNullByDefault({}) RuleTemplateRegistry templateRegistry;

    /**
     * {@link Map} of template UIDs to rules where these templates participated.
     */
    private final Map<String, Set<String>> mapTemplateToRules = new HashMap<>();

    /**
     * Constructor that is responsible to invoke the super constructor with appropriate providerClazz
     * {@link RuleProvider} - the class of the providers that should be tracked automatically after activation.
     */
    public RuleRegistryImpl() {
        super(RuleProvider.class);
    }

    /**
     * Activates this component. Called from DS.
     *
     * @param componentContext this component context.
     */
    @Override
    @Activate
    protected void activate(BundleContext bundleContext) {
        super.activate(bundleContext);
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    @Override
    protected void setEventPublisher(EventPublisher eventPublisher) {
        super.setEventPublisher(eventPublisher);
    }

    @Override
    protected void unsetEventPublisher(EventPublisher eventPublisher) {
        super.unsetEventPublisher(eventPublisher);
    }

    @Override
    @Reference
    protected void setReadyService(ReadyService readyService) {
        super.setReadyService(readyService);
    }

    @Override
    protected void unsetReadyService(ReadyService readyService) {
        super.unsetReadyService(readyService);
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, name = "ManagedRuleProvider")
    protected void setManagedProvider(ManagedRuleProvider managedProvider) {
        super.setManagedProvider(managedProvider);
    }

    protected void unsetManagedProvider(ManagedRuleProvider managedProvider) {
        super.unsetManagedProvider(managedProvider);
    }

    /**
     * Bind the {@link ModuleTypeRegistry} service - called from DS.
     *
     * @param moduleTypeRegistry a {@link ModuleTypeRegistry} service.
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
    protected void setModuleTypeRegistry(ModuleTypeRegistry moduleTypeRegistry) {
        this.moduleTypeRegistry = moduleTypeRegistry;
    }

    /**
     * Unbind the {@link ModuleTypeRegistry} service - called from DS.
     *
     * @param moduleTypeRegistry a {@link ModuleTypeRegistry} service.
     */
    protected void unsetModuleTypeRegistry(ModuleTypeRegistry moduleTypeRegistry) {
        this.moduleTypeRegistry = null;
    }

    /**
     * Bind the {@link RuleTemplateRegistry} service - called from DS.
     *
     * @param templateRegistry a {@link RuleTemplateRegistry} service.
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
    protected void setTemplateRegistry(TemplateRegistry<RuleTemplate> templateRegistry) {
        if (templateRegistry instanceof RuleTemplateRegistry) {
            this.templateRegistry = (RuleTemplateRegistry) templateRegistry;
            templateRegistry.addRegistryChangeListener(this);
        }
    }

    /**
     * Unbind the {@link RuleTemplateRegistry} service - called from DS.
     *
     * @param templateRegistry a {@link RuleTemplateRegistry} service.
     */
    protected void unsetTemplateRegistry(TemplateRegistry<RuleTemplate> templateRegistry) {
        if (templateRegistry instanceof RuleTemplateRegistry) {
            this.templateRegistry = null;
            templateRegistry.removeRegistryChangeListener(this);
        }
    }

    /**
     * This method is used to register a {@link Rule} into the {@link RuleEngineImpl}. First the {@link Rule} become
     * {@link RuleStatus#UNINITIALIZED}.
     * Then verification procedure will be done and the Rule become {@link RuleStatus#IDLE}.
     * If the verification fails, the Rule will stay {@link RuleStatus#UNINITIALIZED}.
     *
     * @param rule a {@link Rule} instance which have to be added into the {@link RuleEngineImpl}.
     * @return a copy of the added {@link Rule}
     * @throws RuntimeException
     *             when passed module has a required configuration property and it is not specified
     *             in rule definition
     *             nor
     *             in the module's module type definition.
     * @throws IllegalArgumentException
     *             when a module id contains dot or when the rule with the same UID already exists.
     */
    @Override
    public Rule add(Rule rule) {
        super.add(rule);
        Rule ruleCopy = get(rule.getUID());
        if (ruleCopy == null) {
            throw new IllegalStateException();
        }
        return ruleCopy;
    }

    @Override
    protected void notifyListenersAboutAddedElement(Rule element) {
        postRuleAddedEvent(element);
        postRuleStatusInfoEvent(element.getUID(), new RuleStatusInfo(RuleStatus.UNINITIALIZED));
        super.notifyListenersAboutAddedElement(element);
    }

    @Override
    protected void notifyListenersAboutUpdatedElement(Rule oldElement, Rule element) {
        postRuleUpdatedEvent(element, oldElement);
        super.notifyListenersAboutUpdatedElement(oldElement, element);
    }

    /**
     * @see RuleRegistryImpl#postEvent(org.openhab.core.events.Event)
     */
    protected void postRuleAddedEvent(Rule rule) {
        postEvent(RuleEventFactory.createRuleAddedEvent(rule, SOURCE));
    }

    /**
     * @see RuleRegistryImpl#postEvent(org.openhab.core.events.Event)
     */
    protected void postRuleRemovedEvent(Rule rule) {
        postEvent(RuleEventFactory.createRuleRemovedEvent(rule, SOURCE));
    }

    /**
     * @see RuleRegistryImpl#postEvent(org.openhab.core.events.Event)
     */
    protected void postRuleUpdatedEvent(Rule rule, Rule oldRule) {
        postEvent(RuleEventFactory.createRuleUpdatedEvent(rule, oldRule, SOURCE));
    }

    /**
     * This method can be used in order to post events through the openHAB events bus. A common
     * use case is to notify event subscribers about the {@link Rule}'s status change.
     *
     * @param ruleUID the UID of the {@link Rule}, whose status is changed.
     * @param statusInfo the new {@link Rule}s status.
     */
    protected void postRuleStatusInfoEvent(String ruleUID, RuleStatusInfo statusInfo) {
        postEvent(RuleEventFactory.createRuleStatusInfoEvent(statusInfo, ruleUID, SOURCE));
    }

    @Override
    protected void onRemoveElement(Rule rule) {
        String uid = rule.getUID();
        String templateUID = rule.getTemplateUID();
        if (templateUID != null) {
            updateRuleTemplateMapping(templateUID, uid, true);
        }
    }

    @Override
    protected void notifyListenersAboutRemovedElement(Rule element) {
        super.notifyListenersAboutRemovedElement(element);
        postRuleRemovedEvent(element);
    }

    @Override
    public Collection<Rule> getByTag(@Nullable String tag) {
        Collection<Rule> result = new LinkedList<>();
        if (tag == null) {
            forEach(result::add);
        } else {
            forEach(rule -> {
                if (rule.getTags().contains(tag)) {
                    result.add(rule);
                }
            });
        }
        return result;
    }

    @Override
    public Collection<Rule> getByTags(String... tags) {
        Set<String> tagSet = new HashSet<>(Arrays.asList(tags));
        Collection<Rule> result = new LinkedList<>();
        if (tagSet.isEmpty()) {
            forEach(result::add);
        } else {
            forEach(rule -> {
                if (rule.getTags().containsAll(tagSet)) {
                    result.add(rule);
                }
            });
        }
        return result;
    }

    /**
     * The method checks if the rule has to be resolved by template or not. If the rule does not contain tempateUID it
     * returns same rule, otherwise it tries to resolve the rule created from template. If the template is available
     * the method creates a new rule based on triggers, conditions and actions from template. If the template is not
     * available returns the same rule.
     *
     * @param rule a rule defined by template.
     * @return the resolved rule(containing modules defined by the template) or not resolved rule, if the template is
     *         missing.
     */
    private Rule resolveRuleByTemplate(Rule rule) {
        String templateUID = rule.getTemplateUID();
        if (templateUID == null) {
            return rule;
        }
        RuleTemplate template = templateRegistry.get(templateUID);
        String uid = rule.getUID();
        if (template == null) {
            updateRuleTemplateMapping(templateUID, uid, false);
            logger.debug("Rule template {} does not exist.", templateUID);
            return rule;
        } else {
            RuleImpl resolvedRule = (RuleImpl) RuleBuilder
                    .create(template, rule.getUID(), rule.getName(), rule.getConfiguration(), rule.getVisibility())
                    .build();
            resolveConfigurations(resolvedRule);
            updateRuleTemplateMapping(templateUID, uid, true);
            return resolvedRule;
        }
    }

    /**
     * Updates the content of the {@link Map} that maps the template to rules, using it to complete their definitions.
     *
     * @param templateUID the {@link RuleTemplate}'s UID specifying the template.
     * @param ruleUID the {@link Rule}'s UID specifying a rule created by the specified template.
     * @param resolved specifies if the {@link Map} should be updated by adding or removing the specified rule
     *            accordingly if the rule is resolved or not.
     */
    private void updateRuleTemplateMapping(String templateUID, String ruleUID, boolean resolved) {
        synchronized (this) {
            Set<String> ruleUIDs = mapTemplateToRules.get(templateUID);
            if (ruleUIDs == null) {
                ruleUIDs = new HashSet<>();
                mapTemplateToRules.put(templateUID, ruleUIDs);
            }
            if (resolved) {
                ruleUIDs.remove(ruleUID);
            } else {
                ruleUIDs.add(ruleUID);
            }
        }
    }

    @Override
    protected void addProvider(Provider<Rule> provider) {
        super.addProvider(provider);
        forEach(provider, rule -> {
            try {
                Rule resolvedRule = resolveRuleByTemplate(rule);
                if (rule != resolvedRule && provider instanceof ManagedRuleProvider) {
                    update(resolvedRule);
                }
            } catch (IllegalArgumentException e) {
                logger.error("Added rule '{}' is invalid", rule.getUID(), e);
            }
        });
    }

    @Override
    public void added(Provider<Rule> provider, Rule element) {
        String ruleUID = element.getUID();
        Rule resolvedRule = element;
        try {
            resolvedRule = resolveRuleByTemplate(element);
        } catch (IllegalArgumentException e) {
            logger.debug("Added rule '{}' is invalid", ruleUID, e);
        }
        super.added(provider, element);
        if (element != resolvedRule) {
            if (provider instanceof ManagedRuleProvider) {
                update(resolvedRule);
            } else {
                super.updated(provider, element, resolvedRule);
            }
        }
    }

    @Override
    public void updated(Provider<Rule> provider, Rule oldElement, Rule element) {
        String uid = element.getUID();
        if (oldElement != null && uid.equals(oldElement.getUID())) {
            Rule resolvedRule = element;
            try {
                resolvedRule = resolveRuleByTemplate(element);
            } catch (IllegalArgumentException e) {
                logger.error("The rule '{}' is not updated, the new version is invalid", uid, e);
            }
            if (element != resolvedRule && provider instanceof ManagedRuleProvider) {
                update(resolvedRule);
            } else {
                super.updated(provider, oldElement, resolvedRule);
            }
        } else {
            throw new IllegalArgumentException(
                    String.format("The rule '%s' is not updated, not matching with any existing rule", uid));
        }
    }

    @Override
    protected void onAddElement(Rule element) throws IllegalArgumentException {
        String uid = element.getUID();
        try {
            resolveConfigurations(element);
        } catch (IllegalArgumentException e) {
            logger.debug("Added rule '{}' is invalid", uid, e);
        }
    }

    @Override
    protected void onUpdateElement(Rule oldElement, Rule element) throws IllegalArgumentException {
        String uid = element.getUID();
        try {
            resolveConfigurations(element);
        } catch (IllegalArgumentException e) {
            logger.debug("The new version of updated rule '{}' is invalid", uid, e);
        }
    }

    /**
     * This method serves to resolve and normalize the {@link Rule}s configuration values and its module configurations.
     *
     * @param rule the {@link Rule}, whose configuration values and module configuration values should be resolved and
     *            normalized.
     */
    private void resolveConfigurations(Rule rule) {
        List<ConfigDescriptionParameter> configDescriptions = rule.getConfigurationDescriptions();
        Configuration configuration = rule.getConfiguration();
        ConfigurationNormalizer.normalizeConfiguration(configuration,
                ConfigurationNormalizer.getConfigDescriptionMap(configDescriptions));
        Map<String, Object> configurationProperties = configuration.getProperties();
        if (rule.getTemplateUID() == null) {
            String uid = rule.getUID();
            try {
                validateConfiguration(configDescriptions, new HashMap<>(configurationProperties));
                resolveModuleConfigReferences(rule.getModules(), configurationProperties);
                ConfigurationNormalizer.normalizeModuleConfigurations(rule.getModules(), moduleTypeRegistry);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(String.format("The rule '%s' has incorrect configurations", uid), e);
            }
        }
    }

    /**
     * This method serves to validate the {@link Rule}s configuration values.
     *
     * @param rule the {@link Rule}, whose configuration values should be validated.
     */
    private void validateConfiguration(List<ConfigDescriptionParameter> configDescriptions,
            Map<String, Object> configurations) {
        if (configurations == null || configurations.isEmpty()) {
            if (isOptionalConfig(configDescriptions)) {
                return;
            } else {
                StringBuffer statusDescription = new StringBuffer();
                String msg = " '%s';";
                for (ConfigDescriptionParameter configParameter : configDescriptions) {
                    if (configParameter.isRequired()) {
                        String name = configParameter.getName();
                        statusDescription.append(String.format(msg, name));
                    }
                }
                throw new IllegalArgumentException(
                        "Missing required configuration properties: " + statusDescription.toString());
            }
        } else {
            for (ConfigDescriptionParameter configParameter : configDescriptions) {
                String configParameterName = configParameter.getName();
                processValue(configurations.remove(configParameterName), configParameter);
            }
            if (!configurations.isEmpty()) {
                StringBuffer statusDescription = new StringBuffer();
                String msg = " '%s';";
                for (String name : configurations.keySet()) {
                    statusDescription.append(String.format(msg, name));
                }
                throw new IllegalArgumentException("Extra configuration properties: " + statusDescription.toString());
            }
        }
    }

    /**
     * Utility method for {@link Rule}s configuration validation.
     *
     * @param configDescriptions the meta-data for {@link Rule}s configuration, used for validation.
     * @return {@code true} if all configuration properties are optional or {@code false} if there is at least one
     *         required property.
     */
    private boolean isOptionalConfig(List<ConfigDescriptionParameter> configDescriptions) {
        if (configDescriptions != null && !configDescriptions.isEmpty()) {
            boolean required = false;
            Iterator<ConfigDescriptionParameter> i = configDescriptions.iterator();
            while (i.hasNext()) {
                ConfigDescriptionParameter param = i.next();
                required = required || param.isRequired();
            }
            return !required;
        }
        return true;
    }

    /**
     * Utility method for {@link Rule}s configuration validation. Validates the value of a configuration property.
     *
     * @param configValue the value for {@link Rule}s configuration property, that should be validated.
     * @param configParameter the meta-data for {@link Rule}s configuration value, used for validation.
     */
    private void processValue(@Nullable Object configValue, ConfigDescriptionParameter configParameter) {
        if (configValue != null) {
            Type type = configParameter.getType();
            if (configParameter.isMultiple()) {
                if (configValue instanceof List) {
                    @SuppressWarnings("rawtypes")
                    List lConfigValues = (List) configValue;
                    for (Object value : lConfigValues) {
                        if (!checkType(type, value)) {
                            throw new IllegalArgumentException("Unexpected value for configuration property \""
                                    + configParameter.getName() + "\". Expected type: " + type);
                        }
                    }
                } else {
                    throw new IllegalArgumentException(
                            "Unexpected value for configuration property \"" + configParameter.getName()
                                    + "\". Expected is Array with type for elements : " + type.toString() + "!");
                }
            } else if (!checkType(type, configValue)) {
                throw new IllegalArgumentException("Unexpected value for configuration property \""
                        + configParameter.getName() + "\". Expected is " + type.toString() + "!");
            }
        } else if (configParameter.isRequired()) {
            throw new IllegalArgumentException(
                    "Required configuration property missing: \"" + configParameter.getName() + "\"!");
        }
    }

    /**
     * Avoid code duplication in {@link #processValue(Object, ConfigDescriptionParameter)} method.
     *
     * @param type the {@link Type} of a parameter that should be checked.
     * @param configValue the value of a parameter that should be checked.
     * @return <code>true</code> if the type and value matching or <code>false</code> in the opposite.
     */
    private boolean checkType(Type type, Object configValue) {
        switch (type) {
            case TEXT:
                return configValue instanceof String;
            case BOOLEAN:
                return configValue instanceof Boolean;
            case INTEGER:
                return configValue instanceof BigDecimal || configValue instanceof Integer
                        || configValue instanceof Double && ((Double) configValue).intValue() == (Double) configValue;
            case DECIMAL:
                return configValue instanceof BigDecimal || configValue instanceof Double;
        }
        return false;
    }

    /**
     * This method serves to replace module configuration references with the {@link Rule}s configuration values.
     *
     * @param modules the {@link Rule}'s modules, whose configuration values should be resolved.
     * @param ruleConfiguration the {@link Rule}'s configuration values that should be resolve module configuration
     *            values.
     */
    private void resolveModuleConfigReferences(List<? extends Module> modules, Map<String, ?> ruleConfiguration) {
        if (modules != null) {
            StringBuffer statusDescription = new StringBuffer();
            for (Module module : modules) {
                try {
                    ReferenceResolver.updateConfiguration(module.getConfiguration(), ruleConfiguration, logger);
                } catch (IllegalArgumentException e) {
                    statusDescription.append(" in module[" + module.getId() + "]: " + e.getLocalizedMessage() + ";");
                }
            }
            String statusDescriptionStr = statusDescription.toString();
            if (!statusDescriptionStr.isEmpty()) {
                throw new IllegalArgumentException(String.format("Incorrect configurations: %s", statusDescriptionStr));
            }
        }
    }

    @Override
    public void added(RuleTemplate element) {
        String templateUID = element.getUID();
        Set<String> rules = new HashSet<>();
        synchronized (this) {
            Set<String> rulesForResolving = mapTemplateToRules.get(templateUID);
            if (rulesForResolving != null) {
                rules.addAll(rulesForResolving);
            }
        }
        for (String rUID : rules) {
            try {
                Rule unresolvedRule = get(rUID);
                if (unresolvedRule != null) {
                    Rule resolvedRule = resolveRuleByTemplate(unresolvedRule);
                    Provider<Rule> provider = getProvider(rUID);
                    if (provider instanceof ManagedRuleProvider) {
                        update(resolvedRule);
                    } else if (provider != null) {
                        updated(provider, unresolvedRule, unresolvedRule);
                    } else {
                        logger.error(
                                "Resolving the rule '{}' by template '{}' failed because the provider is not known",
                                rUID, templateUID);
                    }
                } else {
                    logger.error(
                            "Resolving the rule '{}' by template '{}' failed because it is not known to the registry",
                            rUID, templateUID);
                }
            } catch (IllegalArgumentException e) {
                logger.error("Resolving the rule '{}' by template '{}' failed", rUID, templateUID, e);
            }
        }
    }

    @Override
    public void removed(RuleTemplate element) {
        // Do nothing - resolved rules are independent from templates
    }

    @Override
    public void updated(RuleTemplate oldElement, RuleTemplate element) {
        // Do nothing - resolved rules are independent from templates
    }
}
