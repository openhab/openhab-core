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

import static org.openhab.core.config.discovery.inbox.InboxPredicates.forThingUID;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.Response.StatusType;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Role;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.automation.converter.RuleParser;
import org.openhab.core.automation.converter.RuleSerializer;
import org.openhab.core.automation.converter.RuleSerializer.RuleSerializationOption;
import org.openhab.core.automation.converter.RuleTemplateParser;
import org.openhab.core.automation.converter.RuleTemplateSerializer;
import org.openhab.core.automation.converter.RuleTemplateSerializer.RuleTemplateSerializationOption;
import org.openhab.core.automation.dto.RuleDTO;
import org.openhab.core.automation.dto.RuleDTOMapper;
import org.openhab.core.automation.dto.RuleTemplateDTO;
import org.openhab.core.automation.dto.RuleTemplateDTOMapper;
import org.openhab.core.automation.template.RuleTemplate;
import org.openhab.core.automation.template.TemplateRegistry;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.ConfigUtil;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.inbox.Inbox;
import org.openhab.core.io.dto.SerializationException;
import org.openhab.core.io.rest.JSONResponse;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.io.rest.core.fileformat.ExtendedFileFormatDTO;
import org.openhab.core.io.rest.core.fileformat.FileFormatDTO;
import org.openhab.core.io.rest.core.fileformat.FileFormatItemDTO;
import org.openhab.core.io.rest.core.fileformat.FileFormatItemDTOMapper;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemBuilderFactory;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.items.fileconverter.ItemParser;
import org.openhab.core.items.fileconverter.ItemSerializer;
import org.openhab.core.sitemap.Sitemap;
import org.openhab.core.sitemap.dto.SitemapDTOMapper;
import org.openhab.core.sitemap.dto.SitemapDefinitionDTO;
import org.openhab.core.sitemap.fileconverter.SitemapParser;
import org.openhab.core.sitemap.fileconverter.SitemapSerializer;
import org.openhab.core.sitemap.registry.SitemapFactory;
import org.openhab.core.sitemap.registry.SitemapRegistry;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingFactory;
import org.openhab.core.thing.dto.ChannelDTO;
import org.openhab.core.thing.dto.ThingDTO;
import org.openhab.core.thing.dto.ThingDTOMapper;
import org.openhab.core.thing.fileconverter.ThingParser;
import org.openhab.core.thing.fileconverter.ThingSerializer;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.type.BridgeType;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeRegistry;
import org.openhab.core.thing.util.ThingHelper;
import org.openhab.core.types.StateDescription;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JSONRequired;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * This class acts as a REST resource and provides different methods to generate file format
 * for existing items, things and sitemaps.
 *
 * This resource is registered with the Jersey servlet.
 *
 * @author Laurent Garnier - Initial contribution
 * @author Laurent Garnier - Add YAML output for things
 * @author Laurent Garnier - Add new API for conversion between file format and JSON
 * @author Mark Herwege - Add sitemap DSL
 * @author Laurent Garnier - Add sitemap YAML
 */
@Component
@JaxrsResource
@JaxrsName(FileFormatResource.PATH_FILE_FORMAT)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(FileFormatResource.PATH_FILE_FORMAT)
@Tag(name = FileFormatResource.PATH_FILE_FORMAT)
@NonNullByDefault
public class FileFormatResource implements RESTResource {

    /** The URI path to this resource */
    public static final String PATH_FILE_FORMAT = "file-format";

    private static final String DSL_THINGS_EXAMPLE = """
            Bridge binding:typeBridge:idBridge "Label bridge" @ "Location bridge" [stringParam="my value"] {
                Thing type id "Label thing" @ "Location thing" [booleanParam=true, decimalParam=2.5]
            }
            """;

    private static final String YAML_THINGS_EXAMPLE = """
            version: 1
            things:
              binding:typeBridge:idBridge:
                isBridge: true
                label: Label bridge
                location: Location bridge
                config:
                  stringParam: my value
              binding:type:idBridge:id:
                bridge: binding:typeBridge:idBridge
                label: Label thing
                location: Location thing
                config:
                  booleanParam: true
                  decimalParam: 2.5
            """;

    private static final String DSL_ITEMS_EXAMPLE = """
            Group Group1 "Label"
            Group:Switch:OR(ON,OFF) Group2 "Label"
            Switch MyItem "Label" <icon> (Group1, Group2) [Tag1, Tag2] { channel="binding:type:id:channelid", namespace="my value" [param="my param value"] }
            """;

    private static final String YAML_ITEMS_EXAMPLE = """
            version: 1
            items:
              Group1:
                type: Group
                label: Label
              Group2:
                type: Group
                group:
                  type: Switch
                  function: Or
                  parameters:
                    - "ON"
                    - "OFF"
                label: Label
              MyItem:
                type: Switch
                label: Label
                icon: icon
                groups:
                  - Group1
                  - Group2
                tags:
                  - Tag1
                  - Tag2
                channel: binding:type:id:channelid
                metadata:
                  namespace:
                    value: my value
                    config:
                      param: my param value
            """;

    private static final String DSL_RULE_EXAMPLE = """
            rule "My Rule"
            when
                Time is noon
            then
                logInfo("Test", "MyRule is running")
            end
            """;

    private static final String YAML_RULE_EXAMPLE = """
            version: 1
            rules:
              MyRule:
                label: My Rule
                description: My rule description
                actions:
                  - id: "2"
                    config:
                      type: DSL
                      script: |
                        logInfo("Test", "MyRule is running")
                    type: Script
                triggers:
                  - id: "1"
                    config:
                      time: 12:00
                    type: TimeOfDay
            """;

    private static final String YAML_RULE_TEMPLATE_EXAMPLE = """
            version: 1
            ruleTemplates:
              my-template:
                label: My Template
                description: Logs when an Item state changes to ON
                configDescriptions:
                  sourceItem:
                    context: item
                    description: The source Item whose state to monitor
                    label: Source Item
                    required: false
                    type: TEXT
                    readOnly: false
                    multiple: false
                    advanced: false
                    verify: false
                    limitToOptions: true
                actions:
                  - id: "2"
                    config:
                      type: DSL
                      script: |
                        logInfo("Test", "{{sourceItem}} turned on")
                    type: Script
                triggers:
                  - id: "1"
                    config:
                      itemName: "{{sourceItem}}"
                      state: "ON"
                      previousState: "OFF"
                    type: ItemChanged
            """;

    private static final String DSL_SITEMAPS_EXAMPLE = """
            sitemap MySitemap label="My Sitemap" {
                Frame {
                    Input item=MyItem label="My Input"
                }
            }
            """;

    private static final String YAML_SITEMAPS_EXAMPLE = """
            version: 1
            sitemaps:
              MySitemap:
                label: My Sitemap
                widgets:
                  - type: Frame
                    widgets:
                      - type: Input
                        item: MyItem
                        label: My Input
            """;

    private static final String YAML_FULL_EXAMPLE = """
            version: 1
            things:
              binding:typeBridge:idBridge:
                isBridge: true
                label: Label bridge
                location: Location bridge
                config:
                  stringParam: my value
              binding:type:idBridge:id:
                bridge: binding:typeBridge:idBridge
                label: Label thing
                location: Location thing
                config:
                  booleanParam: true
                  decimalParam: 2.5
            items:
              Group1:
                type: Group
                label: Label
              Group2:
                type: Group
                group:
                  type: Switch
                  function: Or
                  parameters:
                    - "ON"
                    - "OFF"
                label: Label
              MyItem:
                type: Switch
                label: Label
                icon: icon
                groups:
                  - Group1
                  - Group2
                tags:
                  - Tag1
                  - Tag2
                channel: binding:type:idBridge:id:channelid
                metadata:
                  namespace:
                    value: my value
                    config:
                      param: my param value
            rules:
              MyRule:
                label: Label
                actions:
                  - config:
                      type: DSL
                      script: |
                        logInfo("Test", "MyRule is running")
                    type: Script
                triggers:
                  - config:
                      itemName: MyItem
                    type: ItemReceivedCommand
            ruleTemplates:
              my-template:
                label: My Template
                description: Logs when an Item state changes to ON
                configDescriptions:
                  sourceItem:
                    context: item
                    description: The source Item whose state to monitor
                    label: Source Item
                    required: false
                    type: TEXT
                    readOnly: false
                    multiple: false
                    advanced: false
                    verify: false
                    limitToOptions: true
                actions:
                  - id: "2"
                    config:
                      type: DSL
                      script: |
                        logInfo("Test", "{{sourceItem}} turned on")
                    type: Script
                triggers:
                  - id: "1"
                    config:
                      itemName: "{{sourceItem}}"
                      state: "ON"
                      previousState: "OFF"
                    type: ItemChanged
            sitemaps:
              MySitemap:
                label: My Sitemap
                widgets:
                  - type: Frame
                    widgets:
                      - type: Input
                        item: MyItem
                        label: My Input
            """;

