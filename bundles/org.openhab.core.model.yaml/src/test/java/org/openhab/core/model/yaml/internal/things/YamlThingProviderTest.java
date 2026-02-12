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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionBuilder;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.config.core.ConfigDescriptionParameterBuilder;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.ConfigUtil;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.model.yaml.YamlModelUtils;
import org.openhab.core.model.yaml.internal.YamlModelRepositoryImpl;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.WatchService;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.internal.ThingFactoryHelper;
import org.openhab.core.thing.type.AutoUpdatePolicy;
import org.openhab.core.thing.type.ChannelDefinition;
import org.openhab.core.thing.type.ChannelDefinitionBuilder;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeBuilder;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeBuilder;
import org.openhab.core.thing.type.ThingTypeRegistry;
import org.openhab.core.types.Command;
import org.openhab.core.util.BundleResolver;
import org.osgi.framework.Bundle;

/**
 * The {@link YamlThingProviderTest} contains tests for the {@link YamlThingProvider} class.
 *
 * @author Laurent Garnier - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class YamlThingProviderTest {
    private static final Path SOURCE_PATH = Path.of("src/test/resources/model/things");
    private static final String MODEL_NAME = "model.yaml";
    private static final Path MODEL_PATH = Path.of(MODEL_NAME);

    private static final String XML_THING_TYPE = "openhab.xmlThingTypes";
    private static final String NTP_BUNDLE_SYMBOLIC_NAME = "org.openhab.binding.ntp";
    private static final ThingTypeUID NTP_THING_TYPE_UID = new ThingTypeUID("ntp", "ntp");
    private static final String NTP_THING_TYPE_LABEL = "NTP Server";
    private static final String NTP_THING_TYPE_DESCRIPTION = "An NTP server that provides current date and time";
    private static final String NTP_THING_TYPE_EQUIPMENT_TAG = "WebService";
    private static final String DEFAULT_HOSTNAME = "0.pool.ntp.org";
    private static final int DEFAULT_REFRESH_INTERVAL = 60;
    private static final int DEFAULT_REFRESH_NTP = 30;
    private static final int DEFAULT_SERVER_PORT = 123;

    private static final ChannelTypeUID NTP_CHANNEL_TYPE_STRING_CHANNEL_UID = new ChannelTypeUID("ntp",
            "string-channel");
    private static final String NTP_CHANNEL_TYPE_STRING_CHANNEL_LABEL = "Date";
    private static final String NTP_CHANNEL_TYPE_STRING_CHANNEL_DESCRIPTION = "NTP refreshed date and time.";
    private static final String NTP_CHANNEL_TYPE_STRING_CHANNEL_ITEM_TYPE = "String";
    private static final String NTP_CHANNEL_TYPE_STRING_CHANNEL_CATEGORY = "Time";
    private static final String TAG_STATUS = "Status";
    private static final String TAG_TIMESTAMP = "Timestamp";

    private static final String NTP_CHANNEL_STRING_ID = "string";
    private static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss z";

    private static final ThingUID NTP_THING_UID = new ThingUID(NTP_THING_TYPE_UID, "local");

    private @Mock @NonNullByDefault({}) WatchService watchServiceMock;
    private @TempDir @NonNullByDefault({}) Path watchPath;
    private @NonNullByDefault({}) Path fullModelPath;

    private @Mock @NonNullByDefault({}) BundleResolver bundleResolver;
    private @Mock @NonNullByDefault({}) ThingTypeRegistry thingTypeRegistry;
    private @Mock @NonNullByDefault({}) ChannelTypeRegistry channelTypeRegistry;
    private @Mock @NonNullByDefault({}) ConfigDescriptionRegistry configDescriptionRegistry;
    private @Mock @NonNullByDefault({}) LocaleProvider localeProvider;

    private @Mock @NonNullByDefault({}) Bundle bundle;

    private @NonNullByDefault({}) YamlModelRepositoryImpl modelRepository;
    private @NonNullByDefault({}) YamlThingProvider thingProvider;
    private @NonNullByDefault({}) TestThingChangeListener thingListener;

    @BeforeEach
    public void setup() {
        fullModelPath = watchPath.resolve(MODEL_PATH);
        when(watchServiceMock.getWatchPath()).thenReturn(watchPath);

        when(bundleResolver.resolveBundle(any())).thenReturn(bundle);
        when(bundle.getSymbolicName()).thenReturn("org.openhab.binding.ntp");

        URI uriThingType = null;
        URI uriChannelType = null;
        URI uriThing1 = null;
        URI uriChannel1 = null;
        URI uriChannel2 = null;
        try {
            uriThingType = new URI("thing-type:" + NTP_THING_TYPE_UID.getAsString());
            uriChannelType = new URI("channel-type:" + NTP_CHANNEL_TYPE_STRING_CHANNEL_UID.getAsString());
            uriThing1 = new URI("thing:" + NTP_THING_UID.getAsString());
            uriChannel1 = new URI("channel:" + NTP_THING_UID.getAsString() + ":" + NTP_CHANNEL_STRING_ID);
            uriChannel2 = new URI("channel:" + NTP_THING_UID.getAsString() + ":date-only-string");
        } catch (URISyntaxException e) {
        }
        assertNotNull(uriThingType);
        assertNotNull(uriChannelType);
        assertNotNull(uriThing1);
        assertNotNull(uriChannel1);
        assertNotNull(uriChannel2);

        List<ChannelDefinition> channelDefinitions = new ArrayList<>();
        channelDefinitions
                .add(new ChannelDefinitionBuilder(NTP_CHANNEL_STRING_ID, NTP_CHANNEL_TYPE_STRING_CHANNEL_UID).build());
        ThingType ntpThingType = ThingTypeBuilder.instance(NTP_THING_TYPE_UID, NTP_THING_TYPE_LABEL)
                .withDescription(NTP_THING_TYPE_DESCRIPTION).withConfigDescriptionURI(uriThingType)
                .withSemanticEquipmentTag(NTP_THING_TYPE_EQUIPMENT_TAG).withChannelDefinitions(channelDefinitions)
                .build();

        when(thingTypeRegistry.getThingType(eq(NTP_THING_TYPE_UID))).thenReturn(ntpThingType);
        when(thingTypeRegistry.getThingType(eq(NTP_THING_TYPE_UID), eq(Locale.FRENCH))).thenReturn(ntpThingType);

        ChannelType channelType = ChannelTypeBuilder
                .state(NTP_CHANNEL_TYPE_STRING_CHANNEL_UID, NTP_CHANNEL_TYPE_STRING_CHANNEL_LABEL,
                        NTP_CHANNEL_TYPE_STRING_CHANNEL_ITEM_TYPE)
                .withDescription(NTP_CHANNEL_TYPE_STRING_CHANNEL_DESCRIPTION)
                .withCategory(NTP_CHANNEL_TYPE_STRING_CHANNEL_CATEGORY).withConfigDescriptionURI(uriChannelType)
                .withTags(Set.of(TAG_STATUS, TAG_TIMESTAMP)).build();

        when(channelTypeRegistry.getChannelType(eq(NTP_CHANNEL_TYPE_STRING_CHANNEL_UID))).thenReturn(channelType);
        when(channelTypeRegistry.getChannelType(eq(NTP_CHANNEL_TYPE_STRING_CHANNEL_UID), eq(Locale.FRENCH)))
                .thenReturn(channelType);

        List<ConfigDescriptionParameter> params = new ArrayList<>();
        params.add(ConfigDescriptionParameterBuilder.create("hostname", Type.TEXT).withRequired(true)
                .withContext("network-address").withLabel("Hostname<").withDescription("The NTP server hostname.")
                .withDefault(DEFAULT_HOSTNAME).build());
        params.add(
                ConfigDescriptionParameterBuilder.create("refreshInterval", Type.INTEGER).withLabel("Refresh Interval")
                        .withDescription("Interval that new time updates are posted to the event bus in seconds.")
                        .withDefault(String.valueOf(DEFAULT_REFRESH_INTERVAL)).build());
        params.add(
                ConfigDescriptionParameterBuilder.create("refreshNtp", Type.INTEGER).withLabel("NTP Refresh Frequency<")
                        .withDescription("Number of updates before querying the NTP server.")
                        .withDefault(String.valueOf(DEFAULT_REFRESH_NTP)).build());
        params.add(ConfigDescriptionParameterBuilder.create("serverPort", Type.INTEGER).withLabel("Server Port")
                .withDescription("The port that the NTP server could use.")
                .withDefault(String.valueOf(DEFAULT_SERVER_PORT)).build());
        params.add(ConfigDescriptionParameterBuilder.create("timeZone", Type.TEXT).withLabel("Timezone")
                .withDescription("The configured timezone.").build());
        ConfigDescription configDescrThing = ConfigDescriptionBuilder.create(uriThingType).withParameters(params)
                .build();
        when(configDescriptionRegistry.getConfigDescription(eq(uriThingType))).thenReturn(configDescrThing);

        ConfigDescriptionParameter param = ConfigDescriptionParameterBuilder.create("DateTimeFormat", Type.TEXT)
                .withRequired(false).withLabel("Date Time Format").withDescription("Formatting of the date and time.")
                .withDefault(DEFAULT_DATE_TIME_FORMAT).build();
        ConfigDescription configDescrChannel2 = ConfigDescriptionBuilder.create(uriChannelType).withParameter(param)
                .build();
        when(configDescriptionRegistry.getConfigDescription(eq(uriChannelType))).thenReturn(configDescrChannel2);

        when(configDescriptionRegistry.getConfigDescription(eq(uriThing1))).thenReturn(null);
        when(configDescriptionRegistry.getConfigDescription(eq(uriChannel1))).thenReturn(null);
        when(configDescriptionRegistry.getConfigDescription(eq(uriChannel2))).thenReturn(null);

        when(localeProvider.getLocale()).thenReturn(Locale.FRENCH);

        NtpThingHandlerFactory thingHandlerFactory = new NtpThingHandlerFactory();
        thingProvider = new YamlThingProvider(bundleResolver, thingTypeRegistry, channelTypeRegistry,
                configDescriptionRegistry, localeProvider);
        thingProvider.onReadyMarkerAdded(new ReadyMarker(XML_THING_TYPE, NTP_BUNDLE_SYMBOLIC_NAME));
        thingProvider.addThingHandlerFactory(thingHandlerFactory);

        thingListener = new TestThingChangeListener();
        thingProvider.addProviderChangeListener(thingListener);

        modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(thingProvider);
    }

    @Test
    public void testLoadModelWithThing() throws IOException {
        Files.copy(SOURCE_PATH.resolve("thing.yaml"), fullModelPath);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);

        assertFalse(YamlModelUtils.isIsolatedModel(MODEL_NAME));
        assertThat(thingListener.things, is(aMapWithSize(1)));
        assertThat(thingListener.things, hasKey("ntp:ntp:local"));
        assertThat(thingProvider.getAllFromModel(MODEL_NAME), hasSize(1));
        Collection<Thing> things = thingProvider.getAll();
        assertThat(things, hasSize(1));
        Thing thing = things.iterator().next();
        assertFalse(thing instanceof Bridge);
        assertEquals(NTP_THING_UID, thing.getUID());
        assertNull(thing.getBridgeUID());
        assertEquals(NTP_THING_TYPE_UID, thing.getThingTypeUID());
        assertEquals("NTP Local Server", thing.getLabel());
        assertEquals("Paris", thing.getLocation());
        assertEquals(NTP_THING_TYPE_EQUIPMENT_TAG, thing.getSemanticEquipmentTag());
        assertEquals(0, thing.getProperties().size());

        // 2 parameters injected with default value
        assertThat(thing.getConfiguration().keySet(),
                containsInAnyOrder("hostname", "refreshInterval", "refreshNtp", "serverPort", "timeZone", "other"));
        assertEquals("0.fr.pool.ntp.org", thing.getConfiguration().get("hostname"));
        assertEquals(BigDecimal.valueOf(123), thing.getConfiguration().get("serverPort"));
        assertEquals("Europe/Paris", thing.getConfiguration().get("timeZone"));
        assertEquals("A parameter that is not in the thing config description.", thing.getConfiguration().get("other"));
        // default value injected for parameter refreshInterval
        assertEquals(BigDecimal.valueOf(DEFAULT_REFRESH_INTERVAL), thing.getConfiguration().get("refreshInterval"));
        // default value injected for parameter refreshNtp
        assertEquals(BigDecimal.valueOf(DEFAULT_REFRESH_NTP), thing.getConfiguration().get("refreshNtp"));

        assertEquals(2, thing.getChannels().size());
        Iterator<Channel> it = thing.getChannels().iterator();
        Channel channel = it.next();
        assertEquals(new ChannelUID(NTP_THING_UID, "string"), channel.getUID());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_UID, channel.getChannelTypeUID());
        assertEquals(ChannelKind.STATE, channel.getKind());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_ITEM_TYPE, channel.getAcceptedItemType());
        // label in YAML is ignored for a channel provided by the channel type
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_LABEL, channel.getLabel());
        // description in YAML is ignored for a channel provided by the channel type
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_DESCRIPTION, channel.getDescription());
        assertNull(channel.getAutoUpdatePolicy());
        assertThat(channel.getDefaultTags(), containsInAnyOrder(TAG_STATUS, TAG_TIMESTAMP));
        assertEquals(0, channel.getProperties().size());
        assertThat(channel.getConfiguration().keySet(), containsInAnyOrder("DateTimeFormat", "other"));
        assertEquals("dd-MM-yyyy HH:mm", channel.getConfiguration().get("DateTimeFormat"));
        assertEquals("A parameter that is not in the channel config description.",
                channel.getConfiguration().get("other"));
        channel = it.next();
        assertEquals(new ChannelUID(NTP_THING_UID, "date-only-string"), channel.getUID());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_UID, channel.getChannelTypeUID());
        assertEquals(ChannelKind.STATE, channel.getKind());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_ITEM_TYPE, channel.getAcceptedItemType());
        assertEquals("Date Only", channel.getLabel());
        assertEquals("Format with date only.", channel.getDescription());
        assertNull(channel.getAutoUpdatePolicy());
        assertThat(channel.getDefaultTags(), hasSize(0));
        assertEquals(0, channel.getProperties().size());
        assertThat(channel.getConfiguration().keySet(), containsInAnyOrder("DateTimeFormat", "other"));
        assertEquals("dd-MM-yyyy", channel.getConfiguration().get("DateTimeFormat"));
        assertEquals("A parameter that is not in the channel config description.",
                channel.getConfiguration().get("other"));
    }

    @Test
    public void testLoadModelWithThingEmptyConfig() throws IOException {
        Files.copy(SOURCE_PATH.resolve("thingWithEmptyConfig.yaml"), fullModelPath);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);

        assertFalse(YamlModelUtils.isIsolatedModel(MODEL_NAME));
        assertThat(thingListener.things, is(aMapWithSize(1)));
        assertThat(thingListener.things, hasKey("ntp:ntp:local"));
        assertThat(thingProvider.getAllFromModel(MODEL_NAME), hasSize(1));
        Collection<Thing> things = thingProvider.getAll();
        assertThat(things, hasSize(1));
        Thing thing = things.iterator().next();
        assertFalse(thing instanceof Bridge);
        assertEquals(NTP_THING_UID, thing.getUID());
        assertNull(thing.getBridgeUID());
        assertEquals(NTP_THING_TYPE_UID, thing.getThingTypeUID());
        assertEquals(NTP_THING_TYPE_LABEL, thing.getLabel());
        assertNull(thing.getLocation());
        assertEquals(NTP_THING_TYPE_EQUIPMENT_TAG, thing.getSemanticEquipmentTag());
        assertEquals(0, thing.getProperties().size());

        // 4 parameters injected with default value
        assertThat(thing.getConfiguration().keySet(),
                containsInAnyOrder("hostname", "refreshInterval", "refreshNtp", "serverPort"));
        // default value injected for parameter hostname
        assertEquals(DEFAULT_HOSTNAME, thing.getConfiguration().get("hostname"));
        // default value injected for parameter refreshInterval
        assertEquals(BigDecimal.valueOf(DEFAULT_REFRESH_INTERVAL), thing.getConfiguration().get("refreshInterval"));
        // default value injected for parameter refreshNtp
        assertEquals(BigDecimal.valueOf(DEFAULT_REFRESH_NTP), thing.getConfiguration().get("refreshNtp"));
        // default value injected for parameter serverPort
        assertEquals(BigDecimal.valueOf(DEFAULT_SERVER_PORT), thing.getConfiguration().get("serverPort"));

        assertEquals(2, thing.getChannels().size());
        Iterator<Channel> it = thing.getChannels().iterator();
        Channel channel = it.next();
        assertEquals(new ChannelUID(NTP_THING_UID, "string"), channel.getUID());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_UID, channel.getChannelTypeUID());
        assertEquals(ChannelKind.STATE, channel.getKind());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_ITEM_TYPE, channel.getAcceptedItemType());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_LABEL, channel.getLabel());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_DESCRIPTION, channel.getDescription());
        assertNull(channel.getAutoUpdatePolicy());
        assertThat(channel.getDefaultTags(), containsInAnyOrder(TAG_STATUS, TAG_TIMESTAMP));
        assertEquals(0, channel.getProperties().size());
        // Parameter DateTimeFormat injected with default value
        assertThat(channel.getConfiguration().keySet(), contains("DateTimeFormat"));
        assertEquals(DEFAULT_DATE_TIME_FORMAT, channel.getConfiguration().get("DateTimeFormat"));
        channel = it.next();
        assertEquals(new ChannelUID(NTP_THING_UID, "date-only-string"), channel.getUID());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_UID, channel.getChannelTypeUID());
        assertEquals(ChannelKind.STATE, channel.getKind());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_ITEM_TYPE, channel.getAcceptedItemType());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_LABEL, channel.getLabel());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_DESCRIPTION, channel.getDescription());
        assertNull(channel.getAutoUpdatePolicy());
        assertThat(channel.getDefaultTags(), hasSize(0));
        assertEquals(0, channel.getProperties().size());
        // Parameter DateTimeFormat injected with default value
        assertThat(channel.getConfiguration().keySet(), contains("DateTimeFormat"));
        assertEquals(DEFAULT_DATE_TIME_FORMAT, channel.getConfiguration().get("DateTimeFormat"));
    }

    @Test
    public void testCreateIsolatedModelWithThing() throws IOException {
        Files.copy(SOURCE_PATH.resolve("thing.yaml"), fullModelPath);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        try (FileInputStream inputStream = new FileInputStream(fullModelPath.toFile())) {
            String name = modelRepository.createIsolatedModel(inputStream, errors, warnings);
            assertNotNull(name);
            assertEquals(0, errors.size());
            assertEquals(0, warnings.size());

            assertTrue(YamlModelUtils.isIsolatedModel(name));
            assertThat(thingListener.things, is(aMapWithSize(0)));
            assertThat(thingProvider.getAll(), hasSize(0)); // No thing for the registry
            Collection<Thing> things = thingProvider.getAllFromModel(name);
            assertThat(things, hasSize(1));
            Thing thing = things.iterator().next();
            assertFalse(thing instanceof Bridge);
            assertEquals(NTP_THING_UID, thing.getUID());
            assertNull(thing.getBridgeUID());
            assertEquals(NTP_THING_TYPE_UID, thing.getThingTypeUID());
            assertEquals("NTP Local Server", thing.getLabel());
            assertEquals("Paris", thing.getLocation());
            assertEquals(NTP_THING_TYPE_EQUIPMENT_TAG, thing.getSemanticEquipmentTag());
            assertEquals(0, thing.getProperties().size());

            // No parameter injected
            assertThat(thing.getConfiguration().keySet(),
                    containsInAnyOrder("hostname", "serverPort", "timeZone", "other"));
            assertEquals("0.fr.pool.ntp.org", thing.getConfiguration().get("hostname"));
            assertEquals(BigDecimal.valueOf(123), thing.getConfiguration().get("serverPort"));
            assertEquals("Europe/Paris", thing.getConfiguration().get("timeZone"));
            assertEquals("A parameter that is not in the thing config description.",
                    thing.getConfiguration().get("other"));

            assertEquals(2, thing.getChannels().size());
            Iterator<Channel> it = thing.getChannels().iterator();
            Channel channel = it.next();
            assertEquals(new ChannelUID(NTP_THING_UID, "string"), channel.getUID());
            assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_UID, channel.getChannelTypeUID());
            assertEquals(ChannelKind.STATE, channel.getKind());
            assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_ITEM_TYPE, channel.getAcceptedItemType());
            // label in YAML is ignored for a channel provided by the thing type
            assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_LABEL, channel.getLabel());
            // description in YAML is ignored for a channel provided by the thing type
            assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_DESCRIPTION, channel.getDescription());
            assertNull(channel.getAutoUpdatePolicy());
            assertThat(channel.getDefaultTags(), containsInAnyOrder(TAG_STATUS, TAG_TIMESTAMP));
            assertEquals(0, channel.getProperties().size());
            assertThat(channel.getConfiguration().keySet(), containsInAnyOrder("DateTimeFormat", "other"));
            assertEquals("dd-MM-yyyy HH:mm", channel.getConfiguration().get("DateTimeFormat"));
            assertEquals("A parameter that is not in the channel config description.",
                    channel.getConfiguration().get("other"));
            channel = it.next();
            assertEquals(new ChannelUID(NTP_THING_UID, "date-only-string"), channel.getUID());
            assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_UID, channel.getChannelTypeUID());
            assertEquals(ChannelKind.STATE, channel.getKind());
            assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_ITEM_TYPE, channel.getAcceptedItemType());
            assertEquals("Date Only", channel.getLabel());
            assertEquals("Format with date only.", channel.getDescription());
            assertNull(channel.getAutoUpdatePolicy());
            assertThat(channel.getDefaultTags(), hasSize(0));
            assertEquals(0, channel.getProperties().size());
            assertThat(channel.getConfiguration().keySet(), containsInAnyOrder("DateTimeFormat", "other"));
            assertEquals("dd-MM-yyyy", channel.getConfiguration().get("DateTimeFormat"));
            assertEquals("A parameter that is not in the channel config description.",
                    channel.getConfiguration().get("other"));
        }
    }

    @Test
    public void testCreateIsolatedModelWithThingEmptyConfig() throws IOException {
        Files.copy(SOURCE_PATH.resolve("thingWithEmptyConfig.yaml"), fullModelPath);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        try (FileInputStream inputStream = new FileInputStream(fullModelPath.toFile())) {
            String name = modelRepository.createIsolatedModel(inputStream, errors, warnings);
            assertNotNull(name);
            assertEquals(0, errors.size());
            assertEquals(0, warnings.size());

            assertTrue(YamlModelUtils.isIsolatedModel(name));
            assertThat(thingListener.things, is(aMapWithSize(0)));
            assertThat(thingProvider.getAll(), hasSize(0)); // No thing for the registry
            Collection<Thing> things = thingProvider.getAllFromModel(name);
            assertThat(things, hasSize(1));
            Thing thing = things.iterator().next();
            assertFalse(thing instanceof Bridge);
            assertEquals(NTP_THING_UID, thing.getUID());
            assertNull(thing.getBridgeUID());
            assertEquals(NTP_THING_TYPE_UID, thing.getThingTypeUID());
            assertEquals(NTP_THING_TYPE_LABEL, thing.getLabel());
            assertNull(thing.getLocation());
            assertEquals(NTP_THING_TYPE_EQUIPMENT_TAG, thing.getSemanticEquipmentTag());
            assertEquals(0, thing.getProperties().size());

            // No parameter injected
            assertThat(thing.getConfiguration().keySet(), hasSize(0));

            assertEquals(2, thing.getChannels().size());
            Iterator<Channel> it = thing.getChannels().iterator();
            Channel channel = it.next();
            assertEquals(new ChannelUID(NTP_THING_UID, "string"), channel.getUID());
            assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_UID, channel.getChannelTypeUID());
            assertEquals(ChannelKind.STATE, channel.getKind());
            assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_ITEM_TYPE, channel.getAcceptedItemType());
            assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_LABEL, channel.getLabel());
            assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_DESCRIPTION, channel.getDescription());
            assertNull(channel.getAutoUpdatePolicy());
            assertThat(channel.getDefaultTags(), containsInAnyOrder(TAG_STATUS, TAG_TIMESTAMP));
            assertEquals(0, channel.getProperties().size());
            // default value not injected for parameter DateTimeFormat
            assertThat(channel.getConfiguration().keySet(), hasSize(0));
            channel = it.next();
            assertEquals(new ChannelUID(NTP_THING_UID, "date-only-string"), channel.getUID());
            assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_UID, channel.getChannelTypeUID());
            assertEquals(ChannelKind.STATE, channel.getKind());
            assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_ITEM_TYPE, channel.getAcceptedItemType());
            assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_LABEL, channel.getLabel());
            assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_DESCRIPTION, channel.getDescription());
            assertNull(channel.getAutoUpdatePolicy());
            assertThat(channel.getDefaultTags(), hasSize(0));
            assertEquals(0, channel.getProperties().size());
            // default value not injected for parameter DateTimeFormat
            assertThat(channel.getConfiguration().keySet(), hasSize(0));
        }
    }

    @Test
    public void testConversionForConfigWithTextParam() throws IOException {
        Files.copy(SOURCE_PATH.resolve("thingWithNumberInTextParam.yaml"), fullModelPath);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);

        Collection<Thing> things = thingProvider.getAllFromModel(MODEL_NAME);
        assertThat(things, hasSize(1));
        Thing thing = things.iterator().next();
        assertEquals(NTP_THING_UID, thing.getUID());

        assertThat(thing.getConfiguration().keySet(),
                containsInAnyOrder("hostname", "timeZone", "refreshInterval", "refreshNtp", "serverPort"));
        assertEquals("12345", thing.getConfiguration().get("hostname"));
        assertEquals("12.3", thing.getConfiguration().get("timeZone"));

        assertEquals(2, thing.getChannels().size());
        Iterator<Channel> it = thing.getChannels().iterator();
        Channel channel = it.next();
        assertEquals(new ChannelUID(NTP_THING_UID, "string"), channel.getUID());
        assertThat(channel.getConfiguration().keySet(), contains("DateTimeFormat"));
        assertEquals("100", channel.getConfiguration().get("DateTimeFormat"));
        channel = it.next();
        assertEquals(new ChannelUID(NTP_THING_UID, "date-only-string"), channel.getUID());
        assertThat(channel.getConfiguration().keySet(), contains("DateTimeFormat"));
        assertEquals("123.45", channel.getConfiguration().get("DateTimeFormat"));
    }

    private class NtpThingHandlerFactory implements ThingHandlerFactory {
        @Override
        public boolean supportsThingType(ThingTypeUID thingTypeUID) {
            return NTP_THING_TYPE_UID.equals(thingTypeUID);
        }

        @Override
        public ThingHandler registerHandler(Thing thing) {
            return new NtpThingHandler(thing);
        }

        @Override
        public void unregisterHandler(Thing thing) {
        }

        @Override
        public @Nullable Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration,
                @Nullable ThingUID thingUID, @Nullable ThingUID bridgeUID) {
            ThingUID effectiveUID = thingUID != null ? thingUID : ThingFactory.generateRandomThingUID(thingTypeUID);
            ThingType thingType = thingTypeRegistry.getThingType(thingTypeUID);
            if (thingType != null) {
                ThingFactoryHelper.applyDefaultConfiguration(configuration, thingType, configDescriptionRegistry);

                List<Channel> channels = new ArrayList<>();
                List<ChannelDefinition> channelDefinitions = thingType.getChannelDefinitions();
                for (ChannelDefinition channelDefinition : channelDefinitions) {
                    Channel channel = createChannel(channelDefinition, effectiveUID, configDescriptionRegistry);
                    if (channel != null) {
                        channels.add(channel);
                    }
                }

                return ThingBuilder.create(thingTypeUID, effectiveUID).withConfiguration(configuration)
                        .withChannels(channels).withProperties(thingType.getProperties()).withBridge(bridgeUID)
                        .withSemanticEquipmentTag(thingType.getSemanticEquipmentTag()).build();
            } else {
                return null;
            }
        }

        @Override
        public void removeThing(ThingUID thingUID) {
        }

        private @Nullable Channel createChannel(ChannelDefinition channelDefinition, ThingUID thingUID,
                ConfigDescriptionRegistry configDescriptionRegistry) {
            final ChannelUID channelUID = new ChannelUID(thingUID, channelDefinition.getId());
            final ChannelBuilder channelBuilder = createChannelBuilder(channelUID, channelDefinition,
                    configDescriptionRegistry);
            if (channelBuilder == null) {
                return null;
            }
            return channelBuilder.withProperties(channelDefinition.getProperties()).build();
        }

        private @Nullable ChannelBuilder createChannelBuilder(ChannelUID channelUID,
                ChannelDefinition channelDefinition, ConfigDescriptionRegistry configDescriptionRegistry) {
            ChannelType channelType = channelTypeRegistry.getChannelType(channelDefinition.getChannelTypeUID());
            if (channelType == null) {
                return null;
            }

            String label = channelDefinition.getLabel();
            if (label == null) {
                label = channelType.getLabel();
            }

            AutoUpdatePolicy autoUpdatePolicy = channelDefinition.getAutoUpdatePolicy();
            if (autoUpdatePolicy == null) {
                autoUpdatePolicy = channelType.getAutoUpdatePolicy();
            }

            final ChannelBuilder channelBuilder = ChannelBuilder.create(channelUID, channelType.getItemType()) //
                    .withType(channelType.getUID()) //
                    .withDefaultTags(channelType.getTags()) //
                    .withKind(channelType.getKind()) //
                    .withLabel(label) //
                    .withAutoUpdatePolicy(autoUpdatePolicy);

            String description = channelDefinition.getDescription();
            if (description == null) {
                description = channelType.getDescription();
            }
            if (description != null) {
                channelBuilder.withDescription(description);
            }

            // Initialize channel configuration with default-values
            URI uri = channelType.getConfigDescriptionURI();
            if (uri != null) {
                final Configuration configuration = new Configuration();
                ConfigUtil.applyDefaultConfiguration(configuration,
                        configDescriptionRegistry.getConfigDescription(uri));
                channelBuilder.withConfiguration(configuration);
            }

            return channelBuilder;
        }
    }

    private static class NtpThingHandler implements ThingHandler {
        Thing thing;

        NtpThingHandler(Thing thing) {
            this.thing = thing;
        }

        @Override
        public Thing getThing() {
            return thing;
        }

        @Override
        public void initialize() {
        }

        @Override
        public void dispose() {
        }

        @Override
        public void setCallback(@Nullable ThingHandlerCallback thingHandlerCallback) {
        }

        @Override
        public void handleCommand(ChannelUID channelUID, Command command) {
        }

        @Override
        public void handleConfigurationUpdate(Map<String, Object> configurationParameters) {
        }

        @Override
        public void thingUpdated(Thing thing) {
        }

        @Override
        public void channelLinked(ChannelUID channelUID) {
        }

        @Override
        public void channelUnlinked(ChannelUID channelUID) {
        }

        @Override
        public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        }

        @Override
        public void handleRemoval() {
        }
    }

    private static class TestThingChangeListener implements ProviderChangeListener<Thing> {

        public final Map<String, Thing> things = new HashMap<>();

        @Override
        public void added(Provider<Thing> provider, Thing element) {
            things.put(element.getUID().getAsString(), element);
        }

        @Override
        public void removed(Provider<Thing> provider, Thing element) {
            things.remove(element.getUID().getAsString());
        }

        @Override
        public void updated(Provider<Thing> provider, Thing oldelement, Thing element) {
            things.put(element.getUID().getAsString(), element);
        }
    }
}
