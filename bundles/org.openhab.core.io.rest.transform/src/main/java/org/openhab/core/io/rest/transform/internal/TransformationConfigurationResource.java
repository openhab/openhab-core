/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.core.io.rest.transform.internal;

import java.util.Objects;
import java.util.stream.Stream;

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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Role;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.io.rest.Stream2JSONInputStream;
import org.openhab.core.io.rest.transform.TransformationConfigurationDTO;
import org.openhab.core.transform.ManagedTransformationConfigurationProvider;
import org.openhab.core.transform.TransformationConfiguration;
import org.openhab.core.transform.TransformationConfigurationRegistry;
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
 * The {@link TransformationConfigurationResource} is a REST resource for handling transformation configurations
 *
 * @author Jan N. Klug - Initial contribution
 */
@Component(immediate = true)
@JaxrsResource
@JaxrsName(TransformationConfigurationResource.PATH_TRANSFORM)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(TransformationConfigurationResource.PATH_TRANSFORM)
@RolesAllowed({ Role.ADMIN })
@SecurityRequirement(name = "oauth2", scopes = { "admin" })
@Tag(name = TransformationConfigurationResource.PATH_TRANSFORM)
@NonNullByDefault
public class TransformationConfigurationResource implements RESTResource {
    public static final String PATH_TRANSFORM = "transform";

    private final Logger logger = LoggerFactory.getLogger(TransformationConfigurationResource.class);
    private final TransformationConfigurationRegistry transformationConfigurationRegistry;
    private final ManagedTransformationConfigurationProvider managedTransformationConfigurationProvider;
    private @Context @NonNullByDefault({}) UriInfo uriInfo;

    @Activate
    public TransformationConfigurationResource(
            final @Reference TransformationConfigurationRegistry transformationConfigurationRegistry,
            final @Reference ManagedTransformationConfigurationProvider managedTransformationConfigurationProvider) {
        this.transformationConfigurationRegistry = transformationConfigurationRegistry;
        this.managedTransformationConfigurationProvider = managedTransformationConfigurationProvider;
    }

    @GET
    @Path("configuration")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getTransformationConfigurations", summary = "Get a list of all transformation configurations", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TransformationConfigurationDTO.class)))) })
    public Response getTransformationConfigurations() {
        logger.debug("Received HTTP GET request at '{}'", uriInfo.getPath());
        Stream<TransformationConfigurationDTO> stream = transformationConfigurationRegistry.stream()
                .map(TransformationConfigurationDTO::new).peek(c -> c.editable = isEditable(c.uid));
        return Response.ok(new Stream2JSONInputStream(stream)).build();
    }

    @GET
    @Path("configuration/{uid: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getTransformationConfiguration", summary = "Get a single transformation configuration", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = TransformationConfiguration.class))),
            @ApiResponse(responseCode = "404", description = "Not found") })
    public Response getTransformationConfiguration(
            @PathParam("uid") @Parameter(description = "Configuration UID") String uid) {
        logger.debug("Received HTTP GET request at '{}'", uriInfo.getPath());

        TransformationConfiguration configuration = transformationConfigurationRegistry.get(uid);
        if (configuration == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(configuration).build();
    }

    @PUT
    @Path("configuration/{uid: .*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(operationId = "putTransformationConfiguration", summary = "Get a single transformation configuration", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "405", description = "Configuration not editable"),
            @ApiResponse(responseCode = "400", description = "Bad Request (content missing or invalid)") })
    public Response putTransformationConfiguration(
            @PathParam("uid") @Parameter(description = "Configuration UID") String uid,
            @Parameter(description = "configuration", required = true) @Nullable TransformationConfigurationDTO newConfiguration) {
        logger.debug("Received HTTP PUT request at '{}'", uriInfo.getPath());

        TransformationConfiguration oldConfiguration = transformationConfigurationRegistry.get(uid);
        if (oldConfiguration != null && !isEditable(uid)) {
            return Response.status(Response.Status.METHOD_NOT_ALLOWED).build();
        }

        if (newConfiguration == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Content missing.").build();
        }

        if (!uid.equals(newConfiguration.uid)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("UID of configuration and path not matching.")
                    .build();
        }

        TransformationConfiguration transformationConfiguration = new TransformationConfiguration(newConfiguration.uid,
                newConfiguration.label, newConfiguration.type, newConfiguration.language, newConfiguration.content);
        try {
            if (oldConfiguration != null) {
                managedTransformationConfigurationProvider.update(transformationConfiguration);
            } else {
                managedTransformationConfigurationProvider.add(transformationConfiguration);
            }
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Objects.requireNonNullElse(e.getMessage(), ""))
                    .build();
        }

        return Response.ok().build();
    }

    @DELETE
    @Path("configuration/{uid: .*}")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(operationId = "deleteTransformationConfiguration", summary = "Get a single transformation configuration", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "405", description = "Configuration not editable"),
            @ApiResponse(responseCode = "404", description = "UID not fond") })
    public Response deleteTransformationConfiguration(
            @PathParam("uid") @Parameter(description = "Configuration UID") String uid) {
        logger.debug("Received HTTP DELETE request at '{}'", uriInfo.getPath());

        TransformationConfiguration oldConfiguration = transformationConfigurationRegistry.get(uid);
        if (oldConfiguration == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (!isEditable(uid)) {
            return Response.status(Response.Status.METHOD_NOT_ALLOWED).build();
        }

        managedTransformationConfigurationProvider.remove(uid);

        return Response.ok().build();
    }

    private boolean isEditable(String uid) {
        return managedTransformationConfigurationProvider.get(uid) != null;
    }
}
