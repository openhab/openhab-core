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
package org.openhab.core.io.rest.core.internal.fileformat;

import static org.openhab.core.config.discovery.inbox.InboxPredicates.forThingUID;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Role;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.ConfigUtil;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.inbox.Inbox;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.io.rest.core.fileformat.FileFormatDTO;
import org.openhab.core.io.rest.core.fileformat.MetadataDTO;
import org.openhab.core.io.rest.core.fileformat.ParsedFileFormatDTO;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemBuilderFactory;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.items.dto.ItemDTO;
import org.openhab.core.items.dto.ItemDTOMapper;
import org.openhab.core.items.fileconverter.ItemFileGenerator;
import org.openhab.core.items.fileconverter.ItemFileParser;
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
import org.openhab.core.thing.fileconverter.ThingFileGenerator;
import org.openhab.core.thing.fileconverter.ThingFileParser;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.link.dto.ItemChannelLinkDTO;
import org.openhab.core.thing.type.BridgeType;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeRegistry;
import org.openhab.core.thing.util.ThingHelper;
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
 * for existing items and things.
 *
 * This resource is registered with the Jersey servlet.
 *
 * @author Laurent Garnier - Initial contribution
 * @author Laurent Garnier - Add new API to create and parse file formats
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

    private static final String DSL_ITEMS_EXAMPLE = "Group Group1 \"Label\"\nGroup:Switch:OR(ON,OFF) Group2 \"Label\"\nSwitch MyItem \"Label\" <icon> (Group1, Group2) [Tag1, Tag2] { channel=\"binding:type:id:channelid\", namespace=\"my value\" [param=\"my param value\"] }";
    private static final String DSL_ITEM_EXAMPLE = "Number MyItem \"Label\" <icon> (Group1, Group2) [Tag1, Tag2] { channel=\"binding:type:id:channelid\", namespace=\"my value\" [param=\"my param value\"] }";
    private static final String DSL_THINGS_EXAMPLE = "Bridge binding:typeBridge:idBridge \"Label\" @ \"Location\" [stringParam=\"my value\"] {\n    Thing type id \"Label\" @ \"Location\" [booleanParam=true, decimalParam=2.5]\n}";
    private static final String DSL_THING_EXAMPLE = "Thing binding:type:idBridge:id \"Label\" @ \"Location\" (binding:typeBridge:idBridge) [stringParam=\"my value\", booleanParam=true, decimalParam=2.5]";

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
    private final Map<String, ItemFileGenerator> itemFileGenerators = new ConcurrentHashMap<>();
    private final Map<String, ItemFileParser> itemFileParsers = new ConcurrentHashMap<>();
    private final Map<String, ThingFileGenerator> thingFileGenerators = new ConcurrentHashMap<>();
    private final Map<String, ThingFileParser> thingFileParsers = new ConcurrentHashMap<>();

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
            final @Reference ConfigDescriptionRegistry configDescRegistry) {
        this.itemBuilderFactory = itemBuilderFactory;
        this.itemRegistry = itemRegistry;
        this.metadataRegistry = metadataRegistry;
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
        this.thingRegistry = thingRegistry;
        this.inbox = inbox;
        this.thingTypeRegistry = thingTypeRegistry;
        this.channelTypeRegistry = channelTypeRegistry;
        this.configDescRegistry = configDescRegistry;
    }

    @Deactivate
    void deactivate() {
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void addItemFileGenerator(ItemFileGenerator itemFileGenerator) {
        itemFileGenerators.put(itemFileGenerator.getFileFormatGenerator(), itemFileGenerator);
    }

    protected void removeItemFileGenerator(ItemFileGenerator itemFileGenerator) {
        itemFileGenerators.remove(itemFileGenerator.getFileFormatGenerator());
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void addItemFileParser(ItemFileParser itemFileParser) {
        itemFileParsers.put(itemFileParser.getFileFormatParser(), itemFileParser);
    }

    protected void removeItemFileParser(ItemFileParser itemFileParser) {
        itemFileParsers.remove(itemFileParser.getFileFormatParser());
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void addThingFileGenerator(ThingFileGenerator thingFileGenerator) {
        thingFileGenerators.put(thingFileGenerator.getFileFormatGenerator(), thingFileGenerator);
    }

    protected void removeThingFileGenerator(ThingFileGenerator thingFileGenerator) {
        thingFileGenerators.remove(thingFileGenerator.getFileFormatGenerator());
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void addThingFileParser(ThingFileParser thingFileParser) {
        thingFileParsers.put(thingFileParser.getFileFormatParser(), thingFileParser);
    }

    protected void removeThingFileParser(ThingFileParser thingFileParser) {
        thingFileParsers.remove(thingFileParser.getFileFormatParser());
    }

    @GET
    @RolesAllowed({ Role.ADMIN })
    @Path("/items")
    @Produces("text/vnd.openhab.dsl.item")
    @Operation(operationId = "createFileFormatForAllItems", summary = "Create file format for all existing items in registry.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "text/vnd.openhab.dsl.item", schema = @Schema(example = DSL_ITEMS_EXAMPLE))),
                    @ApiResponse(responseCode = "415", description = "Unsupported media type.") })
    public Response createFileFormatForAllItems(final @Context HttpHeaders httpHeaders,
            @DefaultValue("true") @QueryParam("hideDefaultParameters") @Parameter(description = "hide the configuration parameters having the default value") boolean hideDefaultParameters) {
        String acceptHeader = httpHeaders.getHeaderString(HttpHeaders.ACCEPT);
        String format = "text/vnd.openhab.dsl.item".equals(acceptHeader) ? "DSL" : null;
        ItemFileGenerator generator = format == null ? null : itemFileGenerators.get(format);
        if (generator == null) {
            return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                    .entity("Unsupported media type '" + acceptHeader + "'!").build();
        }
        Collection<Item> items = itemRegistry.getAll();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        generator.generateFileFormat(outputStream, sortItems(items), getMetadata(items), hideDefaultParameters);
        return Response.ok(new String(outputStream.toByteArray())).build();
    }

    @GET
    @RolesAllowed({ Role.ADMIN })
    @Path("/items/{itemname: [a-zA-Z_0-9]+}")
    @Produces("text/vnd.openhab.dsl.item")
    @Operation(operationId = "createFileFormatForItem", summary = "Create file format for an existing item in registry.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "text/vnd.openhab.dsl.item", schema = @Schema(example = DSL_ITEM_EXAMPLE))),
                    @ApiResponse(responseCode = "404", description = "Item not found in registry."),
                    @ApiResponse(responseCode = "415", description = "Unsupported media type.") })
    public Response createFileFormatForItem(final @Context HttpHeaders httpHeaders,
            @DefaultValue("true") @QueryParam("hideDefaultParameters") @Parameter(description = "hide the configuration parameters having the default value") boolean hideDefaultParameters,
            @PathParam("itemname") @Parameter(description = "item name") String itemname) {
        String acceptHeader = httpHeaders.getHeaderString(HttpHeaders.ACCEPT);
        String format = "text/vnd.openhab.dsl.item".equals(acceptHeader) ? "DSL" : null;
        ItemFileGenerator generator = format == null ? null : itemFileGenerators.get(format);
        if (generator == null) {
            return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                    .entity("Unsupported media type '" + acceptHeader + "'!").build();
        }
        Item item = itemRegistry.get(itemname);
        if (item == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Item with name '" + itemname + "' not found in the items registry!").build();
        }
        List<Item> items = List.of(item);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        generator.generateFileFormat(outputStream, items, getMetadata(items), hideDefaultParameters);
        return Response.ok(new String(outputStream.toByteArray())).build();
    }

    @GET
    @RolesAllowed({ Role.ADMIN })
    @Path("/things")
    @Produces("text/vnd.openhab.dsl.thing")
    @Operation(operationId = "createFileFormatForAllThings", summary = "Create file format for all existing things in registry.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "text/vnd.openhab.dsl.thing", schema = @Schema(example = DSL_THINGS_EXAMPLE))),
                    @ApiResponse(responseCode = "415", description = "Unsupported media type.") })
    public Response createFileFormatForAllThings(final @Context HttpHeaders httpHeaders,
            @DefaultValue("true") @QueryParam("hideDefaultParameters") @Parameter(description = "hide the configuration parameters having the default value") boolean hideDefaultParameters) {
        String acceptHeader = httpHeaders.getHeaderString(HttpHeaders.ACCEPT);
        String format = "text/vnd.openhab.dsl.thing".equals(acceptHeader) ? "DSL" : null;
        ThingFileGenerator generator = format == null ? null : thingFileGenerators.get(format);
        if (generator == null) {
            return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                    .entity("Unsupported media type '" + acceptHeader + "'!").build();
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        generator.generateFileFormat(outputStream, sortThings(thingRegistry.getAll()), true, hideDefaultParameters);
        return Response.ok(new String(outputStream.toByteArray())).build();
    }

    @GET
    @RolesAllowed({ Role.ADMIN })
    @Path("/things/{thingUID}")
    @Produces("text/vnd.openhab.dsl.thing")
    @Operation(operationId = "createFileFormatForThing", summary = "Create file format for an existing thing in things or discovery registry.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "text/vnd.openhab.dsl.thing", schema = @Schema(example = DSL_THING_EXAMPLE))),
                    @ApiResponse(responseCode = "404", description = "Thing not found in things or discovery registry or thing type not found."),
                    @ApiResponse(responseCode = "415", description = "Unsupported media type.") })
    public Response createFileFormatForThing(final @Context HttpHeaders httpHeaders,
            @DefaultValue("true") @QueryParam("hideDefaultParameters") @Parameter(description = "hide the configuration parameters having the default value") boolean hideDefaultParameters,
            @PathParam("thingUID") @Parameter(description = "thingUID") String thingUID) {
        String acceptHeader = httpHeaders.getHeaderString(HttpHeaders.ACCEPT);
        String format = "text/vnd.openhab.dsl.thing".equals(acceptHeader) ? "DSL" : null;
        ThingFileGenerator generator = format == null ? null : thingFileGenerators.get(format);
        if (generator == null) {
            return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                    .entity("Unsupported media type '" + acceptHeader + "'!").build();
        }
        ThingUID aThingUID = new ThingUID(thingUID);
        Thing thing = thingRegistry.get(aThingUID);
        if (thing == null) {
            List<DiscoveryResult> results = inbox.getAll().stream().filter(forThingUID(new ThingUID(thingUID)))
                    .toList();
            if (results.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Thing with UID '" + thingUID + "' not found in the things or discovery registry!")
                        .build();
            }
            DiscoveryResult result = results.get(0);
            ThingType thingType = thingTypeRegistry.getThingType(result.getThingTypeUID());
            if (thingType == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Thing type with UID '" + result.getThingTypeUID() + "' does not exist!").build();
            }
            thing = simulateThing(result, thingType);
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        generator.generateFileFormat(outputStream, List.of(thing), true, hideDefaultParameters);
        return Response.ok(new String(outputStream.toByteArray())).build();
    }

    @POST
    @RolesAllowed({ Role.ADMIN })
    @Path("/parse")
    @Consumes({ "text/vnd.openhab.dsl.item", "text/vnd.openhab.dsl.thing" })
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "parseFileFormat", summary = "Parse file format.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = ParsedFileFormatDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid syntax."),
                    @ApiResponse(responseCode = "415", description = "Unsupported content type.") })
    public Response parseFileFormat(final @Context HttpHeaders httpHeaders,
            @RequestBody(description = "file format syntax", required = true, content = {
                    @Content(mediaType = "text/vnd.openhab.dsl.item", schema = @Schema(example = DSL_ITEMS_EXAMPLE)),
                    @Content(mediaType = "text/vnd.openhab.dsl.thing", schema = @Schema(example = DSL_THINGS_EXAMPLE)) }) String syntax) {
        String contentTypetHeader = httpHeaders.getHeaderString(HttpHeaders.CONTENT_TYPE);
        List<String> errors = new ArrayList<>();
        ParsedFileFormatDTO result;
        switch (contentTypetHeader) {
            case "text/vnd.openhab.dsl.item":
                ItemFileParser itemFileParser = itemFileParsers.get("DSL");
                if (itemFileParser != null) {
                    result = parseItems(syntax, itemFileParser, errors);
                    return result != null ? Response.ok(result).build()
                            : Response.status(Response.Status.BAD_REQUEST).entity(String.join("\n", errors)).build();
                }
                break;
            case "text/vnd.openhab.dsl.thing":
                ThingFileParser thingFileParser = thingFileParsers.get("DSL");
                if (thingFileParser != null) {
                    result = parseThings(syntax, thingFileParser, errors);
                    return result != null ? Response.ok(result).build()
                            : Response.status(Response.Status.BAD_REQUEST).entity(String.join("\n", errors)).build();
                }
                break;
            default:
                break;
        }
        return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                .entity("Unsupported content type '" + contentTypetHeader + "'!").build();
    }

    @POST
    @RolesAllowed({ Role.ADMIN })
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ "text/vnd.openhab.dsl.item", "text/vnd.openhab.dsl.thing" })
    @Operation(operationId = "createFileFormat", summary = "Create file format.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = {
                            @Content(mediaType = "text/vnd.openhab.dsl.item", schema = @Schema(example = DSL_ITEMS_EXAMPLE)),
                            @Content(mediaType = "text/vnd.openhab.dsl.thing", schema = @Schema(example = DSL_THINGS_EXAMPLE)) }),
                    @ApiResponse(responseCode = "400", description = "Invalid input data."),
                    @ApiResponse(responseCode = "415", description = "Unsupported media type.") })
    public Response createFileFormat(final @Context HttpHeaders httpHeaders,
            @DefaultValue("false") @QueryParam("hideDefaultChannels") @Parameter(description = "hide the non extensible channels having a default configuration") boolean hideDefaultChannels,
            @DefaultValue("false") @QueryParam("hideDefaultParameters") @Parameter(description = "hide the configuration parameters having the default value") boolean hideDefaultParameters,
            @RequestBody(description = "input data", required = true, content = @Content(schema = @Schema(implementation = FileFormatDTO.class))) FileFormatDTO data) {
        String acceptHeader = httpHeaders.getHeaderString(HttpHeaders.ACCEPT);
        List<String> errors = new ArrayList<>();
        String result;
        switch (acceptHeader) {
            case "text/vnd.openhab.dsl.item":
                ItemFileGenerator itemFileGenerator = itemFileGenerators.get("DSL");
                if (itemFileGenerator != null) {
                    result = createItems(data, itemFileGenerator, errors);
                    return result != null ? Response.ok(result).build()
                            : Response.status(Response.Status.BAD_REQUEST).entity(String.join("\n", errors)).build();
                }
                break;
            case "text/vnd.openhab.dsl.thing":
                ThingFileGenerator thingFileGenerator = thingFileGenerators.get("DSL");
                if (thingFileGenerator != null) {
                    result = createThings(data, hideDefaultChannels, hideDefaultParameters, thingFileGenerator, errors);
                    return result != null ? Response.ok(result).build()
                            : Response.status(Response.Status.BAD_REQUEST).entity(String.join("\n", errors)).build();
                }
                break;
            default:
                break;
        }
        return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                .entity("Unsupported media type '" + acceptHeader + "'!").build();
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
     * Sort the items in such a way:
     * - group items are before non group items
     * - group items are sorted to have as much as possible ancestors before their children
     * - items not linked to a channel are before items linked to a channel
     * - items linked to a channel are grouped by thing UID
     * - items linked to the same thing UID are sorted by item name
     */
    private List<Item> sortItems(Collection<Item> items) {
        List<Item> groups = items.stream().filter(item -> item instanceof GroupItem).sorted((item1, item2) -> {
            return item1.getName().compareTo(item2.getName());
        }).collect(Collectors.toList());

        List<Item> topGroups = groups.stream().filter(group -> group.getGroupNames().isEmpty())
                .sorted((group1, group2) -> {
                    return group1.getName().compareTo(group2.getName());
                }).collect(Collectors.toList());

        List<Item> groupTree = new ArrayList<>();
        for (Item group : topGroups) {
            fillGroupTree(groupTree, group);
        }

        if (groupTree.size() != groups.size()) {
            logger.warn("Something want wrong when sorting groups; failback to a sort by name.");
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
        }).collect(Collectors.toList());

        return Stream.of(groupTree, nonGroups).flatMap(List::stream).collect(Collectors.toList());
    }

    private void fillGroupTree(List<Item> groups, Item item) {
        if (item instanceof GroupItem group && !groups.contains(group)) {
            groups.add(group);
            List<Item> members = group.getMembers().stream().sorted((member1, member2) -> {
                return member1.getName().compareTo(member2.getName());
            }).collect(Collectors.toList());
            for (Item member : members) {
                fillGroupTree(groups, member);
            }
        }
    }

    /*
     * Sort the things in such a way:
     * - things are grouped by binding, sorted by natural order of binding name
     * - all things of a binding are sorted to follow the tree, that is bridge thing is before its sub-things
     * - all things of a binding at a certain tree depth are sorted by thing UID
     */
    private List<Thing> sortThings(Collection<Thing> things) {
        List<Thing> thingTree = new ArrayList<>();
        Set<String> bindings = things.stream().map(thing -> thing.getUID().getBindingId()).collect(Collectors.toSet());
        for (String binding : bindings.stream().sorted().collect(Collectors.toList())) {
            List<Thing> topThings = things.stream()
                    .filter(thing -> thing.getUID().getBindingId().equals(binding) && thing.getBridgeUID() == null)
                    .sorted((thing1, thing2) -> {
                        return thing1.getUID().getAsString().compareTo(thing2.getUID().getAsString());
                    }).collect(Collectors.toList());
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
                }).collect(Collectors.toList());
                for (Thing subThing : subThings) {
                    fillThingTree(things, subThing);
                }
            }
        }
    }

    private @Nullable ParsedFileFormatDTO parseItems(String syntax, ItemFileParser parser, List<String> errors) {
        List<Item> items = new ArrayList<>();
        List<Metadata> metadata = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        if (!parser.parseFileFormat(syntax, items, metadata, errors, warnings)) {
            return null;
        }

        ParsedFileFormatDTO dto = new ParsedFileFormatDTO();
        dto.warnings = warnings.isEmpty() ? null : warnings;
        dto.items = new ArrayList<>();
        dto.channelLinks = new ArrayList<>();
        dto.metadata = new ArrayList<>();
        items.forEach(item -> {
            dto.items.add(ItemDTOMapper.map(item));
        });
        metadata.forEach(md -> {
            if ("channel".equals(md.getUID().getNamespace())) {
                dto.channelLinks.add(new ItemChannelLinkDTO(md.getUID().getItemName(), md.getValue(),
                        md.getConfiguration().isEmpty() ? null : md.getConfiguration()));
            } else {
                dto.metadata.add(new MetadataDTO(md.getUID().getItemName(), md.getUID().getNamespace(), md.getValue(),
                        md.getConfiguration().isEmpty() ? null : md.getConfiguration()));
            }
        });
        if (dto.channelLinks.isEmpty()) {
            dto.channelLinks = null;
        }
        if (dto.metadata.isEmpty()) {
            dto.metadata = null;
        }
        return dto;
    }

    private @Nullable ParsedFileFormatDTO parseThings(String syntax, ThingFileParser parser, List<String> errors) {
        List<Thing> things = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        if (!parser.parseFileFormat(syntax, things, errors, warnings)) {
            return null;
        }

        ParsedFileFormatDTO dto = new ParsedFileFormatDTO();
        dto.warnings = warnings.isEmpty() ? null : warnings;
        dto.things = new ArrayList<>();
        things.forEach(thing -> {
            dto.things.add(ThingDTOMapper.map(thing));
        });
        return dto;
    }

    private @Nullable String createItems(FileFormatDTO data, ItemFileGenerator generator, List<String> errors) {
        if (data.items == null) {
            errors.add("No items in data!");
            return null;
        }
        List<Item> items = new ArrayList<>();
        Collection<Metadata> metadata = new ArrayList<>();
        for (ItemDTO itemData : data.items) {
            String name = itemData.name;
            if (name == null || name.isEmpty()) {
                errors.add("Item name missing in items data!");
                return null;
            }

            Item item;
            try {
                item = ItemDTOMapper.map(itemData, itemBuilderFactory);
                if (item == null) {
                    errors.add("Invalid item type in items data!");
                    return null;
                }
            } catch (IllegalArgumentException e) {
                errors.add("Invalid item name in items data!");
                return null;
            }
            items.add(item);
        }

        if (data.channelLinks != null) {
            for (ItemChannelLinkDTO link : data.channelLinks) {
                MetadataKey key = new MetadataKey("channel", link.itemName);
                metadata.add(new Metadata(key, link.channelUID, link.configuration));
            }
        }
        if (data.metadata != null) {
            for (MetadataDTO md : data.metadata) {
                MetadataKey key = new MetadataKey(md.namespace, md.itemName);
                metadata.add(new Metadata(key, md.value, md.configuration));
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        generator.generateFileFormat(outputStream, items, metadata, false);
        return new String(outputStream.toByteArray());
    }

    private @Nullable String createThings(FileFormatDTO data, boolean hideDefaultChannels,
            boolean hideDefaultParameters, ThingFileGenerator generator, List<String> errors) {
        if (data.things == null) {
            errors.add("No things in data!");
            return null;
        }
        List<Thing> things = new ArrayList<>();
        for (ThingDTO thingBean : data.things) {
            ThingUID thingUID = thingBean.UID == null ? null : new ThingUID(thingBean.UID);
            ThingTypeUID thingTypeUID = new ThingTypeUID(thingBean.thingTypeUID);

            ThingUID bridgeUID = null;

            if (thingBean.bridgeUID != null) {
                bridgeUID = new ThingUID(thingBean.bridgeUID);
                if (thingUID != null && (!thingUID.getBindingId().equals(bridgeUID.getBindingId())
                        || !thingUID.getBridgeIds().contains(bridgeUID.getId()))) {
                    errors.add("Thing UID '" + thingUID + "' does not match bridge UID '" + bridgeUID + "'");
                    return null;
                }
            }

            // turn the ThingDTO's configuration into a Configuration
            Configuration configuration = new Configuration(
                    normalizeConfiguration(thingBean.configuration, thingTypeUID, thingUID));
            if (thingUID != null) {
                normalizeChannels(thingBean, thingUID);
            }

            Thing thing = thingRegistry.createThingOfType(thingTypeUID, thingUID, bridgeUID, thingBean.label,
                    configuration);

            if (thing != null) {
                if (thingBean.properties != null) {
                    for (Entry<String, String> entry : thingBean.properties.entrySet()) {
                        thing.setProperty(entry.getKey(), entry.getValue());
                    }
                }
                if (thingBean.location != null) {
                    thing.setLocation(thingBean.location);
                }
                if (thingBean.channels != null) {
                    // The provided channels replace the channels provided by the thing type.
                    ThingDTO thingChannels = new ThingDTO();
                    thingChannels.channels = thingBean.channels;
                    thing = ThingHelper.merge(thing, thingChannels);
                }
            } else if (thingUID != null) {
                // if there wasn't any ThingFactory capable of creating the thing,
                // we create the Thing exactly the way we received it, i.e. we
                // cannot take its thing type into account for automatically
                // populating channels and properties.
                thing = ThingDTOMapper.map(thingBean,
                        thingTypeRegistry.getThingType(thingTypeUID) instanceof BridgeType);
            } else {
                errors.add("A thing UID must be provided, since no binding can create the thing!");
                return null;
            }

            things.add(thing);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        generator.generateFileFormat(outputStream, things, hideDefaultChannels, hideDefaultParameters);
        return new String(outputStream.toByteArray());
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
}
