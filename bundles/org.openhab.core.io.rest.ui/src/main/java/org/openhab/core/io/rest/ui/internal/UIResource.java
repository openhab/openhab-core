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

import java.util.stream.Stream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.io.rest.Stream2JSONInputStream;
import org.openhab.core.io.rest.ui.TileDTO;
import org.openhab.core.ui.tiles.Tile;
import org.openhab.core.ui.tiles.TileProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
@Path(UIResource.PATH_UI)
@Api(value = UIResource.PATH_UI)
@Component(service = { RESTResource.class, UIResource.class })
public class UIResource implements RESTResource {
    private final Logger logger = LoggerFactory.getLogger(UIResource.class);

    /** The URI path to this resource */
    public static final String PATH_UI = "ui";

    @Context
    private UriInfo uriInfo;

    private TileProvider tileProvider;

    @GET
    @Path("/tiles")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Get all registered UI tiles.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = Tile.class) })
    public Response getAll() {
        Stream<TileDTO> tiles = tileProvider.getTiles().map(this::toTileDTO);
        return Response.ok(new Stream2JSONInputStream(tiles)).build();
    }

    @Override
    public boolean isSatisfied() {
        return tileProvider != null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setTileProvider(TileProvider tileProvider) {
        this.tileProvider = tileProvider;
    }

    protected void unsetTileProvider(TileProvider tileProvider) {
        this.tileProvider = null;
    }

    private TileDTO toTileDTO(Tile tile) {
        return new TileDTO(tile.getName(), tile.getUrl(), tile.getOverlay(), tile.getImageUrl());
    }
}
