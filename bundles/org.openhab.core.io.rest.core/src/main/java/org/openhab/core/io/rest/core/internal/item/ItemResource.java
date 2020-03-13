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
package org.openhab.core.io.rest.core.internal.item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Role;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.io.rest.DTOMapper;
import org.openhab.core.io.rest.JSONResponse;
import org.openhab.core.io.rest.LocaleService;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.io.rest.Stream2JSONInputStream;
import org.openhab.core.io.rest.core.item.EnrichedGroupItemDTO;
import org.openhab.core.io.rest.core.item.EnrichedItemDTO;
import org.openhab.core.io.rest.core.item.EnrichedItemDTOMapper;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemBuilderFactory;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.ManagedItemProvider;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.items.dto.GroupItemDTO;
import org.openhab.core.items.dto.ItemDTOMapper;
import org.openhab.core.items.dto.MetadataDTO;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.library.items.RollershutterItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.UpDownType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.TypeParser;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * <p>
 * This class acts as a REST resource for items and provides different methods to interact with them, like retrieving
 * lists of items, sending commands to them or checking a single status.
 *
 * <p>
 * The typical content types are plain text for status values and XML or JSON(P) for more complex data structures
 *
 * <p>
 * This resource is registered with the Jersey servlet.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Dennis Nobel - Added methods for item management
 * @author Andre Fuechsel - Added tag support
 * @author Chris Jackson - Added method to write complete item bean
 * @author Stefan Bußweiler - Migration to new ESH event concept
 * @author Yordan Zhelev - Added Swagger annotations
 * @author Jörg Plewe - refactoring, error handling
 * @author Franck Dechavanne - Added DTOs to ApiResponses
 * @author Stefan Triller - Added bulk item add method
 */
@NonNullByDefault
@Path(ItemResource.PATH_ITEMS)
@Api(value = ItemResource.PATH_ITEMS)
@Component
public class ItemResource implements RESTResource {

    private final Logger logger = LoggerFactory.getLogger(ItemResource.class);

    /** The URI path to this resource */
    public static final String PATH_ITEMS = "items";

    @Context
    @NonNullByDefault({})
    UriInfo uriInfo;

    private @NonNullByDefault({}) ItemRegistry itemRegistry;
    private @NonNullByDefault({}) MetadataRegistry metadataRegistry;
    private @NonNullByDefault({}) EventPublisher eventPublisher;
    private @NonNullByDefault({}) ManagedItemProvider managedItemProvider;
    private @NonNullByDefault({}) DTOMapper dtoMapper;
    private @NonNullByDefault({}) MetadataSelectorMatcher metadataSelectorMatcher;
    private @NonNullByDefault({}) ItemBuilderFactory itemBuilderFactory;
    private @NonNullByDefault({}) LocaleService localeService;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    protected void unsetItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setMetadataRegistry(MetadataRegistry metadataRegistry) {
        this.metadataRegistry = metadataRegistry;
    }

