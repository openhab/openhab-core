/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.io.rest.core.internal.binding;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.openhab.core.auth.Role;
import org.openhab.core.binding.BindingInfo;
import org.openhab.core.binding.BindingInfoRegistry;
import org.openhab.core.binding.dto.BindingInfoDTO;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.ConfigUtil;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.io.rest.LocaleService;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.io.rest.Stream2JSONInputStream;
import org.openhab.core.io.rest.core.config.ConfigurationService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * This class acts as a REST resource for bindings and is registered with the
 * Jersey servlet.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Kai Kreuzer - refactored for using the OSGi JAX-RS connector
 * @author Yordan Zhelev - Added Swagger annotations
 * @author Franck Dechavanne - Added DTOs to ApiResponses
 */
@Path(BindingResource.PATH_BINDINGS)
@RolesAllowed({ Role.ADMIN })
@Api(value = BindingResource.PATH_BINDINGS)
@Component
public class BindingResource implements RESTResource {

    /** The URI path to this resource */
    public static final String PATH_BINDINGS = "bindings";

    private final Logger logger = LoggerFactory.getLogger(BindingResource.class);

    private ConfigurationService configurationService;
    private ConfigDescriptionRegistry configDescRegistry;

    private BindingInfoRegistry bindingInfoRegistry;

    private LocaleService localeService;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setBindingInfoRegistry(BindingInfoRegistry bindingInfoRegistry) {
        this.bindingInfoRegistry = bindingInfoRegistry;
    }

    protected void unsetBindingInfoRegistry(BindingInfoRegistry bindingInfoRegistry) {
        this.bindingInfoRegistry = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setLocaleService(LocaleService localeService) {
        this.localeService = localeService;
    }

    protected void unsetLocaleService(LocaleService localeService) {
        this.localeService = null;
    }

    @Context
    UriInfo uriInfo;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get all bindings.", response = BindingInfoDTO.class, responseContainer = "Set")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = BindingInfoDTO.class, responseContainer = "Set") })
    public Response getAll(@HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") String language) {
        final Locale locale = localeService.getLocale(language);
        Set<BindingInfo> bindingInfos = bindingInfoRegistry.getBindingInfos(locale);

        return Response.ok(new Stream2JSONInputStream(bindingInfos.stream().map(b -> map(b, locale)))).build();
    }

    @GET
    @Path("/{bindingId}/config")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Get binding configuration for given binding ID.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
            @ApiResponse(code = 404, message = "Binding does not exist"),
            @ApiResponse(code = 500, message = "Configuration can not be read due to internal error") })
    public Response getConfiguration(
            @PathParam("bindingId") @ApiParam(value = "service ID", required = true) String bindingId) {
        try {
            String configId = getConfigId(bindingId);
            if (configId == null) {
                logger.warn("Cannot get config id for binding id '{}', probably because binding does not exist.",
                        bindingId);
                return Response.status(404).build();
            }
            Configuration configuration = configurationService.get(configId);
            return configuration != null ? Response.ok(configuration.getProperties()).build()
                    : Response.ok(Collections.emptyMap()).build();
        } catch (IOException ex) {
            logger.error("Cannot get configuration for service {}: {}", bindingId, ex.getMessage(), ex);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PUT
    @Path("/{bindingId}/config")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Updates a binding configuration for given binding ID and returns the old configuration.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
            @ApiResponse(code = 204, message = "No old configuration"),
            @ApiResponse(code = 404, message = "Binding does not exist"),
            @ApiResponse(code = 500, message = "Configuration can not be updated due to internal error") })
    public Response updateConfiguration(
            @PathParam("bindingId") @ApiParam(value = "service ID", required = true) String bindingId,
            Map<String, Object> configuration) {
        try {
            String configId = getConfigId(bindingId);
            if (configId == null) {
                logger.warn("Cannot get config id for binding id '{}', probably because binding does not exist.",
                        bindingId);
                return Response.status(404).build();
            }
            Configuration oldConfiguration = configurationService.get(configId);
            configurationService.update(configId, new Configuration(normalizeConfiguration(configuration, bindingId)));
            return oldConfiguration != null ? Response.ok(oldConfiguration.getProperties()).build()
                    : Response.noContent().build();
        } catch (IOException ex) {
            logger.error("Cannot update configuration for service {}: {}", bindingId, ex.getMessage(), ex);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Map<String, Object> normalizeConfiguration(Map<String, Object> properties, String bindingId) {
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

    private String getConfigId(String bindingId) {
        BindingInfo bindingInfo = this.bindingInfoRegistry.getBindingInfo(bindingId);
        if (bindingInfo != null) {
            return bindingInfo.getServiceId();
        } else {
            return null;
        }
    }

    private BindingInfoDTO map(BindingInfo bindingInfo, Locale locale) {
        URI configDescriptionURI = bindingInfo.getConfigDescriptionURI();
        return new BindingInfoDTO(bindingInfo.getUID(), bindingInfo.getName(), bindingInfo.getAuthor(),
                bindingInfo.getDescription(), configDescriptionURI != null ? configDescriptionURI.toString() : null);
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    protected void unsetConfigurationService(ConfigurationService configurationService) {
        this.configurationService = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setConfigDescriptionRegistry(ConfigDescriptionRegistry configDescriptionRegistry) {
        this.configDescRegistry = configDescriptionRegistry;
    }

    protected void unsetConfigDescriptionRegistry(ConfigDescriptionRegistry configDescriptionRegistry) {
        this.configDescRegistry = null;
    }

    @Override
    public boolean isSatisfied() {
        return configurationService != null && configDescRegistry != null && bindingInfoRegistry != null
                && localeService != null;
    }

}
