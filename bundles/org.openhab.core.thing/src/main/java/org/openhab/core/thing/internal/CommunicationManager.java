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
package org.openhab.core.thing.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.measure.Quantity;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.AbstractUID;
import org.openhab.core.common.SafeCaller;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemFactory;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.ItemStateConverter;
import org.openhab.core.items.ItemUtil;
import org.openhab.core.items.events.ItemCommandEvent;
import org.openhab.core.items.events.ItemStateEvent;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingUID;
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
import org.openhab.core.thing.profiles.ProfileFactory;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.StateProfile;
import org.openhab.core.thing.profiles.TriggerProfile;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.Type;
import org.openhab.core.types.util.UnitUtils;
import org.osgi.service.component.annotations.Component;
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
 */
@NonNullByDefault
@Component(service = { EventSubscriber.class, CommunicationManager.class }, immediate = true)
public class CommunicationManager implements EventSubscriber, RegistryChangeListener<ItemChannelLink> {

    // the timeout to use for any item event processing
    public static final long THINGHANDLER_EVENT_TIMEOUT = TimeUnit.SECONDS.toMillis(30);

    private static final Set<String> SUBSCRIBED_EVENT_TYPES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(ItemStateEvent.TYPE, ItemCommandEvent.TYPE, ChannelTriggeredEvent.TYPE)));

    private final Logger logger = LoggerFactory.getLogger(CommunicationManager.class);

    private @NonNullByDefault({}) SystemProfileFactory defaultProfileFactory;
    private @NonNullByDefault({}) ItemChannelLinkRegistry itemChannelLinkRegistry;
    private @NonNullByDefault({}) ThingRegistry thingRegistry;
    private @NonNullByDefault({}) ItemRegistry itemRegistry;
    private @NonNullByDefault({}) EventPublisher eventPublisher;
    private @NonNullByDefault({}) SafeCaller safeCaller;
    private @NonNullByDefault({}) AutoUpdateManager autoUpdateManager;
    private @NonNullByDefault({}) ItemStateConverter itemStateConverter;
    private @NonNullByDefault({}) ChannelTypeRegistry channelTypeRegistry;

    private final Set<ItemFactory> itemFactories = new CopyOnWriteArraySet<>();

    // link UID -> profile
    private final Map<String, Profile> profiles = new ConcurrentHashMap<>();

    // factory instance -> link UIDs which the factory has created profiles for
    private final Map<ProfileFactory, Set<String>> profileFactories = new ConcurrentHashMap<>();

    private final Set<ProfileAdvisor> profileAdvisors = new CopyOnWriteArraySet<>();

    private final Map<String, @Nullable List<Class<? extends Command>>> acceptedCommandTypeMap = new ConcurrentHashMap<>();
    private final Map<String, @Nullable List<Class<? extends State>>> acceptedStateTypeMap = new ConcurrentHashMap<>();

    @Override
    public Set<String> getSubscribedEventTypes() {
        return SUBSCRIBED_EVENT_TYPES;
    }

    @Override
    public @Nullable EventFilter getEventFilter() {
        return null;
    }

    @Override
    public void receive(Event event) {
        if (event instanceof ItemStateEvent) {
            receiveUpdate((ItemStateEvent) event);
        } else if (event instanceof ItemCommandEvent) {
            receiveCommand((ItemCommandEvent) event);
        } else if (event instanceof ChannelTriggeredEvent) {
            receiveTrigger((ChannelTriggeredEvent) event);
        }
    }

    private @Nullable Thing getThing(ThingUID thingUID) {
        return thingRegistry.get(thingUID);
    }

    private Profile getProfile(ItemChannelLink link, Item item, @Nullable Thing thing) {
        synchronized (profiles) {
            Profile profile = profiles.get(link.getUID());
            if (profile != null) {
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
            return new NoOpProfile();
        }
    }

    private ProfileCallback createCallback(ItemChannelLink link) {
        return new ProfileCallbackImpl(eventPublisher, safeCaller, itemStateConverter, link,
                thingUID -> getThing(thingUID), itemName -> getItem(itemName));
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
                logger.trace("No profile advisor found for link {}, falling back to the defaults", link);
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
            profileName = normalizeProfileName(profileName);
            return new ProfileTypeUID(profileName);
        }
        return null;
    }

    private String normalizeProfileName(String profileName) {
        if (!profileName.contains(AbstractUID.SEPARATOR)) {
            return ProfileTypeUID.SYSTEM_SCOPE + AbstractUID.SEPARATOR + profileName;
        }
        return profileName;
    }

    private @Nullable Profile getProfileFromFactories(ProfileTypeUID profileTypeUID, ItemChannelLink link,
            ProfileCallback callback) {
        ProfileContextImpl context = new ProfileContextImpl(link.getConfiguration());
        if (supportsProfileTypeUID(defaultProfileFactory, profileTypeUID)) {
            logger.trace("using the default ProfileFactory to create profile '{}'", profileTypeUID);
            return defaultProfileFactory.createProfile(profileTypeUID, callback, context);
        }
        for (Entry<ProfileFactory, Set<String>> entry : profileFactories.entrySet()) {
            ProfileFactory factory = entry.getKey();
            if (supportsProfileTypeUID(factory, profileTypeUID)) {
                logger.trace("using ProfileFactory '{}' to create profile '{}'", factory, profileTypeUID);
                Profile profile = factory.createProfile(profileTypeUID, callback, context);
                if (profile == null) {
                    logger.error("ProfileFactory '{}' returned 'null' although it claimed it supports item type '{}'",
                            factory, profileTypeUID);
                } else {
                    entry.getValue().add(link.getUID());
                    return profile;
                }
            }
        }
        logger.debug("no ProfileFactory found which supports '{}'", profileTypeUID);
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

        handleEvent(itemName, command, commandEvent.getSource(), s -> acceptedCommandTypeMap.get(s),
                (profile, thing, convertedCommand) -> {
                    if (profile instanceof StateProfile) {
                        safeCaller.create(((StateProfile) profile), StateProfile.class) //
                                .withAsync() //
                                .withIdentifier(thing) //
                                .withTimeout(THINGHANDLER_EVENT_TIMEOUT) //
                                .build().onCommandFromItem(convertedCommand);
                    }
                });
    }

    private void receiveUpdate(ItemStateEvent updateEvent) {
        final String itemName = updateEvent.getItemName();
        final State newState = updateEvent.getItemState();
        handleEvent(itemName, newState, updateEvent.getSource(), s -> acceptedStateTypeMap.get(s),
                (profile, thing, convertedState) -> {
                    safeCaller.create(profile, Profile.class) //
                            .withAsync() //
                            .withIdentifier(thing) //
                            .withTimeout(THINGHANDLER_EVENT_TIMEOUT) //
                            .build().onStateUpdateFromItem(convertedState);
                });
    }

    @FunctionalInterface
    private static interface ProfileAction<T extends Type> {
        void handle(Profile profile, Thing thing, T type);
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
            Thing thing = getThing(channelUID.getThingUID());
            if (thing != null) {
                Channel channel = thing.getChannel(channelUID.getId());
                if (channel != null) {
                    @Nullable
                    T convertedType = toAcceptedType(type, channel, acceptedTypesFunction, item);
                    if (convertedType != null) {
                        if (thing.getHandler() != null) {
                            Profile profile = getProfile(link, item, thing);
                            action.handle(profile, thing, convertedType);
                        }
                    } else {
                        logger.debug(
                                "Received event '{}' ({}) could not be converted to any type accepted by item '{}' ({})",
                                type, type.getClass().getSimpleName(), itemName, item.getType());
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
    private <T extends Type> @Nullable T toAcceptedType(T originalType, Channel channel,
            Function<@Nullable String, @Nullable List<Class<? extends T>>> acceptedTypesFunction, Item item) {
        String acceptedItemType = channel.getAcceptedItemType();

        // DecimalType command sent to a NumberItem with dimension defined:
        if (originalType instanceof DecimalType && hasDimension(item, acceptedItemType)) {
            @Nullable
            QuantityType<?> quantityType = convertToQuantityType((DecimalType) originalType, item, acceptedItemType);
            if (quantityType != null) {
                return (T) quantityType;
            }
        }

        // The command is sent to an item w/o dimension defined and the channel is legacy (created from a ThingType
        // definition before UoM was introduced to the binding). The dimension information might now be defined on the
        // current ThingType. The binding might expect us to provide a QuantityType so try to convert to the dimension
        // the ChannelType provides.
        // This can be removed once a suitable solution for https://github.com/eclipse/smarthome/issues/2555 (Thing
        // migration) is found.
        if (originalType instanceof DecimalType && !hasDimension(item, acceptedItemType)
                && channelTypeDefinesDimension(channel.getChannelTypeUID())) {
            ChannelType channelType = channelTypeRegistry.getChannelType(channel.getChannelTypeUID());

            String acceptedItemTypeFromChannelType = channelType != null ? channelType.getItemType() : null;
            @Nullable
            QuantityType<?> quantityType = convertToQuantityType((DecimalType) originalType, item,
                    acceptedItemTypeFromChannelType);
            if (quantityType != null) {
                return (T) quantityType;
            }
        }

        if (acceptedItemType == null) {
            return originalType;
        }

        List<Class<? extends T>> acceptedTypes = acceptedTypesFunction.apply(acceptedItemType);
        if (acceptedTypes == null) {
            return originalType;
        }

        if (acceptedTypes.contains(originalType.getClass())) {
            return originalType;
        } else {
            // Look for class hierarchy and convert appropriately
            for (Class<? extends T> typeClass : acceptedTypes) {
                if (!typeClass.isEnum() && typeClass.isAssignableFrom(originalType.getClass()) //
                        && State.class.isAssignableFrom(typeClass) && originalType instanceof State) {
                    T ret = (T) ((State) originalType).as((Class<? extends State>) typeClass);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Converted '{}' ({}) to accepted type '{}' ({}) for channel '{}' ", originalType,
                                originalType.getClass().getSimpleName(), ret, ret.getClass().getName(),
                                channel.getUID());
                    }
                    return ret;
                }
            }
        }
        logger.debug("Received not accepted type '{}' for channel '{}'", originalType.getClass().getSimpleName(),
                channel.getUID());
        return null;
    }

    private boolean channelTypeDefinesDimension(@Nullable ChannelTypeUID channelTypeUID) {
        if (channelTypeUID == null) {
            return false;
        }

        ChannelType channelType = channelTypeRegistry.getChannelType(channelTypeUID);
        return channelType != null && getDimension(channelType.getItemType()) != null;
    }

    private boolean hasDimension(Item item, @Nullable String acceptedItemType) {
        return (item instanceof NumberItem && ((NumberItem) item).getDimension() != null)
                || getDimension(acceptedItemType) != null;
    }

    private @Nullable QuantityType<?> convertToQuantityType(DecimalType originalType, Item item,
            @Nullable String acceptedItemType) {
        NumberItem numberItem = (NumberItem) item;

        // DecimalType command sent via a NumberItem with dimension:
        Class<? extends Quantity<?>> dimension = numberItem.getDimension();

        if (dimension == null) {
            // DecimalType command sent via a plain NumberItem w/o dimension.
            // We try to guess the correct unit from the channel-type's expected item dimension
            // or from the item's state description.
            dimension = getDimension(acceptedItemType);
        }

        if (dimension != null) {
            return numberItem.toQuantityType(originalType, dimension);
        }

        return null;
    }

    private @Nullable Class<? extends Quantity<?>> getDimension(@Nullable String acceptedItemType) {
        if (acceptedItemType == null || acceptedItemType.isEmpty()) {
            return null;
        }
        String itemTypeExtension = ItemUtil.getItemTypeExtension(acceptedItemType);
        if (itemTypeExtension == null) {
            return null;
        }

        return UnitUtils.parseDimension(itemTypeExtension);
    }

    private @Nullable Item getItem(final String itemName) {
        return itemRegistry.get(itemName);
    }

    private void receiveTrigger(ChannelTriggeredEvent channelTriggeredEvent) {
        final ChannelUID channelUID = channelTriggeredEvent.getChannel();
        final String event = channelTriggeredEvent.getEvent();
        final Thing thing = getThing(channelUID.getThingUID());

        handleCallFromHandler(channelUID, thing, profile -> {
            if (profile instanceof TriggerProfile) {
                ((TriggerProfile) profile).onTriggerFromHandler(event);
            }
        });
    }

    public void stateUpdated(ChannelUID channelUID, State state) {
        final Thing thing = getThing(channelUID.getThingUID());

        handleCallFromHandler(channelUID, thing, profile -> {
            if (profile instanceof StateProfile) {
                ((StateProfile) profile).onStateUpdateFromHandler(state);
            }
        });
    }

    public void postCommand(ChannelUID channelUID, Command command) {
        final Thing thing = getThing(channelUID.getThingUID());

        handleCallFromHandler(channelUID, thing, profile -> {
            if (profile instanceof StateProfile) {
                ((StateProfile) profile).onCommandFromHandler(command);
            }
        });
    }

    void handleCallFromHandler(ChannelUID channelUID, @Nullable Thing thing, Consumer<Profile> action) {
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

    @Reference
    protected void setItemChannelLinkRegistry(ItemChannelLinkRegistry itemChannelLinkRegistry) {
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
        itemChannelLinkRegistry.addRegistryChangeListener(this);
    }

    protected void unsetItemChannelLinkRegistry(ItemChannelLinkRegistry itemChannelLinkRegistry) {
        this.itemChannelLinkRegistry = null;
    }

    @Reference
    protected void setThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = thingRegistry;
    }

    protected void unsetThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = null;
    }

    @Reference
    protected void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    protected void unsetEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = null;
    }

    @Reference
    protected void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    protected void unsetItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = null;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addProfileFactory(ProfileFactory profileFactory) {
        this.profileFactories.put(profileFactory, ConcurrentHashMap.newKeySet());
    }

    protected void removeProfileFactory(ProfileFactory profileFactory) {
        Set<String> links = this.profileFactories.remove(profileFactory);
        synchronized (profiles) {
            links.forEach(link -> {
                profiles.remove(link);
            });
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addProfileAdvisor(ProfileAdvisor profileAdvisor) {
        profileAdvisors.add(profileAdvisor);
    }

    protected void removeProfileAdvisor(ProfileAdvisor profileAdvisor) {
        profileAdvisors.remove(profileAdvisor);
    }

    @Reference
    protected void setDefaultProfileFactory(SystemProfileFactory defaultProfileFactory) {
        this.defaultProfileFactory = defaultProfileFactory;
    }

    protected void unsetDefaultProfileFactory(SystemProfileFactory defaultProfileFactory) {
        this.defaultProfileFactory = null;
    }

    @Reference
    protected void setSafeCaller(SafeCaller safeCaller) {
        this.safeCaller = safeCaller;
    }

    protected void unsetSafeCaller(SafeCaller safeCaller) {
        this.safeCaller = null;
    }

    @Reference
    public void setItemStateConverter(ItemStateConverter itemStateConverter) {
        this.itemStateConverter = itemStateConverter;
    }

    public void unsetItemStateConverter(ItemStateConverter itemStateConverter) {
        this.itemStateConverter = null;
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

    @Reference
    public void setAutoUpdateManager(AutoUpdateManager autoUpdateManager) {
        this.autoUpdateManager = autoUpdateManager;
    }

    public void unsetAutoUpdateManager(AutoUpdateManager autoUpdateManager) {
        this.autoUpdateManager = null;
    }

    @Reference
    public void setChannelTypeRegistry(ChannelTypeRegistry channelTypeRegistry) {
        this.channelTypeRegistry = channelTypeRegistry;
    }

    public void unsetChannelTypeRegistry(ChannelTypeRegistry channelTypeRegistry) {
        this.channelTypeRegistry = null;
    }

    private static class NoOpProfile implements Profile {
        @Override
        public ProfileTypeUID getProfileTypeUID() {
            return new ProfileTypeUID(ProfileTypeUID.SYSTEM_SCOPE, "noop");
        }

        @Override
        public void onStateUpdateFromItem(State state) {
            // no-op
        }
    }

}
