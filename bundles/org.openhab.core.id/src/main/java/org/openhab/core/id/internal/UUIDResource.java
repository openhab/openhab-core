/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.id.internal;

import javax.annotation.security.RolesAllowed;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.auth.Role;
import org.openhab.core.id.InstanceUUID;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.jakartars.whiteboard.JakartarsWhiteboardConstants;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsApplicationSelect;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsName;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * This class acts as a REST resource for accessing the UUID of the instance
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Markus Rathgeb - Migrated to JAX-RS Whiteboard Specification
 * @author Wouter Born - Migrated to OpenAPI annotations
 */
@Component
@JakartarsResource
@JakartarsName(UUIDResource.PATH_UUID)
@JakartarsApplicationSelect("(" + JakartarsWhiteboardConstants.JAKARTA_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@Path(UUIDResource.PATH_UUID)
@RolesAllowed({ Role.ADMIN, Role.USER })
@Tag(name = UUIDResource.PATH_UUID)
@NonNullByDefault
public class UUIDResource implements RESTResource {

    public static final String PATH_UUID = "uuid";

    /**
     * Retrieves the instance UUID via REST endpoint.
     * This method exposes the unique instance identifier through a REST API endpoint.
     * The UUID is generated once and persisted, remaining constant across restarts.
     *
     * @return a Response containing the instance UUID as plain text, or null if the UUID cannot be retrieved
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(operationId = "getUUID", summary = "A unified unique id.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = String.class))) })
    public Response getInstanceUUID() {
        return Response.ok(InstanceUUID.get()).build();
    }
}
