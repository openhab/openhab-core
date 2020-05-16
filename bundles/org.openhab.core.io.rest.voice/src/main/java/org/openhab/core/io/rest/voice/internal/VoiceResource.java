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
package org.openhab.core.io.rest.voice.internal;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Role;
import org.openhab.core.io.rest.JSONResponse;
import org.openhab.core.io.rest.LocaleService;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.voice.Voice;
import org.openhab.core.voice.VoiceManager;
import org.openhab.core.voice.text.HumanLanguageInterpreter;
import org.openhab.core.voice.text.InterpretationException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
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
 * This class acts as a REST resource for voice features.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Laurent Garnier - add TTS feature to the REST API
 * @author Markus Rathgeb - Migrated to JAX-RS Whiteboard Specification
 */
@Component
@JaxrsResource
@JaxrsName(VoiceResource.PATH_VOICE)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(VoiceResource.PATH_VOICE)
@RolesAllowed({ Role.USER, Role.ADMIN })
@Api(VoiceResource.PATH_VOICE)
@NonNullByDefault
public class VoiceResource implements RESTResource {

    /** The URI path to this resource */
    public static final String PATH_VOICE = "voice";

    private final LocaleService localeService;
    private final VoiceManager voiceManager;

    @Activate
    public VoiceResource( //
            final @Reference LocaleService localeService, //
            final @Reference VoiceManager voiceManager) {
        this.localeService = localeService;
        this.voiceManager = voiceManager;
    }

    @GET
    @Path("/interpreters")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get the list of all interpreters.", response = HumanLanguageInterpreterDTO.class, responseContainer = "List")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK") })
    public Response getInterpreters(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") @Nullable String language) {
        final Locale locale = localeService.getLocale(language);
        List<HumanLanguageInterpreterDTO> dtos = voiceManager.getHLIs().stream().map(hli -> HLIMapper.map(hli, locale))
                .collect(Collectors.toList());
        return Response.ok(dtos).build();
    }

    @GET
    @Path("/interpreters/{id: [a-zA-Z_0-9]+}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets a single interpreter.", response = HumanLanguageInterpreterDTO.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Interpreter not found") })
    public Response getInterpreter(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") @Nullable String language,
            @PathParam("id") @ApiParam(value = "interpreter id") String id) {
        final Locale locale = localeService.getLocale(language);
        HumanLanguageInterpreter hli = voiceManager.getHLI(id);
        if (hli == null) {
            return JSONResponse.createErrorResponse(Status.NOT_FOUND, "No interpreter found");
        }

        HumanLanguageInterpreterDTO dto = HLIMapper.map(hli, locale);
        return Response.ok(dto).build();
    }

    @POST
    @Path("/interpreters/{id: [a-zA-Z_0-9]+}")
    @Consumes(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Sends a text to a given human language interpreter.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "No human language interpreter was found."),
            @ApiResponse(code = 400, message = "interpretation exception occurs") })
    public Response interpret(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") @Nullable String language,
            @ApiParam(value = "text to interpret", required = true) String text,
            @PathParam("id") @ApiParam(value = "interpreter id") String id) {
        final Locale locale = localeService.getLocale(language);
        HumanLanguageInterpreter hli = voiceManager.getHLI(id);
        if (hli == null) {
            return JSONResponse.createErrorResponse(Status.NOT_FOUND, "No interpreter found");
        }

        try {
            hli.interpret(locale, text);
            return Response.ok(null, MediaType.TEXT_PLAIN).build();
        } catch (InterpretationException e) {
            return JSONResponse.createErrorResponse(Status.BAD_REQUEST, e.getMessage());
        }
    }

    @POST
    @Path("/interpreters")
    @Consumes(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Sends a text to the default human language interpreter.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "No human language interpreter was found."),
            @ApiResponse(code = 400, message = "interpretation exception occurs") })
    public Response interpret(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") @Nullable String language,
            @ApiParam(value = "text to interpret", required = true) String text) {
        final Locale locale = localeService.getLocale(language);
        HumanLanguageInterpreter hli = voiceManager.getHLI();
        if (hli == null) {
            return JSONResponse.createErrorResponse(Status.NOT_FOUND, "No interpreter found");
        }

        try {
            hli.interpret(locale, text);
            return Response.ok(null, MediaType.TEXT_PLAIN).build();
        } catch (InterpretationException e) {
            return JSONResponse.createErrorResponse(Status.BAD_REQUEST, e.getMessage());
        }
    }

    @GET
    @Path("/voices")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get the list of all voices.", response = VoiceDTO.class, responseContainer = "List")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK") })
    public Response getVoices() {
        List<VoiceDTO> dtos = voiceManager.getAllVoices().stream().map(VoiceMapper::map).collect(Collectors.toList());
        return Response.ok(dtos).build();
    }

    @GET
    @Path("/defaultvoice")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets the default voice.", response = VoiceDTO.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "No default voice was found.") })
    public Response getDefaultVoice() {
        Voice voice = voiceManager.getDefaultVoice();
        if (voice == null) {
            return JSONResponse.createErrorResponse(Status.NOT_FOUND, "Default voice not found");
        }

        VoiceDTO dto = VoiceMapper.map(voice);
        return Response.ok(dto).build();
    }

    @POST
    @Path("/say")
    @Consumes(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Speaks a given text with a given voice through the given audio sink.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK") })
    public Response say(@ApiParam(value = "text to speak", required = true) String text,
            @QueryParam("voiceid") @ApiParam(value = "voice id") @Nullable String voiceId,
            @QueryParam("sinkid") @ApiParam(value = "audio sink id") @Nullable String sinkId) {
        voiceManager.say(text, voiceId, sinkId);
        return Response.ok(null, MediaType.TEXT_PLAIN).build();
    }
}
