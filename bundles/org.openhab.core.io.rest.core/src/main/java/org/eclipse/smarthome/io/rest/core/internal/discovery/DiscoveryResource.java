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
package org.eclipse.smarthome.io.rest.core.internal.discovery;

import java.util.Collection;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.DiscoveryServiceRegistry;
import org.eclipse.smarthome.config.discovery.ScanListener;
import org.eclipse.smarthome.core.auth.Role;
import org.eclipse.smarthome.io.rest.RESTService;
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
 * This class acts as a REST resource for discovery and is registered with the
 * Jersey servlet.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Kai Kreuzer - refactored for using the OSGi JAX-RS connector
 * @author Yordan Zhelev - Added Swagger annotations
 * @author Ivaylo Ivanov - Added payload to the response of <code>scan</code>
 * @author Franck Dechavanne - Added DTOs to ApiResponses
 */
@Path(DiscoveryResource.PATH_DISCOVERY)
@RolesAllowed({ Role.ADMIN })
@Api(value = DiscoveryResource.PATH_DISCOVERY)
@Produces(MediaType.APPLICATION_JSON)
@Component(immediate = true)
@JaxrsApplicationSelect("(osgi.jaxrs.name=" + RESTService.REST_APP_NAME + ")")
@JaxrsResource
@NonNullByDefault
public class DiscoveryResource {

    /** The URI path to this resource */
    public static final String PATH_DISCOVERY = "discovery";

    private final Logger logger = LoggerFactory.getLogger(DiscoveryResource.class);

    @Reference
    private @NonNullByDefault({}) DiscoveryServiceRegistry discoveryServiceRegistry;

    @GET
    @ApiOperation(value = "Gets all bindings that support discovery.", response = String.class, responseContainer = "Set")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = String.class, responseContainer = "Set") })
    public Collection<?> getDiscoveryServices() {
        return discoveryServiceRegistry.getSupportedBindings();
    }

    @POST
    @Path("/bindings/{bindingId}/scan")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Starts asynchronous discovery process for a binding and returns the timeout in seconds of the discovery operation.", response = Integer.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = Integer.class) })
    public Integer scan(@PathParam("bindingId") @ApiParam(value = "bindingId") final String bindingId) {
        discoveryServiceRegistry.startScan(bindingId, new ScanListener() {
            @Override
            public void onErrorOccurred(@Nullable Exception exception) {
                logger.error("Error occurred while scanning for binding '{}': {}", bindingId,
                        exception != null ? exception.getMessage() : "", exception);
            }

            @Override
            public void onFinished() {
                logger.debug("Scan for binding '{}' successfully finished.", bindingId);
            }
        });

        return discoveryServiceRegistry.getMaxScanTimeout(bindingId);
    }
}
