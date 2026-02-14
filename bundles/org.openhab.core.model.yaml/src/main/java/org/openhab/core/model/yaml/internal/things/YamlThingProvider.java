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
package org.openhab.core.model.yaml.internal.things;

import static org.openhab.core.model.yaml.YamlModelUtils.isIsolatedModel;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.AbstractUID;
import org.openhab.core.common.registry.AbstractProvider;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.ConfigUtil;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.model.yaml.YamlModelListener;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyService;
import org.openhab.core.service.StartLevelService;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingProvider;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.UID;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.core.thing.binding.builder.BridgeBuilder;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.type.AutoUpdatePolicy;
import org.openhab.core.thing.type.ChannelDefinition;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeRegistry;
import org.openhab.core.thing.util.ThingHelper;
import org.openhab.core.util.BundleResolver;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link YamlThingProvider} is an OSGi service, that allows to define things in YAML configuration files.
 * Files can be added, updated or removed at runtime.
 * These things are automatically exposed to the {@link org.openhab.core.thing.ThingRegistry}.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { ThingProvider.class, YamlThingProvider.class, YamlModelListener.class })
public class YamlThingProvider extends AbstractProvider<Thing>
        implements ThingProvider, YamlModelListener<YamlThingDTO>, ReadyService.ReadyTracker {

    private static final String XML_THING_TYPE = "openhab.xmlThingTypes";

    private final Logger logger = LoggerFactory.getLogger(YamlThingProvider.class);

    private final BundleResolver bundleResolver;
    private final ThingTypeRegistry thingTypeRegistry;
    private final ChannelTypeRegistry channelTypeRegistry;
    private final ConfigDescriptionRegistry configDescriptionRegistry;
    private final LocaleProvider localeProvider;

    private final List<ThingHandlerFactory> thingHandlerFactories = new CopyOnWriteArrayList<>();
    private final Set<String> loadedXmlThingTypes = new CopyOnWriteArraySet<>();

    private final Map<String, Collection<Thing>> thingsMap = new ConcurrentHashMap<>();

    private final List<QueueContent> queue = new CopyOnWriteArrayList<>();

    private final Runnable lazyRetryRunnable = new Runnable() {
        @Override
        public void run() {
            logger.debug("Starting lazy retry thread");
            while (!queue.isEmpty()) {
                for (QueueContent qc : queue) {
                    if (retryCreateThing(qc.thingHandlerFactory, qc.thingTypeUID, qc.configuration, qc.thingUID,
                            qc.bridgeUID)) {
                        queue.remove(qc);
                    }
                }
                if (!queue.isEmpty()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }
            }
            logger.debug("Lazy retry thread ran out of work. Good bye.");
        }
    };

    private boolean modelLoaded = false;

    private @Nullable Thread lazyRetryThread;

    private record QueueContent(ThingHandlerFactory thingHandlerFactory, ThingTypeUID thingTypeUID,
            Configuration configuration, ThingUID thingUID, @Nullable ThingUID bridgeUID) {
    }

    @Activate
    public YamlThingProvider(final @Reference BundleResolver bundleResolver,
            final @Reference ThingTypeRegistry thingTypeRegistry,
            final @Reference ChannelTypeRegistry channelTypeRegistry,
            final @Reference ConfigDescriptionRegistry configDescriptionRegistry,
            final @Reference LocaleProvider localeProvider) {
        this.bundleResolver = bundleResolver;
        this.thingTypeRegistry = thingTypeRegistry;
        this.channelTypeRegistry = channelTypeRegistry;
        this.configDescriptionRegistry = configDescriptionRegistry;
        this.localeProvider = localeProvider;
    }

    @Deactivate
    public void deactivate() {
        queue.clear();
        thingsMap.clear();
        loadedXmlThingTypes.clear();
    }

    @Override
    public Collection<Thing> getAll() {
        // Ignore isolated models
        return thingsMap.keySet().stream().filter(name -> !isIsolatedModel(name))
                .map(name -> thingsMap.getOrDefault(name, List.of())).flatMap(list -> list.stream()).toList();
    }

    public Collection<Thing> getAllFromModel(String modelName) {
        return thingsMap.getOrDefault(modelName, List.of());
    }

    @Override
    public Class<YamlThingDTO> getElementClass() {
        return YamlThingDTO.class;
    }

    @Override
    public boolean isVersionSupported(int version) {
        return version >= 1;
    }

    @Override
    public boolean isDeprecated() {
        return false;
    }

    @Override
    public void addedModel(String modelName, Collection<YamlThingDTO> elements) {
        boolean isolated = isIsolatedModel(modelName);
        List<Thing> added = elements.stream().map(t -> mapThing(t, isolated)).filter(Objects::nonNull).toList();
        Collection<Thing> modelThings = Objects
                .requireNonNull(thingsMap.computeIfAbsent(modelName, k -> new ArrayList<>()));
        modelThings.addAll(added);
        added.forEach(t -> {
            logger.debug("model {} added thing {}", modelName, t.getUID());
            if (!isolated) {
                notifyListenersAboutAddedElement(t);
            }
        });
    }

    @Override
    public void updatedModel(String modelName, Collection<YamlThingDTO> elements) {
        boolean isolated = isIsolatedModel(modelName);
        List<Thing> updated = elements.stream().map(t -> mapThing(t, isolated)).filter(Objects::nonNull).toList();
        Collection<Thing> modelThings = Objects
                .requireNonNull(thingsMap.computeIfAbsent(modelName, k -> new ArrayList<>()));
        updated.forEach(t -> {
            modelThings.stream().filter(th -> th.getUID().equals(t.getUID())).findFirst().ifPresentOrElse(oldThing -> {
                modelThings.remove(oldThing);
                modelThings.add(t);
                logger.debug("model {} updated thing {}", modelName, t.getUID());
                if (!isolated) {
                    notifyListenersAboutUpdatedElement(oldThing, t);
                }
            }, () -> {
                modelThings.add(t);
                logger.debug("model {} added thing {}", modelName, t.getUID());
                if (!isolated) {
                    notifyListenersAboutAddedElement(t);
                }
            });
        });
    }

    @Override
    public void removedModel(String modelName, Collection<YamlThingDTO> elements) {
        boolean isolated = isIsolatedModel(modelName);
        Collection<Thing> modelThings = thingsMap.getOrDefault(modelName, List.of());
        elements.stream().map(this::buildThingUID).filter(Objects::nonNull).forEach(uid -> {
            modelThings.stream().filter(th -> th.getUID().equals(uid)).findFirst().ifPresentOrElse(oldThing -> {
                modelThings.remove(oldThing);
                logger.debug("model {} removed thing {}", modelName, uid);
                if (!isolated) {
                    notifyListenersAboutRemovedElement(oldThing);
                }
            }, () -> logger.debug("model {} thing {} not found", modelName, uid));
        });
        if (modelThings.isEmpty()) {
            thingsMap.remove(modelName);
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addThingHandlerFactory(final ThingHandlerFactory thingHandlerFactory) {
        logger.debug("addThingHandlerFactory {}", thingHandlerFactory.getClass().getSimpleName());
        thingHandlerFactories.add(thingHandlerFactory);
        thingHandlerFactoryAdded(thingHandlerFactory);
    }

    public void removeThingHandlerFactory(final ThingHandlerFactory thingHandlerFactory) {
        thingHandlerFactories.remove(thingHandlerFactory);
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
            thingHandlerFactories.stream().filter(factory -> bsn.equals(getBundleName(factory))).forEach(factory -> {
                thingHandlerFactoryAdded(factory);
            });
        }
    }

    @Override
    public void onReadyMarkerRemoved(ReadyMarker readyMarker) {
        loadedXmlThingTypes.remove(readyMarker.getIdentifier());
    }

    private void thingHandlerFactoryAdded(ThingHandlerFactory handlerFactory) {
        logger.debug("thingHandlerFactoryAdded {} isThingHandlerFactoryReady={}",
                handlerFactory.getClass().getSimpleName(), isThingHandlerFactoryReady(handlerFactory));
        if (isThingHandlerFactoryReady(handlerFactory)) {
            if (!thingsMap.isEmpty()) {
                logger.debug("Refreshing models due to new thing handler factory {}",
                        handlerFactory.getClass().getSimpleName());
                thingsMap.keySet().stream().filter(name -> !isIsolatedModel(name)).forEach(modelName -> {
                    List<Thing> things = thingsMap.getOrDefault(modelName, List.of()).stream()
                            .filter(th -> handlerFactory.supportsThingType(th.getThingTypeUID())).toList();
                    if (!things.isEmpty()) {
                        logger.info("Refreshing YAML model {} ({} things with {})", modelName, things.size(),
                                handlerFactory.getClass().getSimpleName());
                        things.forEach(thing -> {
                            if (!retryCreateThing(handlerFactory, thing.getThingTypeUID(), thing.getConfiguration(),
                                    thing.getUID(), thing.getBridgeUID())) {
                                // Possible cause: Asynchronous loading of the XML files
                                // Add the data to the queue in order to retry it later
                                logger.debug(
                                        "ThingHandlerFactory \'{}\' claimed it can handle \'{}\' type but actually did not. Queued for later refresh.",
                                        handlerFactory.getClass().getSimpleName(), thing.getThingTypeUID());
                                queueRetryThingCreation(handlerFactory, thing.getThingTypeUID(),
                                        thing.getConfiguration(), thing.getUID(), thing.getBridgeUID());
                            }
                        });
                    } else {
                        logger.debug("No refresh needed from YAML model {}", modelName);
                    }
                });
            } else {
                logger.debug("No things yet loaded; no need to trigger a refresh due to new thing handler factory");
            }
        }
    }

    private boolean retryCreateThing(ThingHandlerFactory handlerFactory, ThingTypeUID thingTypeUID,
            Configuration configuration, ThingUID thingUID, @Nullable ThingUID bridgeUID) {
        logger.trace("Retry creating thing {}", thingUID);
        Thing newThing = handlerFactory.createThing(thingTypeUID, new Configuration(configuration), thingUID,
                bridgeUID);
        if (newThing != null) {
            logger.debug("Successfully loaded thing \'{}\' during retry", thingUID);
            Thing oldThing = null;
            for (Map.Entry<String, Collection<Thing>> entry : thingsMap.entrySet()) {
                Collection<Thing> modelThings = entry.getValue();
                oldThing = modelThings.stream().filter(t -> t.getUID().equals(newThing.getUID())).findFirst()
                        .orElse(null);
                if (oldThing != null) {
                    mergeThing(newThing, oldThing, false);
                    modelThings.remove(oldThing);
                    modelThings.add(newThing);
                    logger.debug("Refreshing thing \'{}\' after successful retry", newThing.getUID());
                    if (!ThingHelper.equals(oldThing, newThing) && !isIsolatedModel(entry.getKey())) {
                        notifyListenersAboutUpdatedElement(oldThing, newThing);
                    }
                    break;
                }
            }
            if (oldThing == null) {
                logger.debug("Refreshing thing \'{}\' after retry failed because thing is not found",
                        newThing.getUID());
            }
        }
        return newThing != null;
    }

    private boolean isThingHandlerFactoryReady(ThingHandlerFactory thingHandlerFactory) {
        String bundleName = getBundleName(thingHandlerFactory);
        return bundleName != null && loadedXmlThingTypes.contains(bundleName);
    }

    private @Nullable String getBundleName(ThingHandlerFactory thingHandlerFactory) {
        Bundle bundle = bundleResolver.resolveBundle(thingHandlerFactory.getClass());
        return bundle == null ? null : bundle.getSymbolicName();
    }

    private @Nullable ThingUID buildThingUID(YamlThingDTO thingDto) {
        try {
            return new ThingUID(thingDto.uid);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private @Nullable Thing mapThing(YamlThingDTO thingDto, boolean isolatedModel) {
        try {
            ThingUID thingUID = new ThingUID(thingDto.uid);
            String[] segments = thingUID.getAsString().split(AbstractUID.SEPARATOR);
            ThingTypeUID thingTypeUID = new ThingTypeUID(thingUID.getBindingId(), segments[1]);

            ThingType thingType = thingTypeRegistry.getThingType(thingTypeUID, localeProvider.getLocale());
            ThingUID bridgeUID = thingDto.bridge != null ? new ThingUID(thingDto.bridge) : null;
            Configuration configuration = new Configuration(thingDto.config);

            ThingBuilder thingBuilder = thingDto.isBridge() ? BridgeBuilder.create(thingTypeUID, thingUID)
                    : ThingBuilder.create(thingTypeUID, thingUID);
            thingBuilder.withLabel(
                    thingDto.label != null ? thingDto.label : (thingType != null ? thingType.getLabel() : null));
            thingBuilder.withLocation(thingDto.location);
            thingBuilder.withBridge(bridgeUID);
            thingBuilder.withConfiguration(configuration);

            List<Channel> channels = createChannels(!isolatedModel, thingTypeUID, thingUID,
                    thingDto.channels != null ? thingDto.channels : Map.of(),
                    thingType != null ? thingType.getChannelDefinitions() : List.of());
            thingBuilder.withChannels(channels);

            Thing thing = thingBuilder.build();

            Thing thingFromHandler = null;
            ThingHandlerFactory handlerFactory = thingHandlerFactories.stream()
                    .filter(thf -> isThingHandlerFactoryReady(thf) && thf.supportsThingType(thingTypeUID)).findFirst()
                    .orElse(null);
            if (handlerFactory != null) {
                thingFromHandler = handlerFactory.createThing(thingTypeUID, new Configuration(thingDto.config),
                        thingUID, bridgeUID);
                if (thingFromHandler != null) {
                    mergeThing(thingFromHandler, thing, isolatedModel);
                    logger.debug("Successfully loaded thing \'{}\'", thingUID);
                } else if (!isolatedModel) {
                    // Possible cause: Asynchronous loading of the XML files
                    // Add the data to the queue in order to retry it later
                    logger.debug(
                            "ThingHandlerFactory \'{}\' claimed it can handle \'{}\' type but actually did not. Queued for later refresh.",
                            handlerFactory.getClass().getSimpleName(), thingTypeUID);
                    queueRetryThingCreation(handlerFactory, thingTypeUID, configuration, thingUID, bridgeUID);
                }
            }

            return thingFromHandler != null ? thingFromHandler : thing;
        } catch (IllegalArgumentException e) {
            logger.warn("Error creating thing '{}', thing will be ignored: {}", thingDto.uid, e.getMessage());
            return null;
        }
    }

    private List<Channel> createChannels(boolean applyDefaultConfig, ThingTypeUID thingTypeUID, ThingUID thingUID,
            Map<String, YamlChannelDTO> channelsDto, List<ChannelDefinition> channelDefinitions) {
        Set<String> addedChannelIds = new HashSet<>();
        List<Channel> channels = new ArrayList<>();
        channelsDto.forEach((channelId, channelDto) -> {
            ChannelTypeUID channelTypeUID = channelDto.type == null ? null
                    : new ChannelTypeUID(thingUID.getBindingId(), channelDto.type);
            Channel channel = createChannel(applyDefaultConfig, thingUID, channelId, channelTypeUID,
                    channelDto.getKind(), channelDto.getItemType(), channelDto.label, channelDto.description, null,
                    new Configuration(channelDto.config), true);
            channels.add(channel);
            addedChannelIds.add(channelId);
        });

        channelDefinitions.forEach(channelDef -> {
            String id = channelDef.getId();
            if (addedChannelIds.add(id)) {
                ChannelType channelType = channelTypeRegistry.getChannelType(channelDef.getChannelTypeUID(),
                        localeProvider.getLocale());
                if (channelType != null) {
                    Channel channel = ChannelBuilder.create(new ChannelUID(thingUID, id), channelType.getItemType())
                            .withType(channelDef.getChannelTypeUID())
                            .withAutoUpdatePolicy(channelType.getAutoUpdatePolicy()).build();
                    channels.add(channel);
                } else {
                    logger.warn(
                            "Could not create channel '{}' for thing '{}', because channel type '{}' could not be found.",
                            id, thingUID, channelDef.getChannelTypeUID());
                }
            }
        });
        return channels;
    }

    private Channel createChannel(boolean applyDefaultConfig, ThingUID thingUID, String channelId,
            @Nullable ChannelTypeUID channelTypeUID, ChannelKind channelKind, @Nullable String channelItemType,
            @Nullable String channelLabel, @Nullable String channelDescription,
            @Nullable AutoUpdatePolicy channelAutoUpdatePolicy, Configuration channelConfiguration,
            boolean ignoreMissingChannelType) {
        ChannelKind kind = channelKind;
        String itemType = channelItemType;
        String label = channelLabel;
        String description = channelDescription;
        AutoUpdatePolicy autoUpdatePolicy = channelAutoUpdatePolicy;
        Configuration configuration = new Configuration(channelConfiguration);
        if (channelTypeUID != null) {
            ChannelType channelType = channelTypeRegistry.getChannelType(channelTypeUID, localeProvider.getLocale());
            if (channelType != null) {
                kind = channelType.getKind();
                itemType = channelType.getItemType();
                if (label == null) {
                    label = channelType.getLabel();
                }
                if (description == null) {
                    description = channelType.getDescription();
                }
                autoUpdatePolicy = channelType.getAutoUpdatePolicy();
                URI descUriO = channelType.getConfigDescriptionURI();
                if (applyDefaultConfig && descUriO != null) {
                    ConfigUtil.applyDefaultConfiguration(configuration,
                            configDescriptionRegistry.getConfigDescription(descUriO));
                }
            } else if (!ignoreMissingChannelType) {
                logger.warn("Channel type {} could not be found for thing '{}'.", channelTypeUID, thingUID);
            }
        }

        ChannelBuilder builder = ChannelBuilder.create(new ChannelUID(thingUID, channelId), itemType).withKind(kind)
                .withConfiguration(configuration).withType(channelTypeUID).withAutoUpdatePolicy(autoUpdatePolicy);
        if (label != null) {
            builder.withLabel(label);
        }
        if (description != null) {
            builder.withDescription(description);
        }
        return builder.build();
    }

    private void mergeThing(Thing target, Thing source, boolean keepSourceConfig) {
        String label = source.getLabel();
        if (label == null) {
            ThingType thingType = thingTypeRegistry.getThingType(target.getThingTypeUID(), localeProvider.getLocale());
            label = thingType != null ? thingType.getLabel() : null;
        }
        target.setLabel(label);
        target.setLocation(source.getLocation());
        target.setBridgeUID(source.getBridgeUID());

        if (keepSourceConfig) {
            target.getConfiguration().setProperties(Map.of());
        }
        Configuration thingConfig = processThingConfiguration(target.getThingTypeUID(), target.getUID(),
                source.getConfiguration());
        thingConfig.keySet().forEach(paramName -> {
            target.getConfiguration().put(paramName, thingConfig.get(paramName));
        });

        List<Channel> channelsToAdd = new ArrayList<>();
        source.getChannels().forEach(channel -> {
            Channel targetChannel = target.getChannels().stream().filter(c -> c.getUID().equals(channel.getUID()))
                    .findFirst().orElse(null);
            if (targetChannel != null) {
                if (keepSourceConfig) {
                    targetChannel.getConfiguration().setProperties(Map.of());
                }
                Configuration channelConfig = processChannelConfiguration(targetChannel.getChannelTypeUID(),
                        targetChannel.getUID(), channel.getConfiguration());
                channelConfig.keySet().forEach(paramName -> {
                    targetChannel.getConfiguration().put(paramName, channelConfig.get(paramName));
                });
            } else {
                Channel newChannel = channel;
                if (channel.getChannelTypeUID() != null) {
                    // We create again the user defined channel because channel type was potentially not yet
                    // in the registry when the channel was initially created
                    Configuration channelConfig = processChannelConfiguration(channel.getChannelTypeUID(),
                            channel.getUID(), channel.getConfiguration());
                    newChannel = createChannel(!keepSourceConfig, target.getUID(), channel.getUID().getId(),
                            channel.getChannelTypeUID(), channel.getKind(), channel.getAcceptedItemType(),
                            channel.getLabel(), channel.getDescription(), channel.getAutoUpdatePolicy(), channelConfig,
                            false);
                }
                channelsToAdd.add(newChannel);
            }
        });

        // add the channels only defined in source list to the target list
        ThingHelper.addChannelsToThing(target, channelsToAdd);
    }

    private void queueRetryThingCreation(ThingHandlerFactory handlerFactory, ThingTypeUID thingTypeUID,
            Configuration configuration, ThingUID thingUID, @Nullable ThingUID bridgeUID) {
        queue.add(new QueueContent(handlerFactory, thingTypeUID, configuration, thingUID, bridgeUID));
        Thread thread = lazyRetryThread;
        if (thread == null || !thread.isAlive()) {
            thread = new Thread(lazyRetryRunnable);
            lazyRetryThread = thread;
            thread.start();
        }
    }

    private Configuration processThingConfiguration(ThingTypeUID thingTypeUID, ThingUID thingUID,
            Configuration configuration) {
        Set<String> thingStringParams = !configuration.keySet().isEmpty()
                ? getThingConfigStringParameters(thingTypeUID, thingUID)
                : Set.of();
        return processConfiguration(thingUID, configuration, thingStringParams);
    }

    private Configuration processChannelConfiguration(@Nullable ChannelTypeUID channelTypeUID, ChannelUID channelUID,
            Configuration configuration) {
        Set<String> channelStringParams = !configuration.keySet().isEmpty()
                ? getChannelConfigStringParameters(channelTypeUID, channelUID)
                : Set.of();
        return processConfiguration(channelUID, configuration, channelStringParams);
    }

    private Configuration processConfiguration(UID uid, Configuration configuration, Set<String> stringParameters) {
        Map<String, Object> params = new HashMap<>();

        configuration.keySet().forEach(name -> {
            Object valueIn = configuration.get(name);
            Object valueOut = valueIn;
            // For configuration parameter of type text only
            if (stringParameters.contains(name)) {
                if (valueIn != null && !(valueIn instanceof String)) {
                    logger.info(
                            "\"{}\": the value of the configuration TEXT parameter \"{}\" is not interpreted as a string and will be automatically converted. Enclose your value in double quotes to prevent conversion.",
                            uid, name);
                }
                // if the value in YAML is an unquoted number, the value resulting of the parsing can then be
                // of type BigDecimal or BigInteger.
                // If the value is of type BigDecimal, we convert it into a String. If there is no decimal,
                // we convert it to an integer and return a String from that integer.
                // - Value 1 in YAML is converted into String "1"
                // - Value 1.0 in YAML is converted into String "1"
                // - Value 1.5 in YAML is converted into String "1.5"
                // If the value is not of type BigDecimal, it is kept unchanged. Conversion to a String will
                // be applied at a next step during configuration normalization.
                if (valueIn instanceof BigDecimal bigDecimalValue) {
                    try {
                        valueOut = bigDecimalValue.stripTrailingZeros().scale() <= 0
                                ? String.valueOf(bigDecimalValue.toBigIntegerExact().longValue())
                                : bigDecimalValue.toString();
                        logger.trace("config param {}: {} ({}) converted into {} ({})", name, valueIn,
                                valueIn.getClass().getSimpleName(), valueOut, valueOut.getClass().getSimpleName());
                    } catch (ArithmeticException e) {
                        // Ignore error and return the original value
                    }
                }
            }
            params.put(name, valueOut);
        });

        return new Configuration(params);
    }

    private Set<String> getThingConfigStringParameters(ThingTypeUID thingTypeUID, ThingUID thingUID) {
        Set<String> params = new HashSet<>();

        ThingType thingType = thingTypeRegistry.getThingType(thingTypeUID);
        if (thingType == null) {
            return params;
        }

        URI descURI = thingType.getConfigDescriptionURI();
        if (descURI != null) {
            params.addAll(getStringParameters(descURI));
        }
        try {
            params.addAll(getStringParameters(new URI("thing:" + thingUID)));
        } catch (URISyntaxException e) {
            // Ignore exception, this will never happen with a valid thing UID
        }

        return params;
    }

    private Set<String> getChannelConfigStringParameters(@Nullable ChannelTypeUID channelTypeUID,
            ChannelUID channelUID) {
        Set<String> params = new HashSet<>();

        ChannelType channelType = channelTypeUID == null ? null : channelTypeRegistry.getChannelType(channelTypeUID);
        if (channelType == null) {
            return params;
        }

        URI descURI = channelType.getConfigDescriptionURI();
        if (descURI != null) {
            params.addAll(getStringParameters(descURI));
        }
        try {
            params.addAll(getStringParameters(new URI("channel:" + channelUID)));
        } catch (URISyntaxException e) {
            // Ignore exception, this will never happen with a valid channel UID
        }

        return params;
    }

    private Set<String> getStringParameters(URI uri) {
        Set<String> params = new HashSet<>();
        ConfigDescription configDescription = configDescriptionRegistry.getConfigDescription(uri);
        if (configDescription != null) {
            for (Entry<String, ConfigDescriptionParameter> param : configDescription.toParametersMap().entrySet()) {
                if (param.getValue().getType() == ConfigDescriptionParameter.Type.TEXT) {
                    params.add(param.getKey());
                }
            }
        }
        return params;
    }
}
