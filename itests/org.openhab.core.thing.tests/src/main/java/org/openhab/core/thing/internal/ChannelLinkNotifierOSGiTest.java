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

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.items.ManagedItemProvider;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ManagedThingProvider;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingManager;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.link.ManagedItemChannelLinkProvider;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.util.ThingHandlerHelper;
import org.openhab.core.types.Command;
import org.openhab.core.util.BundleResolver;
import org.osgi.framework.Bundle;

/**
 * Tests {@link ChannelLinkNotifier}.
 *
 * @author Wouter Born - Initial contribution
 */
@NonNullByDefault
public class ChannelLinkNotifierOSGiTest extends JavaOSGiTest {

    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID("binding:type");
    private static final ChannelTypeUID CHANNEL_TYPE_UID = new ChannelTypeUID("binding:channelType");
    private static final int CHANNEL_COUNT = 5;

    private int thingCount;

    private @NonNullByDefault({}) AutoCloseable mocksCloseable;

    private @NonNullByDefault({}) ItemChannelLinkRegistry itemChannelLinkRegistry;
    private @NonNullByDefault({}) ManagedItemChannelLinkProvider managedItemChannelLinkProvider;
    private @NonNullByDefault({}) ManagedItemProvider managedItemProvider;
    private @NonNullByDefault({}) ManagedThingProvider managedThingProvider;
    private @NonNullByDefault({}) ThingRegistry thingRegistry;

    private @Mock @NonNullByDefault({}) Bundle bundleMock;
    private @Mock @NonNullByDefault({}) BundleResolver bundleResolverMock;
    private @Mock @NonNullByDefault({}) ThingHandlerFactory thingHandlerFactoryMock;

    /**
     * A thing handler which updates the {@link ThingStatus} when initialized to the provided {@code thingStatus} value.
     * It also keeps track of the channels for which the handler received channel (un)linked events.
     */
    class TestHandler extends BaseThingHandler {

        private final @Nullable ThingStatus thingStatus;

        private final Map<ChannelUID, List<Boolean>> channelLinkEvents = new HashMap<>();

        public TestHandler(Thing thing, @Nullable ThingStatus thingStatus) {
            super(thing);
            this.thingStatus = thingStatus;
            resetChannelLinkEvents();
        }

        @Override
        public void handleCommand(ChannelUID channelUID, Command command) {
        }

        @Override
        public void initialize() {
            ThingStatus localThingStatus = thingStatus;
            if (localThingStatus != null) {
                updateStatus(localThingStatus);
            }
        }

        @Override
        public void channelLinked(ChannelUID channelUID) {
            channelLinkEvents.getOrDefault(channelUID, List.of()).add(Boolean.TRUE);
        }

        @Override
        public void channelUnlinked(ChannelUID channelUID) {
            channelLinkEvents.getOrDefault(channelUID, List.of()).add(Boolean.FALSE);
        }

        public List<Boolean> getChannelLinkEvents(ChannelUID channelUID) {
            return channelLinkEvents.getOrDefault(channelUID, List.of());
        }

        public @Nullable Boolean isLinkedBasedOnEvent(ChannelUID channelUID) {
            List<Boolean> events = getChannelLinkEvents(channelUID);
            return events.isEmpty() ? null : events.get(events.size() - 1);
        }

        public void resetChannelLinkEvents() {
            channelLinkEvents.clear();
            forEachThingChannelUID(thing, channelUID -> channelLinkEvents.put(channelUID, new ArrayList<>()));
        }
    }

    @BeforeEach
    public void beforeEach() {
        mocksCloseable = openMocks(this);

        registerVolatileStorageService();

        itemChannelLinkRegistry = getService(ItemChannelLinkRegistry.class);
        assertThat(itemChannelLinkRegistry, is(notNullValue()));

        managedItemChannelLinkProvider = getService(ManagedItemChannelLinkProvider.class);
        assertThat(managedItemChannelLinkProvider, is(notNullValue()));

        managedItemProvider = getService(ManagedItemProvider.class);
        assertThat(managedItemProvider, is(notNullValue()));

        managedThingProvider = getService(ManagedThingProvider.class);
        assertThat(managedThingProvider, is(notNullValue()));

        thingRegistry = getService(ThingRegistry.class);
        assertThat(thingRegistry, is(notNullValue()));

        List<ChannelLinkNotifier> notifiers = getServices(RegistryChangeListener.class, ChannelLinkNotifier.class);
        assertThat(notifiers.size(), is(1));

        when(thingHandlerFactoryMock.supportsThingType(eq(THING_TYPE_UID))).thenReturn(true);
        registerService(thingHandlerFactoryMock);

        when(bundleMock.getSymbolicName()).thenReturn("org.openhab.core.thing");
        when(bundleResolverMock.resolveBundle(any())).thenReturn(bundleMock);

        ThingManagerImpl thingManager = (ThingManagerImpl) getService(ThingManager.class);
        assertThat(thingManager, is(notNullValue()));
        if (thingManager != null) {
            thingManager.setBundleResolver(bundleResolverMock);
        }
    }

