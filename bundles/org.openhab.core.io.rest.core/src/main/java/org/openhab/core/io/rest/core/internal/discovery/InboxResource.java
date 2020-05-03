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
package org.openhab.core.io.rest.core.internal.discovery;

import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.openhab.core.auth.Role;
import org.openhab.core.config.discovery.DiscoveryResultFlag;
import org.openhab.core.config.discovery.dto.DiscoveryResultDTO;
import org.openhab.core.config.discovery.dto.DiscoveryResultDTOMapper;
import org.openhab.core.config.discovery.inbox.Inbox;
import org.openhab.core.io.rest.JSONResponse;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.io.rest.Stream2JSONInputStream;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Component;
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
 * @author Markus Rathgeb - Migrated to JAX-RS Whiteboard Specification
 */
@Component(service = { RESTResource.class, InboxResource.class })
@JaxrsResource
@JaxrsName(InboxResource.PATH_INBOX)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(InboxResource.PATH_INBOX)
@RolesAllowed({ Role.ADMIN })
@Api(InboxResource.PATH_INBOX)
public class InboxResource implements RESTResource {
    private final Logger logger = LoggerFactory.getLogger(InboxResource.class);

    /** The URI path to this resource */
    public static final String PATH_INBOX = "inbox";

    private Inbox inbox;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setInbox(Inbox inbox) {
        this.inbox = inbox;
    }

    protected void unsetInbox(Inbox inbox) {
        this.inbox = null;
    }

    @Context
    private UriInfo uriInfo;

    @POST
    @Path("/{thingUID}/approve")
    @Consumes(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Approves the discovery result by adding the thing to the registry.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Thing unable to be approved."),
            @ApiResponse(code = 409, message = "No binding found that supports this thing.") })
    public Response approve(@HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") String language,
            @PathParam("thingUID") @ApiParam(value = "thingUID", required = true) String thingUID,
            @ApiParam(value = "thing label") String label) {
        ThingUID thingUIDObject = new ThingUID(thingUID);
        String notEmptyLabel = label != null && !label.isEmpty() ? label : null;
        Thing thing = null;
        try {
            thing = inbox.approve(thingUIDObject, notEmptyLabel);
        } catch (IllegalArgumentException e) {
            logger.error("Thing {} unable to be approved: {}", thingUID, e.getLocalizedMessage());
            return JSONResponse.createErrorResponse(Status.NOT_FOUND, "Thing unable to be approved.");
        }

        // inbox.approve returns null if no handler is found that supports this thing
        if (thing == null) {
            return JSONResponse.createErrorResponse(Status.CONFLICT, "No binding found that can create the thing");
        }

        return Response.ok(null, MediaType.TEXT_PLAIN).build();
    }

    @DELETE
    @Path("/{thingUID}")
    @ApiOperation(value = "Removes the discovery result from the inbox.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Discovery result not found in the inbox.") })
    public Response delete(@PathParam("thingUID") @ApiParam(value = "thingUID", required = true) String thingUID) {
        if (inbox.remove(new ThingUID(thingUID))) {
            return Response.ok(null, MediaType.TEXT_PLAIN).build();
        } else {
            return JSONResponse.createErrorResponse(Status.NOT_FOUND, "Thing not found in inbox");
        }
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Get all discovered things.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = DiscoveryResultDTO.class) })
    public Response getAll() {
        Stream<DiscoveryResultDTO> discoveryStream = inbox.getAll().stream().map(DiscoveryResultDTOMapper::map);
        return Response.ok(new Stream2JSONInputStream(discoveryStream)).build();
    }

    @POST
    @Path("/{thingUID}/ignore")
    @ApiOperation(value = "Flags a discovery result as ignored for further processing.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK") })
    public Response ignore(@PathParam("thingUID") @ApiParam(value = "thingUID", required = true) String thingUID) {
        inbox.setFlag(new ThingUID(thingUID), DiscoveryResultFlag.IGNORED);
        return Response.ok(null, MediaType.TEXT_PLAIN).build();
    }

    @POST
    @Path("/{thingUID}/unignore")
    @ApiOperation(value = "Removes ignore flag from a discovery result.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK") })
    public Response unignore(@PathParam("thingUID") @ApiParam(value = "thingUID", required = true) String thingUID) {
        inbox.setFlag(new ThingUID(thingUID), DiscoveryResultFlag.NEW);
        return Response.ok(null, MediaType.TEXT_PLAIN).build();
    }

    @Override
    public boolean isSatisfied() {
        return inbox != null;
    }
}
