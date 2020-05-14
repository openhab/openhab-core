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
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * This class acts as a REST resource for audio features.
 *
 * @author Laurent Garnier - Initial contribution
 * @author Markus Rathgeb - Migrated to JAX-RS Whiteboard Specification
 */
@Component
@JaxrsResource
@JaxrsName(AudioResource.PATH_AUDIO)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(AudioResource.PATH_AUDIO)
@RolesAllowed({ Role.USER, Role.ADMIN })
@Api(AudioResource.PATH_AUDIO)
@NonNullByDefault
public class AudioResource implements RESTResource {

    static final String PATH_AUDIO = "audio";

    private @Nullable AudioManager audioManager;
    private @Nullable LocaleService localeService;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    public void setAudioManager(AudioManager audioManager) {
        this.audioManager = audioManager;
    }

    public void unsetAudioManager(AudioManager audioManager) {
        this.audioManager = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setLocaleService(LocaleService localeService) {
        this.localeService = localeService;
    }

    protected void unsetLocaleService(LocaleService localeService) {
        this.localeService = null;
    }

    @GET
    @Path("/sources")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get the list of all sources.", response = AudioSourceDTO.class, responseContainer = "List")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK") })
    public Response getSources(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") @Nullable String language) {
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
    @ApiOperation(value = "Get the default source if defined or the first available source.", response = AudioSourceDTO.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Source not found") })
    public Response getDefaultSource(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") @Nullable String language) {
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
    @ApiOperation(value = "Get the list of all sinks.", response = AudioSinkDTO.class, responseContainer = "List")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK") })
    public Response getSinks(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") @Nullable String language) {
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
    @ApiOperation(value = "Get the default sink if defined or the first available sink.", response = AudioSinkDTO.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Sink not found") })
    public Response getDefaultSink(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") @Nullable String language) {
        final Locale locale = localeService.getLocale(language);
        AudioSink sink = audioManager.getSink();
        if (sink != null) {
            return Response.ok(AudioMapper.map(sink, locale)).build();
        } else {
            return JSONResponse.createErrorResponse(Status.NOT_FOUND, "Sink not found");
        }
    }

    @Override
    public boolean isSatisfied() {
        return audioManager != null && localeService != null;
    }
}
