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
package org.openhab.core.thing.internal;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.cache.ExpiringCacheMap;
import org.openhab.core.common.AbstractUID;
import org.openhab.core.common.SafeCaller;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemFactory;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.ItemStateConverter;
import org.openhab.core.items.ItemUtil;
import org.openhab.core.items.events.AbstractItemRegistryEvent;
import org.openhab.core.items.events.GroupStateUpdatedEvent;
import org.openhab.core.items.events.ItemCommandEvent;
import org.openhab.core.items.events.ItemStateUpdatedEvent;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.events.AbstractThingRegistryEvent;
import org.openhab.core.thing.events.ChannelTriggeredEvent;
import org.openhab.core.thing.events.ThingEventFactory;
import org.openhab.core.thing.internal.link.ItemChannelLinkConfigDescriptionProvider;
import org.openhab.core.thing.internal.profiles.ProfileCallbackImpl;
import org.openhab.core.thing.internal.profiles.SystemProfileFactory;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.profiles.Profile;
import org.openhab.core.thing.profiles.ProfileAdvisor;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileFactory;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.StateProfile;
import org.openhab.core.thing.profiles.TimeSeriesProfile;
import org.openhab.core.thing.profiles.TriggerProfile;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.TimeSeries;
import org.openhab.core.types.Type;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages the state related communication between bindings and the framework.
 *
 * It mainly mediates commands, state updates and triggers from ThingHandlers to the framework and vice versa.
 *
 * @author Simon Kaufmann - Initial contribution factored out of ThingManger
 * @author Jan N. Klug - Added time series support
 */
@NonNullByDefault
@Component(service = { EventSubscriber.class, CommunicationManager.class }, immediate = true)
public class CommunicationManager implements EventSubscriber, RegistryChangeListener<ItemChannelLink> {

    private static final Profile NO_OP_PROFILE = new Profile() {
        private final ProfileTypeUID noOpProfileUID = new ProfileTypeUID(ProfileTypeUID.SYSTEM_SCOPE, "noop");

        @Override
        public ProfileTypeUID getProfileTypeUID() {
            return noOpProfileUID;
        }

        @Override
        public void onStateUpdateFromItem(State state) {
            // no-op
        }
    };

    // how long to cache profile safe call instances
    private static final Duration CACHE_EXPIRATION = Duration.ofMinutes(30);

    // the timeout to use for any item event processing
    public static final long THINGHANDLER_EVENT_TIMEOUT = TimeUnit.SECONDS.toMillis(30);

    private static final Set<String> SUBSCRIBED_EVENT_TYPES = Set.of(ItemStateUpdatedEvent.TYPE, ItemCommandEvent.TYPE,
            GroupStateUpdatedEvent.TYPE, ChannelTriggeredEvent.TYPE);

    private final Logger logger = LoggerFactory.getLogger(CommunicationManager.class);

    private final AutoUpdateManager autoUpdateManager;
    private final SystemProfileFactory defaultProfileFactory;
    private final ItemChannelLinkRegistry itemChannelLinkRegistry;
    private final ItemRegistry itemRegistry;
    private final ItemStateConverter itemStateConverter;
    private final EventPublisher eventPublisher;
    private final SafeCaller safeCaller;
    private final ThingRegistry thingRegistry;

    private final ExpiringCacheMap<Integer, Profile> profileSafeCallCache = new ExpiringCacheMap<>(CACHE_EXPIRATION);

    @Activate
    public CommunicationManager(final @Reference AutoUpdateManager autoUpdateManager,
            final @Reference SystemProfileFactory defaultProfileFactory,
            final @Reference ItemChannelLinkRegistry itemChannelLinkRegistry,
            final @Reference ItemRegistry itemRegistry, //
            final @Reference ItemStateConverter itemStateConverter, //
            final @Reference EventPublisher eventPublisher, //
            final @Reference SafeCaller safeCaller, //
            final @Reference ThingRegistry thingRegistry) {
        this.autoUpdateManager = autoUpdateManager;
        this.defaultProfileFactory = defaultProfileFactory;
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
        this.itemRegistry = itemRegistry;
        this.itemStateConverter = itemStateConverter;
        this.eventPublisher = eventPublisher;
        this.safeCaller = safeCaller;
        this.thingRegistry = thingRegistry;

        itemChannelLinkRegistry.addRegistryChangeListener(this);
    }

    @Deactivate
    public void deactivate() {
        itemChannelLinkRegistry.removeRegistryChangeListener(this);
    }

    private final Set<ItemFactory> itemFactories = new CopyOnWriteArraySet<>();

