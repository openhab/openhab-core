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
package org.openhab.core.thing.binding;

import static java.util.Map.entry;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionBuilder;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameterBuilder;
import org.openhab.core.config.core.ConfigDescriptionProvider;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.items.ManagedItemProvider;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ManagedThingProvider;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.dto.ThingDTOMapper;
import org.openhab.core.thing.internal.ThingStorageEntity;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ManagedItemChannelLinkProvider;
import org.openhab.core.thing.type.ChannelDefinition;
import org.openhab.core.thing.type.ChannelDefinitionBuilder;
import org.openhab.core.thing.type.ChannelGroupType;
import org.openhab.core.thing.type.ChannelGroupTypeProvider;
import org.openhab.core.thing.type.ChannelGroupTypeUID;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeBuilder;
import org.openhab.core.thing.type.ChannelTypeProvider;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeBuilder;
import org.openhab.core.thing.type.ThingTypeRegistry;
import org.openhab.core.types.Command;
import org.osgi.service.component.ComponentContext;

/**
 * @author Simon Kaufmann - Initial contribution
 */
@SuppressWarnings("null")
@NonNullByDefault
public class ChangeThingTypeOSGiTest extends JavaOSGiTest {

    private static final String BINDING_ID = "testBinding";
    private static final String THING_TYPE_GENERIC_ID = "generic";
    private static final String THING_TYPE_SPECIFIC_ID = "specific";
    private static final ThingTypeUID THING_TYPE_GENERIC_UID = new ThingTypeUID(BINDING_ID, THING_TYPE_GENERIC_ID);
    private static final ThingTypeUID THING_TYPE_SPECIFIC_UID = new ThingTypeUID(BINDING_ID, THING_TYPE_SPECIFIC_ID);
    private static final String THING_ID = "testThing";
    private static final ChannelUID CHANNEL_GENERIC_UID = new ChannelUID(
            BINDING_ID + "::" + THING_ID + ":" + "channel" + THING_TYPE_GENERIC_ID);
    private static final ChannelUID CHANNEL_SPECIFIC_UID = new ChannelUID(
            BINDING_ID + "::" + THING_ID + ":" + "channel" + THING_TYPE_SPECIFIC_ID);
    private static final String ITEM_GENERIC = "item" + THING_TYPE_GENERIC_ID;
    private static final String ITEM_SPECIFIC = "item" + THING_TYPE_SPECIFIC_ID;
    private static final ItemChannelLink ITEM_CHANNEL_LINK_GENERIC = new ItemChannelLink(ITEM_GENERIC,
            CHANNEL_GENERIC_UID);
    private static final ItemChannelLink ITEM_CHANNEL_LINK_SPECIFIC = new ItemChannelLink(ITEM_SPECIFIC,
            CHANNEL_SPECIFIC_UID);

    private static final String PROPERTY_ON_GENERIC_THING_TYPE = "onlyGeneric";
    private static final String PROPERTY_ON_SPECIFIC_THING_TYPE = "onlySpecific";
    private static final String PROPERTY_ON_GENERIC_AND_SPECIFIC_THING_TYPE = "genericAndSpecific";
    private static final String GENERIC_VALUE = "generic";
    private static final String SPECIFIC_VALUE = "specific";

    private final Map<ThingTypeUID, ThingType> thingTypes = new HashMap<>();
    private final Map<URI, ConfigDescription> configDescriptions = new HashMap<>();
    private final Map<ChannelTypeUID, ChannelType> channelTypes = new HashMap<>();
    private final Map<ChannelGroupTypeUID, ChannelGroupType> channelGroupTypes = new HashMap<>();

    private @NonNullByDefault({}) ConfigDescriptionRegistry configDescriptionRegistry;
    private @NonNullByDefault({}) ManagedItemChannelLinkProvider managedItemChannelLinkProvider;
    private @NonNullByDefault({}) ManagedThingProvider managedThingProvider;
    private @NonNullByDefault({}) ManagedItemProvider managedItemProvider;
    private @NonNullByDefault({}) SampleThingHandlerFactory thingHandlerFactory;
    private @NonNullByDefault({}) ThingRegistry thingRegistry;

