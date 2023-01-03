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
package org.openhab.core.io.rest.core.internal.item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Role;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.io.rest.DTOMapper;
import org.openhab.core.io.rest.JSONResponse;
import org.openhab.core.io.rest.LocaleService;
import org.openhab.core.io.rest.RESTConstants;
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
import org.openhab.core.library.types.RawType;
import org.openhab.core.library.types.UpDownType;
import org.openhab.core.semantics.SemanticTags;
import org.openhab.core.semantics.SemanticsPredicates;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.TypeParser;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JSONRequired;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

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
 * @author Markus Rathgeb - Migrated to JAX-RS Whiteboard Specification
 * @author Wouter Born - Migrated to OpenAPI annotations
 */
@Component
@JaxrsResource
@JaxrsName(ItemResource.PATH_ITEMS)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(ItemResource.PATH_ITEMS)
@Tag(name = ItemResource.PATH_ITEMS)
@NonNullByDefault
public class ItemResource implements RESTResource {

    /** The URI path to this resource */
    public static final String PATH_ITEMS = "items";

    /**
     * Replaces part of the URI builder by forwarded headers.
     *
     * @param uriBuilder the URI builder
     * @param httpHeaders the HTTP headers
     */
    private static void respectForwarded(final UriBuilder uriBuilder, final @Context HttpHeaders httpHeaders) {
        Optional.ofNullable(httpHeaders.getHeaderString("X-Forwarded-Host")).ifPresent(host -> {
            final int pos1 = host.indexOf("[");
            final int pos2 = host.indexOf("]");
            final String hostWithIpv6 = (pos1 >= 0 && pos2 > pos1) ? host.substring(pos1, pos2 + 1) : null;
            final String[] parts = hostWithIpv6 == null ? host.split(":") : host.substring(pos2 + 1).split(":");
            uriBuilder.host(hostWithIpv6 != null ? hostWithIpv6 : parts[0]);
            if (parts.length > 1) {
                uriBuilder.port(Integer.parseInt(parts[1]));
            }
        });
        Optional.ofNullable(httpHeaders.getHeaderString("X-Forwarded-Proto")).ifPresent(uriBuilder::scheme);
    }

    private final Logger logger = LoggerFactory.getLogger(ItemResource.class);

    private final DTOMapper dtoMapper;
    private final EventPublisher eventPublisher;
    private final ItemBuilderFactory itemBuilderFactory;
    private final ItemRegistry itemRegistry;
    private final LocaleService localeService;
    private final ManagedItemProvider managedItemProvider;
    private final MetadataRegistry metadataRegistry;
    private final MetadataSelectorMatcher metadataSelectorMatcher;

    @Activate
    public ItemResource(//
            final @Reference DTOMapper dtoMapper, //
            final @Reference EventPublisher eventPublisher, //
            final @Reference ItemBuilderFactory itemBuilderFactory, //
            final @Reference ItemRegistry itemRegistry, //
            final @Reference LocaleService localeService, //
            final @Reference ManagedItemProvider managedItemProvider,
            final @Reference MetadataRegistry metadataRegistry,
            final @Reference MetadataSelectorMatcher metadataSelectorMatcher) {
        this.dtoMapper = dtoMapper;
        this.eventPublisher = eventPublisher;
        this.itemBuilderFactory = itemBuilderFactory;
        this.itemRegistry = itemRegistry;
        this.localeService = localeService;
        this.managedItemProvider = managedItemProvider;
        this.metadataRegistry = metadataRegistry;
        this.metadataSelectorMatcher = metadataSelectorMatcher;
    }

    private UriBuilder uriBuilder(final UriInfo uriInfo, final HttpHeaders httpHeaders) {
        final UriBuilder uriBuilder = uriInfo.getBaseUriBuilder().path(PATH_ITEMS).path("{itemName}");
        respectForwarded(uriBuilder, httpHeaders);
        return uriBuilder;
    }

