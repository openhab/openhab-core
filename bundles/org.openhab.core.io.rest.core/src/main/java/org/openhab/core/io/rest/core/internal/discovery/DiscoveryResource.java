/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.core.io.rest.core.internal.discovery;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Role;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.config.discovery.DiscoveryServiceRegistry;
import org.openhab.core.config.discovery.ScanListener;
import org.openhab.core.i18n.I18nUtil;
import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.io.rest.JSONResponse;
import org.openhab.core.io.rest.LocaleService;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.io.rest.core.discovery.DiscoveryInfoDTO;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
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
 * This class acts as a REST resource for discovery and is registered with the
 * Jersey servlet.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Kai Kreuzer - refactored for using the OSGi JAX-RS connector
 * @author Yordan Zhelev - Added Swagger annotations
 * @author Ivaylo Ivanov - Added payload to the response of <code>scan</code>
 * @author Franck Dechavanne - Added DTOs to ApiResponses
 * @author Markus Rathgeb - Migrated to JAX-RS Whiteboard Specification
 * @author Wouter Born - Migrated to OpenAPI annotations
 * @author Laurent Garnier - Added discovery with an optional input parameter
 */
@Component(service = { RESTResource.class, DiscoveryResource.class })
@JaxrsResource
@JaxrsName(DiscoveryResource.PATH_DISCOVERY)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(DiscoveryResource.PATH_DISCOVERY)
@RolesAllowed({ Role.ADMIN })
@SecurityRequirement(name = "oauth2", scopes = { "admin" })
@Tag(name = DiscoveryResource.PATH_DISCOVERY)
@NonNullByDefault
public class DiscoveryResource implements RESTResource {

    /** The URI path to this resource */
    public static final String PATH_DISCOVERY = "discovery";

    private final Logger logger = LoggerFactory.getLogger(DiscoveryResource.class);

    private final DiscoveryServiceRegistry discoveryServiceRegistry;
    private final LocaleService localeService;
    private final TranslationProvider i18nProvider;

    @Activate
    public DiscoveryResource(final @Reference DiscoveryServiceRegistry discoveryServiceRegistry,
            final @Reference TranslationProvider translationProvider, final @Reference LocaleService localeService) {
        this.discoveryServiceRegistry = discoveryServiceRegistry;
        this.i18nProvider = translationProvider;
        this.localeService = localeService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getBindingsWithDiscoverySupport", summary = "Gets all bindings that support discovery.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class), uniqueItems = true))) })
    public Response getDiscoveryServices() {
        Collection<String> supportedBindings = discoveryServiceRegistry.getSupportedBindings();
        return Response.ok(new LinkedHashSet<>(supportedBindings)).build();
    }

    @GET
    @Path("/bindings/{bindingId}/info")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getDiscoveryServicesInfo", summary = "Gets information about the discovery services for a binding.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = DiscoveryInfoDTO.class))),
            @ApiResponse(responseCode = "404", description = "Discovery service not found") })
    public Response getDiscoveryServicesInfo(
            @PathParam("bindingId") @Parameter(description = "binding Id") final String bindingId,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language) {
        final Locale locale = localeService.getLocale(language);
        String label = null;
        String description = null;
        boolean supported = false;
        Set<DiscoveryService> discoveryServices = discoveryServiceRegistry.getDiscoveryServices(bindingId);

        if (discoveryServices.isEmpty()) {
            return JSONResponse.createResponse(Status.NOT_FOUND, null,
                    "No discovery service found for binding " + bindingId);
        }

        for (DiscoveryService discoveryService : discoveryServices) {
            if (discoveryService.isScanInputSupported()) {
                Bundle bundle = FrameworkUtil.getBundle(discoveryService.getClass());
                label = discoveryService.getScanInputLabel();
                if (label != null) {
                    label = i18nProvider.getText(bundle, I18nUtil.stripConstant(label), label, locale);
                }
                description = discoveryService.getScanInputDescription();
                if (description != null) {
                    description = i18nProvider.getText(bundle, I18nUtil.stripConstant(description), description,
                            locale);
                }
                supported = true;
                break;
            }
        }
        return Response.ok(new DiscoveryInfoDTO(supported, label, description)).build();
    }

    @POST
    @Path("/bindings/{bindingId}/scan")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(operationId = "scan", summary = "Starts asynchronous discovery process for a binding and returns the timeout in seconds of the discovery operation.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = Integer.class))),
            @ApiResponse(responseCode = "404", description = "Discovery service not found") })
    public Response scan(@PathParam("bindingId") @Parameter(description = "binding Id") final String bindingId,
            @QueryParam("input") @Parameter(description = "input parameter to start the discovery") @Nullable String input) {
        if (discoveryServiceRegistry.getDiscoveryServices(bindingId).isEmpty()) {
            return JSONResponse.createResponse(Status.NOT_FOUND, null,
                    "No discovery service found for binding " + bindingId);
        }

        discoveryServiceRegistry.startScan(bindingId, input, new ScanListener() {
            @Override
            public void onErrorOccurred(@Nullable Exception exception) {
                logger.error("Error occurred while scanning for binding '{}'", bindingId, exception);
            }

            @Override
            public void onFinished() {
                logger.debug("Scan for binding '{}' successfully finished.", bindingId);
            }
        });

        return Response.ok(discoveryServiceRegistry.getMaxScanTimeout(bindingId)).build();
    }
}