    private static final String GEN_ID_PATTERN = "gen_file_format_%d";

    private static final Response.StatusType UNPROCESSABLE_ENTITY = new StatusType() {

        @Override
        public int getStatusCode() {
            return 422;
        }

        @Override
        public String getReasonPhrase() {
            return "Unprocessable Entity";
        }

        @Override
        public Family getFamily() {
            return Family.CLIENT_ERROR;
        }
    };

    private final Logger logger = LoggerFactory.getLogger(FileFormatResource.class);

    private final ItemBuilderFactory itemBuilderFactory;
    private final ItemRegistry itemRegistry;
    private final MetadataRegistry metadataRegistry;
    private final ItemChannelLinkRegistry itemChannelLinkRegistry;
    private final ThingRegistry thingRegistry;
    private final Inbox inbox;
    private final ThingTypeRegistry thingTypeRegistry;
    private final ChannelTypeRegistry channelTypeRegistry;
    private final ConfigDescriptionRegistry configDescRegistry;
    private final RuleRegistry ruleRegistry;
    private final TemplateRegistry<RuleTemplate> templateRegistry;
    private final SitemapFactory sitemapFactory;
    private final SitemapRegistry sitemapRegistry;
    private final Map<String, ItemSerializer> itemSerializers = new ConcurrentHashMap<>();
    private final Map<String, ItemParser> itemParsers = new ConcurrentHashMap<>();
    private final Map<String, ThingSerializer> thingSerializers = new ConcurrentHashMap<>();
    private final Map<String, ThingParser> thingParsers = new ConcurrentHashMap<>();
    private final Map<String, RuleSerializer> ruleSerializers = new ConcurrentHashMap<>();
    private final Map<String, RuleParser> ruleParsers = new ConcurrentHashMap<>();
    private final Map<String, RuleTemplateSerializer> templateSerializers = new ConcurrentHashMap<>();
    private final Map<String, RuleTemplateParser> templateParsers = new ConcurrentHashMap<>();
    private final Map<String, SitemapSerializer> sitemapSerializers = new ConcurrentHashMap<>();
    private final Map<String, SitemapParser> sitemapParsers = new ConcurrentHashMap<>();

    private final AtomicInteger counter = new AtomicInteger();

    @Activate
    public FileFormatResource(//
            final @Reference ItemBuilderFactory itemBuilderFactory, //
            final @Reference ItemRegistry itemRegistry, //
            final @Reference MetadataRegistry metadataRegistry,
            final @Reference ItemChannelLinkRegistry itemChannelLinkRegistry,
            final @Reference ThingRegistry thingRegistry, //
            final @Reference Inbox inbox, //
            final @Reference ThingTypeRegistry thingTypeRegistry, //
            final @Reference ChannelTypeRegistry channelTypeRegistry, //
            final @Reference ConfigDescriptionRegistry configDescRegistry, //
            final @Reference RuleRegistry ruleRegistry, //
            final @Reference TemplateRegistry<RuleTemplate> templateRegistry, //
            final @Reference SitemapFactory sitemapFactory, //
            final @Reference SitemapRegistry sitemapRegistry) {
        this.itemBuilderFactory = itemBuilderFactory;
        this.itemRegistry = itemRegistry;
        this.metadataRegistry = metadataRegistry;
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
        this.thingRegistry = thingRegistry;
        this.inbox = inbox;
        this.thingTypeRegistry = thingTypeRegistry;
        this.channelTypeRegistry = channelTypeRegistry;
        this.configDescRegistry = configDescRegistry;
        this.ruleRegistry = ruleRegistry;
        this.templateRegistry = templateRegistry;
        this.sitemapFactory = sitemapFactory;
        this.sitemapRegistry = sitemapRegistry;
    }

