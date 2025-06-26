/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.io.rest.media.internal;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Role;
import org.openhab.core.io.rest.LocaleService;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.media.MediaDevice;
import org.openhab.core.media.MediaListenner;
import org.openhab.core.media.MediaService;
import org.openhab.core.media.internal.MediaServlet;
import org.openhab.core.media.model.MediaCollection;
import org.openhab.core.media.model.MediaEntry;
import org.openhab.core.media.model.MediaRegistry;
import org.openhab.core.media.model.MediaSource;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JSONRequired;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * This class acts as a REST resource for audio features.
 *
 * @author Laurent Arnal - Initial contribution
 */
@Component
@JaxrsResource
@JaxrsName(MediaResource.PATH_MEDIA)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(MediaResource.PATH_MEDIA)
@RolesAllowed({ Role.USER, Role.ADMIN })
@Tag(name = MediaResource.PATH_MEDIA)
@NonNullByDefault
public class MediaResource implements RESTResource {

    /** The URI path to this resource */
    public static final String PATH_MEDIA = "media";

    private final MediaService mediaService;
    private final LocaleService localeService;

    @Activate
    public MediaResource( //
            final @Reference MediaService mediaService, final @Reference LocaleService localeService) {
        this.mediaService = mediaService;
        this.localeService = localeService;
    }

    @GET
    @Path("/sources")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getMediaSources", summary = "Get the list of all sources.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = MediaDTO.class)))) })
    public Response getSources(@Context UriInfo uriInfo,
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @QueryParam("path") @Parameter(description = "path of the ressource") @Nullable String path,
            @QueryParam("start") @Parameter(description = "start index to get") long start,
            @QueryParam("size") @Parameter(description = "number of element to get") long size) {
        final Locale locale = localeService.getLocale(language);

        MediaRegistry registry = mediaService.getMediaRegistry();

        URI uri = uriInfo.getRequestUri();
        String scheme = uri.getScheme();
        String host = uri.getHost();
        int port = uri.getPort();

        if (port == -1) {
            if (scheme.equals("http")) {
                port = 80;
            } else if (scheme.equals("https")) {
                port = 443;
            }
        }

        // Handle specific dev case
        if (port == 8081) {
            port = 8080;
        }

        String baseUri = scheme + "://" + host + ":" + port + MediaServlet.SERVLET_PATH;
        mediaService.setBaseUri(baseUri);

        if (path == null || path.isEmpty()) {
            path = "/Root";
        }
        MediaEntry entry = registry.getEntry(path);

        MediaSource mediaSource = entry.getMediaSource(false);
        if (mediaSource != null) {
            MediaListenner mediaListenner = mediaService.getMediaListenner(mediaSource.getKey());
            if (mediaListenner != null) {
                mediaListenner.refreshEntry(entry, start, size);
            }
        }

        if (entry instanceof MediaCollection) {
            MediaCollection col = (MediaCollection) entry;

            MediaDTOCollection dtoCollection = new MediaDTOCollection(entry.getKey(), entry.getPath(),
                    entry.getClass().getTypeName(), entry.getName());
            String artUriCol = col.getExternalArtUri();
            dtoCollection.setArtUri(artUriCol);

            // for (String key : col.getChilds().keySet()) {

            List<MediaEntry> colEntries = col.getChildsAsArray();
            for (long idx = start; idx < start + size && idx < colEntries.size(); idx++) {
                MediaEntry subEntry = colEntries.get((int) idx);

                MediaDTO dto = new MediaDTO(subEntry.getKey(), subEntry.getPath(), subEntry.getClass().getTypeName(),
                        subEntry.getName());
                if (subEntry instanceof MediaCollection) {
                    String artUri = ((MediaCollection) subEntry).getExternalArtUri();
                    dto.setArtUri(artUri);

                }
                dtoCollection.addMediaDTO(dto);
            }
            return Response.ok(dtoCollection).build();
        } else {
            MediaDTO dto = new MediaDTO(entry.getKey(), entry.getPath(), entry.getClass().getTypeName(),
                    entry.getName());
            return Response.ok(dto).build();
        }

    }

    @GET
    @Path("/sinks")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getMediaSinks", summary = "Get the list of all sinks.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = MediaSinkDTO.class)))) })
    public Response getSinks(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @QueryParam("path") @Parameter(description = "path of the ressource") @Nullable String path) {
        final Locale locale = localeService.getLocale(language);

        Map<String, MediaDevice> devices = mediaService.getMediaDevices();

        MediaSinkDTOCollection dtoCol = new MediaSinkDTOCollection();
        for (MediaDevice device : devices.values()) {
            MediaSinkDTO dto = new MediaSinkDTO(device.getId(), device.getName(), device.getType(),
                    device.getBinding());
            dtoCol.addMediaSinkDTO(dto);
        }

        return Response.ok(dtoCol).build();
    }
}
