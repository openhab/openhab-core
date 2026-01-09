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
package org.openhab.core.io.rest.core.internal.tag;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Role;
import org.openhab.core.common.registry.RegistryChangedRunnableListener;
import org.openhab.core.io.rest.JSONResponse;
import org.openhab.core.io.rest.LocaleService;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.io.rest.Stream2JSONInputStream;
import org.openhab.core.semantics.ManagedSemanticTagProvider;
import org.openhab.core.semantics.SemanticTag;
import org.openhab.core.semantics.SemanticTagImpl;
import org.openhab.core.semantics.SemanticTagRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JSONRequired;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

/**
 * This class acts as a REST resource for retrieving a list of tags.
 *
 * @author Jimmy Tanagra - Initial contribution
 * @author Laurent Garnier - Extend REST API to allow adding/updating/removing user tags
 */
@Component
@JaxrsResource
@JaxrsName(TagResource.PATH_TAGS)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(TagResource.PATH_TAGS)
@io.swagger.v3.oas.annotations.tags.Tag(name = TagResource.PATH_TAGS)
@NonNullByDefault
public class TagResource implements RESTResource {

    /** The URI path to this resource */
    public static final String PATH_TAGS = "tags";

    private final LocaleService localeService;
    private final SemanticTagRegistry semanticTagRegistry;
    private final ManagedSemanticTagProvider managedSemanticTagProvider;
    private final RegistryChangedRunnableListener<SemanticTag> resetLastModifiedChangeListener = new RegistryChangedRunnableListener<>(
            () -> lastModified = null);

    private @Nullable Date lastModified = null;

    // TODO pattern in @Path

    @Activate
    public TagResource(final @Reference LocaleService localeService,
            final @Reference SemanticTagRegistry semanticTagRegistry,
            final @Reference ManagedSemanticTagProvider managedSemanticTagProvider) {
        this.localeService = localeService;
        this.semanticTagRegistry = semanticTagRegistry;
        this.managedSemanticTagProvider = managedSemanticTagProvider;

        this.semanticTagRegistry.addRegistryChangeListener(resetLastModifiedChangeListener);
    }

    @Deactivate
    void deactivate() {
        this.semanticTagRegistry.removeRegistryChangeListener(resetLastModifiedChangeListener);
    }