    // link UID -> profile
    private final Map<String, Profile> profiles = new ConcurrentHashMap<>();

    // factory instance -> link UIDs which the factory has created profiles for
    private final Map<ProfileFactory, Set<String>> profileFactories = new ConcurrentHashMap<>();

    private final Set<ProfileAdvisor> profileAdvisors = new CopyOnWriteArraySet<>();

    private final Map<String, List<Class<? extends Command>>> acceptedCommandTypeMap = new ConcurrentHashMap<>();
    private final Map<String, List<Class<? extends State>>> acceptedStateTypeMap = new ConcurrentHashMap<>();

    @Override
    public Set<String> getSubscribedEventTypes() {
        return SUBSCRIBED_EVENT_TYPES;
    }

    @Override
    public void receive(Event event) {
        if (event instanceof ItemStateUpdatedEvent updatedEvent) {
            receiveUpdate(updatedEvent);
        } else if (event instanceof ItemCommandEvent commandEvent) {
            receiveCommand(commandEvent);
        } else if (event instanceof ChannelTriggeredEvent triggeredEvent) {
            receiveTrigger(triggeredEvent);
        } else if (event instanceof AbstractItemRegistryEvent registryEvent) {
            String itemName = registryEvent.getItem().name;
            profiles.entrySet().removeIf(entry -> {
                ItemChannelLink link = itemChannelLinkRegistry.get(entry.getKey());
                return link != null && itemName.equals(link.getItemName());
            });
        } else if (event instanceof AbstractThingRegistryEvent registryEvent) {
            ThingUID thingUid = new ThingUID(registryEvent.getThing().UID);
            profiles.entrySet().removeIf(entry -> {
                ItemChannelLink link = itemChannelLinkRegistry.get(entry.getKey());
                return link != null && thingUid.equals(link.getLinkedUID().getThingUID());
            });
        }
    }

    private Profile getProfile(ItemChannelLink link, Item item, @Nullable Thing thing) {
        synchronized (profiles) {
            Profile profile = profiles.get(link.getUID());
            if (profile != null) {
                logger.trace("Using profile '{}' from cache for link '{}'", profile.getProfileTypeUID(), link);
                return profile;
            }
            ProfileTypeUID profileTypeUID = determineProfileTypeUID(link, item, thing);
            if (profileTypeUID != null) {
                profile = getProfileFromFactories(profileTypeUID, link, createCallback(link));
                if (profile != null) {
                    profiles.put(link.getUID(), profile);
                    return profile;
                }
            }
            logger.trace("No Profile found for link '{}', using NoOpProfile", link);
            return NO_OP_PROFILE;
        }
    }

    private ProfileCallback createCallback(ItemChannelLink link) {
        return new ProfileCallbackImpl(eventPublisher, safeCaller, itemStateConverter, link, thingRegistry::get,
                this::getItem, this::toAcceptedCommand);
    }

    private @Nullable ProfileTypeUID determineProfileTypeUID(ItemChannelLink link, Item item, @Nullable Thing thing) {
        ProfileTypeUID profileTypeUID = getConfiguredProfileTypeUID(link);
        Channel channel = null;
        if (profileTypeUID == null) {
            if (thing == null) {
                return null;
            }

            channel = thing.getChannel(link.getLinkedUID().getId());
            if (channel == null) {
                return null;
            }

            // ask advisors
            profileTypeUID = getAdvice(link, item, channel);

            if (profileTypeUID == null) {
                // ask default advisor
                logger.trace("No profile advisor found for link '{}', falling back to the defaults", link);
                profileTypeUID = defaultProfileFactory.getSuggestedProfileTypeUID(channel, item.getType());
            }
        }
        return profileTypeUID;
    }

