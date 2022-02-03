/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
import java.util.Set;
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
import org.openhab.core.audio.AudioManager;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.auth.Role;
import org.openhab.core.io.rest.JSONResponse;
import org.openhab.core.io.rest.LocaleService;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.voice.KSService;
import org.openhab.core.voice.STTService;
import org.openhab.core.voice.TTSService;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * This class acts as a REST resource for voice features.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Laurent Garnier - add TTS feature to the REST API
 * @author Markus Rathgeb - Migrated to JAX-RS Whiteboard Specification
 * @author Wouter Born - Migrated to OpenAPI annotations
 */
@Component
@JaxrsResource
@JaxrsName(VoiceResource.PATH_VOICE)
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@JSONRequired
@Path(VoiceResource.PATH_VOICE)
@RolesAllowed({ Role.USER, Role.ADMIN })
@Tag(name = VoiceResource.PATH_VOICE)
@NonNullByDefault
public class VoiceResource implements RESTResource {

    /** The URI path to this resource */
    public static final String PATH_VOICE = "voice";

    private final LocaleService localeService;
    private final AudioManager audioManager;
    private final VoiceManager voiceManager;

    @Activate
    public VoiceResource( //
            final @Reference LocaleService localeService, //
            final @Reference AudioManager audioManager, //
            final @Reference VoiceManager voiceManager) {
        this.localeService = localeService;
        this.audioManager = audioManager;
        this.voiceManager = voiceManager;
    }

