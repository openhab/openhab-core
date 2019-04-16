/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.io.rest.core.internal.config;

import java.net.URI;
import java.util.Locale;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.ConfigDescriptionRegistry;
import org.eclipse.smarthome.config.core.dto.ConfigDescriptionDTO;
import org.eclipse.smarthome.config.core.dto.ConfigDescriptionDTOMapper;
import org.eclipse.smarthome.core.auth.Role;
import org.eclipse.smarthome.io.rest.LocaleService;
import org.eclipse.smarthome.io.rest.RESTService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * {@link ConfigDescriptionResource} provides access to {@link ConfigDescription}s via REST.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Chris Jackson - Modify response to use JSONResponse
 * @author Franck Dechavanne - Added DTOs to ApiResponses
 */
@Path(ConfigDescriptionResource.PATH_CONFIG_DESCRIPTIONS)
@RolesAllowed({ Role.ADMIN })
@Api(value = ConfigDescriptionResource.PATH_CONFIG_DESCRIPTIONS)
@Produces(MediaType.APPLICATION_JSON)
@Component(immediate = true)
@JaxrsApplicationSelect("(osgi.jaxrs.name=" + RESTService.REST_APP_NAME + ")")
@JaxrsResource
@NonNullByDefault
public class ConfigDescriptionResource {

    /** The URI path to this resource */
    public static final String PATH_CONFIG_DESCRIPTIONS = "config-descriptions";

    @Reference
    private @NonNullByDefault({}) ConfigDescriptionRegistry configDescriptionRegistry;

    @Reference
    private @NonNullByDefault({}) LocaleService localeService;

    @GET
    @ApiOperation(value = "Gets all available config descriptions.", response = ConfigDescriptionDTO.class, responseContainer = "List")
    @ApiResponses(value = @ApiResponse(code = 200, message = "OK", response = ConfigDescriptionDTO.class, responseContainer = "List"))
    public Stream<?> getAll(@HeaderParam("Accept-Language") @ApiParam(value = "Accept-Language") String language, //
            @QueryParam("scheme") @ApiParam(value = "scheme filter", required = false) @Nullable String scheme) {
        Locale locale = localeService.getLocale(language);

        return configDescriptionRegistry.getConfigDescriptions(locale).stream()
                .filter(configDescription -> scheme == null || scheme.equals(configDescription.getUID().getScheme()))
                .map(ConfigDescriptionDTOMapper::map);
    }

    @GET
    @Path("/{uri}")
    @ApiOperation(value = "Gets a config description by URI.", response = ConfigDescriptionDTO.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = ConfigDescriptionDTO.class),
            @ApiResponse(code = 400, message = "Invalid URI syntax"), @ApiResponse(code = 404, message = "Not found") })
    public ConfigDescriptionDTO getByURI(
            @HeaderParam("Accept-Language") @ApiParam(value = "Accept-Language") String language,
            @PathParam("uri") @ApiParam(value = "uri") String uri) {
        Locale locale = localeService.getLocale(language);
        URI uriObject = UriBuilder.fromPath(uri).build();
        ConfigDescription configDescription = this.configDescriptionRegistry.getConfigDescription(uriObject, locale);
        if (configDescription == null) {
            throw new NotFoundException("Configuration not found: " + uri);
        }
        return ConfigDescriptionDTOMapper.map(configDescription);
    }
}
