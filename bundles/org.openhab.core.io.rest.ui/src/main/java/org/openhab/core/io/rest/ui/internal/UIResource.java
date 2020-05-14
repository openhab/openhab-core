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
package org.openhab.core.io.rest.ui.internal;

import java.security.InvalidParameterException;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.openhab.core.auth.Role;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.io.rest.Stream2JSONInputStream;
import org.openhab.core.io.rest.ui.TileDTO;
import org.openhab.core.ui.components.RootUIComponent;
import org.openhab.core.ui.components.UIComponentRegistry;
import org.openhab.core.ui.components.UIComponentRegistryFactory;
import org.openhab.core.ui.tiles.Tile;
import org.openhab.core.ui.tiles.TileProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JSONRequired;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * This class acts as a REST resource for the UI resources and is registered with the
 * Jersey servlet.
 *
 * @author Yannick Schaus - Initial contribution
 */
@Component(service = { RESTResource.class, UIResource.class })
@JaxrsResource
@JaxrsName(UIResource.PATH_UI)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(UIResource.PATH_UI)
@Api(UIResource.PATH_UI)
public class UIResource implements RESTResource {

    /** The URI path to this resource */
    public static final String PATH_UI = "ui";

    @Context
    private UriInfo uriInfo;

    private TileProvider tileProvider;
    private UIComponentRegistryFactory componentRegistryFactory;

    @GET
    @Path("/tiles")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Get all registered UI tiles.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = Tile.class) })
    public Response getAll() {
        Stream<TileDTO> tiles = tileProvider.getTiles().map(this::toTileDTO);
        return Response.ok(new Stream2JSONInputStream(tiles)).build();
    }

    @GET
    @Path("/components/{namespace}")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Get all registered UI components in the specified namespace.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = Tile.class) })
    public Response getAllComponents(@PathParam("namespace") String namespace) {
        UIComponentRegistry registry = componentRegistryFactory.getRegistry(namespace);
        Stream<RootUIComponent> components = registry.getAll().stream();
        return Response.ok(new Stream2JSONInputStream(components)).build();
    }

    @GET
    @Path("/components/{namespace}/{componentUID}")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Get a specific UI component in the specified namespace.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = Tile.class),
            @ApiResponse(code = 404, message = "Component not found", response = Tile.class) })
    public Response getComponentByUID(@PathParam("namespace") String namespace,
            @PathParam("componentUID") String componentUID) {
        UIComponentRegistry registry = componentRegistryFactory.getRegistry(namespace);
        RootUIComponent component = registry.get(componentUID);
        if (component == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        return Response.ok(component).build();
    }

    @POST
    @RolesAllowed({ Role.ADMIN })
    @Path("/components/{namespace}")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Add an UI component in the specified namespace.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = Tile.class) })
    public Response addComponent(@PathParam("namespace") String namespace, RootUIComponent component) {
        UIComponentRegistry registry = componentRegistryFactory.getRegistry(namespace);
        component.updateTimestamp();
        RootUIComponent createdComponent = registry.add(component);
        return Response.ok(createdComponent).build();
    }

    @PUT
    @RolesAllowed({ Role.ADMIN })
    @Path("/components/{namespace}/{componentUID}")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Update a specific UI component in the specified namespace.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = Tile.class),
            @ApiResponse(code = 404, message = "Component not found", response = Tile.class) })
    public Response updateComponent(@PathParam("namespace") String namespace,
            @PathParam("componentUID") String componentUID, RootUIComponent component) {
        UIComponentRegistry registry = componentRegistryFactory.getRegistry(namespace);
        RootUIComponent existingComponent = registry.get(componentUID);
        if (existingComponent == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        if (!componentUID.equals(component.getUID())) {
            throw new InvalidParameterException(
                    "The component UID in the body of the request should match the UID in the URL");
        }
        component.updateTimestamp();
        registry.update(component);
        return Response.ok(component).build();
    }

    @DELETE
    @RolesAllowed({ Role.ADMIN })
    @Path("/components/{namespace}/{componentUID}")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Remove a specific UI component in the specified namespace.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = Tile.class),
            @ApiResponse(code = 404, message = "Component not found", response = Tile.class) })
    public Response deleteComponent(@PathParam("namespace") String namespace,
            @PathParam("componentUID") String componentUID) {
        UIComponentRegistry registry = componentRegistryFactory.getRegistry(namespace);
        RootUIComponent component = registry.get(componentUID);
        if (component == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        registry.remove(componentUID);
        return Response.ok().build();
    }

    @Override
    public boolean isSatisfied() {
        return tileProvider != null && componentRegistryFactory != null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setTileProvider(TileProvider tileProvider) {
        this.tileProvider = tileProvider;
    }

    protected void unsetTileProvider(TileProvider tileProvider) {
        this.tileProvider = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setComponentRegistryFactory(UIComponentRegistryFactory componentRegistryFactory) {
        this.componentRegistryFactory = componentRegistryFactory;
    }

    protected void unsetComponentRegistryFactory(UIComponentRegistryFactory componentRegistryFactory) {
        this.componentRegistryFactory = null;
    }

    private TileDTO toTileDTO(Tile tile) {
        return new TileDTO(tile.getName(), tile.getUrl(), tile.getOverlay(), tile.getImageUrl());
    }
}
