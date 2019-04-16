/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.io.rest.core.internal.item;

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
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotFoundException;
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
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.auth.Role;
import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.items.GroupItem;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemBuilderFactory;
import org.eclipse.smarthome.core.items.ItemNotFoundException;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.items.ManagedItemProvider;
import org.eclipse.smarthome.core.items.Metadata;
import org.eclipse.smarthome.core.items.MetadataKey;
import org.eclipse.smarthome.core.items.MetadataRegistry;
import org.eclipse.smarthome.core.items.dto.GroupItemDTO;
import org.eclipse.smarthome.core.items.dto.ItemDTOMapper;
import org.eclipse.smarthome.core.items.dto.MetadataDTO;
import org.eclipse.smarthome.core.items.events.ItemEventFactory;
import org.eclipse.smarthome.core.library.items.RollershutterItem;
import org.eclipse.smarthome.core.library.items.SwitchItem;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.UpDownType;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.TypeParser;
import org.eclipse.smarthome.io.rest.DTOMapper;
import org.eclipse.smarthome.io.rest.LocaleService;
import org.eclipse.smarthome.io.rest.RESTService;
import org.eclipse.smarthome.io.rest.core.item.EnrichedGroupItemDTO;
import org.eclipse.smarthome.io.rest.core.item.EnrichedItemDTO;
import org.eclipse.smarthome.io.rest.core.item.EnrichedItemDTOMapper;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * @author Kai Kreuzer - Initial contribution and API
 * @author Dennis Nobel - Added methods for item management
 * @author Andre Fuechsel - Added tag support
 * @author Chris Jackson - Added method to write complete item bean
 * @author Stefan Bußweiler - Migration to new ESH event concept
 * @author Yordan Zhelev - Added Swagger annotations
 * @author Jörg Plewe - refactoring, error handling
 * @author Franck Dechavanne - Added DTOs to ApiResponses
 * @author Stefan Triller - Added bulk item add method
 */
@Path(ItemResource.PATH_ITEMS)
@Api(value = ItemResource.PATH_ITEMS)
@Produces(MediaType.APPLICATION_JSON)
@Component(immediate = true)
@JaxrsApplicationSelect("(osgi.jaxrs.name=" + RESTService.REST_APP_NAME + ")")
@JaxrsResource
@NonNullByDefault
public class ItemResource {

    private final Logger logger = LoggerFactory.getLogger(ItemResource.class);

    /** The URI path to this resource */
    public static final String PATH_ITEMS = "items";

    @NonNullByDefault({})
    @Context
    UriInfo uriInfo;

    @Reference
    private @NonNullByDefault({}) ItemRegistry itemRegistry;

    @Reference
    private @NonNullByDefault({}) MetadataRegistry metadataRegistry;

    @Reference
    private @NonNullByDefault({}) EventPublisher eventPublisher;

    @Reference
    private @NonNullByDefault({}) ManagedItemProvider managedItemProvider;

    @Reference
    private @NonNullByDefault({}) DTOMapper dtoMapper;

    @Reference
    private @NonNullByDefault({}) MetadataSelectorMatcher metadataSelectorMatcher;

    @Reference
    private @NonNullByDefault({}) ItemBuilderFactory itemBuilderFactory;

    @Reference
    private @NonNullByDefault({}) LocaleService localeService;

    @GET
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get all available items.", response = EnrichedItemDTO.class, responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = EnrichedItemDTO.class, responseContainer = "List") })
    public Stream<EnrichedItemDTO> getItems(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") @Nullable String language,
            @QueryParam("type") @ApiParam(value = "item type filter", required = false) @Nullable String type,
            @QueryParam("tags") @ApiParam(value = "item tag filter", required = false) @Nullable String tags,
            @QueryParam("metadata") @ApiParam(value = "metadata selector", required = false) @Nullable String namespaceSelector,
            @DefaultValue("false") @QueryParam("recursive") @ApiParam(value = "get member items recursively", required = false) boolean recursive,
            @QueryParam("fields") @ApiParam(value = "limit output to the given fields (comma separated)", required = false) @Nullable String fields) {
        final Locale locale = localeService.getLocale(language);
        final Set<String> namespaces = metadataSelectorMatcher.filterNamespaces(namespaceSelector, locale);

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

        Stream<EnrichedItemDTO> itemStream = items.stream() //
                .map(item -> EnrichedItemDTOMapper.map(item, recursive, null, uriInfo.getBaseUri(), locale)) //
                .peek(dto -> addMetadata(dto, namespaces, null)) //
                .peek(dto -> dto.editable = managedItemProvider.get(dto.name) != null);
        if (fields == null || fields.isEmpty()) {
            return itemStream;
        }
        return DTOMapper.limitToFields(itemStream, fields);
    }

