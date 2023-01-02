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
package org.openhab.core.io.rest.audio.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioManager;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.auth.Role;
import org.openhab.core.io.rest.JSONResponse;
import org.openhab.core.io.rest.LocaleService;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
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
 * @author Laurent Garnier - Initial contribution
 * @author Markus Rathgeb - Migrated to JAX-RS Whiteboard Specification
 * @author Wouter Born - Migrated to OpenAPI annotations
 */
@Component
@JaxrsResource
@JaxrsName(AudioResource.PATH_AUDIO)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(AudioResource.PATH_AUDIO)
@RolesAllowed({ Role.USER, Role.ADMIN })
@Tag(name = AudioResource.PATH_AUDIO)
@NonNullByDefault
public class AudioResource implements RESTResource {

    /** The URI path to this resource */
    public static final String PATH_AUDIO = "audio";

    private final AudioManager audioManager;
    private final LocaleService localeService;

    @Activate
    public AudioResource( //
            final @Reference AudioManager audioManager, //
            final @Reference LocaleService localeService) {
        this.audioManager = audioManager;
        this.localeService = localeService;
    }

    @GET
    @Path("/sources")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getAudioSources", summary = "Get the list of all sources.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = AudioSourceDTO.class)))) })
    public Response getSources(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language) {
        final Locale locale = localeService.getLocale(language);
        Collection<AudioSource> sources = audioManager.getAllSources();
        List<AudioSourceDTO> dtos = new ArrayList<>(sources.size());
        for (AudioSource source : sources) {
            dtos.add(AudioMapper.map(source, locale));
        }
        return Response.ok(dtos).build();
    }

    @GET
    @Path("/defaultsource")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getAudioDefaultSource", summary = "Get the default source if defined or the first available source.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = AudioSourceDTO.class))),
            @ApiResponse(responseCode = "404", description = "Source not found") })
    public Response getDefaultSource(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language) {
        final Locale locale = localeService.getLocale(language);
        AudioSource source = audioManager.getSource();
        if (source != null) {
            return Response.ok(AudioMapper.map(source, locale)).build();
        } else {
            return JSONResponse.createErrorResponse(Status.NOT_FOUND, "Source not found");
        }
    }

    @GET
    @Path("/sinks")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getAudioSinks", summary = "Get the list of all sinks.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = AudioSinkDTO.class)))) })
    public Response getSinks(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language) {
        final Locale locale = localeService.getLocale(language);
        Collection<AudioSink> sinks = audioManager.getAllSinks();
        List<AudioSinkDTO> dtos = new ArrayList<>(sinks.size());
        for (AudioSink sink : sinks) {
            dtos.add(AudioMapper.map(sink, locale));
        }
        return Response.ok(dtos).build();
    }

    @GET
    @Path("/defaultsink")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getAudioDefaultSink", summary = "Get the default sink if defined or the first available sink.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = AudioSinkDTO.class))),
            @ApiResponse(responseCode = "404", description = "Sink not found") })
    public Response getDefaultSink(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language) {
        final Locale locale = localeService.getLocale(language);
        AudioSink sink = audioManager.getSink();
        if (sink != null) {
            return Response.ok(AudioMapper.map(sink, locale)).build();
        } else {
            return JSONResponse.createErrorResponse(Status.NOT_FOUND, "Sink not found");
        }
    }
}