    @GET
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getSemanticTags", summary = "Get all available semantic tags.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = EnrichedSemanticTagDTO.class)))) })
    public Response getTags(final @Context Request request, final @Context UriInfo uriInfo,
            final @Context HttpHeaders httpHeaders,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language) {
        if (lastModified != null) {
            Response.ResponseBuilder responseBuilder = request.evaluatePreconditions(lastModified);
            if (responseBuilder != null) {
                // send 304 Not Modified
                return responseBuilder.build();
            }
        } else {
            lastModified = Date.from(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        }

        final Locale locale = localeService.getLocale(language);

        Stream<EnrichedSemanticTagDTO> tagsStream = semanticTagRegistry.getAll().stream()
                .sorted(Comparator.comparing(SemanticTag::getUID))
                .map(t -> new EnrichedSemanticTagDTO(t.localized(locale), semanticTagRegistry.isEditable(t)));
        return Response.ok(new Stream2JSONInputStream(tagsStream)).lastModified(lastModified)
                .cacheControl(RESTConstants.CACHE_CONTROL).build();
    }

    @GET
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Path("/{tagId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getSemanticTagAndSubTags", summary = "Gets a semantic tag and its sub tags.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = EnrichedSemanticTagDTO.class)))),
            @ApiResponse(responseCode = "404", description = "Semantic tag not found.") })
    public Response getTagAndSubTags(final @Context Request request,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @PathParam("tagId") @Parameter(description = "tag id") String tagId) {
        if (lastModified != null) {
            Response.ResponseBuilder responseBuilder = request.evaluatePreconditions(lastModified);
            if (responseBuilder != null) {
                // send 304 Not Modified
                return responseBuilder.build();
            }
        } else {
            lastModified = Date.from(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        }

        final Locale locale = localeService.getLocale(language);
        String uid = tagId.trim();

        SemanticTag tag = semanticTagRegistry.get(uid);
        if (tag != null) {
            Stream<EnrichedSemanticTagDTO> tagsStream = semanticTagRegistry.getSubTree(tag).stream()
                    .sorted(Comparator.comparing(SemanticTag::getUID))
                    .map(t -> new EnrichedSemanticTagDTO(t.localized(locale), semanticTagRegistry.isEditable(t)));
            return Response.ok(new Stream2JSONInputStream(tagsStream)).lastModified(lastModified)
                    .cacheControl(RESTConstants.CACHE_CONTROL).build();
        } else {
            return JSONResponse.createErrorResponse(Status.NOT_FOUND, "Tag " + uid + " does not exist!");
        }
    }

    @POST
    @RolesAllowed({ Role.ADMIN })
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(operationId = "createSemanticTag", summary = "Creates a new semantic tag and adds it to the registry.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "201", description = "Created", content = @Content(schema = @Schema(implementation = EnrichedSemanticTagDTO.class))),
                    @ApiResponse(responseCode = "400", description = "The tag identifier is invalid or the tag label is missing."),
                    @ApiResponse(responseCode = "409", description = "A tag with the same identifier already exists.") })
    public Response create(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @Parameter(description = "tag data", required = true) EnrichedSemanticTagDTO data) {
        final Locale locale = localeService.getLocale(language);

        if (data.uid == null || data.uid.isBlank()) {
            return JSONResponse.createErrorResponse(Status.BAD_REQUEST, "Tag identifier is required!");
        }
        if (data.label == null || data.label.isBlank()) {
            return JSONResponse.createErrorResponse(Status.BAD_REQUEST, "Tag label is required!");
        }

        String uid = data.uid.trim();

        // check if a tag with this UID already exists
        SemanticTag tag = semanticTagRegistry.get(uid);
        if (tag != null) {
            // report a conflict
            return JSONResponse.createResponse(Status.CONFLICT,
                    new EnrichedSemanticTagDTO(tag.localized(locale), semanticTagRegistry.isEditable(tag)),
                    "Tag " + uid + " already exists!");
        }

        tag = new SemanticTagImpl(uid, data.label, data.description, data.synonyms);

        // Check that a tag with this uid can be added to the registry
        if (!semanticTagRegistry.canBeAdded(tag)) {
            return JSONResponse.createErrorResponse(Status.BAD_REQUEST, "Invalid tag identifier " + uid);
        }

        managedSemanticTagProvider.add(tag);

        return JSONResponse.createResponse(Status.CREATED,
                new EnrichedSemanticTagDTO(tag.localized(locale), semanticTagRegistry.isEditable(tag)), null);
    }

    @DELETE
    @RolesAllowed({ Role.ADMIN })
    @Path("/{tagId}")
    @Operation(operationId = "removeSemanticTag", summary = "Removes a semantic tag and its sub tags from the registry.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK, was deleted."),
                    @ApiResponse(responseCode = "404", description = "Semantic tag not found."),
                    @ApiResponse(responseCode = "405", description = "Semantic tag not removable.") })
    public Response remove(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @PathParam("tagId") @Parameter(description = "tag id") String tagId) {
        String uid = tagId.trim();

        // check whether tag exists and throw 404 if not
        SemanticTag tag = semanticTagRegistry.get(uid);
        if (tag == null) {
            return JSONResponse.createErrorResponse(Status.NOT_FOUND, "Tag " + uid + " does not exist!");
        }

        // Check that tag is removable, 405 otherwise
        if (!semanticTagRegistry.isEditable(tag)) {
            return JSONResponse.createErrorResponse(Status.METHOD_NOT_ALLOWED, "Tag " + uid + " is not removable.");
        }

        semanticTagRegistry.removeSubTree(tag);

        return Response.ok(null, MediaType.TEXT_PLAIN).build();
    }

    @PUT
    @RolesAllowed({ Role.ADMIN })
    @Path("/{tagId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(operationId = "updateSemanticTag", summary = "Updates a semantic tag.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = EnrichedSemanticTagDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Semantic tag not found."),
                    @ApiResponse(responseCode = "405", description = "Semantic tag not editable.") })
    public Response update(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @PathParam("tagId") @Parameter(description = "tag id") String tagId,
            @Parameter(description = "tag data", required = true) EnrichedSemanticTagDTO data) {
        final Locale locale = localeService.getLocale(language);

        String uid = tagId.trim();

        // check whether tag exists and throw 404 if not
        SemanticTag tag = semanticTagRegistry.get(uid);
        if (tag == null) {
            return JSONResponse.createErrorResponse(Status.NOT_FOUND, "Tag " + uid + " does not exist!");
        }

        // Check that tag is editable, 405 otherwise
        if (!semanticTagRegistry.isEditable(tag)) {
            return JSONResponse.createErrorResponse(Status.METHOD_NOT_ALLOWED, "Tag " + uid + " is not editable.");
        }

        tag = new SemanticTagImpl(uid, data.label != null ? data.label : tag.getLabel(),
                data.description != null ? data.description : tag.getDescription(),
                data.synonyms != null ? data.synonyms : tag.getSynonyms());
        managedSemanticTagProvider.update(tag);

        return JSONResponse.createResponse(Status.OK,
                new EnrichedSemanticTagDTO(tag.localized(locale), semanticTagRegistry.isEditable(tag)), null);
    }
}
