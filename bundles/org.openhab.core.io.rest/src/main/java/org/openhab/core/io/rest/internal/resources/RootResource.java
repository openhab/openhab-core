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
package org.openhab.core.io.rest.internal.resources;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.io.rest.internal.resources.beans.RootBean;
import org.openhab.core.io.rest.internal.resources.beans.RootBean.Links;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;
import org.osgi.service.jaxrs.runtime.dto.ApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.ResourceDTO;
import org.osgi.service.jaxrs.runtime.dto.RuntimeDTO;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JSONRequired;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * <p>
 * This class acts as an entry point / root resource for the REST API.
 *
 * <p>
 * In good HATEOAS manner, it provides links to other offered resources.
 *
 * <p>
 * The result is returned as JSON
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Markus Rathgeb - Migrated to JAX-RS Whiteboard Specification
 * @author Wouter Born - Migrated to OpenAPI annotations
 */
@Component(configurationPid = "org.openhab.restroot")
@JaxrsResource
@JaxrsName(RootResource.RESOURCE_NAME)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Produces(MediaType.APPLICATION_JSON)
@Path("/")
@Tag(name = RootResource.RESOURCE_NAME)
@NonNullByDefault
public class RootResource implements RESTResource {

    public static final String RESOURCE_NAME = "root";

    private final Logger logger = LoggerFactory.getLogger(RootResource.class);
    private final JaxrsServiceRuntime runtime;
    private final LocaleProvider localeProvider;
    private final UnitProvider unitProvider;

    @Activate
    public RootResource(final @Reference JaxrsServiceRuntime runtime, final @Reference LocaleProvider localeProvider,
            final @Reference UnitProvider unitProvider) {
        this.runtime = runtime;
        this.localeProvider = localeProvider;
        this.unitProvider = unitProvider;
    }

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getRoot", summary = "Gets information about the runtime, the API version and links to resources.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = RootBean.class))) })
    public Response getRoot(@Context UriInfo uriInfo) {
        // key: path, value: name (this way we could ensure that ever path is added only once).
        final Map<String, String> collectedLinks = new HashMap<>();

        final RuntimeDTO runtimeDTO = runtime.getRuntimeDTO();
        final RootBean bean = new RootBean(localeProvider, unitProvider);
        for (final ApplicationDTO applicationDTO : runtimeDTO.applicationDTOs) {
            for (final ResourceDTO resourceDTO : applicationDTO.resourceDTOs) {
                // We are using the JAX-RS name per convention for the link type.
                // Let's skip names that begin with a dot (e.g. the generated ones) and empty ones.
                final String name = resourceDTO.name;
                if (name == null || name.isEmpty() || name.startsWith(".") || RESOURCE_NAME.equals(name)) {
                    continue;
                }

                // The path is provided for every resource method by the respective info DTO.
                // We don't want to add every REST endpoint but just the "parent" one.
                // Per convention the name is similar to the path (without the leading "/") for openHAB REST
                // implementations.

                final URI uri = uriInfo.getBaseUriBuilder().path("/" + name).build();
                if (collectedLinks.put(uri.toASCIIString(), name) != null) {
                    logger.warn("Duplicate entry: {}", name);
                }
            }
        }
        collectedLinks.forEach((path, name) -> {
            bean.links.add(new Links(name, path));
        });
        return Response.ok(bean).build();
    }
}
