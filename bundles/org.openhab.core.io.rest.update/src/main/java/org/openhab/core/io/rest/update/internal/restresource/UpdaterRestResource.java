/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.io.rest.update.internal.restresource;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Role;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.io.rest.update.internal.dto.UpdaterExecuteDTO;
import org.openhab.core.io.rest.update.internal.dto.UpdaterStatusDTO;
import org.openhab.core.io.rest.update.internal.factory.UpdaterFactory;
import org.openhab.core.io.rest.update.internal.updaters.BaseUpdater;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JSONRequired;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * This is a REST resource for openHAB self updating features.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@Component
@JaxrsResource
@JaxrsName(UpdaterRestResource.URI_BASE)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(UpdaterRestResource.URI_BASE)
@Tag(name = UpdaterRestResource.URI_BASE)
@NonNullByDefault
public class UpdaterRestResource implements RESTResource {

    // uri's
    public static final String URI_BASE = "update";
    public static final String URI_STATUS = "/status";
    public static final String URI_EXECUTE = "/execute";

    // response messages
    private static final String OK_SUCCESS = "Request succeeded!";
    private static final String OK_UPDATE_STARTED = "Started openHAB self update process";
    private static final String BAD_REQUEST = "Bad request!";
    private static final String BAD_PASSWORD = "Invalid 'password' parameter (must be without white space and not longer than 20 characters).";
    private static final String BAD_USER = "Invalid 'user' parameter (must be aplha-numeric characters only and not longer than 20 characters).";
    private static final String BAD_VERSION = "Invalid 'targetNewVersionType' parameter (must be STABLE / MILSETONE/ SNAPSHOT).";
    private static final String INT_SERV_ERR_UPDATER_NULL = "Updater class not initialized.";

    private @Nullable BaseUpdater updater;

    @Activate
    public UpdaterRestResource() {
        this.updater = UpdaterFactory.newUpdater();
    }

    /**
     * This method handles HTTP GET on the updater resource's 'status' path. It serves a JSON DTO containing the updater
     * status. This is a read only function so both USER and ADMIN roles are allowed.
     *
     * @return HTTP 200 OK, or HTTP 500 INTERNAL SERVER ERROR if the updater is null.
     */
    @GET
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Path(UpdaterRestResource.URI_STATUS)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "status", summary = "Get the updater status.", responses = {
            @ApiResponse(responseCode = "200", description = OK_SUCCESS, content = @Content(schema = @Schema(implementation = UpdaterStatusDTO.class))),
            @ApiResponse(responseCode = "500", description = INT_SERV_ERR_UPDATER_NULL) })
    public Response status() {
        BaseUpdater updater = this.updater;

        // return server error if updater is null
        if (updater == null) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(INT_SERV_ERR_UPDATER_NULL).build();
        }

        // populate and return the DTO
        return Response.ok(updater.getStatusDTO()).build();
    }

    /**
     * This method handles HTTP POST on the updater resource's 'execute' path. It accepts a JSON DTO, and if the
     * parameters are OK starts the updater's update process. It serves a PLAIN TEXT response indicating success or
     * failure. This is an administrator function so only ADMIN role is allowed.
     *
     * @return HTTP 200 OK, HTTP 400 BAD REQUEST if the JSON DTO parameters have errors, or HTTP 500 INTERNAL SERVER
     *         ERROR if the updater is null.
     */
    @POST
    @RolesAllowed({ Role.ADMIN })
    @Path(UpdaterRestResource.URI_EXECUTE)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(operationId = "execute", summary = "Start updating openHAB to a newer version.", responses = {
            @ApiResponse(responseCode = "200", description = OK_SUCCESS),
            @ApiResponse(responseCode = "400", description = BAD_REQUEST),
            @ApiResponse(responseCode = "500", description = INT_SERV_ERR_UPDATER_NULL) })
    public Response execute(
            @Parameter(description = "Update command settings", required = true) @Valid UpdaterExecuteDTO dto) {
        BaseUpdater updater = this.updater;

        // return server error if updater is null
        if (updater == null) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(INT_SERV_ERR_UPDATER_NULL).build();
        }

        // check targetNewVersionType
        if (dto.targetNewVersionType != null) {
            try {
                updater.setNewVersionType(dto.targetNewVersionType);
            } catch (IllegalArgumentException e) {
                return Response.status(Status.BAD_REQUEST).entity(BAD_VERSION).build();
            }
        } else {
            return Response.status(Status.BAD_REQUEST).entity(BAD_VERSION).build();
        }

        // check user name
        if (dto.user != null) {
            try {
                updater.setUserName(dto.user);
            } catch (IllegalArgumentException e) {
                return Response.status(Status.BAD_REQUEST).entity(BAD_USER).build();
            }
        }

        // check password
        if (dto.password != null) {
            try {
                updater.setPassword(dto.password);
            } catch (IllegalArgumentException e) {
                return Response.status(Status.BAD_REQUEST).entity(BAD_PASSWORD).build();
            }
        }

        // finally schedule the updater task for future execution
        updater.submit();

        return Response.ok(OK_UPDATE_STARTED).build();
    }
}
