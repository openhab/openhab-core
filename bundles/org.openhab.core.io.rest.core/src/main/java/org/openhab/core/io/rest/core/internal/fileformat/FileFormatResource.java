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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.eclipse.jdt.annotation.NonNullByDefault;
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
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.items.syntax.ItemSyntaxGenerator;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingFactory;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.syntax.ThingSyntaxGenerator;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeRegistry;
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

    private final Logger logger = LoggerFactory.getLogger(FileFormatResource.class);

    private final ItemRegistry itemRegistry;
    private final MetadataRegistry metadataRegistry;
    private final ItemChannelLinkRegistry itemChannelLinkRegistry;
    private final ThingRegistry thingRegistry;
    private final Inbox inbox;
    private final ThingTypeRegistry thingTypeRegistry;
    private final ConfigDescriptionRegistry configDescRegistry;
    private final Map<String, ItemSyntaxGenerator> itemSyntaxGenerators = new ConcurrentHashMap<>();
    private final Map<String, ThingSyntaxGenerator> thingSyntaxGenerators = new ConcurrentHashMap<>();

    @Activate
    public FileFormatResource(//
            final @Reference ItemRegistry itemRegistry, //
            final @Reference MetadataRegistry metadataRegistry,
            final @Reference ItemChannelLinkRegistry itemChannelLinkRegistry,
            final @Reference ThingRegistry thingRegistry, //
            final @Reference Inbox inbox, //
            final @Reference ThingTypeRegistry thingTypeRegistry, //
            final @Reference ConfigDescriptionRegistry configDescRegistry) {
        this.itemRegistry = itemRegistry;
        this.metadataRegistry = metadataRegistry;
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
        this.thingRegistry = thingRegistry;
        this.inbox = inbox;
        this.thingTypeRegistry = thingTypeRegistry;
        this.configDescRegistry = configDescRegistry;
    }

    @Deactivate
    void deactivate() {
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void addItemSyntaxGenerator(ItemSyntaxGenerator itemSyntaxGenerator) {
        itemSyntaxGenerators.put(itemSyntaxGenerator.getGeneratorFormat(), itemSyntaxGenerator);
    }

    protected void removeItemSyntaxGenerator(ItemSyntaxGenerator itemSyntaxGenerator) {
        itemSyntaxGenerators.remove(itemSyntaxGenerator.getGeneratorFormat());
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void addThingSyntaxGenerator(ThingSyntaxGenerator thingSyntaxGenerator) {
        thingSyntaxGenerators.put(thingSyntaxGenerator.getGeneratorFormat(), thingSyntaxGenerator);
    }

    protected void removeThingSyntaxGenerator(ThingSyntaxGenerator thingSyntaxGenerator) {
        thingSyntaxGenerators.remove(thingSyntaxGenerator.getGeneratorFormat());
    }

    @GET
    @RolesAllowed({ Role.ADMIN })
    @Path("/existing/items")
    @Produces("text/vnd.openhab-dsl")
    @Operation(operationId = "createFileFormatForAllItems", summary = "Create file format for all existing items in registry.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "text/vnd.openhab-dsl", schema = @Schema(example = "Group Group1 \"Label\"\nGroup:Switch:OR(ON,OFF) Group2 \"Label\"\nSwitch MyItem \"Label\" <icon> (Group1, Group2) [Tag1, Tag2] { channel=\"binding:type:id:channelid\", namespace=\"my value\" [param=\"my param value\"] }"))),
                    @ApiResponse(responseCode = "415", description = "Unsupported media type.") })
    public Response createFileFormatForAllItems(final @Context HttpHeaders httpHeaders,
            @DefaultValue("true") @QueryParam("hideDefaultParameters") @Parameter(description = "hide the configuration parameters having the default value") boolean hideDefaultParameters) {
        String acceptHeader = httpHeaders.getHeaderString(HttpHeaders.ACCEPT);
        String format = "text/vnd.openhab-dsl".equals(acceptHeader) ? "DSL" : null;
        ItemSyntaxGenerator generator = format == null ? null : itemSyntaxGenerators.get(format);
        if (generator == null) {
            return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                    .entity("Unsupported media type '" + acceptHeader + "'!").build();
        }
        Collection<Item> items = itemRegistry.getAll();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        generator.generateSyntax(outputStream, sortItems(items), getMetadata(items), hideDefaultParameters);
        return Response.ok(new String(outputStream.toByteArray())).build();
    }

    @GET
    @RolesAllowed({ Role.ADMIN })
    @Path("/existing/item/{itemname: [a-zA-Z_0-9]+}")
    @Produces("text/vnd.openhab-dsl")
    @Operation(operationId = "createFileFormatForItem", summary = "Create file format for an existing item in registry.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "text/vnd.openhab-dsl", schema = @Schema(example = "Number MyItem \"Label\" <icon> (Group1, Group2) [Tag1, Tag2] { channel=\"binding:type:id:channelid\", namespace=\"my value\" [param=\"my param value\"] }"))),
                    @ApiResponse(responseCode = "404", description = "Item not found in registry."),
                    @ApiResponse(responseCode = "415", description = "Unsupported media type.") })
    public Response createFileFormatForItem(final @Context HttpHeaders httpHeaders,
            @DefaultValue("true") @QueryParam("hideDefaultParameters") @Parameter(description = "hide the configuration parameters having the default value") boolean hideDefaultParameters,
            @PathParam("itemname") @Parameter(description = "item name") String itemname) {
        String acceptHeader = httpHeaders.getHeaderString(HttpHeaders.ACCEPT);
        String format = "text/vnd.openhab-dsl".equals(acceptHeader) ? "DSL" : null;
        ItemSyntaxGenerator generator = format == null ? null : itemSyntaxGenerators.get(format);
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
        generator.generateSyntax(outputStream, items, getMetadata(items), hideDefaultParameters);
        return Response.ok(new String(outputStream.toByteArray())).build();
    }

    @GET
    @RolesAllowed({ Role.ADMIN })
    @Path("/existing/things")
    @Produces("text/vnd.openhab-dsl")
    @Operation(operationId = "createFileFormatForAllThings", summary = "Create file format for all existing things in registry.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "text/vnd.openhab-dsl", schema = @Schema(example = "Bridge binding:typeBridge:idBridge \"Label\" @ \"Location\" [stringParam=\"my value\"] {\n    Thing type id \"Label\" @ \"Location\" [booleanParam=true, decimalParam=2.5]\n}"))),
                    @ApiResponse(responseCode = "415", description = "Unsupported media type.") })
    public Response createFileFormatForAllThings(final @Context HttpHeaders httpHeaders,
            @DefaultValue("true") @QueryParam("hideDefaultParameters") @Parameter(description = "hide the configuration parameters having the default value") boolean hideDefaultParameters) {
        String acceptHeader = httpHeaders.getHeaderString(HttpHeaders.ACCEPT);
        String format = "text/vnd.openhab-dsl".equals(acceptHeader) ? "DSL" : null;
        ThingSyntaxGenerator generator = format == null ? null : thingSyntaxGenerators.get(format);
        if (generator == null) {
            return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                    .entity("Unsupported media type '" + acceptHeader + "'!").build();
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        generator.generateSyntax(outputStream, sortThings(thingRegistry.getAll()), hideDefaultParameters);
        return Response.ok(new String(outputStream.toByteArray())).build();
    }

    @GET
    @RolesAllowed({ Role.ADMIN })
    @Path("/existing/thing/{thingUID}")
    @Produces("text/vnd.openhab-dsl")
    @Operation(operationId = "createFileFormatForThing", summary = "Create file format for an existing thing in registry.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "text/vnd.openhab-dsl", schema = @Schema(example = "Thing binding:type:idBridge:id \"Label\" @ \"Location\" (binding:typeBridge:idBridge) [stringParam=\"my value\", booleanParam=true, decimalParam=2.5]"))),
                    @ApiResponse(responseCode = "404", description = "Thing not found in registry."),
                    @ApiResponse(responseCode = "415", description = "Unsupported media type.") })
    public Response createFileFormatForThing(final @Context HttpHeaders httpHeaders,
            @DefaultValue("true") @QueryParam("hideDefaultParameters") @Parameter(description = "hide the configuration parameters having the default value") boolean hideDefaultParameters,
            @PathParam("thingUID") @Parameter(description = "thingUID") String thingUID) {
        String acceptHeader = httpHeaders.getHeaderString(HttpHeaders.ACCEPT);
        String format = "text/vnd.openhab-dsl".equals(acceptHeader) ? "DSL" : null;
        ThingSyntaxGenerator generator = format == null ? null : thingSyntaxGenerators.get(format);
        if (generator == null) {
            return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                    .entity("Unsupported media type '" + acceptHeader + "'!").build();
        }
        ThingUID aThingUID = new ThingUID(thingUID);
        Thing thing = thingRegistry.get(aThingUID);
        if (thing == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Thing with UID '" + thingUID + "' not found in the things registry!").build();
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        generator.generateSyntax(outputStream, List.of(thing), hideDefaultParameters);
        return Response.ok(new String(outputStream.toByteArray())).build();
    }

    @GET
    @Path("/existing/thing-from-inbox/{thingUID}")
    @Produces("text/vnd.openhab-dsl")
    @Operation(operationId = "generateSyntaxForDiscoveryResult", summary = "Create file format for an existing thing in discovey registry.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "text/vnd.openhab-dsl", schema = @Schema(example = "Thing binding:type:idBridge:id \"Label\" (binding:typeBridge:idBridge) [stringParam=\"my value\", booleanParam=true, decimalParam=2.5]"))),
                    @ApiResponse(responseCode = "404", description = "Discovery result not found in the inbox or thing type not found."),
                    @ApiResponse(responseCode = "415", description = "Unsupported media type.") })
    public Response generateSyntaxForDiscoveryResult(final @Context HttpHeaders httpHeaders,
            @DefaultValue("true") @QueryParam("hideDefaultParameters") @Parameter(description = "hide the configuration parameters having the default value") boolean hideDefaultParameters,
            @PathParam("thingUID") @Parameter(description = "thingUID") String thingUID) {
        String acceptHeader = httpHeaders.getHeaderString(HttpHeaders.ACCEPT);
        String format = "text/vnd.openhab-dsl".equals(acceptHeader) ? "DSL" : null;
        ThingSyntaxGenerator generator = format == null ? null : thingSyntaxGenerators.get(format);
        if (generator == null) {
            return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                    .entity("Unsupported media type '" + acceptHeader + "'!").build();
        }

        List<DiscoveryResult> results = inbox.getAll().stream().filter(forThingUID(new ThingUID(thingUID))).toList();
        if (results.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Discovery result for thing with UID '" + thingUID + "' not found in the inbox!").build();
        }
        DiscoveryResult result = results.get(0);
        ThingType thingType = thingTypeRegistry.getThingType(result.getThingTypeUID());
        if (thingType == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Thing type with UID '" + result.getThingTypeUID() + "' does not exist!").build();
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        generator.generateSyntax(outputStream, List.of(simulateThing(result, thingType)), hideDefaultParameters);
        return Response.ok(new String(outputStream.toByteArray())).build();
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