    @GET
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Path("/{itemname: [a-zA-Z_0-9]*}")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Gets a single item.", response = EnrichedItemDTO.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = EnrichedItemDTO.class),
            @ApiResponse(code = 404, message = "Item not found") })
    public EnrichedItemDTO getItemData(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") String language,
            @QueryParam("metadata") @ApiParam(value = "metadata selector", required = false) @Nullable String namespaceSelector,
            @PathParam("itemname") @ApiParam(value = "item name", required = true) String itemname) {
        final Locale locale = localeService.getLocale(language);
        final Set<String> namespaces = metadataSelectorMatcher.filterNamespaces(namespaceSelector, locale);

        Item item = itemRegistry.get(itemname);
        if (item == null) {
            throw new NotFoundException();
        }

        EnrichedItemDTO dto = EnrichedItemDTOMapper.map(item, true, null, uriInfo.getBaseUri(), locale);
        addMetadata(dto, namespaces, null);
        dto.editable = managedItemProvider.get(dto.name) != null;
        return dto;
    }

    @GET
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Path("/{itemname: [a-zA-Z_0-9]*}/state")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Gets the state of an item.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
            @ApiResponse(code = 404, message = "Item not found") })
    public String getPlainItemState(
            @PathParam("itemname") @ApiParam(value = "item name", required = true) String itemname) {
        Item item = itemRegistry.get(itemname);
        if (item == null) {
            throw new NotFoundException();
        }
        return item.getState().toFullString();
    }

    @PUT
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Path("/{itemname: [a-zA-Z_0-9]*}/state")
    @Consumes(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Updates the state of an item.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Item not found"),
            @ApiResponse(code = 400, message = "Item state null") })
    public void putItemState(@PathParam("itemname") @ApiParam(value = "item name", required = true) String itemname,
            @ApiParam(value = "valid item state (e.g. ON, OFF)", required = true) String value) {

        Item item = itemRegistry.get(itemname);
        if (item == null) {
            throw new NotFoundException();
        }

        State state = TypeParser.parseState(item.getAcceptedDataTypes(), value);
        if (state == null) {
            throw new BadRequestException("State could not be parsed: " + value);
        }

        eventPublisher.post(ItemEventFactory.createStateEvent(itemname, state));
    }

    @POST
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Path("/{itemname: [a-zA-Z_0-9]*}")
    @Consumes(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Sends a command to an item.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Item not found"),
            @ApiResponse(code = 400, message = "Item command null") })
    public void postItemCommand(@PathParam("itemname") @ApiParam(value = "item name", required = true) String itemname,
            @ApiParam(value = "valid item command (e.g. ON, OFF, UP, DOWN, REFRESH)", required = true) String value) {
        Item item = itemRegistry.get(itemname);
        Command command = null;
        if (item == null) {
            throw new NotFoundException();
        }
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
        if (command == null) {
            throw new BadRequestException();
        }

        eventPublisher.post(ItemEventFactory.createCommandEvent(itemname, command));
    }

    @PUT
    @RolesAllowed({ Role.ADMIN })
    @Path("/{itemName: [a-zA-Z_0-9]*}/members/{memberItemName: [a-zA-Z_0-9]*}")
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
    @Path("/{itemName: [a-zA-Z_0-9]*}/members/{memberItemName: [a-zA-Z_0-9]*}")
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
    @Path("/{itemname: [a-zA-Z_0-9]*}")
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
    @Path("/{itemname: [a-zA-Z_0-9]*}/tags/{tag}")
    @ApiOperation(value = "Adds a tag to an item.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Item not found."),
            @ApiResponse(code = 405, message = "Item not editable.") })
    public Response addTag(@PathParam("itemname") @ApiParam(value = "item name", required = true) String itemname,
            @PathParam("tag") @ApiParam(value = "tag", required = true) String tag) {
        Item item = itemRegistry.get(itemname);

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
    @Path("/{itemname: [a-zA-Z_0-9]*}/tags/{tag}")
    @ApiOperation(value = "Removes a tag from an item.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Item not found."),
            @ApiResponse(code = 405, message = "Item not editable.") })
    public Response removeTag(@PathParam("itemname") @ApiParam(value = "item name", required = true) String itemname,
            @PathParam("tag") @ApiParam(value = "tag", required = true) String tag) {
        Item item = itemRegistry.get(itemname);

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
    @Path("/{itemname: [a-zA-Z_0-9]*}/metadata/{namespace}")
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

        Item item = itemRegistry.get(itemname);

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
    @Path("/{itemname: [a-zA-Z_0-9]*}/metadata/{namespace}")
    @ApiOperation(value = "Removes metadata from an item.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Item not found."),
            @ApiResponse(code = 405, message = "Meta data not editable.") })
    public void removeMetadata(@PathParam("itemname") @ApiParam(value = "item name", required = true) String itemname,
            @PathParam("namespace") @ApiParam(value = "namespace", required = true) String namespace) {

        Item item = itemRegistry.get(itemname);

        if (item == null) {
            throw new NotFoundException();
        }

        MetadataKey key = new MetadataKey(namespace, itemname);
        if (metadataRegistry.get(key) != null) {
            if (metadataRegistry.remove(key) == null) {
                throw new WebApplicationException(409);
            }
        } else {
            throw new NotFoundException();
        }
    }

    @PUT
    @RolesAllowed({ Role.ADMIN })
    @Path("/{itemname: [a-zA-Z_0-9]*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Adds a new item to the registry or updates the existing item.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
            @ApiResponse(code = 201, message = "Item created."), @ApiResponse(code = 400, message = "Item null."),
            @ApiResponse(code = 404, message = "Item not found."),
            @ApiResponse(code = 405, message = "Item not editable.") })
    public EnrichedItemDTO createOrUpdateItem(@Context HttpServletResponse response,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") String language,
            @PathParam("itemname") @ApiParam(value = "item name", required = true) String itemname,
            @ApiParam(value = "item data", required = true) GroupItemDTO item) {
        final Locale locale = localeService.getLocale(language);

        Item newItem = ItemDTOMapper.map(item, itemBuilderFactory);
        if (newItem == null) {
            throw new BadRequestException();
        }

        if (managedItemProvider.get(itemname) != null) {
            // item already exists as a managed item, update it
            managedItemProvider.update(newItem);
            return EnrichedItemDTOMapper.map(itemRegistry.get(itemname), true, null, uriInfo.getBaseUri(), locale);
        } else if (itemRegistry.get(itemname) == null) {
            // item does not yet exist, create it
            managedItemProvider.add(newItem);
            response.setStatus(Response.Status.CREATED.getStatusCode());
            return EnrichedItemDTOMapper.map(itemRegistry.get(itemname), true, null, uriInfo.getBaseUri(), locale);
        } else {
            throw new NotAllowedException("Cannot update non-managed Item " + itemname);
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
    public Collection<?> createOrUpdateItems(
            @ApiParam(value = "array of item data", required = true) GroupItemDTO @Nullable [] items) {
        // If we didn't get an item list bean, then return!
        if (items == null) {
            throw new BadRequestException();
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
            if (itemRegistry.get(itemName) == null) {
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
        List<StatusObjectDTO> responseList = new ArrayList<>();

        for (GroupItemDTO item : wrongTypes) {
            responseList.add(new StatusObjectDTO(item.name, "error", "Received HTTP PUT request at '"
                    + uriInfo.getPath() + "' with an invalid item type '" + item.type + "'."));
        }
        for (Item item : failedItems) {
            responseList.add(new StatusObjectDTO(item.getName(), "error", "Cannot update non-managed item"));
        }
        for (Item item : createdItems) {
            responseList.add(new StatusObjectDTO(item.getName(), "created", null));
        }
        for (Item item : updatedItems) {
            responseList.add(new StatusObjectDTO(item.getName(), "updated", null));
        }

        return responseList;
    }

    @SuppressWarnings("unused")
    private static class StatusObjectDTO {
        public final String name;
        public final String status;
        public final @Nullable String message;

        public StatusObjectDTO(String name, String status, @Nullable String message) {
            this.name = name;
            this.status = status;
            this.message = message;
        }
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
}
