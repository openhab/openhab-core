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
package org.openhab.core.model.yaml.internal.things;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.AbstractUID;
import org.openhab.core.common.registry.AbstractProvider;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.ConfigUtil;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.model.yaml.YamlElementName;
import org.openhab.core.model.yaml.YamlModelListener;
import org.openhab.core.model.yaml.YamlModelRepository;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyService;
import org.openhab.core.service.StartLevelService;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingProvider;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
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

import com.fasterxml.jackson.databind.ObjectMapper;

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

    private final YamlModelRepository modelRepository;
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
                    logger.trace("Retry creating thing {}", qc.thingUID);
                    Thing newThing = qc.thingHandlerFactory.createThing(qc.thingTypeUID, qc.configuration, qc.thingUID,
                            qc.bridgeUID);
                    if (newThing != null) {
                        logger.debug("Successfully loaded thing \'{}\' during retry", qc.thingUID);
                        Thing oldThing = null;
                        for (Map.Entry<String, Collection<Thing>> entry : thingsMap.entrySet()) {
                            oldThing = entry.getValue().stream().filter(t -> t.getUID().equals(newThing.getUID()))
                                    .findFirst().orElse(null);
                            if (oldThing != null) {
                                mergeThing(newThing, oldThing);
                                Collection<Thing> thingsForModel = Objects
                                        .requireNonNull(thingsMap.get(entry.getKey()));
                                thingsForModel.remove(oldThing);
                                thingsForModel.add(newThing);
                                logger.debug("Refreshing thing \'{}\' after successful retry", newThing.getUID());
                                if (!ThingHelper.equals(oldThing, newThing)) {
                                    notifyListenersAboutUpdatedElement(oldThing, newThing);
                                }
                                break;
                            }
                        }
                        if (oldThing == null) {
                            logger.debug("Refreshing thing \'{}\' after retry failed because thing is not found",
                                    newThing.getUID());
                        }
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
    public YamlThingProvider(final @Reference YamlModelRepository modelRepository,
            final @Reference BundleResolver bundleResolver, final @Reference ThingTypeRegistry thingTypeRegistry,
            final @Reference ChannelTypeRegistry channelTypeRegistry,
            final @Reference ConfigDescriptionRegistry configDescriptionRegistry,
            final @Reference LocaleProvider localeProvider) {
        this.modelRepository = modelRepository;
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
        return thingsMap.values().stream().flatMap(list -> list.stream()).toList();
    }

    @Override
    public Class<YamlThingDTO> getElementClass() {
        return YamlThingDTO.class;
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
    public void addedModel(String modelName, ObjectMapper yamlMapper, Collection<YamlThingDTO> elements) {
        List<Thing> added = elements.stream().map(this::mapThing).filter(Objects::nonNull).toList();
        Collection<Thing> modelThings = Objects
                .requireNonNull(thingsMap.computeIfAbsent(modelName, k -> new ArrayList<>()));
        modelThings.addAll(added);
        added.forEach(t -> {
            logger.debug("model {} added thing {}", modelName, t.getUID());
            notifyListenersAboutAddedElement(t);
        });
    }

    @Override
    public void updatedModel(String modelName, ObjectMapper yamlMapper, Collection<YamlThingDTO> elements) {
        List<Thing> updated = elements.stream().map(this::mapThing).filter(Objects::nonNull).toList();
        Collection<Thing> modelThings = Objects
                .requireNonNull(thingsMap.computeIfAbsent(modelName, k -> new ArrayList<>()));
        updated.forEach(t -> {
            modelThings.stream().filter(th -> th.getUID().equals(t.getUID())).findFirst().ifPresentOrElse(oldThing -> {
                modelThings.remove(oldThing);
                modelThings.add(t);
                logger.debug("model {} updated thing {}", modelName, t.getUID());
                notifyListenersAboutUpdatedElement(oldThing, t);
            }, () -> {
                modelThings.add(t);
                logger.debug("model {} added thing {}", modelName, t.getUID());
                notifyListenersAboutAddedElement(t);
            });
        });
    }

    @Override
    public void removedModel(String modelName, ObjectMapper yamlMapper, Collection<YamlThingDTO> elements) {
        List<Thing> removed = elements.stream().map(this::mapThing).filter(Objects::nonNull).toList();
        Collection<Thing> modelThings = thingsMap.getOrDefault(modelName, List.of());
        removed.forEach(t -> {
            modelThings.stream().filter(th -> th.getUID().equals(t.getUID())).findFirst().ifPresentOrElse(oldThing -> {
                modelThings.remove(oldThing);
                logger.debug("model {} removed thing {}", modelName, t.getUID());
                notifyListenersAboutRemovedElement(oldThing);
            }, () -> logger.debug("model {} thing {} not found", modelName, t.getUID()));
        });
        if (modelThings.isEmpty()) {
            thingsMap.remove(modelName);
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addThingHandlerFactory(final ThingHandlerFactory thingHandlerFactory) {
        thingHandlerFactories.add(thingHandlerFactory);
        thingHandlerFactoryAdded(thingHandlerFactory);
    }

    protected void removeThingHandlerFactory(final ThingHandlerFactory thingHandlerFactory) {
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

    private void thingHandlerFactoryAdded(ThingHandlerFactory thingHandlerFactory) {
        String bundleName = getBundleName(thingHandlerFactory);
        if (bundleName != null && loadedXmlThingTypes.contains(bundleName)) {
            logger.debug("Refreshing models due to new thing handler factory {}",
                    thingHandlerFactory.getClass().getSimpleName());
            thingsMap.keySet().forEach(modelName -> {
                modelRepository.refreshModelElements(modelName,
                        getElementClass().getAnnotation(YamlElementName.class).value());
            });
        }
    }

    private @Nullable String getBundleName(ThingHandlerFactory thingHandlerFactory) {
        Bundle bundle = bundleResolver.resolveBundle(thingHandlerFactory.getClass());
        return bundle == null ? null : bundle.getSymbolicName();
    }

    private @Nullable Thing mapThing(YamlThingDTO thingDto) {
        ThingUID thingUID = new ThingUID(thingDto.uid);
        String[] segments = thingUID.getAsString().split(AbstractUID.SEPARATOR);
        ThingTypeUID thingTypeUID = new ThingTypeUID(thingUID.getBindingId(), segments[1]);

        ThingHandlerFactory handlerFactory = thingHandlerFactories.stream()
                .filter(thf -> thf.supportsThingType(thingTypeUID)).findFirst().orElse(null);
        if (handlerFactory == null) {
            if (modelLoaded) {
                logger.info("No ThingHandlerFactory found for thing {} (thing-type is {}). Deferring initialization.",
                        thingUID, thingTypeUID);
            }
            return null;
        }
        String bundleName = getBundleName(handlerFactory);
        if (bundleName == null || !loadedXmlThingTypes.contains(bundleName)) {
            return null;
        }

        ThingType thingType = thingTypeRegistry.getThingType(thingTypeUID, localeProvider.getLocale());
        ThingUID bridgeUID = thingDto.bridge != null ? new ThingUID(thingDto.bridge) : null;
        Configuration configuration = new Configuration(thingDto.config);

        ThingBuilder thingBuilder = thingDto.isBridge() ? BridgeBuilder.create(thingTypeUID, thingUID)
                : ThingBuilder.create(thingTypeUID, thingUID);
        thingBuilder
                .withLabel(thingDto.label != null ? thingDto.label : (thingType != null ? thingType.getLabel() : null));
        thingBuilder.withLocation(thingDto.location);
        thingBuilder.withBridge(bridgeUID);
        thingBuilder.withConfiguration(configuration);

        List<Channel> channels = createChannels(thingTypeUID, thingUID,
                thingDto.channels != null ? thingDto.channels : Map.of(),
                thingType != null ? thingType.getChannelDefinitions() : List.of());
        thingBuilder.withChannels(channels);

        Thing thing = thingBuilder.build();

        Thing thingFromHandler = handlerFactory.createThing(thingTypeUID, configuration, thingUID, bridgeUID);
        if (thingFromHandler != null) {
            mergeThing(thingFromHandler, thing);
            logger.debug("Successfully loaded thing \'{}\'", thingUID);
        } else {
            // Possible cause: Asynchronous loading of the XML files
            // Add the data to the queue in order to retry it later
            logger.debug(
                    "ThingHandlerFactory \'{}\' claimed it can handle \'{}\' type but actually did not. Queued for later refresh.",
                    handlerFactory.getClass().getSimpleName(), thingTypeUID);
            queue.add(new QueueContent(handlerFactory, thingTypeUID, configuration, thingUID, bridgeUID));
            Thread thread = lazyRetryThread;
            if (thread == null || !thread.isAlive()) {
                thread = new Thread(lazyRetryRunnable);
                lazyRetryThread = thread;
                thread.start();
            }
        }

        return thingFromHandler != null ? thingFromHandler : thing;
    }

    private List<Channel> createChannels(ThingTypeUID thingTypeUID, ThingUID thingUID,
            Map<String, YamlChannelDTO> channelsDto, List<ChannelDefinition> channelDefinitions) {
        Set<String> addedChannelIds = new HashSet<>();
        List<Channel> channels = new ArrayList<>();
        channelsDto.forEach((channelId, channelDto) -> {
            ChannelTypeUID channelTypeUID = null;
            ChannelKind kind = channelDto.getKind();
            String itemType = channelDto.getItemType();
            String label = channelDto.label;
            AutoUpdatePolicy autoUpdatePolicy = null;
            Configuration configuration = new Configuration(channelDto.config);
            if (channelDto.type != null) {
                channelTypeUID = new ChannelTypeUID(thingUID.getBindingId(), channelDto.type);
                ChannelType channelType = channelTypeRegistry.getChannelType(channelTypeUID,
                        localeProvider.getLocale());
                if (channelType != null) {
                    kind = channelType.getKind();
                    itemType = channelType.getItemType();
                    if (label == null) {
                        label = channelType.getLabel();
                    }
                    autoUpdatePolicy = channelType.getAutoUpdatePolicy();
                    URI descUriO = channelType.getConfigDescriptionURI();
                    if (descUriO != null) {
                        ConfigUtil.applyDefaultConfiguration(configuration,
                                configDescriptionRegistry.getConfigDescription(descUriO));
                    }
                } else {
                    logger.warn("Channel type {} could not be found.", channelTypeUID);
                }
            }

            Channel channel = ChannelBuilder.create(new ChannelUID(thingUID, channelId), itemType).withKind(kind)
                    .withConfiguration(configuration).withType(channelTypeUID).withLabel(label)
                    .withAutoUpdatePolicy(autoUpdatePolicy).build();
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