    protected void unsetMetadataRegistry(MetadataRegistry metadataRegistry) {
        this.metadataRegistry = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    protected void unsetEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setManagedItemProvider(ManagedItemProvider managedItemProvider) {
        this.managedItemProvider = managedItemProvider;
    }

    protected void unsetManagedItemProvider(ManagedItemProvider managedItemProvider) {
        this.managedItemProvider = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setDTOMapper(DTOMapper dtoMapper) {
        this.dtoMapper = dtoMapper;
    }

    protected void unsetDTOMapper(DTOMapper dtoMapper) {
        this.dtoMapper = dtoMapper;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setLocaleService(LocaleService localeService) {
        this.localeService = localeService;
    }

    protected void unsetLocaleService(LocaleService localeService) {
        this.localeService = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setMetadataSelectorMatcher(MetadataSelectorMatcher metadataSelectorMatcher) {
        this.metadataSelectorMatcher = metadataSelectorMatcher;
    }

    protected void unsetMetadataSelectorMatcher(MetadataSelectorMatcher metadataSelectorMatcher) {
        this.metadataSelectorMatcher = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    public void setItemBuilderFactory(ItemBuilderFactory itemBuilderFactory) {
        this.itemBuilderFactory = itemBuilderFactory;
    }

    public void unsetItemBuilderFactory(ItemBuilderFactory itemBuilderFactory) {
        this.itemBuilderFactory = null;
    }

    @GET
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get all available items.", response = EnrichedItemDTO.class, responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = EnrichedItemDTO.class, responseContainer = "List") })
    public Response getItems(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") @Nullable String language,
            @QueryParam("type") @ApiParam(value = "item type filter", required = false) @Nullable String type,
            @QueryParam("tags") @ApiParam(value = "item tag filter", required = false) @Nullable String tags,
            @QueryParam("metadata") @ApiParam(value = "metadata selector", required = false) @Nullable String namespaceSelector,
            @DefaultValue("false") @QueryParam("recursive") @ApiParam(value = "get member items recursively", required = false) boolean recursive,
            @QueryParam("fields") @ApiParam(value = "limit output to the given fields (comma separated)", required = false) @Nullable String fields) {
        final Locale locale = localeService.getLocale(language);
        final Set<String> namespaces = splitAndFilterNamespaces(namespaceSelector, locale);
        logger.debug("Received HTTP GET request at '{}'", uriInfo.getPath());

        Stream<EnrichedItemDTO> itemStream = getItems(type, tags).stream() //
                .map(item -> EnrichedItemDTOMapper.map(item, recursive, null, uriInfo.getBaseUri(), locale)) //
                .peek(dto -> addMetadata(dto, namespaces, null)) //
                .peek(dto -> dto.editable = isEditable(dto.name));
        itemStream = dtoMapper.limitToFields(itemStream, fields);
        return Response.ok(new Stream2JSONInputStream(itemStream)).build();
    }

    @GET
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Path("/{itemname: [a-zA-Z_0-9]+}")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Gets a single item.", response = EnrichedItemDTO.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = EnrichedItemDTO.class),
            @ApiResponse(code = 404, message = "Item not found") })
    public Response getItemData(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") @Nullable String language,
            @QueryParam("metadata") @ApiParam(value = "metadata selector", required = false) @Nullable String namespaceSelector,
            @PathParam("itemname") @ApiParam(value = "item name", required = true) String itemname) {
        final Locale locale = localeService.getLocale(language);
        final Set<String> namespaces = splitAndFilterNamespaces(namespaceSelector, locale);
        logger.debug("Received HTTP GET request at '{}'", uriInfo.getPath());

        // get item
        Item item = getItem(itemname);

        // if it exists
        if (item != null) {
            logger.debug("Received HTTP GET request at '{}'.", uriInfo.getPath());
            EnrichedItemDTO dto = EnrichedItemDTOMapper.map(item, true, null, uriInfo.getBaseUri(), locale);
            addMetadata(dto, namespaces, null);
            dto.editable = isEditable(dto.name);
            return JSONResponse.createResponse(Status.OK, dto, null);
        } else {
            logger.info("Received HTTP GET request at '{}' for the unknown item '{}'.", uriInfo.getPath(), itemname);
            return getItemNotFoundResponse(itemname);
        }
    }

    private Set<String> splitAndFilterNamespaces(@Nullable String namespaceSelector, Locale locale) {
        return metadataSelectorMatcher.filterNamespaces(namespaceSelector, locale);
    }

    /**
     *
     * @param itemname
     * @return
     */
    @GET
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Path("/{itemname: [a-zA-Z_0-9]+}/state")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Gets the state of an item.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
            @ApiResponse(code = 404, message = "Item not found") })
    public Response getPlainItemState(
            @PathParam("itemname") @ApiParam(value = "item name", required = true) String itemname) {
        // get item
        Item item = getItem(itemname);

        // if it exists
        if (item != null) {
            logger.debug("Received HTTP GET request at '{}'.", uriInfo.getPath());

            // we cannot use JSONResponse.createResponse() bc. MediaType.TEXT_PLAIN
            // return JSONResponse.createResponse(Status.OK, item.getState().toString(), null);
            return Response.ok(item.getState().toFullString()).build();
        } else {
            logger.info("Received HTTP GET request at '{}' for the unknown item '{}'.", uriInfo.getPath(), itemname);
            return getItemNotFoundResponse(itemname);
        }
    }

    @PUT
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Path("/{itemname: [a-zA-Z_0-9]+}/state")
    @Consumes(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Updates the state of an item.")
    @ApiResponses(value = { @ApiResponse(code = 202, message = "Accepted"),
            @ApiResponse(code = 404, message = "Item not found"),
            @ApiResponse(code = 400, message = "Item state null") })
    public Response putItemState(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") String language,
            @PathParam("itemname") @ApiParam(value = "item name", required = true) String itemname,
            @ApiParam(value = "valid item state (e.g. ON, OFF)", required = true) String value) {
        final Locale locale = localeService.getLocale(language);

        // get Item
        Item item = getItem(itemname);

        // if Item exists
        if (item != null) {
            // try to parse a State from the input
            State state = TypeParser.parseState(item.getAcceptedDataTypes(), value);

            if (state != null) {
                // set State and report OK
                logger.debug("Received HTTP PUT request at '{}' with value '{}'.", uriInfo.getPath(), value);
                eventPublisher.post(ItemEventFactory.createStateEvent(itemname, state));
                return getItemResponse(Status.ACCEPTED, null, locale, null);
            } else {
                // State could not be parsed
                logger.warn("Received HTTP PUT request at '{}' with an invalid status value '{}'.", uriInfo.getPath(),
                        value);
                return JSONResponse.createErrorResponse(Status.BAD_REQUEST, "State could not be parsed: " + value);
            }
        } else {
            // Item does not exist
            logger.info("Received HTTP PUT request at '{}' for the unknown item '{}'.", uriInfo.getPath(), itemname);
            return getItemNotFoundResponse(itemname);
        }
    }

    @POST
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Path("/{itemname: [a-zA-Z_0-9]+}")
    @Consumes(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Sends a command to an item.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Item not found"),
            @ApiResponse(code = 400, message = "Item command null") })
    public Response postItemCommand(
            @PathParam("itemname") @ApiParam(value = "item name", required = true) String itemname,
            @ApiParam(value = "valid item command (e.g. ON, OFF, UP, DOWN, REFRESH)", required = true) String value) {
        Item item = getItem(itemname);
        Command command = null;
        if (item != null) {
            if ("toggle".equalsIgnoreCase(value) && (item instanceof SwitchItem || item instanceof RollershutterItem)) {
                if (OnOffType.ON.equals(item.getStateAs(OnOffType.class))) {
                    command = OnOffType.OFF;
                }
                if (OnOffType.OFF.equals(item.getStateAs(OnOffType.class))) {
                    command = OnOffType.ON;
                }
                if (UpDownType.UP.equals(item.getStateAs(UpDownType.class))) {
                    command = UpDownType.DOWN;
                }
                if (UpDownType.DOWN.equals(item.getStateAs(UpDownType.class))) {
                    command = UpDownType.UP;
                }
            } else {
                command = TypeParser.parseCommand(item.getAcceptedCommandTypes(), value);
            }
            if (command != null) {
                logger.debug("Received HTTP POST request at '{}' with value '{}'.", uriInfo.getPath(), value);
                eventPublisher.post(ItemEventFactory.createCommandEvent(itemname, command));
                ResponseBuilder resbuilder = Response.ok();
                resbuilder.type(MediaType.TEXT_PLAIN);
                return resbuilder.build();
            } else {
                logger.warn("Received HTTP POST request at '{}' with an invalid status value '{}'.", uriInfo.getPath(),
                        value);
                return Response.status(Status.BAD_REQUEST).build();
            }
        } else {
            logger.info("Received HTTP POST request at '{}' for the unknown item '{}'.", uriInfo.getPath(), itemname);
            throw new WebApplicationException(404);
        }
    }

    @PUT
    @RolesAllowed({ Role.ADMIN })
    @Path("/{itemName: [a-zA-Z_0-9]+}/members/{memberItemName: [a-zA-Z_0-9]+}")
    @ApiOperation(value = "Adds a new member to a group item.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Item or member item not found or item is not of type group item."),
            @ApiResponse(code = 405, message = "Member item is not editable.") })
    public Response addMember(@PathParam("itemName") @ApiParam(value = "item name", required = true) String itemName,
            @PathParam("memberItemName") @ApiParam(value = "member item name", required = true) String memberItemName) {
        try {
            Item item = itemRegistry.getItem(itemName);

            if (!(item instanceof GroupItem)) {
                return Response.status(Status.NOT_FOUND).build();
            }

            GroupItem groupItem = (GroupItem) item;

            Item memberItem = itemRegistry.getItem(memberItemName);

            if (!(memberItem instanceof GenericItem)) {
                return Response.status(Status.NOT_FOUND).build();
            }

            if (managedItemProvider.get(memberItemName) == null) {
                return Response.status(Status.METHOD_NOT_ALLOWED).build();
            }

            GenericItem genericMemberItem = (GenericItem) memberItem;
            genericMemberItem.addGroupName(groupItem.getName());
            managedItemProvider.update(genericMemberItem);

            return Response.ok(null, MediaType.TEXT_PLAIN).build();
        } catch (ItemNotFoundException e) {
            return Response.status(Status.NOT_FOUND).build();
        }
    }

    @DELETE
    @RolesAllowed({ Role.ADMIN })
    @Path("/{itemName: [a-zA-Z_0-9]+}/members/{memberItemName: [a-zA-Z_0-9]+}")
    @ApiOperation(value = "Removes an existing member from a group item.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Item or member item not found or item is not of type group item."),
            @ApiResponse(code = 405, message = "Member item is not editable.") })
    public Response removeMember(@PathParam("itemName") @ApiParam(value = "item name", required = true) String itemName,
            @PathParam("memberItemName") @ApiParam(value = "member item name", required = true) String memberItemName) {
        try {
            Item item = itemRegistry.getItem(itemName);

            if (!(item instanceof GroupItem)) {
                return Response.status(Status.NOT_FOUND).build();
            }

            GroupItem groupItem = (GroupItem) item;

            Item memberItem = itemRegistry.getItem(memberItemName);

            if (!(memberItem instanceof GenericItem)) {
                return Response.status(Status.NOT_FOUND).build();
            }

            if (managedItemProvider.get(memberItemName) == null) {
                return Response.status(Status.METHOD_NOT_ALLOWED).build();
            }

            GenericItem genericMemberItem = (GenericItem) memberItem;
            genericMemberItem.removeGroupName(groupItem.getName());
            managedItemProvider.update(genericMemberItem);

            return Response.ok(null, MediaType.TEXT_PLAIN).build();
        } catch (ItemNotFoundException e) {
            return Response.status(Status.NOT_FOUND).build();
        }
    }

    @DELETE
    @RolesAllowed({ Role.ADMIN })
    @Path("/{itemname: [a-zA-Z_0-9]+}")
    @ApiOperation(value = "Removes an item from the registry.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Item not found or item is not editable.") })
    public Response removeItem(@PathParam("itemname") @ApiParam(value = "item name", required = true) String itemname) {
        if (managedItemProvider.remove(itemname) == null) {
            logger.info("Received HTTP DELETE request at '{}' for the unknown item '{}'.", uriInfo.getPath(), itemname);
            return Response.status(Status.NOT_FOUND).build();
        }
        return Response.ok(null, MediaType.TEXT_PLAIN).build();
    }

    @PUT
    @RolesAllowed({ Role.ADMIN })
    @Path("/{itemname: [a-zA-Z_0-9]+}/tags/{tag}")
    @ApiOperation(value = "Adds a tag to an item.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Item not found."),
            @ApiResponse(code = 405, message = "Item not editable.") })
    public Response addTag(@PathParam("itemname") @ApiParam(value = "item name", required = true) String itemname,
            @PathParam("tag") @ApiParam(value = "tag", required = true) String tag) {
        Item item = getItem(itemname);

        if (item == null) {
            logger.info("Received HTTP PUT request at '{}' for the unknown item '{}'.", uriInfo.getPath(), itemname);
            return Response.status(Status.NOT_FOUND).build();
        }

        if (managedItemProvider.get(itemname) == null) {
            return Response.status(Status.METHOD_NOT_ALLOWED).build();
        }

        ((GenericItem) item).addTag(tag);
        managedItemProvider.update(item);

        return Response.ok(null, MediaType.TEXT_PLAIN).build();
    }

    @DELETE
    @RolesAllowed({ Role.ADMIN })
    @Path("/{itemname: [a-zA-Z_0-9]+}/tags/{tag}")
    @ApiOperation(value = "Removes a tag from an item.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Item not found."),
            @ApiResponse(code = 405, message = "Item not editable.") })
    public Response removeTag(@PathParam("itemname") @ApiParam(value = "item name", required = true) String itemname,
            @PathParam("tag") @ApiParam(value = "tag", required = true) String tag) {
        Item item = getItem(itemname);

        if (item == null) {
            logger.info("Received HTTP DELETE request at '{}' for the unknown item '{}'.", uriInfo.getPath(), itemname);
            return Response.status(Status.NOT_FOUND).build();
        }

        if (managedItemProvider.get(itemname) == null) {
            return Response.status(Status.METHOD_NOT_ALLOWED).build();
        }

        ((GenericItem) item).removeTag(tag);
        managedItemProvider.update(item);

        return Response.ok(null, MediaType.TEXT_PLAIN).build();
    }

    @PUT
    @RolesAllowed({ Role.ADMIN })
    @Path("/{itemname: [a-zA-Z_0-9]+}/metadata/{namespace}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Adds metadata to an item.")
    @ApiResponses(value = { //
            @ApiResponse(code = 200, message = "OK"), //
            @ApiResponse(code = 201, message = "Created"), //
            @ApiResponse(code = 400, message = "Metadata value empty."), //
            @ApiResponse(code = 404, message = "Item not found."), //
            @ApiResponse(code = 405, message = "Metadata not editable.") })
    public Response addMetadata(@PathParam("itemname") @ApiParam(value = "item name", required = true) String itemname,
            @PathParam("namespace") @ApiParam(value = "namespace", required = true) String namespace,
            @ApiParam(value = "metadata", required = true) MetadataDTO metadata) {
        Item item = getItem(itemname);

        if (item == null) {
            logger.info("Received HTTP PUT request at '{}' for the unknown item '{}'.", uriInfo.getPath(), itemname);
            return Response.status(Status.NOT_FOUND).build();
        }

        String value = metadata.value;
        if (value == null || value.isEmpty()) {
            logger.info("Received HTTP PUT request at '{}' for item '{}' with empty metadata.", uriInfo.getPath(),
                    itemname);
            return Response.status(Status.BAD_REQUEST).build();
        }

        MetadataKey key = new MetadataKey(namespace, itemname);
        Metadata md = new Metadata(key, value, metadata.config);
        if (metadataRegistry.get(key) == null) {
            metadataRegistry.add(md);
            return Response.status(Status.CREATED).type(MediaType.TEXT_PLAIN).build();
        } else {
            metadataRegistry.update(md);
            return Response.ok(null, MediaType.TEXT_PLAIN).build();
        }

    }

    @DELETE
    @RolesAllowed({ Role.ADMIN })
    @Path("/{itemname: [a-zA-Z_0-9]+}/metadata/{namespace}")
    @ApiOperation(value = "Removes metadata from an item.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Item not found."),
            @ApiResponse(code = 405, message = "Meta data not editable.") })
    public Response removeMetadata(
            @PathParam("itemname") @ApiParam(value = "item name", required = true) String itemname,
            @PathParam("namespace") @ApiParam(value = "namespace", required = true) String namespace) {
        Item item = getItem(itemname);

        if (item == null) {
            logger.info("Received HTTP DELETE request at '{}' for the unknown item '{}'.", uriInfo.getPath(), itemname);
            return Response.status(Status.NOT_FOUND).build();
        }

        MetadataKey key = new MetadataKey(namespace, itemname);
        if (metadataRegistry.get(key) != null) {
            if (metadataRegistry.remove(key) == null) {
                logger.info("Received HTTP DELETE request at '{}' for unmanaged item meta-data '{}'.",
                        uriInfo.getPath(), key);
                return Response.status(Status.CONFLICT).build();
            }
        } else {
            logger.info("Received HTTP DELETE request at '{}' for unknown item meta-data '{}'.", uriInfo.getPath(),
                    key);
            return Response.status(Status.NOT_FOUND).build();
        }

        return Response.ok(null, MediaType.TEXT_PLAIN).build();
    }

    /**
     * Create or Update an item by supplying an item bean.
     *
     * @param itemname
     * @param item the item bean.
     * @return
     */
    @PUT
    @RolesAllowed({ Role.ADMIN })
    @Path("/{itemname: [a-zA-Z_0-9]+}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Adds a new item to the registry or updates the existing item.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
            @ApiResponse(code = 201, message = "Item created."), @ApiResponse(code = 400, message = "Item null."),
            @ApiResponse(code = 404, message = "Item not found."),
            @ApiResponse(code = 405, message = "Item not editable.") })
    public Response createOrUpdateItem(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") String language,
            @PathParam("itemname") @ApiParam(value = "item name", required = true) String itemname,
            @ApiParam(value = "item data", required = true) @Nullable GroupItemDTO item) {
        final Locale locale = localeService.getLocale(language);

        // If we didn't get an item bean, then return!
        if (item == null) {
            return Response.status(Status.BAD_REQUEST).build();
        }

        Item newItem = ItemDTOMapper.map(item, itemBuilderFactory);
        if (newItem == null) {
            logger.warn("Received HTTP PUT request at '{}' with an invalid item type '{}'.", uriInfo.getPath(),
                    item.type);
            return Response.status(Status.BAD_REQUEST).build();
        }

        // Save the item
        if (getItem(itemname) == null) {
            // item does not yet exist, create it
            managedItemProvider.add(newItem);
            return getItemResponse(Status.CREATED, itemRegistry.get(itemname), locale, null);
        } else if (managedItemProvider.get(itemname) != null) {
            // item already exists as a managed item, update it
            managedItemProvider.update(newItem);
            return getItemResponse(Status.OK, itemRegistry.get(itemname), locale, null);
        } else {
            // Item exists but cannot be updated
            logger.warn("Cannot update existing item '{}', because is not managed.", itemname);
            return JSONResponse.createErrorResponse(Status.METHOD_NOT_ALLOWED,
                    "Cannot update non-managed Item " + itemname);
        }
    }

    /**
     * Create or Update a list of items by supplying a list of item beans.
     *
     * @param items the list of item beans.
     * @return array of status information for each item bean
     */
    @PUT
    @RolesAllowed({ Role.ADMIN })
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Adds a list of items to the registry or updates the existing items.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
            @ApiResponse(code = 400, message = "Item list is null.") })
    public Response createOrUpdateItems(
            @ApiParam(value = "array of item data", required = true) GroupItemDTO @Nullable [] items) {
        // If we didn't get an item list bean, then return!
        if (items == null) {
            return Response.status(Status.BAD_REQUEST).build();
        }

        List<GroupItemDTO> wrongTypes = new ArrayList<>();
        List<Item> activeItems = new ArrayList<>();
        Map<String, Collection<String>> tagMap = new HashMap<>();

        for (GroupItemDTO item : items) {
            Item newItem = ItemDTOMapper.map(item, itemBuilderFactory);
            if (newItem == null) {
                wrongTypes.add(item);
                tagMap.put(item.name, item.tags);
            } else {
                activeItems.add(newItem);
            }
        }

        List<Item> createdItems = new ArrayList<>();
        List<Item> updatedItems = new ArrayList<>();
        List<Item> failedItems = new ArrayList<>();

        for (Item activeItem : activeItems) {
            String itemName = activeItem.getName();
            if (getItem(itemName) == null) {
                // item does not yet exist, create it
                managedItemProvider.add(activeItem);
                createdItems.add(activeItem);
            } else if (managedItemProvider.get(itemName) != null) {
                // item already exists as a managed item, update it
                managedItemProvider.update(activeItem);
                updatedItems.add(activeItem);
            } else {
                // Item exists but cannot be updated
                logger.warn("Cannot update existing item '{}', because it is not managed.", itemName);
                failedItems.add(activeItem);
            }
        }

        // build response
        List<JsonObject> responseList = new ArrayList<>();

        for (GroupItemDTO item : wrongTypes) {
            responseList.add(buildStatusObject(item.name, "error", "Received HTTP PUT request at '" + uriInfo.getPath()
                    + "' with an invalid item type '" + item.type + "'."));
        }
        for (Item item : failedItems) {
            responseList.add(buildStatusObject(item.getName(), "error", "Cannot update non-managed item"));
        }
        for (Item item : createdItems) {
            responseList.add(buildStatusObject(item.getName(), "created", null));
        }
        for (Item item : updatedItems) {
            responseList.add(buildStatusObject(item.getName(), "updated", null));
        }

        return JSONResponse.createResponse(Status.OK, responseList, null);
    }

    private JsonObject buildStatusObject(String itemName, String status, @Nullable String message) {
        JsonObject jo = new JsonObject();
        jo.addProperty("name", itemName);
        jo.addProperty("status", status);
        jo.addProperty("message", message);
        return jo;
    }

    /**
     * helper: Response to be sent to client if a Thing cannot be found
     *
     * @param thingUID
     * @return Response configured for 'item not found'
     */
    private static Response getItemNotFoundResponse(String itemname) {
        String message = "Item " + itemname + " does not exist!";
        return JSONResponse.createResponse(Status.NOT_FOUND, null, message);
    }

    /**
     * Prepare a response representing the Item depending in the status.
     *
     * @param status
     * @param item can be null
     * @param locale the locale
     * @param errormessage optional message in case of error
     * @return Response configured to represent the Item in depending on the status
     */
    private Response getItemResponse(Status status, @Nullable Item item, Locale locale, @Nullable String errormessage) {
        Object entity = null != item ? EnrichedItemDTOMapper.map(item, true, null, uriInfo.getBaseUri(), locale) : null;
        return JSONResponse.createResponse(status, entity, errormessage);
    }

    /**
     * convenience shortcut
     *
     * @param itemname
     * @return Item addressed by itemname
     */
    private @Nullable Item getItem(String itemname) {
        return itemRegistry.get(itemname);
    }

    private Collection<Item> getItems(@Nullable String type, @Nullable String tags) {
        Collection<Item> items;
        if (tags == null) {
            if (type == null) {
                items = itemRegistry.getItems();
            } else {
                items = itemRegistry.getItemsOfType(type);
            }
        } else {
            String[] tagList = tags.split(",");
            if (type == null) {
                items = itemRegistry.getItemsByTag(tagList);
            } else {
                items = itemRegistry.getItemsByTagAndType(type, tagList);
            }
        }

        return items;
    }

    private void addMetadata(EnrichedItemDTO dto, Set<String> namespaces, @Nullable Predicate<Metadata> filter) {
        Map<String, Object> metadata = new HashMap<>();
        for (String namespace : namespaces) {
            MetadataKey key = new MetadataKey(namespace, dto.name);
            Metadata md = metadataRegistry.get(key);
            if (md != null && (filter == null || filter.test(md))) {
                MetadataDTO mdDto = new MetadataDTO();
                mdDto.value = md.getValue();
                mdDto.config = md.getConfiguration().isEmpty() ? null : md.getConfiguration();
                metadata.put(namespace, mdDto);
            }
        }
        if (dto instanceof EnrichedGroupItemDTO) {
            for (EnrichedItemDTO member : ((EnrichedGroupItemDTO) dto).members) {
                addMetadata(member, namespaces, filter);
            }
        }
        if (!metadata.isEmpty()) {
            // we only set it in the dto if there is really data available
            dto.metadata = metadata;
        }
    }

    private boolean isEditable(String itemName) {
        return managedItemProvider.get(itemName) != null;
    }

    @Override
    public boolean isSatisfied() {
        return itemRegistry != null && managedItemProvider != null && eventPublisher != null
                && itemBuilderFactory != null && dtoMapper != null && metadataRegistry != null
                && metadataSelectorMatcher != null && localeService != null;
    }
}