    private @Nullable ProfileTypeUID getAdvice(ItemChannelLink link, Item item, Channel channel) {
        ProfileTypeUID ret;
        for (ProfileAdvisor advisor : profileAdvisors) {
            ret = advisor.getSuggestedProfileTypeUID(channel, item.getType());
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    private @Nullable ProfileTypeUID getConfiguredProfileTypeUID(ItemChannelLink link) {
        String profileName = (String) link.getConfiguration()
                .get(ItemChannelLinkConfigDescriptionProvider.PARAM_PROFILE);
        if (profileName != null && !profileName.trim().isEmpty()) {
            if (!profileName.contains(AbstractUID.SEPARATOR)) {
                profileName = ProfileTypeUID.SYSTEM_SCOPE + AbstractUID.SEPARATOR + profileName;
            }
            return new ProfileTypeUID(profileName);
        }
        return null;
    }

    private @Nullable Profile getProfileFromFactories(ProfileTypeUID profileTypeUID, ItemChannelLink link,
            ProfileCallback callback) {
        ProfileContext context = null;

        Item item = getItem(link.getItemName());
        ThingUID thingUID = link.getLinkedUID().getThingUID();
        Thing thing = thingRegistry.get(thingUID);
        if (item != null && thing != null) {
            Channel channel = thing.getChannel(link.getLinkedUID());
            if (channel != null) {
                String acceptedItemType = Objects.requireNonNullElse(channel.getAcceptedItemType(), "");
                if (acceptedItemType.startsWith("Number")) {
                    acceptedItemType = "Number";
                }
                context = new ProfileContextImpl(link.getConfiguration(), item.getAcceptedDataTypes(),
                        item.getAcceptedCommandTypes(),
                        acceptedCommandTypeMap.getOrDefault(acceptedItemType, List.of()));
            }
        }

        if (context == null) {
            logger.debug("Could not create full channel context, item or channel missing in registry.");
            return null;
        }

        if (supportsProfileTypeUID(defaultProfileFactory, profileTypeUID)) {
            logger.trace("Using the default ProfileFactory to create profile '{}' for link '{}'", profileTypeUID, link);
            return defaultProfileFactory.createProfile(profileTypeUID, callback, context);
        }
        for (Entry<ProfileFactory, Set<String>> entry : profileFactories.entrySet()) {
            ProfileFactory factory = entry.getKey();
            if (supportsProfileTypeUID(factory, profileTypeUID)) {
                logger.trace("Using ProfileFactory '{}' to create profile '{}' for link '{}'", factory, profileTypeUID,
                        link);
                Profile profile = factory.createProfile(profileTypeUID, callback, context);
                if (profile == null) {
                    logger.error("ProfileFactory '{}' returned 'null' although it claimed to support profile '{}'",
                            factory, profileTypeUID);
                } else {
                    entry.getValue().add(link.getUID());
                    return profile;
                }
            }
        }
        logger.warn("No ProfileFactory found which supports profile '{}' for link '{}'", profileTypeUID, link);
        return null;
    }

    private boolean supportsProfileTypeUID(ProfileFactory profileFactory, ProfileTypeUID profileTypeUID) {
        return profileFactory.getSupportedProfileTypeUIDs().contains(profileTypeUID);
    }

    private void receiveCommand(ItemCommandEvent commandEvent) {
        final String itemName = commandEvent.getItemName();
        final Command command = commandEvent.getItemCommand();
        final Item item = getItem(itemName);

        if (item != null) {
            autoUpdateManager.receiveCommand(commandEvent, item);
        }

        handleEvent(itemName, command, commandEvent.getSource(), acceptedCommandTypeMap::get,
                this::applyProfileForCommand);
    }

    private void receiveUpdate(ItemStateUpdatedEvent updateEvent) {
        final String itemName = updateEvent.getItemName();
        final State newState = updateEvent.getItemState();
        handleEvent(itemName, newState, updateEvent.getSource(), acceptedStateTypeMap::get,
                this::applyProfileForUpdate);
    }

    @FunctionalInterface
    private interface ProfileAction<T extends Type> {
        void applyProfile(Profile profile, Thing thing, T type);
    }

    private void applyProfileForUpdate(Profile profile, Thing thing, State convertedState) {
        int key = Objects.hash("UPDATE", profile, thing);
        Profile p = profileSafeCallCache.putIfAbsentAndGet(key, () -> safeCaller.create(profile, Profile.class) //
                .withAsync() //
                .withIdentifier(thing) //
                .withTimeout(THINGHANDLER_EVENT_TIMEOUT) //
                .build());
        if (p != null) {
            p.onStateUpdateFromItem(convertedState);
        } else {
            throw new IllegalStateException("ExpiringCache didn't provide a Profile instance!");
        }
    }

    private void applyProfileForCommand(Profile profile, Thing thing, Command convertedCommand) {
        if (profile instanceof StateProfile stateProfile) {
            int key = Objects.hash("COMMAND", profile, thing);
            Profile p = profileSafeCallCache.putIfAbsentAndGet(key,
                    () -> safeCaller.create(stateProfile, StateProfile.class) //
                            .withAsync() //
                            .withIdentifier(thing) //
                            .withTimeout(THINGHANDLER_EVENT_TIMEOUT) //
                            .build());
            if (p instanceof StateProfile profileP) {
                profileP.onCommandFromItem(convertedCommand);
            } else {
                throw new IllegalStateException("ExpiringCache didn't provide a StateProfile instance!");
            }
        }
    }

    private <T extends Type> void handleEvent(String itemName, T type, @Nullable String source,
            Function<@Nullable String, @Nullable List<Class<? extends T>>> acceptedTypesFunction,
            ProfileAction<T> action) {
        final Item item = getItem(itemName);
        if (item == null) {
            logger.debug("Received an event for item {} which does not exist", itemName);
            return;
        }

        itemChannelLinkRegistry.getLinks(itemName).stream().filter(link -> {
            // make sure the command event is not sent back to its source
            return !link.getLinkedUID().toString().equals(source);
        }).forEach(link -> {
            ChannelUID channelUID = link.getLinkedUID();
            ThingUID thingUID = channelUID.getThingUID();
            Thing thing = thingRegistry.get(thingUID);
            if (thing != null) {
                Channel channel = thing.getChannel(channelUID.getId());
                if (channel != null) {
                    if (thing.getHandler() != null) {
                        // fix QuantityType/DecimalType, leave others as-is
                        @Nullable
                        T uomType = fixUoM(type, channel, item);
                        Profile profile = getProfile(link, item, thing);
                        action.applyProfile(profile, thing, uomType != null ? uomType : type);
                    }
                } else {
                    logger.debug("Received  event '{}' for non-existing channel '{}', not forwarding it to the handler",
                            type, channelUID);
                }
            } else {
                logger.debug("Received  event '{}' for non-existing thing '{}', not forwarding it to the handler", type,
                        channelUID.getThingUID());
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <T extends Type> @Nullable T fixUoM(@Nullable T originalType, Channel channel, Item item) {
        String channelAcceptedItemType = channel.getAcceptedItemType();

        if (channelAcceptedItemType == null) {
            return originalType;
        }

        // handle Number-Channels for backward compatibility
        if (CoreItemFactory.NUMBER.equals(channelAcceptedItemType)
                && originalType instanceof QuantityType<?> quantityType) {
            // strip unit from QuantityType for channels that accept plain number
            return (T) new DecimalType(quantityType.toBigDecimal());
        }

        String itemDimension = ItemUtil.getItemTypeExtension(item.getType());
        String channelDimension = ItemUtil.getItemTypeExtension(channelAcceptedItemType);

        if (originalType instanceof DecimalType decimalType && channelDimension != null
                && channelDimension.equals(itemDimension)) {
            // Add unit from item to DecimalType when dimensions are equal
            Unit<?> unit = Objects.requireNonNull(((NumberItem) item).getUnit());
            return (T) new QuantityType<>(decimalType.toBigDecimal(), unit);
        }
        return null;
    }

    public @Nullable Command toAcceptedCommand(Command originalType, @Nullable Channel channel, @Nullable Item item) {
        if (item == null || channel == null) {
            logger.warn("Trying to convert types for non-existing channel or item, discarding command.");
            return null;
        }
        String channelAcceptedItemType = channel.getAcceptedItemType();

        if (channelAcceptedItemType == null) {
            return originalType;
        }

        Command uomCommand = fixUoM(originalType, channel, item);
        if (uomCommand != null) {
            return uomCommand;
        }

        // handle HSBType/PercentType
        if (CoreItemFactory.DIMMER.equals(channelAcceptedItemType) && originalType instanceof HSBType hsb) {
            return hsb.as(PercentType.class);
        }

        // check for other cases if the type is acceptable
        List<Class<? extends Command>> acceptedTypes = acceptedCommandTypeMap.get(channelAcceptedItemType);
        if (acceptedTypes == null || acceptedTypes.contains(originalType.getClass())) {
            return originalType;
        } else if (acceptedTypes.contains(PercentType.class) && originalType instanceof State state
                && PercentType.class.isAssignableFrom(originalType.getClass())) {
            return state.as(PercentType.class);
        } else if (acceptedTypes.contains(OnOffType.class) && originalType instanceof State state
                && PercentType.class.isAssignableFrom(originalType.getClass())) {
            return state.as(OnOffType.class);
        } else {
            logger.debug("Received not accepted type '{}' for channel '{}'", originalType.getClass().getSimpleName(),
                    channel.getUID());
            return null;
        }
    }

    private @Nullable Item getItem(final String itemName) {
        return itemRegistry.get(itemName);
    }

    private void receiveTrigger(ChannelTriggeredEvent channelTriggeredEvent) {
        final ChannelUID channelUID = channelTriggeredEvent.getChannel();
        final String event = channelTriggeredEvent.getEvent();
        ThingUID thingUID = channelUID.getThingUID();
        final Thing thing = thingRegistry.get(thingUID);

        handleCallFromHandler(channelUID, thing, profile -> {
            if (profile instanceof TriggerProfile triggerProfile) {
                triggerProfile.onTriggerFromHandler(event);
            }
        });
    }

    public void stateUpdated(ChannelUID channelUID, State state) {
        ThingUID thingUID = channelUID.getThingUID();
        final Thing thing = thingRegistry.get(thingUID);

        handleCallFromHandler(channelUID, thing, profile -> {
            if (profile instanceof StateProfile stateProfile) {
                stateProfile.onStateUpdateFromHandler(state);
            }
        });
    }

    public void postCommand(ChannelUID channelUID, Command command) {
        ThingUID thingUID = channelUID.getThingUID();
        final Thing thing = thingRegistry.get(thingUID);

        handleCallFromHandler(channelUID, thing, profile -> {
            if (profile instanceof StateProfile stateProfile) {
                stateProfile.onCommandFromHandler(command);
            }
        });
    }

    public void sendTimeSeries(ChannelUID channelUID, TimeSeries timeSeries) {
        ThingUID thingUID = channelUID.getThingUID();
        Thing thing = thingRegistry.get(thingUID);
        handleCallFromHandler(channelUID, thing, profile -> {
            // TODO: check which profiles need enhancements
            if (profile instanceof TimeSeriesProfile timeSeriesProfile) {
                timeSeriesProfile.onTimeSeriesFromHandler(timeSeries);
            } else {
                logger.warn("Profile '{}' on channel {} does not support time series.", profile.getProfileTypeUID(),
                        channelUID);
            }
        });
    }

    private void handleCallFromHandler(ChannelUID channelUID, @Nullable Thing thing, Consumer<Profile> action) {
        itemChannelLinkRegistry.getLinks(channelUID).forEach(link -> {
            final Item item = getItem(link.getItemName());
            if (item != null) {
                final Profile profile = getProfile(link, item, thing);
                action.accept(profile);
            }
        });
    }

    public void channelTriggered(Thing thing, ChannelUID channelUID, String event) {
        eventPublisher.post(ThingEventFactory.createTriggerEvent(event, channelUID));
    }

    private void cleanup(ItemChannelLink link) {
        synchronized (profiles) {
            profiles.remove(link.getUID());
        }
        profileFactories.values().forEach(list -> list.remove(link.getUID()));
    }

    @Override
    public void added(ItemChannelLink element) {
        // nothing to do
    }

    @Override
    public void removed(ItemChannelLink element) {
        cleanup(element);
    }

    @Override
    public void updated(ItemChannelLink oldElement, ItemChannelLink element) {
        cleanup(oldElement);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addProfileFactory(ProfileFactory profileFactory) {
        profileFactories.put(profileFactory, ConcurrentHashMap.newKeySet());
    }

    @SuppressWarnings("null")
    protected void removeProfileFactory(ProfileFactory profileFactory) {
        Set<String> links = profileFactories.remove(profileFactory);
        synchronized (profiles) {
            links.forEach(profiles::remove);
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addProfileAdvisor(ProfileAdvisor profileAdvisor) {
        profileAdvisors.add(profileAdvisor);
    }

    protected void removeProfileAdvisor(ProfileAdvisor profileAdvisor) {
        profileAdvisors.remove(profileAdvisor);
    }

    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC)
    protected void addItemFactory(ItemFactory itemFactory) {
        itemFactories.add(itemFactory);
        calculateAcceptedTypes();
    }

    protected void removeItemFactory(ItemFactory itemFactory) {
        itemFactories.remove(itemFactory);
        calculateAcceptedTypes();
    }

    private synchronized void calculateAcceptedTypes() {
        acceptedCommandTypeMap.clear();
        acceptedStateTypeMap.clear();
        for (ItemFactory itemFactory : itemFactories) {
            for (String itemTypeName : itemFactory.getSupportedItemTypes()) {
                Item item = itemFactory.createItem(itemTypeName, "tmp");
                if (item != null) {
                    acceptedCommandTypeMap.put(itemTypeName, item.getAcceptedCommandTypes());
                    acceptedStateTypeMap.put(itemTypeName, item.getAcceptedDataTypes());
                } else {
                    logger.error("Item factory {} suggested it can create items of type {} but returned null",
                            itemFactory, itemTypeName);
                }
            }
        }
    }
}
