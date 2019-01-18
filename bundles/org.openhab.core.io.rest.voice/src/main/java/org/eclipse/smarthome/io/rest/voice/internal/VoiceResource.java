/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.io.rest.voice.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.eclipse.smarthome.core.auth.Role;
import org.eclipse.smarthome.core.voice.VoiceManager;
import org.eclipse.smarthome.core.voice.text.HumanLanguageInterpreter;
import org.eclipse.smarthome.core.voice.text.InterpretationException;
import org.eclipse.smarthome.io.rest.JSONResponse;
import org.eclipse.smarthome.io.rest.LocaleService;
import org.eclipse.smarthome.io.rest.RESTResource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * This class acts as a REST resource for voice features.
 *
 * @author Kai Kreuzer - Initial contribution and API
 */
@Component
@Path(VoiceResource.PATH_SITEMAPS)
@RolesAllowed({ Role.USER, Role.ADMIN })
@Api(value = VoiceResource.PATH_SITEMAPS)
public class VoiceResource implements RESTResource {

    static final String PATH_SITEMAPS = "voice";

    @Context
    UriInfo uriInfo;

    private VoiceManager voiceManager;
    private LocaleService localeService;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    public void setVoiceManager(VoiceManager voiceManager) {
        this.voiceManager = voiceManager;
    }

    public void unsetVoiceManager(VoiceManager voiceManager) {
        this.voiceManager = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setLocaleService(LocaleService localeService) {
        this.localeService = localeService;
    }

    protected void unsetLocaleService(LocaleService localeService) {
        this.localeService = null;
    }

    @GET
    @Path("/interpreters")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get the list of all interpreters.", response = HumanLanguageInterpreterDTO.class, responseContainer = "List")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK") })
    public Response getInterpreters(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") String language) {
        final Locale locale = localeService.getLocale(language);
        Collection<HumanLanguageInterpreter> hlis = voiceManager.getHLIs();
        List<HumanLanguageInterpreterDTO> dtos = new ArrayList<>(hlis.size());
        for (HumanLanguageInterpreter hli : hlis) {
            dtos.add(HLIMapper.map(hli, locale));
        }
        return Response.ok(dtos).build();
    }

    @GET
    @Path("/interpreters/{id: [a-zA-Z_0-9]*}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets a single interpreters.", response = HumanLanguageInterpreterDTO.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Interpreter not found") })
    public Response getInterpreter(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") String language,
            @PathParam("id") @ApiParam(value = "interpreter id", required = true) String id) {
        final Locale locale = localeService.getLocale(language);
        HumanLanguageInterpreter hli = voiceManager.getHLI(id);
        if (hli != null) {
            HumanLanguageInterpreterDTO dto = HLIMapper.map(hli, locale);
            return Response.ok(dto).build();
        } else {
            return JSONResponse.createErrorResponse(Status.NOT_FOUND, "Interpreter not found");
        }
    }

    @POST
    @Path("/interpreters/{id: [a-zA-Z_0-9]*}")
    @Consumes(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Sends a text to a given human language interpreter.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "No human language interpreter was found."),
            @ApiResponse(code = 400, message = "interpretation exception occurs") })
    public Response interpret(@HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") String language,
            @ApiParam(value = "text to interpret", required = true) String text,
            @PathParam("id") @ApiParam(value = "interpreter id", required = true) String id) {
        final Locale locale = localeService.getLocale(language);
        HumanLanguageInterpreter hli = voiceManager.getHLI(id);
        if (hli != null) {
            try {
                hli.interpret(locale, text);
                return Response.ok(null, MediaType.TEXT_PLAIN).build();
            } catch (InterpretationException e) {
                return JSONResponse.createErrorResponse(Status.BAD_REQUEST, e.getMessage());
            }
        } else {
            return JSONResponse.createErrorResponse(Status.NOT_FOUND, "Interpreter not found");
        }
    }

    @POST
    @Path("/interpreters")
    @Consumes(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Sends a text to the default human language interpreter.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "No human language interpreter was found."),
            @ApiResponse(code = 400, message = "interpretation exception occurs") })
    public Response interpret(@HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") String language,
            @ApiParam(value = "text to interpret", required = true) String text) {
        final Locale locale = localeService.getLocale(language);
        HumanLanguageInterpreter hli = voiceManager.getHLI();
        if (hli != null) {
            try {
                hli.interpret(locale, text);
                return Response.ok(null, MediaType.TEXT_PLAIN).build();
            } catch (InterpretationException e) {
                return JSONResponse.createErrorResponse(Status.BAD_REQUEST, e.getMessage());
            }
        } else {
            return JSONResponse.createErrorResponse(Status.NOT_FOUND, "No interpreter found");
        }
    }

    @Override
    public boolean isSatisfied() {
        return voiceManager != null && localeService != null;
    }
}
