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
package org.eclipse.smarthome.io.rest.core.internal.discovery;

import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.DiscoveryResultFlag;
import org.eclipse.smarthome.config.discovery.dto.DiscoveryResultDTO;
import org.eclipse.smarthome.config.discovery.dto.DiscoveryResultDTOMapper;
import org.eclipse.smarthome.config.discovery.inbox.Inbox;
import org.eclipse.smarthome.core.auth.Role;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingUID;
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
 * This class acts as a REST resource for the inbox and is registered with the
 * Jersey servlet.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Kai Kreuzer - refactored for using the OSGi JAX-RS connector and removed ThingSetupManager
 * @author Yordan Zhelev - Added Swagger annotations
 * @author Chris Jackson - Updated to use JSONResponse. Fixed null response from approve. Improved error reporting.
 * @author Franck Dechavanne - Added DTOs to ApiResponses
 */
@Path(InboxResource.PATH_INBOX)
@RolesAllowed({ Role.ADMIN })
@Api(value = InboxResource.PATH_INBOX)
@Produces(MediaType.APPLICATION_JSON)
@Component(immediate = true)
@JaxrsApplicationSelect("(osgi.jaxrs.name=" + RESTService.REST_APP_NAME + ")")
@JaxrsResource
@NonNullByDefault
public class InboxResource {
    /** The URI path to this resource */
    public static final String PATH_INBOX = "inbox";

    @Reference
    protected @NonNullByDefault({}) Inbox inbox;

    @POST
    @Path("/{thingUID}/approve")
    @Consumes(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Approves the discovery result by adding the thing to the registry.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Thing unable to be approved."),
            @ApiResponse(code = 409, message = "No binding found that supports this thing.") })
    public void approve(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") @Nullable String language,
            @PathParam("thingUID") @ApiParam(value = "thingUID", required = true) String thingUID,
            @ApiParam(value = "thing label") String label) {
        ThingUID thingUIDObject = new ThingUID(thingUID);
        String notEmptyLabel = label != null && !label.isEmpty() ? label : null;
        Thing thing = null;
        try {
            // inbox.approve returns null if no handler is found that supports this thing
            thing = inbox.approve(thingUIDObject, notEmptyLabel);
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("Thing unable to be approved.", e);
        }

        if (thing == null) {
            throw new WebApplicationException("No binding found that can create the thing", Response.Status.CONFLICT);
        }
    }

    @DELETE
    @Path("/{thingUID}")
    @ApiOperation(value = "Removes the discovery result from the inbox.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Discovery result not found in the inbox.") })
    public void delete(@PathParam("thingUID") @ApiParam(value = "thingUID", required = true) String thingUID) {
        if (!inbox.remove(new ThingUID(thingUID))) {
            throw new NotFoundException("Thing not found in inbox");
        }
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Get all discovered things.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = DiscoveryResultDTO.class) })
    public Stream<?> getAll() {
        return inbox.getAll().stream().map(DiscoveryResultDTOMapper::map);
    }

    @POST
    @Path("/{thingUID}/ignore")
    @ApiOperation(value = "Flags a discovery result as ignored for further processing.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK") })
    public void ignore(@PathParam("thingUID") @ApiParam(value = "thingUID", required = true) String thingUID) {
        inbox.setFlag(new ThingUID(thingUID), DiscoveryResultFlag.IGNORED);
    }

    @POST
    @Path("/{thingUID}/unignore")
    @ApiOperation(value = "Removes ignore flag from a discovery result.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK") })
    public void unignore(@PathParam("thingUID") @ApiParam(value = "thingUID", required = true) String thingUID) {
        inbox.setFlag(new ThingUID(thingUID), DiscoveryResultFlag.NEW);
    }
}