    private @NonNullByDefault({}) ThingType thingTypeGeneric;
    private @NonNullByDefault({}) ThingType thingTypeSpecific;

    private boolean selfChanging = false;

    private int specificInits = 0;
    private int genericInits = 0;
    private int unregisterHandlerDelay = 0;

    @BeforeEach
    public void setup() throws URISyntaxException {
        thingRegistry = getService(ThingRegistry.class);
        assertThat(thingRegistry, is(notNullValue()));

        registerVolatileStorageService();
        managedThingProvider = getService(ManagedThingProvider.class);
        assertThat(managedThingProvider, is(notNullValue()));

        configDescriptionRegistry = getService(ConfigDescriptionRegistry.class);
        assertThat(configDescriptionRegistry, is(notNullValue()));

        managedItemChannelLinkProvider = getService(ManagedItemChannelLinkProvider.class);
        assertThat(managedItemChannelLinkProvider, is(notNullValue()));

        managedItemProvider = getService(ManagedItemProvider.class);
        assertThat(managedItemProvider, is(notNullValue()));

        ComponentContext componentContext = mock(ComponentContext.class);
        when(componentContext.getBundleContext()).thenReturn(bundleContext);
        thingHandlerFactory = new SampleThingHandlerFactory();
        thingHandlerFactory.activate(componentContext);
        registerService(thingHandlerFactory, ThingHandlerFactory.class.getName());

        Map<String, String> thingTypeGenericProperties = Map.ofEntries(
                entry(PROPERTY_ON_GENERIC_THING_TYPE, GENERIC_VALUE),
                entry(PROPERTY_ON_GENERIC_AND_SPECIFIC_THING_TYPE, GENERIC_VALUE));

        Map<String, String> thingTypeSpecificProperties = Map.ofEntries(
                entry(PROPERTY_ON_SPECIFIC_THING_TYPE, SPECIFIC_VALUE),
                entry(PROPERTY_ON_GENERIC_AND_SPECIFIC_THING_TYPE, SPECIFIC_VALUE));

        thingTypeGeneric = registerThingTypeAndConfigDescription(THING_TYPE_GENERIC_UID, thingTypeGenericProperties);
        thingTypeSpecific = registerThingTypeAndConfigDescription(THING_TYPE_SPECIFIC_UID, thingTypeSpecificProperties);

        ThingTypeProvider thingTypeProvider = mock(ThingTypeProvider.class);
        when(thingTypeProvider.getThingType(any(), any()))
                .thenAnswer(invocation -> thingTypes.get(invocation.getArgument(0)));
        registerService(thingTypeProvider);

        ThingTypeRegistry thingTypeRegistry = mock(ThingTypeRegistry.class);
        when(thingTypeRegistry.getThingType(any(), any()))
                .thenAnswer(invocation -> thingTypes.get(invocation.getArgument(0)));
        registerService(thingTypeRegistry);

        ConfigDescriptionProvider configDescriptionProvider = mock(ConfigDescriptionProvider.class);
        when(configDescriptionProvider.getConfigDescription(any(), any()))
                .thenAnswer(invocation -> configDescriptions.get(invocation.getArgument(0)));
        registerService(configDescriptionProvider);

        ChannelTypeProvider channelTypeProvider = mock(ChannelTypeProvider.class);
        when(channelTypeProvider.getChannelTypes(any())).thenReturn(channelTypes.values());
        when(channelTypeProvider.getChannelType(any(), any()))
                .thenAnswer(invocation -> channelTypes.get(invocation.getArgument(0)));
        registerService(channelTypeProvider);

        ChannelGroupTypeProvider channelGroupTypeProvider = mock(ChannelGroupTypeProvider.class);
        when(channelGroupTypeProvider.getChannelGroupTypes(any())).thenReturn(channelGroupTypes.values());
        when(channelGroupTypeProvider.getChannelGroupType(any(), any()))
                .thenAnswer(invocation -> channelGroupTypes.get(invocation.getArgument(0)));
        registerService(channelGroupTypeProvider);

        managedItemProvider.add(new StringItem(ITEM_GENERIC));
        managedItemProvider.add(new StringItem(ITEM_SPECIFIC));

        managedItemChannelLinkProvider.add(ITEM_CHANNEL_LINK_GENERIC);
        managedItemChannelLinkProvider.add(ITEM_CHANNEL_LINK_SPECIFIC);
    }

