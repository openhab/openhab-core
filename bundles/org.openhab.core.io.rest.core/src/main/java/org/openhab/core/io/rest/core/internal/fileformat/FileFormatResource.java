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
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.items.fileconverter.ItemFileGenerator;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingFactory;
import org.openhab.core.thing.fileconverter.ThingFileGenerator;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
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
 * @author Laurent Garnier - Add YAML output for things
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
            version: 2
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
            version: 2
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
                category: icon
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

    private final Logger logger = LoggerFactory.getLogger(FileFormatResource.class);

    private final ItemRegistry itemRegistry;
    private final MetadataRegistry metadataRegistry;
    private final ItemChannelLinkRegistry itemChannelLinkRegistry;
    private final ThingRegistry thingRegistry;
    private final Inbox inbox;
    private final ThingTypeRegistry thingTypeRegistry;
    private final ConfigDescriptionRegistry configDescRegistry;
    private final Map<String, ItemFileGenerator> itemFileGenerators = new ConcurrentHashMap<>();
    private final Map<String, ThingFileGenerator> thingFileGenerators = new ConcurrentHashMap<>();

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
    protected void addItemFileGenerator(ItemFileGenerator itemFileGenerator) {
        itemFileGenerators.put(itemFileGenerator.getFileFormatGenerator(), itemFileGenerator);
    }

    protected void removeItemFileGenerator(ItemFileGenerator itemFileGenerator) {
        itemFileGenerators.remove(itemFileGenerator.getFileFormatGenerator());
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void addThingFileGenerator(ThingFileGenerator thingFileGenerator) {
        thingFileGenerators.put(thingFileGenerator.getFileFormatGenerator(), thingFileGenerator);
    }

    protected void removeThingFileGenerator(ThingFileGenerator thingFileGenerator) {
        thingFileGenerators.remove(thingFileGenerator.getFileFormatGenerator());
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
        ItemFileGenerator generator = getItemFileGenerator(acceptHeader);
        if (generator == null) {
            return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                    .entity("Unsupported media type '" + acceptHeader + "'!").build();
        }
        Collection<Item> items = null;
        if (itemNames == null || itemNames.isEmpty()) {
            items = itemRegistry.getAll();
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
        generator.generateFileFormat(outputStream, sortItems(items), getMetadata(items), hideDefaultParameters);
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
        ThingFileGenerator generator = getThingFileGenerator(acceptHeader);
        if (generator == null) {
            return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                    .entity("Unsupported media type '" + acceptHeader + "'!").build();
        }
        Collection<Thing> things = null;
        if (thingUIDs == null || thingUIDs.isEmpty()) {
            things = thingRegistry.getAll();
        } else {
            try {
                things = getThingsOrDiscoveryResult(thingUIDs);
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
            }
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        generator.generateFileFormat(outputStream, sortThings(things), hideDefaultParameters);
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
     * Sort the things in such a way:
     * - things are grouped by binding, sorted by natural order of binding name
     * - all things of a binding are sorted to follow the tree, that is bridge thing is before its sub-things
     * - all things of a binding at a certain tree depth are sorted by thing UID
     */
    private List<Thing> sortThings(Collection<Thing> things) {
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

    private @Nullable ItemFileGenerator getItemFileGenerator(String mediaType) {
        return switch (mediaType) {
            case "text/vnd.openhab.dsl.item" -> itemFileGenerators.get("DSL");
            case "application/yaml" -> itemFileGenerators.get("YAML");
            default -> null;
        };
    }

    private @Nullable ThingFileGenerator getThingFileGenerator(String mediaType) {
        return switch (mediaType) {
            case "text/vnd.openhab.dsl.thing" -> thingFileGenerators.get("DSL");
            case "application/yaml" -> thingFileGenerators.get("YAML");
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
}
