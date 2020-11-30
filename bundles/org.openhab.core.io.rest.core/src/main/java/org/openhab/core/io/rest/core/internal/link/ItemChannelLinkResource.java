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
package org.openhab.core.io.rest.core.internal.link;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Role;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.io.rest.JSONResponse;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.io.rest.Stream2JSONInputStream;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.link.AbstractLink;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.link.dto.ItemChannelLinkDTO;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JSONRequired;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * This class acts as a REST resource for links.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Yordan Zhelev - Added Swagger annotations
 * @author Kai Kreuzer - Removed Thing links and added auto link url
 * @author Franck Dechavanne - Added DTOs to ApiResponses
 * @author Yannick Schaus - Added filters to getAll
 * @author Markus Rathgeb - Migrated to JAX-RS Whiteboard Specification
 * @author Wouter Born - Migrated to OpenAPI annotations
 */
@Component(service = { RESTResource.class, ItemChannelLinkResource.class })
@JaxrsResource
@JaxrsName(ItemChannelLinkResource.PATH_LINKS)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(ItemChannelLinkResource.PATH_LINKS)
@RolesAllowed({ Role.ADMIN })
@SecurityRequirement(name = "oauth2", scopes = { "admin" })
@Tag(name = ItemChannelLinkResource.PATH_LINKS)
@NonNullByDefault
public class ItemChannelLinkResource implements RESTResource {

    /** The URI path to this resource */
    public static final String PATH_LINKS = "links";

    private final ItemChannelLinkRegistry itemChannelLinkRegistry;

    @Activate
    public ItemChannelLinkResource(final @Reference ItemChannelLinkRegistry itemChannelLinkRegistry) {
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getItemLinks", summary = "Gets all available links.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ItemChannelLinkDTO.class)))) })
    public Response getAll(
            @QueryParam("channelUID") @Parameter(description = "filter by channel UID") @Nullable String channelUID,
            @QueryParam("itemName") @Parameter(description = "filter by item name") @Nullable String itemName) {
        Stream<ItemChannelLinkDTO> linkStream = itemChannelLinkRegistry.getAll().stream().map(this::toBeans);

        if (channelUID != null) {
            linkStream = linkStream.filter(link -> channelUID.equals(link.channelUID));
        }
        if (itemName != null) {
            linkStream = linkStream.filter(link -> itemName.equals(link.itemName));
        }

        return Response.ok(new Stream2JSONInputStream(linkStream)).build();
    }

    @GET
    @Path("/{itemName}/{channelUID}")
    @Operation(operationId = "getItemLink", summary = "Retrieves an individual link.", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Content does not match the path") })
    public Response getLink(@PathParam("itemName") @Parameter(description = "itemName") String itemName,
            @PathParam("channelUID") @Parameter(description = "channelUID") String channelUid) {
        List<ItemChannelLinkDTO> links = itemChannelLinkRegistry.getAll().stream()
                .filter(link -> channelUid.equals(link.getLinkedUID().getAsString()))
                .filter(link -> itemName.equals(link.getItemName())).map(this::toBeans).collect(Collectors.toList());

        if (links.size() == 1) {
            ItemChannelLinkDTO link = links.get(0);
            return JSONResponse.createResponse(Status.OK, link, null);
        }
        return JSONResponse.createErrorResponse(Status.NOT_FOUND,
                "No link found for item '" + itemName + "' + and channelUID '" + channelUid + "'");
    }

    @PUT
    @RolesAllowed({ Role.ADMIN })
    @Path("/{itemName}/{channelUID}")
    @Operation(operationId = "linkItemToChannel", summary = "Links item to a channel.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Content does not match the path"),
                    @ApiResponse(responseCode = "405", description = "Link is not editable") })
    public Response link(@PathParam("itemName") @Parameter(description = "itemName") String itemName,
            @PathParam("channelUID") @Parameter(description = "channelUID") String channelUid,
            @Parameter(description = "link data") @Nullable ItemChannelLinkDTO bean) {
        ItemChannelLink link;
        if (bean == null) {
            link = new ItemChannelLink(itemName, new ChannelUID(channelUid), new Configuration());
        } else {
            if (bean.channelUID != null && !bean.channelUID.equals(channelUid)) {
                return Response.status(Status.BAD_REQUEST).build();
            }
            if (bean.itemName != null && !bean.itemName.equals(itemName)) {
                return Response.status(Status.BAD_REQUEST).build();
            }
            link = new ItemChannelLink(itemName, new ChannelUID(channelUid), new Configuration(bean.configuration));
        }
        if (itemChannelLinkRegistry.get(link.getUID()) == null) {
            itemChannelLinkRegistry.add(link);
        } else {
            ItemChannelLink oldLink = itemChannelLinkRegistry.update(link);
            if (oldLink == null) {
                return Response.status(Status.METHOD_NOT_ALLOWED).build();
            }
        }
        return Response.ok(null, MediaType.TEXT_PLAIN).build();
    }

    @DELETE
    @RolesAllowed({ Role.ADMIN })
    @Path("/{itemName}/{channelUID}")
    @Operation(operationId = "unlinkItemFromChannel", summary = "Unlinks item from a channel.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Link not found."),
                    @ApiResponse(responseCode = "405", description = "Link not editable.") })
    public Response unlink(@PathParam("itemName") @Parameter(description = "itemName") String itemName,
            @PathParam("channelUID") @Parameter(description = "channelUID") String channelUid) {
        String linkId = AbstractLink.getIDFor(itemName, new ChannelUID(channelUid));
        if (itemChannelLinkRegistry.get(linkId) == null) {
            String message = "Link " + linkId + " does not exist!";
            return JSONResponse.createResponse(Status.NOT_FOUND, null, message);
        }

        ItemChannelLink result = itemChannelLinkRegistry.remove(linkId);
        if (result != null) {
            return Response.ok(null, MediaType.TEXT_PLAIN).build();
        } else {
            return JSONResponse.createErrorResponse(Status.METHOD_NOT_ALLOWED, "Channel is read-only.");
        }
    }

    private ItemChannelLinkDTO toBeans(ItemChannelLink link) {
        return new ItemChannelLinkDTO(link.getItemName(), link.getLinkedUID().toString(),
                link.getConfiguration().getProperties());
    }
}