    class SampleThingHandlerFactory extends BaseThingHandlerFactory {

        @Override
        public boolean supportsThingType(ThingTypeUID thingTypeUID) {
            return true;
        }

        @Override
        protected @Nullable ThingHandler createHandler(Thing thing) {
            if (THING_TYPE_GENERIC_UID.equals(thing.getThingTypeUID())) {
                return new GenericThingHandler(thing);
            }
            if (THING_TYPE_SPECIFIC_UID.equals(thing.getThingTypeUID())) {
                return new SpecificThingHandler(thing);
            }

            return null;
        }

        @Override
        public void unregisterHandler(Thing thing) {
            try {
                Thread.sleep(unregisterHandlerDelay);
            } catch (InterruptedException e) {
                //
            }
            super.unregisterHandler(thing);
        }
    }

    class GenericThingHandler extends BaseThingHandler {

        GenericThingHandler(Thing thing) {
            super(thing);
        }

        @Override
        public void initialize() {
            updateStatus(ThingStatus.ONLINE);
            genericInits++;
            if (selfChanging) {
                Map<String, Object> properties = Map.of("providedspecific", "there");
                changeThingType(THING_TYPE_SPECIFIC_UID, new Configuration(properties));
            }
        }

        @Override
        public void handleCommand(ChannelUID channelUID, Command command) {
            // nop
        }
    }

    class SpecificThingHandler extends BaseThingHandler {

        SpecificThingHandler(Thing thing) {
            super(thing);
        }

        @Override
        public void initialize() {
            // println "[ChangeThingTypeOSGiTest] SpecificThingHandler.initialize"
            specificInits++;
            updateStatus(ThingStatus.ONLINE);
        }

        @Override
        public void handleCommand(ChannelUID channelUID, Command command) {
            // nop
        }
    }

    @Test
    public void assertChangingTheThingTypeWorks() {
        // println "[ChangeThingTypeOSGiTest] ======== assert changing the ThingType works"
        Thing thing = ThingFactory.createThing(thingTypeGeneric, new ThingUID("testBinding", "testThing"),
                new Configuration(), null, configDescriptionRegistry);
        thing.setProperty("universal", "survives");
        managedThingProvider.add(thing);

        // Pre-flight checks - see below
        assertThat(thing.getHandler(), instanceOf(GenericThingHandler.class));
        assertThat(thing.getConfiguration().get("parametergeneric"), is("defaultgeneric"));
        assertThat(thing.getConfiguration().get("providedspecific"), is(nullValue()));
        assertThat(thing.getChannels().size(), is(1));
        assertThat(thing.getChannels().get(0).getUID(), is(CHANNEL_GENERIC_UID));
        assertThat(thing.getProperties().get("universal"), is("survives"));

        ThingHandlerFactory handlerFactory = getService(ThingHandlerFactory.class, SampleThingHandlerFactory.class);
        assertThat(handlerFactory, not(nullValue()));

        thing.getHandler().handleCommand(mock(ChannelUID.class), mock(Command.class));
        waitForAssert(() -> {
            assertThat(thing.getStatus(), is(ThingStatus.ONLINE));
        }, 4000, 100);

        // Now do the actual migration
        Map<String, Object> properties = new HashMap<>(1);
        properties.put("providedspecific", "there");
        ((BaseThingHandler) thing.getHandler()).changeThingType(THING_TYPE_SPECIFIC_UID, new Configuration(properties));

        assertThingWasChanged(thing);
    }

