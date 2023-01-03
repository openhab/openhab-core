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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * This class acts as a REST resource for the inbox and is registered with the
 * Jersey servlet.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Kai Kreuzer - refactored for using the OSGi JAX-RS connector and
 *         removed ThingSetupManager
 * @author Yordan Zhelev - Added Swagger annotations
 * @author Chris Jackson - Updated to use JSONResponse. Fixed null response from
 *         approve. Improved error reporting.
 * @author Franck Dechavanne - Added DTOs to ApiResponses
 * @author Markus Rathgeb - Migrated to JAX-RS Whiteboard Specification
 * @author Wouter Born - Migrated to OpenAPI annotations
 * @author Laurent Garnier - Added optional parameter newThingId to approve API
 */
@Component(service = { RESTResource.class, InboxResource.class })
@JaxrsResource
@JaxrsName(InboxResource.PATH_INBOX)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(InboxResource.PATH_INBOX)
@RolesAllowed({ Role.ADMIN })
@SecurityRequirement(name = "oauth2", scopes = { "admin" })
@Tag(name = InboxResource.PATH_INBOX)
@NonNullByDefault
public class InboxResource implements RESTResource {
    private final Logger logger = LoggerFactory.getLogger(InboxResource.class);

    /** The URI path to this resource */
    public static final String PATH_INBOX = "inbox";

    private final Inbox inbox;

    @Activate
    public InboxResource(final @Reference Inbox inbox) {
        this.inbox = inbox;
    }

    @POST
    @Path("/{thingUID}/approve")
    @Consumes(MediaType.TEXT_PLAIN)
    @Operation(operationId = "approveInboxItemById", summary = "Approves the discovery result by adding the thing to the registry.", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Invalid new thing ID."),
            @ApiResponse(responseCode = "404", description = "Thing unable to be approved."),
            @ApiResponse(responseCode = "409", description = "No binding found that supports this thing.") })
    public Response approve(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @PathParam("thingUID") @Parameter(description = "thingUID") String thingUID,
            @Parameter(description = "thing label") @Nullable String label,
            @QueryParam("newThingId") @Parameter(description = "new thing ID") @Nullable String newThingId) {
        ThingUID thingUIDObject = new ThingUID(thingUID);
        String notEmptyLabel = label != null && !label.isEmpty() ? label : null;
        String notEmptyNewThingId = newThingId != null && !newThingId.isEmpty() ? newThingId : null;
        Thing thing = null;
        try {
            thing = inbox.approve(thingUIDObject, notEmptyLabel, notEmptyNewThingId);
        } catch (IllegalArgumentException e) {
            logger.error("Thing {} unable to be approved: {}", thingUID, e.getLocalizedMessage());
            String errMsg = e.getMessage();
            return errMsg != null
                    && (errMsg.contains("must not contain multiple segments") || errMsg.startsWith("Invalid thing UID"))
                            ? JSONResponse.createErrorResponse(Status.BAD_REQUEST, "Invalid new thing ID.")
                            : JSONResponse.createErrorResponse(Status.NOT_FOUND, "Thing unable to be approved.");
        }

        // inbox.approve returns null if no handler is found that supports this thing
        if (thing == null) {
            return JSONResponse.createErrorResponse(Status.CONFLICT, "No binding found that can create the thing");
        }

        return Response.ok(null, MediaType.TEXT_PLAIN).build();
    }

    @DELETE
    @Path("/{thingUID}")
    @Operation(operationId = "removeItemFromInbox", summary = "Removes the discovery result from the inbox.", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Discovery result not found in the inbox.") })
    public Response delete(@PathParam("thingUID") @Parameter(description = "thingUID") String thingUID) {
        if (inbox.remove(new ThingUID(thingUID))) {
            return Response.ok(null, MediaType.TEXT_PLAIN).build();
        } else {
            return JSONResponse.createErrorResponse(Status.NOT_FOUND, "Thing not found in inbox");
        }
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(operationId = "getDiscoveredInboxItems", summary = "Get all discovered things.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = DiscoveryResultDTO.class)))) })
    public Response getAll() {
        Stream<DiscoveryResultDTO> discoveryStream = inbox.getAll().stream().map(DiscoveryResultDTOMapper::map);
        return Response.ok(new Stream2JSONInputStream(discoveryStream)).build();
    }

    @POST
    @Path("/{thingUID}/ignore")
    @Operation(operationId = "flagInboxItemAsIgnored", summary = "Flags a discovery result as ignored for further processing.", responses = {
            @ApiResponse(responseCode = "200", description = "OK") })
    public Response ignore(@PathParam("thingUID") @Parameter(description = "thingUID") String thingUID) {
        inbox.setFlag(new ThingUID(thingUID), DiscoveryResultFlag.IGNORED);
        return Response.ok(null, MediaType.TEXT_PLAIN).build();
    }

    @POST
    @Path("/{thingUID}/unignore")
    @Operation(operationId = "removeIgnoreFlagOnInboxItem", summary = "Removes ignore flag from a discovery result.", responses = {
            @ApiResponse(responseCode = "200", description = "OK") })
    public Response unignore(@PathParam("thingUID") @Parameter(description = "thingUID") String thingUID) {
        inbox.setFlag(new ThingUID(thingUID), DiscoveryResultFlag.NEW);
        return Response.ok(null, MediaType.TEXT_PLAIN).build();
    }
}