    @GET
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getItems", summary = "Get all available items.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = EnrichedItemDTO.class)))) })
    public Response getItems(final @Context UriInfo uriInfo, final @Context HttpHeaders httpHeaders,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @QueryParam("type") @Parameter(description = "item type filter") @Nullable String type,
            @QueryParam("tags") @Parameter(description = "item tag filter") @Nullable String tags,
            @QueryParam("metadata") @Parameter(description = "metadata selector - a comma separated list or a regular expression (suppressed if no value given)") @Nullable String namespaceSelector,
            @DefaultValue("false") @QueryParam("recursive") @Parameter(description = "get member items recursively") boolean recursive,
            @QueryParam("fields") @Parameter(description = "limit output to the given fields (comma separated)") @Nullable String fields) {
        final Locale locale = localeService.getLocale(language);
        final Set<String> namespaces = splitAndFilterNamespaces(namespaceSelector, locale);

        final UriBuilder uriBuilder = uriBuilder(uriInfo, httpHeaders);

        Stream<EnrichedItemDTO> itemStream = getItems(type, tags).stream() //
                .map(item -> EnrichedItemDTOMapper.map(item, recursive, null, uriBuilder, locale)) //
                .peek(dto -> addMetadata(dto, namespaces, null)) //
                .peek(dto -> dto.editable = isEditable(dto.name));
        itemStream = dtoMapper.limitToFields(itemStream, fields);
        return Response.ok(new Stream2JSONInputStream(itemStream)).build();
    }

    /**
     *
     * @param itemname name of the item
     * @return the namesspace of that item
     */
    @GET
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Path("/{itemname: [a-zA-Z_0-9]+}/metadata/namespaces")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getItemNamespaces", summary = "Gets the namespace of an item.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "Item not found") })
    public Response getItemNamespaces(@PathParam("itemname") @Parameter(description = "item name") String itemname,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language) {
        final Item item = getItem(itemname);

        if (item != null) {
            final Collection<String> namespaces = metadataRegistry.getAllNamespaces(itemname);
            return Response.ok(new Stream2JSONInputStream(namespaces.stream())).build();
        } else {
            return getItemNotFoundResponse(itemname);
        }
    }

    @GET
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Path("/{itemname: [a-zA-Z_0-9]+}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getItemByName", summary = "Gets a single item.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = EnrichedItemDTO.class))),
            @ApiResponse(responseCode = "404", description = "Item not found") })
    public Response getItemData(final @Context UriInfo uriInfo, final @Context HttpHeaders httpHeaders,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @QueryParam("metadata") @Parameter(description = "metadata selector - a comma separated list or a regular expression (suppressed if no value given)") @Nullable String namespaceSelector,
            @DefaultValue("true") @QueryParam("recursive") @Parameter(description = "get member items if the item is a group item") boolean recursive,
            @PathParam("itemname") @Parameter(description = "item name") String itemname) {
        final Locale locale = localeService.getLocale(language);
        final Set<String> namespaces = splitAndFilterNamespaces(namespaceSelector, locale);

        // get item
        Item item = getItem(itemname);

        // if it exists
        if (item != null) {
            EnrichedItemDTO dto = EnrichedItemDTOMapper.map(item, recursive, null, uriBuilder(uriInfo, httpHeaders),
                    locale);
            addMetadata(dto, namespaces, null);
            dto.editable = isEditable(dto.name);
            return JSONResponse.createResponse(Status.OK, dto, null);
        } else {
            return getItemNotFoundResponse(itemname);
        }
    }

    private Set<String> splitAndFilterNamespaces(@Nullable String namespaceSelector, Locale locale) {
        return metadataSelectorMatcher.filterNamespaces(namespaceSelector, locale);
    }

    /**
     *
     * @param itemname item name to get the state from
     * @return the state of the item as mime-type text/plain
     */
    @GET
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Path("/{itemname: [a-zA-Z_0-9]+}/state")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(operationId = "getItemState", summary = "Gets the state of an item.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "Item not found") })
    public Response getPlainItemState(@PathParam("itemname") @Parameter(description = "item name") String itemname) {
        // get item
        Item item = getItem(itemname);

        // if it exists
        if (item != null) {
            // we cannot use JSONResponse.createResponse() bc. MediaType.TEXT_PLAIN
            // return JSONResponse.createResponse(Status.OK, item.getState().toString(), null);
            return Response.ok(item.getState().toFullString()).build();
        } else {
            return getItemNotFoundResponse(itemname);
        }
    }

    /**
     *
     * @param itemname the item from which to get the binary state
     * @return the binary state of the item
     */
    @GET
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Path("/{itemname: [a-zA-Z_0-9]+}/state")
    @Operation(operationId = "getItemState", summary = "Gets the state of an item.", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Item state is not RawType"),
            @ApiResponse(responseCode = "404", description = "Item not found"),
            @ApiResponse(responseCode = "415", description = "MediaType not supported by item state") })
    public Response getBinaryItemState(@HeaderParam("Accept") @Nullable String mediaType,
            @PathParam("itemname") @Parameter(description = "item name") String itemname) {
        List<String> acceptedMediaTypes = Arrays.stream(Objects.requireNonNullElse(mediaType, "").split(","))
                .map(String::trim).collect(Collectors.toList());

        Item item = getItem(itemname);

        // if it exists
        if (item != null) {
            State state = item.getState();
            if (state instanceof RawType) {
                String mimeType = ((RawType) state).getMimeType();
                byte[] data = ((RawType) state).getBytes();
                if ((acceptedMediaTypes.contains("image/*") && mimeType.startsWith("image/"))
                        || acceptedMediaTypes.contains(mimeType)) {
                    return Response.ok(data).type(mimeType).build();
                } else if (acceptedMediaTypes.contains(MediaType.APPLICATION_OCTET_STREAM)) {
                    return Response.ok(data).type(MediaType.APPLICATION_OCTET_STREAM).build();
                } else {
                    return Response.status(Status.UNSUPPORTED_MEDIA_TYPE).build();
                }
            }
            return Response.status(Status.BAD_REQUEST).build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }
    }

    @PUT
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Path("/{itemname: [a-zA-Z_0-9]+}/state")
    @Consumes(MediaType.TEXT_PLAIN)
    @Operation(operationId = "updateItemState", summary = "Updates the state of an item.", responses = {
            @ApiResponse(responseCode = "202", description = "Accepted"),
            @ApiResponse(responseCode = "404", description = "Item not found"),
            @ApiResponse(responseCode = "400", description = "Item state null") })
    public Response putItemState(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @PathParam("itemname") @Parameter(description = "item name") String itemname,
            @Parameter(description = "valid item state (e.g. ON, OFF)", required = true) String value) {
        final Locale locale = localeService.getLocale(language);

        // get Item
        Item item = getItem(itemname);

        // if Item exists
        if (item != null) {
            // try to parse a State from the input
            State state = TypeParser.parseState(item.getAcceptedDataTypes(), value);

            if (state != null) {
                // set State and report OK
                eventPublisher.post(ItemEventFactory.createStateEvent(itemname, state));
                return getItemResponse(null, Status.ACCEPTED, null, locale, null);
            } else {
                // State could not be parsed
                return JSONResponse.createErrorResponse(Status.BAD_REQUEST, "State could not be parsed: " + value);
            }
        } else {
            // Item does not exist
            return getItemNotFoundResponse(itemname);
        }
    }

    @POST
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Path("/{itemname: [a-zA-Z_0-9]+}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Operation(operationId = "sendItemCommand", summary = "Sends a command to an item.", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Item not found"),
            @ApiResponse(responseCode = "400", description = "Item command null") })
    public Response postItemCommand(@PathParam("itemname") @Parameter(description = "item name") String itemname,
            @Parameter(description = "valid item command (e.g. ON, OFF, UP, DOWN, REFRESH)", required = true) String value) {
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
                eventPublisher.post(ItemEventFactory.createCommandEvent(itemname, command));
                ResponseBuilder resbuilder = Response.ok();
                resbuilder.type(MediaType.TEXT_PLAIN);
                return resbuilder.build();
            } else {
                return Response.status(Status.BAD_REQUEST).build();
            }
        } else {
            throw new WebApplicationException(404);
        }
    }

    @PUT
    @RolesAllowed({ Role.ADMIN })
    @Path("/{itemName: [a-zA-Z_0-9]+}/members/{memberItemName: [a-zA-Z_0-9]+}")
    @Operation(operationId = "addMemberToGroupItem", summary = "Adds a new member to a group item.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Item or member item not found or item is not of type group item."),
                    @ApiResponse(responseCode = "405", description = "Member item is not editable.") })
    public Response addMember(@PathParam("itemName") @Parameter(description = "item name") String itemName,
            @PathParam("memberItemName") @Parameter(description = "member item name") String memberItemName) {
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
    @Operation(operationId = "removeMemberFromGroupItem", summary = "Removes an existing member from a group item.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Item or member item not found or item is not of type group item."),
                    @ApiResponse(responseCode = "405", description = "Member item is not editable.") })
    public Response removeMember(@PathParam("itemName") @Parameter(description = "item name") String itemName,
            @PathParam("memberItemName") @Parameter(description = "member item name") String memberItemName) {
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
    @Operation(operationId = "removeItemFromRegistry", summary = "Removes an item from the registry.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Item not found or item is not editable.") })
    public Response removeItem(@PathParam("itemname") @Parameter(description = "item name") String itemname) {
        if (managedItemProvider.remove(itemname) == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        return Response.ok(null, MediaType.TEXT_PLAIN).build();
    }

    @PUT
    @RolesAllowed({ Role.ADMIN })
    @Path("/{itemname: [a-zA-Z_0-9]+}/tags/{tag}")
    @Operation(operationId = "addTagToItem", summary = "Adds a tag to an item.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Item not found."),
                    @ApiResponse(responseCode = "405", description = "Item not editable.") })
    public Response addTag(@PathParam("itemname") @Parameter(description = "item name") String itemname,
            @PathParam("tag") @Parameter(description = "tag") String tag) {
        Item item = getItem(itemname);

        if (item == null) {
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
    @Operation(operationId = "removeTagFromItem", summary = "Removes a tag from an item.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Item not found."),
                    @ApiResponse(responseCode = "405", description = "Item not editable.") })
    public Response removeTag(@PathParam("itemname") @Parameter(description = "item name") String itemname,
            @PathParam("tag") @Parameter(description = "tag") String tag) {
        Item item = getItem(itemname);

        if (item == null) {
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
    @Operation(operationId = "addMetadataToItem", summary = "Adds metadata to an item.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = { //
                    @ApiResponse(responseCode = "200", description = "OK"), //
                    @ApiResponse(responseCode = "201", description = "Created"), //
                    @ApiResponse(responseCode = "400", description = "Metadata value empty."), //
                    @ApiResponse(responseCode = "404", description = "Item not found."), //
                    @ApiResponse(responseCode = "405", description = "Metadata not editable.") })
    public Response addMetadata(@PathParam("itemname") @Parameter(description = "item name") String itemname,
            @PathParam("namespace") @Parameter(description = "namespace") String namespace,
            @Parameter(description = "metadata", required = true) MetadataDTO metadata) {
        Item item = getItem(itemname);

        if (item == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        String value = metadata.value;
        if (value == null || value.isEmpty()) {
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
    @Operation(operationId = "removeMetadataFromItem", summary = "Removes metadata from an item.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Item not found."),
                    @ApiResponse(responseCode = "405", description = "Meta data not editable.") })
    public Response removeMetadata(@PathParam("itemname") @Parameter(description = "item name") String itemname,
            @Nullable @PathParam("namespace") @Parameter(description = "namespace") String namespace) {
        Item item = getItem(itemname);

        if (item == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        if (namespace == null) {
            metadataRegistry.removeItemMetadata(itemname);
        } else {
            MetadataKey key = new MetadataKey(namespace, itemname);
            if (metadataRegistry.get(key) != null) {
                if (metadataRegistry.remove(key) == null) {
                    return Response.status(Status.CONFLICT).build();
                }
            } else {
                return Response.status(Status.NOT_FOUND).build();
            }
        }

        return Response.ok(null, MediaType.TEXT_PLAIN).build();
    }

    @POST
    @RolesAllowed({ Role.ADMIN })
    @Path("/metadata/purge")
    @Operation(operationId = "purgeDatabase", summary = "Remove unused/orphaned metadata.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK") })
    public Response purge() {
        Collection<String> itemNames = itemRegistry.stream().map(Item::getName)
                .collect(Collectors.toCollection(HashSet::new));

        metadataRegistry.getAll().stream().filter(md -> !itemNames.contains(md.getUID().getItemName()))
                .forEach(md -> metadataRegistry.remove(md.getUID()));
        return Response.ok().build();
    }

    /**
     * Create or Update an item by supplying an item bean.
     *
     * @param itemname the item name
     * @param item the item bean.
     * @return Response configured to represent the Item in depending on the status
     */
    @PUT
    @RolesAllowed({ Role.ADMIN })
    @Path("/{itemname: [a-zA-Z_0-9]+}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(operationId = "addOrUpdateItemInRegistry", summary = "Adds a new item to the registry or updates the existing item.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = EnrichedItemDTO.class))),
                    @ApiResponse(responseCode = "201", description = "Item created."),
                    @ApiResponse(responseCode = "400", description = "Payload invalid."),
                    @ApiResponse(responseCode = "404", description = "Item not found or name in path invalid."),
                    @ApiResponse(responseCode = "405", description = "Item not editable.") })
    public Response createOrUpdateItem(final @Context UriInfo uriInfo, final @Context HttpHeaders httpHeaders,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @PathParam("itemname") @Parameter(description = "item name") String itemname,
            @Parameter(description = "item data", required = true) @Nullable GroupItemDTO item) {
        final Locale locale = localeService.getLocale(language);

        // If we didn't get an item bean, then return!
        if (item == null) {
            return Response.status(Status.BAD_REQUEST).build();
        } else if (!itemname.equalsIgnoreCase((item.name))) {
            logger.warn(
                    "Received HTTP PUT request at '{}' with an item name '{}' that does not match the one in the url.",
                    uriInfo.getPath(), item.name);
            return Response.status(Status.BAD_REQUEST).build();
        }

        try {
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
                return getItemResponse(uriBuilder(uriInfo, httpHeaders), Status.CREATED, itemRegistry.get(itemname),
                        locale, null);
            } else if (managedItemProvider.get(itemname) != null) {
                // item already exists as a managed item, update it
                managedItemProvider.update(newItem);
                return getItemResponse(uriBuilder(uriInfo, httpHeaders), Status.OK, itemRegistry.get(itemname), locale,
                        null);
            } else {
                // Item exists but cannot be updated
                logger.warn("Cannot update existing item '{}', because is not managed.", itemname);
                return JSONResponse.createErrorResponse(Status.METHOD_NOT_ALLOWED,
                        "Cannot update non-managed Item " + itemname);
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Received HTTP PUT request at '{}' with an invalid item name '{}'.", uriInfo.getPath(),
                    item.name);
            return Response.status(Status.BAD_REQUEST).build();
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
    @Operation(operationId = "addOrUpdateItemsInRegistry", summary = "Adds a list of items to the registry or updates the existing items.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode = "400", description = "Payload is invalid.") })
    public Response createOrUpdateItems(
            @Parameter(description = "array of item data", required = true) GroupItemDTO @Nullable [] items) {
        // If we didn't get an item list bean, then return!
        if (items == null) {
            return Response.status(Status.BAD_REQUEST).build();
        }

        List<GroupItemDTO> wrongTypes = new ArrayList<>();
        List<Item> activeItems = new ArrayList<>();
        Map<String, Collection<String>> tagMap = new HashMap<>();

        for (GroupItemDTO item : items) {
            try {
                Item newItem = ItemDTOMapper.map(item, itemBuilderFactory);
                if (newItem == null) {
                    wrongTypes.add(item);
                    tagMap.put(item.name, item.tags);
                } else {
                    activeItems.add(newItem);
                }
            } catch (IllegalArgumentException e) {
                logger.warn("Received HTTP PUT request with an invalid item name '{}'.", item.name);
                return Response.status(Status.BAD_REQUEST).build();
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
            responseList.add(buildStatusObject(item.name, "error",
                    "Received HTTP PUT request with an invalid item type '" + item.type + "'."));
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

    @GET
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Path("/{itemName: \\w+}/semantic/{semanticClass: \\w+}")
    @Operation(operationId = "getSemanticItem", summary = "Gets the item which defines the requested semantics of an item.", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Item not found") })
    public Response getSemanticItem(final @Context UriInfo uriInfo, final @Context HttpHeaders httpHeaders,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @PathParam("itemName") @Parameter(description = "item name") String itemName,
            @PathParam("semanticClass") @Parameter(description = "semantic class") String semanticClassName) {
        Locale locale = localeService.getLocale(language);

        Class<? extends org.openhab.core.semantics.Tag> semanticClass = SemanticTags.getById(semanticClassName);
        if (semanticClass == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        Item foundItem = findParentByTag(getItem(itemName), SemanticsPredicates.isA(semanticClass));
        if (foundItem == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        EnrichedItemDTO dto = EnrichedItemDTOMapper.map(foundItem, false, null, uriBuilder(uriInfo, httpHeaders),
                locale);
        dto.editable = isEditable(dto.name);
        return JSONResponse.createResponse(Status.OK, dto, null);
    }

    private JsonObject buildStatusObject(String itemName, String status, @Nullable String message) {
        JsonObject jo = new JsonObject();
        jo.addProperty("name", itemName);
        jo.addProperty("status", status);
        jo.addProperty("message", message);
        return jo;
    }

    private @Nullable Item findParentByTag(@Nullable Item item, Predicate<Item> predicate) {
        if (item == null) {
            return null;
        }

        if (predicate.test(item)) {
            return item;
        }

        // check parents
        return item.getGroupNames().stream().map(this::getItem).map(i -> findParentByTag(i, predicate))
                .filter(Objects::nonNull).findAny().orElse(null);
    }

    /**
     * helper: Response to be sent to client if an item cannot be found
     *
     * @param itemname item name that could not be found
     * @return Response configured for 'item not found'
     */
    private static Response getItemNotFoundResponse(String itemname) {
        String message = "Item " + itemname + " does not exist!";
        return JSONResponse.createResponse(Status.NOT_FOUND, null, message);
    }

    /**
     * Prepare a response representing the Item depending on the status.
     *
     * @param uriBuilder the URI builder
     * @param status http status
     * @param item can be null
     * @param locale the locale
     * @param errormessage optional message in case of error
     * @return Response configured to represent the Item in depending on the status
     */
    private Response getItemResponse(final @Nullable UriBuilder uriBuilder, Status status, @Nullable Item item,
            Locale locale, @Nullable String errormessage) {
        Object entity = null != item ? EnrichedItemDTOMapper.map(item, true, null, uriBuilder, locale) : null;
        return JSONResponse.createResponse(status, entity, errormessage);
    }

    /**
     * convenience shortcut
     *
     * @param itemname the name of the item to be retrieved
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
}
