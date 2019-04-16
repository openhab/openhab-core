/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.nlp.internal.rest;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.smarthome.core.auth.Role;
import org.eclipse.smarthome.core.voice.VoiceManager;
import org.eclipse.smarthome.core.voice.text.InterpretationException;
import org.eclipse.smarthome.io.rest.LocaleService;
import org.eclipse.smarthome.io.rest.RESTResource;
import org.openhab.nlp.ChatReply;
import org.openhab.nlp.Intent;
import org.openhab.nlp.ItemNamedAttribute;
import org.openhab.nlp.ItemResolver;
import org.openhab.nlp.internal.AnswerFormatter;
import org.openhab.nlp.internal.OpenNLPInterpreter;
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
 * This class describes the /nlp resource of the REST API.
 *
 * @author Yannick Schaus - Initial contribution
 */
@Component
@RolesAllowed({ Role.USER, Role.ADMIN })
@Path(NLPResource.PATH_NLP)
@Api(NLPResource.PATH_NLP)
public class NLPResource implements RESTResource {

    private static final String OPENNLP_HLI = "opennlp";

    private VoiceManager voiceManager;

    private LocaleService localeService;

    private ItemResolver itemResolver;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    public void setVoiceManager(VoiceManager voiceManager) {
        this.voiceManager = voiceManager;
    }

    public void unsetVoiceManager(VoiceManager voiceManager) {
        this.voiceManager = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    public void setLocaleService(LocaleService localeService) {
        this.localeService = localeService;
    }

    public void unsetLocaleService(LocaleService localeService) {
        this.localeService = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setItemNamedAttributesResolver(ItemResolver itemResolver) {
        this.itemResolver = itemResolver;
    }

    protected void unsetItemNamedAttributesResolver(ItemResolver itemResolver) {
        this.itemResolver = null;
    }

    public static final String PATH_NLP = "nlp";

    @GET
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Path("/greet")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Retrieves a first greeting message from the bot in the specified or configured language.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = ChatReply.class),
            @ApiResponse(code = 500, message = "There is no support for the configured language") })
    public Response greet(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language (will use the default if omitted)") String language) {
        final Locale locale = this.localeService.getLocale(null);

        AnswerFormatter answerFormatter = new AnswerFormatter(locale);

        String greeting = answerFormatter.getRandomAnswer("greeting");
        ChatReply reply = new ChatReply(locale, "", new Intent(""), greeting, null, null);
        return Response.ok(reply).build();
    }

    @POST
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Path("/chat")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Send a query to the natural language processor to interpret.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = ChatReply.class),
            @ApiResponse(code = 500, message = "An interpretation error occured") })
    public Response chat(@HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") String language,
            @ApiParam(value = "human language query", required = true) String query) throws Exception {
        final Locale locale = this.localeService.getLocale(null);

        // interpret
        OpenNLPInterpreter hli = (OpenNLPInterpreter) voiceManager.getHLI(OPENNLP_HLI);
        if (hli == null) {
            throw new InterpretationException("The OpenNLP interpreter is not available");
        }
        ChatReply reply = hli.getReply(locale, query);

        return Response.ok(reply).build();
    }

    @GET
    @RolesAllowed({ Role.USER, Role.ADMIN })
    @Path("/attributes")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets all item named attributes.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = ChatReply.class),
            @ApiResponse(code = 500, message = "An error occurred") })
    public Response getAttributes(
            @HeaderParam(HttpHeaders.ACCEPT_LANGUAGE) @ApiParam(value = "language") String language) throws Exception {
        final Locale locale = this.localeService.getLocale(null);

        this.itemResolver.setLocale(locale);
        Map<String, Set<ItemNamedAttribute>> attributesByItemName = new HashMap<String, Set<ItemNamedAttribute>>();
        this.itemResolver.getAllItemNamedAttributes().entrySet().stream()
                .forEach(entry -> attributesByItemName.put(entry.getKey().getName(), entry.getValue()));

        return Response.ok(attributesByItemName).build();
    }

    @Override
    public boolean isSatisfied() {
        return localeService != null && voiceManager != null;
    }
}