    @Test
    public void assertChangingThingTypeWithinInitializeWorks() {
        // println "[ChangeThingTypeOSGiTest] ======== assert changing thing type within initialize works"
        selfChanging = true;
        // println "[ChangeThingTypeOSGiTest] Create thing"
        Thing thing = ThingFactory.createThing(thingTypeGeneric, new ThingUID("testBinding", "testThing"),
                new Configuration(), null, configDescriptionRegistry);
        thing.setProperty("universal", "survives");
        // println "[ChangeThingTypeOSGiTest] Add thing to managed thing provider"
        managedThingProvider.add(thing);

        // println "[ChangeThingTypeOSGiTest] Wait for thing changed"
        assertThingWasChanged(thing);
    }

    @Test
    public void assertChangingThingTypeWithinInitializeWorksEvenIfServiceDeregistrationIsSlow() {
        // println "[ChangeThingTypeOSGiTest] ======== assert changing thing type within initialize works even if
        // service deregistration is slow"
        selfChanging = true;
        unregisterHandlerDelay = 6000;
        // println "[ChangeThingTypeOSGiTest] Create thing"
        Thing thing = ThingFactory.createThing(thingTypeGeneric, new ThingUID("testBinding", "testThing"),
                new Configuration(), null, configDescriptionRegistry);
        thing.setProperty("universal", "survives");
        // println "[ChangeThingTypeOSGiTest] Add thing to managed thing provider"
        managedThingProvider.add(thing);

        // println "[ChangeThingTypeOSGiTest] Wait for thing changed"
        assertThingWasChanged(thing);
    }

    @Test
    public void assertLoadingSpecializedThingTypeWorksDirectly() {
        // println "[ChangeThingTypeOSGiTest] ======== assert loading specialized thing type works directly"

        StorageService storage = getService(StorageService.class);
        Map<String, Object> properties = Map.of("providedspecific", "there");
        Thing persistedThing = ThingFactory.createThing(thingTypeSpecific,
                new ThingUID("testBinding", "persistedThing"), new Configuration(properties), null, null);
        persistedThing.setProperty("universal", "survives");
        storage.getStorage(Thing.class.getName()).put("testBinding::persistedThing",
                new ThingStorageEntity(ThingDTOMapper.map(persistedThing), false));
        selfChanging = true;

        unregisterService(storage);
        managedThingProvider = getService(ManagedThingProvider.class);
        assertThat(managedThingProvider, is(nullValue()));

        registerService(storage);
        managedThingProvider = getService(ManagedThingProvider.class);
        assertThat(managedThingProvider, is(notNullValue()));

        Collection<Thing> res = thingRegistry.getAll();
        assertThat(res.size(), is(1));

        Thing thing = res.iterator().next();
        assertThat(thing.getUID().toString(), is("testBinding::persistedThing"));

        // Ensure that the ThingHandler has been registered as an OSGi service correctly
        waitForAssert(() -> {
            assertThat(thing.getHandler(), instanceOf(SpecificThingHandler.class));
        }, 4000, 100);
        ThingHandlerFactory handlerFactory = getService(ThingHandlerFactory.class, SampleThingHandlerFactory.class);
        assertThat(handlerFactory, not(nullValue()));

        // Ensure it's initialized
        waitForAssert(() -> {
            assertThat(specificInits, is(1));
            assertThat(genericInits, is(0));
        });

        // Ensure the Thing is ONLINE again
        assertThat(thing.getStatus(), is(ThingStatus.ONLINE));
    }