    @AfterEach
    public void afterEach() throws Exception {
        managedItemChannelLinkProvider.getAll()
                .forEach(itemChannelLink -> managedItemChannelLinkProvider.remove(itemChannelLink.getUID()));
        managedItemProvider.getAll().forEach(item -> managedItemProvider.remove(item.getUID()));
        managedThingProvider.getAll().forEach(thing -> managedThingProvider.remove(thing.getUID()));

        thingCount = 0;

        mocksCloseable.close();
    }

    private Thing addThing(@Nullable ThingStatus thingStatus) {
        Thing thing = createThing();
        TestHandler handler = new TestHandler(thing, thingStatus);
        when(thingHandlerFactoryMock.registerHandler(thing)).thenReturn(handler);
        managedThingProvider.add(thing);
        return thing;
    }

    private TestHandler getHandler(Thing thing) {
        TestHandler handler = (TestHandler) thing.getHandler();
        if (handler != null) {
            return handler;
        }
        throw new IllegalStateException("Thing '" + thing.getUID() + "' has no thing handler");
    }

    private Thing addInitializedThing() {
        Thing thing = addThing(ThingStatus.ONLINE);
        assertThat(ThingHandlerHelper.isHandlerInitialized(getHandler(thing)), is(true));
        return thing;
    }

    private Thing addUninitializedThing() {
        Thing thing = addThing(null);
        assertThat(ThingHandlerHelper.isHandlerInitialized(getHandler(thing)), is(false));
        return thing;
    }

    private Thing createThing() {
        ThingUID thingUID = new ThingUID(THING_TYPE_UID, "thing" + thingCount++);
        List<Channel> channels = IntStream.range(0, CHANNEL_COUNT).mapToObj(index -> createChannel(thingUID, index))
                .collect(Collectors.toList());
        return ThingBuilder.create(THING_TYPE_UID, thingUID).withChannels(channels).build();
    }

    private Channel createChannel(ThingUID thingUID, int index) {
        ChannelUID channelUID = new ChannelUID(thingUID, "channel" + index);
        return ChannelBuilder.create(channelUID, CoreItemFactory.NUMBER).withKind(ChannelKind.STATE)
                .withType(CHANNEL_TYPE_UID).build();
    }

    private void forEachThingChannelUID(Thing thing, Consumer<ChannelUID> consumer) {
        thing.getChannels().stream().map(Channel::getUID).forEach(channelUID -> consumer.accept(channelUID));
    }

    private void addItemsAndLinks(Thing thing, String itemSuffix) {
        forEachThingChannelUID(thing, channelUID -> {
            String itemName = getItemName(thing, channelUID, itemSuffix);
            managedItemProvider.add(new NumberItem(itemName));
            managedItemChannelLinkProvider.add(new ItemChannelLink(itemName, channelUID));
        });
    }

    private void removeItemsAndLinks(Thing thing, String itemSuffix) {
        forEachThingChannelUID(thing, channelUID -> {
            String itemName = getItemName(thing, channelUID, itemSuffix);

            Optional<ItemChannelLink> itemChannelLink = managedItemChannelLinkProvider.getAll().stream()
                    .filter(icl -> itemName.equals(icl.getItemName())).findFirst();
            if (itemChannelLink.isPresent()) {
                managedItemChannelLinkProvider.remove(itemChannelLink.get().getUID());
            }

            managedItemProvider.remove(itemName);
        });
    }

    private void updateLinks(Thing thing, String itemSuffix) {
        forEachThingChannelUID(thing, channelUID -> {
            String itemName = getItemName(thing, channelUID, itemSuffix);
            managedItemChannelLinkProvider.update(new ItemChannelLink(itemName, channelUID));
        });
    }

