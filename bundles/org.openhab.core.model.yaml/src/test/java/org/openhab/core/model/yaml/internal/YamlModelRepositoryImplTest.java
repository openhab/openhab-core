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
package org.openhab.core.model.yaml.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionBuilder;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.config.core.ConfigDescriptionParameterBuilder;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.ConfigUtil;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.model.yaml.YamlModelListener;
import org.openhab.core.model.yaml.internal.things.YamlThingProvider;
import org.openhab.core.model.yaml.test.FirstTypeDTO;
import org.openhab.core.model.yaml.test.SecondTypeDTO;
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
import org.yaml.snakeyaml.Yaml;

/**
 * The {@link YamlModelRepositoryImplTest} contains tests for the {@link YamlModelRepositoryImpl} class.
 *
 * @author Jan N. Klug - Initial contribution
 * @author Laurent Garnier - Extended tests to cover version 2
 * @author Laurent Garnier - Added one test for version management
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class YamlModelRepositoryImplTest {
    private static final Path SOURCE_PATH = Path.of("src/test/resources/model");
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

    private static final String TAG_STATUS = "Status";
    private static final String TAG_TIMESTAMP = "Timestamp";

    private static final ChannelTypeUID NTP_CHANNEL_TYPE_DATETIME_CHANNEL_UID = new ChannelTypeUID("ntp",
            "dateTime-channel");
    private static final String NTP_CHANNEL_TYPE_DATETIME_CHANNEL_LABEL = "Date";
    private static final String NTP_CHANNEL_TYPE_DATETIME_CHANNEL_DESCRIPTION = "NTP refreshed date and time.";
    private static final String NTP_CHANNEL_TYPE_DATETIME_CHANNEL_ITEM_TYPE = "DateTime";
    private static final String NTP_CHANNEL_TYPE_DATETIME_CHANNEL_CATEGORY = "Time";
    private static final Set<String> NTP_CHANNEL_TYPE_DATETIME_CHANNEL_TAGS = Set.of(TAG_STATUS, TAG_TIMESTAMP);

    private static final ChannelTypeUID NTP_CHANNEL_TYPE_STRING_CHANNEL_UID = new ChannelTypeUID("ntp",
            "string-channel");
    private static final String NTP_CHANNEL_TYPE_STRING_CHANNEL_LABEL = "Date";
    private static final String NTP_CHANNEL_TYPE_STRING_CHANNEL_DESCRIPTION = "NTP refreshed date and time.";
    private static final String NTP_CHANNEL_TYPE_STRING_CHANNEL_ITEM_TYPE = "String";
    private static final String NTP_CHANNEL_TYPE_STRING_CHANNEL_CATEGORY = "Time";
    private static final Set<String> NTP_CHANNEL_TYPE_STRING_CHANNEL_TAGS = Set.of(TAG_STATUS, TAG_TIMESTAMP);

    private static final String NTP_CHANNEL_DATETIME_ID = "dateTime";
    private static final String NTP_CHANNEL_STRING_ID = "string";
    private static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss z";

    private @Mock @NonNullByDefault({}) WatchService watchServiceMock;
    private @TempDir @NonNullByDefault({}) Path watchPath;
    private @NonNullByDefault({}) Path fullModelPath;

    private @Mock @NonNullByDefault({}) YamlModelListener<@NonNull FirstTypeDTO> firstTypeListener;
    private @Mock @NonNullByDefault({}) YamlModelListener<@NonNull SecondTypeDTO> secondTypeListener1;
    private @Mock @NonNullByDefault({}) YamlModelListener<@NonNull SecondTypeDTO> secondTypeListener2;

    private @Captor @NonNullByDefault({}) ArgumentCaptor<Collection<FirstTypeDTO>> firstTypeCaptor;
    private @Captor @NonNullByDefault({}) ArgumentCaptor<Collection<SecondTypeDTO>> secondTypeCaptor1;
    private @Captor @NonNullByDefault({}) ArgumentCaptor<Collection<SecondTypeDTO>> secondTypeCaptor2;

    private @Mock @NonNullByDefault({}) BundleResolver bundleResolver;
    private @Mock @NonNullByDefault({}) ThingTypeRegistry thingTypeRegistry;
    private @Mock @NonNullByDefault({}) ChannelTypeRegistry channelTypeRegistry;
    private @Mock @NonNullByDefault({}) ConfigDescriptionRegistry configDescriptionRegistry;
    private @Mock @NonNullByDefault({}) LocaleProvider localeProvider;

    private @Mock @NonNullByDefault({}) Bundle bundle;

    private @NonNullByDefault({}) YamlThingProvider thingProvider;

    @BeforeEach
    public void setup() {
        fullModelPath = watchPath.resolve(MODEL_PATH);
        when(watchServiceMock.getWatchPath()).thenReturn(watchPath);

        when(firstTypeListener.getElementClass()).thenReturn(FirstTypeDTO.class);
        when(firstTypeListener.isVersionSupported(anyInt())).thenReturn(true);
        when(firstTypeListener.isDeprecated()).thenReturn(false);
        when(secondTypeListener1.getElementClass()).thenReturn(SecondTypeDTO.class);
        when(secondTypeListener1.isVersionSupported(anyInt())).thenReturn(true);
        when(secondTypeListener1.isDeprecated()).thenReturn(false);
        when(secondTypeListener2.getElementClass()).thenReturn(SecondTypeDTO.class);
        when(secondTypeListener2.isVersionSupported(anyInt())).thenReturn(true);
        when(secondTypeListener2.isDeprecated()).thenReturn(false);

        when(bundleResolver.resolveBundle(any())).thenReturn(bundle);
        when(bundle.getSymbolicName()).thenReturn("org.openhab.binding.ntp");

        URI uriThingType = null;
        URI uriChannelType1 = null;
        URI uriChannelType2 = null;
        URI uriThing1 = null;
        URI uriChannel1 = null;
        URI uriChannel2 = null;
        URI uriChannel3 = null;
        URI uriChannel4 = null;
        try {
            uriThingType = new URI("thing-type:" + NTP_THING_TYPE_UID.getAsString());
            uriChannelType1 = new URI("channel-type:" + NTP_CHANNEL_TYPE_DATETIME_CHANNEL_UID.getAsString());
            uriChannelType2 = new URI("channel-type:" + NTP_CHANNEL_TYPE_STRING_CHANNEL_UID.getAsString());
            uriThing1 = new URI("thing:ntp:ntp:paris");
            uriChannel1 = new URI("channel:ntp:ntp:paris:dateTime");
            uriChannel2 = new URI("channel:ntp:ntp:paris:string");
            uriChannel3 = new URI("channel:ntp:ntp:paris:string2");
            uriChannel4 = new URI("channel:ntp:ntp:paris:string3");
        } catch (URISyntaxException e) {
        }
        assertNotNull(uriThingType);
        assertNotNull(uriChannelType1);
        assertNotNull(uriChannelType2);
        assertNotNull(uriThing1);
        assertNotNull(uriChannel1);
        assertNotNull(uriChannel2);
        assertNotNull(uriChannel3);
        assertNotNull(uriChannel4);

        List<ChannelDefinition> channelDefinitions = new ArrayList<>();
        channelDefinitions.add(
                new ChannelDefinitionBuilder(NTP_CHANNEL_DATETIME_ID, NTP_CHANNEL_TYPE_DATETIME_CHANNEL_UID).build());
        channelDefinitions
                .add(new ChannelDefinitionBuilder(NTP_CHANNEL_STRING_ID, NTP_CHANNEL_TYPE_STRING_CHANNEL_UID).build());
        ThingType ntpThingType = ThingTypeBuilder.instance(NTP_THING_TYPE_UID, NTP_THING_TYPE_LABEL)
                .withDescription(NTP_THING_TYPE_DESCRIPTION).withConfigDescriptionURI(uriThingType)
                .withSemanticEquipmentTag(NTP_THING_TYPE_EQUIPMENT_TAG).withChannelDefinitions(channelDefinitions)
                .build();

        when(thingTypeRegistry.getThingType(eq(NTP_THING_TYPE_UID))).thenReturn(ntpThingType);
        when(thingTypeRegistry.getThingType(eq(NTP_THING_TYPE_UID), eq(Locale.FRENCH))).thenReturn(ntpThingType);

        ChannelType channelType1 = ChannelTypeBuilder
                .state(NTP_CHANNEL_TYPE_DATETIME_CHANNEL_UID, NTP_CHANNEL_TYPE_DATETIME_CHANNEL_LABEL,
                        NTP_CHANNEL_TYPE_DATETIME_CHANNEL_ITEM_TYPE)
                .withDescription(NTP_CHANNEL_TYPE_DATETIME_CHANNEL_DESCRIPTION)
                .withCategory(NTP_CHANNEL_TYPE_DATETIME_CHANNEL_CATEGORY).withConfigDescriptionURI(uriChannelType1)
                .withTags(NTP_CHANNEL_TYPE_DATETIME_CHANNEL_TAGS).build();

        when(channelTypeRegistry.getChannelType(eq(NTP_CHANNEL_TYPE_DATETIME_CHANNEL_UID))).thenReturn(channelType1);
        when(channelTypeRegistry.getChannelType(eq(NTP_CHANNEL_TYPE_DATETIME_CHANNEL_UID), eq(Locale.FRENCH)))
                .thenReturn(channelType1);

        ChannelType channelType2 = ChannelTypeBuilder
                .state(NTP_CHANNEL_TYPE_STRING_CHANNEL_UID, NTP_CHANNEL_TYPE_STRING_CHANNEL_LABEL,
                        NTP_CHANNEL_TYPE_STRING_CHANNEL_ITEM_TYPE)
                .withDescription(NTP_CHANNEL_TYPE_STRING_CHANNEL_DESCRIPTION)
                .withCategory(NTP_CHANNEL_TYPE_STRING_CHANNEL_CATEGORY).withConfigDescriptionURI(uriChannelType2)
                .withTags(NTP_CHANNEL_TYPE_STRING_CHANNEL_TAGS).build();

        when(channelTypeRegistry.getChannelType(eq(NTP_CHANNEL_TYPE_STRING_CHANNEL_UID))).thenReturn(channelType2);
        when(channelTypeRegistry.getChannelType(eq(NTP_CHANNEL_TYPE_STRING_CHANNEL_UID), eq(Locale.FRENCH)))
                .thenReturn(channelType2);

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

        ConfigDescription configDescrChannel1 = ConfigDescriptionBuilder.create(uriChannelType1).build();
        when(configDescriptionRegistry.getConfigDescription(eq(uriChannelType1))).thenReturn(configDescrChannel1);

        ConfigDescriptionParameter param = ConfigDescriptionParameterBuilder.create("DateTimeFormat", Type.TEXT)
                .withRequired(false).withLabel("Date Time Format").withDescription("Formatting of the date and time.")
                .withDefault(DEFAULT_DATE_TIME_FORMAT).build();
        ConfigDescription configDescrChannel2 = ConfigDescriptionBuilder.create(uriChannelType2).withParameter(param)
                .build();
        when(configDescriptionRegistry.getConfigDescription(eq(uriChannelType2))).thenReturn(configDescrChannel2);

        when(configDescriptionRegistry.getConfigDescription(eq(uriThing1))).thenReturn(null);
        when(configDescriptionRegistry.getConfigDescription(eq(uriChannel1))).thenReturn(null);
        when(configDescriptionRegistry.getConfigDescription(eq(uriChannel2))).thenReturn(null);
        when(configDescriptionRegistry.getConfigDescription(eq(uriChannel3))).thenReturn(null);
        when(configDescriptionRegistry.getConfigDescription(eq(uriChannel4))).thenReturn(null);

        when(localeProvider.getLocale()).thenReturn(Locale.FRENCH);

        NtpThingHandlerFactory thingHandlerFactory = new NtpThingHandlerFactory();
        thingProvider = new YamlThingProvider(bundleResolver, thingTypeRegistry, channelTypeRegistry,
                configDescriptionRegistry, localeProvider);
        thingProvider.onReadyMarkerAdded(new ReadyMarker(XML_THING_TYPE, NTP_BUNDLE_SYMBOLIC_NAME));
        thingProvider.addThingHandlerFactory(thingHandlerFactory);
    }

    @Test
    public void testFileAddedAfterListeners() throws IOException {
        Files.copy(SOURCE_PATH.resolve("modelFileAddedOrRemoved.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(firstTypeListener);
        modelRepository.addYamlModelListener(secondTypeListener1);
        modelRepository.addYamlModelListener(secondTypeListener2);

        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);

        verify(firstTypeListener).addedModel(eq(MODEL_NAME), firstTypeCaptor.capture());
        verify(firstTypeListener, never()).updatedModel(any(), any());
        verify(firstTypeListener, never()).removedModel(any(), any());
        verify(secondTypeListener1).addedModel(eq(MODEL_NAME), secondTypeCaptor1.capture());
        verify(secondTypeListener1, never()).updatedModel(any(), any());
        verify(secondTypeListener1, never()).removedModel(any(), any());
        verify(secondTypeListener2).addedModel(eq(MODEL_NAME), secondTypeCaptor2.capture());
        verify(secondTypeListener2, never()).updatedModel(any(), any());
        verify(secondTypeListener2, never()).removedModel(any(), any());

        List<Collection<FirstTypeDTO>> firstTypeCaptorValues = firstTypeCaptor.getAllValues();
        assertThat(firstTypeCaptorValues, hasSize(1));
        List<Collection<SecondTypeDTO>> secondTypeCaptor1Values = secondTypeCaptor1.getAllValues();
        assertThat(secondTypeCaptor1Values, hasSize(1));
        List<Collection<SecondTypeDTO>> secondTypeCaptor2Values = secondTypeCaptor2.getAllValues();
        assertThat(secondTypeCaptor2Values, hasSize(1));

        Collection<FirstTypeDTO> firstTypeElements = firstTypeCaptorValues.getFirst();
        Collection<SecondTypeDTO> secondTypeElements1 = secondTypeCaptor1Values.getFirst();
        Collection<SecondTypeDTO> secondTypeElements2 = secondTypeCaptor2Values.getFirst();

        assertThat(firstTypeElements, hasSize(2));
        assertThat(firstTypeElements,
                containsInAnyOrder(new FirstTypeDTO("First1", "Description1"), new FirstTypeDTO("First2", null)));
        assertThat(secondTypeElements1, hasSize(1));
        assertThat(secondTypeElements1, contains(new SecondTypeDTO("Second1", "Label1")));
        assertThat(secondTypeElements2, hasSize(1));
        assertThat(secondTypeElements2, contains(new SecondTypeDTO("Second1", "Label1")));
    }

    @Test
    public void testFileAddedBeforeListeners() throws IOException {
        Files.copy(SOURCE_PATH.resolve("modelFileAddedOrRemoved.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);

        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);

        modelRepository.addYamlModelListener(firstTypeListener);
        modelRepository.addYamlModelListener(secondTypeListener1);
        modelRepository.addYamlModelListener(secondTypeListener2);

        verify(firstTypeListener).addedModel(eq(MODEL_NAME), firstTypeCaptor.capture());
        verify(firstTypeListener, never()).updatedModel(any(), any());
        verify(firstTypeListener, never()).removedModel(any(), any());
        verify(secondTypeListener1).addedModel(eq(MODEL_NAME), secondTypeCaptor1.capture());
        verify(secondTypeListener1, never()).updatedModel(any(), any());
        verify(secondTypeListener1, never()).removedModel(any(), any());
        verify(secondTypeListener2).addedModel(eq(MODEL_NAME), secondTypeCaptor2.capture());
        verify(secondTypeListener2, never()).updatedModel(any(), any());
        verify(secondTypeListener2, never()).removedModel(any(), any());

        List<Collection<FirstTypeDTO>> firstTypeCaptorValues = firstTypeCaptor.getAllValues();
        assertThat(firstTypeCaptorValues, hasSize(1));
        List<Collection<SecondTypeDTO>> secondTypeCaptor1Values = secondTypeCaptor1.getAllValues();
        assertThat(secondTypeCaptor1Values, hasSize(1));
        List<Collection<SecondTypeDTO>> secondTypeCaptor2Values = secondTypeCaptor2.getAllValues();
        assertThat(secondTypeCaptor2Values, hasSize(1));

        Collection<FirstTypeDTO> firstTypeElements = firstTypeCaptorValues.getFirst();
        Collection<SecondTypeDTO> secondTypeElements1 = secondTypeCaptor1Values.getFirst();
        Collection<SecondTypeDTO> secondTypeElements2 = secondTypeCaptor2Values.getFirst();

        assertThat(firstTypeElements, hasSize(2));
        assertThat(firstTypeElements,
                containsInAnyOrder(new FirstTypeDTO("First1", "Description1"), new FirstTypeDTO("First2", null)));
        assertThat(secondTypeElements1, hasSize(1));
        assertThat(secondTypeElements1, contains(new SecondTypeDTO("Second1", "Label1")));
        assertThat(secondTypeElements2, hasSize(1));
        assertThat(secondTypeElements2, contains(new SecondTypeDTO("Second1", "Label1")));
    }

    @Test
    public void testFileUpdated() throws IOException {
        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(firstTypeListener);

        Files.copy(SOURCE_PATH.resolve("modelFileUpdatePost.yaml"), fullModelPath);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);
        verify(firstTypeListener).addedModel(eq(MODEL_NAME), any());

        Files.copy(SOURCE_PATH.resolve("modelFileUpdatePre.yaml"), fullModelPath, StandardCopyOption.REPLACE_EXISTING);
        modelRepository.processWatchEvent(WatchService.Kind.MODIFY, fullModelPath);
        verify(firstTypeListener, times(2)).addedModel(eq(MODEL_NAME), firstTypeCaptor.capture());
        verify(firstTypeListener).updatedModel(eq(MODEL_NAME), firstTypeCaptor.capture());
        verify(firstTypeListener).removedModel(eq(MODEL_NAME), firstTypeCaptor.capture());

        List<Collection<FirstTypeDTO>> firstTypeCaptorValues = firstTypeCaptor.getAllValues();
        assertThat(firstTypeCaptorValues, hasSize(4));

        // added originally
        assertThat(firstTypeCaptorValues.getFirst(), hasSize(3));
        assertThat(firstTypeCaptorValues.getFirst(), containsInAnyOrder(new FirstTypeDTO("First", "First original"),
                new FirstTypeDTO("Second", "Second original"), new FirstTypeDTO("Third", "Third original")));
        // added by update
        assertThat(firstTypeCaptorValues.get(1), hasSize(1));
        assertThat(firstTypeCaptorValues.get(1), contains(new FirstTypeDTO("Fourth", "Fourth original")));
        // updated by update
        assertThat(firstTypeCaptorValues.get(2), hasSize(1));
        assertThat(firstTypeCaptorValues.get(2), contains(new FirstTypeDTO("Second", "Second modified")));
        // removed by update
        assertThat(firstTypeCaptorValues.get(3), hasSize(1));
        assertThat(firstTypeCaptorValues.get(3), contains(new FirstTypeDTO("Third", "Third original")));
    }

    @ParameterizedTest
    @CsvSource({ //
            "modelFileUpdateRemovedElements.yaml", "modelFileUpdateRenamedElements.yaml",
            "modelFileUpdateRemovedVersion.yaml" //
    })
    public void testFileRemovedElements(String file) throws IOException {
        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(firstTypeListener);

        Files.copy(SOURCE_PATH.resolve("modelFileUpdatePost.yaml"), fullModelPath);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);
        verify(firstTypeListener).addedModel(eq(MODEL_NAME), any());

        Files.copy(SOURCE_PATH.resolve(file), fullModelPath, StandardCopyOption.REPLACE_EXISTING);
        modelRepository.processWatchEvent(WatchService.Kind.MODIFY, fullModelPath);
        verify(firstTypeListener).removedModel(eq(MODEL_NAME), firstTypeCaptor.capture());

        Collection<FirstTypeDTO> firstTypeElements = firstTypeCaptor.getAllValues().getFirst();

        // Check that the elements were removed
        assertThat(firstTypeElements, hasSize(3));
        assertThat(firstTypeElements, containsInAnyOrder(new FirstTypeDTO("First", "First original"),
                new FirstTypeDTO("Second", "Second original"), new FirstTypeDTO("Third", "Third original")));
    }

    @Test
    public void testFileRemoved() throws IOException {
        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);

        modelRepository.addYamlModelListener(firstTypeListener);

        Files.copy(SOURCE_PATH.resolve("modelFileUpdatePost.yaml"), fullModelPath);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);
        modelRepository.processWatchEvent(WatchService.Kind.DELETE, fullModelPath);

        verify(firstTypeListener).addedModel(eq(MODEL_NAME), firstTypeCaptor.capture());
        verify(firstTypeListener, never()).updatedModel(any(), any());
        verify(firstTypeListener).removedModel(eq(MODEL_NAME), firstTypeCaptor.capture());

        List<Collection<FirstTypeDTO>> firstTypeCaptorValues = firstTypeCaptor.getAllValues();
        assertThat(firstTypeCaptorValues, hasSize(2));

        // all are added
        assertThat(firstTypeCaptorValues.getFirst(), hasSize(3));
        assertThat(firstTypeCaptorValues.getFirst(), containsInAnyOrder(new FirstTypeDTO("First", "First original"),
                new FirstTypeDTO("Second", "Second original"), new FirstTypeDTO("Third", "Third original")));
        // all are removed
        assertThat(firstTypeCaptorValues.get(1), hasSize(3));
        assertThat(firstTypeCaptorValues.get(1), containsInAnyOrder(new FirstTypeDTO("First", "First original"),
                new FirstTypeDTO("Second", "Second original"), new FirstTypeDTO("Third", "Third original")));
    }

    @Test
    public void testAddElementToModel() throws IOException {
        Files.copy(SOURCE_PATH.resolve("modifyModelInitialContent.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(firstTypeListener);
        modelRepository.addYamlModelListener(secondTypeListener1);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);

        FirstTypeDTO added = new FirstTypeDTO("element3", "description3");
        modelRepository.addElementToModel(MODEL_NAME, added);
        SecondTypeDTO added2 = new SecondTypeDTO("elt1", "My label");
        modelRepository.addElementToModel(MODEL_NAME, added2);

        verify(firstTypeListener, times(2)).addedModel(eq(MODEL_NAME), firstTypeCaptor.capture());
        verify(firstTypeListener, never()).updatedModel(any(), any());
        verify(firstTypeListener, never()).removedModel(any(), any());
        verify(secondTypeListener1).addedModel(eq(MODEL_NAME), secondTypeCaptor1.capture());
        verify(secondTypeListener1, never()).updatedModel(any(), any());
        verify(secondTypeListener1, never()).removedModel(any(), any());

        String actualFileContent = Files.readString(fullModelPath);
        String expectedFileContent = Files.readString(SOURCE_PATH.resolve("addToModelExpectedContent.yaml"));
        Yaml yaml = new Yaml();

        assertThat(yaml.load(actualFileContent), equalTo(yaml.load(expectedFileContent)));

        List<Collection<FirstTypeDTO>> firstTypeCaptorValues = firstTypeCaptor.getAllValues();
        assertThat(firstTypeCaptorValues, hasSize(2));
        assertThat(firstTypeCaptorValues.get(1), hasSize(1));
        assertThat(firstTypeCaptorValues.get(1), contains(new FirstTypeDTO("element3", "description3")));

        List<Collection<SecondTypeDTO>> secondTypeCaptor1Values = secondTypeCaptor1.getAllValues();
        assertThat(secondTypeCaptor1Values, hasSize(1));
        assertThat(secondTypeCaptor1Values.getFirst(), hasSize(1));
        assertThat(secondTypeCaptor1Values.getFirst(), contains(new SecondTypeDTO("elt1", "My label")));
    }

    @Test
    public void testUpdateElementInModel() throws IOException {
        Files.copy(SOURCE_PATH.resolve("modifyModelInitialContent.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(firstTypeListener);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);

        FirstTypeDTO updated = new FirstTypeDTO("element1", "newDescription1");
        modelRepository.updateElementInModel(MODEL_NAME, updated);

        verify(firstTypeListener).addedModel(eq(MODEL_NAME), any());
        verify(firstTypeListener).updatedModel(eq(MODEL_NAME), firstTypeCaptor.capture());
        verify(firstTypeListener, never()).removedModel(any(), any());

        String actualFileContent = Files.readString(fullModelPath);
        String expectedFileContent = Files.readString(SOURCE_PATH.resolve("updateInModelExpectedContent.yaml"));
        Yaml yaml = new Yaml();

        assertThat(yaml.load(actualFileContent), equalTo(yaml.load(expectedFileContent)));

        List<Collection<FirstTypeDTO>> firstTypeCaptorValues = firstTypeCaptor.getAllValues();
        assertThat(firstTypeCaptorValues, hasSize(1));
        assertThat(firstTypeCaptorValues.getFirst(), hasSize(1));
        assertThat(firstTypeCaptorValues.getFirst(), contains(new FirstTypeDTO("element1", "newDescription1")));
    }

    @Test
    public void testRemoveElementFromModel() throws IOException {
        Files.copy(SOURCE_PATH.resolve("modifyModelInitialContent.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);

        FirstTypeDTO removed = new FirstTypeDTO("element1", "description1");
        modelRepository.addYamlModelListener(firstTypeListener);
        modelRepository.removeElementFromModel(MODEL_NAME, removed);

        verify(firstTypeListener).addedModel(eq(MODEL_NAME), any());
        verify(firstTypeListener, never()).updatedModel(any(), any());
        verify(firstTypeListener).removedModel(eq(MODEL_NAME), firstTypeCaptor.capture());

        String actualFileContent = Files.readString(fullModelPath);
        String expectedFileContent = Files.readString(SOURCE_PATH.resolve("removeFromModelExpectedContent.yaml"));
        Yaml yaml = new Yaml();

        assertThat(yaml.load(actualFileContent), equalTo(yaml.load(expectedFileContent)));

        List<Collection<FirstTypeDTO>> firstTypeCaptorValues = firstTypeCaptor.getAllValues();
        assertThat(firstTypeCaptorValues, hasSize(1));
        assertThat(firstTypeCaptorValues.getFirst(), hasSize(1));
        assertThat(firstTypeCaptorValues.getFirst(), contains(new FirstTypeDTO("element1", "description1")));
    }

    @Test
    public void testReadOnlyModelNotUpdated() throws IOException {
        Files.copy(SOURCE_PATH.resolve("modelFileAddedOrRemoved.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);

        FirstTypeDTO added = new FirstTypeDTO("element3", "description3");
        modelRepository.addElementToModel(MODEL_NAME, added);

        FirstTypeDTO removed = new FirstTypeDTO("element1", "description1");
        modelRepository.removeElementFromModel(MODEL_NAME, removed);

        FirstTypeDTO updated = new FirstTypeDTO("element2", "newDescription2");
        modelRepository.updateElementInModel(MODEL_NAME, updated);

        String actualFileContent = Files.readString(fullModelPath);
        String expectedFileContent = Files.readString(SOURCE_PATH.resolve("modelFileAddedOrRemoved.yaml"));
        Yaml yaml = new Yaml();

        assertThat(yaml.load(actualFileContent), equalTo(yaml.load(expectedFileContent)));
    }

    @Test
    public void testExistingProviderForVersion() throws IOException {
        // Provider supports version 1
        when(firstTypeListener.isVersionSupported(eq(1))).thenReturn(true);
        when(firstTypeListener.isDeprecated()).thenReturn(false);

        Files.copy(SOURCE_PATH.resolve("modelFileAddedOrRemoved.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(firstTypeListener);

        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);

        verify(firstTypeListener).addedModel(eq(MODEL_NAME), firstTypeCaptor.capture());
        verify(firstTypeListener, never()).updatedModel(any(), any());
        verify(firstTypeListener, never()).removedModel(any(), any());

        Collection<FirstTypeDTO> firstTypeElements = firstTypeCaptor.getValue();
        assertThat(firstTypeElements, hasSize(2));
        assertThat(firstTypeElements,
                containsInAnyOrder(new FirstTypeDTO("First1", "Description1"), new FirstTypeDTO("First2", null)));
    }

    @Test
    public void testExistingDeprecatedProviderForVersion() throws IOException {
        // Provider supports version 1 as deprecated
        when(firstTypeListener.isVersionSupported(eq(1))).thenReturn(true);
        when(firstTypeListener.isDeprecated()).thenReturn(true);

        Files.copy(SOURCE_PATH.resolve("modelFileAddedOrRemoved.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(firstTypeListener);

        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);

        verify(firstTypeListener).addedModel(eq(MODEL_NAME), firstTypeCaptor.capture());
        verify(firstTypeListener, never()).updatedModel(any(), any());
        verify(firstTypeListener, never()).removedModel(any(), any());

        Collection<FirstTypeDTO> firstTypeElements = firstTypeCaptor.getValue();
        assertThat(firstTypeElements, hasSize(2));
        assertThat(firstTypeElements,
                containsInAnyOrder(new FirstTypeDTO("First1", "Description1"), new FirstTypeDTO("First2", null)));
    }

    @Test
    public void testNoProviderForVersion() throws IOException {
        // Provider does not support version 1
        when(firstTypeListener.isVersionSupported(eq(1))).thenReturn(false);
        when(firstTypeListener.isDeprecated()).thenReturn(false);

        Files.copy(SOURCE_PATH.resolve("modelFileAddedOrRemoved.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(firstTypeListener);

        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);

        verify(firstTypeListener, never()).addedModel(any(), any());
        verify(firstTypeListener, never()).updatedModel(any(), any());
        verify(firstTypeListener, never()).removedModel(any(), any());
    }

    @Test
    public void testDifferentProvidersDependingOnVersion() throws IOException {
        // secondTypeListener1 supports version 1
        when(secondTypeListener1.isVersionSupported(eq(1))).thenReturn(true);
        when(secondTypeListener1.isDeprecated()).thenReturn(false);
        // secondTypeListener2 does not supports version 1
        when(secondTypeListener2.isVersionSupported(eq(1))).thenReturn(false);
        when(secondTypeListener2.isDeprecated()).thenReturn(false);

        Files.copy(SOURCE_PATH.resolve("modelFileAddedOrRemoved.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(secondTypeListener1);
        modelRepository.addYamlModelListener(secondTypeListener2);

        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);

        verify(secondTypeListener1).addedModel(eq(MODEL_NAME), secondTypeCaptor1.capture());
        verify(secondTypeListener1, never()).updatedModel(any(), any());
        verify(secondTypeListener1, never()).removedModel(any(), any());

        verify(secondTypeListener2, never()).addedModel(any(), any());
        verify(secondTypeListener2, never()).updatedModel(any(), any());
        verify(secondTypeListener2, never()).removedModel(any(), any());

        Collection<SecondTypeDTO> secondTypeElements = secondTypeCaptor1.getValue();
        assertThat(secondTypeElements, hasSize(1));
        assertThat(secondTypeElements, contains(new SecondTypeDTO("Second1", "Label1")));
    }

    @Test
    public void testLoadModelWithThing() throws IOException {
        Files.copy(SOURCE_PATH.resolve("modelWithThing.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(thingProvider);

        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);

        Collection<Thing> things = thingProvider.getAll();
        assertThat(things, hasSize(1));
        Iterator<Thing> it = things.iterator();
        Thing thing = it.next();
        assertFalse(thing instanceof Bridge);
        ThingUID thingUid = new ThingUID("ntp", "ntp", "paris");
        assertEquals(thingUid, thing.getUID());
        assertNull(thing.getBridgeUID());
        assertEquals(NTP_THING_TYPE_UID, thing.getThingTypeUID());
        assertEquals("NTP Paris", thing.getLabel());
        assertEquals("Paris", thing.getLocation());
        assertEquals(NTP_THING_TYPE_EQUIPMENT_TAG, thing.getSemanticEquipmentTag());
        assertEquals(0, thing.getProperties().size());

        // 2 parameters injected with default value
        assertThat(thing.getConfiguration().keySet(), hasSize(5));
        assertThat(thing.getConfiguration().keySet(),
                containsInAnyOrder("hostname", "refreshInterval", "refreshNtp", "serverPort", "timeZone"));
        assertEquals("0.fr.pool.ntp.org", thing.getConfiguration().get("hostname"));
        assertEquals(BigDecimal.valueOf(123), thing.getConfiguration().get("serverPort"));
        assertEquals("Europe/Paris", thing.getConfiguration().get("timeZone"));
        // default value injected for parameter refreshInterval
        assertEquals(BigDecimal.valueOf(DEFAULT_REFRESH_INTERVAL), thing.getConfiguration().get("refreshInterval"));
        // default value injected for parameter refreshNtp
        assertEquals(BigDecimal.valueOf(DEFAULT_REFRESH_NTP), thing.getConfiguration().get("refreshNtp"));

        assertEquals(3, thing.getChannels().size());
        Iterator<Channel> it2 = thing.getChannels().iterator();
        Channel channel = it2.next();
        assertEquals(new ChannelUID(thingUid, "dateTime"), channel.getUID());
        assertEquals(NTP_CHANNEL_TYPE_DATETIME_CHANNEL_UID, channel.getChannelTypeUID());
        assertEquals(ChannelKind.STATE, channel.getKind());
        assertEquals(NTP_CHANNEL_TYPE_DATETIME_CHANNEL_ITEM_TYPE, channel.getAcceptedItemType());
        assertEquals(NTP_CHANNEL_TYPE_DATETIME_CHANNEL_LABEL, channel.getLabel());
        assertEquals(NTP_CHANNEL_TYPE_DATETIME_CHANNEL_DESCRIPTION, channel.getDescription());
        assertNull(channel.getAutoUpdatePolicy());
        assertThat(channel.getDefaultTags(), hasSize(2));
        assertThat(channel.getDefaultTags(), containsInAnyOrder(TAG_STATUS, TAG_TIMESTAMP));
        assertEquals(0, channel.getProperties().size());
        assertThat(channel.getConfiguration().keySet(), hasSize(0));
        channel = it2.next();
        assertEquals(new ChannelUID(thingUid, "string"), channel.getUID());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_UID, channel.getChannelTypeUID());
        assertEquals(ChannelKind.STATE, channel.getKind());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_ITEM_TYPE, channel.getAcceptedItemType());
        // label in YAML is ignored for a channel provided by the channel type
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_LABEL, channel.getLabel());
        // description in YAML is ignored for a channel provided by the channel type
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_DESCRIPTION, channel.getDescription());
        assertNull(channel.getAutoUpdatePolicy());
        assertThat(channel.getDefaultTags(), hasSize(2));
        assertThat(channel.getDefaultTags(), containsInAnyOrder(TAG_STATUS, TAG_TIMESTAMP));
        assertEquals(0, channel.getProperties().size());
        assertThat(channel.getConfiguration().keySet(), hasSize(1));
        assertThat(channel.getConfiguration().keySet(), contains("DateTimeFormat"));
        assertEquals("dd-MM-yyyy HH:mm", channel.getConfiguration().get("DateTimeFormat"));
        channel = it2.next();
        assertEquals(new ChannelUID(thingUid, "string2"), channel.getUID());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_UID, channel.getChannelTypeUID());
        assertEquals(ChannelKind.STATE, channel.getKind());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_ITEM_TYPE, channel.getAcceptedItemType());
        assertEquals("string2 channel", channel.getLabel());
        assertEquals("The string2 channel", channel.getDescription());
        assertNull(channel.getAutoUpdatePolicy());
        assertThat(channel.getDefaultTags(), hasSize(0));
        assertEquals(0, channel.getProperties().size());
        assertThat(channel.getConfiguration().keySet(), hasSize(1));
        assertThat(channel.getConfiguration().keySet(), contains("DateTimeFormat"));
        assertEquals("dd-MM-yyyy", channel.getConfiguration().get("DateTimeFormat"));
    }

    @Test
    public void testLoadModelWithThingEmptyConfig() throws IOException {
        Files.copy(SOURCE_PATH.resolve("modelWithThingEmptyConfig.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(thingProvider);

        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);

        Collection<Thing> things = thingProvider.getAll();
        assertThat(things, hasSize(1));
        Iterator<Thing> it = things.iterator();
        Thing thing = it.next();
        assertFalse(thing instanceof Bridge);
        ThingUID thingUid = new ThingUID("ntp", "ntp", "paris");
        assertEquals(thingUid, thing.getUID());
        assertNull(thing.getBridgeUID());
        assertEquals(NTP_THING_TYPE_UID, thing.getThingTypeUID());
        assertEquals(NTP_THING_TYPE_LABEL, thing.getLabel());
        assertNull(thing.getLocation());
        assertEquals(NTP_THING_TYPE_EQUIPMENT_TAG, thing.getSemanticEquipmentTag());
        assertEquals(0, thing.getProperties().size());

        // 4 parameters injected with default value
        assertThat(thing.getConfiguration().keySet(), hasSize(4));
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

        assertEquals(3, thing.getChannels().size());
        Iterator<Channel> it2 = thing.getChannels().iterator();
        Channel channel = it2.next();
        assertEquals(new ChannelUID(thingUid, "dateTime"), channel.getUID());
        assertEquals(NTP_CHANNEL_TYPE_DATETIME_CHANNEL_UID, channel.getChannelTypeUID());
        assertEquals(ChannelKind.STATE, channel.getKind());
        assertEquals(NTP_CHANNEL_TYPE_DATETIME_CHANNEL_ITEM_TYPE, channel.getAcceptedItemType());
        assertEquals(NTP_CHANNEL_TYPE_DATETIME_CHANNEL_LABEL, channel.getLabel());
        assertEquals(NTP_CHANNEL_TYPE_DATETIME_CHANNEL_DESCRIPTION, channel.getDescription());
        assertNull(channel.getAutoUpdatePolicy());
        assertThat(channel.getDefaultTags(), hasSize(2));
        assertThat(channel.getDefaultTags(), containsInAnyOrder(TAG_STATUS, TAG_TIMESTAMP));
        assertEquals(0, channel.getProperties().size());
        assertThat(channel.getConfiguration().keySet(), hasSize(0));
        channel = it2.next();
        assertEquals(new ChannelUID(thingUid, "string"), channel.getUID());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_UID, channel.getChannelTypeUID());
        assertEquals(ChannelKind.STATE, channel.getKind());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_ITEM_TYPE, channel.getAcceptedItemType());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_LABEL, channel.getLabel());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_DESCRIPTION, channel.getDescription());
        assertNull(channel.getAutoUpdatePolicy());
        assertThat(channel.getDefaultTags(), hasSize(2));
        assertThat(channel.getDefaultTags(), containsInAnyOrder(TAG_STATUS, TAG_TIMESTAMP));
        assertEquals(0, channel.getProperties().size());
        // Parameter DateTimeFormat injected with default value
        assertThat(channel.getConfiguration().keySet(), hasSize(1));
        assertThat(channel.getConfiguration().keySet(), contains("DateTimeFormat"));
        assertEquals(DEFAULT_DATE_TIME_FORMAT, channel.getConfiguration().get("DateTimeFormat"));
        channel = it2.next();
        assertEquals(new ChannelUID(thingUid, "string2"), channel.getUID());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_UID, channel.getChannelTypeUID());
        assertEquals(ChannelKind.STATE, channel.getKind());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_ITEM_TYPE, channel.getAcceptedItemType());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_LABEL, channel.getLabel());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_DESCRIPTION, channel.getDescription());
        assertNull(channel.getAutoUpdatePolicy());
        assertThat(channel.getDefaultTags(), hasSize(0));
        assertEquals(0, channel.getProperties().size());
        // Parameter DateTimeFormat injected with default value
        assertThat(channel.getConfiguration().keySet(), hasSize(1));
        assertThat(channel.getConfiguration().keySet(), contains("DateTimeFormat"));
        assertEquals(DEFAULT_DATE_TIME_FORMAT, channel.getConfiguration().get("DateTimeFormat"));
    }

    @Test
    public void testLoadModelWithThingUnexpectedConfigParam() throws IOException {
        Files.copy(SOURCE_PATH.resolve("modelWithThingUnexpectedConfigParam.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(thingProvider);

        modelRepository.processWatchEvent(WatchService.Kind.CREATE, fullModelPath);

        Collection<Thing> things = thingProvider.getAll();
        assertThat(things, hasSize(1));
        Iterator<Thing> it = things.iterator();
        Thing thing = it.next();
        assertFalse(thing instanceof Bridge);
        ThingUID thingUid = new ThingUID("ntp", "ntp", "paris");
        assertEquals(thingUid, thing.getUID());
        assertNull(thing.getBridgeUID());
        assertEquals(NTP_THING_TYPE_UID, thing.getThingTypeUID());
        assertEquals(NTP_THING_TYPE_LABEL, thing.getLabel());
        assertNull(thing.getLocation());
        assertEquals(NTP_THING_TYPE_EQUIPMENT_TAG, thing.getSemanticEquipmentTag());
        assertEquals(0, thing.getProperties().size());

        // 4 parameters injected with default value
        assertThat(thing.getConfiguration().keySet(), hasSize(5));
        assertThat(thing.getConfiguration().keySet(),
                containsInAnyOrder("hostname", "refreshInterval", "refreshNtp", "serverPort", "other"));
        assertEquals("A parameter that is not in the thing config description.", thing.getConfiguration().get("other"));
        // default value injected for parameter hostname
        assertEquals(DEFAULT_HOSTNAME, thing.getConfiguration().get("hostname"));
        // default value injected for parameter refreshInterval
        assertEquals(BigDecimal.valueOf(DEFAULT_REFRESH_INTERVAL), thing.getConfiguration().get("refreshInterval"));
        // default value injected for parameter refreshNtp
        assertEquals(BigDecimal.valueOf(DEFAULT_REFRESH_NTP), thing.getConfiguration().get("refreshNtp"));
        // default value injected for parameter serverPort
        assertEquals(BigDecimal.valueOf(DEFAULT_SERVER_PORT), thing.getConfiguration().get("serverPort"));

        assertEquals(3, thing.getChannels().size());
        Iterator<Channel> it2 = thing.getChannels().iterator();
        Channel channel = it2.next();
        assertEquals(new ChannelUID(thingUid, "dateTime"), channel.getUID());
        assertEquals(NTP_CHANNEL_TYPE_DATETIME_CHANNEL_UID, channel.getChannelTypeUID());
        assertEquals(ChannelKind.STATE, channel.getKind());
        assertEquals(NTP_CHANNEL_TYPE_DATETIME_CHANNEL_ITEM_TYPE, channel.getAcceptedItemType());
        assertEquals(NTP_CHANNEL_TYPE_DATETIME_CHANNEL_LABEL, channel.getLabel());
        assertEquals(NTP_CHANNEL_TYPE_DATETIME_CHANNEL_DESCRIPTION, channel.getDescription());
        assertNull(channel.getAutoUpdatePolicy());
        assertThat(channel.getDefaultTags(), hasSize(2));
        assertThat(channel.getDefaultTags(), containsInAnyOrder(TAG_STATUS, TAG_TIMESTAMP));
        assertEquals(0, channel.getProperties().size());
        assertThat(channel.getConfiguration().keySet(), hasSize(1));
        assertThat(channel.getConfiguration().keySet(), contains("other"));
        assertEquals("A parameter that is not in the channel config description.",
                channel.getConfiguration().get("other"));
        channel = it2.next();
        assertEquals(new ChannelUID(thingUid, "string"), channel.getUID());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_UID, channel.getChannelTypeUID());
        assertEquals(ChannelKind.STATE, channel.getKind());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_ITEM_TYPE, channel.getAcceptedItemType());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_LABEL, channel.getLabel());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_DESCRIPTION, channel.getDescription());
        assertNull(channel.getAutoUpdatePolicy());
        assertThat(channel.getDefaultTags(), hasSize(2));
        assertThat(channel.getDefaultTags(), containsInAnyOrder(TAG_STATUS, TAG_TIMESTAMP));
        assertEquals(0, channel.getProperties().size());
        // Parameter DateTimeFormat injected with default value
        assertThat(channel.getConfiguration().keySet(), hasSize(2));
        assertThat(channel.getConfiguration().keySet(), containsInAnyOrder("DateTimeFormat", "other"));
        assertEquals(DEFAULT_DATE_TIME_FORMAT, channel.getConfiguration().get("DateTimeFormat"));
        assertEquals("A parameter that is not in the channel config description.",
                channel.getConfiguration().get("other"));
        channel = it2.next();
        assertEquals(new ChannelUID(thingUid, "string2"), channel.getUID());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_UID, channel.getChannelTypeUID());
        assertEquals(ChannelKind.STATE, channel.getKind());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_ITEM_TYPE, channel.getAcceptedItemType());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_LABEL, channel.getLabel());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_DESCRIPTION, channel.getDescription());
        assertNull(channel.getAutoUpdatePolicy());
        assertThat(channel.getDefaultTags(), hasSize(0));
        assertEquals(0, channel.getProperties().size());
        // Parameter DateTimeFormat injected with default value
        assertThat(channel.getConfiguration().keySet(), hasSize(2));
        assertThat(channel.getConfiguration().keySet(), containsInAnyOrder("DateTimeFormat", "other"));
        assertEquals(DEFAULT_DATE_TIME_FORMAT, channel.getConfiguration().get("DateTimeFormat"));
        assertEquals("A parameter that is not in the channel config description.",
                channel.getConfiguration().get("other"));
    }

    @Test
    public void testCreateIsolatedModelWithThing() throws IOException {
        Files.copy(SOURCE_PATH.resolve("modelWithThing.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(thingProvider);

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        String name = modelRepository.createIsolatedModel(new FileInputStream(fullModelPath.toFile()), errors,
                warnings);
        assertNotNull(name);
        assertEquals(0, errors.size());
        assertEquals(0, warnings.size());

        Collection<Thing> things = thingProvider.getAllFromModel(name);
        assertThat(things, hasSize(1));
        Iterator<Thing> it = things.iterator();
        Thing thing = it.next();
        assertFalse(thing instanceof Bridge);
        ThingUID thingUid = new ThingUID("ntp", "ntp", "paris");
        assertEquals(thingUid, thing.getUID());
        assertNull(thing.getBridgeUID());
        assertEquals(NTP_THING_TYPE_UID, thing.getThingTypeUID());
        assertEquals("NTP Paris", thing.getLabel());
        assertEquals("Paris", thing.getLocation());
        assertEquals(NTP_THING_TYPE_EQUIPMENT_TAG, thing.getSemanticEquipmentTag());
        assertEquals(0, thing.getProperties().size());

        // No parameter injected
        assertThat(thing.getConfiguration().keySet(), hasSize(3));
        assertThat(thing.getConfiguration().keySet(), containsInAnyOrder("hostname", "serverPort", "timeZone"));
        assertEquals("0.fr.pool.ntp.org", thing.getConfiguration().get("hostname"));
        assertEquals(BigDecimal.valueOf(123), thing.getConfiguration().get("serverPort"));
        assertEquals("Europe/Paris", thing.getConfiguration().get("timeZone"));

        assertEquals(3, thing.getChannels().size());
        Iterator<Channel> it2 = thing.getChannels().iterator();
        Channel channel = it2.next();
        assertEquals(new ChannelUID(thingUid, "dateTime"), channel.getUID());
        assertEquals(NTP_CHANNEL_TYPE_DATETIME_CHANNEL_UID, channel.getChannelTypeUID());
        assertEquals(ChannelKind.STATE, channel.getKind());
        assertEquals(NTP_CHANNEL_TYPE_DATETIME_CHANNEL_ITEM_TYPE, channel.getAcceptedItemType());
        assertEquals(NTP_CHANNEL_TYPE_DATETIME_CHANNEL_LABEL, channel.getLabel());
        assertEquals(NTP_CHANNEL_TYPE_DATETIME_CHANNEL_DESCRIPTION, channel.getDescription());
        assertNull(channel.getAutoUpdatePolicy());
        assertThat(channel.getDefaultTags(), hasSize(2));
        assertThat(channel.getDefaultTags(), containsInAnyOrder(TAG_STATUS, TAG_TIMESTAMP));
        assertEquals(0, channel.getProperties().size());
        assertThat(channel.getConfiguration().keySet(), hasSize(0));
        channel = it2.next();
        assertEquals(new ChannelUID(thingUid, "string"), channel.getUID());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_UID, channel.getChannelTypeUID());
        assertEquals(ChannelKind.STATE, channel.getKind());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_ITEM_TYPE, channel.getAcceptedItemType());
        // label in YAML is ignored for a channel provided by the channel type
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_LABEL, channel.getLabel());
        // description in YAML is ignored for a channel provided by the channel type
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_DESCRIPTION, channel.getDescription());
        assertNull(channel.getAutoUpdatePolicy());
        assertThat(channel.getDefaultTags(), hasSize(2));
        assertThat(channel.getDefaultTags(), containsInAnyOrder(TAG_STATUS, TAG_TIMESTAMP));
        assertEquals(0, channel.getProperties().size());
        assertThat(channel.getConfiguration().keySet(), hasSize(1));
        assertThat(channel.getConfiguration().keySet(), contains("DateTimeFormat"));
        assertEquals("dd-MM-yyyy HH:mm", channel.getConfiguration().get("DateTimeFormat"));
        channel = it2.next();
        assertEquals(new ChannelUID(thingUid, "string2"), channel.getUID());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_UID, channel.getChannelTypeUID());
        assertEquals(ChannelKind.STATE, channel.getKind());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_ITEM_TYPE, channel.getAcceptedItemType());
        assertEquals("string2 channel", channel.getLabel());
        assertEquals("The string2 channel", channel.getDescription());
        assertNull(channel.getAutoUpdatePolicy());
        assertThat(channel.getDefaultTags(), hasSize(0));
        assertEquals(0, channel.getProperties().size());
        assertThat(channel.getConfiguration().keySet(), hasSize(1));
        assertThat(channel.getConfiguration().keySet(), contains("DateTimeFormat"));
        assertEquals("dd-MM-yyyy", channel.getConfiguration().get("DateTimeFormat"));
    }

    @Test
    public void testCreateIsolatedModelWithThingEmptyConfig() throws IOException {
        Files.copy(SOURCE_PATH.resolve("modelWithThingEmptyConfig.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(thingProvider);

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        String name = modelRepository.createIsolatedModel(new FileInputStream(fullModelPath.toFile()), errors,
                warnings);
        assertNotNull(name);
        assertEquals(0, errors.size());
        assertEquals(0, warnings.size());

        Collection<Thing> things = thingProvider.getAllFromModel(name);
        assertThat(things, hasSize(1));
        Iterator<Thing> it = things.iterator();
        Thing thing = it.next();
        assertFalse(thing instanceof Bridge);
        ThingUID thingUid = new ThingUID("ntp", "ntp", "paris");
        assertEquals(thingUid, thing.getUID());
        assertNull(thing.getBridgeUID());
        assertEquals(NTP_THING_TYPE_UID, thing.getThingTypeUID());
        assertEquals(NTP_THING_TYPE_LABEL, thing.getLabel());
        assertNull(thing.getLocation());
        assertEquals(NTP_THING_TYPE_EQUIPMENT_TAG, thing.getSemanticEquipmentTag());
        assertEquals(0, thing.getProperties().size());

        // No parameter injected
        assertThat(thing.getConfiguration().keySet(), hasSize(0));

        assertEquals(3, thing.getChannels().size());
        Iterator<Channel> it2 = thing.getChannels().iterator();
        Channel channel = it2.next();
        assertEquals(new ChannelUID(thingUid, "dateTime"), channel.getUID());
        assertEquals(NTP_CHANNEL_TYPE_DATETIME_CHANNEL_UID, channel.getChannelTypeUID());
        assertEquals(ChannelKind.STATE, channel.getKind());
        assertEquals(NTP_CHANNEL_TYPE_DATETIME_CHANNEL_ITEM_TYPE, channel.getAcceptedItemType());
        assertEquals(NTP_CHANNEL_TYPE_DATETIME_CHANNEL_LABEL, channel.getLabel());
        assertEquals(NTP_CHANNEL_TYPE_DATETIME_CHANNEL_DESCRIPTION, channel.getDescription());
        assertNull(channel.getAutoUpdatePolicy());
        assertThat(channel.getDefaultTags(), hasSize(2));
        assertThat(channel.getDefaultTags(), containsInAnyOrder(TAG_STATUS, TAG_TIMESTAMP));
        assertEquals(0, channel.getProperties().size());
        assertThat(channel.getConfiguration().keySet(), hasSize(0));
        channel = it2.next();
        assertEquals(new ChannelUID(thingUid, "string"), channel.getUID());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_UID, channel.getChannelTypeUID());
        assertEquals(ChannelKind.STATE, channel.getKind());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_ITEM_TYPE, channel.getAcceptedItemType());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_LABEL, channel.getLabel());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_DESCRIPTION, channel.getDescription());
        assertNull(channel.getAutoUpdatePolicy());
        assertThat(channel.getDefaultTags(), hasSize(2));
        assertThat(channel.getDefaultTags(), containsInAnyOrder(TAG_STATUS, TAG_TIMESTAMP));
        assertEquals(0, channel.getProperties().size());
        // default value not injected for parameter DateTimeFormat
        assertThat(channel.getConfiguration().keySet(), hasSize(0));
        channel = it2.next();
        assertEquals(new ChannelUID(thingUid, "string2"), channel.getUID());
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

    @Test
    public void testCreateIsolatedModelWithThingUnexpectedConfigParam() throws IOException {
        Files.copy(SOURCE_PATH.resolve("modelWithThingUnexpectedConfigParam.yaml"), fullModelPath);

        YamlModelRepositoryImpl modelRepository = new YamlModelRepositoryImpl(watchServiceMock);
        modelRepository.addYamlModelListener(thingProvider);

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        String name = modelRepository.createIsolatedModel(new FileInputStream(fullModelPath.toFile()), errors,
                warnings);
        assertNotNull(name);
        assertEquals(0, errors.size());
        assertEquals(0, warnings.size());

        Collection<Thing> things = thingProvider.getAllFromModel(name);
        assertThat(things, hasSize(1));
        Iterator<Thing> it = things.iterator();
        Thing thing = it.next();
        assertFalse(thing instanceof Bridge);
        ThingUID thingUid = new ThingUID("ntp", "ntp", "paris");
        assertEquals(thingUid, thing.getUID());
        assertNull(thing.getBridgeUID());
        assertEquals(NTP_THING_TYPE_UID, thing.getThingTypeUID());
        assertEquals(NTP_THING_TYPE_LABEL, thing.getLabel());
        assertNull(thing.getLocation());
        assertEquals(NTP_THING_TYPE_EQUIPMENT_TAG, thing.getSemanticEquipmentTag());
        assertEquals(0, thing.getProperties().size());

        // No parameter injected
        assertThat(thing.getConfiguration().keySet(), hasSize(1));
        assertThat(thing.getConfiguration().keySet(), contains("other"));
        assertEquals("A parameter that is not in the thing config description.", thing.getConfiguration().get("other"));

        assertEquals(3, thing.getChannels().size());
        Iterator<Channel> it2 = thing.getChannels().iterator();
        Channel channel = it2.next();
        assertEquals(new ChannelUID(thingUid, "dateTime"), channel.getUID());
        assertEquals(NTP_CHANNEL_TYPE_DATETIME_CHANNEL_UID, channel.getChannelTypeUID());
        assertEquals(ChannelKind.STATE, channel.getKind());
        assertEquals(NTP_CHANNEL_TYPE_DATETIME_CHANNEL_ITEM_TYPE, channel.getAcceptedItemType());
        assertEquals(NTP_CHANNEL_TYPE_DATETIME_CHANNEL_LABEL, channel.getLabel());
        assertEquals(NTP_CHANNEL_TYPE_DATETIME_CHANNEL_DESCRIPTION, channel.getDescription());
        assertNull(channel.getAutoUpdatePolicy());
        assertThat(channel.getDefaultTags(), hasSize(2));
        assertThat(channel.getDefaultTags(), containsInAnyOrder(TAG_STATUS, TAG_TIMESTAMP));
        assertEquals(0, channel.getProperties().size());
        assertThat(channel.getConfiguration().keySet(), hasSize(1));
        assertThat(channel.getConfiguration().keySet(), contains("other"));
        assertEquals("A parameter that is not in the channel config description.",
                channel.getConfiguration().get("other"));
        channel = it2.next();
        assertEquals(new ChannelUID(thingUid, "string"), channel.getUID());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_UID, channel.getChannelTypeUID());
        assertEquals(ChannelKind.STATE, channel.getKind());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_ITEM_TYPE, channel.getAcceptedItemType());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_LABEL, channel.getLabel());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_DESCRIPTION, channel.getDescription());
        assertNull(channel.getAutoUpdatePolicy());
        assertThat(channel.getDefaultTags(), hasSize(2));
        assertThat(channel.getDefaultTags(), containsInAnyOrder(TAG_STATUS, TAG_TIMESTAMP));
        assertEquals(0, channel.getProperties().size());
        // default value not injected for parameter DateTimeFormat
        assertThat(channel.getConfiguration().keySet(), hasSize(1));
        assertThat(channel.getConfiguration().keySet(), contains("other"));
        assertEquals("A parameter that is not in the channel config description.",
                channel.getConfiguration().get("other"));
        channel = it2.next();
        assertEquals(new ChannelUID(thingUid, "string2"), channel.getUID());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_UID, channel.getChannelTypeUID());
        assertEquals(ChannelKind.STATE, channel.getKind());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_ITEM_TYPE, channel.getAcceptedItemType());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_LABEL, channel.getLabel());
        assertEquals(NTP_CHANNEL_TYPE_STRING_CHANNEL_DESCRIPTION, channel.getDescription());
        assertNull(channel.getAutoUpdatePolicy());
        assertThat(channel.getDefaultTags(), hasSize(0));
        assertEquals(0, channel.getProperties().size());
        // default value not injected for parameter DateTimeFormat
        assertThat(channel.getConfiguration().keySet(), hasSize(1));
        assertThat(channel.getConfiguration().keySet(), contains("other"));
        assertEquals("A parameter that is not in the channel config description.",
                channel.getConfiguration().get("other"));
    }

    class NtpThingHandlerFactory implements ThingHandlerFactory {
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

    class NtpThingHandler implements ThingHandler {
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
}
