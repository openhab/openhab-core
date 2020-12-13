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
import java.util.List;
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
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Role;
import org.openhab.core.binding.BindingInfo;
import org.openhab.core.binding.BindingInfoRegistry;
import org.openhab.core.binding.dto.BindingInfoDTO;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.ConfigUtil;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.io.rest.LocaleService;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.io.rest.Stream2JSONInputStream;
import org.openhab.core.io.rest.core.config.ConfigurationService;
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
 * This class acts as a REST resource for bindings and is registered with the
 * Jersey servlet.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Kai Kreuzer - refactored for using the OSGi JAX-RS connector
 * @author Yordan Zhelev - Added Swagger annotations
 * @author Franck Dechavanne - Added DTOs to ApiResponses
 * @author Markus Rathgeb - Migrated to JAX-RS Whiteboard Specification
 * @author Wouter Born - Migrated to OpenAPI annotations
 */
@Component
@JaxrsResource
@JaxrsName(BindingResource.PATH_BINDINGS)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(BindingResource.PATH_BINDINGS)
@RolesAllowed({ Role.ADMIN })
@SecurityRequirement(name = "oauth2", scopes = { "admin" })
@Tag(name = BindingResource.PATH_BINDINGS)
@NonNullByDefault
public class BindingResource implements RESTResource {

    /** The URI path to this resource */
    public static final String PATH_BINDINGS = "bindings";

    private final Logger logger = LoggerFactory.getLogger(BindingResource.class);

    private final BindingInfoRegistry bindingInfoRegistry;
    private final ConfigurationService configurationService;
    private final ConfigDescriptionRegistry configDescRegistry;
    private final LocaleService localeService;

    @Activate
    public BindingResource( //
            final @Reference BindingInfoRegistry bindingInfoRegistry,
            final @Reference ConfigurationService configurationService,
            final @Reference ConfigDescriptionRegistry configDescRegistry,
            final @Reference LocaleService localeService) {
        this.bindingInfoRegistry = bindingInfoRegistry;
        this.configurationService = configurationService;
        this.configDescRegistry = configDescRegistry;
        this.localeService = localeService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getBindings", summary = "Get all bindings.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = BindingInfoDTO.class), uniqueItems = true))) })
    public Response getAll(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language) {
        final Locale locale = localeService.getLocale(language);
        Set<BindingInfo> bindingInfos = bindingInfoRegistry.getBindingInfos(locale);

        return Response.ok(new Stream2JSONInputStream(bindingInfos.stream().map(b -> map(b, locale)))).build();
    }

    @GET
    @Path("/{bindingId}/config")
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(operationId = "getBindingConfiguration", summary = "Get binding configuration for given binding ID.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "Binding does not exist"),
            @ApiResponse(responseCode = "500", description = "Configuration can not be read due to internal error") })
    public Response getConfiguration(@PathParam("bindingId") @Parameter(description = "service ID") String bindingId) {
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
    @Operation(operationId = "updateBindingConfiguration", summary = "Updates a binding configuration for given binding ID and returns the old configuration.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "204", description = "No old configuration"),
            @ApiResponse(responseCode = "404", description = "Binding does not exist"),
            @ApiResponse(responseCode = "500", description = "Configuration can not be updated due to internal error") })
    public Response updateConfiguration(@PathParam("bindingId") @Parameter(description = "service ID") String bindingId,
            @Nullable Map<String, Object> configuration) {
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

    private @Nullable Map<String, Object> normalizeConfiguration(@Nullable Map<String, Object> properties,
            String bindingId) {
        if (properties == null || properties.isEmpty()) {
            return properties;
        }

        BindingInfo bindingInfo = this.bindingInfoRegistry.getBindingInfo(bindingId);
        if (bindingInfo == null || bindingInfo.getConfigDescriptionURI() == null) {
            return properties;
        }

        URI descURI = bindingInfo.getConfigDescriptionURI();
        if (descURI != null) {
            ConfigDescription configDesc = configDescRegistry.getConfigDescription(descURI);
            if (configDesc != null) {
                return ConfigUtil.normalizeTypes(properties, List.of(configDesc));
            }
        }

        return properties;
    }

    private @Nullable String getConfigId(String bindingId) {
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
}
