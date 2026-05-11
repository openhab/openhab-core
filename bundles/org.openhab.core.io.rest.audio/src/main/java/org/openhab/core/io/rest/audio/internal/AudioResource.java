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
package org.openhab.core.io.rest.audio.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.annotation.security.RolesAllowed;

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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * This class acts as a REST resource for audio features.
 *
 * @author Laurent Garnier - Initial contribution
 * @author Markus Rathgeb - Migrated to JAX-RS Whiteboard Specification
 * @author Wouter Born - Migrated to OpenAPI annotations
 */
@Component
@JakartarsResource
@JakartarsName(AudioResource.PATH_AUDIO)
@JakartarsApplicationSelect("(" + JakartarsWhiteboardConstants.JAKARTA_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
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
