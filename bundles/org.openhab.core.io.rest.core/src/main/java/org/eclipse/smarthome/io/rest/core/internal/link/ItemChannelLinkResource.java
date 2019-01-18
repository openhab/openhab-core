/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.io.rest.core.internal.link;

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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.auth.Role;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.link.AbstractLink;
import org.eclipse.smarthome.core.thing.link.ItemChannelLink;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
import org.eclipse.smarthome.core.thing.link.ThingLinkManager;
import org.eclipse.smarthome.core.thing.link.dto.ItemChannelLinkDTO;
import org.eclipse.smarthome.io.rest.JSONResponse;
import org.eclipse.smarthome.io.rest.RESTResource;
import org.eclipse.smarthome.io.rest.Stream2JSONInputStream;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * This class acts as a REST resource for links.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Yordan Zhelev - Added Swagger annotations
 * @author Kai Kreuzer - Removed Thing links and added auto link url
 * @author Franck Dechavanne - Added DTOs to ApiResponses
 */
@Path(ItemChannelLinkResource.PATH_LINKS)
@RolesAllowed({ Role.ADMIN })
@Api(value = ItemChannelLinkResource.PATH_LINKS)
@Component(service = { RESTResource.class, ItemChannelLinkResource.class })
public class ItemChannelLinkResource implements RESTResource {

    /** The URI path to this resource */
    public static final String PATH_LINKS = "links";

    private ItemChannelLinkRegistry itemChannelLinkRegistry;
    private ThingLinkManager thingLinkManager;

    @Context
    UriInfo uriInfo;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets all available links.", response = ItemChannelLinkDTO.class, responseContainer = "Collection")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = ItemChannelLinkDTO.class, responseContainer = "Collection") })
    public Response getAll() {
        Stream<ItemChannelLinkDTO> linkStream = itemChannelLinkRegistry.getAll().stream().map(this::toBeans);
        return Response.ok(new Stream2JSONInputStream(linkStream)).build();
    }

    @GET
    @Path("/auto")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Tells whether automatic link mode is active or not", response = Boolean.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = Boolean.class) })
    public Response isAutomatic() {
        return Response.ok(thingLinkManager.isAutoLinksEnabled()).build();
    }

    @GET
    @Path("/{itemName}/{channelUID}")
    @ApiOperation(value = "Retrieves links.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Content does not match the path") })
    public Response getLink(@PathParam("itemName") @ApiParam(value = "itemName") String itemName,
            @PathParam("channelUID") @ApiParam(value = "channelUID") String channelUid) {

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
    @Path("/{itemName}/{channelUID}")
    @ApiOperation(value = "Links item to a channel.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 400, message = "Content does not match the path"),
            @ApiResponse(code = 405, message = "Link is not editable") })
    public Response link(@PathParam("itemName") @ApiParam(value = "itemName") String itemName,
            @PathParam("channelUID") @ApiParam(value = "channelUID") String channelUid,
            @ApiParam(value = "link data", required = false) ItemChannelLinkDTO bean) {
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
    @Path("/{itemName}/{channelUID}")
    @ApiOperation(value = "Unlinks item from a channel.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Link not found."),
            @ApiResponse(code = 405, message = "Link not editable.") })
    public Response unlink(@PathParam("itemName") @ApiParam(value = "itemName") String itemName,
            @PathParam("channelUID") @ApiParam(value = "channelUID") String channelUid) {
        String linkId = AbstractLink.getIDFor(itemName, new ChannelUID(channelUid));
        if (itemChannelLinkRegistry.get(linkId) == null) {
            String message = "Link " + linkId + " does not exist!";
            return JSONResponse.createResponse(Status.NOT_FOUND, null, message);
        }

        ItemChannelLink result = itemChannelLinkRegistry
                .remove(AbstractLink.getIDFor(itemName, new ChannelUID(channelUid)));
        if (result != null) {
            return Response.ok(null, MediaType.TEXT_PLAIN).build();
        } else {
            return JSONResponse.createErrorResponse(Status.METHOD_NOT_ALLOWED, "Channel is read-only.");
        }
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setThingLinkManager(ThingLinkManager thingLinkManager) {
        this.thingLinkManager = thingLinkManager;
    }

    protected void unsetThingLinkManager(ThingLinkManager thingLinkManager) {
        this.thingLinkManager = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setItemChannelLinkRegistry(ItemChannelLinkRegistry itemChannelLinkRegistry) {
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
    }

    protected void unsetItemChannelLinkRegistry(ItemChannelLinkRegistry itemChannelLinkRegistry) {
        this.itemChannelLinkRegistry = null;
    }

    private ItemChannelLinkDTO toBeans(ItemChannelLink link) {
        return new ItemChannelLinkDTO(link.getItemName(), link.getLinkedUID().toString(),
                link.getConfiguration().getProperties());
    }

    @Override
    public boolean isSatisfied() {
        return itemChannelLinkRegistry != null && thingLinkManager != null;
    }

}
