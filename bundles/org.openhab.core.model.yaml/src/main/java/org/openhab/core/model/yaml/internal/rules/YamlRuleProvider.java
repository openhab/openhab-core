/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.model.yaml.internal.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleProvider;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.Visibility;
import org.openhab.core.automation.util.ModuleBuilder;
import org.openhab.core.automation.util.RuleBuilder;
import org.openhab.core.common.registry.AbstractProvider;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.model.yaml.YamlElementName;
import org.openhab.core.model.yaml.YamlModelListener;
import org.openhab.core.model.yaml.YamlModelRepository;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyService;
import org.openhab.core.service.StartLevelService;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.core.thing.util.ThingHelper;
import org.openhab.core.util.BundleResolver;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link YamlRuleProvider} is an OSGi service, that allows definition of rules in YAML configuration files. Files can
 * be added, updated or removed at runtime. The rules are automatically registered with
 * {@link org.openhab.core.automation.RuleRegistry}.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { RuleProvider.class, YamlRuleProvider.class, YamlModelListener.class })
public class YamlRuleProvider extends AbstractProvider<Rule>
        implements RuleProvider, YamlModelListener<YamlRuleDTO>, ReadyService.ReadyTracker {

    private static final String XML_THING_TYPE = "openhab.xmlThingTypes"; //TODO: (Nad) Cleanup + JavaDocs

    private final Logger logger = LoggerFactory.getLogger(YamlRuleProvider.class);

    private final YamlModelRepository modelRepository;
    private final BundleResolver bundleResolver;
    private final ConfigDescriptionRegistry configDescriptionRegistry;
    private final LocaleProvider localeProvider;

    private final Set<String> loadedXmlThingTypes = new CopyOnWriteArraySet<>();

    private final Map<String, Collection<Rule>> rulesMap = new ConcurrentHashMap<>();

    private final List<QueueContent> queue = new CopyOnWriteArrayList<>();

    private final Runnable lazyRetryRunnable = new Runnable() {
        @Override
        public void run() {
            logger.debug("Starting lazy retry thread");
//            while (!queue.isEmpty()) {
//                for (QueueContent qc : queue) {
//                    logger.trace("Retry creating thing {}", qc.thingUID);
//                    Rule newRule = qc.thingHandlerFactory.createThing(qc.thingTypeUID, qc.configuration, qc.thingUID,
//                            qc.bridgeUID);
//                    if (newRule != null) {
//                        logger.debug("Successfully loaded thing \'{}\' during retry", qc.thingUID);
//                        Rule oldRule = null;
//                        for (Map.Entry<String, Collection<Rule>> entry : rulesMap.entrySet()) {
//                            oldRule = entry.getValue().stream().filter(t -> t.getUID().equals(newRule.getUID()))
//                                    .findFirst().orElse(null);
//                            if (oldRule != null) {
//                                mergeThing(newRule, oldRule);
//                                Collection<Rule> thingsForModel = Objects
//                                        .requireNonNull(rulesMap.get(entry.getKey()));
//                                thingsForModel.remove(oldRule);
//                                thingsForModel.add(newRule);
//                                logger.debug("Refreshing thing \'{}\' after successful retry", newRule.getUID());
//                                if (!ThingHelper.equals(oldRule, newRule)) {
//                                    notifyListenersAboutUpdatedElement(oldRule, newRule);
//                                }
//                                break;
//                            }
//                        }
//                        if (oldRule == null) {
//                            logger.debug("Refreshing thing \'{}\' after retry failed because thing is not found",
//                                    newRule.getUID());
//                        }
//                        queue.remove(qc);
//                    }
//                }
//                if (!queue.isEmpty()) {
//                    try {
//                        Thread.sleep(1000);
//                    } catch (InterruptedException e) {
//                    }
//                }
//            }
//            logger.debug("Lazy retry thread ran out of work. Good bye.");
        }
    };

    private boolean modelLoaded = false;

    private @Nullable Thread lazyRetryThread;

    private record QueueContent(ThingHandlerFactory thingHandlerFactory, ThingTypeUID thingTypeUID,
            Configuration configuration, ThingUID thingUID, @Nullable ThingUID bridgeUID) {
    }

    @Activate
    public YamlRuleProvider(final @Reference YamlModelRepository modelRepository,
            final @Reference BundleResolver bundleResolver,
            final @Reference ConfigDescriptionRegistry configDescriptionRegistry,
            final @Reference LocaleProvider localeProvider) {
        this.modelRepository = modelRepository;
        this.bundleResolver = bundleResolver;
        this.configDescriptionRegistry = configDescriptionRegistry;
        this.localeProvider = localeProvider;
    }

    @Deactivate
    public void deactivate() {
        queue.clear();
        rulesMap.clear();
        loadedXmlThingTypes.clear();
    }

    @Override
    public Collection<Rule> getAll() {
        return rulesMap.values().stream().flatMap(list -> list.stream()).toList();
    }

    @Override
    public Class<YamlRuleDTO> getElementClass() {
        return YamlRuleDTO.class;
    }

    @Override
    public boolean isVersionSupported(int version) {
        return version >= 2;
    }

    @Override
    public boolean isDeprecated() {
        return false;
    }

    @Override
    public void addedModel(String modelName, Collection<YamlRuleDTO> elements) {
        List<Rule> added = elements.stream().map(this::mapRule).filter(Objects::nonNull).toList();
        Collection<Rule> modelRules = Objects
                .requireNonNull(rulesMap.computeIfAbsent(modelName, k -> new ArrayList<>()));
        modelRules.addAll(added);
        added.forEach(r -> {
            logger.debug("model {} added rule {}", modelName, r.getUID());
            notifyListenersAboutAddedElement(r);
        });
    }

    @Override
    public void updatedModel(String modelName, Collection<YamlRuleDTO> elements) {
        List<Rule> updated = elements.stream().map(this::mapRule).filter(Objects::nonNull).toList();
        Collection<Rule> modelRules = Objects
                .requireNonNull(rulesMap.computeIfAbsent(modelName, k -> new ArrayList<>()));
        updated.forEach(r -> {
            modelRules.stream().filter(rule -> rule.getUID().equals(r.getUID())).findFirst().ifPresentOrElse(oldRule -> {
                modelRules.remove(oldRule);
                modelRules.add(r);
                logger.debug("model {} updated rule {}", modelName, r.getUID());
                notifyListenersAboutUpdatedElement(oldRule, r);
            }, () -> {
                modelRules.add(r);
                logger.debug("model {} added rule {}", modelName, r.getUID());
                notifyListenersAboutAddedElement(r);
            });
        });
    }

    @Override
    public void removedModel(String modelName, Collection<YamlRuleDTO> elements) {
        List<Rule> removed = elements.stream().map(this::mapRule).filter(Objects::nonNull).toList();
        Collection<Rule> modelRules = rulesMap.getOrDefault(modelName, List.of());
        removed.forEach(r -> {
            modelRules.stream().filter(rule -> rule.getUID().equals(r.getUID())).findFirst().ifPresentOrElse(oldRule -> {
                modelRules.remove(oldRule);
                logger.debug("model {} removed rule {}", modelName, r.getUID());
                notifyListenersAboutRemovedElement(oldRule);
            }, () -> logger.debug("model {} rule {} not found", modelName, r.getUID()));
        });
        if (modelRules.isEmpty()) {
            rulesMap.remove(modelName);
        }
    }

    @Reference
    public void setReadyService(final ReadyService readyService) {
        readyService.registerTracker(this);
    }

    public void unsetReadyService(final ReadyService readyService) {
        readyService.unregisterTracker(this);
    }

    @Override
    public void onReadyMarkerAdded(ReadyMarker readyMarker) {
        String type = readyMarker.getType();
        if (StartLevelService.STARTLEVEL_MARKER_TYPE.equals(type)) {
            modelLoaded = Integer.parseInt(readyMarker.getIdentifier()) >= StartLevelService.STARTLEVEL_MODEL;
        } else if (XML_THING_TYPE.equals(type)) {
            String bsn = readyMarker.getIdentifier();
            loadedXmlThingTypes.add(bsn);
//            thingHandlerFactories.stream().filter(factory -> bsn.equals(getBundleName(factory))).forEach(factory -> {
//                thingHandlerFactoryAdded(factory);
//            });
        }
    }

    @Override
    public void onReadyMarkerRemoved(ReadyMarker readyMarker) {
        loadedXmlThingTypes.remove(readyMarker.getIdentifier());
    }

    private void thingHandlerFactoryAdded(ThingHandlerFactory thingHandlerFactory) {
        String bundleName = getBundleName(thingHandlerFactory);
        if (bundleName != null && loadedXmlThingTypes.contains(bundleName)) {
            logger.debug("Refreshing models due to new thing handler factory {}",
                    thingHandlerFactory.getClass().getSimpleName());
            rulesMap.keySet().forEach(modelName -> {
                modelRepository.refreshModelElements(modelName,
                        getElementClass().getAnnotation(YamlElementName.class).value());
            });
        }
    }

    private @Nullable String getBundleName(ThingHandlerFactory thingHandlerFactory) {
        Bundle bundle = bundleResolver.resolveBundle(thingHandlerFactory.getClass());
        return bundle == null ? null : bundle.getSymbolicName();
    }

    private @Nullable Rule mapRule(YamlRuleDTO ruleDto) {

//        public String templateUID;
//        public String name;
//        public Set<@NonNull String> tags;
//        public String description;
//        public Visibility visibility;
//        public Map<@NonNull String, @NonNull Object> configuration;
//        public List<@NonNull ConfigDescriptionParameter> configurationDescriptions;
//        public List<@NonNull YamlConditionDTO> conditions;
//        public List<@NonNull YamlActionDTO> actions;
//        public List<@NonNull YamlModuleDTO> triggers;

        String s;
        RuleBuilder ruleBuilder = RuleBuilder.create(ruleDto.uid);

        if ((s = ruleDto.name) != null) {
            ruleBuilder.withName(s);
        }
        if ((s = ruleDto.templateUID) != null) {
            ruleBuilder.withTemplateUID(s);
        }
        Set<String> tags = ruleDto.tags;
        if (tags != null) {
            ruleBuilder.withTags(tags);
        }
        if ((s = ruleDto.description) != null) {
            ruleBuilder.withDescription(s);
        }
        Visibility visibility = ruleDto.visibility;
        if (visibility != null) {
            ruleBuilder.withVisibility(visibility);
        }
        Map<String, Object> configuration = ruleDto.configuration;
        if (configuration != null) {
            ruleBuilder.withConfiguration(new Configuration(configuration));
        }
        List<ConfigDescriptionParameter> configurationDescriptions = ruleDto.configurationDescriptions;
        if (configurationDescriptions != null) {
            ruleBuilder.withConfigurationDescriptions(configurationDescriptions);
        }
        List<YamlConditionDTO> conditionDTOs = ruleDto.conditions;
        if (conditionDTOs != null) {
            List<Condition> conditions = new ArrayList<>(conditionDTOs.size());
            for (YamlConditionDTO conditionDto : conditionDTOs) {
                conditions.add(ModuleBuilder.createCondition().withId(conditionDto.id).withTypeUID(conditionDto.type)
                    .withConfiguration(new Configuration(conditionDto.configuration)).withInputs(conditionDto.inputs)
                    .withLabel(conditionDto.label).withDescription(conditionDto.description).build());
            }
            ruleBuilder.withConditions(conditions);
        }
        List<YamlActionDTO> actionDTOs = ruleDto.actions;
        if (actionDTOs != null) {
            List<Action> actions = new ArrayList<>(actionDTOs.size());
            for (YamlActionDTO actionDto : actionDTOs) {
                actions.add(ModuleBuilder.createAction().withId(actionDto.id).withTypeUID(actionDto.type)
                    .withConfiguration(new Configuration(actionDto.configuration)).withInputs(actionDto.inputs)
                    .withLabel(actionDto.label).withDescription(actionDto.description).build());
            }
            ruleBuilder.withActions(actions);
        }
        List<YamlModuleDTO> triggerDTOs = ruleDto.triggers;
        if (triggerDTOs != null) {
            List<Trigger> triggers = new ArrayList<>(triggerDTOs.size());
            for (YamlModuleDTO triggerDto : triggerDTOs) {
                triggers.add(ModuleBuilder.createTrigger().withId(triggerDto.id).withTypeUID(triggerDto.type)
                    .withConfiguration(new Configuration(triggerDto.configuration)).withLabel(triggerDto.label)
                    .withDescription(triggerDto.description).build());
            }
            ruleBuilder.withTriggers(triggers);
        }


        return ruleBuilder.build();
    }

    private void mergeThing(Thing target, Thing source) {
        target.setLabel(source.getLabel());
        target.setLocation(source.getLocation());
        target.setBridgeUID(source.getBridgeUID());

        source.getConfiguration().keySet().forEach(paramName -> {
            target.getConfiguration().put(paramName, source.getConfiguration().get(paramName));
        });

        List<Channel> channelsToAdd = new ArrayList<>();
        source.getChannels().forEach(channel -> {
            Channel targetChannel = target.getChannels().stream().filter(c -> c.getUID().equals(channel.getUID()))
                    .findFirst().orElse(null);
            if (targetChannel != null) {
                channel.getConfiguration().keySet().forEach(paramName -> {
                    targetChannel.getConfiguration().put(paramName, channel.getConfiguration().get(paramName));
                });
            } else {
                channelsToAdd.add(channel);
            }
        });

        // add the channels only defined in source list to the target list
        ThingHelper.addChannelsToThing(target, channelsToAdd);
    }
}
