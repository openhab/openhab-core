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
package org.eclipse.smarthome.io.rest.core.internal.binding;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.ConfigDescriptionRegistry;
import org.eclipse.smarthome.config.core.ConfigUtil;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.auth.Role;
import org.eclipse.smarthome.core.binding.BindingInfo;
import org.eclipse.smarthome.core.binding.BindingInfoRegistry;
import org.eclipse.smarthome.core.binding.dto.BindingInfoDTO;
import org.eclipse.smarthome.io.rest.LocaleService;
import org.eclipse.smarthome.io.rest.RESTService;
import org.eclipse.smarthome.io.rest.core.config.ConfigurationService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * This class acts as a REST resource for bindings
 *
 * @author Dennis Nobel - Initial contribution
 * @author Kai Kreuzer - refactored for using the OSGi JAX-RS connector
 * @author Yordan Zhelev - Added Swagger annotations
 * @author Franck Dechavanne - Added DTOs to ApiResponses
 */
@Path(BindingResource.PATH_BINDINGS)
@RolesAllowed({ Role.ADMIN })
@Api(value = BindingResource.PATH_BINDINGS)
@Component(immediate = true)
@JaxrsApplicationSelect("(osgi.jaxrs.name=" + RESTService.REST_APP_NAME + ")")
@JaxrsResource
@NonNullByDefault
public class BindingResource {

    /** The URI path to this resource */
    public static final String PATH_BINDINGS = "bindings";

    private final Logger logger = LoggerFactory.getLogger(BindingResource.class);

    @Reference
    private @NonNullByDefault({}) ConfigurationService configurationService;

    @Reference
    private @NonNullByDefault({}) ConfigDescriptionRegistry configDescRegistry;

    @Reference
    private @NonNullByDefault({}) BindingInfoRegistry bindingInfoRegistry;

    @Reference
    private @NonNullByDefault({}) LocaleService localeService;

    @Context
    private @NonNullByDefault({}) UriInfo uriInfo;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get all bindings.", response = BindingInfoDTO.class, responseContainer = "Set")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = BindingInfoDTO.class, responseContainer = "Set") })
    public Stream<?> getAll(@HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") String language) {
        final Locale locale = localeService.getLocale(language);
        return bindingInfoRegistry.getBindingInfos(locale).stream().map(b -> map(b));
    }

    @SuppressWarnings("null")
    @GET
    @Path("/{bindingId}/config")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Get binding configuration for given binding ID.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
            @ApiResponse(code = 404, message = "Binding does not exist"),
            @ApiResponse(code = 500, message = "Configuration can not be read due to internal error") })
    public Map<String, Object> getConfiguration(
            @PathParam("bindingId") @ApiParam(value = "service ID", required = true) String bindingId) {
        try {
            String configId = getConfigId(bindingId);
            if (configId == null) {
                throw new NotFoundException();
            }
            Configuration configuration = configurationService.get(configId);
            return configuration != null ? configuration.getProperties() : Collections.emptyMap();
        } catch (IOException ex) {
            throw new InternalServerErrorException(ex);
        }
    }

    @SuppressWarnings("null")
    @PUT
    @Path("/{bindingId}/config")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Updates a binding configuration for given binding ID and returns the old configuration.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
            @ApiResponse(code = 204, message = "No old configuration"),
            @ApiResponse(code = 404, message = "Binding does not exist"),
            @ApiResponse(code = 500, message = "Configuration can not be updated due to internal error") })
    public Map<String, Object> updateConfiguration(
            @PathParam("bindingId") @ApiParam(value = "service ID", required = true) String bindingId,
            Map<String, Object> configuration) {
        try {
            String configId = getConfigId(bindingId);
            if (configId == null) {
                logger.warn("Cannot get config id for binding id '{}', probably because binding does not exist.",
                        bindingId);
                throw new NotFoundException();
            }
            Configuration oldConfiguration = configurationService.get(configId);
            configurationService.update(configId, new Configuration(normalizeConfiguration(configuration, bindingId)));
            return oldConfiguration != null ? oldConfiguration.getProperties() : Collections.emptyMap();
        } catch (IOException ex) {
            throw new InternalServerErrorException(ex);
        }
    }

    private @Nullable Map<String, Object> normalizeConfiguration(@Nullable Map<String, Object> properties,
            String bindingId) {
        if (properties == null || properties.isEmpty()) {
            return properties;
        }

        BindingInfo bindingInfo = this.bindingInfoRegistry.getBindingInfo(bindingId);
        if (bindingInfo == null || bindingInfo.getConfigDescriptionURI() == null) {
            return properties;
        }

        ConfigDescription configDesc = configDescRegistry.getConfigDescription(bindingInfo.getConfigDescriptionURI());
        if (configDesc == null) {
            return properties;
        }

        return ConfigUtil.normalizeTypes(properties, Collections.singletonList(configDesc));
    }

    private @Nullable String getConfigId(String bindingId) {
        BindingInfo bindingInfo = this.bindingInfoRegistry.getBindingInfo(bindingId);
        if (bindingInfo != null) {
            return bindingInfo.getServiceId();
        } else {
            return null;
        }
    }

    private BindingInfoDTO map(BindingInfo bindingInfo) {
        URI configDescriptionURI = bindingInfo.getConfigDescriptionURI();
        return new BindingInfoDTO(bindingInfo.getUID(), bindingInfo.getName(), bindingInfo.getAuthor(),
                bindingInfo.getDescription(), configDescriptionURI != null ? configDescriptionURI.toString() : null);
    }
}
