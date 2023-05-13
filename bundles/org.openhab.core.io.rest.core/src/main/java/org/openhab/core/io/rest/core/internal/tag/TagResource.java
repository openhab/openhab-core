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
package org.openhab.core.io.rest.core.internal.tag;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Role;
import org.openhab.core.io.rest.JSONResponse;
import org.openhab.core.io.rest.LocaleService;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.semantics.model.equipment.Equipments;
import org.openhab.core.semantics.model.location.Locations;
import org.openhab.core.semantics.model.point.Points;
import org.openhab.core.semantics.model.property.Properties;
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
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * This class acts as a REST resource for retrieving a list of tags.
 *
 * @author Jimmy Tanagra - Initial contribution
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

    @Activate
    public TagResource(final @Reference LocaleService localeService) {
        this.localeService = localeService;
    }

    @GET
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getTags", summary = "Get all available tags.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TagDTO.class)))) })
    public Response getTags(final @Context UriInfo uriInfo, final @Context HttpHeaders httpHeaders,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language) {
        final Locale locale = localeService.getLocale(language);

        Map<String, List<TagDTO>> tags = Map.of( //
                Locations.class.getSimpleName(), Locations.stream().map(tag -> new TagDTO(tag, locale)).toList(), //
                Equipments.class.getSimpleName(), Equipments.stream().map(tag -> new TagDTO(tag, locale)).toList(), //
                Points.class.getSimpleName(), Points.stream().map(tag -> new TagDTO(tag, locale)).toList(), //
                Properties.class.getSimpleName(), Properties.stream().map(tag -> new TagDTO(tag, locale)).toList() //
        );

        return JSONResponse.createResponse(Status.OK, tags, null);
    }
}