    @GET
    @Path("/interpreters")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getVoiceInterpreters", summary = "Get the list of all interpreters.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = HumanLanguageInterpreterDTO.class)))) })
    public Response getInterpreters(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language) {
        final Locale locale = localeService.getLocale(language);
        List<HumanLanguageInterpreterDTO> dtos = voiceManager.getHLIs().stream().map(hli -> HLIMapper.map(hli, locale))
                .collect(Collectors.toList());
        return Response.ok(dtos).build();
    }

    @GET
    @Path("/interpreters/{id: [a-zA-Z_0-9]+}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getVoiceInterpreterByUID", summary = "Gets a single interpreter.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = HumanLanguageInterpreterDTO.class)))),
            @ApiResponse(responseCode = "404", description = "Interpreter not found") })
    public Response getInterpreter(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @PathParam("id") @Parameter(description = "interpreter id") String id) {
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
    @Operation(operationId = "interpretText", summary = "Sends a text to a given human language interpreter.", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "No human language interpreter was found."),
            @ApiResponse(responseCode = "400", description = "interpretation exception occurs") })
    public Response interpret(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @Parameter(description = "text to interpret", required = true) String text,
            @PathParam("id") @Parameter(description = "interpreter id") String id) {
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
    @Operation(operationId = "interpretTextByDefaultInterpreter", summary = "Sends a text to the default human language interpreter.", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "No human language interpreter was found."),
            @ApiResponse(responseCode = "400", description = "interpretation exception occurs") })
    public Response interpret(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @Parameter(description = "text to interpret", required = true) String text) {
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
    @Operation(operationId = "getVoices", summary = "Get the list of all voices.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = VoiceDTO.class)))) })
    public Response getVoices() {
        List<VoiceDTO> dtos = voiceManager.getAllVoices().stream().map(VoiceMapper::map).collect(Collectors.toList());
        return Response.ok(dtos).build();
    }

    @GET
    @Path("/defaultvoice")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getDefaultVoice", summary = "Gets the default voice.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = VoiceDTO.class))),
            @ApiResponse(responseCode = "404", description = "No default voice was found.") })
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
    @Operation(operationId = "textToSpeech", summary = "Speaks a given text with a given voice through the given audio sink.", responses = {
            @ApiResponse(responseCode = "200", description = "OK") })
    public Response say(@Parameter(description = "text to speak", required = true) String text,
            @QueryParam("voiceid") @Parameter(description = "voice id") @Nullable String voiceId,
            @QueryParam("sinkid") @Parameter(description = "audio sink id") @Nullable String sinkId) {
        voiceManager.say(text, voiceId, sinkId);
        return Response.ok(null, MediaType.TEXT_PLAIN).build();
    }

    @POST
    @Path("/dialog/{sourceId: [a-zA-Z_0-9]+}/start")
    @Consumes(MediaType.TEXT_PLAIN)
    @Operation(operationId = "startDialog", summary = "Start dialog processing for a given audio source.", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "One of the given ids is wrong."),
            @ApiResponse(responseCode = "400", description = "Services are missing or dialog processing is already started for the audio source.") })
    public Response startDialog(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @Parameter(description = "language") @Nullable String language,
            @PathParam("sourceId") @Parameter(description = "source ID") String sourceId,
            @QueryParam("ksId") @Parameter(description = "keywork spotter ID") @Nullable String ksId,
            @QueryParam("sttId") @Parameter(description = "Speech-to-Text ID") @Nullable String sttId,
            @QueryParam("ttsId") @Parameter(description = "Text-to-Speech ID") @Nullable String ttsId,
            @QueryParam("hliId") @Parameter(description = "interpreter ID") @Nullable String hliId,
            @QueryParam("sinkId") @Parameter(description = "audio sink ID") @Nullable String sinkId,
            @QueryParam("keyword") @Parameter(description = "keyword") @Nullable String keyword,
            @QueryParam("listeningItem") @Parameter(description = "listening item") @Nullable String listeningItem) {
        AudioSource source = getSource(sourceId);
        if (source == null) {
            return JSONResponse.createErrorResponse(Status.NOT_FOUND, "Audio source not found");
        }
        KSService ks = null;
        if (ksId != null) {
            ks = voiceManager.getKS(ksId);
            if (ks == null) {
                return JSONResponse.createErrorResponse(Status.NOT_FOUND, "Keyword spotter not found");
            }
        }
        STTService stt = null;
        if (sttId != null) {
            stt = voiceManager.getSTT(sttId);
            if (stt == null) {
                return JSONResponse.createErrorResponse(Status.NOT_FOUND, "Speech-to-Text not found");
            }
        }
        TTSService tts = null;
        if (ttsId != null) {
            tts = voiceManager.getTTS(ttsId);
            if (tts == null) {
                return JSONResponse.createErrorResponse(Status.NOT_FOUND, "Text-to-Speech not found");
            }
        }
        HumanLanguageInterpreter hli = null;
        if (hliId != null) {
            hli = voiceManager.getHLI(hliId);
            if (hli == null) {
                return JSONResponse.createErrorResponse(Status.NOT_FOUND, "Interpreter not found");
            }
        }
        AudioSink sink = null;
        if (sinkId != null) {
            sink = audioManager.getSink(sinkId);
            if (sink == null) {
                return JSONResponse.createErrorResponse(Status.NOT_FOUND, "Audio sink not found");
            }
        }
        final Locale locale = localeService.getLocale(language);

        try {
            voiceManager.startDialog(ks, stt, tts, hli, source, sink, locale, keyword, listeningItem);
            return Response.ok(null, MediaType.TEXT_PLAIN).build();
        } catch (IllegalStateException e) {
            return JSONResponse.createErrorResponse(Status.BAD_REQUEST, e.getMessage());
        }
    }

    @POST
    @Path("/dialog/{sourceId: [a-zA-Z_0-9]+}/stop")
    @Consumes(MediaType.TEXT_PLAIN)
    @Operation(operationId = "stopDialog", summary = "Stop dialog processing for a given audio source.", responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "No audio source was found."),
            @ApiResponse(responseCode = "400", description = "No dialog processing is started for the audio source.") })
    public Response stopDialog(@PathParam("sourceId") @Parameter(description = "source ID") String sourceId) {
        AudioSource source = getSource(sourceId);
        if (source == null) {
            return JSONResponse.createErrorResponse(Status.NOT_FOUND, "Audio source not found");
        }

        try {
            voiceManager.stopDialog(source);
            return Response.ok(null, MediaType.TEXT_PLAIN).build();
        } catch (IllegalStateException e) {
            return JSONResponse.createErrorResponse(Status.BAD_REQUEST, e.getMessage());
        }
    }

    private @Nullable AudioSource getSource(String sourceId) {
        Set<AudioSource> sources = audioManager.getAllSources();
        for (AudioSource source : sources) {
            if (source.getId().equals(sourceId)) {
                return source;
            }
        }
        return null;
    }
}
