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
package org.openhab.core.io.rest.core.internal.discovery;

import java.util.Collection;
import java.util.LinkedHashSet;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.openhab.core.auth.Role;
import org.openhab.core.config.discovery.DiscoveryServiceRegistry;
import org.openhab.core.config.discovery.ScanListener;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

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
 */
@Component(service = { RESTResource.class, DiscoveryResource.class })
@JaxrsResource
@JaxrsName(DiscoveryResource.PATH_DISCOVERY)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(DiscoveryResource.PATH_DISCOVERY)
@RolesAllowed({ Role.ADMIN })
@Api(DiscoveryResource.PATH_DISCOVERY)
public class DiscoveryResource implements RESTResource {

    /** The URI path to this resource */
    public static final String PATH_DISCOVERY = "discovery";

    private final Logger logger = LoggerFactory.getLogger(DiscoveryResource.class);

    private DiscoveryServiceRegistry discoveryServiceRegistry;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setDiscoveryServiceRegistry(DiscoveryServiceRegistry discoveryServiceRegistry) {
        this.discoveryServiceRegistry = discoveryServiceRegistry;
    }

    protected void unsetDiscoveryServiceRegistry(DiscoveryServiceRegistry discoveryServiceRegistry) {
        this.discoveryServiceRegistry = null;
    }

    @Context
    private UriInfo uriInfo;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets all bindings that support discovery.", response = String.class, responseContainer = "Set")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = String.class, responseContainer = "Set") })
    public Response getDiscoveryServices() {
        Collection<String> supportedBindings = discoveryServiceRegistry.getSupportedBindings();
        return Response.ok(new LinkedHashSet<>(supportedBindings)).build();
    }

    @POST
    @Path("/bindings/{bindingId}/scan")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Starts asynchronous discovery process for a binding and returns the timeout in seconds of the discovery operation.", response = Integer.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = Integer.class) })
    public Response scan(@PathParam("bindingId") @ApiParam(value = "bindingId") final String bindingId) {
        discoveryServiceRegistry.startScan(bindingId, new ScanListener() {
            @Override
            public void onErrorOccurred(Exception exception) {
                logger.error("Error occurred while scanning for binding '{}': {}", bindingId, exception.getMessage(),
                        exception);
            }

            @Override
            public void onFinished() {
                logger.debug("Scan for binding '{}' successfully finished.", bindingId);
            }
        });

        return Response.ok(discoveryServiceRegistry.getMaxScanTimeout(bindingId)).build();
    }

    @Override
    public boolean isSatisfied() {
        return discoveryServiceRegistry != null;
    }
}
