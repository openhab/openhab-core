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
package org.openhab.core.id.internal;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.openhab.core.auth.Role;
import org.openhab.core.id.InstanceUUID;
import org.openhab.core.io.rest.RESTResource;
import org.osgi.service.component.annotations.Component;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * This class acts as a REST resource for accessing the UUID of the instance
 *
 * @author Kai Kreuzer - Initial contribution
 */
@Component
@Path(UUIDResource.PATH_UUID)
@Api(value = UUIDResource.PATH_UUID)
@RolesAllowed({ Role.ADMIN })
public class UUIDResource implements RESTResource {

    public static final String PATH_UUID = "uuid";

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "A unified unique id.", response = String.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class) })
    public Response getInstanceUUID() {
        return Response.ok(InstanceUUID.get()).build();
    }

}
