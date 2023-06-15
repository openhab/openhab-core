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
package org.openhab.core.io.rest.transform.internal;

import java.util.Collection;
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
import org.openhab.core.io.rest.transform.TransformationDTO;
import org.openhab.core.transform.ManagedTransformationProvider;
import org.openhab.core.transform.Transformation;
import org.openhab.core.transform.TransformationRegistry;
import org.openhab.core.transform.TransformationService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
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
 * The {@link TransformationResource} is a REST resource for handling transformations
 *
 * @author Jan N. Klug - Initial contribution
 */
@Component(immediate = true)
@JaxrsResource
@JaxrsName(TransformationResource.PATH_TRANSFORMATIONS)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(TransformationResource.PATH_TRANSFORMATIONS)
@RolesAllowed({ Role.ADMIN })
@SecurityRequirement(name = "oauth2", scopes = { "admin" })
@Tag(name = TransformationResource.PATH_TRANSFORMATIONS)
@NonNullByDefault
public class TransformationResource implements RESTResource {
    public static final String PATH_TRANSFORMATIONS = "transformations";

    private final Logger logger = LoggerFactory.getLogger(TransformationResource.class);
    private final TransformationRegistry transformationRegistry;
    private final ManagedTransformationProvider managedTransformationProvider;
    private final BundleContext bundleContext;

    private @Context @NonNullByDefault({}) UriInfo uriInfo;

    @Activate
    public TransformationResource(final @Reference TransformationRegistry transformationRegistry,
            final @Reference ManagedTransformationProvider managedTransformationProvider,
            final BundleContext bundleContext) {
        this.transformationRegistry = transformationRegistry;
        this.managedTransformationProvider = managedTransformationProvider;
        this.bundleContext = bundleContext;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getTransformations", summary = "Get a list of all transformations", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TransformationDTO.class)))) })
    public Response getTransformations() {
        logger.debug("Received HTTP GET request at '{}'", uriInfo.getPath());
        Stream<TransformationDTO> stream = transformationRegistry.stream().map(TransformationDTO::new)
                .peek(c -> c.editable = isEditable(c.uid));
        return Response.ok(new Stream2JSONInputStream(stream)).build();
    }

    @GET
    @Path("services")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getTransformationServices", summary = "Get all transformation services", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class)))) })
    public Response getTransformationServices() {
        try {
            Collection<ServiceReference<TransformationService>> refs = bundleContext
                    .getServiceReferences(TransformationService.class, null);
            Stream<String> services = refs.stream()
                    .map(ref -> (String) ref.getProperty(TransformationService.SERVICE_PROPERTY_NAME))
                    .filter(Objects::nonNull).map(Objects::requireNonNull).sorted();
            return Response.ok(new Stream2JSONInputStream(services)).build();
        } catch (InvalidSyntaxException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("{uid}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getTransformation", summary = "Get a single transformation", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = Transformation.class))),
            @ApiResponse(responseCode = "404", description = "Not found") })
    public Response getTransformation(@PathParam("uid") @Parameter(description = "Transformation UID") String uid) {
        logger.debug("Received HTTP GET request at '{}'", uriInfo.getPath());

        Transformation transformation = transformationRegistry.get(uid);
        if (transformation == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        TransformationDTO dto = new TransformationDTO(transformation);
        dto.editable = isEditable(uid);
        return Response.ok(dto).build();
    }

    @PUT
    @Path("{uid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(operationId = "putTransformation", summary = "Put a single transformation", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Bad Request (content missing or invalid)"),
            @ApiResponse(responseCode = "405", description = "Transformation not editable") })
    public Response putTransformation(@PathParam("uid") @Parameter(description = "Transformation UID") String uid,
            @Parameter(description = "transformation", required = true) @Nullable TransformationDTO newTransformation) {
        logger.debug("Received HTTP PUT request at '{}'", uriInfo.getPath());

        Transformation oldTransformation = transformationRegistry.get(uid);
        if (oldTransformation != null && !isEditable(uid)) {
            return Response.status(Response.Status.METHOD_NOT_ALLOWED).build();
        }

        if (newTransformation == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Content missing.").build();
        }

        if (!uid.equals(newTransformation.uid)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("UID of transformation and path not matching.")
                    .build();
        }

        Transformation transformation = new Transformation(newTransformation.uid, newTransformation.label,
                newTransformation.type, newTransformation.configuration);
        try {
            if (oldTransformation != null) {
                managedTransformationProvider.update(transformation);
            } else {
                managedTransformationProvider.add(transformation);
            }
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Objects.requireNonNullElse(e.getMessage(), ""))
                    .build();
        }

        return Response.ok().build();
    }

    @DELETE
    @Path("{uid}")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(operationId = "deleteTransformation", summary = "Get a single transformation", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "UID not found"),
            @ApiResponse(responseCode = "405", description = "Transformation not editable") })
    public Response deleteTransformation(@PathParam("uid") @Parameter(description = "Transformation UID") String uid) {
        logger.debug("Received HTTP DELETE request at '{}'", uriInfo.getPath());

        Transformation oldTransformation = transformationRegistry.get(uid);
        if (oldTransformation == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (!isEditable(uid)) {
            return Response.status(Response.Status.METHOD_NOT_ALLOWED).build();
        }

        managedTransformationProvider.remove(uid);

        return Response.ok().build();
    }

    private boolean isEditable(String uid) {
        return managedTransformationProvider.get(uid) != null;
    }
}