    private void assertThingWasChanged(Thing thing) {
        // Ensure that the ThingHandler has been registered as an OSGi service correctly
        waitForAssert(() -> {
            assertThat(thing.getHandler(), instanceOf(SpecificThingHandler.class));
        }, 30000, 100);

        ThingHandlerFactory handlerFactory = getService(ThingHandlerFactory.class, SampleThingHandlerFactory.class);
        assertThat(handlerFactory, not(nullValue()));

        // Ensure it's initialized
        waitForAssert(() -> {
            assertThat(specificInits, is(1));
            assertThat(genericInits, is(1));
        }, 4000, 0);

        thing.getHandler().handleCommand(mock(ChannelUID.class), mock(Command.class));

        // Ensure that the provided configuration has been applied and default values have been added
        assertThat(thing.getConfiguration().get("parameterspecific"), is("defaultspecific"));
        assertThat(thing.getConfiguration().get("parametergeneric"), is(nullValue()));
        assertThat(thing.getConfiguration().get("providedspecific"), is("there"));

        // Ensure that the new set of channels is there
        assertThat(thing.getChannels().size(), is(1));
        assertThat(thing.getChannels().get(0).getUID().getId(), containsString("specific"));

        // Ensure that the old properties are still there
        assertThat(thing.getProperties().get("universal"), is("survives"));
        assertThat(thing.getProperties().get(PROPERTY_ON_GENERIC_THING_TYPE), is(GENERIC_VALUE));

        // Ensure that the properties of the new thing type are there
        assertThat(thing.getProperties().get(PROPERTY_ON_SPECIFIC_THING_TYPE), is(SPECIFIC_VALUE));
        assertThat(thing.getProperties().get(PROPERTY_ON_GENERIC_AND_SPECIFIC_THING_TYPE), is(SPECIFIC_VALUE));

        // Ensure the new thing type got written correctly into the storage
        assertThat(managedThingProvider.get(new ThingUID("testBinding", "testThing")).getThingTypeUID(),
                is(THING_TYPE_SPECIFIC_UID));

        // Ensure the Thing is ONLINE again
        assertThat(thing.getStatus(), is(ThingStatus.ONLINE));

        // Ensure the new thing type has been persisted into the database
        Storage<ThingStorageEntity> storage = getService(StorageService.class).getStorage(Thing.class.getName());
        Thing persistedThing = ThingDTOMapper.map(Objects.requireNonNull(storage.get("testBinding::testThing")), false);
        assertThat(persistedThing.getThingTypeUID().getAsString(), is("testBinding:specific"));
    }

    private ThingType registerThingTypeAndConfigDescription(ThingTypeUID thingTypeUID,
            Map<String, String> thingTypeProperties) throws URISyntaxException {
        URI configDescriptionUri = new URI("test:" + thingTypeUID.getId());
        ThingType thingType = ThingTypeBuilder.instance(thingTypeUID, "label")
                .withChannelDefinitions(getChannelDefinitions(thingTypeUID))
                .withConfigDescriptionURI(configDescriptionUri).withProperties(thingTypeProperties).build();
        ConfigDescription configDescription = ConfigDescriptionBuilder.create(configDescriptionUri)
                .withParameters(List.of(
                        ConfigDescriptionParameterBuilder
                                .create("parameter" + thingTypeUID.getId(), ConfigDescriptionParameter.Type.TEXT)
                                .withRequired(false).withDefault("default" + thingTypeUID.getId()).build(),
                        ConfigDescriptionParameterBuilder
                                .create("provided" + thingTypeUID.getId(), ConfigDescriptionParameter.Type.TEXT)
                                .withRequired(false).build()))
                .build();

        thingTypes.put(thingTypeUID, thingType);
        configDescriptions.put(configDescriptionUri, configDescription);

        return thingType;
    }

    private List<ChannelDefinition> getChannelDefinitions(ThingTypeUID thingTypeUID) throws URISyntaxException {
        URI configDescriptionUri = new URI("scheme", "channelType:" + thingTypeUID.getId(), null);
        ChannelTypeUID channelTypeUID = new ChannelTypeUID("test:" + thingTypeUID.getId());
        ChannelType channelType = ChannelTypeBuilder.state(channelTypeUID, "channelLabel", "itemType")
                .withDescription("description") //
                .withCategory("category") //
                .withConfigDescriptionURI(configDescriptionUri).build();
        channelTypes.put(channelTypeUID, channelType);

        ConfigDescription configDescription = ConfigDescriptionBuilder.create(configDescriptionUri).build();
        configDescriptions.put(configDescriptionUri, configDescription);

        ChannelDefinition cd = new ChannelDefinitionBuilder("channel" + thingTypeUID.getId(), channelTypeUID).build();
        return List.of(cd);
    }
}
