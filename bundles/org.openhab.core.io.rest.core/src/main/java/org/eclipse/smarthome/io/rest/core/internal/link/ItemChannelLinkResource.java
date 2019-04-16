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
package org.eclipse.smarthome.io.rest.core.internal.link;

import java.util.NoSuchElementException;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.auth.Role;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.link.AbstractLink;
import org.eclipse.smarthome.core.thing.link.ItemChannelLink;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
import org.eclipse.smarthome.core.thing.link.ThingLinkManager;
import org.eclipse.smarthome.core.thing.link.dto.ItemChannelLinkDTO;
import org.eclipse.smarthome.io.rest.RESTService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

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
@Produces(MediaType.APPLICATION_JSON)
@Component(immediate = true)
@JaxrsApplicationSelect("(osgi.jaxrs.name=" + RESTService.REST_APP_NAME + ")")
@JaxrsResource
@NonNullByDefault
public class ItemChannelLinkResource {

    /** The URI path to this resource */
    public static final String PATH_LINKS = "links";

    @Reference
    private @NonNullByDefault({}) ItemChannelLinkRegistry itemChannelLinkRegistry;
    @Reference
    private @NonNullByDefault({}) ThingLinkManager thingLinkManager;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets all available links.", response = ItemChannelLinkDTO.class, responseContainer = "Collection")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = ItemChannelLinkDTO.class, responseContainer = "Collection") })
    public Stream<?> getAll() {
        return itemChannelLinkRegistry.getAll().stream().map(ItemChannelLinkResource::map);
    }

    @GET
    @Path("/auto")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Tells whether automatic link mode is active or not", response = Boolean.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = Boolean.class) })
    public boolean isAutomatic() {
        return thingLinkManager.isAutoLinksEnabled();
    }

    @GET
    @Path("/{itemName}/{channelUID}")
    @ApiOperation(value = "Retrieves links.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Content does not match the path") })
    public ItemChannelLinkDTO getLink(@PathParam("itemName") @ApiParam(value = "itemName") String itemName,
            @PathParam("channelUID") @ApiParam(value = "channelUID") String channelUid) {

        try {
            return itemChannelLinkRegistry.getAll().stream()
                    .filter(link -> channelUid.equals(link.getLinkedUID().getAsString()))
                    .filter(link -> itemName.equals(link.getItemName())).map(ItemChannelLinkResource::map).findAny()
                    .get();
        } catch (NoSuchElementException ex) {
            throw new NotFoundException(
                    "No link found for item '" + itemName + "' + and channelUID '" + channelUid + "'");
        }
    }

    @PUT
    @Path("/{itemName}/{channelUID}")
    @ApiOperation(value = "Links item to a channel.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 400, message = "Content does not match the path"),
            @ApiResponse(code = 405, message = "Link is not editable") })
    public void link(@PathParam("itemName") @ApiParam(value = "itemName") String itemName,
            @PathParam("channelUID") @ApiParam(value = "channelUID") String channelUid,
            @ApiParam(value = "link data", required = false) @Nullable ItemChannelLinkDTO channelLink) {
        ItemChannelLink link;
        if (channelLink == null) {
            link = new ItemChannelLink(itemName, new ChannelUID(channelUid), new Configuration());
        } else {
            if (channelLink.channelUID != null && !channelLink.channelUID.equals(channelUid)) {
                throw new BadRequestException();
            }
            if (channelLink.itemName != null && !channelLink.itemName.equals(itemName)) {
                throw new BadRequestException();
            }
            link = new ItemChannelLink(itemName, new ChannelUID(channelUid),
                    new Configuration(channelLink.configuration));
        }
        if (itemChannelLinkRegistry.get(link.getUID()) == null) {
            itemChannelLinkRegistry.add(link);
        } else {
            ItemChannelLink oldLink = itemChannelLinkRegistry.update(link);
            if (oldLink == null) {
                throw new BadRequestException();
            }
        }
    }

    @DELETE
    @Path("/{itemName}/{channelUID}")
    @ApiOperation(value = "Unlinks item from a channel.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Link not found."),
            @ApiResponse(code = 400, message = "Link not editable.") })
    public void unlink(@PathParam("itemName") @ApiParam(value = "itemName") String itemName,
            @PathParam("channelUID") @ApiParam(value = "channelUID") String channelUid) {
        String linkId = AbstractLink.getIDFor(itemName, new ChannelUID(channelUid));
        if (itemChannelLinkRegistry.get(linkId) == null) {
            throw new NotFoundException();
        }

        ItemChannelLink result = itemChannelLinkRegistry
                .remove(AbstractLink.getIDFor(itemName, new ChannelUID(channelUid)));
        if (result == null) {
            throw new BadRequestException("Channel is read-only.");
        }
    }

    private static ItemChannelLinkDTO map(ItemChannelLink link) {
        return new ItemChannelLinkDTO(link.getItemName(), link.getLinkedUID().toString(),
                link.getConfiguration().getProperties());
    }
}