    @Deactivate
    void deactivate() {
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void addItemSerializer(ItemSerializer itemSerializer) {
        itemSerializers.put(itemSerializer.getGeneratedFormat(), itemSerializer);
    }

    protected void removeItemSerializer(ItemSerializer itemSerializer) {
        itemSerializers.remove(itemSerializer.getGeneratedFormat());
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void addItemParser(ItemParser itemParser) {
        itemParsers.put(itemParser.getParserFormat(), itemParser);
    }

    protected void removeItemParser(ItemParser itemParser) {
        itemParsers.remove(itemParser.getParserFormat());
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void addThingSerializer(ThingSerializer thingSerializer) {
        thingSerializers.put(thingSerializer.getGeneratedFormat(), thingSerializer);
    }

    protected void removeThingSerializer(ThingSerializer thingSerializer) {
        thingSerializers.remove(thingSerializer.getGeneratedFormat());
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void addThingParser(ThingParser thingParser) {
        thingParsers.put(thingParser.getParserFormat(), thingParser);
    }

    protected void removeThingParser(ThingParser thingParser) {
        thingParsers.remove(thingParser.getParserFormat());
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void addRuleSerializer(RuleSerializer ruleSerializer) {
        ruleSerializers.put(ruleSerializer.getGeneratedFormat(), ruleSerializer);
    }

    protected void removeRuleSerializer(RuleSerializer ruleSerializer) {
        ruleSerializers.remove(ruleSerializer.getGeneratedFormat());
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void addRuleParser(RuleParser ruleParser) {
        ruleParsers.put(ruleParser.getParserFormat(), ruleParser);
    }

    protected void removeRuleParser(RuleParser ruleParser) {
        ruleParsers.remove(ruleParser.getParserFormat());
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void addRuleTemplateSerializer(RuleTemplateSerializer templateSerializer) {
        templateSerializers.put(templateSerializer.getGeneratedFormat(), templateSerializer);
    }

    protected void removeRuleTemplateSerializer(RuleTemplateSerializer templateSerializer) {
        templateSerializers.remove(templateSerializer.getGeneratedFormat());
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void addRuleTemplateParser(RuleTemplateParser templateParser) {
        templateParsers.put(templateParser.getParserFormat(), templateParser);
    }

    protected void removeRuleTemplateParser(RuleTemplateParser templateParser) {
        templateParsers.remove(templateParser.getParserFormat());
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void addSitemapSerializer(SitemapSerializer sitemapSerializer) {
        sitemapSerializers.put(sitemapSerializer.getGeneratedFormat(), sitemapSerializer);
    }

    protected void removeSitemapSerializer(SitemapSerializer sitemapSerializer) {
        sitemapSerializers.remove(sitemapSerializer.getGeneratedFormat());
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void addSitemapParser(SitemapParser sitemapParser) {
        sitemapParsers.put(sitemapParser.getParserFormat(), sitemapParser);
    }

    protected void removeSitemapParser(SitemapParser sitemapParser) {
        sitemapParsers.remove(sitemapParser.getParserFormat());
    }

    @POST
    @RolesAllowed({ Role.ADMIN })
    @Path("/items")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ "text/vnd.openhab.dsl.item", "application/yaml" })
    @Operation(operationId = "createFileFormatForItems", summary = "Create file format for a list of items in registry.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = {
                            @Content(mediaType = "text/vnd.openhab.dsl.item", schema = @Schema(example = DSL_ITEMS_EXAMPLE)),
                            @Content(mediaType = "application/yaml", schema = @Schema(example = YAML_ITEMS_EXAMPLE)) }),
                    @ApiResponse(responseCode = "404", description = "One or more items not found in registry."),
                    @ApiResponse(responseCode = "415", description = "Unsupported media type.") })
    public Response createFileFormatForItems(final @Context HttpHeaders httpHeaders,
            @DefaultValue("true") @QueryParam("hideDefaultParameters") @Parameter(description = "hide the configuration parameters having the default value") boolean hideDefaultParameters,
            @Parameter(description = "Array of item names. If empty or omitted, return all Items.") @Nullable List<String> itemNames) {
        String acceptHeader = httpHeaders.getHeaderString(HttpHeaders.ACCEPT);
        logger.debug("createFileFormatForItems: mediaType = {}, itemNames = {}", acceptHeader, itemNames);
        ItemSerializer serializer = getItemSerializer(acceptHeader);
        if (serializer == null) {
            return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                    .entity("Unsupported media type '" + acceptHeader + "'!").build();
        }
        List<Item> items;
        if (itemNames == null || itemNames.isEmpty()) {
            items = getAllItemsSorted();
        } else {
            items = new ArrayList<>();
            for (String itemname : itemNames) {
                Item item = itemRegistry.get(itemname);
                if (item == null) {
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity("Item with name '" + itemname + "' not found in the items registry!").build();
                }
                items.add(item);
            }
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        String genId = newIdForSerialization();
        Map<String, String> stateFormatters = new HashMap<>();
        items.forEach(item -> {
            StateDescription stateDescr = item.getStateDescription();
            String format = stateDescr == null ? null : stateDescr.getPattern();
            if (format != null) {
                stateFormatters.put(item.getName(), format);
            }
        });
        serializer.setItemsToBeSerialized(genId, items, getMetadata(items), stateFormatters, hideDefaultParameters);
        serializer.generateFormat(genId, outputStream);
        return Response.ok(new String(outputStream.toByteArray())).build();
    }

    @POST
    @RolesAllowed({ Role.ADMIN })
    @Path("/things")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ "text/vnd.openhab.dsl.thing", "application/yaml" })
    @Operation(operationId = "createFileFormatForThings", summary = "Create file format for a list of things in things or discovery registry.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = {
                            @Content(mediaType = "text/vnd.openhab.dsl.thing", schema = @Schema(example = DSL_THINGS_EXAMPLE)),
                            @Content(mediaType = "application/yaml", schema = @Schema(example = YAML_THINGS_EXAMPLE)) }),
                    @ApiResponse(responseCode = "404", description = "One or more things not found in registry."),
                    @ApiResponse(responseCode = "415", description = "Unsupported media type.") })
    public Response createFileFormatForThings(final @Context HttpHeaders httpHeaders,
            @DefaultValue("true") @QueryParam("hideDefaultParameters") @Parameter(description = "hide the configuration parameters having the default value") boolean hideDefaultParameters,
            @Parameter(description = "Array of Thing UIDs. If empty or omitted, return all Things from the Registry.") @Nullable List<String> thingUIDs) {
        String acceptHeader = httpHeaders.getHeaderString(HttpHeaders.ACCEPT);
        logger.debug("createFileFormatForThings: mediaType = {}, thingUIDs = {}", acceptHeader, thingUIDs);
        ThingSerializer serializer = getThingSerializer(acceptHeader);
        if (serializer == null) {
            return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                    .entity("Unsupported media type '" + acceptHeader + "'!").build();
        }
        List<Thing> things;
        if (thingUIDs == null || thingUIDs.isEmpty()) {
            things = getAllThingsSorted();
        } else {
            try {
                things = getThingsOrDiscoveryResult(thingUIDs);
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
            }
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        String genId = newIdForSerialization();
        serializer.setThingsToBeSerialized(genId, things, true, hideDefaultParameters);
        serializer.generateFormat(genId, outputStream);
        return Response.ok(new String(outputStream.toByteArray())).build();
    }

    @POST
    @RolesAllowed({ Role.ADMIN })
    @Path("/rules")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ "application/vnd.openhab.dsl.rule", "application/yaml", MediaType.APPLICATION_JSON })
    @Operation(operationId = "createFileFormatForRules", summary = "Create file format for a list of rules in the registry.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = {
                            @Content(mediaType = "application/vnd.openhab.dsl.rule", schema = @Schema(example = DSL_RULE_EXAMPLE)),
                            @Content(mediaType = "application/yaml", schema = @Schema(example = YAML_RULE_EXAMPLE)) }),
                    @ApiResponse(responseCode = "404", description = "One or more rules not found in the registry."),
                    @ApiResponse(responseCode = "415", description = "Unsupported media type."),
                    @ApiResponse(responseCode = "422", description = "Unable to serialize rule.") })
    public Response createFileFormatForRules(@Context HttpHeaders httpHeaders,
            @DefaultValue("Normal") @QueryParam("serializationOption") @Parameter(description = "Decides what to include in serialized rules") RuleSerializationOption option,
            @Parameter(description = "Array of rule UIDs. If empty or omitted, return all rules.") @Nullable List<String> ruleUIDs) {
        String acceptHeader = httpHeaders.getHeaderString(HttpHeaders.ACCEPT);
        logger.debug("createFileFormatForRules: mediaType = {}, ruleUIDs = {}", acceptHeader, ruleUIDs);
        RuleSerializer serializer = getRuleSerializer(acceptHeader);
        if (serializer == null) {
            return JSONResponse.createErrorResponse(Response.Status.UNSUPPORTED_MEDIA_TYPE,
                    "Unsupported media type '" + acceptHeader + "'");
        }
        List<Rule> rules;
        if (ruleUIDs == null || ruleUIDs.isEmpty()) {
            Collection<Rule> all = ruleRegistry.getAll();
            if (all instanceof List<Rule> allList) {
                rules = allList;
            } else {
                rules = new ArrayList<>(all);
            }
        } else {
            rules = new ArrayList<>();
            for (String ruleUID : ruleUIDs) {
                Rule rule = ruleRegistry.get(ruleUID);
                if (rule == null) {
                    return JSONResponse.createErrorResponse(Response.Status.NOT_FOUND,
                            "Rule with ID '" + ruleUID + "' not found in the rule registry!");
                }
                rules.add(rule);
            }
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        String genId = newIdForSerialization();
        try {
            serializer.setRulesToBeSerialized(genId, rules, option);
        } catch (SerializationException e) {
            return JSONResponse.createErrorResponse(UNPROCESSABLE_ENTITY, e.getMessage());
        }
        serializer.generateFormat(genId, outputStream);
        return Response.ok(new String(outputStream.toByteArray(), StandardCharsets.UTF_8)).build();
    }

    @POST
    @RolesAllowed({ Role.ADMIN })
    @Path("/ruletemplates")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ "application/yaml", MediaType.APPLICATION_JSON })
    @Operation(operationId = "createFileFormatForRuleTemplates", summary = "Create file format for a list of rule templates in the registry.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = {
                            @Content(mediaType = "application/yaml", schema = @Schema(example = YAML_RULE_TEMPLATE_EXAMPLE)) }),
                    @ApiResponse(responseCode = "404", description = "One or more rule templates not found in the registry."),
                    @ApiResponse(responseCode = "415", description = "Unsupported media type."),
                    @ApiResponse(responseCode = "422", description = "Unable to serialize rule template.") })
    public Response createFileFormatForRuleTemplates(@Context HttpHeaders httpHeaders,
            @DefaultValue("Normal") @QueryParam("serializationOption") @Parameter(description = "Decides what to include in serialized rule templates") RuleTemplateSerializationOption option,
            @Parameter(description = "Array of rule template UIDs. If empty or omitted, return all rule templates.") @Nullable List<String> templateUIDs) {
        String acceptHeader = httpHeaders.getHeaderString(HttpHeaders.ACCEPT);
        logger.debug("createFileFormatForRules: mediaType = {}, ruleUIDs = {}", acceptHeader, templateUIDs);
        RuleTemplateSerializer serializer = getRuleTemplateSerializer(acceptHeader);
        if (serializer == null) {
            return JSONResponse.createErrorResponse(Response.Status.UNSUPPORTED_MEDIA_TYPE,
                    "Unsupported media type '" + acceptHeader + "'");
        }
        List<RuleTemplate> templates;
        if (templateUIDs == null || templateUIDs.isEmpty()) {
            Collection<RuleTemplate> all = templateRegistry.getAll();
            if (all instanceof List<RuleTemplate> allList) {
                templates = allList;
            } else {
                templates = new ArrayList<>(all);
            }
        } else {
            templates = new ArrayList<>();
            for (String templateUID : templateUIDs) {
                RuleTemplate template = templateRegistry.get(templateUID);
                if (template == null) {
                    return JSONResponse.createErrorResponse(Response.Status.NOT_FOUND,
                            "Rule template with ID '" + templateUID + "' not found in the registry!");
                }
                templates.add(template);
            }
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        String genId = newIdForSerialization();
        try {
            serializer.setTemplatesToBeSerialized(genId, templates, option);
        } catch (SerializationException e) {
            return JSONResponse.createErrorResponse(UNPROCESSABLE_ENTITY, e.getMessage());
        }
        serializer.generateFormat(genId, outputStream);
        return Response.ok(new String(outputStream.toByteArray(), StandardCharsets.UTF_8)).build();
    }

    @POST
    @RolesAllowed({ Role.ADMIN })
    @Path("/sitemaps")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ "text/vnd.openhab.dsl.sitemap", "application/yaml" })
    @Operation(operationId = "createFileFormatForSitemaps", summary = "Create file format for a list of sitemaps in registry.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = {
                            @Content(mediaType = "text/vnd.openhab.dsl.sitemap", schema = @Schema(example = DSL_SITEMAPS_EXAMPLE)),
                            @Content(mediaType = "application/yaml", schema = @Schema(example = YAML_SITEMAPS_EXAMPLE)) }),
                    @ApiResponse(responseCode = "404", description = "One or more sitemaps not found in registry."),
                    @ApiResponse(responseCode = "415", description = "Unsupported media type.") })
    public Response createFileFormatForSitemaps(final @Context HttpHeaders httpHeaders,
            @Parameter(description = "Array of Sitemap names. If empty or omitted, return all Sitemaps from the Registry.") @Nullable List<String> sitemapNames) {
        String acceptHeader = httpHeaders.getHeaderString(HttpHeaders.ACCEPT);
        logger.debug("createFileFormatForSitemaps: mediaType = {}, sitemapNames = {}", acceptHeader, sitemapNames);
        SitemapSerializer serializer = getSitemapSerializer(acceptHeader);
        if (serializer == null) {
            return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                    .entity("Unsupported media type '" + acceptHeader + "'!").build();
        }

        List<Sitemap> sitemaps;
        if (sitemapNames == null || sitemapNames.isEmpty()) {
            sitemaps = sitemapRegistry.getAll().stream().sorted(Comparator.comparing(Sitemap::getName)).toList();
        } else {
            sitemaps = new ArrayList<>();
            for (String sitemapName : sitemapNames) {
                Sitemap sitemap = sitemapRegistry.get(sitemapName);
                if (sitemap == null) {
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity("Sitemap with name '" + sitemapName + "' not found in the sitemaps registry!")
                            .build();
                }
                sitemaps.add(sitemap);
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        String genId = newIdForSerialization();
        serializer.setSitemapsToBeSerialized(genId, sitemaps);
        serializer.generateFormat(genId, outputStream);
        return Response.ok(new String(outputStream.toByteArray())).build();
    }

    @POST
    @RolesAllowed({ Role.ADMIN })
    @Path("/create")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ "text/vnd.openhab.dsl.thing", "text/vnd.openhab.dsl.item", "application/vnd.openhab.dsl.rule",
            "text/vnd.openhab.dsl.sitemap", "application/yaml" })
    @Operation(operationId = "create", summary = "Create file format.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = {
                            @Content(mediaType = "text/vnd.openhab.dsl.thing", schema = @Schema(example = DSL_THINGS_EXAMPLE)),
                            @Content(mediaType = "text/vnd.openhab.dsl.item", schema = @Schema(example = DSL_ITEMS_EXAMPLE)),
                            @Content(mediaType = "application/vnd.openhab.dsl.rule", schema = @Schema(example = DSL_RULE_EXAMPLE)),
                            @Content(mediaType = "text/vnd.openhab.dsl.sitemap", schema = @Schema(example = DSL_SITEMAPS_EXAMPLE)),
                            @Content(mediaType = "application/yaml", schema = @Schema(example = YAML_FULL_EXAMPLE)) }),
                    @ApiResponse(responseCode = "400", description = "Invalid JSON data."),
                    @ApiResponse(responseCode = "415", description = "Unsupported media type."),
                    @ApiResponse(responseCode = "422", description = "Unable to serialize entity.") })
    public Response create(final @Context HttpHeaders httpHeaders,
            @DefaultValue("false") @QueryParam("hideDefaultParameters") @Parameter(description = "hide the configuration parameters having the default value") boolean hideDefaultParameters,
            @DefaultValue("false") @QueryParam("hideDefaultChannels") @Parameter(description = "hide the non extensible channels having a default configuration") boolean hideDefaultChannels,
            @DefaultValue("false") @QueryParam("hideChannelLinksAndMetadata") @Parameter(description = "hide the channel links and metadata for items") boolean hideChannelLinksAndMetadata,
            @DefaultValue("Normal") @QueryParam("ruleSerializationOption") @Parameter(description = "Decides what to include in serialized rules and rule templates") RuleSerializationOption ruleOption,
            @RequestBody(description = "JSON data", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FileFormatDTO.class))) FileFormatDTO data) {
        String acceptHeader = httpHeaders.getHeaderString(HttpHeaders.ACCEPT);
        logger.debug("create: mediaType = {}", acceptHeader);

        List<Thing> things = new ArrayList<>();
        List<Item> items = new ArrayList<>();
        List<Metadata> metadata = new ArrayList<>();
        Map<String, String> stateFormatters = new HashMap<>();
        List<Rule> rules = new ArrayList<>();
        List<RuleTemplate> templates = new ArrayList<>();
        List<Sitemap> sitemaps = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        if (!convertFromFileFormatDTO(data, things, items, metadata, stateFormatters, rules, templates, sitemaps,
                errors)) {
            return Response.status(Response.Status.BAD_REQUEST).entity(String.join("\n", errors)).build();
        }

        ThingSerializer thingSerializer = getThingSerializer(acceptHeader);
        ItemSerializer itemSerializer = getItemSerializer(acceptHeader);
        RuleSerializer ruleSerializer = getRuleSerializer(acceptHeader);
        RuleTemplateSerializer templateSerializer = getRuleTemplateSerializer(acceptHeader);
        SitemapSerializer sitemapSerializer = getSitemapSerializer(acceptHeader);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        String genId = newIdForSerialization();
        switch (acceptHeader) {
            case "text/vnd.openhab.dsl.thing":
                if (thingSerializer == null) {
                    return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                            .entity("Unsupported media type '" + acceptHeader + "'!").build();
                } else if (things.isEmpty()) {
                    return Response.status(Response.Status.BAD_REQUEST).entity("No thing loaded from input").build();
                }
                thingSerializer.setThingsToBeSerialized(genId, things, hideDefaultChannels, hideDefaultParameters);
                thingSerializer.generateFormat(genId, outputStream);
                break;
            case "text/vnd.openhab.dsl.item":
                if (itemSerializer == null) {
                    return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                            .entity("Unsupported media type '" + acceptHeader + "'!").build();
                } else if (items.isEmpty()) {
                    return Response.status(Response.Status.BAD_REQUEST).entity("No item loaded from input").build();
                }
                itemSerializer.setItemsToBeSerialized(genId, items, hideChannelLinksAndMetadata ? List.of() : metadata,
                        stateFormatters, hideDefaultParameters);
                itemSerializer.generateFormat(genId, outputStream);
                break;
            case "application/vnd.openhab.dsl.rule":
                if (ruleSerializer == null) {
                    return JSONResponse.createErrorResponse(Response.Status.UNSUPPORTED_MEDIA_TYPE,
                            "Unsupported media type '" + acceptHeader + "'");
                } else if (rules.isEmpty()) {
                    return JSONResponse.createErrorResponse(Response.Status.BAD_REQUEST, "No rule loaded from input");
                }
                try {
                    ruleSerializer.setRulesToBeSerialized(genId, rules, ruleOption);
                } catch (SerializationException e) {
                    return JSONResponse.createErrorResponse(UNPROCESSABLE_ENTITY, e.getMessage());
                }
                ruleSerializer.generateFormat(genId, outputStream);
                break;
            case "text/vnd.openhab.dsl.sitemap":
                if (sitemapSerializer == null) {
                    return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                            .entity("Unsupported media type '" + acceptHeader + "'!").build();
                } else if (sitemaps.isEmpty()) {
                    return Response.status(Response.Status.BAD_REQUEST).entity("No sitemaps loaded from input").build();
                }
                sitemapSerializer.setSitemapsToBeSerialized(genId, sitemaps);
                sitemapSerializer.generateFormat(genId, outputStream);
                break;
            case "application/yaml":
                if (thingSerializer != null) {
                    thingSerializer.setThingsToBeSerialized(genId, things, hideDefaultChannels, hideDefaultParameters);
                }
                if (itemSerializer != null) {
                    itemSerializer.setItemsToBeSerialized(genId, items,
                            hideChannelLinksAndMetadata ? List.of() : metadata, stateFormatters, hideDefaultParameters);
                }
                if (ruleSerializer != null) {
                    try {
                        ruleSerializer.setRulesToBeSerialized(genId, rules, ruleOption);
                    } catch (SerializationException e) {
                        return JSONResponse.createErrorResponse(UNPROCESSABLE_ENTITY, e.getMessage());
                    }
                }
                if (templateSerializer != null) {
                    try {
                        templateSerializer.setTemplatesToBeSerialized(genId, templates,
                                ruleOption == RuleSerializationOption.INCLUDE_ALL
                                        ? RuleTemplateSerializationOption.INCLUDE_ALL
                                        : RuleTemplateSerializationOption.NORMAL);
                    } catch (SerializationException e) {
                        return JSONResponse.createErrorResponse(UNPROCESSABLE_ENTITY, e.getMessage());
                    }
                }
                if (sitemapSerializer != null) {
                    sitemapSerializer.setSitemapsToBeSerialized(genId, sitemaps);
                }
                if (thingSerializer != null) {
                    thingSerializer.generateFormat(genId, outputStream);
                } else if (itemSerializer != null) {
                    itemSerializer.generateFormat(genId, outputStream);
                } else if (ruleSerializer != null) {
                    ruleSerializer.generateFormat(genId, outputStream);
                } else if (templateSerializer != null) {
                    templateSerializer.generateFormat(genId, outputStream);
                } else if (sitemapSerializer != null) {
                    sitemapSerializer.generateFormat(genId, outputStream);
                }
                break;
            default:
                return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                        .entity("Unsupported media type '" + acceptHeader + "'!").build();
        }
        return Response.ok(new String(outputStream.toByteArray())).build();
    }

    @POST
    @RolesAllowed({ Role.ADMIN })
    @Path("/parse")
    @Consumes({ "text/vnd.openhab.dsl.thing", "text/vnd.openhab.dsl.item", "application/vnd.openhab.dsl.rule",
            "text/vnd.openhab.dsl.sitemap", "application/yaml" })
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "parse", summary = "Parse file format.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ExtendedFileFormatDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input data."),
                    @ApiResponse(responseCode = "415", description = "Unsupported content type.") })
    public Response parse(final @Context HttpHeaders httpHeaders,
            @RequestBody(description = "file format syntax", required = true, content = {
                    @Content(mediaType = "text/vnd.openhab.dsl.thing", schema = @Schema(example = DSL_THINGS_EXAMPLE)),
                    @Content(mediaType = "text/vnd.openhab.dsl.item", schema = @Schema(example = DSL_ITEMS_EXAMPLE)),
                    @Content(mediaType = "application/vnd.openhab.dsl.rule", schema = @Schema(example = DSL_RULE_EXAMPLE)),
                    @Content(mediaType = "text/vnd.openhab.dsl.sitemap", schema = @Schema(example = DSL_SITEMAPS_EXAMPLE)),
                    @Content(mediaType = "application/yaml", schema = @Schema(example = YAML_FULL_EXAMPLE)) }) String input) {
        String contentTypeHeader = httpHeaders.getHeaderString(HttpHeaders.CONTENT_TYPE);
        logger.debug("parse: contentType = {}", contentTypeHeader);

        // First parse the input
        Collection<Thing> things = List.of();
        Collection<Item> items = List.of();
        Collection<Sitemap> sitemaps = List.of();
        Collection<Metadata> metadata = List.of();
        Collection<ItemChannelLink> channelLinks = List.of();
        Map<String, String> stateFormatters = Map.of();
        Collection<Rule> rules = List.of();
        Collection<RuleTemplate> templates = List.of();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        ThingParser thingParser = getThingParser(contentTypeHeader);
        ItemParser itemParser = getItemParser(contentTypeHeader);
        RuleParser ruleParser = getRuleParser(contentTypeHeader);
        RuleTemplateParser templateParser = getRuleTemplateParser(contentTypeHeader);
        SitemapParser sitemapParser = getSitemapParser(contentTypeHeader);
        String modelName = null;
        String thingModelName = null;
        String itemModelName = null;
        String ruleModelName = null;
        String templateModelName = null;
        String sitemapModelName = null;
        switch (contentTypeHeader) {
            case "text/vnd.openhab.dsl.thing":
                if (thingParser == null) {
                    return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                            .entity("Unsupported content type '" + contentTypeHeader + "'!").build();
                }
                thingModelName = thingParser.startParsingFormat(input, errors, warnings);
                if (thingModelName == null) {
                    return Response.status(Response.Status.BAD_REQUEST).entity(String.join("\n", errors)).build();
                }
                things = thingParser.getParsedObjects(thingModelName);
                if (things.isEmpty()) {
                    thingParser.finishParsingFormat(thingModelName);
                    return Response.status(Response.Status.BAD_REQUEST).entity("No thing loaded from input").build();
                }
                break;
            case "text/vnd.openhab.dsl.item":
                if (itemParser == null) {
                    return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                            .entity("Unsupported content type '" + contentTypeHeader + "'!").build();
                }
                itemModelName = itemParser.startParsingFormat(input, errors, warnings);
                if (itemModelName == null) {
                    return Response.status(Response.Status.BAD_REQUEST).entity(String.join("\n", errors)).build();
                }
                items = itemParser.getParsedObjects(itemModelName);
                if (items.isEmpty()) {
                    itemParser.finishParsingFormat(itemModelName);
                    return Response.status(Response.Status.BAD_REQUEST).entity("No item loaded from input").build();
                }
                metadata = itemParser.getParsedMetadata(itemModelName);
                stateFormatters = itemParser.getParsedStateFormatters(itemModelName);
                // We need to go through the thing parser to retrieve the items channel links
                // But there is no need to parse again the input
                if (thingParser != null) {
                    channelLinks = thingParser.getParsedChannelLinks(itemModelName);
                }
                break;
            case "application/vnd.openhab.dsl.rule":
                if (ruleParser == null) {
                    return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                            .entity("Unsupported content type '" + contentTypeHeader + "'!").build();
                }
                ruleModelName = ruleParser.startParsingFormat(input, errors, warnings);
                if (ruleModelName == null) {
                    return Response.status(Response.Status.BAD_REQUEST).entity(String.join("\n", errors)).build();
                }
                rules = ruleParser.getParsedObjects(ruleModelName);
                if (rules.isEmpty()) {
                    ruleParser.finishParsingFormat(ruleModelName);
                    return Response.status(Response.Status.BAD_REQUEST).entity("No rule loaded from input").build();
                }
                break;
            case "text/vnd.openhab.dsl.sitemap":
                if (sitemapParser == null) {
                    return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                            .entity("Unsupported content type '" + contentTypeHeader + "'!").build();
                }
                sitemapModelName = sitemapParser.startParsingFormat(input, errors, warnings);
                if (sitemapModelName == null) {
                    return Response.status(Response.Status.BAD_REQUEST).entity(String.join("\n", errors)).build();
                }
                sitemaps = sitemapParser.getParsedObjects(sitemapModelName);
                if (sitemaps.isEmpty()) {
                    sitemapParser.finishParsingFormat(sitemapModelName);
                    return Response.status(Response.Status.BAD_REQUEST).entity("No sitemap loaded from input").build();
                }
                break;
            case "application/yaml":
                if (thingParser != null) {
                    thingModelName = thingParser.startParsingFormat(input, errors, warnings);
                    if (thingModelName == null) {
                        return Response.status(Response.Status.BAD_REQUEST).entity(String.join("\n", errors)).build();
                    }
                    modelName = thingModelName;
                    things = thingParser.getParsedObjects(thingModelName);
                    channelLinks = thingParser.getParsedChannelLinks(thingModelName);
                }
                if (itemParser != null) {
                    // Avoid parsing the input again
                    if (modelName == null) {
                        itemModelName = itemParser.startParsingFormat(input, errors, warnings);
                        if (itemModelName == null) {
                            return Response.status(Response.Status.BAD_REQUEST).entity(String.join("\n", errors))
                                    .build();
                        }
                        modelName = itemModelName;
                    } else {
                        itemModelName = modelName;
                    }
                    items = itemParser.getParsedObjects(itemModelName);
                    metadata = itemParser.getParsedMetadata(itemModelName);
                    stateFormatters = itemParser.getParsedStateFormatters(itemModelName);
                }
                if (ruleParser != null) {
                    // Avoid parsing the input again
                    if (modelName == null) {
                        ruleModelName = ruleParser.startParsingFormat(input, errors, warnings);
                        if (ruleModelName == null) {
                            return Response.status(Response.Status.BAD_REQUEST).entity(String.join("\n", errors))
                                    .build();
                        }
                        modelName = ruleModelName;
                    } else {
                        ruleModelName = modelName;
                    }
                    rules = ruleParser.getParsedObjects(ruleModelName);
                }
                if (templateParser != null) {
                    // Avoid parsing the input again
                    if (modelName == null) {
                        templateModelName = templateParser.startParsingFormat(input, errors, warnings);
                        if (templateModelName == null) {
                            return Response.status(Response.Status.BAD_REQUEST).entity(String.join("\n", errors))
                                    .build();
                        }
                        modelName = templateModelName;
                    } else {
                        templateModelName = modelName;
                    }
                    templates = templateParser.getParsedObjects(templateModelName);
                }
                if (sitemapParser != null) {
                    // Avoid parsing the input again
                    if (modelName == null) {
                        sitemapModelName = sitemapParser.startParsingFormat(input, errors, warnings);
                        if (sitemapModelName == null) {
                            return Response.status(Response.Status.BAD_REQUEST).entity(String.join("\n", errors))
                                    .build();
                        }
                        modelName = sitemapModelName;
                    } else {
                        sitemapModelName = modelName;
                    }
                    sitemaps = sitemapParser.getParsedObjects(sitemapModelName);
                }
                break;
            default:
                return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                        .entity("Unsupported content type '" + contentTypeHeader + "'!").build();
        }
        ExtendedFileFormatDTO result = convertToFileFormatDTO(things, items, metadata, stateFormatters, channelLinks,
                rules, templates, sitemaps, warnings);
        if (thingModelName != null && thingParser != null) {
            thingParser.finishParsingFormat(thingModelName);
        }
        if (itemModelName != null && itemParser != null) {
            itemParser.finishParsingFormat(itemModelName);
        }
        if (ruleModelName != null && ruleParser != null) {
            ruleParser.finishParsingFormat(ruleModelName);
        }
        if (templateModelName != null && templateParser != null) {
            templateParser.finishParsingFormat(templateModelName);
        }
        if (sitemapModelName != null && sitemapParser != null) {
            sitemapParser.finishParsingFormat(sitemapModelName);
        }
        return Response.ok(result).build();
    }

    private String newIdForSerialization() {
        return String.format(Locale.ROOT, GEN_ID_PATTERN, counter.incrementAndGet());
    }

    /*
     * Get all the metadata for a list of items including channel links mapped to metadata in the namespace "channel"
     */
    private Collection<Metadata> getMetadata(Collection<Item> items) {
        Collection<Metadata> metadata = new ArrayList<>();
        for (Item item : items) {
            String itemName = item.getName();
            metadataRegistry.getAll().stream().filter(md -> md.getUID().getItemName().equals(itemName)).forEach(md -> {
                metadata.add(md);
            });
            itemChannelLinkRegistry.getLinks(itemName).forEach(link -> {
                MetadataKey key = new MetadataKey("channel", itemName);
                Metadata md = new Metadata(key, link.getLinkedUID().getAsString(),
                        link.getConfiguration().getProperties());
                metadata.add(md);
            });
        }
        return metadata;
    }

    /*
     * Get all items from registry sorted in such a way:
     * - group items are before non group items
     * - group items are sorted to have as much as possible ancestors before their children
     * - items not linked to a channel are before items linked to a channel
     * - items linked to a channel are grouped by thing UID
     * - items linked to the same thing UID are sorted by item name
     */
    private List<Item> getAllItemsSorted() {
        Collection<Item> items = itemRegistry.getAll();
        List<Item> groups = items.stream().filter(item -> item instanceof GroupItem).sorted((item1, item2) -> {
            return item1.getName().compareTo(item2.getName());
        }).toList();

        List<Item> topGroups = groups.stream().filter(group -> group.getGroupNames().isEmpty())
                .sorted((group1, group2) -> {
                    return group1.getName().compareTo(group2.getName());
                }).toList();

        List<Item> groupTree = new ArrayList<>();
        for (Item group : topGroups) {
            fillGroupTree(groupTree, group);
        }

        if (groupTree.size() != groups.size()) {
            logger.warn("Something went wrong when sorting groups; failback to a sort by name.");
            groupTree = groups;
        }

        List<Item> nonGroups = items.stream().filter(item -> !(item instanceof GroupItem)).sorted((item1, item2) -> {
            Set<ItemChannelLink> channelLinks1 = itemChannelLinkRegistry.getLinks(item1.getName());
            String thingUID1 = channelLinks1.isEmpty() ? null
                    : channelLinks1.iterator().next().getLinkedUID().getThingUID().getAsString();
            Set<ItemChannelLink> channelLinks2 = itemChannelLinkRegistry.getLinks(item2.getName());
            String thingUID2 = channelLinks2.isEmpty() ? null
                    : channelLinks2.iterator().next().getLinkedUID().getThingUID().getAsString();

            if (thingUID1 == null && thingUID2 != null) {
                return -1;
            } else if (thingUID1 != null && thingUID2 == null) {
                return 1;
            } else if (thingUID1 != null && thingUID2 != null && !thingUID1.equals(thingUID2)) {
                return thingUID1.compareTo(thingUID2);
            }
            return item1.getName().compareTo(item2.getName());
        }).toList();

        return Stream.of(groupTree, nonGroups).flatMap(List::stream).toList();
    }

    private void fillGroupTree(List<Item> groups, Item item) {
        if (item instanceof GroupItem group && !groups.contains(group)) {
            groups.add(group);
            List<Item> members = group.getMembers().stream().sorted((member1, member2) -> {
                return member1.getName().compareTo(member2.getName());
            }).toList();
            for (Item member : members) {
                fillGroupTree(groups, member);
            }
        }
    }

    /*
     * Get all things from registry sorted in such a way:
     * - things are grouped by binding, sorted by natural order of binding name
     * - all things of a binding are sorted to follow the tree, that is bridge thing is before its sub-things
     * - all things of a binding at a certain tree depth are sorted by thing UID
     */
    private List<Thing> getAllThingsSorted() {
        Collection<Thing> things = thingRegistry.getAll();
        List<Thing> thingTree = new ArrayList<>();
        Set<String> bindings = things.stream().map(thing -> thing.getUID().getBindingId()).collect(Collectors.toSet());
        for (String binding : bindings.stream().sorted().toList()) {
            List<Thing> topThings = things.stream()
                    .filter(thing -> thing.getUID().getBindingId().equals(binding) && thing.getBridgeUID() == null)
                    .sorted((thing1, thing2) -> {
                        return thing1.getUID().getAsString().compareTo(thing2.getUID().getAsString());
                    }).toList();
            for (Thing thing : topThings) {
                fillThingTree(thingTree, thing);
            }
        }
        return thingTree;
    }

    private void fillThingTree(List<Thing> things, Thing thing) {
        if (!things.contains(thing)) {
            things.add(thing);
            if (thing instanceof Bridge bridge) {
                List<Thing> subThings = bridge.getThings().stream().sorted((thing1, thing2) -> {
                    return thing1.getUID().getAsString().compareTo(thing2.getUID().getAsString());
                }).toList();
                for (Thing subThing : subThings) {
                    fillThingTree(things, subThing);
                }
            }
        }
    }

    /*
     * Create a thing from a discovery result without inserting it in the thing registry
     */
    private Thing simulateThing(DiscoveryResult result, ThingType thingType) {
        Map<String, Object> configParams = new HashMap<>();
        List<ConfigDescriptionParameter> configDescriptionParameters = List.of();
        URI descURI = thingType.getConfigDescriptionURI();
        if (descURI != null) {
            ConfigDescription desc = configDescRegistry.getConfigDescription(descURI);
            if (desc != null) {
                configDescriptionParameters = desc.getParameters();
            }
        }
        for (ConfigDescriptionParameter param : configDescriptionParameters) {
            Object value = result.getProperties().get(param.getName());
            Object normalizedValue = value != null ? ConfigUtil.normalizeType(value, param) : null;
            if (normalizedValue != null) {
                configParams.put(param.getName(), normalizedValue);
            }
        }
        Configuration config = new Configuration(configParams);
        return ThingFactory.createThing(thingType, result.getThingUID(), config, result.getBridgeUID(),
                configDescRegistry);
    }

    private @Nullable ItemSerializer getItemSerializer(String mediaType) {
        return switch (mediaType) {
            case "text/vnd.openhab.dsl.item" -> itemSerializers.get("DSL");
            case "application/yaml" -> itemSerializers.get("YAML");
            default -> null;
        };
    }

    private @Nullable ThingSerializer getThingSerializer(String mediaType) {
        return switch (mediaType) {
            case "text/vnd.openhab.dsl.thing" -> thingSerializers.get("DSL");
            case "application/yaml" -> thingSerializers.get("YAML");
            default -> null;
        };
    }

    private @Nullable RuleSerializer getRuleSerializer(String mediaType) {
        switch (mediaType) {
            case "application/yaml":
                return ruleSerializers.get("YAML");
            case "application/vnd.openhab.dsl.rule":
                return ruleSerializers.get("DSL");
            default:
                return null;
        }
    }

    private @Nullable RuleTemplateSerializer getRuleTemplateSerializer(String mediaType) {
        switch (mediaType) {
            case "application/yaml":
                return templateSerializers.get("YAML");
            default:
                return null;
        }
    }

    private @Nullable SitemapSerializer getSitemapSerializer(String mediaType) {
        return switch (mediaType) {
            case "text/vnd.openhab.dsl.sitemap" -> sitemapSerializers.get("DSL");
            case "application/yaml" -> sitemapSerializers.get("YAML");
            default -> null;
        };
    }

    private @Nullable ItemParser getItemParser(String contentType) {
        return switch (contentType) {
            case "text/vnd.openhab.dsl.item" -> itemParsers.get("DSL");
            case "application/yaml" -> itemParsers.get("YAML");
            default -> null;
        };
    }

    private @Nullable ThingParser getThingParser(String contentType) {
        return switch (contentType) {
            case "text/vnd.openhab.dsl.thing" -> thingParsers.get("DSL");
            case "text/vnd.openhab.dsl.item" -> thingParsers.get("DSL");
            case "application/yaml" -> thingParsers.get("YAML");
            default -> null;
        };
    }

    private @Nullable RuleParser getRuleParser(String contentType) {
        switch (contentType) {
            case "application/yaml":
                return ruleParsers.get("YAML");
            case "application/vnd.openhab.dsl.rule":
                return ruleParsers.get("DSL");
            default:
                return null;
        }
    }

    private @Nullable RuleTemplateParser getRuleTemplateParser(String contentType) {
        switch (contentType) {
            case "application/yaml":
                return templateParsers.get("YAML");
            default:
                return null;
        }
    }

    private @Nullable SitemapParser getSitemapParser(String contentType) {
        return switch (contentType) {
            case "text/vnd.openhab.dsl.sitemap" -> sitemapParsers.get("DSL");
            case "application/yaml" -> sitemapParsers.get("YAML");
            default -> null;
        };
    }

    private List<Thing> getThingsOrDiscoveryResult(List<String> thingUIDs) {
        return thingUIDs.stream().distinct().map(uid -> {
            ThingUID thingUID = new ThingUID(uid);
            Thing thing = thingRegistry.get(thingUID);
            if (thing != null) {
                return thing;
            }

            DiscoveryResult discoveryResult = inbox.stream().filter(forThingUID(thingUID)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Thing with UID '" + uid + "' not found in the things or discovery registry!"));

            ThingTypeUID thingTypeUID = discoveryResult.getThingTypeUID();
            ThingType thingType = thingTypeRegistry.getThingType(thingTypeUID);
            if (thingType == null) {
                throw new IllegalArgumentException("Thing type with UID '" + thingTypeUID + "' does not exist!");
            }
            return simulateThing(discoveryResult, thingType);
        }).toList();
    }

    private boolean convertFromFileFormatDTO(FileFormatDTO data, List<Thing> things, List<Item> items,
            List<Metadata> metadata, Map<String, String> stateFormatters, List<Rule> rules,
            List<RuleTemplate> templates, List<Sitemap> sitemaps, List<String> errors) {
        boolean ok = true;
        if (data.things != null) {
            for (ThingDTO thingData : data.things) {
                ThingUID thingUID = thingData.UID == null ? null : new ThingUID(thingData.UID);
                ThingTypeUID thingTypeUID = new ThingTypeUID(thingData.thingTypeUID);

                ThingUID bridgeUID = null;

                if (thingData.bridgeUID != null) {
                    bridgeUID = new ThingUID(thingData.bridgeUID);
                }

                // turn the ThingDTO's configuration into a Configuration
                Configuration configuration = new Configuration(
                        normalizeConfiguration(thingData.configuration, thingTypeUID, thingUID));
                if (thingUID != null) {
                    normalizeChannels(thingData, thingUID);
                }

                Thing thing = thingRegistry.createThingOfType(thingTypeUID, thingUID, bridgeUID, thingData.label,
                        configuration);

                if (thing != null) {
                    if (thingData.properties != null) {
                        for (Map.Entry<String, String> entry : thingData.properties.entrySet()) {
                            thing.setProperty(entry.getKey(), entry.getValue());
                        }
                    }
                    if (thingData.location != null) {
                        thing.setLocation(thingData.location);
                    }
                    if (thingData.channels != null) {
                        // The provided channels replace the channels provided by the thing type.
                        ThingDTO thingChannels = new ThingDTO();
                        thingChannels.channels = thingData.channels;
                        thing = ThingHelper.merge(thing, thingChannels);
                    }
                } else if (thingUID != null) {
                    // if there wasn't any ThingFactory capable of creating the thing,
                    // we create the Thing exactly the way we received it, i.e. we
                    // cannot take its thing type into account for automatically
                    // populating channels and properties.
                    thing = ThingDTOMapper.map(thingData,
                            thingTypeRegistry.getThingType(thingTypeUID) instanceof BridgeType);
                } else {
                    errors.add("A thing UID must be provided, since no binding can create the thing!");
                    ok = false;
                    continue;
                }

                things.add(thing);
            }
        }
        if (data.items != null) {
            for (FileFormatItemDTO itemData : data.items) {
                String name = itemData.name;
                if (name == null || name.isEmpty()) {
                    errors.add("Missing item name in items data!");
                    ok = false;
                    continue;
                }

                Item item;
                try {
                    item = FileFormatItemDTOMapper.map(itemData, itemBuilderFactory);
                    if (item == null) {
                        errors.add("Invalid item type in items data!");
                        ok = false;
                        continue;
                    }
                } catch (IllegalArgumentException e) {
                    errors.add("Invalid item name in items data!");
                    ok = false;
                    continue;
                }
                items.add(item);
                metadata.addAll(FileFormatItemDTOMapper.mapMetadata(itemData));
                if (itemData.format != null) {
                    stateFormatters.put(name, itemData.format);
                }
            }
        }
        if (data.rules != null) {
            for (RuleDTO ruleData : data.rules) { // TODO: (Nad) Any checks/validation to do?
                rules.add(RuleDTOMapper.map(ruleData));
            }
        }
        if (data.ruleTemplates != null) {
            for (RuleTemplateDTO templateData : data.ruleTemplates) {
                templates.add(RuleTemplateDTOMapper.map(templateData));
            }
        }
        if (data.sitemaps != null) {
            for (SitemapDefinitionDTO sitemapData : data.sitemaps) {
                String name = sitemapData.name;
                try {
                    Sitemap sitemap = SitemapDTOMapper.map(sitemapData, sitemapFactory);
                    sitemaps.add(sitemap);
                } catch (IllegalArgumentException e) {
                    errors.add("Invalid sitemap data" + (name != null ? " for sitemap '" + name + "'" : "") + ": "
                            + e.getMessage());
                    ok = false;
                    continue;
                }
            }
        }
        return ok;
    }

    private ExtendedFileFormatDTO convertToFileFormatDTO(Collection<Thing> things, Collection<Item> items,
            Collection<Metadata> metadata, Map<String, String> stateFormatters,
            Collection<ItemChannelLink> channelLinks, Collection<Rule> rules, Collection<RuleTemplate> templates,
            Collection<Sitemap> sitemaps, List<String> warnings) {
        ExtendedFileFormatDTO dto = new ExtendedFileFormatDTO();
        dto.warnings = warnings.isEmpty() ? null : warnings;
        if (!things.isEmpty()) {
            dto.things = new ArrayList<>();
            things.forEach(thing -> {
                // Normalize thing configuration
                @Nullable
                Map<String, @Nullable Object> normalizedThingConfig = normalizeConfiguration(
                        thing.getConfiguration().getProperties(), thing.getThingTypeUID(), thing.getUID());
                if (normalizedThingConfig != null) {
                    thing.getConfiguration().keySet().forEach(paramName -> {
                        @Nullable
                        Object normalizedValue = normalizedThingConfig.get(paramName);
                        if (normalizedValue != null) {
                            thing.getConfiguration().put(paramName, normalizedValue);
                        }
                    });
                }
                // Normalize channel configuration
                thing.getChannels().forEach(channel -> {
                    ChannelTypeUID channelTypeUID = channel.getChannelTypeUID();
                    if (channelTypeUID != null) {
                        @Nullable
                        Map<String, @Nullable Object> normalizedChannelConfig = normalizeConfiguration(
                                channel.getConfiguration().getProperties(), channelTypeUID, channel.getUID());
                        if (normalizedChannelConfig != null) {
                            channel.getConfiguration().keySet().forEach(paramName -> {
                                @Nullable
                                Object normalizedValue = normalizedChannelConfig.get(paramName);
                                if (normalizedValue != null) {
                                    channel.getConfiguration().put(paramName, normalizedValue);
                                }
                            });
                        }
                    }
                });
                dto.things.add(ThingDTOMapper.map(thing));
            });
        }
        if (!items.isEmpty()) {
            dto.items = new ArrayList<>();
            items.forEach(item -> {
                dto.items.add(
                        FileFormatItemDTOMapper.map(item, metadata, stateFormatters.get(item.getName()), channelLinks));
            });
        }
        if (!rules.isEmpty()) {
            dto.rules = new ArrayList<>();
            for (Rule rule : rules) {
                dto.rules.add(RuleDTOMapper.map(rule));
            }
        }
        if (!templates.isEmpty()) {
            dto.ruleTemplates = new ArrayList<>();
            for (RuleTemplate template : templates) {
                dto.ruleTemplates.add(RuleTemplateDTOMapper.map(template));
            }
        }
        if (!sitemaps.isEmpty()) {
            dto.sitemaps = new ArrayList<>();
            sitemaps.forEach(sitemap -> {
                dto.sitemaps.add(SitemapDTOMapper.map(sitemap));
            });
        }
        return dto;
    }

    private @Nullable Map<String, @Nullable Object> normalizeConfiguration(
            @Nullable Map<String, @Nullable Object> properties, ThingTypeUID thingTypeUID,
            @Nullable ThingUID thingUID) {
        if (properties == null || properties.isEmpty()) {
            return properties;
        }

        ThingType thingType = thingTypeRegistry.getThingType(thingTypeUID);
        if (thingType == null) {
            return properties;
        }

        List<ConfigDescription> configDescriptions = new ArrayList<>(2);

        URI descURI = thingType.getConfigDescriptionURI();
        if (descURI != null) {
            ConfigDescription typeConfigDesc = configDescRegistry.getConfigDescription(descURI);
            if (typeConfigDesc != null) {
                configDescriptions.add(typeConfigDesc);
            }
        }

        if (thingUID != null) {
            ConfigDescription thingConfigDesc = configDescRegistry
                    .getConfigDescription(getConfigDescriptionURI(thingUID));
            if (thingConfigDesc != null) {
                configDescriptions.add(thingConfigDesc);
            }
        }

        if (configDescriptions.isEmpty()) {
            return properties;
        }

        return ConfigUtil.normalizeTypes(properties, configDescriptions);
    }

    private @Nullable Map<String, @Nullable Object> normalizeConfiguration(Map<String, @Nullable Object> properties,
            ChannelTypeUID channelTypeUID, ChannelUID channelUID) {
        if (properties == null || properties.isEmpty()) {
            return properties;
        }

        ChannelType channelType = channelTypeRegistry.getChannelType(channelTypeUID);
        if (channelType == null) {
            return properties;
        }

        List<ConfigDescription> configDescriptions = new ArrayList<>(2);
        URI descURI = channelType.getConfigDescriptionURI();
        if (descURI != null) {
            ConfigDescription typeConfigDesc = configDescRegistry.getConfigDescription(descURI);
            if (typeConfigDesc != null) {
                configDescriptions.add(typeConfigDesc);
            }
        }
        if (getConfigDescriptionURI(channelUID) != null) {
            ConfigDescription channelConfigDesc = configDescRegistry
                    .getConfigDescription(getConfigDescriptionURI(channelUID));
            if (channelConfigDesc != null) {
                configDescriptions.add(channelConfigDesc);
            }
        }

        if (configDescriptions.isEmpty()) {
            return properties;
        }

        return ConfigUtil.normalizeTypes(properties, configDescriptions);
    }

    private void normalizeChannels(ThingDTO thingBean, ThingUID thingUID) {
        if (thingBean.channels != null) {
            for (ChannelDTO channelBean : thingBean.channels) {
                if (channelBean.channelTypeUID != null) {
                    channelBean.configuration = normalizeConfiguration(channelBean.configuration,
                            new ChannelTypeUID(channelBean.channelTypeUID), new ChannelUID(thingUID, channelBean.id));
                }
            }
        }
    }

    private URI getConfigDescriptionURI(ThingUID thingUID) {
        String uriString = "thing:" + thingUID;
        try {
            return new URI(uriString);
        } catch (URISyntaxException e) {
            throw new BadRequestException("Invalid URI syntax: " + uriString);
        }
    }

    private URI getConfigDescriptionURI(ChannelUID channelUID) {
        String uriString = "channel:" + channelUID;
        try {
            return new URI(uriString);
        } catch (URISyntaxException e) {
            throw new BadRequestException("Invalid URI syntax: " + uriString);
        }
    }
}
