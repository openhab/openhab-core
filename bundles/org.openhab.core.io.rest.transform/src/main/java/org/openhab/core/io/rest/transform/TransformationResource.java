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
package org.openhab.core.io.rest.transform;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
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
import org.openhab.core.OpenHAB;
import org.openhab.core.auth.Role;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.io.rest.Stream2JSONInputStream;
import org.openhab.core.transform.ManagedTransformationService;
import org.openhab.core.transform.TransformationService;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * The {@link TransformationResource} is a
 *
 * @author Jan N. Klug - Initial contribution
 */
@Component(immediate = true)
@JaxrsResource
@JaxrsName(TransformationResource.PATH_TRANSFORM)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(TransformationResource.PATH_TRANSFORM)
@RolesAllowed({ Role.ADMIN })
@SecurityRequirement(name = "oauth2", scopes = { "admin" })
@Tag(name = TransformationResource.PATH_TRANSFORM)
@NonNullByDefault
public class TransformationResource implements RESTResource {
    public static final String PATH_TRANSFORM = "transform";

    private static final java.nio.file.Path TRANSFORM_FOLDER = java.nio.file.Path.of(OpenHAB.getConfigFolder(),
            TransformationService.TRANSFORM_FOLDER_NAME);

    private final Logger logger = LoggerFactory.getLogger(TransformationResource.class);
    private @Context @NonNullByDefault({}) UriInfo uriInfo;

    private final List<ManagedTransformationService> transformationServices = new CopyOnWriteArrayList<>();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addTransformationService(ManagedTransformationService transformationService) {
        transformationServices.add(transformationService);
    }

    protected void removeTransformationService(ManagedTransformationService transformationService) {
        transformationServices.remove(transformationService);
    }

    @GET
    @Path("files")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getTransformationFiles", summary = "Get a list of all transformation files", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class)))),
            @ApiResponse(responseCode = "500", description = "Could not list files") })
    public Response getTransformationFiles() {
        logger.debug("Received HTTP GET request at '{}'", uriInfo.getPath());

        try (Stream<java.nio.file.Path> files = Files.walk(TRANSFORM_FOLDER)) {
            List<String> fileList = files.filter(this::fileFilter).map(p -> TRANSFORM_FOLDER.relativize(p).toString())
                    .collect(Collectors.toList());
            return Response.ok(new Stream2JSONInputStream(fileList.stream())).build();
        } catch (IOException ignored) {
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    @GET
    @Path("files/{fileName: .+}")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(operationId = "getTransformationFile", summary = "Get the content of a transformation file", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "403", description = "Path is forbidden"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Could not read file or invalid path") })
    public Response getTransformationFile(
            @PathParam("fileName") @Parameter(description = "file name") String fileName) {
        logger.debug("Received HTTP GET request at '{}'", uriInfo.getPath());

        try {
            java.nio.file.Path filePath = TRANSFORM_FOLDER.resolve(fileName).normalize();
            if (!filePath.startsWith(TRANSFORM_FOLDER)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            if (!Files.exists(filePath)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            return Response.ok(content, MediaType.TEXT_PLAIN).build();
        } catch (InvalidPathException | IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PUT
    @Path("files/{fileName: .+}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Operation(operationId = "putTransformationFile", summary = "Add or modify a transformation file", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Invalid extension of content invalid"),
            @ApiResponse(responseCode = "403", description = "Path is forbidden"),
            @ApiResponse(responseCode = "500", description = "Could not write file or invalid path") })
    public Response putTransformationFile(@PathParam("fileName") @Parameter(description = "file name") String fileName,
            @Parameter(description = "file content", required = true) @Nullable String content) {
        logger.debug("Received HTTP PUT request at '{}'", uriInfo.getPath());

        try {
            java.nio.file.Path filePath = TRANSFORM_FOLDER.resolve(fileName).normalize();
            if (!filePath.startsWith(TRANSFORM_FOLDER)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            ManagedTransformationService transformationService = transformationServices.stream()
                    .filter(t -> t.supportedFileExtensions().stream().anyMatch(fileName::endsWith)).findAny()
                    .orElse(null);

            if (transformationService != null && transformationService.configurationIsValid(content)) {
                Files.writeString(filePath, Objects.requireNonNullElse(content, ""), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE);

                return Response.ok().build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

        } catch (InvalidPathException | IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check if file is a regular file, is not hidden and has an extension that belongs to one of the registered
     * services
     * 
     * @param path {@link java.nio.file.Path} to a single directory entry
     * @return true if all conditions are fulfilled, false otherwise
     */
    private boolean fileFilter(java.nio.file.Path path) {
        try {
            return Files.isRegularFile(path) && !Files.isHidden(path) && hasValidExtension(path);
        } catch (IOException ignored) {
        }
        return false;
    }

    private boolean hasValidExtension(java.nio.file.Path path) {
        String pathStr = path.toString();
        return transformationServices.stream().map(ManagedTransformationService::supportedFileExtensions)
                .flatMap(List::stream).anyMatch(pathStr::endsWith);
    }
}
