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
package org.openhab.core.io.rest.core.internal.config;

import java.net.URI;
import java.util.Collection;
import java.util.Locale;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Role;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.dto.ConfigDescriptionDTO;
import org.openhab.core.io.rest.JSONResponse;
import org.openhab.core.io.rest.LocaleService;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.io.rest.Stream2JSONInputStream;
import org.openhab.core.io.rest.core.config.EnrichedConfigDescriptionDTOMapper;
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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * {@link ConfigDescriptionResource} provides access to {@link ConfigDescription}s via REST.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Chris Jackson - Modify response to use JSONResponse
 * @author Franck Dechavanne - Added DTOs to ApiResponses
 * @author Markus Rathgeb - Migrated to JAX-RS Whiteboard Specification
 * @author Wouter Born - Migrated to OpenAPI annotations
 */
@Component
@JaxrsResource
@JaxrsName(ConfigDescriptionResource.PATH_CONFIG_DESCRIPTIONS)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(ConfigDescriptionResource.PATH_CONFIG_DESCRIPTIONS)
@RolesAllowed({ Role.ADMIN })
@SecurityRequirement(name = "oauth2", scopes = { "admin" })
@Tag(name = ConfigDescriptionResource.PATH_CONFIG_DESCRIPTIONS)
@NonNullByDefault
public class ConfigDescriptionResource implements RESTResource {

    /** The URI path to this resource */
    public static final String PATH_CONFIG_DESCRIPTIONS = "config-descriptions";

    private final ConfigDescriptionRegistry configDescriptionRegistry;
    private final LocaleService localeService;

    @Activate
    public ConfigDescriptionResource( //
            final @Reference ConfigDescriptionRegistry configDescriptionRegistry,
            final @Reference LocaleService localeService) {
        this.configDescriptionRegistry = configDescriptionRegistry;
        this.localeService = localeService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getConfigDescriptions", summary = "Gets all available config descriptions.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ConfigDescriptionDTO.class)))) })
    public Response getAll(
            @HeaderParam("Accept-Language") @Parameter(description = "language") @Nullable String language, //
            @QueryParam("scheme") @Parameter(description = "scheme filter") @Nullable String scheme) {
        Locale locale = localeService.getLocale(language);
        Collection<ConfigDescription> configDescriptions = configDescriptionRegistry.getConfigDescriptions(locale);
        return Response.ok(new Stream2JSONInputStream(configDescriptions.stream().filter(configDescription -> {
            return scheme == null || scheme.equals(configDescription.getUID().getScheme());
        }).map(EnrichedConfigDescriptionDTOMapper::map))).build();
    }

    @GET
    @Path("/{uri}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getConfigDescriptionByURI", summary = "Gets a config description by URI.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = ConfigDescriptionDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid URI syntax"),
            @ApiResponse(responseCode = "404", description = "Not found") })
    public Response getByURI(
            @HeaderParam("Accept-Language") @Parameter(description = "language") @Nullable String language,
            @PathParam("uri") @Parameter(description = "uri") String uri) {
        Locale locale = localeService.getLocale(language);
        URI uriObject = UriBuilder.fromPath(uri).build();
        ConfigDescription configDescription = configDescriptionRegistry.getConfigDescription(uriObject, locale);
        return configDescription != null
                ? JSONResponse.createResponse(Status.OK, EnrichedConfigDescriptionDTOMapper.map(configDescription),
                        null)
                : JSONResponse.createErrorResponse(Status.NOT_FOUND, "Configuration not found: " + uri);
    }
}
