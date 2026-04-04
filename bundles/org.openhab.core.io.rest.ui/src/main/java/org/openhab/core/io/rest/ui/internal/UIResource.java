/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Role;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.io.rest.Stream2JSONInputStream;
import org.openhab.core.io.rest.ui.TileDTO;
import org.openhab.core.ui.components.RootUIComponent;
import org.openhab.core.ui.components.UIComponentRegistry;
import org.openhab.core.ui.components.UIComponentRegistryFactory;
import org.openhab.core.ui.tiles.Tile;
import org.openhab.core.ui.tiles.TileProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jakartars.whiteboard.JakartarsWhiteboardConstants;
import org.osgi.service.jakartars.whiteboard.propertytypes.JSONRequired;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsApplicationSelect;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsName;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * This class acts as a REST resource for the UI resources and is registered with the
 * Jersey servlet.
 *
 * @author Yannick Schaus - Initial contribution
 * @author Wouter Born - Migrated to OpenAPI annotations
 */
@Component(service = { RESTResource.class, UIResource.class })
@JakartarsResource
@JakartarsName(UIResource.PATH_UI)
@JakartarsApplicationSelect("(" + JakartarsWhiteboardConstants.JAKARTA_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(UIResource.PATH_UI)
@Tag(name = UIResource.PATH_UI)
@NonNullByDefault
public class UIResource implements RESTResource {

    /** The URI path to this resource */
    public static final String PATH_UI = "ui";

    private final UIComponentRegistryFactory componentRegistryFactory;
    private final TileProvider tileProvider;

    private Map<String, Date> lastModifiedDates = new HashMap<>();
    private Map<String, RegistryChangeListener<RootUIComponent>> registryChangeListeners = new HashMap<>();

    @Activate
    public UIResource( //
            final @Reference UIComponentRegistryFactory componentRegistryFactory,
            final @Reference TileProvider tileProvider) {
        this.componentRegistryFactory = componentRegistryFactory;
        this.tileProvider = tileProvider;
    }

    @Deactivate
    public void deactivate() {
        registryChangeListeners.forEach((n, l) -> {
            UIComponentRegistry registry = componentRegistryFactory.getRegistry(n);
            registry.removeRegistryChangeListener(l);
        });
    }

    @GET
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Path("/tiles")
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(operationId = "getUITiles", summary = "Get all registered UI tiles.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TileDTO.class)))) })
    public Response getAll() {
        Stream<TileDTO> tiles = tileProvider.getTiles().map(this::toTileDTO);
        return Response.ok(new Stream2JSONInputStream(tiles)).build();
    }

    @GET
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Path("/components/{namespace}")
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(operationId = "getRegisteredUIComponentsInNamespace", summary = "Get all registered UI components in the specified namespace.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = RootUIComponent.class)))) })
    public Response getAllComponents(@Context Request request, @PathParam("namespace") String namespace,
            @QueryParam("summary") @Parameter(description = "summary fields only") @Nullable Boolean summary) {
        UIComponentRegistry registry = componentRegistryFactory.getRegistry(namespace);
        Stream<RootUIComponent> components = registry.getAll().stream();
        if (summary != null && summary) {
            components = components.map(c -> {
                RootUIComponent component = new RootUIComponent(c.getUID(), c.getType());
                @Nullable
                Set<String> tags = c.getTags();
                if (tags != null) {
                    component.addTags(c.getTags());
                }
                @Nullable
                Date timestamp = c.getTimestamp();
                if (timestamp != null) {
                    component.setTimestamp(timestamp);
                }
                return component;
            });
            return Response.ok(new Stream2JSONInputStream(components)).build();
        } else {
            if (!registryChangeListeners.containsKey(namespace)) {
                RegistryChangeListener<RootUIComponent> changeListener = new ResetLastModifiedChangeListener(namespace);
                registryChangeListeners.put(namespace, changeListener);
                registry.addRegistryChangeListener(changeListener);
            }

            Date lastModifiedDate = Date.from(Instant.now());
            if (lastModifiedDates.containsKey(namespace)) {
                lastModifiedDate = lastModifiedDates.get(namespace);
                Response.ResponseBuilder responseBuilder = request.evaluatePreconditions(lastModifiedDate);
                if (responseBuilder != null) {
                    // send 304 Not Modified
                    return responseBuilder.build();
                }
            } else {
                lastModifiedDate = Date.from(Instant.now().truncatedTo(ChronoUnit.SECONDS));
                lastModifiedDates.put(namespace, lastModifiedDate);
            }

            return Response.ok(new Stream2JSONInputStream(components)).lastModified(lastModifiedDate)
                    .cacheControl(RESTConstants.CACHE_CONTROL).build();
        }
    }

    @GET
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Path("/components/{namespace}/{componentUID}")
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(operationId = "getUIComponentInNamespace", summary = "Get a specific UI component in the specified namespace.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = RootUIComponent.class))),
            @ApiResponse(responseCode = "404", description = "Component not found") })
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(operationId = "addUIComponentToNamespace", summary = "Add a UI component in the specified namespace.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = RootUIComponent.class))) })
    public Response addComponent(@PathParam("namespace") String namespace, RootUIComponent component) {
        UIComponentRegistry registry = componentRegistryFactory.getRegistry(namespace);
        component.updateTimestamp();
        RootUIComponent createdComponent = registry.add(component);
        return Response.ok(createdComponent).build();
    }

    @PUT
    @RolesAllowed({ Role.ADMIN })
    @Path("/components/{namespace}/{componentUID}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(operationId = "updateUIComponentInNamespace", summary = "Update a specific UI component in the specified namespace.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = RootUIComponent.class))),
                    @ApiResponse(responseCode = "404", description = "Component not found") })
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
    @Operation(operationId = "removeUIComponentFromNamespace", summary = "Remove a specific UI component in the specified namespace.", security = {
            @SecurityRequirement(name = "oauth2", scopes = { "admin" }) }, responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Component not found") })
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

    private TileDTO toTileDTO(Tile tile) {
        return new TileDTO(tile.getName(), tile.getUrl(), tile.getOverlay(), tile.getImageUrl());
    }

    private void resetLastModifiedDate(String namespace) {
        lastModifiedDates.remove(namespace);
    }

    private class ResetLastModifiedChangeListener implements RegistryChangeListener<RootUIComponent> {

        private String namespace;

        ResetLastModifiedChangeListener(String namespace) {
            this.namespace = namespace;
        }

        @Override
        public void added(RootUIComponent element) {
            resetLastModifiedDate(namespace);
        }

        @Override
        public void removed(RootUIComponent element) {
            resetLastModifiedDate(namespace);
        }

        @Override
        public void updated(RootUIComponent oldElement, RootUIComponent element) {
            resetLastModifiedDate(namespace);
        }
    }
}