    private String getItemName(Thing thing, ChannelUID channelUID, String itemSuffix) {
        String itemName = thing.getUID().getId() + "_" + channelUID.getId();
        if (!itemSuffix.isBlank()) {
            itemName += "_" + itemSuffix;
        }
        return itemName;
    }

    private void assertAllChannelsLinkedBasedOnEvents(Thing thing, int eventCount) {
        TestHandler handler = getHandler(thing);
        forEachThingChannelUID(thing, channelUID -> {
            assertThat(handler.isLinkedBasedOnEvent(channelUID), is(true));
            assertThat(handler.getChannelLinkEvents(channelUID).size(), is(eventCount));
        });
    }

    private void assertAllChannelsUnlinkedBasedOnEvents(Thing thing, int eventCount) {
        TestHandler handler = getHandler(thing);
        forEachThingChannelUID(thing, channelUID -> {
            assertThat(handler.isLinkedBasedOnEvent(channelUID), is(false));
            assertThat(handler.getChannelLinkEvents(channelUID).size(), is(eventCount));
        });
    }

    private void assertNoChannelLinkEventsReceived(Thing thing) {
        TestHandler handler = getHandler(thing);
        forEachThingChannelUID(thing, channelUID -> {
            assertThat(handler.isLinkedBasedOnEvent(channelUID), is(nullValue()));
            assertThat(handler.getChannelLinkEvents(channelUID).size(), is(0));
        });
    }

    @Test
    public void initializedThingHandlerReceivesChannelLinkedEventsWithSingleLinkWhenNotifierIsActivated() {
        Thing subjectThing = addInitializedThing();

        // link each thing channel to an item
        addItemsAndLinks(subjectThing, "link1");

        getHandler(subjectThing).resetChannelLinkEvents();

        registerService(new ChannelLinkNotifier(itemChannelLinkRegistry, thingRegistry));
        assertAllChannelsLinkedBasedOnEvents(subjectThing, 1);
    }

    @Test
    public void initializedThingHandlerReceivesChannelLinkedEventsWithMultipleLinksWhenNotifierIsActivated() {
        Thing subjectThing = addInitializedThing();

        // link each thing channel to three items
        addItemsAndLinks(subjectThing, "link1");
        addItemsAndLinks(subjectThing, "link2");
        addItemsAndLinks(subjectThing, "link3");

        getHandler(subjectThing).resetChannelLinkEvents();

        registerService(new ChannelLinkNotifier(itemChannelLinkRegistry, thingRegistry));
        assertAllChannelsLinkedBasedOnEvents(subjectThing, 1);
    }

    @Test
    public void uninitializedThingHandlerReceivesNoChannelLinkedEventsWhenNotifierIsActivated() {
        Thing subjectThing = addUninitializedThing();

        // link each thing channel to an item
        addItemsAndLinks(subjectThing, "link1");

        registerService(new ChannelLinkNotifier(itemChannelLinkRegistry, thingRegistry));
        assertNoChannelLinkEventsReceived(subjectThing);
    }

    @Test
    public void initializedThingHandlerReceivesChannelLinkedEventsWhenAddingLinks() {
        Thing subjectThing = addInitializedThing();
        Thing otherThing = addInitializedThing();

        // link each thing channel to an item
        addItemsAndLinks(subjectThing, "link1");
        assertAllChannelsLinkedBasedOnEvents(subjectThing, 1);
        assertNoChannelLinkEventsReceived(otherThing);

        getHandler(subjectThing).resetChannelLinkEvents();

        // link each thing channel also to a second item
        addItemsAndLinks(subjectThing, "link2");
        assertAllChannelsLinkedBasedOnEvents(subjectThing, 1);
        assertNoChannelLinkEventsReceived(otherThing);
    }

    @Test
    public void initializedThingHandlerReceivesChannelUnlinkedEventsWithSingleLinkWhenRemovingLinks() {
        Thing subjectThing = addInitializedThing();
        Thing otherThing = addInitializedThing();

        // link each thing channel to an item
        addItemsAndLinks(subjectThing, "link1");
        assertAllChannelsLinkedBasedOnEvents(subjectThing, 1);
        assertNoChannelLinkEventsReceived(otherThing);

        getHandler(subjectThing).resetChannelLinkEvents();

        // unlink each thing channel from each item
        removeItemsAndLinks(subjectThing, "link1");
        assertAllChannelsUnlinkedBasedOnEvents(subjectThing, 1);
        assertNoChannelLinkEventsReceived(otherThing);
    }

