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
package org.openhab.core.io.rest.core.internal.fileformat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.automation.converter.RuleSerializer.RuleSerializationOption;
import org.openhab.core.automation.template.TemplateRegistry;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.ConfigUtil;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.discovery.inbox.Inbox;
import org.openhab.core.io.rest.core.fileformat.FileFormatDTO;
import org.openhab.core.items.ItemBuilderFactory;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.semantics.SemanticTagRegistry;
import org.openhab.core.sitemap.registry.SitemapFactory;
import org.openhab.core.sitemap.registry.SitemapRegistry;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.dto.ChannelDTO;
import org.openhab.core.thing.dto.ThingDTO;
import org.openhab.core.thing.fileconverter.ThingSerializer;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.thing.type.ThingTypeRegistry;

/**
 * Tests for raw configuration handling in the file-format create endpoint.
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
public class FileFormatResourceTest {

    private static final String RAW_PLACEHOLDER = "${ENV:HOSTNAME}";
    private static final String ACCEPT_DSL_THING = "text/vnd.openhab.dsl.thing";

    private static class ConfigUtilAccessor extends ConfigUtil {
        static void setEnv(Map<String, String> values) {
            setEnvProvider(values::get);
        }

        static void resetEnv() {
            setEnvProvider(System::getenv);
        }
    }

    private @Mock @NonNullByDefault({}) SemanticTagRegistry semanticTagRegistry;
    private @Mock @NonNullByDefault({}) ItemBuilderFactory itemBuilderFactory;
    private @Mock @NonNullByDefault({}) ItemRegistry itemRegistry;
    private @Mock @NonNullByDefault({}) MetadataRegistry metadataRegistry;
    private @Mock @NonNullByDefault({}) ItemChannelLinkRegistry itemChannelLinkRegistry;
    private @Mock @NonNullByDefault({}) ThingRegistry thingRegistry;
    private @Mock @NonNullByDefault({}) Inbox inbox;
    private @Mock @NonNullByDefault({}) ThingTypeRegistry thingTypeRegistry;
    private @Mock @NonNullByDefault({}) ChannelTypeRegistry channelTypeRegistry;
    private @Mock @NonNullByDefault({}) ConfigDescriptionRegistry configDescRegistry;
    private @Mock @NonNullByDefault({}) RuleRegistry ruleRegistry;
    @SuppressWarnings("rawtypes")
    private @Mock @NonNullByDefault({}) TemplateRegistry templateRegistry;
    private @Mock @NonNullByDefault({}) SitemapFactory sitemapFactory;
    private @Mock @NonNullByDefault({}) SitemapRegistry sitemapRegistry;
    private @Mock @NonNullByDefault({}) ThingSerializer thingSerializer;
    private @Mock @NonNullByDefault({}) HttpHeaders httpHeaders;

    private @NonNullByDefault({}) FileFormatResource resource;

    @BeforeEach
    public void setUp() {
        ConfigUtilAccessor.setEnv(Map.of("HOSTNAME", "openhab-host"));
        resource = new FileFormatResource(semanticTagRegistry, itemBuilderFactory, itemRegistry, metadataRegistry,
                itemChannelLinkRegistry, thingRegistry, inbox, thingTypeRegistry, channelTypeRegistry,
                configDescRegistry, ruleRegistry, templateRegistry, sitemapFactory, sitemapRegistry);
        when(thingSerializer.getGeneratedFormat()).thenReturn("DSL");
        resource.addThingSerializer(thingSerializer);
        when(httpHeaders.getHeaderString(HttpHeaders.ACCEPT)).thenReturn(ACCEPT_DSL_THING);
    }

    @AfterEach
    public void tearDown() {
        ConfigUtilAccessor.resetEnv();
    }

    @Test
    public void createUsesRawValuesWhenFallingBackToThingDTOMapper() {
        ThingTypeUID thingTypeUID = new ThingTypeUID("binding", "type");
        ThingUID thingUID = new ThingUID(thingTypeUID, "thing");

        ThingDTO thingDTO = new ThingDTO();
        thingDTO.thingTypeUID = thingTypeUID.getAsString();
        thingDTO.UID = thingUID.getAsString();
        thingDTO.configuration = Map.of("thingParam", RAW_PLACEHOLDER);

        ChannelDTO channelDTO = new ChannelDTO();
        channelDTO.uid = new ChannelUID(thingUID, "channel1").getAsString();
        channelDTO.itemType = CoreItemFactory.STRING;
        channelDTO.kind = "STATE";
        channelDTO.configuration = Map.of("channelParam", RAW_PLACEHOLDER);
        channelDTO.properties = Map.of();
        channelDTO.defaultTags = Set.of();
        thingDTO.channels = List.of(channelDTO);

        FileFormatDTO data = new FileFormatDTO();
        data.things = List.of(thingDTO);

        when(thingRegistry.createThingOfType(any(), any(), any(), any(), any())).thenReturn(null);

        try (Response response = resource.create(httpHeaders, false, false, false, RuleSerializationOption.NORMAL,
                data)) {
            assertThat(response.getStatus(), is(200));
        }

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Thing>> captor = (ArgumentCaptor<List<Thing>>) (ArgumentCaptor<?>) ArgumentCaptor
                .forClass(List.class);
        verify(thingSerializer).setThingsToBeSerialized(anyString(), captor.capture(), anyBoolean(), anyBoolean());
        Thing serializedThing = captor.getValue().getFirst();
        assertThat(serializedThing.getConfiguration().get("thingParam"), is("openhab-host"));
        assertThat(serializedThing.getConfiguration().getRawProperties().get("thingParam"), is(RAW_PLACEHOLDER));
        assertThat(serializedThing.getChannels().getFirst().getConfiguration().get("channelParam"), is("openhab-host"));
        assertThat(serializedThing.getChannels().getFirst().getConfiguration().getRawProperties().get("channelParam"),
                is(RAW_PLACEHOLDER));
    }

    @Test
    public void createPassesRawConfigurationToThingFactoryPath() {
        ThingTypeUID thingTypeUID = new ThingTypeUID("binding", "type");
        ThingUID thingUID = new ThingUID(thingTypeUID, "thing");

        ThingDTO thingDTO = new ThingDTO();
        thingDTO.thingTypeUID = thingTypeUID.getAsString();
        thingDTO.UID = thingUID.getAsString();
        thingDTO.configuration = Map.of("thingParam", RAW_PLACEHOLDER);

        FileFormatDTO data = new FileFormatDTO();
        data.things = List.of(thingDTO);

        when(thingRegistry.createThingOfType(any(), any(), any(), any(), any())).thenReturn(mock(Thing.class));

        try (Response response = resource.create(httpHeaders, false, false, false, RuleSerializationOption.NORMAL,
                data)) {
            assertThat(response.getStatus(), is(200));
        }

        ArgumentCaptor<Configuration> configCaptor = ArgumentCaptor.forClass(Configuration.class);
        verify(thingRegistry).createThingOfType(any(ThingTypeUID.class), any(ThingUID.class), any(), any(),
                configCaptor.capture());
        assertThat(configCaptor.getValue().get("thingParam"), is("openhab-host"));
        assertThat(configCaptor.getValue().getRawProperties().get("thingParam"), is(RAW_PLACEHOLDER));
    }
}
