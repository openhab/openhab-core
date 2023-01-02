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
package org.openhab.core.karaf.internal;

import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.karaf.log.core.Level;
import org.apache.karaf.log.core.LogService;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Role;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * This class acts as a REST resource for changing logging configuration.
 *
 * @author Jan N. Klug - Initial contribution
 */
@Component
@JaxrsResource
@JaxrsName(LoggerResource.PATH_LOGGING)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(LoggerResource.PATH_LOGGING)
@RolesAllowed({ Role.ADMIN })
@SecurityRequirement(name = "oauth2", scopes = { "admin" })
@Tag(name = LoggerResource.PATH_LOGGING)
@NonNullByDefault
public class LoggerResource implements RESTResource {
    public static final String PATH_LOGGING = "logging";

    private static final Set<String> LOG_LEVELS = Set.of(Level.strings());
    private static final Pattern BUNDLE_REGEX = Pattern.compile("[\\w.]+");

    private final LogService logService;

    @Activate
    public LoggerResource(@Reference LogService logService) {
        this.logService = logService;
    }

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getLogger", summary = "Get all loggers", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = LoggerBean.class))) })
    public Response getLoggers(@Context UriInfo uriInfo) {
        final LoggerBean bean = new LoggerBean(logService.getLevel("ALL"));
        return Response.ok(bean).build();
    }

    @PUT
    @Path("/{loggerName: [a-zA-Z0-9.]+}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(operationId = "putLogger", summary = "Modify or add logger", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Payload is invalid.") })
    public Response putLoggers(@PathParam("loggerName") @Parameter(description = "logger name") String loggerName,
            @Parameter(description = "logger", required = true) LoggerBean.@Nullable LoggerInfo logger,
            @Context UriInfo uriInfo) {
        if (logger == null || !BUNDLE_REGEX.matcher(logger.loggerName).matches() || !LOG_LEVELS.contains(logger.level)
                || !logger.loggerName.equals(loggerName)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        logService.setLevel(logger.loggerName, logger.level);
        return Response.ok(null, MediaType.TEXT_PLAIN).build();
    }

    @GET
    @Path("/{loggerName: [a-zA-Z0-9.]+}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getLogger", summary = "Get a single logger.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = LoggerBean.LoggerInfo.class))) })
    public Response getLogger(@PathParam("loggerName") @Parameter(description = "logger name") String loggerName,
            @Context UriInfo uriInfo) {
        final LoggerBean bean = new LoggerBean(logService.getLevel(loggerName));
        return Response.ok(bean).build();
    }

    @DELETE
    @Path("/{loggerName: [a-zA-Z0-9.]+}")
    @Operation(operationId = "removeLogger", summary = "Remove a single logger.", responses = {
            @ApiResponse(responseCode = "200", description = "OK") })
    public Response removeLogger(@PathParam("loggerName") @Parameter(description = "logger name") String loggerName,
            @Context UriInfo uriInfo) {
        logService.setLevel(loggerName, "DEFAULT");
        return Response.ok(null, MediaType.TEXT_PLAIN).build();
    }
}
