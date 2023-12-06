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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Objects;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Role;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.io.rest.internal.resources.beans.SystemInfoBean;
import org.openhab.core.io.rest.internal.resources.beans.UoMInfoBean;
import org.openhab.core.service.StartLevelService;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JSONRequired;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * This class acts as a REST resource for system information.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@Component
@JaxrsResource
@JaxrsName(SystemInfoResource.PATH_SYSTEMINFO)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(SystemInfoResource.PATH_SYSTEMINFO)
@RolesAllowed({ Role.ADMIN })
@SecurityRequirement(name = "oauth2", scopes = { "admin" })
@Tag(name = SystemInfoResource.PATH_SYSTEMINFO)
@NonNullByDefault
public class SystemInfoResource implements RESTResource, ConfigurationListener {

    /** The URI path to this resource */
    public static final String PATH_SYSTEMINFO = "systeminfo";

    private final StartLevelService startLevelService;
    private final UnitProvider unitProvider;
    private @Nullable Date lastModified = null;

    @Activate
    public SystemInfoResource(@Reference StartLevelService startLevelService, @Reference UnitProvider unitProvider) {
        this.startLevelService = startLevelService;
        this.unitProvider = unitProvider;
    }

    @Override
    public void configurationEvent(@Nullable ConfigurationEvent event) {
        if (Objects.equals(event.getPid(), "org.openhab.i18n")) {
            lastModified = null;
        }
    }

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getSystemInformation", summary = "Gets information about the system.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SystemInfoBean.class))) })
    public Response getSystemInfo(@Context UriInfo uriInfo) {
        final SystemInfoBean bean = new SystemInfoBean(startLevelService.getStartLevel());
        return Response.ok(bean).build();
    }

    @GET
    @Path("/uom")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getUoMInformation", summary = "Get all supported dimensions and their system units.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = UoMInfoBean.class))) })
    public Response getUoMInfo(final @Context Request request, final @Context UriInfo uriInfo) {
        if (lastModified != null) {
            Response.ResponseBuilder responseBuilder = request.evaluatePreconditions(lastModified);
            if (responseBuilder != null) {
                // send 304 Not Modified
                return responseBuilder.build();
            }
        } else {
            lastModified = Date.from(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        }

        CacheControl cc = new CacheControl();
        cc.setMustRevalidate(true);
        cc.setPrivate(true);

        final UoMInfoBean bean = new UoMInfoBean(unitProvider);
        return Response.ok(bean).lastModified(lastModified).cacheControl(cc).build();
    }
}