    @Test
    public void initializedThingHandlerReceivesNoChannelUnlinkedEventsWithMultipleLinksWhenRemovingOneLink() {
        Thing subjectThing = addInitializedThing();
        Thing otherThing = addInitializedThing();

        // link each thing channel to two items
        addItemsAndLinks(subjectThing, "link1");
        addItemsAndLinks(subjectThing, "link2");
        assertAllChannelsLinkedBasedOnEvents(subjectThing, 2);
        assertNoChannelLinkEventsReceived(otherThing);

        getHandler(subjectThing).resetChannelLinkEvents();

        // remove one of the links from each thing channel so they are still linked to one item
        removeItemsAndLinks(subjectThing, "link2");
        assertNoChannelLinkEventsReceived(subjectThing);
        assertNoChannelLinkEventsReceived(otherThing);
    }

    @Test
    public void initializedThingHandlerReceivesChannelUnlinkedEventsWithMultipleLinksWhenRemovingAllLinks() {
        Thing subjectThing = addInitializedThing();
        Thing otherThing = addInitializedThing();

        // link each thing channel to two items
        addItemsAndLinks(subjectThing, "link1");
        addItemsAndLinks(subjectThing, "link2");
        assertAllChannelsLinkedBasedOnEvents(subjectThing, 2);
        assertNoChannelLinkEventsReceived(otherThing);

        getHandler(subjectThing).resetChannelLinkEvents();

        // unlink each thing channel from all items
        removeItemsAndLinks(subjectThing, "link1");
        removeItemsAndLinks(subjectThing, "link2");
        assertAllChannelsUnlinkedBasedOnEvents(subjectThing, 1);
        assertNoChannelLinkEventsReceived(otherThing);
    }

    @Test
    public void initializedThingHandlerReceivesChannelLinkedEventsWhenUpdatingLinks() {
        Thing subjectThing = addInitializedThing();
        Thing otherThing = addInitializedThing();

        // link each thing channel to an item
        addItemsAndLinks(subjectThing, "link1");
        assertAllChannelsLinkedBasedOnEvents(subjectThing, 1);
        assertNoChannelLinkEventsReceived(otherThing);

        getHandler(subjectThing).resetChannelLinkEvents();

        // update the links of each thing channel
        updateLinks(subjectThing, "link1");
        assertAllChannelsLinkedBasedOnEvents(subjectThing, 1);
        assertNoChannelLinkEventsReceived(otherThing);
    }

    @Test
    public void uninitializedThingHandlerReceivesNoChannelLinkedEventsWhenAddingLinks() {
        Thing subjectThing = addUninitializedThing();
        Thing otherThing = addInitializedThing();

        // link each thing channel to an item
        addItemsAndLinks(subjectThing, "link1");
        assertNoChannelLinkEventsReceived(subjectThing);
        assertNoChannelLinkEventsReceived(otherThing);

        // link each thing channel also to a second item
        addItemsAndLinks(subjectThing, "link2");
        assertNoChannelLinkEventsReceived(subjectThing);
        assertNoChannelLinkEventsReceived(otherThing);
    }

    @Test
    public void uninitializedThingHandlerReceivesNoChannelUnlinkedEventsWhenRemovingLinks() {
        Thing subjectThing = addUninitializedThing();
        Thing otherThing = addInitializedThing();

        // link each thing channel to an item
        addItemsAndLinks(subjectThing, "link1");
        assertNoChannelLinkEventsReceived(subjectThing);
        assertNoChannelLinkEventsReceived(otherThing);

        // unlink each thing channel from each item
        removeItemsAndLinks(subjectThing, "link1");
        assertNoChannelLinkEventsReceived(subjectThing);
        assertNoChannelLinkEventsReceived(otherThing);
    }

    @Test
    public void uninitializedThingHandlerReceivesNoChannelLinkedEventsWhenUpdatingLinks() {
        Thing subjectThing = addUninitializedThing();
        Thing otherThing = addInitializedThing();

        // link each thing channel to an item
        addItemsAndLinks(subjectThing, "link1");
        assertNoChannelLinkEventsReceived(subjectThing);
        assertNoChannelLinkEventsReceived(otherThing);

        // update the links of each thing channel
        updateLinks(subjectThing, "link1");
        assertNoChannelLinkEventsReceived(subjectThing);
        assertNoChannelLinkEventsReceived(otherThing);
    }
}
